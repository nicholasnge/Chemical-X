package com.example.chemicalx.Fragment_Tasks;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;

import com.example.chemicalx.Fragment_Tasks.crollerTest.Croller;
import com.example.chemicalx.Fragment_Tasks.crollerTest.OnCrollerChangeListener;
import com.example.chemicalx.R;
import com.example.chemicalx.TextClassificationClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.w3c.dom.Text;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class AddTask extends DialogFragment {
    View view;
    EditText taskTitle;
    Croller croller;
    TextView taskDuration;
    TextView dueDateTextView;
    Date taskDueDate;
    Spinner categorySpinner;
    Button createTaskButton;
    int durationHours;
    int durationTenMinutes;
    //for tf model
    TextClassificationClient tf_classifytasks;

    public AddTask(TextClassificationClient tf_classifytasks) {
        this.tf_classifytasks = tf_classifytasks;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullscreenDialogTheme);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.add_todo_fragment, container, false);

        setupToolbar();
        setupTaskTitle();
        setupCroller();
        setupSpinner();
        setupDatePicker();
        setupSubmitButton();
        return view;
    }

    private void setupToolbar() {
        Toolbar t = view.findViewById(R.id.addTaskToolbar);
        t.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }

    private void setupDatePicker() {
        // set up Date Picker
        dueDateTextView = view.findViewById(R.id.taskDueDate);
        dueDateTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar c = Calendar.getInstance(); // for initialising datepicker with current date

                DatePickerDialog DP = new DatePickerDialog(getContext(), new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        taskDueDate = new Date(year-1900, month, dayOfMonth);
                        SimpleDateFormat display = new SimpleDateFormat("EEEE, dd MMM yyyy");
                        dueDateTextView.setText(display.format(taskDueDate));
                    }
                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
                DP.show();
            }
        });
    }

    private void setupSpinner() {
        // set up category spinner
        categorySpinner = view.findViewById(R.id.category_spinner);
        ArrayAdapter<CharSequence> arrayAdapter = ArrayAdapter.createFromResource(view.getContext(), R.array.todo_categories, android.R.layout.simple_spinner_item);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(arrayAdapter);
    }

    private void setupSubmitButton() {
        // set up submit button
        createTaskButton = view.findViewById(R.id.createTaskButton);
        createTaskButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (taskTitle.getText().toString().isEmpty()){
                    Snackbar.make(view, R.string.add_task_error_no_title, BaseTransientBottomBar.LENGTH_SHORT).show();
                    return;
                }

                //pass data back
                Map<String, Object> task = new HashMap<>();
                task.put("title", taskTitle.getText().toString());
                task.put("totalTime", getSeconds(croller.getProgress()));
                task.put("timePassed", 0);
                if (taskDueDate != null) {
                    task.put("dueDate", new Timestamp(taskDueDate));
                }
                // let tf choose category for user if tf specifies so
                if (categorySpinner.getSelectedItem().toString().equals("What do you think?")){
                    task.put("category", tf_classifytasks.classify(taskTitle.getText().toString()));
                } else {
                    task.put("category", categorySpinner.getSelectedItem().toString());
                }

                FirebaseFirestore.getInstance().collection("users")
                        .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .collection("tasks")
                        .add(task)
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e("TASK FAILED TO SAVE", e.getMessage());
                                Toast.makeText(view.getRootView().getContext(), "task failed to save", Toast.LENGTH_LONG).show();
                            }
                        })
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d("TASK SUCEEDED TO SAVE", "SUCCESS");
                    }
                });
                dismiss();
            }
        });
    }

    private void setupCroller() {
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
    }

    private void setupTaskTitle() {
        //set taskTitle to be multiline and enter button to be 'done' action
        taskTitle = view.findViewById(R.id.taskTitle);
        taskTitle.setInputType(EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        taskTitle.setHorizontallyScrolling(false);
        taskTitle.setMaxLines(3);
    }

    private void updateDurationText(int progress) {
        //if duration exceeds 3hours, each ticker is 30 mins beyond 3 hours
        if (progress > 18) {
            progress -= 18;
            durationHours = progress / 2 + 3;
            durationTenMinutes = (progress % 2) * 3;
        }
        //if duration under 3 hours, each ticker is 10 mins
        else {
            durationHours = progress / 6;
            durationTenMinutes = progress % 6;
        }
        if (durationHours == 0) {
            taskDuration.setText(durationTenMinutes + "0:00");
        } else {
            taskDuration.setText(durationHours + ":" + durationTenMinutes + "0:00");
        }
    }

    public int getSeconds(int progress) {
        int minutes = 0;

        //if duration exceeds 3hours, each ticker is 30 mins beyond 3 hours
        if (progress > 18) {
            progress -= 18;
            // number of minutes in 18 ticks + excess ticks * 30min
            minutes = (progress * 30) + 180;
        }
        //if duration under 3 hours, each ticker is 10 mins
        else {
            minutes = progress * 10;
        }
        return minutes * 60; //convert to seconds
    }
}
