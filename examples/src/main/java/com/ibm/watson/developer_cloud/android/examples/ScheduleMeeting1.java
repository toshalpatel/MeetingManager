package com.ibm.watson.developer_cloud.android.examples;


import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.TimePicker;


import com.firebase.client.Firebase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;

public class ScheduleMeeting1 extends AppCompatActivity implements Button.OnClickListener {

    TimePicker timePicker;
    Button btnDone;
    String time;
    int selectedHours, selectedMinutes;
    DatabaseReference mDatabase;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_meeting1);

        timePicker = (TimePicker) findViewById(R.id.timePicker);
        btnDone = (Button) findViewById(R.id.btnDone);



        Firebase.setAndroidContext(this);
        mDatabase = FirebaseDatabase.getInstance().getReference();

        btnDone.setOnClickListener(this);
    }
    @Override

    public void onClick(View v) {

        int hour = timePicker.getCurrentHour();
        int mins = timePicker.getCurrentMinute();

        time = hour + ":" + mins;
        final FirebaseAuth mAuth;
        mAuth = FirebaseAuth.getInstance();

        FirebaseUser user = mAuth.getCurrentUser();
        Firebase ref = new Firebase("https://speech-d73b3.firebaseio.com/");

        //SetTime st = new SetTime(time);
        String userId = mDatabase.push().getKey();
        mDatabase.child("MeetingDetails/MeetingTime").child(userId).setValue(time);

        Intent i = new Intent(ScheduleMeeting1.this,ScheduleMeeting2.class);
        startActivity(i);
    }
}