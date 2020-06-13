package com.example.chemicalx.Fragment_Todolist;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.chemicalx.Fragment_Todolist.crollerTest.Croller;
import com.example.chemicalx.Fragment_Todolist.crollerTest.OnCrollerChangeListener;
import com.example.chemicalx.R;

import java.util.Objects;

public class AddTodo extends DialogFragment {
    EditText taskTitle;
    Croller croller;
    TextView taskDuration;
    int durationHours;
    int durationTenMinutes;

    public AddTodo() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullscreenDialogTheme);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
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

            @Override
            public void onStartTrackingTouch(Croller croller) {
            }
            @Override
            public void onStopTrackingTouch(Croller croller) {
            }
        });

        return view;
    }
}
