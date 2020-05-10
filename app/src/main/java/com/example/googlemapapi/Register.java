package com.example.googlemapapi;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Register extends AppCompatActivity {
    public static final String TAG = "TAG";
    EditText mFullName, mMail, mPassword, mConfirmPass;
    Button mRegisterBtn;
    TextView mLoginBtn;
    FirebaseAuth fAuth;
    FirebaseFirestore fStore;
    ProgressBar progressBar;
    String userID;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mFullName = findViewById(R.id.name);
        mMail = findViewById(R.id.registerEmail);
        mPassword = findViewById(R.id.password);
        mConfirmPass = findViewById(R.id.confirmPassword);
        mRegisterBtn = findViewById(R.id.btnRegister);
        mLoginBtn = findViewById(R.id.loginHere);

        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();

        progressBar = findViewById(R.id.progressBar);

        if (fAuth.getCurrentUser() != null){
            startActivity(new Intent(Register.this, MainActivity.class));
            finish();
        }
        mLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Register.this, Login.class));
            }
        });
        mRegisterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = mFullName.getText().toString();
                String email = mMail.getText().toString();
                String password = mPassword.getText().toString();
                String confirmPass = mConfirmPass.getText().toString();
                confirmCredentials(email, password, confirmPass);
                progressBar.setVisibility(View.VISIBLE);
                registerAccount(email, password, name);
            }
        });
    }
    private void confirmCredentials(String email, String password, String confirmPass){
        if (TextUtils.isEmpty(email)) {
            mMail.setError("Email is Required");
        }
        if (TextUtils.isEmpty(password)){
            mPassword.setError("Password is Required");
        }
        if (!password.equals(confirmPass)){
            mConfirmPass.setError("Password is not the same as confirm pass");
        }
        if (password.length() < 6) {
            mPassword.setError("Password must be 6 or more characters");
        }
    }
    private void registerAccount(final String email, final String pass, final String name){
        fAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(Register.this, "User Created", Toast.LENGTH_SHORT).show();
                    storeToFireStore(name, email, pass);
                    startActivity(new Intent(Register.this, MainActivity.class));
                } else {
                    Toast.makeText(Register.this,"Error! " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.INVISIBLE);
                }
            }
        });
    }
    public void storeToFireStore(String name, String email, String pass){
        userID = fAuth.getCurrentUser().getUid();
        DocumentReference documentReference = fStore.collection("users").document(userID);
        Map<String, Object> user = new HashMap<>();
        user.put("fName", name);
        user.put("email", email);

        documentReference.set(user).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d(TAG, "onSuccess: user profile is created for " + userID);
            }
        });
    }
}
