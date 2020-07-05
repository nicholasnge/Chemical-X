package com.example.chemicalx;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;
import java.util.List;

import com.example.chemicalx.Fragment_Schedule.Fragment_Schedule;
import com.example.chemicalx.Fragment_Todolist.Fragment_Tasks;
import com.example.chemicalx.Fragment_Insights.Fragment_Insights;
import com.example.chemicalx.settings.SettingsActivity;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GetTokenResult;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, FirebaseAuth.AuthStateListener {
    public static final String TAG = "MainActivity";
    public static final int APPUSAGE_REQUEST_CODE = 1;
    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private GoogleSignInClient googleSignInClient;

    NavigationView navView;
    DrawerLayout drawerLayout;
    ActionBarDrawerToggle toggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // set view to activity main
        setContentView(R.layout.activity_main);

        //mount toolbar
        toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        //set up tabs (fragments one two)
        viewPager = (ViewPager) findViewById(R.id.viewpager);
        setupViewPager(viewPager);
        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

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
        if (mode != AppOpsManager.MODE_ALLOWED){
            startActivityForResult(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), APPUSAGE_REQUEST_CODE);
        }
    }

    private void startLoginActivity() {
        Intent intent = new Intent(this, FirebaseLoginActivity.class);
        startActivity(intent);
        finish(); // to prevent user from clicking back to this activity
    }

    @Override
    protected void onStart() {
        super.onStart();

        // SETTING UP GOOGLE ACCOUNT
        // TODO: 6/25/2020 check if this code needs to be removed with the new firebase login
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

        // Initialise this activity as firebaseAuthListener to check for user account changes
        FirebaseAuth.getInstance().addAuthStateListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        FirebaseAuth.getInstance().removeAuthStateListener(this);
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new Fragment_Schedule(), "SCHEDULE");
        adapter.addFragment(new Fragment_Tasks(), "TASKS");
        adapter.addFragment(new Fragment_Insights(), "INSIGHTS");
        viewPager.setAdapter(adapter);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id){
            case R.id.logOut:
//                sign out using google api (jay version)
//                signOut();

                //sign out using firebase
                AuthUI.getInstance().signOut(this);
                break;
            case R.id.settings:
                Intent toSettings = new Intent(this, SettingsActivity.class);
                startActivity(toSettings);
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
    }
}
