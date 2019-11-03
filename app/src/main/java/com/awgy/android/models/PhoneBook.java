package com.awgy.android.models;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.support.v4.content.LocalBroadcastManager;

import com.awgy.android.AppDelegate;
import com.awgy.android.utils.Constants;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PhoneBook {

    private String mPhoneNumber;
    private String mRegionCode;

    private PhoneNumberUtil mPhoneUtil;

    private Map<String, String> mNames;
    private Map<String, String> mFirstNames;
    private Map<String, String> mNamesShort;
    private Map<String, String> mNamesInitials;

    private List<String> mOrderedPhoneNumbers;

    private Map<String, Long> mIds;
    private Map<String, String> mKeys;


    private static final PhoneBook mInstance = new PhoneBook();
    public static PhoneBook getInstance() {
        return mInstance;
    }

    private PhoneBook () {
        mNames = new HashMap<String, String>();
        mFirstNames = new HashMap<String, String>();
        mNamesShort = new HashMap<String, String>();
        mNamesInitials = new HashMap<String, String>();

        mOrderedPhoneNumbers = new ArrayList<String>();

        mIds = new HashMap<String, Long>();
        mKeys = new HashMap<String, String>();

        mPhoneUtil = PhoneNumberUtil.getInstance();
    }


    public void setPhoneNumber(String phoneNumber) {

        mPhoneNumber = phoneNumber;

        try {
            Phonenumber.PhoneNumber number = mPhoneUtil.parse(String.format("+%s", phoneNumber), "");
            mRegionCode = mPhoneUtil.getRegionCodeForCountryCode(number.getCountryCode());
        } catch (NumberParseException e) {
            System.err.println("NumberParseException was thrown: " + e.toString());
        }


    }

    public String getName(String username) {

        String name = mNames.get(username);
        if (name == null) {
            if(isNumeric(username)) {
                name = String.format("+%s",username);
            } else {
                name = username;
            }
        }
        return name;

    }

    public String getFirstName(String username) {

        String firstName = mFirstNames.get(username);
        if (firstName == null) {
            if(isNumeric(username)) {
                firstName = String.format("+%s", username);
            } else {
                firstName = username;
            }
        }
        return firstName;

    }

    public String getNameShort(String username) {

        String nameShort = mNamesShort.get(username);
        if (nameShort == null) {
            if(isNumeric(username)) {
                nameShort = String.format("+%s", username);
            } else {
                nameShort = username;
            }
        }
        return nameShort;

    }

    public String getNameInitials(String username) {

        String nameInitials = mNamesInitials.get(username);
        if (nameInitials == null) {
            if(isNumeric(username)) {
                nameInitials = username.substring(username.length() - 2, username.length());
            } else {
                nameInitials = "";
                String[] components = username.split("\\W");
                for (String component : components) {
                    if (component.length() > 0) {
                        nameInitials += component.substring(0, 1);
                    }
                }
            }
        }
        return nameInitials;

    }

    public List<String> getOrderedPhoneNumbers() {
        return mOrderedPhoneNumbers;
    }


    public Map<String, String> getNames() {
        return mNames;
    }

    public Long getId(String phoneNUmber) {
        return mIds.get(phoneNUmber);
    }

    public String getKey(String phoneNUmber) {
        return mKeys.get(phoneNUmber);
    }

    public void fetchPhoneBook() {

        mNames.clear();
        mFirstNames.clear();
        mNamesShort.clear();
        mNamesInitials.clear();
        mOrderedPhoneNumbers.clear();

        mIds.clear();
        mKeys.clear();

        ContentResolver cr = AppDelegate.getContext().getContentResolver();

        String whereName = Data.MIMETYPE + " = ?";
        String[] whereNameParams = new String[] { StructuredName.CONTENT_ITEM_TYPE };
        Cursor cursor = cr.query(Data.CONTENT_URI, null, whereName, whereNameParams, StructuredName.GIVEN_NAME);

        //Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);

        if (cursor.getCount() > 0) {
            while (cursor.moveToNext()) {

                //Long id = cursor.getLong(cursor.getColumnIndex(StructuredName.RAW_CONTACT_ID));

                String key = cursor.getString(cursor.getColumnIndex(StructuredName.LOOKUP_KEY));
                String display = cursor.getString(cursor.getColumnIndex(StructuredName.DISPLAY_NAME));
                String given = cursor.getString(cursor.getColumnIndex(StructuredName.GIVEN_NAME));
                String family = cursor.getString(cursor.getColumnIndex(StructuredName.FAMILY_NAME));

                String name;
                if (!(given == null || given.length() == 0) && (family == null || family.length() == 0)) {
                    name = given;
                } else if ((given == null || given.length() == 0) && !(family == null || family.length() == 0)) {
                    name = family;
                    given = family;
                } else if (!(given == null || given.length() == 0) && !(family == null || family.length() == 0)) {
                    name = given + " " + family;
                } else {
                    if (!(display == null || display.length() == 0)) {
                        name = display;
                        given = display;
                    } else {
                        name = "No Name";
                        given = "No Name";
                    }
                }

                /*Long id = cursor.getLong(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                String key = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
                String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                if (name == null || name.length() == 0) name = "No Name";*/

                String[] componentsShort = name.split(" ");
                String nameShort = componentsShort[0];
                if (componentsShort.length > 1) {
                    nameShort += " ";
                    for (int i = 1; i < componentsShort.length; i++) {
                        String component = componentsShort[i];
                        if (component.length() > 0) {
                            nameShort += component.substring(0, 1);
                        }
                    }
                }

                String nameInitials = "";
                String[] componentsInitials = name.split("\\W");
                for (String component : componentsInitials) {
                    if (component.length() > 0) {
                        nameInitials += component.substring(0, 1);
                    }
                }

                if (Integer.parseInt(cursor.getString(cursor.getColumnIndex(StructuredName.HAS_PHONE_NUMBER))) > 0) {

                    Cursor phoneCursor = cr.query(Phone.CONTENT_URI, null, Phone.LOOKUP_KEY + " = ?", new String[]{key}, null);

                    while (phoneCursor.moveToNext()) {
                        String phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(Phone.NUMBER));

                        try {

                            Phonenumber.PhoneNumber number = mPhoneUtil.parse(phoneNumber,mRegionCode);
                            phoneNumber = mPhoneUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.E164).replaceAll("[^A-Za-z0-9 ]", "");

                            mNames.put(phoneNumber, name);
                            mFirstNames.put(phoneNumber, given);
                            mNamesShort.put(phoneNumber, nameShort);
                            mNamesInitials.put(phoneNumber, nameInitials);

                            if(!mOrderedPhoneNumbers.contains(phoneNumber)) mOrderedPhoneNumbers.add(phoneNumber);

                            mIds.put(phoneNumber, phoneCursor.getLong(phoneCursor.getColumnIndex(Phone.CONTACT_ID)));
                            mKeys.put(phoneNumber, key);

                        } catch (NumberParseException e) {
                            System.err.println("NumberParseException was thrown: " + e.toString());
                        }

                    }
                    phoneCursor.close();
                }
            }
        }
        cursor.close();

        Collections.sort(mOrderedPhoneNumbers, new Comparator<String>() {
            public int compare(String username1, String username2) {
                String name1 = mNames.get(username1);
                if (name1 == null) name1 = String.format("+%s", username1);
                String name2 = mNames.get(username2);
                if (name2 == null) name2 = String.format("+%s", username2);
                return name1.compareTo(name2);
            }
        });

        Intent intent = new Intent(Constants.PHONEBOOK_PHONEBOOK_HAS_BEEN_UPDATED_NOTIFICATION);
        LocalBroadcastManager.getInstance(AppDelegate.getContext()).sendBroadcast(intent);

    }

    public void clear() {

        mNames.clear();
        mFirstNames.clear();
        mNamesShort.clear();
        mNamesInitials.clear();
        mOrderedPhoneNumbers.clear();

        mIds.clear();
        mKeys.clear();

        mPhoneNumber = null;
        mRegionCode = null;

    }


    public static boolean isNumeric(String str) {
        try {
            double d = Double.parseDouble(str);
        } catch(NumberFormatException nfe) {
            return false;
        }
        return true;
    }

}

