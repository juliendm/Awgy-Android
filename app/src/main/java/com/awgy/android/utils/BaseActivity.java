package com.awgy.android.utils;

import android.support.v7.app.AppCompatActivity;

import com.awgy.android.AppDelegate;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onPause() {
        super.onPause();

        ((AppDelegate)getApplication()).startActivityTransitionTimer();

    }

    @Override
    protected void onResume() {
        super.onResume();

        ((AppDelegate)getApplication()).stopActivityTransitionTimer();

    }

}