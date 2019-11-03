package com.awgy.android.adapters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import android.content.Intent;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.awgy.android.AppDelegate;
import com.awgy.android.R;
import com.awgy.android.models.GroupSelfie;
import com.awgy.android.models.PhoneBook;
import com.awgy.android.ui.camera.CameraActivity;
import com.awgy.android.ui.main.MainActivity;
import com.awgy.android.utils.ClassAdapter;
import com.awgy.android.utils.Constants;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import com.google.common.base.Joiner;

import bolts.Continuation;
import bolts.Task;

public class OngoingAdapter extends ClassAdapter<GroupSelfie, OngoingAdapter.OngoingViewHolder> {

    private OngoingAdapter.OnGroupSelfieEventListener mOnGroupSelfieEventListener;

    public OngoingAdapter(android.app.Activity activity) {
        this(activity, new ClassAdapter.QueryFactory<GroupSelfie>() {
            @Override
            public ParseQuery<GroupSelfie> create() {
                ParseQuery<GroupSelfie> query = ParseQuery.getQuery(GroupSelfie.class);  // Will do the job given the ACL
                query.whereExists(Constants.KEY_GROUPSELFIE_IMAGE);
                return query;
            }
        });
    }

    public OngoingAdapter(android.app.Activity activity, ClassAdapter.QueryFactory<GroupSelfie> queryFactory) {
        super(activity, queryFactory);

        setPinName(Constants.CLASS_GROUPSELFIE);
        setKeyName(Constants.CLASS_GROUPSELFIE);

        setIsLocalBuildable(true);
        setPaginationEnabled(true);
        setObjectsPerPage(25);

        setEmptyTableViewImageResource(R.drawable.ongoing_empty);
        setEmptyTableViewLabelTitle(AppDelegate.getContext().getResources().getString(R.string.empty_title_ongoing));
        setEmptyTableViewLabelMessage(AppDelegate.getContext().getResources().getString(R.string.empty_message_ongoing));


        addOnQueryLoadListener(new OnQueryLoadListener() {
            @Override
            public void onLoading() {
            }

            @Override
            public void onLoaded(boolean cache) {

                orderObjects();
                refreshCounterLabel();

                for (final GroupSelfie groupSelfie : getObjects()) {
                    groupSelfie.syncCountDown();
                }

            }

            @Override
            public void onReloadData() {
            }

            @Override
            public void onDidCallNetwork() {
            }

        });

    }

    public void refreshCounterLabel() {

        int count = 0;
        for (GroupSelfie groupSelfie : getObjects()) {
            if (!groupSelfie.getSeenIds().contains(ParseUser.getCurrentUser().getObjectId())) {
                count++;
            }
        }

        if (MainActivity.badge_ongoing != null) {
            final int finalCount = count;
            if (MainActivity.getActivity() != null) {
                MainActivity.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (finalCount > 0) {
                            MainActivity.badge_ongoing.setText(finalCount + "");
                            MainActivity.badge_ongoing.setVisibility(View.VISIBLE);
                        } else {
                            MainActivity.badge_ongoing.setVisibility(View.INVISIBLE);
                        }
                    }
                });
            }
        }

    }

    public void orderObjects() {

        Collections.sort(getObjects(), new Comparator<GroupSelfie>() {
            public int compare(GroupSelfie groupSelfie1, GroupSelfie groupSelfie2) {

                Integer bool1 = (groupSelfie1.getSeenIds().contains(ParseUser.getCurrentUser().getObjectId())) ? 1 : 0;
                Integer bool2 = (groupSelfie2.getSeenIds().contains(ParseUser.getCurrentUser().getObjectId())) ? 1 : 0;
                if (!bool1.equals(bool2)) {
                    return bool1.compareTo(bool2);
                } else {
                    long interval1 = (new Date()).getTime()-groupSelfie1.getCreatedAt().getTime();
                    Double remaining1 = groupSelfie1.getDuration()*1000-interval1;
                    long interval2 = (new Date()).getTime()-groupSelfie2.getCreatedAt().getTime();
                    Double remaining2 = groupSelfie2.getDuration()*1000-interval2;
                    return remaining1.compareTo(remaining2);
                }
            }
        });

    }

    @Override
    public OngoingViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.ongoing_list_item, parent, false);
        return new OngoingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(OngoingViewHolder holder, int position) {
        GroupSelfie groupSelfie = getItem(position);
        if (groupSelfie != null) {
            holder.bindGroupSelfie(groupSelfie, false);
        }
    }

    @Override
    public int getItemCount() {
        if (getObjects() != null) {
            return getObjects().size();
        } else {
            return 0;
        }
    }

    @Override
    public void onItemClicked(int position) {

        GroupSelfie groupSelfie = getItem(position);

        if (groupSelfie.getSeenIds() == null || !groupSelfie.getSeenIds().contains(ParseUser.getCurrentUser().getObjectId())) {
            CameraActivity.setStaticGroupSelfie(groupSelfie);
            CameraActivity.setNeedResetBitmap(true);
            CameraActivity.setNeedResetGroupSelfie(false);
            CameraActivity.setDelegate(MainActivity.getCameraDelegate());
            Intent cameraActivityIntent = new Intent(getActivity(), CameraActivity.class);
            getActivity().startActivity(cameraActivityIntent);

        }

    }

    @Override
    public boolean canDismissItem(int position) {
        return true;
    }

    @Override
    public void onItemDismiss(final int position) {

        if (isNetworkAvailable()) {

            final GroupSelfie groupSelfie = getItem(position);

            if (groupSelfie != null) {

                getObjects().remove(position);
                notifyItemRemoved(position);
                refreshCounterLabel();

                notifyOnDeleteGroupSelfieListener(groupSelfie);

                groupSelfie.deleteGroupSelfie().continueWithTask(new Continuation<Void, Task<Void>>() {
                    @Override
                    public Task<Void> then(Task<Void> task) throws Exception {

                        if (!task.isFaulted() && !task.isCancelled()) {

                            checkIfEnoughCells();

                            if (getActivity() != null) {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        refreshEmptyView();
                                        refreshCounterLabel();
                                    }
                                });
                            }

                        } else {

                            getObjects().add(position, groupSelfie);

                            notifyOnAddGroupSelfieListener(groupSelfie);

                            if (getActivity() != null) {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        notifyItemInserted(position);
                                        refreshEmptyView();
                                        refreshCounterLabel();
                                    }
                                });
                            }

                        }

                        return null;
                    }
                });

            }

        }

    }

    public class OngoingViewHolder extends RecyclerView.ViewHolder {

        public GroupSelfie mGroupSelfie;

        public TextView mHashtagLabel;
        public TextView mGuestsLabel;
        public ImageView mActionImageView;
        public TextView mRemainingLabel;

        public OngoingViewHolder(View itemView) {
            super(itemView);


            mHashtagLabel = (TextView) itemView.findViewById(R.id.hashtagLabel);
            mGuestsLabel = (TextView) itemView.findViewById(R.id.guestsLabel);
            mActionImageView = (ImageView) itemView.findViewById(R.id.actionImage);
            mRemainingLabel = (TextView) itemView.findViewById(R.id.remainingLabel);

        }

        public void bindGroupSelfie(GroupSelfie groupSelfie, boolean forceReload) {

            mGroupSelfie = groupSelfie;

            // Hashtag
            mHashtagLabel.setText(String.format("#%s", groupSelfie.getHashtag()));

            // Guests - Waiting

            List<String> seenIds = mGroupSelfie.getSeenIds();
            List<String> groupIds = mGroupSelfie.getGroupIds();
            List<String> groupUsernames = mGroupSelfie.getGroupUsernames();


            List<String> names = new ArrayList<String>();
            List<String> ids = new ArrayList<String>();
            names.add(AppDelegate.getContext().getResources().getString(R.string.you));
            ids.add(ParseUser.getCurrentUser().getObjectId());
            for (int i = 0; i < groupUsernames.size(); i++) {
                if (!groupUsernames.get(i).equals(ParseUser.getCurrentUser().getUsername())) {
                    String name = PhoneBook.getInstance().getName(groupUsernames.get(i));
                    names.add(name);
                    if (i < groupIds.size()) { ids.add(groupIds.get(i)); } else { ids.add("---"); }
                }
            }

            SpannableString spannableString = new SpannableString(Joiner.on(", ").join(names));

            int begin;
            int end = -2;
            for (int index = 0; index < names.size(); index++) {
                begin = end + 2;
                end = begin + names.get(index).length();
                if (seenIds == null || !seenIds.contains(ids.get(index))) {
                    spannableString.setSpan(new ForegroundColorSpan(getActivity().getResources().getColor(R.color.notDoneGray)), begin, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }

            mGuestsLabel.setText(spannableString);

            // Date
            refreshRemainingLabel();

        }

        public void refreshRemainingLabel() {

            long interval = (new Date()).getTime() - mGroupSelfie.getLocalCreatedAt().getTime();
            int minutes = (int) Math.floor(mGroupSelfie.getDuration() / 60.0) - (int) Math.floor(interval / 1000.0 / 60.0);
            if (minutes < 0) minutes = 0;
            if (mGroupSelfie.getSeenIds() != null && mGroupSelfie.getSeenIds().contains(ParseUser.getCurrentUser().getObjectId())) {

                mActionImageView.setImageResource(R.drawable.check);
                mRemainingLabel.setText(String.format("%dmin", minutes));
            } else {
                mActionImageView.setImageResource(R.drawable.camera_color);
                mRemainingLabel.setText(String.format("%dmin", minutes));
            }

        }


    }

    public void refreshRemainingsLabel() {

        for (int index = 0; index < getObjects().size(); index++) {
            OngoingAdapter.OngoingViewHolder ongoingViewHolder = (OngoingAdapter.OngoingViewHolder) getWeakRecyclerView().get().findViewHolderForAdapterPosition(index);
            if (ongoingViewHolder != null) {
                ongoingViewHolder.refreshRemainingLabel();
            }
        }

    }

    public void manageGroupSelfie(final GroupSelfie groupSelfie) {

        final int index = getObjects().indexOf(groupSelfie);

        if (index >= 0) {

            if (groupSelfie.getImage() != null || groupSelfie.getDuration() == 0.0) {

                // Remove
                getObjects().remove(index);
                setLastLoadCount(-1);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            notifyItemRemoved(index);
                            refreshEmptyView();
                        }
                    });
                }

            } else {

                // Update

                final OngoingViewHolder viewHolder = (OngoingViewHolder) getWeakRecyclerView().get().findViewHolderForAdapterPosition(index);
                if (viewHolder != null) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                viewHolder.bindGroupSelfie(groupSelfie, true);
                            }
                        });
                    }
                }
            }

        } else {

            if (groupSelfie.getImage() == null) {

                // Add

                getObjects().add(0, groupSelfie);
                orderObjects();
                final int index_in = getObjects().indexOf(groupSelfie);

                final int indexRemove = getObjects().size() - 1;
                final boolean needRemove = (indexRemove + 1 > getObjectsPerPage());
                if (needRemove) {
                    getObjects().remove(indexRemove);
                    setLastLoadCount(-1);
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            refreshEmptyView();
                            if (index_in >= 0) notifyItemInserted(index_in);
                            if (needRemove) notifyItemRemoved(indexRemove);
                            getWeakRecyclerView().get().scrollToPosition(0);
                        }
                    });
                }

            }

        }

        refreshCounterLabel();


    }

    // Listeners

    public interface OnGroupSelfieEventListener {
        void onAddGroupSelfie(GroupSelfie groupSelfie);
        void onDeleteGroupSelfie(GroupSelfie groupSelfie);
        void onCountGroupSelfies(int count);
    }

    public void setOnGroupSelfieEventListener(OngoingAdapter.OnGroupSelfieEventListener onGroupSelfieEventListener) {
        mOnGroupSelfieEventListener = onGroupSelfieEventListener;
    }

    public void notifyOnAddGroupSelfieListener(GroupSelfie groupSelfie) {
        if (mOnGroupSelfieEventListener != null) {
            mOnGroupSelfieEventListener.onAddGroupSelfie(groupSelfie);
        }
    }

    public void notifyOnDeleteGroupSelfieListener(GroupSelfie groupSelfie) {
        if (mOnGroupSelfieEventListener != null) {
            mOnGroupSelfieEventListener.onDeleteGroupSelfie(groupSelfie);
        }
    }

    public void notifyOnCountGroupSelfiesListener(int count) {
        if (mOnGroupSelfieEventListener != null) {
            mOnGroupSelfieEventListener.onCountGroupSelfies(count);
        }
    }



}






