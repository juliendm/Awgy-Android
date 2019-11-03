package com.awgy.android.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.SmsMessage;

import com.awgy.android.AppDelegate;

public class SmsBroadcastReceiver extends BroadcastReceiver {

    public void onReceive(Context context, Intent intent) {
        Bundle pudsBundle = intent.getExtras();
        if (pudsBundle != null) {
            try {
                Object[] pdus = (Object[]) pudsBundle.get("pdus");
                SmsMessage messages = SmsMessage.createFromPdu((byte[]) pdus[0]);
                if (messages.getMessageBody().contains("Awgy")) {
                    String code = messages.getMessageBody().replaceAll("[^0-9]", "");
                    Intent broadcastIntent = new Intent(Constants.VERIFICATION_CODE_HAS_BEEN_RECEIVED_NOTIFICATION);
                    broadcastIntent.putExtra(Constants.VERIFICATION_CODE,code);
                    LocalBroadcastManager.getInstance(AppDelegate.getContext()).sendBroadcast(broadcastIntent);
                }
            } catch (Exception e) {

            }
        }
    }

}