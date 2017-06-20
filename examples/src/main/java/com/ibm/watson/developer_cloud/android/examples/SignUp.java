package com.ibm.watson.developer_cloud.android.examples;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.firebase.client.Firebase;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignUp extends AppCompatActivity implements Button.OnClickListener{

    EditText txtName, txtDept, txtComp, txtEmail, txtPassword, txtConfPassword, txtPhone;
    Button btnSignUp;
    Firebase ref;
    DatabaseReference mDatabase;
    String email, password, name, comp, dept, phno;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        txtName = (EditText) findViewById(R.id.txtName);
        txtDept = (EditText) findViewById(R.id.txtDept);
        txtComp = (EditText) findViewById(R.id.txtComp);
        txtPhone = (EditText) findViewById(R.id.txtPhone);
        txtEmail = (EditText) findViewById(R.id.txtEmail);
        txtPassword = (EditText) findViewById(R.id.txtPassword);
        txtConfPassword = (EditText) findViewById(R.id.txtConfPassword);

        Firebase.setAndroidContext(this);

        ref = new Firebase("https://speech-d73b3.firebaseio.com/");
        mDatabase = FirebaseDatabase.getInstance().getReference();

        btnSignUp = (Button) findViewById(R.id.btnSignUp);
        btnSignUp.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        email = txtEmail.getText().toString().trim();
        String confpassword = txtConfPassword.getText().toString().trim();
        password = txtPassword.getText().toString().trim();
        if(confpassword.equals(password))
        {
            final FirebaseAuth mAuth;

            name = txtName.getText().toString().trim();
            comp = txtComp.getText().toString().trim();
            dept = txtDept.getText().toString().trim();
            phno = txtPhone.getText().toString().trim();

            mAuth = FirebaseAuth.getInstance();

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(SignUp.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                // Sign in success, update UI with the signed-in user's information
                                //Log.d(TAG, "createUserWithEmail:success");
                                User u = new User(email,password,name,comp,dept,phno);
                                String userId = mDatabase.push().getKey();
                                mDatabase.child("Users").child(userId).setValue(u);
                                Toast.makeText(SignUp.this, "Successfully registered", Toast.LENGTH_SHORT).show();

                                Intent i = new Intent(SignUp.this, LoginActivity.class);
                                startActivity(i);

                                //updateUI(user);
                            } else {
                                // If sign in fails, display a message to the user.
                                //Log.w(TAG, "createUserWithEmail:failure", task.getException());
                                Toast.makeText(SignUp.this, "Authentication failed",
                                        Toast.LENGTH_SHORT).show();
                                //updateUI(null);
                            }
                        }
                    });
        }
        else {
            Toast.makeText(SignUp.this, "Password doesn't match", Toast.LENGTH_SHORT).show();
        }
    }
}
