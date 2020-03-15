package com.thuha.delivrer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ClientLoginActivity extends AppCompatActivity {

    private EditText nEmail, nPassword;
    private Button nLogin, nRegister;
    private FirebaseAuth nAuth;
    private FirebaseAuth.AuthStateListener fireBaseAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_login_activty);
        nAuth = FirebaseAuth.getInstance();
        fireBaseAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if(user!=null){
                    Intent intent = new Intent(ClientLoginActivity.this, ClientMapActivity.class);
                    startActivity(intent);
                    finish();
                    return;
                }
            }
        };
        nEmail = (EditText)findViewById(R.id.email);
        nPassword = (EditText)findViewById(R.id.password);
        nLogin = (Button)findViewById(R.id.login);
        nRegister = (Button)findViewById(R.id.register);

        nRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = nEmail.getText().toString();
                final String password = nPassword.getText().toString();
                nAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(ClientLoginActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(!task.isSuccessful()){
                            Toast.makeText(ClientLoginActivity.this, "sign up error", Toast.LENGTH_SHORT).show();
                        }else{
                            String user_id = nAuth.getCurrentUser().getUid();
                            DatabaseReference current_user_db = FirebaseDatabase.getInstance().getReference().child("Users").child("Clients").child(user_id);
                            current_user_db.setValue(true);
                        }
                    }
                });
            }
        });
        nLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = nEmail.getText().toString();
                final String password = nPassword.getText().toString();
                nAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(ClientLoginActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(!task.isSuccessful()){
                            Toast.makeText(ClientLoginActivity.this, "sign in error", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        nAuth.addAuthStateListener(fireBaseAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        nAuth.removeAuthStateListener(fireBaseAuthListener);
    }
}
