package com.ibm.watson.developer_cloud.android.examples;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.DatePicker;
import android.widget.Toast;

import com.firebase.client.Firebase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public class ScheduleMeeting2 extends AppCompatActivity implements Button.OnClickListener {

    DatePicker datePicker;
    Button btnDone;
    DatabaseReference mDatabase;

    String d;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_meeting2);

        datePicker = (DatePicker) findViewById(R.id.datePicker);
        btnDone = (Button) findViewById(R.id.btnDone);

        Firebase.setAndroidContext(this);
        mDatabase = FirebaseDatabase.getInstance().getReference();

        btnDone.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {

        int day = datePicker.getDayOfMonth();
        int month = datePicker.getMonth();
        int year = datePicker.getYear();

        String date = day + "/" + month + "/" + year;
        System.out.println(date);
        final FirebaseAuth mAuth;
        mAuth = FirebaseAuth.getInstance();

        FirebaseUser user = mAuth.getCurrentUser();
        Firebase ref = new Firebase("https://speech-d73b3.firebaseio.com/");

        //SetDate sd = new SetDate(d);
        String userId = mDatabase.push().getKey();
        mDatabase.child("MeetingDetails/MeetingDate").child(userId).setValue(date);
        //ref.child("MeetingDetails").push().setValue(sd);

        Toast.makeText(ScheduleMeeting2.this, "Meeting details stored", Toast.LENGTH_SHORT).show();

        Intent i = new Intent(ScheduleMeeting2.this, HomeActivity.class);
        startActivity(i);

    }
}
