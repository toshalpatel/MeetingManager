package com.example.toshalpatel.contactgroups;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class MainActivity extends Activity {
    ListView listView = null;
    EditText editText = null;
    Cursor cursor = null;
    Context context = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;
        listView =(ListView) findViewById(R.id.mylist);
        editText = (EditText) findViewById(R.id.textview);

        try {
            // Create an array of Strings, for List
            ArrayAdapter<Model> adapter = new InteractiveArrayAdapter(this,getModel());

            // Assign adapter to ListView
            listView.setAdapter(adapter);

            listView.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    Toast.makeText(getApplicationContext(),
                            "Click ListItem Number " + position, Toast.LENGTH_LONG).show();
                }
            });

        } catch(Exception e) {
            Log.d("**** Exception: ",e.getMessage());
        }
    }

    private List<Model> getModel() {
        List<Model> list = new ArrayList<Model>();

        try {
            ContentResolver cr = getContentResolver();
            cursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,null, null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");

            cursor.moveToFirst();
            if (cursor.moveToFirst()) {
                do {
                    String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    String s = name + "\n" + number;
                    list.add(get(s));
                    s = null;
                } while (cursor.moveToNext());
            }
        } catch(Exception e){
            Log.d("Error in Contacts Read:",e.getMessage());
        }

        return list;
    }

    private Model get(String s) {
        return new Model(s);
    }


    public void onClick(View v) {
        switch(v.getId())
        {
            case R.id.mybutton:
                String s="";
                for(int i=0; i<InteractiveArrayAdapter.list.size(); i++)  {
                    if(InteractiveArrayAdapter.list.get(i).isSelected()) {
                        s = s+i+" ";
                    }
                }
                String s1 = null;
                s1 = editText.getText().toString();

                // Check the edittext is empty or not
                if(s1.equals("")){
                    Toast.makeText(MainActivity.this, "Please Enter Any Text", Toast.LENGTH_SHORT).show();
                    return;
                }


                // Check the Group is available or not
                Cursor groupCursor = null;
                String[] GROUP_PROJECTION = new String[] { ContactsContract.Groups._ID, ContactsContract.Groups.TITLE };
                groupCursor = this.getContentResolver().query(ContactsContract.Groups.CONTENT_URI, GROUP_PROJECTION, ContactsContract.Groups.TITLE+ "=?", new String[]{s1}, ContactsContract.Groups.TITLE + " ASC");
                Log.d("*** Here Counts: ","** "+groupCursor.getCount());

                if(groupCursor.getCount() > 0){
                    Toast.makeText(MainActivity.this, "Group is already available", Toast.LENGTH_SHORT).show();
                    return;
                }
                else {
                    Toast.makeText(MainActivity.this, "Not available", Toast.LENGTH_SHORT).show();

                    //Here we create a new Group
                    try {
                        ContentValues groupValues = null;
                        ContentResolver cr = this.getContentResolver();
                        groupValues = new ContentValues();
                        groupValues.put(ContactsContract.Groups.TITLE, s1);
                        cr.insert(ContactsContract.Groups.CONTENT_URI, groupValues);
                        Log.d("###### Success", "##### Group Creation Finished :");
                    }
                    catch(Exception e){
                        Log.d("########### Exception :",""+e.getMessage());
                    }
                    Toast.makeText(MainActivity.this, "Created Successfully", Toast.LENGTH_SHORT).show();
                }

                groupCursor.close();
                groupCursor = null;


                Log.d("**** Fine", " **** Contacts add to Groups...");

                String groupID = null;
                Cursor getGroupID_Cursor = null;
                getGroupID_Cursor = this.getContentResolver().query(ContactsContract.Groups.CONTENT_URI,  GROUP_PROJECTION, ContactsContract.Groups.TITLE+ "=?", new String[]{s1}, null);
                Log.d("** " + getGroupID_Cursor.getCount(), "**** Now Empty Cursor size:");
                getGroupID_Cursor.moveToFirst();
                groupID = (getGroupID_Cursor.getString(getGroupID_Cursor.getColumnIndex("_id")));
                Log.d(" **** Group ID is: ","** "+groupID);

                getGroupID_Cursor.close();
                getGroupID_Cursor = null;


                for(int i=0; i<InteractiveArrayAdapter.list.size(); i++)  {
                    if(InteractiveArrayAdapter.list.get(i).isSelected()) {
                        cursor.moveToPosition(i);
                        String contactID = cursor.getString(cursor.getColumnIndex (ContactsContract.CommonDataKinds.Phone.CONTACT_ID));

                        long contact = Long.parseLong(contactID);
                        long group = Long.parseLong(groupID);

                        addToGroup(contact, group);

                        String name = cursor.getString(cursor.getColumnIndex (ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                        Log.d(" **** Contact Added: ","* :"+name);
                        Toast.makeText(MainActivity.this, name+" Added Successfully", Toast.LENGTH_SHORT).show();
                    }
                }



                break;
        }
    }



    public Uri addToGroup(long personId, long groupId) {

        ContentValues values = new ContentValues();
        values.put(ContactsContract.CommonDataKinds.GroupMembership.RAW_CONTACT_ID,
                personId);
        values.put(
                ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID,
                groupId);
        values
                .put(
                        ContactsContract.CommonDataKinds.GroupMembership.MIMETYPE,
                        ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE);

        return this.context.getContentResolver().insert(ContactsContract.Data.CONTENT_URI, values);

    }



}
