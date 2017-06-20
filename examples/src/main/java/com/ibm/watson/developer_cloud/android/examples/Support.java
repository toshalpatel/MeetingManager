package com.ibm.watson.developer_cloud.android.examples;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by Megha on 5/15/2017.
 */
public class Support extends Fragment {

    View myView;
    TextView txtSupport, txthelp;



    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        myView = inflater.inflate(R.layout.support , container, false);
        return myView;
    }
}
