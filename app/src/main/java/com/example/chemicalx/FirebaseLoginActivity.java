package com.example.chemicalx;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Arrays;

public class FirebaseLoginActivity extends AppCompatActivity {
    private static final int AUTHUI_REQUEST_CODE = 10001;
    Button loginRegisterButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firebase_login);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish(); // so the user cant press back into this activity
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AUTHUI_REQUEST_CODE) {
            if (resultCode == RESULT_OK){
                // we have signed in the user or we have a new user
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                Log.d("LoginActivity",user.getEmail());
                if (user.getMetadata().getCreationTimestamp() == user.getMetadata().getLastSignInTimestamp()){
                    Toast.makeText(this, "Welcome new user", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Welcome back", Toast.LENGTH_SHORT).show();
                }

                // login successful, go to main activity
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish(); // dont let the user click back to this
            }
            else {
                // sign in failed
                IdpResponse response = IdpResponse.fromResultIntent(data);
                if (response == null) {
                    Log.d("LoginActivity", "onActivityResult: user cancelled sign in request");
                } else {
                    Log.e("LoginActivity", "onActivityResult:", response.getError());
                }
            }
        }
    }

    public void handleLoginRegister(View view) {
        Intent intent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(Arrays.asList(
                        new AuthUI.IdpConfig.EmailBuilder().build())
                )
                .setLogo(R.drawable.ic_molecular_white)
                .setTheme(R.style.AppTheme)
                .build();
        startActivityForResult(intent, AUTHUI_REQUEST_CODE);
    }
}