package com.awgy.android.models;

import com.awgy.android.utils.Constants;
import com.parse.ParseClassName;
import com.parse.ParseCloud;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import bolts.Continuation;
import bolts.Task;

@ParseClassName(Constants.CLASS_RELATIONSHIP)

public class Relationship extends ParseObject {

    public void clear() {
        remove(Constants.KEY_RELATIONSHIP_LOCAL_NUMBER_COM_SELFIES);
    }

    public Number getNComSelfies() {
        if (getNumber(Constants.KEY_RELATIONSHIP_LOCAL_NUMBER_COM_SELFIES) != null) {
            return getNumber(Constants.KEY_RELATIONSHIP_LOCAL_NUMBER_COM_SELFIES);
        } else {
            return getNumber(Constants.KEY_RELATIONSHIP_NUMBER_COM_SELFIES);
        }
    }

    public void setNComSelfies(Number nComSelfies, boolean cloud) {
        if (cloud) {
            remove(Constants.KEY_RELATIONSHIP_LOCAL_NUMBER_COM_SELFIES);
            put(Constants.KEY_RELATIONSHIP_NUMBER_COM_SELFIES, nComSelfies);
        } else {
            put(Constants.KEY_RELATIONSHIP_LOCAL_NUMBER_COM_SELFIES, nComSelfies);
        }
    }

    public String getToUsername() {
        return getString(Constants.KEY_RELATIONSHIP_TO_USERNAME);
    }

    public void setToUsername(String toUsername) {
        put(Constants.KEY_RELATIONSHIP_TO_USERNAME, toUsername);
    }

    public String getToUserId() {
        return getString(Constants.KEY_RELATIONSHIP_TO_USER_ID);
    }

    public void setToUserId(String toUserId) {
        put(Constants.KEY_RELATIONSHIP_TO_USER_ID, toUserId);
    }

    public String getToType() {
        return getString(Constants.KEY_RELATIONSHIP_TO_TYPE);
    }

    public String getWarning() {
        return getString(Constants.KEY_RELATIONSHIP_WARNING);
    }

    public void setWarning(String warning) {
        put(Constants.KEY_RELATIONSHIP_WARNING, warning);
    }

    public ParseUser getFromUser() {
        return getParseUser(Constants.KEY_RELATIONSHIP_FROM_USER);
    }

    public void setFromUser(ParseUser fromUser) {
        put(Constants.KEY_RELATIONSHIP_FROM_USER, fromUser);
    }

    public boolean getActive() {
        return getBoolean(Constants.KEY_RELATIONSHIP_ACTIVE);
    }

    public void setActive(boolean active) {
        put(Constants.KEY_RELATIONSHIP_ACTIVE, active);
    }

    public static Task<Relationship> relationshipWithUserId_andUsername_generate(final String userId, final String username, final boolean generate) {

        ParseQuery<Relationship> query = ParseQuery.getQuery(Relationship.class);
        query.whereEqualTo(Constants.KEY_RELATIONSHIP_TO_USER_ID, userId);
        query.fromPin(Constants.CLASS_RELATIONSHIP);
        return query.getFirstInBackground().continueWithTask(new Continuation<Relationship, Task<Relationship>>() {
            @Override
            public Task<Relationship> then(Task<Relationship> task) throws Exception {
                if ((task.isFaulted() || task.isCancelled()) && generate) {
                    Relationship relationship = new Relationship();
                    relationship.setToUserId(userId);
                    relationship.setToUsername(username);
                    Task<Relationship>.TaskCompletionSource source = Task.create();
                    source.setResult(relationship);
                    return source.getTask();
                } else {
                    return task;
                }
            }
        });

    }

    public static Task<Void> updateRelationships(final List<Relationship> relationships) {

        List<String> related = new ArrayList<String>();
        List<String> nonRelated = new ArrayList<String>();

        related.add(ParseUser.getCurrentUser().getUsername());
        if (relationships != null) for (Relationship relationship : relationships) related.add(relationship.getToUsername());

        for (String username : PhoneBook.getInstance().getNames().keySet()) {
            if (!related.contains(username)) {
                nonRelated.add(username);
            }
        }

        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put(Constants.KEY_RELATIONSHIP_FUNCTION_CONTACTS, nonRelated);
        return ParseCloud.callFunctionInBackground(Constants.KEY_RELATIONSHIP_FUNCTION, params).continueWithTask(new Continuation<Object, Task<Void>>() {
            @Override
            public Task<Void> then(Task<Object> task) throws Exception {
                if (!task.isFaulted() && !task.isCancelled()) {
                    List<Relationship> new_relationships = (List<Relationship>) task.getResult();
                    ParseObject.pinAllInBackground(Constants.CLASS_RELATIONSHIP, new_relationships);
                }
                Task<Void>.TaskCompletionSource source = Task.create();
                source.setError(task.getError());
                return source.getTask();
            }
        });

    }

    public Task<Void> saveRelationship() {
        return saveInBackground().continueWithTask(new Continuation<Void, Task<Void>>() {
            @Override
            public Task<Void> then(Task<Void> task) throws Exception {
                if (!task.isFaulted() && !task.isCancelled()) {
                    return pinInBackground(Constants.CLASS_RELATIONSHIP);
                } else {
                    return task;
                }
            }
        });
    }

    public static Task<Void> userDeletedWithNotificationPayload(JSONObject notificationPayload) {

        try {

            String deletingUserId = notificationPayload.getString(Constants.KEY_PUSHPAYLOAD_DELETING_USER_ID);
            String deletingUsername = notificationPayload.getString(Constants.KEY_PUSHPAYLOAD_DELETING_USERNAME);

            return Relationship.relationshipWithUserId_andUsername_generate(deletingUserId, deletingUsername, false).continueWithTask(new Continuation<Relationship, Task<Void>>() {
                @Override
                public Task<Void> then(Task<Relationship> task) throws Exception {
                    if (!task.isFaulted() && !task.isCancelled()) {
                        Relationship relationship = task.getResult();
                        int number = relationship.getNComSelfies().intValue();
                        number--;
                        if (number < 0) number = 0;
                        relationship.setNComSelfies(number, false);
                        return relationship.pinInBackground(Constants.CLASS_RELATIONSHIP);
                    } else {
                        Task<Void>.TaskCompletionSource source = Task.create();
                        source.setError(task.getError());
                        return source.getTask();
                    }
                }
            });

        } catch (JSONException e) {

            Task<Void>.TaskCompletionSource source = Task.create();
            source.setError(e);
            return source.getTask();

        }
    }

    public static String keyWithUserId(String userId) {
        return String.format("%s_%s_%s", Constants.CLASS_USER, userId, Constants.CLASS_GROUPSELFIE);
    }

}