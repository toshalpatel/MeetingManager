package com.ibm.watson.developer_cloud.android.examples;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.firebase.client.Firebase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import android.widget.TextView;

public class DetailsActivity extends AppCompatActivity implements Button.OnClickListener {

    TextView txtDetails;
    EditText txtTopic, txtRemarks, txtVenue;
    Button btnOkay;
    DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        txtDetails = (TextView) findViewById(R.id.txtDetails);
        txtTopic = (EditText) findViewById(R.id.txtTopic);
        txtRemarks = (EditText) findViewById(R.id.txtRemarks);
        txtVenue = (EditText) findViewById(R.id.txtVenue);

        btnOkay = (Button) findViewById(R.id.btnOkay);
        btnOkay.setOnClickListener(this);

        Firebase.setAndroidContext(this);
        mDatabase = FirebaseDatabase.getInstance().getReference();

    }

    @Override
    public void onClick(View v) {

        String topic, venue, remark;


        venue = txtVenue.getText().toString().trim();
        remark = txtRemarks.getText().toString().trim();
        topic = txtTopic.getText().toString().trim();
        MeetingDetails md = new MeetingDetails(topic,venue,remark);

        String userId = mDatabase.push().getKey();
        mDatabase.child("MeetingDetails/Meeting").child(userId).setValue(md);


        Intent i = new Intent(DetailsActivity.this,ScheduleMeeting1.class);
        startActivity(i);
    }
}
