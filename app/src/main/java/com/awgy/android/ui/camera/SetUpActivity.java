package com.awgy.android.ui.camera;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.awgy.android.R;
import com.awgy.android.adapters.RecipientsAdapter;
import com.awgy.android.models.GroupSelfie;
import com.awgy.android.models.Relationship;
import com.awgy.android.ui.main.MainActivity;
import com.awgy.android.utils.BaseActivity;
import com.awgy.android.utils.ClassAdapter;
import com.awgy.android.utils.Constants;
import com.google.common.base.Joiner;
import com.parse.ParseUser;

import java.lang.ref.WeakReference;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class SetUpActivity extends BaseActivity implements ClassAdapter.ClassAdapterDelegate {

    private static RecipientsAdapter staticRecipientsAdpater = new RecipientsAdapter(null);
    public static RecipientsAdapter getStaticRecipientsAdpater() {
        return staticRecipientsAdpater;
    }

    private static GroupSelfie staticGroupSelfie;
    private GroupSelfie mGroupSelfie;
    public static void setStaticGroupSelfie(GroupSelfie groupSelfie) {
        staticGroupSelfie = groupSelfie;
    }

    private static Bitmap staticBitmap;
    private Bitmap mBitmap;
    public static void setStaticBitmap(Bitmap bitmap) {
        staticBitmap = bitmap;
    }

    private EditText mHashtagField;
    private EditText mDetailsField;
    private Spinner mSpinner;
    private final Double[] mDurations = {300.0,1800.0,3600.0};


    private String mHashtag;
    private String mDetails;
    private List<String> mGroupIds;
    private List<String> mGroupUsernames;
    private List<String> mWarnings;

    private RecyclerView mRecyclerView;

    private static String mNeedShowMessage = null;
    public static void setNeedShowMessage(String message) {
        mNeedShowMessage = message;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_up);

        mGroupIds = new ArrayList<String>();
        mGroupUsernames = new ArrayList<String>();
        mWarnings = new ArrayList<String>();

        mHashtagField = (EditText) findViewById(R.id.hashtagField);
        mHashtagField.addTextChangedListener(mHashtagListener);

        mDetailsField = (EditText) findViewById(R.id.detailsField);
        mDetailsField.addTextChangedListener(mDetailsListener);

        setUpKeyboard(findViewById(R.id.mainLinearLayout));

        mSpinner = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.choices_array, R.layout.spinner_item);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        mSpinner.setAdapter(adapter);
        mSpinner.setSelection(1);

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        mRecyclerView.setHasFixedSize(true);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);

        // Initialize

        if (staticRecipientsAdpater != null) {
            staticRecipientsAdpater.setActivity(this);
            staticRecipientsAdpater.setDelegate(this);

            staticRecipientsAdpater.setWeakRecyclerView(new WeakReference<RecyclerView>(mRecyclerView));
            mRecyclerView.setAdapter(staticRecipientsAdpater);
            staticRecipientsAdpater.checkProgressBarStatus();
            staticRecipientsAdpater.refreshEmptyView();
        }

        if (staticGroupSelfie != null) {
            mGroupSelfie = staticGroupSelfie;
            staticGroupSelfie = null;

            // Hashtag
            mHashtagField.setText(mGroupSelfie.getHashtag());

            // Duration
            if (mGroupSelfie.getDuration() != null) {
                Integer index = Arrays.asList(mDurations).indexOf(mGroupSelfie.getDuration());
                if (index != -1 && index < mDurations.length){
                    mSpinner.setSelection(index);
                } else {
                    mSpinner.setSelection(1);
                }
            }

            // Details
            if (mGroupSelfie.getDetails() != null) mDetailsField.setText(mGroupSelfie.getDetails());

            // Group
            if (mGroupSelfie.getGroupUsernames() != null) {
                staticRecipientsAdpater.initialize(mGroupSelfie.getGroupUsernames());
            }
        }

        if (staticBitmap != null) {
            mBitmap = staticBitmap;
            staticBitmap = null;
        }

        // Notification

        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.registerReceiver(mBroadcastReceiver, new IntentFilter(Constants.PHONEBOOK_PHONEBOOK_HAS_BEEN_UPDATED_NOTIFICATION));

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mNeedShowMessage != null) {

            String[] components = mNeedShowMessage.split("\n");

            AlertDialog.Builder builder = new AlertDialog.Builder(SetUpActivity.this);

            if (components.length > 1) {

                String[] body_message = Arrays.copyOfRange(components, 1, components.length);

                builder.setMessage(Joiner.on("\n").join(body_message))
                        .setTitle(components[0])
                        .setPositiveButton(android.R.string.ok, null);

            } else {

                builder.setMessage(mNeedShowMessage)
                        .setTitle(getResources().getString(R.string.oops))
                        .setPositiveButton(android.R.string.ok, null);

            }

            AlertDialog dialog = builder.create();
            dialog.show();

            mNeedShowMessage = null;
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Notification
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.unregisterReceiver(mBroadcastReceiver);

        if (staticRecipientsAdpater != null) {
            staticRecipientsAdpater.setActivity(null);
            staticRecipientsAdpater.setDelegate(null);
            staticRecipientsAdpater.clear();
            staticRecipientsAdpater.destroyEmptyView();
        }

    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        onBackAction();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_set_up, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch(item.getItemId()) {

            case android.R.id.home:

                onBackAction();
                return true;

            case R.id.action_send:

                sendActionButton();
                return true;

        }

        return false;
    }

    public void onBackAction() {

        mHashtagField.clearFocus();
        mDetailsField.clearFocus();

        processHashtag();
        processDetails();
        processGroups();

        if (mGroupSelfie == null) mGroupSelfie = new GroupSelfie();
        mGroupSelfie.setGroupUsernames(mGroupUsernames, true);
        mGroupSelfie.setDuration(mDurations[mSpinner.getSelectedItemPosition()], true);
        if (mHashtag != null) mGroupSelfie.setHashtag(mHashtag);
        if (mDetails != null) mGroupSelfie.setDetails(mDetails);

        CameraActivity.setStaticGroupSelfie(mGroupSelfie);

        staticRecipientsAdpater.getGroup().clear();
        finish();
        overridePendingTransition(0, 0);
    }

    public void processHashtag() {

        String hashtag = mHashtagField.getText().toString().trim();
        hashtag = Normalizer.normalize(hashtag, Normalizer.Form.NFD);
        hashtag = hashtag.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        mHashtag = hashtag.replaceAll("[^A-Za-z0-9 ]", "");
        if (mHashtag.length() == 0) mHashtag = null;

    }

    private TextWatcher mHashtagListener = new TextWatcher() {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) { }

        @Override
        public void afterTextChanged(Editable s) {
            String result = s.toString().replaceAll(" ", "");
            if (!s.toString().equals(result)) {
                mHashtagField.setText(result);
                mHashtagField.setSelection(result.length());
            }
        }
    };

    public void processDetails() {

        String details = mDetailsField.getText().toString().trim();
        mDetails = details.replaceAll("\n", " ").replaceAll("\r", " ");
        if (mDetails.length() == 0) mDetails = null;

    }

    private TextWatcher mDetailsListener = new TextWatcher() {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) { }

        @Override
        public void afterTextChanged(Editable s) {
            for(int i = s.length(); i > 0; i--) {
                if(s.subSequence(i-1, i).toString().equals("\n"))
                    s.replace(i-1, i, "");
            }
        }
    };

    public void processGroups() {

        mGroupIds.clear();
        mGroupUsernames.clear();
        mWarnings.clear();

        for (Relationship relationship : staticRecipientsAdpater.getGroup()) {
            mGroupIds.add(relationship.getToUserId());
            mGroupUsernames.add(relationship.getToUsername());
            if (relationship.getWarning() != null) mWarnings.add(relationship.getWarning());
        }

    }

    public void sendActionButton() {

        if (mHashtagField.hasFocus()) {
            mHashtagField.clearFocus();
        }

        if (mDetailsField.hasFocus()) {
            mDetailsField.clearFocus();
        }

        // Hashtag
        processHashtag();

        // Details
        processDetails();

        // Group
        processGroups();

        ParseUser user = ParseUser.getCurrentUser();
        if (!mGroupIds.contains(user.getObjectId())) {
            mGroupIds.add(user.getObjectId());
            mGroupUsernames.add(user.getUsername());
        }

        // Proceed

        if (mHashtag == null || mHashtag.length() < 3) {

            AlertDialog.Builder builder = new AlertDialog.Builder(SetUpActivity.this);
            builder.setMessage(getResources().getString(R.string.error_no_hashtag))
                    .setTitle(getResources().getString(R.string.oops))
                    .setPositiveButton(android.R.string.ok, null);
            AlertDialog dialog = builder.create();
            dialog.show();

        } else if (mGroupUsernames.size() <= 1) {

            AlertDialog.Builder builder = new AlertDialog.Builder(SetUpActivity.this);
            builder.setMessage(getResources().getString(R.string.error_no_recipients))
                    .setTitle(getResources().getString(R.string.oops))
                    .setPositiveButton(android.R.string.ok, null);
            AlertDialog dialog = builder.create();
            dialog.show();

        } else if (isNetworkAvailable()) {

            if (mWarnings.size() > 0) {
                showWarning();
            } else {
                createNewGroupSelfie();
            }

        }
    }

    public void showWarning() {

        AlertDialog.Builder builder = new AlertDialog.Builder(SetUpActivity.this);
        builder.setMessage(mWarnings.get(0))
                .setTitle(getResources().getString(R.string.warning))
                .setPositiveButton(getResources().getString(R.string.accept), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        if (mWarnings.size() > 0) {
                            showWarning();
                        } else {
                            createNewGroupSelfie();
                        }

                    }
                })
                .setNegativeButton(getResources().getString(R.string.cancel), null);
        mWarnings.remove(0);
        AlertDialog dialog = builder.create();
        dialog.show();

    }

    public void createNewGroupSelfie() {

        mGroupSelfie = new GroupSelfie();
        mGroupSelfie.setGroupUsernames(mGroupUsernames, true);
        mGroupSelfie.setGroupIds(mGroupIds, true);
        mGroupSelfie.setDuration(mDurations[mSpinner.getSelectedItemPosition()], true);
        mGroupSelfie.setHashtag(mHashtag);
        if (mDetails!= null) mGroupSelfie.setDetails(mDetails);
        List<String> seenIds = new ArrayList<String>();
        seenIds.add(ParseUser.getCurrentUser().getObjectId());
        mGroupSelfie.setSeenIds(seenIds, true);

        if (mDelegate != null) {
            mDelegate.onSetUpSendButtonClick(mGroupSelfie, mBitmap);
        }

        // Dismiss

        MainActivity.setTabToSelectOnResume(1);
        Intent intent = new Intent(SetUpActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);

    }

    // Keyboard

    public void setUpKeyboard(View view) {
        if(!(view instanceof EditText)) {
            view.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    hideKeyboard(v);
                    return false;
                }
            });
        }
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View innerView = ((ViewGroup) view).getChildAt(i);
                setUpKeyboard(innerView);
            }
        }
    }

    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        if (mHashtagField.hasFocus()) {
            mHashtagField.clearFocus();
        }
        if (mDetailsField.hasFocus()) {
            mDetailsField.clearFocus();
        }
    }

    // Notification

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (staticRecipientsAdpater != null) staticRecipientsAdpater.notifyDataSetChanged();
        }
    };

    @Override
    public void onEmptyButtonClick() {

    }

    // Delegate

    public interface SetUpActivityDelegate {
        void onSetUpSendButtonClick(GroupSelfie groupSelfie, Bitmap bitmap);
    }

    private static SetUpActivity.SetUpActivityDelegate mDelegate;

    public static void setDelegate(SetUpActivity.SetUpActivityDelegate delegate) {
        mDelegate = delegate;
    }

    // Network

    private boolean isNetworkAvailable() {

        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        boolean available = activeNetworkInfo != null && activeNetworkInfo.isConnected();

        if (!available) {
            AlertDialog.Builder builder = new AlertDialog.Builder(SetUpActivity.this);
            builder.setMessage(getResources().getString(R.string.no_internet))
                    .setTitle(getResources().getString(R.string.oops))
                    .setPositiveButton(android.R.string.ok, null);
            AlertDialog dialog = builder.create();
            dialog.show();
        }

        return available;

    }

}
