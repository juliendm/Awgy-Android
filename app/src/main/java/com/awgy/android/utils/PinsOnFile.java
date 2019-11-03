package com.awgy.android.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.awgy.android.AppDelegate;
import com.parse.ParseObject;

import java.util.ArrayList;
import java.util.List;

import bolts.Task;

public class PinsOnFile {

    private List<String> mPinNames;
    private SharedPreferences mSharedPreferences;

    private static PinsOnFile mInstance;

    public static PinsOnFile getInstance() {
        if (mInstance == null) {
            mInstance = new PinsOnFile(AppDelegate.getContext());
        }
        return mInstance;
    }

    private PinsOnFile(Context context) {
        mSharedPreferences = context.getSharedPreferences(Constants.KEY_SHARED_PREFERENCES_PINNAMES, Context.MODE_PRIVATE);
        mPinNames = load(mSharedPreferences);
    }

    public void initiate() {
        mPinNames.clear();
        mPinNames.add(Constants.CLASS_GROUPSELFIE);
        mPinNames.add(Constants.CLASS_HASHTAG);
        mPinNames.add(Constants.CLASS_RELATIONSHIP);
        mPinNames.add(Constants.CLASS_USER);

        save(mSharedPreferences, mPinNames);
    }

    public void addPin(String pinName) {
        if (!mPinNames.contains(pinName)) {
            mPinNames.add(pinName);
            if (mPinNames.size() > 150) {
                mPinNames.remove(0);
            }
            add(mSharedPreferences, pinName);
        }
    }

    public Task<Void> clear() {

        List<Task<Void>> tasks = new ArrayList<Task<Void>>();

        tasks.add(ParseObject.unpinAllInBackground());
        for (String pinName : mPinNames) {
            tasks.add(ParseObject.unpinAllInBackground(pinName));
        }
        mPinNames.clear();
        save(mSharedPreferences, mPinNames);

        return Task.whenAll(tasks);
    }

    private void save(SharedPreferences sharedPreferences, List<String> array) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.putInt("size", array.size());
        for(int i=0; i<array.size(); i++)
            editor.putString(i+"", array.get(i));
        editor.apply();
    }

    private void add(SharedPreferences sharedPreferences, String string) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        int previousSize = sharedPreferences.getInt("size", 0);
        if (previousSize < 150) {
            editor.putInt("size", previousSize+1);
            editor.putString(previousSize+"", string);
        } else {
            ParseObject.unpinAllInBackground(sharedPreferences.getString("0", null));
            for(int i=0; i<previousSize-1; i++) {
                editor.putString(i+"", sharedPreferences.getString((i+1)+"", null));
            }
            editor.putString((previousSize-1)+"", string);
        }
        editor.apply();
    }

    private List<String> load(SharedPreferences sharedPreferences) {
        int size = sharedPreferences.getInt("size", 0);
        List<String> array = new ArrayList<String>();
        for(int i=0; i<size; i++)
            array.add(sharedPreferences.getString(i+"", null));
        return array;
    }

}

