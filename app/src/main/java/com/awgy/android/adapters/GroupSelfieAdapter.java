package com.awgy.android.adapters;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.text.TextPaint;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import com.awgy.android.R;
import com.awgy.android.models.Activity;
import com.awgy.android.models.GroupSelfie;
import com.awgy.android.models.PhoneBook;
import com.awgy.android.utils.ClassAdapter;
import com.awgy.android.utils.Constants;
import com.google.common.base.Joiner;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import bolts.Continuation;
import bolts.Task;

public class GroupSelfieAdapter extends ClassAdapter<Activity, GroupSelfieAdapter.GroupSelfieViewHolder> {

    private boolean mOnlyOne;

    private GroupSelfie mGroupSelfie;

    private static final int SELF_TYPE = 0;
    private static final int OTHER_TYPE = 1;

    private static final int LABEL_SIZE = 12;

    public GroupSelfieAdapter(android.app.Activity activity, final GroupSelfie groupSelfie) {

        super(activity, new ClassAdapter.QueryFactory<Activity>() {
            @Override
            public ParseQuery<Activity> create() {
                ParseQuery<Activity> query = ParseQuery.getQuery(Activity.class);
                query.whereEqualTo(Constants.KEY_ACTIVITY_TYPE, Constants.KEY_ACTIVITY_TYPE_COMMENTED);
                query.whereEqualTo(Constants.KEY_ACTIVITY_TO_GROUPSELFIE_ID, groupSelfie.getObjectId());
                return query;
            }
        });

        mGroupSelfie = groupSelfie;

        setPinName(GroupSelfie.keyWithGroupSelfieId(groupSelfie.getObjectId()));
        setKeyName(GroupSelfie.keyWithGroupSelfieId(groupSelfie.getObjectId()));

        setPaginationEnabled(true);
        setObjectsPerPage(20);

        setIsLocalBuildable(true);
        setReversed(true);

        setEmptyTableViewImageResource(null);
        setEmptyTableViewLabelTitle(null);
        setEmptyTableViewLabelMessage(null);

        mOnlyOne = false;

        addOnQueryLoadListener(new OnQueryLoadListener() {
            @Override
            public void onLoading() {

            }

            @Override
            public void onLoaded(boolean cache) {
                boolean morePeople = false;
                for (Activity message : getObjects()) {
                    morePeople = !groupSelfie.getGroupUsernames().contains(message.getFromUsername());
                    if (morePeople) {
                        break;
                    }
                }

                mOnlyOne = ((groupSelfie.getGroupUsernames().size() <= 2) && !morePeople);

            }

            @Override
            public void onReloadData() {
                if (getReversed() && getCurrentPage() == 0 && !getHasAskedForMorePages()) {
                    getWeakRecyclerView().get().scrollToPosition(getObjects().size() - 1);
                }
            }

            @Override
            public void onDidCallNetwork() {

            }
        });
    }

    @Override
    public GroupSelfieViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == SELF_TYPE) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.groupselfie_self_list_item, parent, false);
            return new GroupSelfieViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.groupselfie_other_list_item, parent, false);
            return new GroupSelfieViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(GroupSelfieViewHolder holder, int position) {

        Activity message = getItem(position);
        Activity previousMessage = null;
        Activity nextMessage = null;

        if (position > 0) previousMessage = getItem(position-1);
        if (position < getObjects().size()-1) nextMessage = getItem(position+1);

        if (message != null) {
            holder.bindActivity(previousMessage, message, nextMessage);
        }
    }

    @Override
    public int getItemCount() {
        return getObjects().size();
    }

    @Override
    public int getItemViewType(int position) {
        Activity message = getItem(position);
        if (message != null) {
            if (message.getFromUsername() == null) {
                return SELF_TYPE;
            } else {
                if (message.getFromUsername().equals(ParseUser.getCurrentUser().getUsername())) {
                    return SELF_TYPE;
                } else {
                    return OTHER_TYPE;
                }
            }
        }
        return OTHER_TYPE;
    }

    public class GroupSelfieViewHolder extends RecyclerView.ViewHolder {

        public TextView mDateLabel;
        public TextView mNameLabel;
        public TextView mMessageLabel;
        public TextView mSeenLabel;

        public int mMaxWidth;

        public GroupSelfieViewHolder(View itemView) {
            super(itemView);

            mDateLabel = (TextView) itemView.findViewById(R.id.dateLabel);
            mNameLabel = (TextView) itemView.findViewById(R.id.nameLabel);

            mMessageLabel = (TextView) itemView.findViewById(R.id.messageLabel);
            mMaxWidth = 100;
            if (getActivity() != null) {
                Display display = getActivity().getWindowManager().getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);
                mMaxWidth = (int)Math.floor(0.75*size.x);
            }
            mMessageLabel.setMaxWidth(mMaxWidth);

            mSeenLabel = (TextView) itemView.findViewById(R.id.seenLabel);

        }

        public void bindActivity(Activity previousMessage, Activity message, Activity nextMessage) {

            int spacing_2 = 0;
            int spacing_5 = 0;
            int spacing_9 = 0;
            if (getActivity() != null) {
                spacing_2 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getActivity().getResources().getDisplayMetrics());
                spacing_5 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, getActivity().getResources().getDisplayMetrics());
            }

            String username = message.getFromUsername();
            if (username == null) username = ParseUser.getCurrentUser().getUsername();
            String previousUsername = null;
            if(previousMessage != null) previousUsername = previousMessage.getFromUsername();

            long date;
            if (message.getLocalCreatedAt() != null) {
                date = message.getLocalCreatedAt().getTime();
            } else {
                date = (new Date()).getTime();
            }

            long previousDate;
            if (previousMessage != null) {
                if (previousMessage.getLocalCreatedAt() != null) {
                    previousDate = previousMessage.getLocalCreatedAt().getTime();
                } else {
                    previousDate = (new Date()).getTime();
                }
            } else {
                previousDate = 0;
            }

            boolean isSelf = (username.equals(ParseUser.getCurrentUser().getUsername()));
            boolean isSame = false;
            if (previousUsername != null) {
                isSame = username.equals(previousUsername);
            }
            boolean isClose = (date-previousDate <= 30*60*1000);

            // Date
            if (isClose) {
                mDateLabel.setText(null);
                mDateLabel.setPadding(0, 0, 0, 0);
                mDateLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 0);
            } else {
                String format;
                Calendar calendarDate = Calendar.getInstance();
                calendarDate.setTimeInMillis(date);
                Calendar calendarNow = Calendar.getInstance();
                if (calendarNow.get(Calendar.DATE) == calendarDate.get(Calendar.DATE)) {
                    format = "hh:mm a";
                } else if ((new Date()).getTime()-date < 3600*1000*24*7) {
                    format = "EEEE hh:mm a";
                } else if (calendarNow.get(Calendar.YEAR) == calendarDate.get(Calendar.YEAR)) {
                    format = "EEE d MMM hh:mm a";
                } else {
                    format = "d MMM yyyy hh:mm a";
                }
                DateFormat df = new SimpleDateFormat(format);
                String convertedDate = df.format(date);
                mDateLabel.setText(convertedDate.substring(0, 1).toUpperCase() + convertedDate.substring(1));
                mDateLabel.setPadding(0, spacing_5, 0, spacing_5);
                mDateLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, LABEL_SIZE);
            }

            // Name
            if (isSame) {
                mNameLabel.setText(null);
                mNameLabel.setPadding(0, 0, 0, 0);
                mNameLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 0);
            } else {
                if (isSelf) {
                    mNameLabel.setText(null);
                    mNameLabel.setPadding(0, 0, 0, 0);
                    mNameLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 0);
                    if (isClose) {
                        mDateLabel.setPadding(0, spacing_5, 0, 0);
                    }
                } else {
                    if (!mOnlyOne) {
                        String name = PhoneBook.getInstance().getName(message.getFromUsername());
                        mNameLabel.setText(name);
                        mNameLabel.setPadding(0, 0, 0, spacing_2);
                        mNameLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, LABEL_SIZE);
                        if (isClose) {
                            mDateLabel.setPadding(0, spacing_5, 0, 0);
                        }
                    } else {
                        mNameLabel.setText(null);
                        mNameLabel.setPadding(0, 0, 0, 0);
                        mNameLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 0);
                        if (isClose) {
                            mDateLabel.setPadding(0, spacing_5, 0, 0);
                        }
                    }
                }
            }

            // Message
            mMessageLabel.setText(message.getContent());

            // Seen
            if (nextMessage == null) {
                if (message.getObjectId() != null) {
                    List<String> seenIds_others = new ArrayList<String>(mGroupSelfie.getSeenIds());
                    seenIds_others.remove(ParseUser.getCurrentUser().getObjectId());
                    List<String> groupIds_others = new ArrayList<String>(mGroupSelfie.getGroupIds());
                    groupIds_others.remove(ParseUser.getCurrentUser().getObjectId());

                    if (mGroupSelfie.getSeenIds().size() == 1 && mGroupSelfie.getSeenIds().get(0).equals(ParseUser.getCurrentUser().getObjectId())) {
                        mSeenLabel.setText(getActivity().getResources().getString(R.string.sent));
                        mSeenLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, LABEL_SIZE);
                    } else if (seenIds_others.size() == groupIds_others.size()) {
                        if (mOnlyOne) {
                            if (isSelf) {
                                mSeenLabel.setText(getActivity().getResources().getString(R.string.seen));
                                mSeenLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, LABEL_SIZE);
                            } else {
                                mSeenLabel.setText(null);
                                mSeenLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 0);
                            }
                        } else {
                            if (mGroupSelfie.getGroupIds().size() > 2) {
                                mSeenLabel.setText(getActivity().getResources().getString(R.string.seen_by_all));
                                mSeenLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, LABEL_SIZE);
                            } else {
                                mSeenLabel.setText(getActivity().getResources().getString(R.string.seen));
                                mSeenLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, LABEL_SIZE);
                            }
                        }
                    } else if (mGroupSelfie.getSeenIds().size() > 0) {
                        List<String> groupUsernames = mGroupSelfie.getGroupUsernames();
                        List<String> groupIds = mGroupSelfie.getGroupIds();

                        List<String> seenNames = new ArrayList<String>();

                        for (String seenId : mGroupSelfie.getSeenIds()) {
                            if (!seenId.equals(ParseUser.getCurrentUser().getObjectId())) {
                                int index = groupIds.indexOf(seenId);
                                if (index >= 0 && index < groupUsernames.size()) {
                                    String username_in = groupUsernames.get(index);
                                    if (!username_in.equals(message.getFromUsername())) {
                                        seenNames.add(PhoneBook.getInstance().getNameInitials(username_in));
                                    }
                                }
                            }
                        }
                        if (seenNames.size() > 0) {
                            mSeenLabel.setText(getActivity().getResources().getString(R.string.seen_by) + " " + Joiner.on(", ").join(seenNames));
                            mSeenLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, LABEL_SIZE);
                        } else {
                            mSeenLabel.setText(null);
                            mSeenLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 0);
                        }
                    } else {
                        mSeenLabel.setText(null);
                        mSeenLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 0);
                    }
                } else {
                    mSeenLabel.setText(getActivity().getResources().getString(R.string.sending));
                    mSeenLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, LABEL_SIZE);
                }
            } else {
                mSeenLabel.setText(null);
                mSeenLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 0);
            }

        }

    }

}






