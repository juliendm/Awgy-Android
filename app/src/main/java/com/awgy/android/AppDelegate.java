package com.awgy.android;


import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.awgy.android.models.Activity;
import com.awgy.android.models.GroupSelfie;

import com.awgy.android.models.Hashtag;
import com.awgy.android.models.Relationship;
import com.awgy.android.models.SingleSelfie;
import com.awgy.android.utils.Constants;
import com.awgy.android.utils.NeedNetwork;
import com.parse.Parse;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

public class AppDelegate extends Application {

    private static Context mContext;
    public static Context getContext() { return mContext; }

    private Timer mActivityTransitionTimer;
    private TimerTask mActivityTransitionTimerTask;
    private final long MAX_ACTIVITY_TRANSITION_TIME_MS = 2000;

    private static boolean mApplicationWillEnterForeground = false;
    public static void setApplicationWillEnterForeground(boolean applicationWillEnterForeground) {
        mApplicationWillEnterForeground = applicationWillEnterForeground;
    }
    public static boolean getApplicationWillEnterForeground() {
        return mApplicationWillEnterForeground;
    }

	@Override
	public void onCreate() { 
		super.onCreate();

        mContext = getApplicationContext();

        ParseObject.registerSubclass(GroupSelfie.class);
        ParseObject.registerSubclass(SingleSelfie.class);
        ParseObject.registerSubclass(Activity.class);
        ParseObject.registerSubclass(Relationship.class);
        ParseObject.registerSubclass(Hashtag.class);

        Parse.initialize(new Parse.Configuration.Builder(this)
                        .applicationId("awgy")
                        .clientKey("...")
                        .server("https://api.awgy.com/parse/")
                        .enableLocalDataStore()
                        .build()
        );

        updateParseInstallation();

	}

	public static void updateParseInstallation() {

        ParseUser user = ParseUser.getCurrentUser();
        if (user != null) {
            final ParseInstallation installation = ParseInstallation.getCurrentInstallation();
		    installation.put(Constants.KEY_INSTALLATION_USER, user);
            installation.saveInBackground();
        }

	}

    public void startActivityTransitionTimer() {
        mActivityTransitionTimer = new Timer();
        mActivityTransitionTimerTask = new TimerTask() {
            public void run() {
                NeedNetwork.getInstance().clear();
                mApplicationWillEnterForeground = true;
            }
        };
        mActivityTransitionTimer.schedule(mActivityTransitionTimerTask,
                MAX_ACTIVITY_TRANSITION_TIME_MS);
    }

    public void stopActivityTransitionTimer() {
        if (mActivityTransitionTimerTask != null) {
            mActivityTransitionTimerTask.cancel();
        }
        if (mActivityTransitionTimer != null) {
            mActivityTransitionTimer.cancel();
        }
    }

}
