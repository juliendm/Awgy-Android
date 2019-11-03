package com.awgy.android.ui.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.FragmentTabHost;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.awgy.android.R;
import com.awgy.android.adapters.HashtagsAdapter;
import com.awgy.android.adapters.OngoingAdapter;
import com.awgy.android.adapters.StreamAdapter;
import com.awgy.android.adapters.UsersAdapter;
import com.awgy.android.models.GroupSelfie;
import com.awgy.android.models.PhoneBook;
import com.awgy.android.models.SingleSelfie;
import com.awgy.android.ui.camera.CameraActivity;
import com.awgy.android.ui.camera.NotificationsActivity;
import com.awgy.android.ui.camera.SetUpActivity;
import com.awgy.android.ui.login.AcceptTermsOfUseActivity;
import com.awgy.android.ui.login.LoginActivity;
import com.awgy.android.ui.menu.MenuFragment;
import com.awgy.android.ui.ongoing.OngoingFragment;
import com.awgy.android.ui.search.SearchFragment;
import com.awgy.android.ui.search.SearchListFragment;
import com.awgy.android.ui.stream.GroupSelfieActivity;
import com.awgy.android.ui.stream.StreamFragment;
import com.awgy.android.utils.BaseActivity;
import com.awgy.android.utils.Constants;
import com.awgy.android.utils.NeedNetwork;
import com.awgy.android.utils.PinsOnFile;
import com.crashlytics.android.Crashlytics;
import com.google.common.base.Joiner;
import com.parse.ParseCloud;
import com.parse.ParseInstallation;
import com.parse.ParseUser;

import io.fabric.sdk.android.Fabric;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import bolts.Continuation;
import bolts.Task;

public class MainActivity extends BaseActivity implements CameraActivity.CameraActivityDelegate, SetUpActivity.SetUpActivityDelegate {

    public static TextView badge_stream;
    public static TextView badge_ongoing;

    public FragmentTabHost mTabHost;

    private static Activity mActivity;
    public static Activity getActivity() { return mActivity; }
    private static CameraActivity.CameraActivityDelegate mCameraDelegate;
    public static CameraActivity.CameraActivityDelegate getCameraDelegate() { return mCameraDelegate; }
    private static SetUpActivity.SetUpActivityDelegate mSetUpDelegate;
    public static SetUpActivity.SetUpActivityDelegate getSetUpDelegate() { return mSetUpDelegate; }

    private static Integer staticTabToSelectOnResume;
    public static void setTabToSelectOnResume(Integer tabToSelectOnResume) {
        staticTabToSelectOnResume = tabToSelectOnResume;
    }

    private static JSONObject staticShouldStartActivityForGroupSelfieNewNotificationPayload;
    public static void setStaticShouldStartActivityForGroupSelfieNewNotificationPayload(JSONObject notificationPayload) {
        staticShouldStartActivityForGroupSelfieNewNotificationPayload = notificationPayload;
    }
    private static JSONObject staticShouldStartActivityForGroupSelfieReNewNotificationPayload;
    public static void setStaticShouldStartActivityForGroupSelfieReNewNotificationPayload(JSONObject notificationPayload) {
        staticShouldStartActivityForGroupSelfieReNewNotificationPayload = notificationPayload;
    }
    private static JSONObject staticShouldStartActivityForGroupSelfieReadyNotificationPayload;
    public static void setStaticShouldStartActivityForGroupSelfieReadyNotificationPayload(JSONObject notificationPayload) {
        staticShouldStartActivityForGroupSelfieReadyNotificationPayload = notificationPayload;
    }
    private static JSONObject staticShouldStartActivityForActivityNotificationPayload;
    public static void setStaticShouldStartActivityForActivityNotificationPayload(JSONObject notificationPayload) {
        staticShouldStartActivityForActivityNotificationPayload = notificationPayload;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());

        ParseUser currentUser = ParseUser.getCurrentUser();

        if (currentUser == null) {

            navigateToLogin();

        } else {

            setContentView(R.layout.activity_main);

            mActivity = this;
            mCameraDelegate = this;
            mSetUpDelegate = this;

            StreamFragment.setStaticStreamAdapter(new StreamAdapter(null));
            OngoingFragment.setStaticOngoingAdapter(new OngoingAdapter(null));
            SearchListFragment.setStaticUsersAdapter(new UsersAdapter(null));
            SearchListFragment.setStaticHashtagsAdapter(new HashtagsAdapter(null));

            // Action Bar

            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayShowTitleEnabled(false);
                actionBar.setDisplayShowCustomEnabled(true);
            }

            // Cache Policy
            NeedNetwork.getInstance().clear();

            // PhoneBook
            PhoneBook.getInstance().setPhoneNumber(currentUser.getUsername());
            PhoneBook.getInstance().fetchPhoneBook();

            mTabHost = (FragmentTabHost) findViewById(android.R.id.tabhost);
            mTabHost.setup(this, getSupportFragmentManager(), R.id.real_tab_content);

            FragmentTabHost.TabSpec streamTabSpec = mTabHost.newTabSpec("stream");
            FragmentTabHost.TabSpec ongoingTabSpec = mTabHost.newTabSpec("ongoing");
            FragmentTabHost.TabSpec dummyTabSpec = mTabHost.newTabSpec("dummy");
            FragmentTabHost.TabSpec searchTabSpec = mTabHost.newTabSpec("search");
            FragmentTabHost.TabSpec menuTabSpec = mTabHost.newTabSpec("menu");

            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int width = (int) Math.ceil(size.x / 5.0);

            // Camera

            ImageButton cameraButton = (ImageButton) findViewById(R.id.cameraButton);
            cameraButton.getLayoutParams().width = width;
            cameraButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    CameraActivity.setNeedResetBitmap(true);
                    CameraActivity.setNeedResetGroupSelfie(true);
                    CameraActivity.setDelegate(MainActivity.this);
                    Intent cameraActivityIntent = new Intent(MainActivity.this, CameraActivity.class);
                    startActivity(cameraActivityIntent);

                }
            });


            // Tabs

            LinearLayout.LayoutParams img_llp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            img_llp.gravity = Gravity.CENTER_VERTICAL;
            img_llp.width = width;
            int image_padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());

            RelativeLayout.LayoutParams bckgd_llp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
            bckgd_llp.width = width;

            // Stream

            ImageView img = new ImageView(this);
            img.setPadding(image_padding, image_padding, image_padding, image_padding);
            img.setLayoutParams(img_llp);
            img.setImageDrawable(getResources().getDrawable(R.drawable.stream));

            RelativeLayout bckgd = new RelativeLayout(this);
            bckgd.setLayoutParams(bckgd_llp);
            bckgd.setBackgroundResource(R.drawable.background_selector);
            bckgd.addView(img);
            badge_stream = (TextView) LayoutInflater.from(getActivity()).inflate(R.layout.badge_view, bckgd, false);
            bckgd.addView(badge_stream);

            streamTabSpec.setIndicator(bckgd);

            // Ongoing

            img = new ImageView(this);
            img.setPadding(image_padding, image_padding, image_padding, image_padding);
            img.setLayoutParams(img_llp);
            img.setImageDrawable(getResources().getDrawable(R.drawable.ongoing));

            bckgd = new RelativeLayout(this);
            bckgd.setLayoutParams(bckgd_llp);
            bckgd.setBackgroundResource(R.drawable.background_selector);
            bckgd.addView(img);
            badge_ongoing = (TextView) LayoutInflater.from(getActivity()).inflate(R.layout.badge_view, bckgd, false);
            bckgd.addView(badge_ongoing);

            ongoingTabSpec.setIndicator(bckgd);

            // Dummy

            View view = new View(this);
            view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
            view.getLayoutParams().width = width;
            dummyTabSpec.setIndicator(view);

            // Search

            img = new ImageView(this);
            img.setPadding(image_padding, image_padding, image_padding, image_padding);
            img.setLayoutParams(img_llp);
            img.setImageDrawable(getResources().getDrawable(R.drawable.search));

            bckgd = new RelativeLayout(this);
            bckgd.setLayoutParams(bckgd_llp);
            bckgd.setBackgroundResource(R.drawable.background_selector);
            bckgd.addView(img);

            searchTabSpec.setIndicator(bckgd);



            // Menu

            img = new ImageView(this);
            img.setPadding(image_padding, image_padding, image_padding, image_padding);
            img.setLayoutParams(img_llp);
            img.setImageDrawable(getResources().getDrawable(R.drawable.settings));

            bckgd = new RelativeLayout(this);
            bckgd.setLayoutParams(bckgd_llp);
            bckgd.setBackgroundResource(R.drawable.background_selector);
            bckgd.addView(img);

            menuTabSpec.setIndicator(bckgd);

            // Load All Caches (has to be after badges are created)

            StreamFragment.getStaticStreamAdapter().loadCache();
            OngoingFragment.getStaticOngoingAdapter().loadCache();

            SearchListFragment.getStaticUsersAdapter().loadCache();
            SearchListFragment.getStaticHashtagsAdpater().loadCache();

            // Add tabSpec to the TabHost

            Fragment fragment = new Fragment();

            mTabHost.addTab(streamTabSpec, StreamFragment.class, null);
            mTabHost.addTab(ongoingTabSpec, OngoingFragment.class, null);
            mTabHost.addTab(dummyTabSpec, Fragment.class, null);
            mTabHost.addTab(searchTabSpec, SearchFragment.class, null);
            mTabHost.addTab(menuTabSpec, MenuFragment.class, null);

            // Start Activities

            if (staticShouldStartActivityForGroupSelfieNewNotificationPayload != null) {
                shouldStartActivityForGroupSelfieNewNotificationPayload(staticShouldStartActivityForGroupSelfieNewNotificationPayload);
                staticShouldStartActivityForGroupSelfieNewNotificationPayload = null;
            }

            if (staticShouldStartActivityForGroupSelfieReNewNotificationPayload != null) {
                shouldStartActivityForGroupSelfieReNewNotificationPayload(staticShouldStartActivityForGroupSelfieReNewNotificationPayload);
                staticShouldStartActivityForGroupSelfieReNewNotificationPayload = null;
            }

            if (staticShouldStartActivityForGroupSelfieReadyNotificationPayload != null) {
                shouldStartActivityForGroupSelfieReadyNotificationPayload(staticShouldStartActivityForGroupSelfieReadyNotificationPayload);
                staticShouldStartActivityForGroupSelfieReadyNotificationPayload = null;
            }

            if (staticShouldStartActivityForActivityNotificationPayload != null) {
                shouldStartActivityForActivityNotificationPayload(staticShouldStartActivityForActivityNotificationPayload);
                staticShouldStartActivityForActivityNotificationPayload = null;
            }

        }

    }

    @Override
    public void onResume() {
        super.onResume();

        if (staticTabToSelectOnResume != null) {
            mTabHost.setCurrentTab(staticTabToSelectOnResume);
            staticTabToSelectOnResume = null;
        }

        checkAnnouncement();
        checkConditions();
    }

    @Override
    public void onCameraSendButtonClick(final GroupSelfie groupSelfie, final Bitmap bitmap) {

        SingleSelfie singleSelfie = new SingleSelfie();
        singleSelfie.saveInBackgroundWithBitmap(bitmap, groupSelfie);

    }

    @Override
    public void onSetUpSendButtonClick(final GroupSelfie groupSelfie, final Bitmap bitmap) {

        groupSelfie.saveGroupSelfie().continueWithTask(new Continuation<Void, Task<Void>>() {
            @Override
            public Task<Void> then(Task<Void> task) throws Exception {
                if (!task.isFaulted() && !task.isCancelled()) {

                    SingleSelfie singleSelfie = new SingleSelfie();
                    singleSelfie.saveInBackgroundWithBitmap(bitmap, groupSelfie);

                    // Warn if not on app

                    final List<String> notOnApp = new ArrayList<String>();
                    for (int index = 0; index < groupSelfie.getGroupIds().size(); index++) {
                        if (groupSelfie.getGroupIds().get(index).equals("---")) {
                            notOnApp.add(groupSelfie.getGroupUsernames().get(index));
                        }
                    }

                    if (notOnApp.size() > 0) {
                        NotificationsActivity.setStaticNotOnApp(notOnApp);
                        NotificationsActivity.setStaticGroupSelfie(groupSelfie);
                        Intent notificationsActivityIntent = new Intent(MainActivity.this, NotificationsActivity.class);
                        notificationsActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(notificationsActivityIntent);
                    }

                    groupSelfie.syncCountDown().continueWithTask(new Continuation<Double, Task<Void>>() {
                        @Override
                        public Task<Void> then(Task<Double> task) throws Exception {
                            if (!task.isFaulted() && !task.isCancelled() && StreamFragment.getStaticStreamAdapter() != null) {
                                OngoingFragment.getStaticOngoingAdapter().manageGroupSelfie(groupSelfie);
                            }
                            return null;
                        }
                    });

                } else {

                    String message = "";
                    if (task.getError() != null) {
                        message = task.getError().getMessage();
                    }

                    final String finalMessage = message;

                    SetUpActivity.getStaticRecipientsAdpater().loadCache().continueWithTask(new Continuation<Void, Task<Void>>() {
                        @Override
                        public Task<Void> then(Task<Void> task) throws Exception {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    CameraActivity.setStaticGroupSelfie(groupSelfie);
                                    CameraActivity.setStaticBitmap(bitmap);
                                    CameraActivity.setDelegate(MainActivity.this);
                                    Intent cameraActivityIntent = new Intent(MainActivity.this, CameraActivity.class);
                                    cameraActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                    startActivity(cameraActivityIntent);

                                    SetUpActivity.setNeedShowMessage(finalMessage);
                                    SetUpActivity.setStaticGroupSelfie(groupSelfie);
                                    SetUpActivity.setStaticBitmap(bitmap);
                                    SetUpActivity.setDelegate(MainActivity.this);
                                    Intent setUpActivityIntent = new Intent(MainActivity.this, SetUpActivity.class);
                                    setUpActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                    startActivity(setUpActivityIntent);

                                }
                            });
                            return null;
                        }
                    });


                }
                return null;
            }
        });

    }

    public void checkAnnouncement() {

        if (ParseUser.getCurrentUser() != null) {

            ParseCloud.callFunctionInBackground(Constants.KEY_USER_FUNCTION_CHECK_HAS_ANNOUNCEMENT, new HashMap<String, Object>()).continueWithTask(new Continuation<Object, Task<Void>>() {
                @Override
                public Task<Void> then(Task<Object> task) {
                    if (!task.isFaulted() && !task.isCancelled()) {

                        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                        String message = (String) task.getResult();
                        String[] messages = message.split("\n");

                        if (messages.length > 1) {

                            final String lastPart = messages[messages.length - 1];

                            if (lastPart.split("://")[0].equals("https")) {

                                String body_message = null;
                                if (messages.length > 2) {
                                    body_message = Joiner.on("\n").join(Arrays.copyOfRange(messages, 1, messages.length - 2));
                                }


                                builder.setMessage(body_message)
                                        .setTitle(messages[0])
                                        .setPositiveButton(getResources().getString(R.string.details), new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {

                                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(lastPart));
                                                startActivity(browserIntent);

                                            }
                                        })
                                        .setNegativeButton(getResources().getString(R.string.ok), null);


                            } else {

                                String body_message = Joiner.on("\n").join(Arrays.copyOfRange(messages, 1, messages.length - 1));

                                builder.setMessage(body_message)
                                        .setTitle(messages[0])
                                        .setPositiveButton(getResources().getString(R.string.ok), null);

                            }

                        } else {

                            builder.setMessage(message)
                                    .setTitle(getResources().getString(R.string.announcement))
                                    .setPositiveButton(getResources().getString(R.string.ok), null);

                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                AlertDialog dialog = builder.create();
                                dialog.show();
                            }
                        });


                    }
                    return null;
                }
            });

        }
    }

    public void checkConditions() {

        final NeedNetwork needNetwork = NeedNetwork.getInstance();
        if (needNetwork.needNetworkForPinName(Constants.KEY_ACCEPT_CONDITIONS)) {

            if (ParseUser.getCurrentUser() != null) {

                ParseCloud.callFunctionInBackground(Constants.KEY_USER_FUNCTION_CHECK_HAS_ACCEPTED, new HashMap<String, Object>()).continueWithTask(new Continuation<Object, Task<Void>>() {
                    @Override
                    public Task<Void> then(Task<Object> task) {
                        if (!task.isFaulted() && !task.isCancelled()) {
                            needNetwork.addDone(Constants.KEY_ACCEPT_CONDITIONS);
                            Number accepted = (Number) task.getResult();
                            if (accepted.intValue() == 0) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {

                                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                        builder.setMessage(getResources().getString(R.string.verif_new_conditions))
                                                .setTitle(getResources().getString(R.string.conditions_changed))
                                                .setPositiveButton(getResources().getString(R.string.review), new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {

                                                        Intent intent = new Intent(MainActivity.this, AcceptTermsOfUseActivity.class);
                                                        intent.putExtra(Constants.KEY_ALREADY_LOG_IN, true);
                                                        startActivity(intent);

                                                    }
                                                })
                                                .setNegativeButton(getResources().getString(R.string.log_out), new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        logOutUser();
                                                    }
                                                });
                                        AlertDialog dialog = builder.create();
                                        dialog.setCanceledOnTouchOutside(false);
                                        dialog.show();
                                    }
                                });
                            }
                        }
                        return null;
                    }
                });

            }

        }
    }

    public void logOutUser() {

        final ProgressDialog progressDialog = ProgressDialog.show(MainActivity.this, null, getResources().getString(R.string.please_wait));
        ParseInstallation installation = ParseInstallation.getCurrentInstallation();
        installation.remove(Constants.KEY_INSTALLATION_USER);
        installation.saveInBackground().continueWithTask(new Continuation<Void, Task<Void>>() {
            @Override
            public Task<Void> then(Task<Void> task) throws Exception {
                progressDialog.dismiss();
                PhoneBook.getInstance().clear();
                PinsOnFile.getInstance().clear();
                NeedNetwork.getInstance().clear();
                ParseUser.logOut();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }
                });
                return null;
            }
        });

    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    // Navigation

    public void shouldStartActivityForGroupSelfieNewNotificationPayload(final JSONObject notificationPayload) {

        GroupSelfie.groupSelfieWithNotificationPayload(notificationPayload, false).continueWithTask(new Continuation<GroupSelfie, Task<Void>>() {
            @Override
            public Task<Void> then(Task<GroupSelfie> task) throws Exception {
                if (!task.isFaulted() && !task.isCancelled()) {
                    handleGroupSelfie(task.getResult());
                }
                return null;
            }
        });

    }

    public void shouldStartActivityForGroupSelfieReNewNotificationPayload(final JSONObject notificationPayload) {

        GroupSelfie.groupSelfieWithNotificationPayload(notificationPayload, false).continueWithTask(new Continuation<GroupSelfie, Task<Void>>() {
            @Override
            public Task<Void> then(Task<GroupSelfie> task) throws Exception {
                if (!task.isFaulted() && !task.isCancelled()) {
                    handleGroupSelfie(task.getResult());
                }
                return null;
            }
        });

    }

    public void handleGroupSelfie(final GroupSelfie groupSelfie) {

        groupSelfie.syncCountDown().continueWithTask(new Continuation<Double, Task<Void>>() {
            @Override
            public Task<Void> then(Task<Double> task) throws Exception {
                if (!task.isFaulted() && !task.isCancelled()) {

                    if (task.getResult() > 0.0) {

                        final Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                        CameraActivity.setStaticGroupSelfie(groupSelfie);
                        CameraActivity.setNeedResetBitmap(true);
                        CameraActivity.setDelegate(MainActivity.this);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                startActivity(intent);
                            }
                        });

                    }

                }
                return null;
            }
        });

    }

    public void shouldStartActivityForGroupSelfieReadyNotificationPayload(final JSONObject notificationPayload) {

        GroupSelfie.groupSelfieWithNotificationPayload(notificationPayload, false).continueWithTask(new Continuation<GroupSelfie, Task<Void>>() {
            @Override
            public Task<Void> then(Task<GroupSelfie> task) throws Exception {

                if (!task.isFaulted() && !task.isCancelled()) {

                    final GroupSelfie groupSelfie = task.getResult();

                    GroupSelfieActivity.setStaticGroupSelfie(groupSelfie);
                    GroupSelfieActivity.getStaticGroupSelfieAdpater().loadCache().continueWithTask(new Continuation<Void, Task<Void>>() {
                        @Override
                        public Task<Void> then(Task<Void> task) throws Exception {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Intent intent = new Intent(MainActivity.this, GroupSelfieActivity.class);
                                    startActivity(intent);
                                }
                            });
                            return null;
                        }
                    });

                }

                return null;
            }
        });

    }

    public void shouldStartActivityForActivityNotificationPayload(final JSONObject notificationPayload) {

        try {

            GroupSelfie.groupSelfieWithId_loadIfNeeded(notificationPayload.getString(Constants.KEY_PUSHPAYLOAD_TO_GROUPSELFIE_ID), true).continueWithTask(new Continuation<GroupSelfie, Task<Object>>() {
                @Override
                public Task<Object> then(Task<GroupSelfie> task) throws Exception {
                    if (!task.isFaulted() && !task.isCancelled()) {
                        GroupSelfie groupSelfie = task.getResult();

                        // Don't need to update groupSelfie, already handled by notification, even when app not running

                        GroupSelfieActivity.setStaticGroupSelfie(groupSelfie);
                        GroupSelfieActivity.getStaticGroupSelfieAdpater().loadCache().continueWithTask(new Continuation<Void, Task<Void>>() {
                            @Override
                            public Task<Void> then(Task<Void> task) throws Exception {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Intent intent = new Intent(MainActivity.this, GroupSelfieActivity.class);
                                        startActivity(intent);
                                    }
                                });
                                return null;
                            }
                        });

                    }
                    return null;
                }
            });

        } catch(JSONException e) {

        }

    }

}
