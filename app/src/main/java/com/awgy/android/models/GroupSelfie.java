package com.awgy.android.models;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.awgy.android.AppDelegate;
import com.awgy.android.utils.Constants;

import com.awgy.android.utils.PinsOnFile;
import com.parse.ParseClassName;
import com.parse.ParseCloud;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import bolts.Continuation;
import bolts.Task;

@ParseClassName(Constants.CLASS_GROUPSELFIE)

public class GroupSelfie extends ParseObject {

    private byte[] mByteImageSmall;

    private List<GroupSelfie.GroupSelfieListener> mGroupSelfieListeners = new ArrayList<GroupSelfieListener>();

    public void clear() {
        remove(Constants.KEY_GROUPSELFIE_LOCAL_SEEN_IDS);
    }

    public List<String> getGroupIds() {
        return getList(Constants.KEY_GROUPSELFIE_GROUP_IDS);
    }

    public void setGroupIds(List<String> groupIds) {
        put(Constants.KEY_GROUPSELFIE_GROUP_IDS, groupIds);
    }

    public List<String> getGroupUsernames() {
        return getList(Constants.KEY_GROUPSELFIE_GROUP_USERNAMES);
    }

    public void setGroupUsernames(List groupUsernames, boolean cloud) {
        put(Constants.KEY_GROUPSELFIE_GROUP_USERNAMES, groupUsernames);
    }

    public List<String> getSeenIds() {
        if (getList(Constants.KEY_GROUPSELFIE_LOCAL_SEEN_IDS) != null) {
            return getList(Constants.KEY_GROUPSELFIE_LOCAL_SEEN_IDS);
        } else {
            return getList(Constants.KEY_GROUPSELFIE_SEEN_IDS);
        }
    }

    public void setSeenIds(List<String> seenIds, boolean cloud) {
        if (cloud) {
            remove(Constants.KEY_GROUPSELFIE_SEEN_IDS);
            put(Constants.KEY_GROUPSELFIE_SEEN_IDS, seenIds);
        } else {
            put(Constants.KEY_GROUPSELFIE_LOCAL_SEEN_IDS, seenIds);
        }
    }

    public Date getImprovedAt() {
        if (getDate(Constants.KEY_GROUPSELFIE_LOCAL_IMPROVED_AT) != null) {
            return getDate(Constants.KEY_GROUPSELFIE_LOCAL_IMPROVED_AT);
        } else {
            return getDate(Constants.KEY_GROUPSELFIE_IMPROVED_AT);
        }
    }

    public void setImprovedAt(Date messagedAt) {
        put(Constants.KEY_GROUPSELFIE_LOCAL_IMPROVED_AT, messagedAt);
    }





    public String getHashtag() {
        return getString(Constants.KEY_GROUPSELFIE_HASHTAG);
    }

    public void setHashtag(String hashtag) {
        put(Constants.KEY_GROUPSELFIE_HASHTAG, hashtag);
    }

    public ParseFile getImage() {
        return getParseFile(Constants.KEY_GROUPSELFIE_IMAGE);
    }

    public ParseFile getImageSmall() {
        return getParseFile(Constants.KEY_GROUPSELFIE_IMAGE_SMALL);
    }

    public Double getImageRatio() {
        return getDouble(Constants.KEY_GROUPSELFIE_IMAGE_RATIO);
    }

    public Date getLocalCreatedAt() {
        return getDate(Constants.KEY_LOCAL_CREATED_AT);
    }

    public void setLocalCreatedAt(Date localCreatedAt) {
        put(Constants.KEY_LOCAL_CREATED_AT, localCreatedAt);
    }

    //public CountDown getSecondaryCountDown() {
    //    return mSecondaryCountDown;
    //}

    //public void setSecondaryCountDown(CountDown secondaryCountDown) {
    //    mSecondaryCountDown = secondaryCountDown;
    //}

    public byte[] getByteImageSmall() {
        return mByteImageSmall;
    }

    public void setByteImageSmall(byte[] byteImageSmall) {
        mByteImageSmall = byteImageSmall;
    }

    public static Task<GroupSelfie> groupSelfieWithNotificationPayload(final JSONObject notificationPayload, final boolean refresh) {

        try {

            final String objectId = notificationPayload.getString(Constants.KEY_PUSHPAYLOAD_TO_GROUPSELFIE_ID);

            ParseQuery<GroupSelfie> query = ParseQuery.getQuery(GroupSelfie.class);
            query.fromPin(Constants.CLASS_GROUPSELFIE);
            return query.getInBackground(objectId).continueWithTask(new Continuation<GroupSelfie, Task<GroupSelfie>>() {
                @Override
                public Task<GroupSelfie> then(Task<GroupSelfie> task) throws Exception {

                    if (!task.isFaulted() && !task.isCancelled()) {

                        // Already in memory
                        final GroupSelfie groupSelfie = task.getResult();

                        if (!notificationPayload.has(Constants.KEY_PUSHPAYLOAD_IMAGE)) {

                            if (refresh) {

                                // Refresh and pin without increment

                                groupSelfie.setDuration(notificationPayload.getDouble(Constants.KEY_PUSHPAYLOAD_DURATION), false);
                                JSONArray groupIdsArray = notificationPayload.getJSONArray(Constants.KEY_PUSHPAYLOAD_GROUP_IDS);
                                List<String> groupIds = new ArrayList<String>();
                                for (int i = 0; i < groupIdsArray.length(); i++) {
                                    groupIds.add(groupIdsArray.getString(i));
                                }
                                groupSelfie.setGroupIds(groupIds, false);
                                JSONArray groupUsernamesArray = notificationPayload.getJSONArray(Constants.KEY_PUSHPAYLOAD_GROUP_USERNAMES);
                                List<String> groupUsernames = new ArrayList<String>();
                                for (int i = 0; i < groupUsernamesArray.length(); i++) {
                                    groupUsernames.add(groupUsernamesArray.getString(i));
                                }
                                groupSelfie.setGroupUsernames(groupUsernames, false);
                                JSONArray loveIdsArray = notificationPayload.getJSONArray(Constants.KEY_PUSHPAYLOAD_LOVE_IDS);
                                List<String> loveIds = new ArrayList<String>();
                                for (int i = 0; i < loveIdsArray.length(); i++) {
                                    loveIds.add(loveIdsArray.getString(i));
                                }
                                groupSelfie.setLoveIds(loveIds, false);
                                JSONArray discoveredIdsArray = notificationPayload.getJSONArray(Constants.KEY_PUSHPAYLOAD_DISCOVERED_IDS);
                                List<String> discoveredIds = new ArrayList<String>();
                                for (int i = 0; i < discoveredIdsArray.length(); i++) {
                                    discoveredIds.add(discoveredIdsArray.getString(i));
                                }
                                groupSelfie.setDiscoveredIds(discoveredIds, false);
                                JSONArray seenIdsArray = notificationPayload.getJSONArray(Constants.KEY_PUSHPAYLOAD_SEEN_IDS);
                                List<String> seenIds = new ArrayList<String>();
                                for (int i = 0; i < seenIdsArray.length(); i++) {
                                    seenIds.add(seenIdsArray.getString(i));
                                }
                                groupSelfie.setSeenIds(seenIds, false);
                                groupSelfie.setNMessages(notificationPayload.getInt(Constants.KEY_PUSHPAYLOAD_NMESSAGES), false);
                                groupSelfie.setMessagedAt(new Date(notificationPayload.getLong(Constants.KEY_PUSHPAYLOAD_MESSAGED_AT)));

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

                                return task;

                            }

                        } else {

                            // Load and pin without increment

                            groupSelfie.clear();

                            ParseQuery<GroupSelfie> query = ParseQuery.getQuery(GroupSelfie.class);
                            return query.getInBackground(objectId).continueWithTask(new Continuation<GroupSelfie, Task<GroupSelfie>>() {
                                @Override
                                public Task<GroupSelfie> then(Task<GroupSelfie> task) throws Exception {
                                    if (!task.isFaulted() && !task.isCancelled()) {
                                        final GroupSelfie groupSelfie = task.getResult();
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

                        }

                    } else {

                        if (!notificationPayload.has(Constants.KEY_PUSHPAYLOAD_IMAGE)) {

                            // Build and pin with increment

                            final GroupSelfie groupSelfie = buildGroupSelfieWithNotificationPayload(notificationPayload,false);
                            return groupSelfie.pinGroupSelfieWithIncrement(true).continueWithTask(new Continuation<Void, Task<GroupSelfie>>() {
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

                            // Load and pin with increment

                            ParseQuery<GroupSelfie> query = ParseQuery.getQuery(GroupSelfie.class);
                            return query.getInBackground(objectId).continueWithTask(new Continuation<GroupSelfie, Task<GroupSelfie>>() {
                                @Override
                                public Task<GroupSelfie> then(Task<GroupSelfie> task) throws Exception {
                                    if (!task.isFaulted() && !task.isCancelled()) {
                                        final GroupSelfie groupSelfie = task.getResult();
                                        return groupSelfie.pinGroupSelfieWithIncrement(true).continueWithTask(new Continuation<Void, Task<GroupSelfie>>() {
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

                        }

                    }
                }
            });

        } catch (JSONException e) {

            Task<GroupSelfie>.TaskCompletionSource source = Task.create();
            source.setError(e);
            return source.getTask();

        }

    }

    public static GroupSelfie buildGroupSelfieWithNotificationPayload(JSONObject notificationPayload) {
        return GroupSelfie.buildGroupSelfieWithNotificationPayload(notificationPayload, true);
    }

    public static GroupSelfie buildGroupSelfieWithNotificationPayload(JSONObject notificationPayload, boolean pin) {

        try {

            GroupSelfie groupSelfie = new GroupSelfie();

            groupSelfie.setObjectId(notificationPayload.getString(Constants.KEY_PUSHPAYLOAD_TO_GROUPSELFIE_ID));
            groupSelfie.setDuration(notificationPayload.getDouble(Constants.KEY_PUSHPAYLOAD_DURATION), false);

            JSONArray groupIdsArray = notificationPayload.getJSONArray(Constants.KEY_PUSHPAYLOAD_GROUP_IDS);
            List<String> groupIds = new ArrayList<String>();
            for (int i = 0; i < groupIdsArray.length(); i++) {
                groupIds.add(groupIdsArray.getString(i));
            }
            groupSelfie.setGroupIds(groupIds, false);

            JSONArray groupUsernamesArray = notificationPayload.getJSONArray(Constants.KEY_PUSHPAYLOAD_GROUP_USERNAMES);
            List<String> groupUsernames = new ArrayList<String>();
            for (int i = 0; i < groupUsernamesArray.length(); i++) {
                groupUsernames.add(groupUsernamesArray.getString(i));
            }
            groupSelfie.setGroupUsernames(groupUsernames, false);

            if (notificationPayload.has(Constants.KEY_PUSHPAYLOAD_LOVE_IDS)) {
                JSONArray loveIdsArray = notificationPayload.getJSONArray(Constants.KEY_PUSHPAYLOAD_LOVE_IDS);
                List<String> loveIds = new ArrayList<String>();
                for (int i = 0; i < loveIdsArray.length(); i++) {
                    loveIds.add(loveIdsArray.getString(i));
                }
                groupSelfie.setLoveIds(loveIds, false);
            }

            if (notificationPayload.has(Constants.KEY_PUSHPAYLOAD_DISCOVERED_IDS)) {
                JSONArray discoveredIdsArray = notificationPayload.getJSONArray(Constants.KEY_PUSHPAYLOAD_DISCOVERED_IDS);
                List<String> discoveredIds = new ArrayList<String>();
                for (int i = 0; i < discoveredIdsArray.length(); i++) {
                    discoveredIds.add(discoveredIdsArray.getString(i));
                }
                groupSelfie.setDiscoveredIds(discoveredIds, false);
            }

            if (notificationPayload.has(Constants.KEY_PUSHPAYLOAD_SEEN_IDS)) {
                JSONArray seenIdsArray = notificationPayload.getJSONArray(Constants.KEY_PUSHPAYLOAD_SEEN_IDS);
                List<String> seenIds = new ArrayList<String>();
                for (int i = 0; i < seenIdsArray.length(); i++) {
                    seenIds.add(seenIdsArray.getString(i));
                }
                groupSelfie.setSeenIds(seenIds, false);
            }

            groupSelfie.setNMessages(notificationPayload.getInt(Constants.KEY_PUSHPAYLOAD_NMESSAGES), false);

            groupSelfie.setHashtag(notificationPayload.getString(Constants.KEY_PUSHPAYLOAD_HASHTAG));
            groupSelfie.setDetails(notificationPayload.getString(Constants.KEY_PUSHPAYLOAD_DETAILS));
            groupSelfie.setMessagedAt(new Date(notificationPayload.getLong(Constants.KEY_PUSHPAYLOAD_MESSAGED_AT)));
            groupSelfie.setLocalCreatedAt(new Date(notificationPayload.getLong(Constants.KEY_PUSHPAYLOAD_CREATED_AT)));

            if (pin) groupSelfie.pinGroupSelfieWithIncrement(true);

            return groupSelfie;

        } catch (JSONException e) {
            return null;
        }

    }

    public static Task<GroupSelfie> groupSelfieWithId_loadIfNeeded(final String objectId, final boolean loadIfNeeded) {

        ParseQuery<GroupSelfie> query = ParseQuery.getQuery(GroupSelfie.class);
        query.fromPin(Constants.CLASS_GROUPSELFIE);
        return query.getInBackground(objectId).continueWithTask(new Continuation<GroupSelfie, Task<GroupSelfie>>() {
            @Override
            public Task<GroupSelfie> then(Task<GroupSelfie> task) throws Exception {
                if ((task.isFaulted() || task.isCancelled()) && loadIfNeeded) {
                    ParseQuery<GroupSelfie> query = ParseQuery.getQuery(GroupSelfie.class);
                    return query.getInBackground(objectId).continueWithTask(new Continuation<GroupSelfie, Task<GroupSelfie>>() {
                        @Override
                        public Task<GroupSelfie> then(final Task<GroupSelfie> task) throws Exception {
                            if (!task.isFaulted() && !task.isCancelled()) {
                                return task.getResult().pinGroupSelfieWithIncrement(true).continueWithTask(new Continuation<Void, Task<GroupSelfie>>() {
                                    @Override
                                    public Task<GroupSelfie> then(Task<Void> pinTask) throws Exception {
                                        if (!pinTask.isFaulted() && !pinTask.isCancelled()) {
                                            return task;
                                        } else {
                                            Task<GroupSelfie>.TaskCompletionSource source = Task.create();
                                            source.setError(pinTask.getError());
                                            return source.getTask();
                                        }
                                    }
                                });
                            } else {
                                return task;
                            }
                        }
                    });
                } else {
                    return task;
                }
            }
        });

    }

    public Task<Void> saveGroupSelfie() {

        return saveInBackground().continueWithTask(new Continuation<Void, Task<Void>>() {
            @Override
            public Task<Void> then(Task<Void> task) throws Exception {
                if (!task.isFaulted() && !task.isCancelled()) {
                    notifyOnSavedListeners();
                    return pinGroupSelfieWithIncrement(true);
                } else {
                    return task;
                }
            }
        });

    }

    public Task<Void> deleteGroupSelfie() {

        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put(Constants.KEY_GROUPSELFIE_FUNCTION_ID, getObjectId());
        return ParseCloud.callFunctionInBackground(Constants.KEY_GROUPSELFIE_FUNCTION_DELETE, params).continueWithTask(new Continuation
                <Object, Task<Void>>() {
            @Override
            public Task<Void> then(Task<Object> task) throws Exception {
                if (!task.isFaulted() && !task.isCancelled()) {
                    return unpinGroupSelfie().continueWithTask(new Continuation<Void, Task<Void>>() {
                        @Override
                        public Task<Void> then(Task<Void> task) throws Exception {
                            if (!task.isFaulted() && !task.isCancelled()) {
                                Intent intent = new Intent(Constants.GROUPSELFIE_GROUPSELFIE_HAS_BEEN_DELETED_NOTIFICATION);
                                intent.putExtra(Constants.CLASS_GROUPSELFIE, getObjectId());
                                LocalBroadcastManager.getInstance(AppDelegate.getContext()).sendBroadcast(intent);
                            }
                            return task;
                        }
                    });
                } else {
                    Task<Void>.TaskCompletionSource source = Task.create();
                    source.setError(task.getError());
                    return source.getTask();
                }
            }
        });

    }

    public static Task<GroupSelfie> userDeletedWithNotificationPayload(JSONObject notificationPayload) {

        try {

            String groupSelfieId = notificationPayload.getString(Constants.KEY_PUSHPAYLOAD_TO_GROUPSELFIE_ID);
            final String deletingUserId = notificationPayload.getString(Constants.KEY_PUSHPAYLOAD_DELETING_USER_ID);
            final String deletingUsername = notificationPayload.getString(Constants.KEY_PUSHPAYLOAD_DELETING_USERNAME);

            ParseQuery<GroupSelfie> query = ParseQuery.getQuery(GroupSelfie.class);
            query.fromPin(Constants.CLASS_GROUPSELFIE);
            return query.getInBackground(groupSelfieId).continueWithTask(new Continuation<GroupSelfie, Task<GroupSelfie>>() {
                @Override
                public Task<GroupSelfie> then(Task<GroupSelfie> task) throws Exception {
                    if (!task.isFaulted() && !task.isCancelled()) {
                        GroupSelfie groupSelfie = task.getResult();

                        List<String> groupIds = groupSelfie.getGroupIds();
                        groupIds.remove(deletingUserId);
                        groupSelfie.setGroupIds((List<String>) groupIds, false);

                        List<String> groupUsernames = groupSelfie.getGroupUsernames();
                        groupUsernames.remove(deletingUsername);
                        groupSelfie.setGroupUsernames((List<String>) groupUsernames, false);
                    }
                    return task;
                }
            });

        } catch (JSONException e) {

            Task<GroupSelfie>.TaskCompletionSource source = Task.create();
            source.setError(e);
            return source.getTask();

        }

    }

    public Task<Void> addDiscoveredId() {

        List<String> discoveredIds;
        if (getDiscoveredIds() != null) {
            discoveredIds = new ArrayList<String>(getDiscoveredIds());
        } else {
            discoveredIds = new ArrayList<String>();
        }

        if (!discoveredIds.contains(ParseUser.getCurrentUser().getObjectId())) discoveredIds.add(ParseUser.getCurrentUser().getObjectId());
        setDiscoveredIds(discoveredIds, false);

        return pinGroupSelfieWithIncrement(false).continueWithTask(new Continuation<Void, Task<Void>>() {
            @Override
            public Task<Void> then(Task<Void> task) throws Exception {

                Intent intent = new Intent(Constants.GROUPSELFIE_DISCOVERED_IDS_HAS_BEEN_UPDATED_NOTIFICATION);
                intent.putExtra(Constants.CLASS_GROUPSELFIE, getObjectId());
                LocalBroadcastManager.getInstance(AppDelegate.getContext()).sendBroadcast(intent);

                HashMap<String, Object> params = new HashMap<String, Object>();
                params.put(Constants.KEY_OBJECT_ID, getObjectId());
                ParseCloud.callFunctionInBackground(Constants.KEY_GROUPSELFIE_FUNCTION_ADD_DISCOVERED_ID, params);

                return task;
            }
        });

    }

    public Task<Void> addLoveId(final boolean callNetwork) {

        List<String> loveIds;
        if (getLoveIds() != null) {
            loveIds = new ArrayList<String>(getLoveIds());
        } else {
            loveIds = new ArrayList<String>();
        }

        if (!loveIds.contains(ParseUser.getCurrentUser().getObjectId())) loveIds.add(ParseUser.getCurrentUser().getObjectId());
        setLoveIds(loveIds, false);

        return pinGroupSelfieWithIncrement(false).continueWithTask(new Continuation<Void, Task<Void>>() {
            @Override
            public Task<Void> then(Task<Void> task) throws Exception {

                if (callNetwork) {
                    HashMap<String, Object> params = new HashMap<String, Object>();
                    params.put(Constants.KEY_OBJECT_ID, getObjectId());
                    return ParseCloud.callFunctionInBackground(Constants.KEY_GROUPSELFIE_FUNCTION_ADD_LOVE_ID, params).continueWithTask(new Continuation<Object, Task<Void>>() {
                        @Override
                        public Task<Void> then(Task<Object> task) throws Exception {
                            if (!task.isFaulted() && !task.isCancelled()) {
                                Intent intent = new Intent(Constants.GROUPSELFIE_LOVE_IDS_HAS_BEEN_UPDATED_NOTIFICATION);
                                intent.putExtra(Constants.CLASS_GROUPSELFIE, getObjectId());
                                LocalBroadcastManager.getInstance(AppDelegate.getContext()).sendBroadcast(intent);
                            }
                            Task<Void>.TaskCompletionSource source = Task.create();
                            source.setResult(null);
                            return source.getTask();
                        }
                    });
                } else {
                    return task;
                }

            }
        });

    }

    public Task<Void> removeLoveId(final boolean callNetwork) {

        List<String> loveIds;
        if (getLoveIds() != null) {
            loveIds = new ArrayList<String>(getLoveIds());
        } else {
            loveIds = new ArrayList<String>();
        }

        if (loveIds.contains(ParseUser.getCurrentUser().getObjectId())) loveIds.remove(ParseUser.getCurrentUser().getObjectId());
        setLoveIds(loveIds, false);

        return pinGroupSelfieWithIncrement(false).continueWithTask(new Continuation<Void, Task<Void>>() {
            @Override
            public Task<Void> then(Task<Void> task) throws Exception {

                if (callNetwork) {
                    HashMap<String, Object> params = new HashMap<String, Object>();
                    params.put(Constants.KEY_OBJECT_ID, getObjectId());
                    return ParseCloud.callFunctionInBackground(Constants.KEY_GROUPSELFIE_FUNCTION_REMOVE_LOVE_ID, params).continueWithTask(new Continuation<Object, Task<Void>>() {
                        @Override
                        public Task<Void> then(Task<Object> task) throws Exception {
                            if (!task.isFaulted() && !task.isCancelled()) {
                                Intent intent = new Intent(Constants.GROUPSELFIE_LOVE_IDS_HAS_BEEN_UPDATED_NOTIFICATION);
                                intent.putExtra(Constants.CLASS_GROUPSELFIE, getObjectId());
                                LocalBroadcastManager.getInstance(AppDelegate.getContext()).sendBroadcast(intent);
                            }
                            Task<Void>.TaskCompletionSource source = Task.create();
                            source.setResult(null);
                            return source.getTask();
                        }
                    });
                } else {
                    return task;
                }
                
            }
        });

    }

    public Task<Void> addSeenId(final boolean broadcast, final boolean callNetwork) {

        List<String> seenIds;
        if (getSeenIds() != null) {
            seenIds = new ArrayList<String>(getSeenIds());
        } else {
            seenIds = new ArrayList<String>();
        }

        if (!seenIds.contains(ParseUser.getCurrentUser().getObjectId())) seenIds.add(ParseUser.getCurrentUser().getObjectId());
        setSeenIds(seenIds, false);

        return pinGroupSelfieWithIncrement(false).continueWithTask(new Continuation<Void, Task<Void>>() {
            @Override
            public Task<Void> then(Task<Void> task) throws Exception {
                if (broadcast) {
                    Intent intent = new Intent(Constants.GROUPSELFIE_SEEN_IDS_HAS_BEEN_UPDATED_NOTIFICATION);
                    intent.putExtra(Constants.CLASS_GROUPSELFIE, getObjectId());
                    LocalBroadcastManager.getInstance(AppDelegate.getContext()).sendBroadcast(intent);
                }
                if (callNetwork) {
                    HashMap<String, Object> params = new HashMap<String, Object>();
                    params.put(Constants.KEY_OBJECT_ID, getObjectId());
                    ParseCloud.callFunctionInBackground(Constants.KEY_GROUPSELFIE_FUNCTION_ADD_SEEN_ID, params);
                }
                return task;
            }
        });

    }

    // Pin

    public Task<Void> pinGroupSelfieWithIncrement(boolean increment) {

        List<Task<Void>> tasks = new ArrayList<Task<Void>>();

        tasks.add(pinInBackground(Constants.CLASS_GROUPSELFIE));

        if (increment) {

            // increase related relationships counters
            ParseQuery<Relationship> query = ParseQuery.getQuery(Relationship.class);
            query.whereGreaterThanOrEqualTo(Constants.KEY_RELATIONSHIP_NUMBER_COM_SELFIES, 0);
            query.setLimit(1000);
            query.fromPin(Constants.CLASS_RELATIONSHIP);
            tasks.add(query.findInBackground().continueWithTask(new Continuation<List<Relationship>, Task<Void>>() {
                @Override
                public Task<Void> then(Task<List<Relationship>> task) throws Exception {
                    if (!task.isFaulted() && !task.isCancelled()) {
                        List<Task<Void>> tasks_in = new ArrayList<Task<Void>>();
                        for (final Relationship relationship : task.getResult()) {
                            if (getGroupIds().contains(relationship.getToUserId())) {
                                int number = relationship.getNComSelfies().intValue();
                                number++;
                                relationship.setNComSelfies(number, false);
                                tasks_in.add(relationship.pinInBackground(Constants.CLASS_RELATIONSHIP).continueWithTask(new Continuation<Void, Task<Void>>() {
                                    @Override
                                    public Task<Void> then(Task<Void> task) throws Exception {
                                        Intent intent = new Intent(Constants.GROUPSELFIE_RELATIONSHIP_COUNT_HAS_BEEN_UPDATED_NOTIFICATION);
                                        intent.putExtra(Constants.CLASS_RELATIONSHIP,relationship.getObjectId());
                                        LocalBroadcastManager.getInstance(AppDelegate.getContext()).sendBroadcast(intent);
                                        return task;
                                    }
                                }));
                            }
                        }
                        return Task.whenAll(tasks_in);
                    } else {
                        // Succeed even if can't find a relationship to increment
                        Task<Void>.TaskCompletionSource source = Task.create();
                        source.setResult(null);
                        return source.getTask();
                    }
                }
            }));

            // increase related hashtags counters (create if none)
            tasks.add(Hashtag.hashtagWithName(this.getHashtag()).continueWithTask(new Continuation<Hashtag, Task<Void>>() {
                @Override
                public Task<Void> then(Task<Hashtag> task) throws Exception {
                    if (!task.isFaulted() && !task.isCancelled()) {
                        final Hashtag hashtag = task.getResult();
                        int number = hashtag.getNumberCommon().intValue();
                        number++;
                        hashtag.setNumberCommon(number, false);
                        return hashtag.pinInBackground(Constants.CLASS_HASHTAG).continueWithTask(new Continuation<Void, Task<Void>>() {
                            @Override
                            public Task<Void> then(Task<Void> task) throws Exception {
                                Intent intent = new Intent(Constants.GROUPSELFIE_HASHTAG_COUNT_HAS_BEEN_UPDATED_NOTIFICATION);
                                intent.putExtra(Constants.CLASS_HASHTAG, hashtag.getObjectId());
                                intent.putExtra(Constants.KEY_HASHTAG_NUMBER, hashtag.getNumberCommon().intValue());
                                LocalBroadcastManager.getInstance(AppDelegate.getContext()).sendBroadcast(intent);
                                return task;
                            }
                        });
                    } else {
                        // Here it is a true error, since was supposed to be created if not found
                        Task<Void>.TaskCompletionSource source = Task.create();
                        source.setError(task.getError());
                        return source.getTask();
                    }
                }
            }));
        }

        return Task.whenAll(tasks);
    }

    public Task<Void> unpinGroupSelfie() {

        List<Task<Void>> tasks = new ArrayList<Task<Void>>();

        tasks.add(unpinInBackground(Constants.CLASS_GROUPSELFIE));
        tasks.add(ParseObject.unpinAllInBackground(GroupSelfie.keyWithGroupSelfieId(getObjectId())));

        // decrease related relationships counters
        ParseQuery<Relationship> relationship_query = ParseQuery.getQuery(Relationship.class);
        relationship_query.whereGreaterThanOrEqualTo(Constants.KEY_RELATIONSHIP_NUMBER_COM_SELFIES, 0);
        relationship_query.setLimit(1000);
        relationship_query.fromPin(Constants.CLASS_RELATIONSHIP);
        tasks.add(relationship_query.findInBackground().continueWithTask(new Continuation<List<Relationship>, Task<Void>>() {
            @Override
            public Task<Void> then(Task<List<Relationship>> task) throws Exception {
                if (!task.isFaulted() && !task.isCancelled()) {
                    List<Task<Void>> tasks_in = new ArrayList<Task<Void>>();
                    for (final Relationship relationship : task.getResult()) {
                        if (getGroupIds().contains(relationship.getToUserId())) {
                            int number = relationship.getNComSelfies().intValue();
                            number--;
                            if (number < 0) number = 0;
                            relationship.setNComSelfies(number, false);
                            tasks_in.add(relationship.pinInBackground(Constants.CLASS_RELATIONSHIP).continueWithTask(new Continuation<Void, Task<Void>>() {
                                @Override
                                public Task<Void> then(Task<Void> task) throws Exception {
                                    Intent intent = new Intent(Constants.GROUPSELFIE_RELATIONSHIP_COUNT_HAS_BEEN_UPDATED_NOTIFICATION);
                                    intent.putExtra(Constants.CLASS_RELATIONSHIP, relationship.getObjectId());
                                    LocalBroadcastManager.getInstance(AppDelegate.getContext()).sendBroadcast(intent);
                                    return task;
                                }
                            }));
                        }
                    }
                    return Task.whenAll(tasks_in);
                } else {
                    // Succeed even if can't find a relationship to decrement
                    Task<Void>.TaskCompletionSource source = Task.create();
                    source.setResult(null);
                    return source.getTask();
                }
            }
        }));

        // decrease related hashtags counters (destroy if gets to 0)
        ParseQuery<Hashtag> hashtag_query = ParseQuery.getQuery(Hashtag.class);
        hashtag_query.whereEqualTo(Constants.KEY_HASHTAG_NAME, this.getHashtag());
        hashtag_query.fromPin(Constants.CLASS_HASHTAG);
        tasks.add(hashtag_query.getFirstInBackground().continueWithTask(new Continuation<Hashtag, Task<Void>>() {
            @Override
            public Task<Void> then(Task<Hashtag> task) throws Exception {
                if (!task.isFaulted() && !task.isCancelled()) {
                    final Hashtag hashtag = task.getResult();
                    int number = hashtag.getNumberCommon().intValue();
                    number--;
                    if (number <= 0) {
                        return hashtag.unpinInBackground(Constants.CLASS_HASHTAG).continueWithTask(new Continuation<Void, Task<Void>>() {
                            @Override
                            public Task<Void> then(Task<Void> task) throws Exception {
                                Intent intent = new Intent(Constants.GROUPSELFIE_HASHTAG_COUNT_HAS_BEEN_UPDATED_NOTIFICATION);
                                intent.putExtra(Constants.CLASS_HASHTAG, hashtag.getObjectId());
                                intent.putExtra(Constants.KEY_HASHTAG_NUMBER, hashtag.getNumberCommon().intValue());
                                LocalBroadcastManager.getInstance(AppDelegate.getContext()).sendBroadcast(intent);
                                return task;
                            }
                        });
                    } else {
                        hashtag.setNumberCommon(number, false);
                        return hashtag.pinInBackground(Constants.CLASS_HASHTAG).continueWithTask(new Continuation<Void, Task<Void>>() {
                            @Override
                            public Task<Void> then(Task<Void> task) throws Exception {
                                Intent intent = new Intent(Constants.GROUPSELFIE_HASHTAG_COUNT_HAS_BEEN_UPDATED_NOTIFICATION);
                                intent.putExtra(Constants.CLASS_HASHTAG, hashtag.getObjectId());
                                intent.putExtra(Constants.KEY_HASHTAG_NUMBER, hashtag.getNumberCommon().intValue());
                                LocalBroadcastManager.getInstance(AppDelegate.getContext()).sendBroadcast(intent);
                                return task;
                            }
                        });
                    }
                } else {
                    // Succeed even if can't find a hashtag to decrement
                    Task<Void>.TaskCompletionSource source = Task.create();
                    source.setResult(null);
                    return source.getTask();
                }
            }
        }));

        return Task.whenAll(tasks);
    }

    public static String keyWithGroupSelfieId(String groupSelfieId) {
        String key = String.format("%s_%s_%s", Constants.CLASS_GROUPSELFIE, groupSelfieId, Constants.CLASS_ACTIVITY);
        PinsOnFile.getInstance().addPin(key);
        return key;
    }

    public Task<Double> syncCountDown() {

        Number duration = getDuration();

        if (getCountDown() == null) {

            if (getImage() == null && duration.floatValue() > 0.0) {

                // Create CountDown
                setCountDown(new CountDown());
                getCountDown().addCountDownListener(new CountDown.CountDownListener() {
                    @Override
                    public void onUpdate() {
                    }

                    @Override
                    public void onComplete() {
                    }

                    @Override
                    public void onPing() {

                        ParseQuery<GroupSelfie> query = ParseQuery.getQuery(GroupSelfie.class);
                        query.getInBackground(getObjectId()).continueWithTask(new Continuation<GroupSelfie, Task<GroupSelfie>>() {
                            @Override
                            public Task<GroupSelfie> then(Task<GroupSelfie> task) throws Exception {
                                if (!task.isFaulted() && !task.isCancelled()) {
                                    final GroupSelfie groupSelfie = task.getResult();
                                    groupSelfie.pinInBackground(Constants.CLASS_GROUPSELFIE).continueWithTask(new Continuation<Void, Task<Void>>() {
                                        @Override
                                        public Task<Void> then(Task<Void> task) throws Exception {

                                            if (getImage() == null && groupSelfie.getDuration().floatValue() > 0.0) {
                                                // Flag groupSelfie - add discrepancy so that all user do not flag at exact same time
                                                new Timer().schedule(new TimerTask() {
                                                    @Override
                                                    public void run() {
                                                        HashMap<String, Object> params = new HashMap<String, Object>();
                                                        params.put(Constants.KEY_GROUPSELFIE_FUNCTION_ID, groupSelfie.getObjectId());
                                                        ParseCloud.callFunctionInBackground(Constants.KEY_GROUPSELFIE_FUNCTION_FLAG, params);
                                                    }
                                                }, (long) (Math.random() * 3.0) * 1000);
                                            }

                                            getCountDown().notifyOnCancelListeners();
                                            setCountDown(null);

                                            return null;
                                        }
                                    });
                                } else {
                                    setDuration(0.0, false);
                                    pinInBackground(Constants.CLASS_GROUPSELFIE).continueWithTask(new Continuation<Void, Task<Void>>() {
                                        @Override
                                        public Task<Void> then(Task<Void> task) throws Exception {

                                            // Flag groupSelfie - add discrepancy so that all user do not flag at exact same time
                                            new Timer().schedule(new TimerTask() {
                                                @Override
                                                public void run() {
                                                    HashMap<String, Object> params = new HashMap<String, Object>();
                                                    params.put(Constants.KEY_GROUPSELFIE_FUNCTION_ID, getObjectId());
                                                    ParseCloud.callFunctionInBackground(Constants.KEY_GROUPSELFIE_FUNCTION_FLAG, params);
                                                }
                                            }, (long) (Math.random() * 3.0) * 1000);

                                            getCountDown().notifyOnCancelListeners();
                                            setCountDown(null);

                                            return null;
                                        }
                                    });
                                }

                                return null;
                            }
                        });

                    }

                    @Override
                    public void onCancel() {
                    }
                });

                // Create Secondary CountDown
                /*if (getSecondaryCountDown() == null && getSeenIds()!= null && getSeenIds().contains(ParseUser.getCurrentUser().getObjectId())) {
                    setSecondaryCountDown(new CountDown());

                    getSecondaryCountDown().addCountDownListener(new CountDown.CountDownListener() {

                        @Override
                        public void onUpdate() {
                        }

                        @Override
                        public void onComplete() {
                        }

                        @Override
                        public void onPing() {
                            setSecondaryCountDown(null);
                            if (getImage() == null) {
                                HashMap<String, Object> params = new HashMap<String, Object>();
                                params.put(Constants.KEY_GROUPSELFIE_FUNCTION_ID, getObjectId());
                                ParseCloud.callFunctionInBackground(Constants.KEY_GROUPSELFIE_FUNCTION_FLAG, params);
                            }
                        }

                        @Override
                        public void onCancel() {
                        }
                    });

                    getSecondaryCountDown().setRemainingTime(0.2);
                }*/

                if (getObjectId() != null) {

                    HashMap<String, Object> params = new HashMap<String, Object>();
                    params.put(Constants.KEY_OBJECT_ID, getObjectId());
                    return ParseCloud.callFunctionInBackground(Constants.KEY_GROUPSELFIE_FUNCTION_REMAINING_TIME, params).continueWithTask(new Continuation
                            <Object, Task<Double>>() {
                        @Override
                        public Task<Double> then(Task<Object> task) {
                            if (!task.isFaulted() && !task.isCancelled()) {
                                Number remainingMilli = (Number) task.getResult();
                                Double remainingTime = Math.round(remainingMilli.doubleValue() / 100.0) / 10.0;

                                getCountDown().setRemainingTime(remainingTime);

                                notifyOnSetRemainingTimeListeners();

                                Task<Double>.TaskCompletionSource source = Task.create();
                                source.setResult(remainingTime);
                                return source.getTask();

                            } else {

                                Task<Double>.TaskCompletionSource source = Task.create();
                                source.setError(task.getError());
                                return source.getTask();

                            }

                        }
                    });

                } else {

                    getCountDown().setRemainingTime(getDuration());

                    notifyOnSetRemainingTimeListeners();

                    Task<Double>.TaskCompletionSource source = Task.create();
                    source.setResult(getDuration());
                    return source.getTask();

                }

            } else {

                Task<Double>.TaskCompletionSource source = Task.create();
                source.setResult(0.0);
                return source.getTask();

            }

        } else {

            if (getImage() != null || duration.floatValue() == 0.0) {

                getCountDown().deleteCountDown();
                setCountDown(null);

                /*if (getSecondaryCountDown() != null) {
                    getSecondaryCountDown().deleteCountDown();
                    setSecondaryCountDown(null);
                }*/

                Task<Double>.TaskCompletionSource source = Task.create();
                source.setResult(0.0);
                return source.getTask();

            } else {

                Task<Double>.TaskCompletionSource source = Task.create();
                source.setResult(getCountDown().getRemainingTime());
                return source.getTask();

            }

        }

    }

    public interface GroupSelfieListener {
        void onSaved();
        void onSetRemainingTime();
    }

    public void addGroupSelfieListener(GroupSelfie.GroupSelfieListener listener) {
        if (!mGroupSelfieListeners.contains(listener)) {
            mGroupSelfieListeners.add(listener);
        }
    }

    public void removeGroupSelfieListener(GroupSelfie.GroupSelfieListener listener) {
        if (mGroupSelfieListeners.contains(listener)) {
            mGroupSelfieListeners.remove(listener);
        }
    }

    private void notifyOnSavedListeners() {
        Iterator i$ = mGroupSelfieListeners.iterator();

        while(i$.hasNext()) {
            GroupSelfie.GroupSelfieListener listener = (GroupSelfie.GroupSelfieListener)i$.next();
            listener.onSaved();
        }
    }

    private void notifyOnSetRemainingTimeListeners() {
        Iterator i$ = mGroupSelfieListeners.iterator();

        while(i$.hasNext()) {
            GroupSelfie.GroupSelfieListener listener = (GroupSelfie.GroupSelfieListener)i$.next();
            listener.onSetRemainingTime();
        }
    }

}