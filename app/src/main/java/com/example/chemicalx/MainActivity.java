package com.example.chemicalx;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.example.chemicalx.Fragment_Schedule.Fragment_Schedule;
import com.example.chemicalx.Fragment_Schedule.ReadCalendarPermissionDialogFragment;
import com.example.chemicalx.Fragment_Insights.Fragment_Insights;
import com.example.chemicalx.Fragment_Tasks.AddTask;
import com.example.chemicalx.Fragment_Tasks.Fragment_Tasks;
import com.example.chemicalx.Fragment_Tasks.TaskItemModel;
import com.example.chemicalx.settings.SettingsActivity;
import com.example.chemicalx.tasksuggester.AutoSuggestTasksBroadcastReceiver;
import com.example.chemicalx.tasksuggester.AutoSuggestTasksService;
import com.example.chemicalx.tasksuggester.RequestTaskSuggestionDialogFragment;
import com.example.chemicalx.tasksuggester.TaskSuggester;
import com.example.chemicalx.tasksuggester.TaskSuggestionResultDialogFragment;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, FirebaseAuth.AuthStateListener,
        ReadCalendarPermissionDialogFragment.ReadCalendarPermissionDialogListener,
        RequestTaskSuggestionDialogFragment.RequestTaskSuggestionDialogListener,
        TaskSuggester.TaskSuggestionResponseListener {
    public static final String TAG = "MainActivity";
    public static final int APPUSAGE_REQUEST_CODE = 1;
    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private ViewPagerAdapter viewPagerAdapter;
    private GoogleSignInClient googleSignInClient;

    // tasks fragment
    Fragment_Tasks task_fragment;
    FirebaseFirestore db;
    public static ArrayList<TaskItemModel> tasks = new ArrayList<>();

    NavigationView navView;
    DrawerLayout drawerLayout;
    ActionBarDrawerToggle toggle;

    private Fragment_Schedule schedule_fragment;

    // for tf model
    public TextClassificationClient tf_classifytasks;

    //autosuggest tasks
    boolean finishedSuggestingTasks = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialise this activity as firebaseAuthListener to check for user account changes
        FirebaseAuth.getInstance().addAuthStateListener(this);

        // set view to activity main
        setContentView(R.layout.activity_main);

        //initialise task classifier tf
        tf_classifytasks = new TextClassificationClient(this);
        tf_classifytasks.load();

        //mount toolbar
        toolbar = (Toolbar) findViewById(R.id.addTaskToolbar);
        setSupportActionBar(toolbar);

        //setup tabs
        viewPager = (ViewPager) findViewById(R.id.viewpager);
        setupViewPager(viewPager);

        //set up navigation drawer
        drawerLayout = findViewById(R.id.drawer);
        navView = findViewById(R.id.design_navigation_view);
        navView.setNavigationItemSelectedListener(this);
        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open, R.string.close);
        drawerLayout.addDrawerListener(toggle);
        getSupportActionBar().setHomeButtonEnabled(true);
        toggle.syncState();

        //check if theres permission to pull app usage stats
        AppOpsManager appOps = (AppOpsManager)
                getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getPackageName());
        if (mode != AppOpsManager.MODE_ALLOWED) {
            startActivityForResult(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), APPUSAGE_REQUEST_CODE);
        }

        //check if there's permission for calendar
        mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getPackageName());
        if (mode != AppOpsManager.MODE_ALLOWED) {
            startActivityForResult(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), APPUSAGE_REQUEST_CODE);
        }

        //set floating action button to open addTodo
        FloatingActionButton fab = findViewById(R.id.floatingActionButton);
        fab.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary));
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment currentActive = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.viewpager + ":" + viewPager.getCurrentItem());
                // based on the current position you can then cast the page to the correct
                // class and call the method:
                if (viewPager.getCurrentItem() == 0 && currentActive != null) {
                    ((Fragment_Schedule) currentActive).addEvent();
                } else {
                    DialogFragment addTodo = new AddTask(tf_classifytasks);
                    addTodo.show(getSupportFragmentManager(), TAG);
                }
            }
        });
    }

    private void startLoginActivity() {
        Intent intent = new Intent(this, FirebaseLoginActivity.class);
        startActivity(intent);
        finish(); // to prevent user from clicking back to this activity
    }

    @Override
    protected void onStart() {
        super.onStart();
        GoogleSignInOptions gso = new GoogleSignInOptions
                .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);

        if (acct != null) {
            String personName = acct.getDisplayName();
            String personEmail = acct.getEmail();
            TextView userDisplay = navView
                    .getHeaderView(0)
                    .findViewById(R.id.userDisplay);
            TextView userContactDisplay = navView
                    .getHeaderView(0)
                    .findViewById(R.id.userContactDisplay);
            userDisplay.setText(personName);
            userContactDisplay.setText(personEmail);
        }
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FirebaseAuth.getInstance().removeAuthStateListener(this);
    }

    private void setupViewPager(ViewPager viewPager) {
        //set up tabs (fragments)
        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        schedule_fragment = new Fragment_Schedule(tf_classifytasks);
        viewPagerAdapter.addFragment(schedule_fragment, "SCHEDULE");

        //taskfragment
        task_fragment = new Fragment_Tasks(tf_classifytasks, tasks);
        viewPagerAdapter.addFragment(task_fragment, "TASKS");

        viewPagerAdapter.addFragment(new Fragment_Insights(), "INSIGHTS");
        viewPager.setAdapter(viewPagerAdapter);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.logOut:
                //sign out using firebase
                AuthUI.getInstance().signOut(this);
                break;
            case R.id.settings:
                Intent toSettings = new Intent(this, SettingsActivity.class);
                startActivity(toSettings);
                break;
            case R.id.suggestTask:
                RequestTaskSuggestionDialogFragment requestTaskSuggestionDialogFragment =
                        new RequestTaskSuggestionDialogFragment(this);
                requestTaskSuggestionDialogFragment.show(getSupportFragmentManager(),
                        "Request Task Suggestion Dialog Fragment");
                break;
            case R.id.addTask:
                FloatingActionButton fab = findViewById(R.id.floatingActionButton);
                fab.callOnClick();
                break;
            default:
        }

        return false;
    }

    private void signOut() {
        googleSignInClient.signOut()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Intent logoutIntent = new Intent(MainActivity.this,
                                LoginActivity.class);
                        startActivity(logoutIntent);
                    }
                });
    }

    @Override
    public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
        if (firebaseAuth.getCurrentUser() == null) {
            startLoginActivity();
        } else {
            firebaseAuth.getCurrentUser().getIdToken(true)
                    .addOnSuccessListener(new OnSuccessListener<GetTokenResult>() {
                        @Override
                        public void onSuccess(GetTokenResult getTokenResult) {
                            Log.d(TAG, "onSuccess: " + getTokenResult.getToken());

                            // after authenticating user, get tasks from firebase
                            getTasks();
                        }
                    });
        }
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }

        @Override
        public int getItemPosition(Object object) {
            if (object instanceof Fragment_Schedule) {
                Fragment_Schedule sched = (Fragment_Schedule) object;
                if (sched.needsToRefresh()) {
                    sched.hasRefreshed();
                    return FragmentPagerAdapter.POSITION_NONE;
                }
            }

            return super.getItemPosition(object);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        switch (requestCode) {
            case Fragment_Schedule.READ_CALENDAR_PERMISSION_REQUEST_CODE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission is granted. Continue the action or workflow
                    // in your app.
                    Fragment_Schedule.isReadCalendarGranted = true;
                    schedule_fragment.refresh();
                    viewPagerAdapter.notifyDataSetChanged();
                } else {
                    // Explain to the user that the feature is unavailable because
                    // the features requires a permission that the user has denied.
                    // At the same time, respect the user's decision. Don't link to
                    // system settings in an effort to convince the user to change
                    // their decision.
                    Fragment_Schedule.isReadCalendarGranted = false;
                }
                return;
            case Fragment_Schedule.WRITE_CALENDAR_PERMISSION_REQUEST_CODE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission is granted. Continue the action or workflow
                    // in your app.
                    Fragment_Schedule.isWriteCalendarGranted = true;
                } else {
                    // Explain to the user that the feature is unavailable because
                    // the features requires a permission that the user has denied.
                    // At the same time, respect the user's decision. Don't link to
                    // system settings in an effort to convince the user to change
                    // their decision.
                    Fragment_Schedule.isWriteCalendarGranted = false;
                }
                return;
            // Other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    public void onReadCalendarPermissionDialogGrantClick(DialogFragment dialog) {
        // request READ_CALENDAR permission
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_CALENDAR},
                Fragment_Schedule.READ_CALENDAR_PERMISSION_REQUEST_CODE);
    }

    public void onReadCalendarPermissionDialogDenyClick(DialogFragment dialog) {
        dialog.getDialog().cancel();
    }

    private void getTasks() {
        db = FirebaseFirestore.getInstance();
        db.collection("users")
                .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .collection("tasks")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.e(TAG, "FirebaseFirestoreException");
                            return;
                        }
                        if (queryDocumentSnapshots != null) {
                            for (DocumentChange docChange : queryDocumentSnapshots.getDocumentChanges()) {
                                if (docChange.getType() == DocumentChange.Type.ADDED) {
                                    DocumentSnapshot snapshot = docChange.getDocument();

                                    TaskItemModel task = new TaskItemModel(
                                            snapshot.getId(),
                                            snapshot.getString("title"),
                                            snapshot.getString("category"),
                                            snapshot.getLong("totalTime").intValue(),
                                            snapshot.getLong("timePassed").intValue(),
                                            snapshot.getTimestamp("dueDate"));

                                    //add task to list of tasks
                                    tasks.add(task);
                                    task_fragment.addTask(task);
                                }
                            }
                            //after retrievin tasks, suggest them if haven't
                            if (!finishedSuggestingTasks){
                                autosuggestTasks(tasks);
                                finishedSuggestingTasks = true;
                            }
                        }
                    }

                });
    }

    @Override
    public void onRequestTaskSuggestionDialogRequestClick(DialogFragment dialog, int durationInMinutes) {
        long nowMillis = Calendar.getInstance().getTimeInMillis();
        TaskSuggester.suggestTask(this, this, tf_classifytasks, tasks, durationInMinutes, nowMillis);
    }

    @Override
    public void onTaskSuggestionResponse(int taskIndex, int durationInMinutes) {
        int numOfHours = durationInMinutes / 60;
        int numOfMinutes = durationInMinutes % 60;
        TaskSuggestionResultDialogFragment taskSuggestionResultDialogFragment;
        if (taskIndex < 0) {
            taskSuggestionResultDialogFragment = new TaskSuggestionResultDialogFragment(
                    null, numOfHours, numOfMinutes);
        } else {
            TaskItemModel task = tasks.get(taskIndex);
            taskSuggestionResultDialogFragment = new TaskSuggestionResultDialogFragment(
                    task, numOfHours, numOfMinutes);
        }
        taskSuggestionResultDialogFragment.show(getSupportFragmentManager(),
                "Task Suggestion Result Dialog Fragment");
    }

    public void autosuggestTasks(ArrayList<TaskItemModel> tasks) {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this /* Activity context */);
        Boolean enable_autosuggest_tasks = sharedPreferences.getBoolean("enable_autosuggest_tasks", true);

        Log.d(TAG, "autosuggest tasks: " + enable_autosuggest_tasks);
        if (enable_autosuggest_tasks) {
            setAutosuggestAlarm();
        }
    }

    private void setAutosuggestAlarm() {
        // Set the alarm to start at approximately 9:00 a.m.
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 9);

        Intent intent = new Intent(this, AutoSuggestTasksBroadcastReceiver.class);
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList("tasks", tasks);
        intent.putExtra("bundle",bundle);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // With setInexactRepeating(), you have to use one of the AlarmManager interval
// constants--in this case, AlarmManager.INTERVAL_DAY.
//        alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
//                AlarmManager.INTERVAL_DAY, pendingIntent);
        AlarmManager alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmMgr.set(AlarmManager.RTC_WAKEUP,calendar.getTimeInMillis(),pendingIntent);
        Log.d(TAG, "auto suggesting tasks");

        //if not yet 9pm, also start suggesting now
//        if (Calendar.getInstance().HOUR_OF_DAY < 21){
//            sendBroadcast(intent);
//        }
    }
}

