package com.awgy.android.models;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.awgy.android.AppDelegate;
import com.awgy.android.utils.Constants;
import com.parse.ParseClassName;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bolts.Continuation;
import bolts.Task;

@ParseClassName(Constants.CLASS_ACTIVITY)

public class Activity extends ParseObject {

    public String getContent() {
        return getString(Constants.KEY_ACTIVITY_CONTENT);
    }

    public void setContent(String content) {
        put(Constants.KEY_ACTIVITY_CONTENT, content);
    }

    public String getFromUsername() {
        return getString(Constants.KEY_ACTIVITY_FROM_USERNAME);
    }

    public void setFromUsername(String fromUsername) {
        put(Constants.KEY_ACTIVITY_FROM_USERNAME, fromUsername);
    }

    public String getToGroupSelfieId() {
        return getString(Constants.KEY_ACTIVITY_TO_GROUPSELFIE_ID);
    }

    public void setToGroupSelfieId(String toGroupSelfieId) {
        put(Constants.KEY_ACTIVITY_TO_GROUPSELFIE_ID, toGroupSelfieId);
    }

    public String getType() {
        return getString(Constants.KEY_ACTIVITY_TYPE);
    }

    public void setType(String type) {
        put(Constants.KEY_ACTIVITY_TYPE, type);
    }

    public Date getLocalCreatedAt() {
        return getDate(Constants.KEY_LOCAL_CREATED_AT);
    }

    public void setLocalCreatedAt(Date localCreatedAt) {
        put(Constants.KEY_LOCAL_CREATED_AT, localCreatedAt);
    }

    public static Task<Activity> messageToGroupSelfie_withNotificationPayload(final GroupSelfie groupSelfie, JSONObject notificationPayload) {

        try {

            // no need to fetch local datastore since only one push per activity

            final Activity message = new Activity();
            message.setObjectId(notificationPayload.getString(Constants.KEY_PUSHPAYLOAD_ID));
            message.setType(Constants.KEY_ACTIVITY_TYPE_COMMENTED);
            message.setFromUsername(notificationPayload.getString(Constants.KEY_PUSHPAYLOAD_FROM_USERNAME));
            message.setToGroupSelfieId(notificationPayload.getString(Constants.KEY_PUSHPAYLOAD_TO_GROUPSELFIE_ID));
            message.setContent(notificationPayload.getString(Constants.KEY_PUSHPAYLOAD_CONTENT));
            message.setLocalCreatedAt(new Date(notificationPayload.getLong(Constants.KEY_PUSHPAYLOAD_CREATED_AT)));

            groupSelfie.setMessagedAt(message.getLocalCreatedAt());
            List<String> seenIds = new ArrayList<String>();
            seenIds.add(notificationPayload.getString(Constants.KEY_PUSHPAYLOAD_FROM_USER_ID));
            groupSelfie.setSeenIds(seenIds, false);

            return groupSelfie.pinGroupSelfieWithIncrement(false).continueWithTask(new Continuation<Void, Task<Activity>>() {
                @Override
                public Task<Activity> then(Task<Void> task) throws Exception {
                    if (!task.isFaulted() && !task.isCancelled()) {
                        return message.pinInBackground(GroupSelfie.keyWithGroupSelfieId(message.getToGroupSelfieId())).continueWithTask(new Continuation<Void, Task<Activity>>() {
                            @Override
                            public Task<Activity> then(Task<Void> task) throws Exception {
                                if (!task.isFaulted() && !task.isCancelled()) {
                                    Task<Activity>.TaskCompletionSource source = Task.create();
                                    source.setResult(message);
                                    return source.getTask();
                                } else {
                                    Task<Activity>.TaskCompletionSource source = Task.create();
                                    source.setError(task.getError());
                                    return source.getTask();
                                }
                            }
                        });
                    } else {
                        Task<Activity>.TaskCompletionSource source = Task.create();
                        source.setError(task.getError());
                        return source.getTask();
                    }
                }
            });

        } catch (JSONException e) {

            Task<Activity>.TaskCompletionSource source = Task.create();
            source.setError(e);
            return source.getTask();

        }

    }

    public Task<Void> saveMessageToGroupSelfie(final GroupSelfie groupSelfie) {

        groupSelfie.setMessagedAt(new Date());
        List<String> seenIds = new ArrayList<String>();
        seenIds.add(ParseUser.getCurrentUser().getObjectId());
        groupSelfie.setSeenIds(seenIds, false);
        groupSelfie.setNMessages(groupSelfie.getNMessages()+1, false);

        return saveInBackground().continueWithTask(new Continuation<Void, Task<Void>>() {
            @Override
            public Task<Void> then(Task<Void> task) throws Exception {
                if (!task.isFaulted() && !task.isCancelled()) {
                    groupSelfie.setMessagedAt(getCreatedAt());
                    groupSelfie.pinGroupSelfieWithIncrement(false);
                    return pinInBackground(GroupSelfie.keyWithGroupSelfieId(getToGroupSelfieId())).continueWithTask(new Continuation<Void, Task<Void>>() {
                        @Override
                        public Task<Void> then(Task<Void> task) throws Exception {
                            if (!task.isFaulted() && !task.isCancelled()) {
                                //Intent intent = new Intent(Constants.ACTIVITY_MESSAGE_HAS_BEEN_SAVED_NOTIFICATION);
                                //intent.putExtra(Constants.CLASS_ACTIVITY, getObjectId());
                                //LocalBroadcastManager.getInstance(AppDelegate.getContext()).sendBroadcast(intent);
                            }
                            return task;
                        }
                    });
                } else {
                    return task;
                }
            }
        });

    }

    public static Task<GroupSelfie> userDidDiscoverWithNotificationPayload(final JSONObject notificationPayload) {

        try {

            final String toGroupSelfieId = notificationPayload.getString(Constants.KEY_PUSHPAYLOAD_TO_GROUPSELFIE_ID);

            return GroupSelfie.groupSelfieWithId_loadIfNeeded(toGroupSelfieId,true).continueWithTask(new Continuation<GroupSelfie, Task<GroupSelfie>>() {
                @Override
                public Task<GroupSelfie> then(Task<GroupSelfie> task) throws Exception {
                    if (!task.isFaulted() && !task.isCancelled()) {
                        final GroupSelfie groupSelfie = task.getResult();
                        List<String> discoveredIds = groupSelfie.getDiscoveredIds();
                        if (discoveredIds == null) discoveredIds = new ArrayList<String>();
                        if (!discoveredIds.contains(notificationPayload.getString(Constants.KEY_PUSHPAYLOAD_FROM_USER_ID))) discoveredIds.add(notificationPayload.getString(Constants.KEY_PUSHPAYLOAD_FROM_USER_ID));
                        groupSelfie.setDiscoveredIds(discoveredIds, false);
                        return groupSelfie.pinGroupSelfieWithIncrement(false).continueWithTask(new Continuation<Void, Task<GroupSelfie>>() {
                            @Override
                            public Task<GroupSelfie> then(Task<Void> task) throws Exception {
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
            });

        } catch (JSONException e) {

            Task<GroupSelfie>.TaskCompletionSource source = Task.create();
            source.setError(e);
            return source.getTask();

        }

    }

    public static Task<GroupSelfie> userDidAddLoveWithNotificationPayload(final JSONObject notificationPayload) {

        try {

            final String toGroupSelfieId = notificationPayload.getString(Constants.KEY_PUSHPAYLOAD_TO_GROUPSELFIE_ID);

            return GroupSelfie.groupSelfieWithId_loadIfNeeded(toGroupSelfieId,true).continueWithTask(new Continuation<GroupSelfie, Task<GroupSelfie>>() {
                @Override
                public Task<GroupSelfie> then(Task<GroupSelfie> task) throws Exception {
                    if (!task.isFaulted() && !task.isCancelled()) {
                        final GroupSelfie groupSelfie = task.getResult();
                        List<String> loveIds = groupSelfie.getLoveIds();
                        if (loveIds == null) loveIds = new ArrayList<String>();
                        if (!loveIds.contains(notificationPayload.getString(Constants.KEY_PUSHPAYLOAD_FROM_USER_ID))) loveIds.add(notificationPayload.getString(Constants.KEY_PUSHPAYLOAD_FROM_USER_ID));
                        groupSelfie.setLoveIds(loveIds, false);
                        return groupSelfie.pinGroupSelfieWithIncrement(false).continueWithTask(new Continuation<Void, Task<GroupSelfie>>() {
                            @Override
                            public Task<GroupSelfie> then(Task<Void> task) throws Exception {
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
            });

        } catch (JSONException e) {

            Task<GroupSelfie>.TaskCompletionSource source = Task.create();
            source.setError(e);
            return source.getTask();

        }

    }

    public static Task<GroupSelfie> userDidRemoveLoveWithNotificationPayload(final JSONObject notificationPayload) {

        try {

            final String toGroupSelfieId = notificationPayload.getString(Constants.KEY_PUSHPAYLOAD_TO_GROUPSELFIE_ID);

            return GroupSelfie.groupSelfieWithId_loadIfNeeded(toGroupSelfieId,true).continueWithTask(new Continuation<GroupSelfie, Task<GroupSelfie>>() {
                @Override
                public Task<GroupSelfie> then(Task<GroupSelfie> task) throws Exception {
                    if (!task.isFaulted() && !task.isCancelled()) {
                        final GroupSelfie groupSelfie = task.getResult();
                        List<String> loveIds = groupSelfie.getLoveIds();
                        if (loveIds == null) loveIds = new ArrayList<String>();
                        if (loveIds.contains(notificationPayload.getString(Constants.KEY_PUSHPAYLOAD_FROM_USER_ID))) loveIds.remove(notificationPayload.getString(Constants.KEY_PUSHPAYLOAD_FROM_USER_ID));
                        groupSelfie.setLoveIds(loveIds, false);
                        return groupSelfie.pinGroupSelfieWithIncrement(false).continueWithTask(new Continuation<Void, Task<GroupSelfie>>() {
                            @Override
                            public Task<GroupSelfie> then(Task<Void> task) throws Exception {
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
            });

        } catch (JSONException e) {

            Task<GroupSelfie>.TaskCompletionSource source = Task.create();
            source.setError(e);
            return source.getTask();

        }

    }

    public static Task<GroupSelfie> userDidReadWithNotificationPayload(final JSONObject notificationPayload) {

        try {

            final String toGroupSelfieId = notificationPayload.getString(Constants.KEY_PUSHPAYLOAD_TO_GROUPSELFIE_ID);

            return GroupSelfie.groupSelfieWithId_loadIfNeeded(toGroupSelfieId,true).continueWithTask(new Continuation<GroupSelfie, Task<GroupSelfie>>() {
                @Override
                public Task<GroupSelfie> then(Task<GroupSelfie> task) throws Exception {
                    if (!task.isFaulted() && !task.isCancelled()) {
                        final GroupSelfie groupSelfie = task.getResult();
                        List<String> seenIds = groupSelfie.getSeenIds();
                        if (seenIds == null) seenIds = new ArrayList<String>();
                        if (!seenIds.contains(notificationPayload.getString(Constants.KEY_PUSHPAYLOAD_FROM_USER_ID))) seenIds.add(notificationPayload.getString(Constants.KEY_PUSHPAYLOAD_FROM_USER_ID));
                        groupSelfie.setSeenIds(seenIds, false);
                        return groupSelfie.pinGroupSelfieWithIncrement(false).continueWithTask(new Continuation<Void, Task<GroupSelfie>>() {
                            @Override
                            public Task<GroupSelfie> then(Task<Void> task) throws Exception {
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
            });

        } catch (JSONException e) {

            Task<GroupSelfie>.TaskCompletionSource source = Task.create();
            source.setError(e);
            return source.getTask();

        }

    }

}