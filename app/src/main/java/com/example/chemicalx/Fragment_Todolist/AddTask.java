package com.example.chemicalx.Fragment_Todolist;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.chemicalx.Fragment_Todolist.crollerTest.Croller;
import com.example.chemicalx.Fragment_Todolist.crollerTest.OnCrollerChangeListener;
import com.example.chemicalx.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AddTask extends DialogFragment {
    FirebaseFirestore db;
    EditText taskTitle;
    Croller croller;
    TextView taskDuration;
    Spinner categorySpinner;
    Button createTaskButton;
    int durationHours;
    int durationTenMinutes;

    public AddTask() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullscreenDialogTheme);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.add_todo_fragment, container, false);

        //set taskTitle to be multiline and enter button to be 'done' action
        taskTitle = view.findViewById(R.id.taskTitle);
        taskTitle.setInputType(EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE);
        taskTitle.setHorizontallyScrolling(false);
        taskTitle.setMaxLines(3);

        //croller set onProgressListener
        taskDuration = view.findViewById(R.id.taskDuration);
        croller = view.findViewById(R.id.croller);
        croller.setOnCrollerChangeListener(new OnCrollerChangeListener() {
            @Override
            public void onProgressChanged(Croller croller, int progress) {
                updateDurationText(progress);
            }

            @Override
            public void onStartTrackingTouch(Croller croller) {
            }
            @Override
            public void onStopTrackingTouch(Croller croller) {
            }
        });

        // set up category spinner
        categorySpinner = view.findViewById(R.id.category_spinner);
        ArrayAdapter<CharSequence> arrayAdapter = ArrayAdapter.createFromResource(view.getContext(), R.array.todo_categories, android.R.layout.simple_spinner_item);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(arrayAdapter);

        // set up submit button
        db = FirebaseFirestore.getInstance();
        createTaskButton = view.findViewById(R.id.createTaskButton);
        createTaskButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //pass data back
                Map<String, Object> task = new HashMap<>();
                task.put("title", taskTitle.getText().toString());
                task.put("category", categorySpinner.getSelectedItem().toString());
                task.put("totalTime", getMinutes(croller.getProgress()));
                task.put("timePassed", 0);

                db.collection("users")
                        // TODO: 6/25/2020 change to ID of current user
                        .document("testuser")
                        .collection("tasks")
                        .add(task)
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(container.getContext(), "task failed to save", Toast.LENGTH_LONG).show();
                            }
                        });
                dismiss();
            }
        });

        return view;
    }

    private void updateDurationText(int progress) {
        //if duration exceeds 3hours, each ticker is 30 mins beyond 3 hours
        if (progress > 18){
            progress -= 18;
            durationHours = progress/2 + 3;
            durationTenMinutes = (progress%2) * 3;
        }
        //if duration under 3 hours, each ticker is 10 mins
        else {
            durationHours = progress / 6;
            durationTenMinutes = progress % 6;
        }
        if (durationHours == 0){
            taskDuration.setText(durationTenMinutes + "0:00");
        }
        else {
            taskDuration.setText(durationHours + ":" + durationTenMinutes + "0:00");
        }
    }

    private int getMinutes(int progress) {
        int minutes = 0;

        //if duration exceeds 3hours, each ticker is 30 mins beyond 3 hours
        if (progress > 18){
            progress -= 18;
            // number of minutes in 18 ticks + excess ticks * 30min
            minutes = (progress * 30) + 180;
        }
        //if duration under 3 hours, each ticker is 10 mins
        else {
            minutes = progress * 10;
        }
        return minutes;
    }
}
