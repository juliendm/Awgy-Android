package com.awgy.android.ui.login;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import com.awgy.android.R;
import com.awgy.android.ui.main.MainActivity;
import com.awgy.android.ui.stream.StreamFragment;
import com.awgy.android.utils.BaseActivity;
import com.awgy.android.utils.Constants;
import com.awgy.android.utils.PinsOnFile;
import com.parse.FunctionCallback;
import com.parse.LogInCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.HashMap;
import java.util.Random;

public class VerificationActivity extends BaseActivity {

	private EditText mVerificationCodeField;
	private Button mButton;

    private boolean mAlreadyLogIn;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_verification);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setCustomView(R.layout.main_action_bar);
        }

        mAlreadyLogIn = false;

        Bundle extras = getIntent().getExtras();
        String phoneNumberExtra = "";
        if (extras != null) {
            if (extras.containsKey(Constants.KEY_VERIFICATION_FUNCTION_PHONENUMBER)) {
                phoneNumberExtra = extras.getString(Constants.KEY_VERIFICATION_FUNCTION_PHONENUMBER);
            }
            if (extras.containsKey(Constants.KEY_ALREADY_LOG_IN)) {
                mAlreadyLogIn = extras.getBoolean(Constants.KEY_ALREADY_LOG_IN);
            }
        }
        final String phoneNumber = phoneNumberExtra;

		mVerificationCodeField = (EditText) findViewById(R.id.verificationCodeField);

		setUpKeyboard(findViewById(R.id.mainRelativeLayout));

		mButton = (Button) findViewById(R.id.button);
		mButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

                String verifCode = mVerificationCodeField.getText().toString().trim().replaceAll("[^A-Za-z0-9 ]", "");

                if (verifCode.length() == 0) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(VerificationActivity.this);
                    builder.setMessage(getResources().getString(R.string.error_no_code))
                            .setTitle(getResources().getString(R.string.oops))
                            .setPositiveButton(android.R.string.ok, null);
                    AlertDialog dialog = builder.create();
                    dialog.show();

                } else {

                    final ProgressDialog progressDialog = ProgressDialog.show(VerificationActivity.this, null, getResources().getString(R.string.please_wait));

                    Random generator = new Random();
                    StringBuilder randomStringBuilder = new StringBuilder(60);
                    String letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
                    for (int i = 0; i < 60; i++){
                        randomStringBuilder.append(letters.charAt(generator.nextInt(letters.length())));
                    }
                    final String password = randomStringBuilder.toString();

                    HashMap<String, Object> params = new HashMap<String, Object>();
                    params.put(Constants.KEY_USER_FUNCTION_USERNAME, phoneNumber);
                    params.put(Constants.KEY_USER_FUNCTION_PASSWORD, password);
                    params.put(Constants.KEY_USER_FUNCTION_VERIF_CODE, verifCode);

                    ParseCloud.callFunctionInBackground(Constants.KEY_USER_FUNCTION, params, new FunctionCallback<Object>() {
                        @Override
                        public void done(Object o, ParseException e) {

                            if (e != null) {

                                progressDialog.dismiss();

                                String message = e.getMessage();
                                boolean needAddTag = message.split("\n")[0].equals("WRONG");
                                if (needAddTag) message = message.split("\n")[1];
                                AlertDialog.Builder builder = new AlertDialog.Builder(VerificationActivity.this);
                                builder.setMessage(message)
                                        .setTitle(getResources().getString(R.string.oops));
                                if (needAddTag) {
                                    builder.setPositiveButton(android.R.string.ok, null);
                                } else {
                                    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            onBackAction();
                                        }
                                    });
                                }
                                AlertDialog dialog = builder.create();
                                dialog.show();

                            } else {

                                if (ParseUser.getCurrentUser() != null) {

                                    progressDialog.dismiss();

                                    ParseUser.getCurrentUser().setUsername(phoneNumber);
                                    finish();
                                    overridePendingTransition(R.anim.right_in, R.anim.right_out);

                                } else {

                                    ParseUser.logInInBackground(phoneNumber, password, new LogInCallback() {
                                        @Override
                                        public void done(ParseUser parseUser, ParseException e) {

                                            if (e != null) {

                                                progressDialog.dismiss();

                                                AlertDialog.Builder builder = new AlertDialog.Builder(VerificationActivity.this);
                                                builder.setMessage(e.getMessage())
                                                        .setTitle(getResources().getString(R.string.oops))
                                                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {
                                                                onBackAction();
                                                            }
                                                        });
                                                AlertDialog dialog = builder.create();
                                                dialog.show();

                                            } else {

                                                // Associate the device with a user
                                                ParseInstallation installation = ParseInstallation.getCurrentInstallation();
                                                installation.put(Constants.KEY_INSTALLATION_USER, parseUser);
                                                installation.saveInBackground(new SaveCallback() {
                                                    @Override
                                                    public void done(ParseException e) {

                                                        progressDialog.dismiss();

                                                        if (e != null) {

                                                            ParseUser.logOut();

                                                            AlertDialog.Builder builder = new AlertDialog.Builder(VerificationActivity.this);
                                                            builder.setMessage(getResources().getString(R.string.error_reinstall))
                                                                    .setTitle(getResources().getString(R.string.oops))
                                                                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                                                        @Override
                                                                        public void onClick(DialogInterface dialog, int which) {
                                                                            onBackAction();
                                                                        }
                                                                    });
                                                            AlertDialog dialog = builder.create();
                                                            dialog.show();

                                                        } else {

                                                            PinsOnFile.getInstance().initiate();

                                                            SharedPreferences.Editor editor = getSharedPreferences(Constants.KEY_SHARED_PREFERENCES_USERNAME, Context.MODE_PRIVATE).edit();
                                                            editor.putBoolean(Constants.KEY_SHARED_PREFERENCES_VERIFYING, false);
                                                            editor.apply();

                                                            Intent intent = new Intent(VerificationActivity.this, MainActivity.class);
                                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                            startActivity(intent);
                                                        }

                                                    }
                                                });
                                            }
                                        }
                                    });
                                }
                            }
                        }
                    });
                }
			}
		});

        // Notification
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.registerReceiver(mVerificationCodeHasBeenReceivedNotification, new IntentFilter(Constants.VERIFICATION_CODE_HAS_BEEN_RECEIVED_NOTIFICATION));

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Notification
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.unregisterReceiver(mVerificationCodeHasBeenReceivedNotification);

    }

    private BroadcastReceiver mVerificationCodeHasBeenReceivedNotification = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                if (bundle.containsKey(Constants.VERIFICATION_CODE)) {
                    String code = bundle.getString(Constants.VERIFICATION_CODE);
                    mVerificationCodeField.setText(code);
                }
            }

        }
    };

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		onBackAction();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);

		switch(item.getItemId()) {
			case android.R.id.home:
				onBackAction();
				return true;
		}

		return false;
	}

    public void onBackAction() {

        if (mAlreadyLogIn) {

            finish();
            overridePendingTransition(R.anim.right_in, R.anim.right_out);

        } else {

            SharedPreferences.Editor editor = getSharedPreferences(Constants.KEY_SHARED_PREFERENCES_USERNAME, Context.MODE_PRIVATE).edit();
            editor.putBoolean(Constants.KEY_SHARED_PREFERENCES_VERIFYING, false);
            editor.apply();

            Intent intent = new Intent(VerificationActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            overridePendingTransition(R.anim.right_in, R.anim.right_out);

        }

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
		if (mVerificationCodeField.hasFocus()) {
			mVerificationCodeField.clearFocus();
		}
	}

}
