/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.chemicalx;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.util.Log;
import androidx.annotation.WorkerThread;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.tensorflow.lite.Interpreter;

/** Interface to load TfLite model and provide predictions. */
public class TextClassificationClient {
    private static final String TAG = "TextClassificationDemo";
    private static final String MODEL_PATH = "text_classification.tflite";
    private static final String DIC_PATH = "text_classification_vocab.txt";
    private static final String LABEL_PATH = "text_classification_labels.txt";

    private static final int SENTENCE_LEN = 15;  // The maximum length of an input sentence.
    // Simple delimiter to split words.
    private static final String SIMPLE_SPACE_OR_PUNCTUATION = " |\\,|\\.|\\!|\\?|\n";
    /*
     * Reserved values in ImdbDataSet dic:
     * dic["<PAD>"] = 0      used for padding
     * dic["<START>"] = 1    mark for the start of a sentence
     * dic["<UNKNOWN>"] = 2  mark for unknown words (OOV)
     */
    private static final String START = "<START>";
    private static final String PAD = "<PAD>";
    private static final String UNKNOWN = "<UNKNOWN>";

    /** Number of results to show in the UI. */
    private static final int MAX_RESULTS = 3;

    private final Context context;
    private final Map<String, Integer> dic = new HashMap<>();
    private final List<String> labels = new ArrayList<>();
    private Interpreter tflite;

    public TextClassificationClient(Context context) {
        this.context = context;
    }

    /** Load the TF Lite model and dictionary so that the client can start classifying text. */
    @WorkerThread
    public void load() {
        loadModel();
        loadDictionary();
        loadLabels();
    }

    /** Load TF Lite model. */
    @WorkerThread
    private synchronized void loadModel() {
        try {
            ByteBuffer buffer = loadModelFile(this.context.getAssets());
            tflite = new Interpreter(buffer);
            Log.v(TAG, "TFLite model loaded.");
        } catch (IOException ex) {
            Log.e(TAG, ex.getMessage());
        }
    }

    /** Load words dictionary. */
    @WorkerThread
    private synchronized void loadDictionary() {
        try {
            loadDictionaryFile(this.context.getAssets());
            Log.v(TAG, "Dictionary loaded.");
        } catch (IOException ex) {
            Log.e(TAG, ex.getMessage());
        }
    }

    /** Load labels. */
    @WorkerThread
    private synchronized void loadLabels() {
        try {
            loadLabelFile(this.context.getAssets());
            Log.v(TAG, "Labels loaded.");
        } catch (IOException ex) {
            Log.e(TAG, ex.getMessage());
        }
    }

    /** Free up resources as the client is no longer needed. */
    @WorkerThread
    public synchronized void unload() {
        tflite.close();
        dic.clear();
        labels.clear();
    }

    /** Classify an input string and returns the classification results.
     * @return*/
    @WorkerThread
    public synchronized String classify(String text) {
        // Pre-prosessing.
        float[][] input = tokenizeInputText(text);

        // Run inference.
        Log.v(TAG, "Classifying text with TF Lite...");
        float[][] output = new float[1][labels.size()];
        tflite.run(input, output);

        //find highest probability class
        String category = "";
        float max = -10;
        for (int i = 0; i < labels.size(); i++) {
            if (max < output[0][i]){
                max = output[0][i];
                category = labels.get(i);
            }
        }
        return category;
    }



    /** Load TF Lite model from assets. */
    private static MappedByteBuffer loadModelFile(AssetManager assetManager) throws IOException {
        try (AssetFileDescriptor fileDescriptor = assetManager.openFd(MODEL_PATH);
             FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor())) {
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        }
    }

    /** Load dictionary from assets. */
    private void loadLabelFile(AssetManager assetManager) throws IOException {
        try (InputStream ins = assetManager.open(LABEL_PATH);
             BufferedReader reader = new BufferedReader(new InputStreamReader(ins))) {
            // Each line in the label file is a label.
            while (reader.ready()) {
                labels.add(reader.readLine());
            }
        }
    }

    /** Load labels from assets. */
    private void loadDictionaryFile(AssetManager assetManager) throws IOException {
        try (InputStream ins = assetManager.open(DIC_PATH);
             BufferedReader reader = new BufferedReader(new InputStreamReader(ins))) {
            // Each line in the dictionary has two columns.
            // First column is a word, and the second is the index of this word.
            int count = 0;
            while (reader.ready()) {
                String s = reader.readLine();
                dic.put(s, count);
                count++;
            }
        }
    }

    /** Pre-prosessing: tokenize and map the input words into a float array. */
    float[][] tokenizeInputText(String text) {
        float[] tmp = new float[SENTENCE_LEN];
        List<String> array = Arrays.asList(text.toLowerCase().split(SIMPLE_SPACE_OR_PUNCTUATION));

        int index = 0;
        // Prepend <START> if it is in vocabulary file.
        if (dic.containsKey(START)) {
            tmp[index++] = dic.get(START);
        }
        for (String word : array) {
            if (index >= SENTENCE_LEN) {
                break;
            }
            tmp[index++] = dic.containsKey(word)
                    ? dic.get(word)
                    : (int) dic.get(UNKNOWN);
        }
        // Padding and wrapping.
        Arrays.fill(tmp, index, SENTENCE_LEN - 1, (int) dic.get(PAD));
        float[][] ans = {tmp};
        Log.d(TAG, Arrays.toString(ans[0]));
        return ans;
    }

    Map<String, Integer> getDic() {
        return this.dic;
    }

    Interpreter getTflite() {
        return this.tflite;
    }

    List<String> getLabels() {
        return this.labels;
    }
}
