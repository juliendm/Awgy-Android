package com.awgy.android.utils;

public final class Constants {

    public static final String PUSH_BROADCAST_RECEIVER_DID_RECEIVE_REMOTE_GROUSELFIE_NOTIFICATION = "pushBroadcastReceiver.didReceiveRemoteGroupSelfieNotification";
    public static final String PUSH_BROADCAST_RECEIVER_DID_RECEIVE_REMOTE_ACTIVITY_NOTIFICATION   = "pushBroadcastReceiver.didReceiveRemoteActivityNotification";

    public static final String PHONEBOOK_PHONEBOOK_HAS_BEEN_UPDATED_NOTIFICATION            = "phoneBook.phoneBookHasBeenUpdatedNotification";
    public static final String GROUPSELFIE_GROUPSELFIE_HAS_BEEN_UPDATED_NOTIFICATION        = "groupSelfie.groupSelfieHasBeenDeletedNotification";

    public static final String VERIFICATION_CODE_HAS_BEEN_RECEIVED_NOTIFICATION             = "smsBroadcastReceiver.verificationCodeHasBeenReceivedNotification";
    public static final String VERIFICATION_CODE                                            = "smsBroadcastReceiver.verificationCode";




    public static final String STREAM_ACTIVITY_NEED_LOAD_CACHE                              = "streamActivity.needLoadCache";

    public static final String KEY_ACCEPT_CONDITIONS = "acceptConditions";
    public static final String KEY_ALREADY_LOG_IN = "alreadyLogIn";

    public static final String KEY_CREATED_AT       = "createdAt";
    public static final String KEY_LOCAL_CREATED_AT = "localCreatedAt";
    public static final String KEY_OBJECT_ID        = "objectId";

    // Image dimensions
    public static final int DELAY = 7;

    // Image dimensions
    public static final int IMAGE_BORDER = 5;
    public static final int IMAGE_CANVAS = 350;

    // Installation Class
    public static final String KEY_INSTALLATION_USER = "user";

    // User Class
    public static final String CLASS_USER = "_User";

    public static final String KEY_USER_FUNCTION = "updateUser";
    public static final String KEY_USER_FUNCTION_USERNAME   = "username";
    public static final String KEY_USER_FUNCTION_PASSWORD   = "password";
    public static final String KEY_USER_FUNCTION_VERIF_CODE = "verifCode";
    public static final String KEY_USER_FUNCTION_CHECK_HAS_ACCEPTED     = "checkHasAccepted";
    public static final String KEY_USER_FUNCTION_ACCEPT_CONDITIONS      = "acceptConditions";

    public static final String KEY_USER_NUMBER_FRIENDS     = "nFriends";
    public static final String KEY_USER_PHOTO_ID           = "photoId";
    public static final String KEY_USER_TYPE               = "type";

    public static final String KEY_TYPE_REGULAR     = "regular";
    public static final String KEY_TYPE_EVENT       = "event";

    // Activity Class
    public static final String CLASS_ACTIVITY = "Activity";

    public static final String KEY_ACTIVITY_FROM_USERNAME     = "fromUsername";
    public static final String KEY_ACTIVITY_TYPE              = "type";
    public static final String KEY_ACTIVITY_TO_GROUPSELFIE_ID = "toGroupSelfieId";
    public static final String KEY_ACTIVITY_CONTENT           = "content";

    // Type values
    public static final String KEY_ACTIVITY_TYPE_CREATED         = "cr";
    public static final String KEY_ACTIVITY_TYPE_PARTICIPATED    = "pa";
    public static final String KEY_ACTIVITY_TYPE_COMMENTED       = "co";
    public static final String KEY_ACTIVITY_TYPE_VOTED           = "vo";
    public static final String KEY_ACTIVITY_TYPE_RE_VOTED        = "rv";

    // Relationship Class
    public static final String CLASS_RELATIONSHIP = "Relationship";

    public static final String KEY_RELATIONSHIP_FUNCTION           = "updateRelationships";
    public static final String KEY_RELATIONSHIP_FUNCTION_CONTACTS  = "contacts";
    public static final String KEY_RELATIONSHIP_SPONSORED_FUNCTION = "checkHasSponsoredRelationships";

    public static final String KEY_RELATIONSHIP_FROM_USER          = "fromUser";
    public static final String KEY_RELATIONSHIP_TO_USER_ID         = "toUserId";
    public static final String KEY_RELATIONSHIP_TO_USERNAME        = "toUsername";
    public static final String KEY_RELATIONSHIP_ACTIVE             = "active";

    public static final String KEY_RELATIONSHIP_TYPE               = "type";
    public static final String KEY_RELATIONSHIP_TYPE_SINGLE        = "s";
    public static final String KEY_RELATIONSHIP_TYPE_GROUP         = "g";

    public static final String KEY_RELATIONSHIP_NUMBER_COM_SELFIES       = "nComSelfies";
    public static final String KEY_RELATIONSHIP_LOCAL_NUMBER_COM_SELFIES = "local_nComSelfies";

    // GroupSelfie Class
    public static final String CLASS_GROUPSELFIE = "GroupSelfie";

    public static final String KEY_GROUPSELFIE_IMAGE            = "image";
    public static final String KEY_GROUPSELFIE_IMAGE_SMALL      = "imageSmall";
    public static final String KEY_GROUPSELFIE_IMAGE_RATIO      = "imageRatio";
    public static final String KEY_GROUPSELFIE_SENDER_USER      = "senderUser";
    public static final String KEY_GROUPSELFIE_LOCAL_CREATED_AT = "localCreatedAt";
    public static final String KEY_GROUPSELFIE_HASHTAG          = "hashtag";

    public static final String KEY_GROUPSELFIE_GROUP_IDS             = "groupIds";
    public static final String KEY_GROUPSELFIE_GROUP_USERNAMES       = "groupUsernames";
    public static final String KEY_GROUPSELFIE_SEEN_IDS              = "seenIds";
    public static final String KEY_GROUPSELFIE_LOCAL_SEEN_IDS        = "local_seenIds";
    public static final String KEY_GROUPSELFIE_IMPROVED_AT           = "improvedAt";
    public static final String KEY_GROUPSELFIE_LOCAL_IMPROVED_AT     = "local_improvedAt";

    public static final String KEY_GROUPSELFIE_FUNCTION_ADD_PARTICIPATED_ID   = "addParticipatedId";
    public static final String KEY_GROUPSELFIE_FUNCTION_ADD_SEEN_ID           = "addSeenId";
    public static final String KEY_GROUPSELFIE_FUNCTION_USER                  = "user";

    public static final String KEY_GROUPSELFIE_FUNCTION_ID         = "groupSelfie_id";

    // SingleSelfie Class
    public static final String CLASS_SINGLESELFIE = "SingleSelfie";

    public static final String KEY_SINGLESELFIE_IMAGE             = "image";
    public static final String KEY_SINGLESELFIE_NUMBER_OF_FACES   = "nFaces";
    public static final String KEY_SINGLESELFIE_TO_GROUPSELFIE_ID = "toGroupSelfieId";

    // Verification Class
    public static final String KEY_VERIFICATION_FUNCTION = "newVerifCodeForPhoneNumber";
    public static final String KEY_VERIFICATION_FUNCTION_PHONENUMBER = "phoneNumber";

    // Push Notification Payload

    public static final String KEY_PUSHPAYLOAD_PAYLOAD             = "p";
    public static final String KEY_PUSHPAYLOAD_PAYLOAD_ACTIVITY    = "a";
    //public static final String KEY_PUSHPAYLOAD_PAYLOAD_GROUPSELFIE = "gs";

    public static final String KEY_PUSHPAYLOAD_TYPE                 = "ty";
    public static final String KEY_PUSHPAYLOAD_TYPE_COMMENTED       = "co";
    public static final String KEY_PUSHPAYLOAD_TYPE_CREATED         = "cr";
    public static final String KEY_PUSHPAYLOAD_TYPE_PARTICIPATED    = "pa";
    public static final String KEY_PUSHPAYLOAD_TYPE_VOTED           = "vo";
    public static final String KEY_PUSHPAYLOAD_TYPE_REVOTED         = "rv";

    public static final String KEY_PUSHPAYLOAD_ID                = "id";
    public static final String KEY_PUSHPAYLOAD_FROM_USERNAME     = "fu";
    public static final String KEY_PUSHPAYLOAD_FROM_USER_ID      = "fui";
    public static final String KEY_PUSHPAYLOAD_TO_GROUPSELFIE_ID = "tg";
    public static final String KEY_PUSHPAYLOAD_CONTENT           = "co";
    public static final String KEY_PUSHPAYLOAD_CREATED_AT        = "ca";
    public static final String KEY_PUSHPAYLOAD_PARTICIPATED      = "pa";
    public static final String KEY_PUSHPAYLOAD_VOTE_IDS          = "vo";

    // SharedPreferences
    public static final String KEY_SHARED_PREFERENCES_PINNAMES   = "pinNames";
    public static final String KEY_SHARED_PREFERENCES_USERNAME   = "username";
    public static final String KEY_SHARED_PREFERENCES_VERIFYING  = "verfying";

}
