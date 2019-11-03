package com.awgy.android.adapters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.awgy.android.AppDelegate;
import com.awgy.android.R;
import com.awgy.android.models.PhoneBook;
import com.awgy.android.models.Relationship;
import com.awgy.android.utils.ClassAdapter;
import com.awgy.android.utils.Constants;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import bolts.Continuation;
import bolts.Task;

public class RecipientsAdapter extends ClassAdapter<Relationship, RecyclerView.ViewHolder> {

    private List<Relationship> mGroup;
    public List<Relationship> getGroup() {
        return mGroup;
    }

    private List<Integer> mMap;

    private static List<Relationship> staticSponsored;
    public static void setStaticSponsored(List<Relationship> sponsored) {
        staticSponsored = sponsored;
    }

    private static final int HEADER_1_TYPE = 0;
    private static final int HEADER_2_TYPE = 1;
    private static final int ON_APP_TYPE = 2;
    private static final int NOT_ON_APP_TYPE = 3;

    public RecipientsAdapter(Activity activity) {
        super(activity, new ClassAdapter.QueryFactory<Relationship>() {
            @Override
            public ParseQuery<Relationship> create() {
                ParseQuery<Relationship> query = ParseQuery.getQuery(Relationship.class);
                query.whereEqualTo(Constants.KEY_RELATIONSHIP_FROM_USER, ParseUser.getCurrentUser());
                query.setLimit(1000);
                return query;
            }
        });

        mGroup = new ArrayList<Relationship>();
        mMap = new ArrayList<Integer>();

        setPinName(Constants.CLASS_RELATIONSHIP);
        setKeyName(Constants.CLASS_RELATIONSHIP);

        setPaginationEnabled(false);

        setCacheSubsetKey(Constants.KEY_RELATIONSHIP_ACTIVE);
        setCacheSubsetInverted(false);
        setCacheSubsetInPhoneBook(true);

        setEmptyTableViewImageResource(null);
        setEmptyTableViewLabelTitle(null);
        setEmptyTableViewLabelMessage(null);

        addOnQueryLoadListener(new OnQueryLoadListener() {
            @Override
            public void onLoading() {

            }

            @Override
            public void onLoaded(boolean cache) {
                Collections.sort(getObjects(), new Comparator<Relationship>() {
                    public int compare(Relationship rel1, Relationship rel2) {

                        if (rel1.getNComSelfies().intValue() != rel2.getNComSelfies().intValue()) {
                            return (rel1.getNComSelfies().intValue() < rel2.getNComSelfies().intValue() ? 1 : -1);
                        } else {
                            String name1 = PhoneBook.getInstance().getName(rel1.getToUsername());
                            String name2 = PhoneBook.getInstance().getName(rel2.getToUsername());
                            return name1.compareToIgnoreCase(name2);
                        }

                    }

                });

                // Add Sponors

                List<Relationship> reversedSponsored = new ArrayList<Relationship>(staticSponsored);
                Collections.reverse(reversedSponsored);
                for (Relationship sponsor : reversedSponsored) {
                    getObjects().add(0,sponsor);
                }

                // Add Others

                List<String> alreadyIn = new ArrayList<String>();
                for (Relationship relationship : getObjects()) alreadyIn.add(relationship.getToUsername());
                alreadyIn.add(ParseUser.getCurrentUser().getUsername());
                for (String username :PhoneBook.getInstance().getOrderedPhoneNumbers()) {
                    if (!alreadyIn.contains(username)) {
                        Relationship relatioship = new Relationship();
                        relatioship.setToUsername(username);
                        relatioship.setToUserId("---");
                        relatioship.setActive(true);
                        relatioship.setNComSelfies(0,true);
                        getObjects().add(relatioship);
                    }
                }

                // Mapping
                mMap.clear();
                for (Integer index = 0; index < getObjects().size(); index++) {
                    mMap.add(index);
                }

            }

            @Override
            public void onReloadData() {

            }

            @Override
            public void onDidCallNetwork() {

                Relationship.updateRelationships(getObjects()).continueWithTask(new Continuation<Void, Task<Void>>() {
                    @Override
                    public Task<Void> then(Task<Void> task) throws Exception {
                        if (!task.isFaulted() && !task.isCancelled()) {
                            loadCache();
                        }
                        return null;
                    }
                });

            }
        });
    }

    public void clear() {
        mGroup.clear();
        mapping();
    }

    public void initialize(List<String> groupUsernames) {
        mGroup.clear();
        for (Relationship relationship : getObjects()) {
            if (groupUsernames.contains(relationship.getToUsername())) {
                mGroup.add(relationship);
            }
        }
        mapping();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == HEADER_1_TYPE) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.header_1_list_item, parent, false);
            return new HeaderViewHolder(view);
        } else if (viewType == HEADER_2_TYPE) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.header_2_list_item, parent, false);
            return new HeaderViewHolder(view);
        } else if (viewType == ON_APP_TYPE) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.users_list_item, parent, false);
            return new OnAppViewHolder(view);
        } else if (viewType == NOT_ON_APP_TYPE) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.non_users_list_item, parent, false);
            return new NotOnAppViewHolder(view);
        } else {
            return null;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (position == 0) {
            ((HeaderViewHolder)holder).mTitleLabel.setText(getActivity().getResources().getString(R.string.invited));
        } else if (position == mGroup.size()+1) {
            ((HeaderViewHolder)holder).mTitleLabel.setText(getActivity().getResources().getString(R.string.contacts));
        } else {
            Relationship relationship = getItem(position);
            if (relationship != null) {
                if (!relationship.getToUserId().equals("---")) {
                    ((OnAppViewHolder) holder).bindRelationship(relationship);
                } else {
                    ((NotOnAppViewHolder) holder).bindRelationship(relationship);
                }
            }
        }
    }

    @Override
    public Relationship getItem(int position) {
        try {
            if (position < mGroup.size() + 1) {
                return mGroup.get(position - 1);
            } else if (position > mGroup.size() + 1) {
                Integer index = mMap.get(position - 2 - mGroup.size());
                return getObjects().get(index);
            }
            return null;
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    public void mapping() {
        mMap.clear();
        Integer index = 0;
        for (Relationship relationship : getObjects()) {
            if (!mGroup.contains(relationship)) {
                mMap.add(index);
            }
            index++;
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return HEADER_1_TYPE;
        } else if (position == mGroup.size()+1) {
            return HEADER_2_TYPE;
        } else {
            Relationship relationship = getItem(position);
            if (relationship != null) {
                if (!relationship.getToUserId().equals("---")) {
                    return ON_APP_TYPE;
                } else {
                    return NOT_ON_APP_TYPE;
                }
            } else {
                return ON_APP_TYPE;
            }
        }
    }

    @Override
    public int getItemCount() {
        return getObjects().size()+2;
    }


    public class HeaderViewHolder extends RecyclerView.ViewHolder {

        public TextView mTitleLabel;

        public HeaderViewHolder(View itemView) {
            super(itemView);

            mTitleLabel = (TextView) itemView.findViewById(R.id.titleLabel);

        }

    }



    public class OnAppViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public Relationship mRelationship;
        public TextView mNameLabel;
        public TextView mNumberLabel;

        public OnAppViewHolder(View itemView) {
            super(itemView);

            mNameLabel = (TextView) itemView.findViewById(R.id.nameLabel);
            mNumberLabel = (TextView) itemView.findViewById(R.id.numberLabel);

            itemView.setOnClickListener(this);
        }

        public void bindRelationship(Relationship relationship) {

            mRelationship = relationship;

            String name = PhoneBook.getInstance().getName(relationship.getToUsername());

            mNameLabel.setText(name);

            if (relationship.getToType().equals(Constants.KEY_TYPE_EVENT)) {
                mNameLabel.setTextColor(getActivity().getResources().getColor(R.color.color1));
            } else {
                mNameLabel.setTextColor(getActivity().getResources().getColor(R.color.color2));
            }

            mNumberLabel.setText(relationship.getNComSelfies().intValue() + "");

        }

        @Override
        public void onClick(View v) {
            onClickRelationship(mRelationship);
        }
    }

    public class NotOnAppViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public Relationship mRelationship;
        public TextView mNameLabel;
        public TextView mNumberLabel;
        public TextView mPhoneNumberLabel;

        public NotOnAppViewHolder(View itemView) {
            super(itemView);

            mNameLabel = (TextView) itemView.findViewById(R.id.nameLabel);
            mNumberLabel = (TextView) itemView.findViewById(R.id.numberLabel);
            mPhoneNumberLabel = (TextView) itemView.findViewById(R.id.phoneNumberLabel);

            itemView.setOnClickListener(this);
        }

        public void bindRelationship(Relationship relationship) {

            mRelationship = relationship;

            String name = PhoneBook.getInstance().getName(relationship.getToUsername());

            mNameLabel.setText(name);
            mNumberLabel.setText("+");
            mPhoneNumberLabel.setText(String.format("+%s",relationship.getToUsername()));

        }

        @Override
        public void onClick(View v) {
            onClickRelationship(mRelationship);
        }
    }

    public void onClickRelationship(Relationship relationship) {

        int maxNumberPerGroupSelfie = 12;

        if (mGroup.contains(relationship)) {
            Integer index = mGroup.indexOf(relationship)+1;
            mGroup.remove(relationship);
            mapping();
            Integer newIndex = mMap.indexOf(getObjects().indexOf(relationship))+2+mGroup.size();
            notifyItemMoved(index,newIndex);
        } else {
            if (mGroup.size() < maxNumberPerGroupSelfie - 1) {
                Integer index = mMap.indexOf(getObjects().indexOf(relationship)) + 2 + mGroup.size();
                mGroup.add(relationship);
                mapping();
                Integer newIndex = mGroup.indexOf(relationship) + 1;
                notifyItemMoved(index, newIndex);
            } else {
                if (getActivity() != null) {
                    Toast.makeText(getActivity(), String.format(getActivity().getResources().getString(R.string.info_max_reached), maxNumberPerGroupSelfie), Toast.LENGTH_LONG).show();
                }
            }
        }

    }

}






