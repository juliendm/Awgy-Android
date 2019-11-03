package com.awgy.android.utils;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.awgy.android.models.GroupSelfie;
import com.awgy.android.models.Activity;
import com.awgy.android.models.Relationship;
import com.awgy.android.ui.main.MainActivity;
import com.awgy.android.ui.ongoing.OngoingFragment;
import com.awgy.android.ui.stream.StreamFragment;
import com.parse.ParsePushBroadcastReceiver;
import com.parse.ParseUser;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import bolts.Continuation;
import bolts.Task;

public class PushBroadcastReceiver extends ParsePushBroadcastReceiver {

    private static List<PushBroadcastReceiver.PushBroadcastReceiverListener> mPushBroadcastReceiverListeners = new ArrayList<PushBroadcastReceiver.PushBroadcastReceiverListener>();

    private static String mBusyWithGroupSelfieId;
    public static void setBusyWithGroupSelfieId(String busyWithGroupSelfieId) {
        mBusyWithGroupSelfieId = busyWithGroupSelfieId;
    }

    @Override
    protected void onPushReceive(final Context context, Intent intent) {
        super.onPushReceive(context, intent);

        Bundle extras = intent.getExtras();
        if (extras != null) {
            try {
                final JSONObject data = new JSONObject(extras.getString("com.parse.Data"));

                String payloadKey = data.getString(Constants.KEY_PUSHPAYLOAD_PAYLOAD);
                if (payloadKey.equals(Constants.KEY_PUSHPAYLOAD_PAYLOAD_GROUPSELFIE)) {

                    final String typeKey = data.getString(Constants.KEY_PUSHPAYLOAD_TYPE);

                    if (typeKey.equals(Constants.KEY_PUSHPAYLOAD_TYPE_NEW) ||
                            typeKey.equals(Constants.KEY_PUSHPAYLOAD_TYPE_RENEW)) {

                        GroupSelfie.groupSelfieWithNotificationPayload(data, true).continueWithTask(new Continuation<GroupSelfie, Task<Void>>() {
                            @Override
                            public Task<Void> then(Task<GroupSelfie> task) throws Exception {
                                if (!task.isFaulted() && !task.isCancelled()) {

                                    final GroupSelfie groupSelfie = task.getResult();

                                    groupSelfie.syncCountDown().continueWithTask(new Continuation<Double, Task<Void>>() {
                                        @Override
                                        public Task<Void> then(Task<Double> task) throws Exception {
                                            if (!task.isFaulted() && !task.isCancelled()) {
                                                notifyOnManageGroupSelfie(groupSelfie, false);
                                            }
                                            return null;
                                        }
                                    });
                                }
                                return null;
                            }
                        });

                    } else if (typeKey.equals(Constants.KEY_PUSHPAYLOAD_TYPE_READY) ||
                            typeKey.equals(Constants.KEY_PUSHPAYLOAD_TYPE_CANCELED)) {

                        GroupSelfie.groupSelfieWithNotificationPayload(data, true).continueWithTask(new Continuation<GroupSelfie, Task<Void>>() {
                            @Override
                            public Task<Void> then(Task<GroupSelfie> task) throws Exception {
                                if (!task.isFaulted() && !task.isCancelled()) {

                                    final GroupSelfie groupSelfie = task.getResult();

                                    groupSelfie.syncCountDown().continueWithTask(new Continuation<Double, Task<Void>>() {
                                        @Override
                                        public Task<Void> then(Task<Double> task) throws Exception {
                                            if (!task.isFaulted() && !task.isCancelled()) {
                                                notifyOnManageGroupSelfie(groupSelfie, false);
                                            }
                                            return null;
                                        }
                                    });
                                }
                                return null;
                            }
                        });

                    } else if (typeKey.equals(Constants.KEY_PUSHPAYLOAD_TYPE_DELETED)) {

                        Relationship.userDeletedWithNotificationPayload(data);
                        GroupSelfie.userDeletedWithNotificationPayload(data).continueWithTask(new Continuation<GroupSelfie, Task<Void>>() {
                            @Override
                            public Task<Void> then(Task<GroupSelfie> task) throws Exception {
                                if (!task.isFaulted() && !task.isCancelled()) {
                                    final GroupSelfie groupSelfie = task.getResult();
                                    groupSelfie.syncCountDown().continueWithTask(new Continuation<Double, Task<Void>>() {
                                        @Override
                                        public Task<Void> then(Task<Double> task) throws Exception {
                                            if (!task.isFaulted() && !task.isCancelled()) {
                                                notifyOnDelete(groupSelfie);
                                            }
                                            return null;
                                        }
                                    });
                                }
                                return null;
                            }
                        });

                    }

                } else if (payloadKey.equals(Constants.KEY_PUSHPAYLOAD_PAYLOAD_ACTIVITY)) {

                    String typeKey = data.getString(Constants.KEY_PUSHPAYLOAD_TYPE);

                    if (typeKey.equals(Constants.KEY_PUSHPAYLOAD_TYPE_COMMENTED)) {

                        GroupSelfie.groupSelfieWithId_loadIfNeeded(data.getString(Constants.KEY_PUSHPAYLOAD_TO_GROUPSELFIE_ID),true).continueWithTask(new Continuation<GroupSelfie, Task<GroupSelfie>>() {
                            @Override
                            public Task<GroupSelfie> then(Task<GroupSelfie> task) throws Exception {
                                if (!task.isFaulted() && !task.isCancelled()) {
                                    final GroupSelfie groupSelfie = task.getResult();
                                    return Activity.messageToGroupSelfie_withNotificationPayload(groupSelfie,data).continueWithTask(new Continuation<Activity, Task<GroupSelfie>>() {
                                        @Override
                                        public Task<GroupSelfie> then(Task<Activity> task) throws Exception {
                                            if (!task.isFaulted() && !task.isCancelled()) {
                                                Task<GroupSelfie>.TaskCompletionSource source = Task.create();
                                                source.setResult(groupSelfie);
                                                return source.getTask();
                                            } else {
                                                Task<GroupSelfie>.TaskCompletionSource source = Task.create();
                                                source.setError(task.getError());
                                                return source.getTask();
                                            }
                                        }
                                    });
                                } else {
                                    Task<GroupSelfie>.TaskCompletionSource source = Task.create();
                                    source.setError(task.getError());
                                    return source.getTask();
                                }
                            }
                        }).continueWithTask(new Continuation<GroupSelfie, Task<Void>>() {
                            @Override
                            public Task<Void> then(Task<GroupSelfie> task) throws Exception {
                                if (!task.isFaulted() && !task.isCancelled()) {
                                    notifyOnMessage(task.getResult());
                                }
                                return null;
                            }
                        });

                    } else if (typeKey.equals(Constants.KEY_PUSHPAYLOAD_TYPE_READ)) {

                        Activity.userDidReadWithNotificationPayload(data).continueWithTask(new Continuation<GroupSelfie, Task<Void>>() {
                            @Override
                            public Task<Void> then(Task<GroupSelfie> task) throws Exception {
                                if (!task.isFaulted() && !task.isCancelled()) {
                                    notifyOnRead(task.getResult());
                                }
                                return null;
                            }
                        });

                    } else if (typeKey.equals(Constants.KEY_PUSHPAYLOAD_TYPE_DISCOVERED)) {

                        Activity.userDidDiscoverWithNotificationPayload(data).continueWithTask(new Continuation<GroupSelfie, Task<Void>>() {
                            @Override
                            public Task<Void> then(Task<GroupSelfie> task) throws Exception {
                                if (!task.isFaulted() && !task.isCancelled()) {
                                    notifyOnDiscover(task.getResult());
                                }
                                return null;
                            }
                        });

                    } else if (typeKey.equals(Constants.KEY_PUSHPAYLOAD_TYPE_LOVE_ADD)) {

                        Activity.userDidAddLoveWithNotificationPayload(data).continueWithTask(new Continuation<GroupSelfie, Task<Void>>() {
                            @Override
                            public Task<Void> then(Task<GroupSelfie> task) throws Exception {
                                if (!task.isFaulted() && !task.isCancelled()) {
                                    notifyOnLove(task.getResult());
                                }
                                return null;
                            }
                        });

                    } else if (typeKey.equals(Constants.KEY_PUSHPAYLOAD_TYPE_LOVE_REMOVE)) {

                        Activity.userDidRemoveLoveWithNotificationPayload(data).continueWithTask(new Continuation<GroupSelfie, Task<Void>>() {
                            @Override
                            public Task<Void> then(Task<GroupSelfie> task) throws Exception {
                                if (!task.isFaulted() && !task.isCancelled()) {
                                    notifyOnLove(task.getResult());
                                }
                                return null;
                            }
                        });

                    }

                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    protected void onPushOpen(Context context, Intent intent) {

        if (ParseUser.getCurrentUser() != null) {

            Bundle extras = intent.getExtras();
            if (extras != null) {
                try {
                    JSONObject data = new JSONObject(extras.getString("com.parse.Data"));

                    String payloadKey = data.getString(Constants.KEY_PUSHPAYLOAD_PAYLOAD);
                    if (payloadKey.equals(Constants.KEY_PUSHPAYLOAD_PAYLOAD_GROUPSELFIE)) {
                        String typeKey = data.getString(Constants.KEY_PUSHPAYLOAD_TYPE);

                        // If the push notification payload references a New GroupSelfie, we will attempt to push the right ViewController
                        if (typeKey.equals(Constants.KEY_PUSHPAYLOAD_TYPE_NEW)) {

                            Intent mainActivityIntent = new Intent(context, MainActivity.class);
                            MainActivity.setStaticShouldStartActivityForGroupSelfieNewNotificationPayload(data);
                            MainActivity.setStaticShouldStartActivityForGroupSelfieReNewNotificationPayload(null);
                            MainActivity.setStaticShouldStartActivityForGroupSelfieReadyNotificationPayload(null);
                            MainActivity.setStaticShouldStartActivityForActivityNotificationPayload(null);
                            mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            context.startActivity(mainActivityIntent);

                        } else if (typeKey.equals(Constants.KEY_PUSHPAYLOAD_TYPE_RENEW)) {

                            Intent mainActivityIntent = new Intent(context, MainActivity.class);
                            MainActivity.setStaticShouldStartActivityForGroupSelfieNewNotificationPayload(null);
                            MainActivity.setStaticShouldStartActivityForGroupSelfieReNewNotificationPayload(data);
                            MainActivity.setStaticShouldStartActivityForGroupSelfieReadyNotificationPayload(null);
                            MainActivity.setStaticShouldStartActivityForActivityNotificationPayload(null);
                            mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            context.startActivity(mainActivityIntent);

                        } else if (typeKey.equals(Constants.KEY_PUSHPAYLOAD_TYPE_READY)) {

                            Intent mainActivityIntent = new Intent(context, MainActivity.class);
                            MainActivity.setStaticShouldStartActivityForGroupSelfieNewNotificationPayload(null);
                            MainActivity.setStaticShouldStartActivityForGroupSelfieReNewNotificationPayload(null);
                            MainActivity.setStaticShouldStartActivityForGroupSelfieReadyNotificationPayload(data);
                            MainActivity.setStaticShouldStartActivityForActivityNotificationPayload(null);
                            mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            context.startActivity(mainActivityIntent);

                        }

                    } else if (payloadKey.equals(Constants.KEY_PUSHPAYLOAD_PAYLOAD_ACTIVITY)) {
                        String typeKey = data.getString(Constants.KEY_PUSHPAYLOAD_TYPE);

                        // If the push notification payload references a New Message, we will attempt to push GroupSelfieViewController
                        if (typeKey.equals(Constants.KEY_PUSHPAYLOAD_TYPE_COMMENTED)) {
                            Intent mainActivityIntent = new Intent(context, MainActivity.class);
                            MainActivity.setStaticShouldStartActivityForGroupSelfieNewNotificationPayload(null);
                            MainActivity.setStaticShouldStartActivityForGroupSelfieReNewNotificationPayload(null);
                            MainActivity.setStaticShouldStartActivityForGroupSelfieReadyNotificationPayload(null);
                            MainActivity.setStaticShouldStartActivityForActivityNotificationPayload(data);
                            mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            context.startActivity(mainActivityIntent);

                        }

                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        }

    }

    @Override
    protected void onPushDismiss(Context context, Intent intent) {
        super.onPushDismiss(context, intent);
    }

    @Override
    protected Notification getNotification(Context context, Intent intent) {

        Bundle extras = intent.getExtras();
        if (extras != null) {
            try {
                final JSONObject data = new JSONObject(extras.getString("com.parse.Data"));

                String payloadKey = data.getString(Constants.KEY_PUSHPAYLOAD_PAYLOAD);
                String typeKey = data.getString(Constants.KEY_PUSHPAYLOAD_TYPE);

                if (payloadKey.equals(Constants.KEY_PUSHPAYLOAD_PAYLOAD_ACTIVITY) &&
                        typeKey.equals(Constants.KEY_PUSHPAYLOAD_TYPE_COMMENTED) &&
                        mBusyWithGroupSelfieId != null &&
                        mBusyWithGroupSelfieId.equals(data.getString(Constants.KEY_PUSHPAYLOAD_TO_GROUPSELFIE_ID)) ) {
                    return null;
                } else {
                    return super.getNotification(context, intent);
                }

            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            return null;
        }

    }



    public interface PushBroadcastReceiverListener {
        void onManageGroupSelfie(GroupSelfie groupSelfie, boolean moveTop);
        void onMessage(GroupSelfie groupSelfie);
        void onDiscover(GroupSelfie groupSelfie);
        void onLove(GroupSelfie groupSelfie);
        void onRead(GroupSelfie groupSelfie);
        void onDelete(GroupSelfie groupSelfie);
    }

    public static void addPushBroadcastReceiverListener(PushBroadcastReceiver.PushBroadcastReceiverListener listener) {
        if (!mPushBroadcastReceiverListeners.contains(listener)) {
            mPushBroadcastReceiverListeners.add(listener);
        }
    }

    public static void removePushBroadcastReceiverListener(PushBroadcastReceiver.PushBroadcastReceiverListener listener) {
        if (mPushBroadcastReceiverListeners.contains(listener)) {
            mPushBroadcastReceiverListeners.remove(listener);
        }
    }

    private void notifyOnManageGroupSelfie(GroupSelfie groupSelfie, boolean moveTop) {

        if (StreamFragment.getStaticStreamAdapter() != null && groupSelfie.getImage() != null) StreamFragment.getStaticStreamAdapter().manageGroupSelfie(groupSelfie, moveTop, false);
        if (OngoingFragment.getStaticOngoingAdapter() != null) OngoingFragment.getStaticOngoingAdapter().manageGroupSelfie(groupSelfie);

        Iterator i$ = mPushBroadcastReceiverListeners.iterator();

        while(i$.hasNext()) {
            PushBroadcastReceiver.PushBroadcastReceiverListener listener = (PushBroadcastReceiver.PushBroadcastReceiverListener)i$.next();
            listener.onManageGroupSelfie(groupSelfie, moveTop);
        }
    }

    private void notifyOnMessage(GroupSelfie groupSelfie) {

        if (StreamFragment.getStaticStreamAdapter() != null && groupSelfie.getImage() != null) StreamFragment.getStaticStreamAdapter().manageGroupSelfie(groupSelfie, true, true);

        Iterator i$ = mPushBroadcastReceiverListeners.iterator();

        while(i$.hasNext()) {
            PushBroadcastReceiver.PushBroadcastReceiverListener listener = (PushBroadcastReceiver.PushBroadcastReceiverListener)i$.next();
            listener.onMessage(groupSelfie);
        }
    }

    private void notifyOnDiscover(GroupSelfie groupSelfie) {

        Iterator i$ = mPushBroadcastReceiverListeners.iterator();

        while(i$.hasNext()) {
            PushBroadcastReceiver.PushBroadcastReceiverListener listener = (PushBroadcastReceiver.PushBroadcastReceiverListener)i$.next();
            listener.onDiscover(groupSelfie);
        }
    }

    private void notifyOnLove(GroupSelfie groupSelfie) {

        if (StreamFragment.getStaticStreamAdapter() != null && groupSelfie.getImage() != null) StreamFragment.getStaticStreamAdapter().manageGroupSelfie(groupSelfie, false, true);

        Iterator i$ = mPushBroadcastReceiverListeners.iterator();

        while(i$.hasNext()) {
            PushBroadcastReceiver.PushBroadcastReceiverListener listener = (PushBroadcastReceiver.PushBroadcastReceiverListener)i$.next();
            listener.onLove(groupSelfie);
        }
    }

    private void notifyOnRead(GroupSelfie groupSelfie) {

        if (OngoingFragment.getStaticOngoingAdapter() != null  && groupSelfie.getImage() == null) OngoingFragment.getStaticOngoingAdapter().manageGroupSelfie(groupSelfie);

        Iterator i$ = mPushBroadcastReceiverListeners.iterator();

        while(i$.hasNext()) {
            PushBroadcastReceiver.PushBroadcastReceiverListener listener = (PushBroadcastReceiver.PushBroadcastReceiverListener)i$.next();
            listener.onRead(groupSelfie);
        }
    }

    private void notifyOnDelete(GroupSelfie groupSelfie) {

        if (StreamFragment.getStaticStreamAdapter() != null && groupSelfie.getImage() != null) StreamFragment.getStaticStreamAdapter().manageGroupSelfie(groupSelfie, false, false);
        if (OngoingFragment.getStaticOngoingAdapter() != null  && groupSelfie.getImage() == null) OngoingFragment.getStaticOngoingAdapter().manageGroupSelfie(groupSelfie);

        Iterator i$ = mPushBroadcastReceiverListeners.iterator();

        while(i$.hasNext()) {
            PushBroadcastReceiver.PushBroadcastReceiverListener listener = (PushBroadcastReceiver.PushBroadcastReceiverListener)i$.next();
            listener.onDelete(groupSelfie);
        }
    }

}