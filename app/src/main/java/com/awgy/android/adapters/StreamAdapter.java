package com.awgy.android.adapters;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.format.DateUtils;
import android.text.style.ImageSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.awgy.android.AppDelegate;
import com.awgy.android.R;
import com.awgy.android.models.GroupSelfie;
import com.awgy.android.models.PhoneBook;
import com.awgy.android.ui.main.MainActivity;
import com.awgy.android.ui.stream.GroupSelfieActivity;
import com.awgy.android.utils.ClassAdapter;
import com.awgy.android.utils.Constants;
import com.awgy.android.views.PlaceHolderDrawable;
import com.parse.ParseFile;
import com.parse.ParseImageView;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import com.google.common.base.Joiner;

import bolts.Continuation;
import bolts.Task;

public class StreamAdapter extends ClassAdapter<GroupSelfie, StreamAdapter.StreamViewHolder> {

    private StreamAdapter.OnGroupSelfieEventListener mOnGroupSelfieEventListener;

    private final double[] dividers = {7*24*3600*1000,24*3600*1000,3600*1000,60*1000,1};
    private String[] suffix;

    public StreamAdapter(android.app.Activity activity) {
        this(activity, new ClassAdapter.QueryFactory<GroupSelfie>() {
            @Override
            public ParseQuery<GroupSelfie> create() {
                ParseQuery<GroupSelfie> query = ParseQuery.getQuery(GroupSelfie.class);  // Will do the job given the ACL
                query.whereExists(Constants.KEY_GROUPSELFIE_IMAGE);
                query.addDescendingOrder(Constants.KEY_GROUPSELFIE_IMPROVED_AT);
                return query;
            }
        });
    }

    public StreamAdapter(android.app.Activity activity, ClassAdapter.QueryFactory<GroupSelfie> queryFactory) {
        super(activity, queryFactory);

        setPinName(Constants.CLASS_GROUPSELFIE);
        setKeyName(Constants.CLASS_GROUPSELFIE);

        setIsLocalBuildable(true);
        setPaginationEnabled(true);
        setObjectsPerPage(25);

        String[] suffix_initialization = {AppDelegate.getContext().getResources().getString(R.string.week_abbr),
                AppDelegate.getContext().getResources().getString(R.string.day_abbr),
                AppDelegate.getContext().getResources().getString(R.string.hour_abbr),
                AppDelegate.getContext().getResources().getString(R.string.minute_abbr),
                "now"};

        suffix = suffix_initialization;

        setEmptyTableViewImageResource(R.drawable.stream_empty);
        setEmptyTableViewLabelTitle(AppDelegate.getContext().getResources().getString(R.string.empty_title_stream));
        setEmptyTableViewLabelMessage(AppDelegate.getContext().getResources().getString(R.string.empty_message_stream));

        addOnQueryLoadListener(new OnQueryLoadListener() {
            @Override
            public void onLoading() {
            }

            @Override
            public void onLoaded(boolean cache) {

                refreshCounterLabel();

                if (cache) {
                    Collections.sort(getObjects(), new Comparator<GroupSelfie>() {
                        public int compare(GroupSelfie groupSelfie1, GroupSelfie groupSelfie2) {
                            Date date1 = groupSelfie1.getMessagedAt();
                            Date date2 = groupSelfie2.getMessagedAt();
                            return date2.compareTo(date1);
                        }
                    });
                }

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

        if (MainActivity.badge_stream != null) {
            final int finalCount = count;
            if (MainActivity.getActivity() != null) {
                MainActivity.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (finalCount > 0) {
                            MainActivity.badge_stream.setText(finalCount + "");
                            MainActivity.badge_stream.setVisibility(View.VISIBLE);
                        } else {
                            MainActivity.badge_stream.setVisibility(View.INVISIBLE);
                        }
                    }
                });
            }
        }

    }

    @Override
    public StreamViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.stream_list_item, parent, false);
        return new StreamViewHolder(view);
    }

    @Override
    public void onBindViewHolder(StreamViewHolder holder, int position) {
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

        // Need to be improved
        NotificationManager notifManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        notifManager.cancelAll();

        GroupSelfie groupSelfie = getItem(position);

        if (groupSelfie != null) {

            if (getActivity() != null) {
                GroupSelfieActivity.setStaticGroupSelfie(groupSelfie);
                GroupSelfieActivity.getStaticGroupSelfieAdpater().loadCache().continueWithTask(new Continuation<Void, Task<Void>>() {
                    @Override
                    public Task<Void> then(Task<Void> task) throws Exception {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Intent intent = new Intent(getActivity(), GroupSelfieActivity.class);
                                getActivity().startActivity(intent);
                            }
                        });
                        return null;
                    }
                });
            }

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

    public class StreamViewHolder extends RecyclerView.ViewHolder {

        public GroupSelfie mGroupSelfie;

        public ParseImageView mParseImageView;
        public TextView mHashtagLabel;
        public TextView mGuestsLabel;
        public TextView mInfoLabel;

        public StreamViewHolder(View itemView) {
            super(itemView);

            mParseImageView = (ParseImageView) itemView.findViewById(R.id.imageSmall);

            mHashtagLabel = (TextView) itemView.findViewById(R.id.hashtagLabel);

            mGuestsLabel = (TextView) itemView.findViewById(R.id.guestsLabel);

            mInfoLabel = (TextView) itemView.findViewById(R.id.infoLabel);

        }

        public void bindGroupSelfie(GroupSelfie groupSelfie, boolean forceReload) {

            mGroupSelfie = groupSelfie;
            boolean needReload = mGroupSelfie.getByteImageSmall() == null || forceReload;

            WindowManager wm = (WindowManager)getActivity().getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);

            int border = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, getActivity().getResources().getDisplayMetrics());
            int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, getActivity().getResources().getDisplayMetrics());

            mParseImageView.getLayoutParams().width = size.x - 2 * border - 2 * margin;
            mParseImageView.getLayoutParams().height = (int)Math.ceil(mParseImageView.getLayoutParams().width/mGroupSelfie.getImageRatio());

            // Place Holder
            PlaceHolderDrawable placeHolderDrawable = null;
            if (getActivity() != null) {
                placeHolderDrawable = new PlaceHolderDrawable(getActivity(), mGroupSelfie);
                mParseImageView.setPlaceholder(placeHolderDrawable);
            }

            // Image if in stock
            byte[] byteImageSmall = mGroupSelfie.getByteImageSmall();
            if (byteImageSmall != null) {
                mParseImageView.setImageBitmap(BitmapFactory.decodeByteArray(byteImageSmall, 0, byteImageSmall.length));
            }

            // Reload if Needed
            final ParseFile imageFile = groupSelfie.getImageSmall();
            if (imageFile != null) {
                if (needReload) {
                    if (placeHolderDrawable != null) {
                        mParseImageView.setImageDrawable(placeHolderDrawable);
                    }
                    mParseImageView.setParseFile(imageFile);
                    mParseImageView.loadInBackground().continueWithTask(new Continuation<byte[], Task<Void>>() {
                        @Override
                        public Task<Void> then(Task<byte[]> task) throws Exception {
                            if (!task.isFaulted() && !task.isCancelled()) {
                                mGroupSelfie.setByteImageSmall(task.getResult());
                                if (mGroupSelfie.getDiscoveredIds() == null || !mGroupSelfie.getDiscoveredIds().contains(ParseUser.getCurrentUser().getObjectId())) mGroupSelfie.addDiscoveredId();
                            }
                            return null;
                        }
                    });
                }
            } else if (groupSelfie.getDuration() == 0.0) {
                mParseImageView.setParseFile(null);
                if (placeHolderDrawable != null) {
                    mParseImageView.setImageDrawable(placeHolderDrawable);
                }
            } else {
                mParseImageView.setParseFile(null);
                if (placeHolderDrawable != null) {
                    mParseImageView.setImageDrawable(placeHolderDrawable);
                }
            }

            // Hashtag
            mHashtagLabel.setText(String.format("#%s", groupSelfie.getHashtag()));

            // Guests

            List<String> names = new ArrayList<String>();

            for (String username : groupSelfie.getGroupUsernames()) {
                if (!username.equals(ParseUser.getCurrentUser().getUsername())) {
                    String name = PhoneBook.getInstance().getFirstName(username);
                    names.add(name);
                }
            }

            if (names.size() == 0) {
                names.add(AppDelegate.getContext().getResources().getString(R.string.only_you));
            }

            mGuestsLabel.setText(Joiner.on(", ").join(names));

            // Date
            refreshInfoLabel();

        }

        public void refreshInfoLabel() {

            if (mGroupSelfie != null) {

                int numberOfLoves;
                if (mGroupSelfie.getLoveIds() != null) {
                    numberOfLoves = mGroupSelfie.getLoveIds().size();
                } else {
                    numberOfLoves = 0;
                }
                int numberOfMessages = mGroupSelfie.getNMessages();

                String info = numberOfLoves + " ♥ " + numberOfMessages + " ✔   ";

                long pictureDate;
                int duration = 0;
                if (mGroupSelfie.getDuration() != null)
                    duration = mGroupSelfie.getDuration().intValue();
                if (mGroupSelfie.getLocalCreatedAt() != null) {
                    pictureDate = mGroupSelfie.getLocalCreatedAt().getTime() + duration * 1000;
                } else {
                    pictureDate = 0;
                }

                long interval = (new Date()).getTime()-pictureDate;

                int val = 0;
                int index;
                String dateFormatted = "now";
                for (index = 0; index < dividers.length; index++) {
                    val = (int)Math.floor(interval/dividers[index]);
                    if (val > 0) break;
                }
                if (index < dividers.length && index < suffix.length) {
                    dateFormatted = val + suffix[index];
                    if (index == dividers.length - 1) dateFormatted = suffix[index];
                }

                info += dateFormatted;

                StringBuilder infoBuilder = new StringBuilder(info);
                SpannableString spannableInfo = new SpannableString(info);

                int imageSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getActivity().getResources().getDisplayMetrics());

                while (infoBuilder.indexOf("♥") > -1) {
                    index = infoBuilder.indexOf("♥");
                    Drawable heartDrawable = getActivity().getResources().getDrawable(R.drawable.heart_grey);
                    if (heartDrawable != null) heartDrawable.setBounds(0, 0, imageSize, imageSize);
                    ImageSpan heart = new ImageSpan(heartDrawable, ImageSpan.ALIGN_BASELINE);
                    spannableInfo.setSpan(heart, index, index+1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                    infoBuilder.setCharAt(index, ' ');
                }

                while (infoBuilder.indexOf("✔") > -1) {
                    index = infoBuilder.indexOf("✔");
                    Drawable checkDrawable;
                    if (mGroupSelfie.getSeenIds().contains(ParseUser.getCurrentUser().getObjectId())) {
                        checkDrawable = getActivity().getResources().getDrawable(R.drawable.comment);
                    } else {
                        checkDrawable = getActivity().getResources().getDrawable(R.drawable.comment_on);
                    }
                    if (checkDrawable != null) checkDrawable.setBounds(0, 0, imageSize, imageSize);
                    ImageSpan check = new ImageSpan(checkDrawable, ImageSpan.ALIGN_BASELINE);
                    spannableInfo.setSpan(check, index, index+1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                    infoBuilder.setCharAt(index, ' ');
                }

                mInfoLabel.setText(spannableInfo);

            }

        }

    }

    public void refreshInfoLabel() {

        for (int index = 0; index < getObjects().size(); index++) {
            StreamAdapter.StreamViewHolder streamViewHolder = (StreamAdapter.StreamViewHolder) getWeakRecyclerView().get().findViewHolderForAdapterPosition(index);
            if (streamViewHolder != null) {
                streamViewHolder.refreshInfoLabel();
            }
        }

    }

    public void manageGroupSelfie(final GroupSelfie groupSelfie, boolean moveTop, final boolean onlyInfo) {

        if (groupSelfie.getImage() != null) {

            final int index = getObjects().indexOf(groupSelfie);

            if (index >= 0) {

                // Already in

                if (index == 0) moveTop = false;

                if (moveTop) {

                    getObjects().remove(index);
                    getObjects().add(0, groupSelfie);

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                notifyItemChanged(index);
                                notifyItemMoved(index, 0);
                                getWeakRecyclerView().get().scrollToPosition(0);

                                // or
                                //notifyDataSetChanged();
                            }
                        });
                    }

                } else {

                    final StreamViewHolder viewHolder = (StreamViewHolder) getWeakRecyclerView().get().findViewHolderForAdapterPosition(index);
                    if (viewHolder != null) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (onlyInfo) {
                                        Log.d("checkPush","onlyInfo");
                                        viewHolder.refreshInfoLabel();
                                    } else {
                                        Log.d("checkPush","else");
                                        viewHolder.bindGroupSelfie(groupSelfie, true);
                                    }
                                }
                            });
                        }
                    }

                }

            } else {

                // Is new here

                getObjects().add(0, groupSelfie);

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
                            notifyItemInserted(0);
                            if (needRemove) notifyItemRemoved(indexRemove);
                            getWeakRecyclerView().get().scrollToPosition(0);
                        }
                    });
                }

            }

            refreshCounterLabel();

        }


    }

    // Listeners

    public interface OnGroupSelfieEventListener {
        void onAddGroupSelfie(GroupSelfie groupSelfie);
        void onDeleteGroupSelfie(GroupSelfie groupSelfie);
        void onCountGroupSelfies(int count);
    }

    public void setOnGroupSelfieEventListener(StreamAdapter.OnGroupSelfieEventListener onGroupSelfieEventListener) {
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






