package com.awgy.android.ui.login;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import com.awgy.android.R;
import com.awgy.android.utils.BaseActivity;
import com.awgy.android.utils.Constants;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.parse.FunctionCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;

import java.util.HashMap;


public class LoginActivity extends BaseActivity {

	protected EditText mCountryCodeField;
	protected EditText mPhoneNumberField;
	protected Button mButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setCustomView(R.layout.main_action_bar);
        }
		
		mCountryCodeField = (EditText) findViewById(R.id.countryCodeField);
		mPhoneNumberField = (EditText) findViewById(R.id.phoneNumberField);

        setUpKeyboard(findViewById(R.id.mainRelativeLayout));

		mButton = (Button) findViewById(R.id.button);
		mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
                int countryCode = 0;
                String string_int = mCountryCodeField.getText().toString().trim().replaceAll("[^A-Za-z0-9 ]", "");
                if (string_int.length() > 0) {
                    countryCode = Integer.parseInt(string_int);
                }
                String regionCode = phoneUtil.getRegionCodeForCountryCode(countryCode);
                String nationalNumber = mPhoneNumberField.getText().toString().trim().replaceAll("[^A-Za-z0-9 ]", "");

                try {

                    Phonenumber.PhoneNumber number = phoneUtil.parse(nationalNumber, regionCode);
                    final String phoneNumber = phoneUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.E164).replaceAll("[^A-Za-z0-9 ]", "");

                    SharedPreferences.Editor editor = getSharedPreferences(Constants.KEY_SHARED_PREFERENCES_USERNAME, Context.MODE_PRIVATE).edit();
                    editor.clear();
                    editor.putString(Constants.KEY_SHARED_PREFERENCES_USERNAME, phoneNumber);
                    editor.putBoolean(Constants.KEY_SHARED_PREFERENCES_VERIFYING, false);
                    editor.apply();

                    Intent intent = new Intent(LoginActivity.this, AcceptTermsOfUseActivity.class);
                    intent.putExtra(Constants.KEY_VERIFICATION_FUNCTION_PHONENUMBER, phoneNumber);
                    intent.putExtra(Constants.KEY_ALREADY_LOG_IN, false);
                    startActivity(intent);
                    overridePendingTransition(R.anim.left_in, R.anim.left_out);

                } catch (NumberParseException e) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                    builder.setMessage(getResources().getString(R.string.error_phone_number))
                            .setTitle(getResources().getString(R.string.oops))
                            .setPositiveButton(android.R.string.ok, null);
                    AlertDialog dialog = builder.create();
                    dialog.show();

                }

            }
        });

        // Default values

        SharedPreferences sharedPreferences = getSharedPreferences(Constants.KEY_SHARED_PREFERENCES_USERNAME, Context.MODE_PRIVATE);
        String username = sharedPreferences.getString(Constants.KEY_SHARED_PREFERENCES_USERNAME, null);

        if (username != null) {
            try {
                PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
                Phonenumber.PhoneNumber number = phoneUtil.parse(String.format("+%s", username), "");
                mCountryCodeField.setText(number.getCountryCode()+"");
                mPhoneNumberField.setText(number.getNationalNumber()+"");
            } catch (NumberParseException e) {
                mCountryCodeField.setText("1");
                mPhoneNumberField.requestFocus();
            }
        } else {
            TelephonyManager mgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            String phoneNumber = mgr.getLine1Number();
            if (phoneNumber != null) {
                try {
                    PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
                    Phonenumber.PhoneNumber number = phoneUtil.parse(phoneNumber, "");
                    mCountryCodeField.setText(number.getCountryCode() + "");
                    mPhoneNumberField.setText(number.getNationalNumber() + "");
                } catch (NumberParseException e) {
                    mCountryCodeField.setText("1");
                    mPhoneNumberField.requestFocus();
                }
            } else {
                mCountryCodeField.setText("1");
                mPhoneNumberField.requestFocus();
            }
        }

	}

    @Override
    public void onResume() {
        super.onResume();

        SharedPreferences sharedPreferences = getSharedPreferences(Constants.KEY_SHARED_PREFERENCES_USERNAME, Context.MODE_PRIVATE);
        String username = sharedPreferences.getString(Constants.KEY_SHARED_PREFERENCES_USERNAME, null);
        boolean verifying = sharedPreferences.getBoolean(Constants.KEY_SHARED_PREFERENCES_VERIFYING, false);

        if (username != null && verifying) {
            Intent intent = new Intent(LoginActivity.this, VerificationActivity.class);
            intent.putExtra(Constants.KEY_VERIFICATION_FUNCTION_PHONENUMBER, username);
            intent.putExtra(Constants.KEY_ALREADY_LOG_IN, false);
            startActivity(intent);
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
        if (mCountryCodeField.hasFocus()) {
            mCountryCodeField.clearFocus();
        }
        if (mPhoneNumberField.hasFocus()) {
            mPhoneNumberField.clearFocus();
        }
    }

}
