package com.awgy.android.ui.stream;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.awgy.android.adapters.GroupSelfieAdapter;

import com.awgy.android.R;
import com.awgy.android.models.CountDown;
import com.awgy.android.models.GroupSelfie;
import com.awgy.android.models.Activity;
import com.awgy.android.models.PhoneBook;
import com.awgy.android.ui.camera.CameraActivity;
import com.awgy.android.ui.main.MainActivity;
import com.awgy.android.utils.BaseActivity;
import com.awgy.android.utils.Constants;
import com.awgy.android.utils.NeedNetwork;
import com.awgy.android.utils.PushBroadcastReceiver;
import com.awgy.android.views.PlaceHolderDrawable;
import com.google.common.base.Joiner;
import com.parse.ParseFile;
import com.parse.ParseImageView;
import com.parse.ParseUser;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import bolts.Continuation;
import bolts.Task;

public class GroupSelfieActivity extends BaseActivity implements View.OnFocusChangeListener {

    private boolean mIsShowing = false;

    private static GroupSelfie staticGroupSelfie;
    private GroupSelfie mGroupSelfie;
    private static GroupSelfieAdapter staticGroupSelfieAdpater;
    public static GroupSelfieAdapter getStaticGroupSelfieAdpater() {
        return staticGroupSelfieAdpater;
    }

    public static void setStaticGroupSelfie(GroupSelfie groupSelfie) {
        staticGroupSelfie = groupSelfie;
        staticGroupSelfieAdpater = new GroupSelfieAdapter(null, groupSelfie);
    }

    private static final int CONTACT_RESULT = 9;

    private List<String> mGroupIds;
    private List<String> mGroupUsernames;

    private ParseImageView mParseImageView;
    private RelativeLayout mParseImageViewRelativeLayout;

    private RecyclerView mRecyclerView;
    private View mCustomActionBarView;

    private TextView mDetailsTextView;
    private TextView mNamesTextView;
    private EditText mMessageField;

    private ImageButton mLoveButton;

    private Timer mTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.onCreate(savedInstanceState);

        //if (getSupportActionBar() != null) {
        //    getSupportActionBar().hide();
        //}

        if (staticGroupSelfieAdpater != null && staticGroupSelfieAdpater.getActivity() != null) {
            staticGroupSelfieAdpater.setNeedRemoveProgressBar(true);
        }

        setContentView(R.layout.activity_groupselfie);
        mParseImageView = (ParseImageView) findViewById(R.id.parseImageView);
        mParseImageView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mParseImageView.getDrawable() != null && !(mParseImageView.getDrawable() instanceof PlaceHolderDrawable)) {
                    ImageActivity.setStaticGroupSelfie(mGroupSelfie);
                    ImageActivity.setStaticBitmap(processBitmap(((BitmapDrawable)mParseImageView.getDrawable()).getBitmap(),mGroupSelfie));
                    Intent intent = new Intent(GroupSelfieActivity.this, ImageActivity.class);
                    startActivity(intent);
                }
            }
        });
        mParseImageViewRelativeLayout = (RelativeLayout) findViewById(R.id.parseImageViewRelativeLayout);

        mGroupIds = new ArrayList<String>();
        mGroupUsernames = new ArrayList<String>();

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        mRecyclerView.setHasFixedSize(true);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);

        mCustomActionBarView = getLayoutInflater().inflate(R.layout.groupselfie_action_bar, null);

        mMessageField = (EditText) findViewById(R.id.messageField);
        mMessageField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    mRecyclerView.scrollToPosition(staticGroupSelfieAdpater.getObjects().size() - 1);
                } catch (Exception e) {

                }
            }
        });
        mMessageField.setOnFocusChangeListener(this);

        mLoveButton = (ImageButton) findViewById(R.id.loveButton);
        mLoveButton.setOnClickListener(mDidTapLoveButton);

        Button sendButton = (Button) findViewById(R.id.sendButton);
        sendButton.setOnClickListener(mDidTapSendButton);

        mRecyclerView.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                hideKeyboard(v);
                return false;
            }
        });

        mDetailsTextView = (TextView) findViewById(R.id.detailsTextView);
        mNamesTextView = (TextView) findViewById(R.id.namesTextView);

        PushBroadcastReceiver.addPushBroadcastReceiverListener(mPushBroadcastReceiverListener);

        if (staticGroupSelfie != null) {
            setGroupSelfie(staticGroupSelfie);
            staticGroupSelfie = null;
        }

    }

    @Override
    protected void onDestroy(){
        super.onDestroy();

        if (mGroupSelfie != null) {
            staticGroupSelfie = mGroupSelfie;

            if (mGroupSelfie.getCountDown() != null) {
                mGroupSelfie.getCountDown().removeCountDownListener(mCountDownListener);
            }

        }

        PushBroadcastReceiver.removePushBroadcastReceiverListener(mPushBroadcastReceiverListener);

    }

    @Override
    protected void onResume() {
        super.onResume();

        mIsShowing = true;
        if (mGroupSelfie != null) {
            if (mGroupSelfie.getSeenIds() == null || !mGroupSelfie.getSeenIds().contains(ParseUser.getCurrentUser().getObjectId())) {
                mGroupSelfie.addSeenId(true, true);
            }
            PushBroadcastReceiver.setBusyWithGroupSelfieId(mGroupSelfie.getObjectId());
        }

        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateHashtagView();
                    }
                });
            }
        }, 30000, 30000);

    }

    @Override
    protected void onPause() {
        super.onPause();

        mIsShowing = false;
        PushBroadcastReceiver.setBusyWithGroupSelfieId(null);

        if (mTimer != null) {
            mTimer.cancel();
        }

    }

    public void setGroupSelfie(GroupSelfie groupSelfie) {

        mGroupSelfie = groupSelfie;

        // Size

        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        int border = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, getResources().getDisplayMetrics());
        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, getResources().getDisplayMetrics());

        mParseImageViewRelativeLayout.getLayoutParams().width = size.x - 2 * border - 2 * margin;
        mParseImageViewRelativeLayout.getLayoutParams().height = Math.min((int) Math.ceil(mParseImageViewRelativeLayout.getLayoutParams().width / mGroupSelfie.getImageRatio()), (int) Math.ceil(0.35 * size.y));

        // PlaceHolder

        PlaceHolderDrawable placeHolderDrawable = new PlaceHolderDrawable(this, mGroupSelfie);
        mParseImageView.setPlaceholder(placeHolderDrawable);

        // Adapter

        if (staticGroupSelfieAdpater != null) {
            staticGroupSelfieAdpater.setActivity(this);
            if (mRecyclerView != null) {
                mRecyclerView.setAdapter(staticGroupSelfieAdpater);
                staticGroupSelfieAdpater.setWeakRecyclerView(new WeakReference<RecyclerView>(mRecyclerView));
                try {
                    mRecyclerView.scrollToPosition(staticGroupSelfieAdpater.getObjects().size() - 1);
                } catch (Exception e) {

                }
            }
            staticGroupSelfieAdpater.checkProgressBarStatus();
            staticGroupSelfieAdpater.refreshEmptyView();
        }

        // GroupSelfie

        if (groupSelfie.getCountDown() != null) {
            mGroupSelfie.getCountDown().addCountDownListener(mCountDownListener);
        }

        ParseFile imageFile = mGroupSelfie.getImage();
        if (imageFile != null) {
            mParseImageView.setParseFile(imageFile);
            mParseImageView.loadInBackground();
        }

        if (mGroupSelfie.getSeenIds() == null || !mGroupSelfie.getSeenIds().contains(ParseUser.getCurrentUser().getObjectId())) {
            mGroupSelfie.addSeenId(true, true);
        }

        // Hashtag

        mCustomActionBarView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        updateHashtagView();

        // Details

        if (groupSelfie.getDetails() != null) {
            mDetailsTextView.setText(groupSelfie.getDetails());
        } else {
            mDetailsTextView.setText(null);
            mDetailsTextView.setPadding(0, 0, 0, 0);
            mDetailsTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 0);
        }

        // Love Button

        if (groupSelfie.getLoveIds() != null && groupSelfie.getLoveIds().contains(ParseUser.getCurrentUser().getObjectId())) {
            mLoveButton.setImageResource(R.drawable.heart);
        } else {
            mLoveButton.setImageResource(R.drawable.heart_empty);
        }

        // Refresh Names

        refreshNames(groupSelfie);
    }

    public void refreshNames(GroupSelfie groupSelfie) {

        mGroupIds.clear();
        String currentUserId = ParseUser.getCurrentUser().getObjectId();
        for (String userId : mGroupSelfie.getGroupIds()) {
            if (!(userId.equals(currentUserId))) {
                mGroupIds.add(userId);
            }
        }

        mGroupUsernames.clear();
        String currentUsername = ParseUser.getCurrentUser().getUsername();
        for (String username : mGroupSelfie.getGroupUsernames()) {
            if (!(username.equals(currentUsername))) {
                mGroupUsernames.add(username);
            }
        }

        List<String> names = new ArrayList<String>();

        if (mGroupIds.size() > 0) {

            for (int index = 0; index < mGroupUsernames.size(); index++) {
                String name = PhoneBook.getInstance().getFirstName(mGroupUsernames.get(index));
                if (index < mGroupIds.size()) {
                    if (groupSelfie.getLoveIds() != null && groupSelfie.getLoveIds().contains(mGroupIds.get(index))) {
                        name = name + " ♥";
                    } else if (groupSelfie.getDiscoveredIds() != null && groupSelfie.getDiscoveredIds().contains(mGroupIds.get(index))) {
                        name = name + " ✔";
                    }
                }
                if (index < mGroupUsernames.size() - 1) {
                    name = name + ",";
                }

                names.add(name);
            }

        } else {
            names.add(getResources().getString(R.string.only_you));
        }



        // Guests

        String string = Joiner.on(" ").join(names);
        StringBuilder stringBuilder = new StringBuilder(string);
        SpannableString spannableString = new SpannableString(string);

        int imageSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());

        while (stringBuilder.indexOf("♥") > -1) {
            int index = stringBuilder.indexOf("♥");
            Drawable heartDrawable = getResources().getDrawable(R.drawable.heart);
            if (heartDrawable != null) heartDrawable.setBounds(0, 0, imageSize, imageSize);
            ImageSpan heart = new ImageSpan(heartDrawable, ImageSpan.ALIGN_BASELINE);
            spannableString.setSpan(heart, index, index+1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            stringBuilder.setCharAt(index, ' ');
        }

        while (stringBuilder.indexOf("✔") > -1) {
            int index = stringBuilder.indexOf("✔");
            Drawable checkDrawable = getResources().getDrawable(R.drawable.check_discovered);
            if (checkDrawable != null) checkDrawable.setBounds(0, 0, imageSize, imageSize);
            ImageSpan check = new ImageSpan(checkDrawable, ImageSpan.ALIGN_BASELINE);
            spannableString.setSpan(check, index, index+1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            stringBuilder.setCharAt(index, ' ');
        }

        int begin;
        int end = -1;
        for (int index = 0; index < names.size(); index++) {
            begin = end + 1;
            end = begin + names.get(index).length();
            final int position = index;
            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(View textView) {
                    if (mGroupIds.size() > 0) {

                        try {

                            onRemaneAction(mGroupUsernames.get(position));

                        } catch (ArrayIndexOutOfBoundsException e) {

                        }
                    }
                }

                @Override
                public void updateDrawState(TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(false);
                    ds.setColor(getResources().getColor(R.color.color2));
                }
            };
            spannableString.setSpan(clickableSpan, begin, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }


        mNamesTextView.setText(spannableString);
        mNamesTextView.setMovementMethod(LinkMovementMethod.getInstance());
        mNamesTextView.setHighlightColor(Color.TRANSPARENT);

    }

    public void onRemaneAction(String username) {

        if (isNumeric(username)) {

            PhoneBook phoneBook = PhoneBook.getInstance();

            String key = phoneBook.getKey(username);

            Intent intent;

            if (key != null) {

                intent = new Intent(Intent.ACTION_EDIT);
                Uri selectedContactUri = ContactsContract.Contacts.getLookupUri(phoneBook.getId(username), key);
                intent.setDataAndType(selectedContactUri, ContactsContract.Contacts.CONTENT_ITEM_TYPE);

            } else {

                intent = new Intent(ContactsContract.Intents.Insert.ACTION);
                intent.setType(ContactsContract.RawContacts.CONTENT_TYPE);
                intent.putExtra(ContactsContract.Intents.Insert.PHONE, String.format("+%s", username))
                        .putExtra(ContactsContract.Intents.Insert.PHONE_TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE);

            }

            intent.putExtra("finishActivityOnSaveCompleted", true);
            startActivityForResult(intent, CONTACT_RESULT);

        }

    }

    public static boolean isNumeric(String str) {
        try {
            double d = Double.parseDouble(str);
        } catch(NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == CONTACT_RESULT && resultCode == RESULT_OK) {
            PhoneBook.getInstance().fetchPhoneBook();
        }

    }

    View.OnClickListener mDidTapLoveButton = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (isNetworkAvailable()) {

                mLoveButton.setEnabled(false);

                if (mGroupSelfie.getLoveIds() != null && mGroupSelfie.getLoveIds().contains(ParseUser.getCurrentUser().getObjectId())) {
                    mLoveButton.setImageResource(R.drawable.heart_empty);
                    mGroupSelfie.removeLoveId(true).continueWithTask(new Continuation<Void, Task<Void>>() {
                        @Override
                        public Task<Void> then(final Task<Void> task) throws Exception {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (task.isFaulted() || task.isCancelled()) {
                                        mLoveButton.setImageResource(R.drawable.heart);
                                        mGroupSelfie.addLoveId(false);
                                    }
                                    mLoveButton.setEnabled(true);
                                }
                            });
                            return null;
                        }
                    });
                } else {
                    mLoveButton.setImageResource(R.drawable.heart);
                    mGroupSelfie.addLoveId(true).continueWithTask(new Continuation<Void, Task<Void>>() {
                        @Override
                        public Task<Void> then(final Task<Void> task) throws Exception {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (task.isFaulted() || task.isCancelled()) {
                                        mLoveButton.setImageResource(R.drawable.heart_empty);
                                        mGroupSelfie.addLoveId(false);
                                    }
                                    mLoveButton.setEnabled(true);
                                }
                            });
                            return null;
                        }
                    });
                }

            }

        }
    };

    View.OnClickListener mDidTapSendButton = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (mGroupSelfie.getGroupIds().size() > 1) {

                if (isNetworkAvailable()) {

                    String trimmedMessage = mMessageField.getText().toString().trim();

                    if (trimmedMessage.length() > 0) {

                        final Activity message = new Activity();
                        message.setContent(trimmedMessage);
                        message.setToGroupSelfieId(mGroupSelfie.getObjectId());
                        message.setType(Constants.KEY_ACTIVITY_TYPE_COMMENTED);

                        staticGroupSelfieAdpater.getObjects().add(0, message);
                        staticGroupSelfieAdpater.notifyDataSetChanged();

                        message.saveMessageToGroupSelfie(mGroupSelfie).continueWithTask(new Continuation<Void, Task<Void>>() {
                            @Override
                            public Task<Void> then(Task<Void> task) throws Exception {
                                if (!task.isFaulted() && !task.isCancelled()) {
                                    staticGroupSelfieAdpater.loadCache();
                                } else {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(GroupSelfieActivity.this);
                                    builder.setMessage(getResources().getString(R.string.try_again))
                                            .setTitle(getResources().getString(R.string.error))
                                            .setPositiveButton(android.R.string.ok, null);
                                    AlertDialog dialog = builder.create();
                                    dialog.show();
                                }
                                return null;
                            }
                        });

                    }

                    mMessageField.setText(null);
                    try {
                        mRecyclerView.scrollToPosition(staticGroupSelfieAdpater.getObjects().size() - 1);
                    } catch (Exception e) {

                    }

                }

            } else {

                AlertDialog.Builder builder = new AlertDialog.Builder(GroupSelfieActivity.this);
                builder.setMessage(getResources().getString(R.string.speaking_alone))
                        .setTitle(getResources().getString(R.string.oops))
                        .setPositiveButton(android.R.string.ok, null);
                AlertDialog dialog = builder.create();
                dialog.show();

            }

        }
    };

    private void updateHashtagView() {

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {

            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayShowCustomEnabled(true);

            TextView customTitle = (TextView) mCustomActionBarView.findViewById(R.id.action_bar_title);
            customTitle.setText(String.format("#%s", mGroupSelfie.getHashtag()));
            TextView customSubtitle = (TextView) mCustomActionBarView.findViewById(R.id.action_bar_subtitle);

            String pictureConvertedDate;
            if (mGroupSelfie.getLocalCreatedAt() != null) {
                pictureConvertedDate = formatter(mGroupSelfie.getLocalCreatedAt().getTime() + mGroupSelfie.getDuration().intValue() * 1000);
            } else {
                pictureConvertedDate = getResources().getString(R.string.less_than_a_minute_ago);
            }
            customSubtitle.setText(pictureConvertedDate);
            actionBar.setCustomView(mCustomActionBarView);

        }

    }

    public String formatter(long date) {

        long interval = (new Date()).getTime()-date;

        if (interval < 60*1000) {
            return getResources().getString(R.string.less_than_a_minute_ago);
        }

        String format;
        Calendar calendarDate = Calendar.getInstance();
        calendarDate.setTimeInMillis(date);
        Calendar calendarNow = Calendar.getInstance();

        if (calendarNow.get(Calendar.YEAR) != calendarDate.get(Calendar.YEAR)) {
            format = "d MMM yyyy hh:mm a";
        } else if (interval >= 3600*1000*24*7) {
            format = "EEE d MMM hh:mm a";
        } else if (calendarNow.get(Calendar.DATE) - calendarDate.get(Calendar.DATE) == 1 && interval >= 3600*1000*24) {
            format = "hh:mm a";
            DateFormat df = new SimpleDateFormat(format);
            String convertedDate = df.format(date);
            return getResources().getString(R.string.yesterday_at) + " "  + convertedDate;
        } else if (interval >= 3600*1000*24) {
            format = "EEEE hh:mm a";
        } else {
            String convertedDate = DateUtils.getRelativeTimeSpanString(
                    date,
                    new Date().getTime(),
                    DateUtils.SECOND_IN_MILLIS).toString();
            return convertedDate.substring(0, 1).toUpperCase()+convertedDate.substring(1);
        }

        DateFormat df = new SimpleDateFormat(format);
        String convertedDate = df.format(date);
        return convertedDate.substring(0, 1).toUpperCase()+convertedDate.substring(1);

    }

    private CountDown.CountDownListener mCountDownListener = new CountDown.CountDownListener() {
        @Override
        public void onUpdate() {}

        @Override
        public void onComplete() {}

        @Override
        public void onPing() {}

        @Override
        public void onCancel() {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setGroupSelfie(mGroupSelfie);
                }
            });

        }
    };

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        onBackAction();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_groupselfie, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch(item.getItemId()) {
            case android.R.id.home:
                onBackAction();
                return true;
            case R.id.action_retake:

                if (mGroupSelfie.getGroupIds().size() <= 1) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(GroupSelfieActivity.this);
                    builder.setMessage(getResources().getString(R.string.only_you))
                            .setTitle(getResources().getString(R.string.oops))
                            .setPositiveButton(android.R.string.ok, null);
                    AlertDialog dialog = builder.create();
                    dialog.show();
                } else {
                    CameraActivity.setNeedResetBitmap(true);
                    CameraActivity.setNeedResetGroupSelfie(false);
                    GroupSelfie groupSelfie = new GroupSelfie();
                    groupSelfie.setGroupUsernames(mGroupSelfie.getGroupUsernames(), true);
                    groupSelfie.setHashtag(mGroupSelfie.getHashtag());
                    CameraActivity.setStaticGroupSelfie(groupSelfie);
                    CameraActivity.setDelegate(MainActivity.getCameraDelegate());
                    Intent cameraActivityIntent = new Intent(GroupSelfieActivity.this, CameraActivity.class);
                    startActivity(cameraActivityIntent);
                }

                return true;
        }

        return false;
    }

    public void onBackAction() {

        finish();

        if (staticGroupSelfieAdpater != null) {
            staticGroupSelfieAdpater.setActivity(null);
            staticGroupSelfieAdpater.destroyEmptyView();
            staticGroupSelfieAdpater = null;
        }

    }

    private PushBroadcastReceiver.PushBroadcastReceiverListener mPushBroadcastReceiverListener = new PushBroadcastReceiver.PushBroadcastReceiverListener() {
        @Override
        public void onManageGroupSelfie(final GroupSelfie groupSelfie, boolean moveTop) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (groupSelfie.getImage() != null && mGroupSelfie.getObjectId().equals(groupSelfie.getObjectId())) {
                        setGroupSelfie(groupSelfie);
                    }
                }
            });
        }

        @Override
        public void onMessage(GroupSelfie groupSelfie) {

            if (groupSelfie.getImage() != null && mGroupSelfie.getObjectId().equals(groupSelfie.getObjectId())) {
                if ((mGroupSelfie.getSeenIds() == null || !mGroupSelfie.getSeenIds().contains(ParseUser.getCurrentUser().getObjectId())) && mIsShowing) {
                    mGroupSelfie.addSeenId(true, true).continueWithTask(new Continuation<Void, Task<Void>>() {
                        @Override
                        public Task<Void> then(Task<Void> task) throws Exception {
                            if (staticGroupSelfieAdpater != null) {
                                staticGroupSelfieAdpater.loadCache().continueWithTask(new Continuation<Void, Task<Void>>() {
                                    @Override
                                    public Task<Void> then(Task<Void> task) throws Exception {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                try {
                                                    mRecyclerView.scrollToPosition(staticGroupSelfieAdpater.getObjects().size() - 1);
                                                } catch (Exception e) {

                                                }
                                            }
                                        });
                                        return null;
                                    }
                                });
                            }
                            return null;
                        }
                    });
                } else {
                    if (staticGroupSelfieAdpater != null) {
                        staticGroupSelfieAdpater.loadCache().continueWithTask(new Continuation<Void, Task<Void>>() {
                            @Override
                            public Task<Void> then(Task<Void> task) throws Exception {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            mRecyclerView.scrollToPosition(staticGroupSelfieAdpater.getObjects().size() - 1);
                                        } catch (Exception e) {

                                        }
                                    }
                                });
                                return null;
                            }
                        });
                    }
                }

            }
        }

        @Override
        public void onDiscover(final GroupSelfie groupSelfie) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (groupSelfie.getImage() != null && mGroupSelfie.getObjectId().equals(groupSelfie.getObjectId())) {
                        refreshNames(groupSelfie);
                    }
                }
            });
        }

        @Override
        public void onLove(final GroupSelfie groupSelfie) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (groupSelfie.getImage() != null && mGroupSelfie.getObjectId().equals(groupSelfie.getObjectId())) {
                        refreshNames(groupSelfie);
                    }
                }
            });
        }

        @Override
        public void onRead(GroupSelfie groupSelfie) {
            if (groupSelfie.getImage() != null && mGroupSelfie.getObjectId().equals(groupSelfie.getObjectId())) {
                if (staticGroupSelfieAdpater != null) staticGroupSelfieAdpater.loadCache();
            }
        }

        @Override
        public void onDelete(GroupSelfie groupSelfie) {
            if (groupSelfie.getImage() != null && mGroupSelfie.getObjectId().equals(groupSelfie.getObjectId())) {
                NeedNetwork.getInstance().removeDone(GroupSelfie.keyWithGroupSelfieId(groupSelfie.getObjectId()));
                setGroupSelfie(mGroupSelfie);
            }
        }
    };

    public Bitmap processBitmap(Bitmap bmp, GroupSelfie groupSelfie) {

        // Strings
        String hashtag = String.format("#%s", groupSelfie.getHashtag());

        String format = "EEEE";
        DateFormat df = new SimpleDateFormat(format);
        String date1 = df.format(mGroupSelfie.getLocalCreatedAt().getTime() + mGroupSelfie.getDuration().intValue() * 1000);

        format = "MMMM d, yyyy, h:mm a";
        df = new SimpleDateFormat(format);
        String date2 = df.format(mGroupSelfie.getLocalCreatedAt().getTime() + mGroupSelfie.getDuration().intValue() * 1000);

        String date = date1.substring(0,1).toUpperCase() + date1.substring(1) + ", " + date2.substring(0,1).toUpperCase() + date2.substring(1);

        String credit = "Awgy.com";

        // Fonts
        float density = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());
        int topFontSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, (int) Math.ceil(bmp.getWidth()/density/15.0f), getResources().getDisplayMetrics());
        int bottomFontSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, (int) Math.ceil(bmp.getWidth()/density/30.0f), getResources().getDisplayMetrics());

        // Dimensions
        Paint topPaint = new Paint();
        topPaint.setStyle(Paint.Style.FILL);
        topPaint.setColor(getResources().getColor(R.color.color2));
        topPaint.setTextSize(topFontSize);
        topPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint bottomPaint = new Paint();
        bottomPaint.setStyle(Paint.Style.FILL);
        bottomPaint.setColor(getResources().getColor(R.color.color2));
        bottomPaint.setTextSize(bottomFontSize);

        Rect hashtagBounds = new Rect();
        topPaint.getTextBounds(hashtag, 0, hashtag.length(), hashtagBounds);

        Rect dateBounds = new Rect();
        bottomPaint.getTextBounds(date, 0, date.length(), dateBounds);

        Rect creditBounds = new Rect();
        bottomPaint.getTextBounds(credit, 0, credit.length(), creditBounds);

        int hashtagWidth = hashtagBounds.width();
        int creditWidth = creditBounds.width();

        int topHeight = (int) Math.ceil( Math.abs(topPaint.ascent()) + Math.abs(topPaint.descent()) ); //hashtagBounds.height();
        int bottomHeight = (int) Math.ceil( Math.abs(bottomPaint.ascent()) + Math.abs(bottomPaint.descent()) ); //dateBounds.height();

        int borderSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics());

        // New Bitmap
        Bitmap newBmp = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight() + topHeight + bottomHeight + 2*borderSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(newBmp);
        canvas.drawColor(Color.WHITE);

        // Image
        canvas.drawBitmap(bmp, 0, topHeight + borderSize, new Paint());

        // Hashtag
        canvas.drawText(hashtag, (int) Math.ceil(0.5f*(bmp.getWidth()-hashtagWidth)), borderSize + (int) Math.ceil(Math.abs(topPaint.ascent())), topPaint);

        // Date
        canvas.drawText(date, borderSize, bmp.getHeight() + topHeight + borderSize + (int) Math.ceil(Math.abs(bottomPaint.ascent())), bottomPaint);

        // Credit
        canvas.drawText(credit, bmp.getWidth()-creditWidth-borderSize, bmp.getHeight() + topHeight + borderSize + (int) Math.ceil(Math.abs(bottomPaint.ascent())), bottomPaint);

        return newBmp;

    }

    // Keyboard

    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(android.app.Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        if (mMessageField.hasFocus()) {
            mMessageField.clearFocus();
        }
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        try {
            mRecyclerView.scrollToPosition(staticGroupSelfieAdpater.getObjects().size() - 1);
        } catch (Exception e) {

        }
    }

    // Network

    private boolean isNetworkAvailable() {

        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        boolean available = activeNetworkInfo != null && activeNetworkInfo.isConnected();

        if (!available) {
            AlertDialog.Builder builder = new AlertDialog.Builder(GroupSelfieActivity.this);
            builder.setMessage(getResources().getString(R.string.no_internet))
                    .setTitle(getResources().getString(R.string.oops))
                    .setPositiveButton(android.R.string.ok, null);
            AlertDialog dialog = builder.create();
            dialog.show();
        }

        return available;

    }

}
