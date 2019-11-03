package com.awgy.android.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.awgy.android.AppDelegate;
import com.awgy.android.R;
import com.awgy.android.models.GroupSelfie;
import com.awgy.android.models.Hashtag;
import com.awgy.android.models.PhoneBook;
import com.awgy.android.models.Relationship;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import bolts.Continuation;
import bolts.Task;

public abstract class ClassAdapter<T extends ParseObject, ViewHolder extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<ViewHolder> {

    private Activity mActivity;
    private View mView;

    // Objects
    private List<T> mObjects;

    // Options
    private boolean mPaginationEnabled;
    private String mPinName;
    private String mKeyName;
    private boolean mIsLocalBuildable;
    private String mCacheSubsetKey;
    private boolean mCacheSubsetInverted;
    private boolean mCacheSubsetInPhoneBook;
    private boolean mCacheSubsetInSponsors;
    private int mObjectsPerPage;
    private boolean mReversed;
    private boolean mAlwaysNeedNetwork;

    private List<String> mSponsors;

    private ClassAdapter.QueryFactory<T> mQueryFactory;

    // Utils
    private boolean mScrollViewContentIsEstablished;
    private int mLastLoadCount;
    private boolean mNeedNetwork;
    private boolean mNeedLoadNextPage;
    private boolean mDoneWithInitialDownload;
    private boolean mHasAskedForMorePages;
    private int mPreviousTotalItemCount;
    private int mCurrentPage;
    private boolean mLoading;

    // Empty
    private LinearLayout mEmptyView;
    private Integer mEmptyTableViewImageResource;
    private String mEmptyTableViewLabelTitle;
    private String mEmptyTableViewLabelMessage;
    private boolean mEmptyTableViewRemoveCallForAction;

    // Listeners
    private List<ClassAdapter.OnQueryLoadListener> mOnQueryLoadListeners;

    // Delegate
    private ClassAdapter.ClassAdapterDelegate mDelegate;

    // View
    private WeakReference<RecyclerView> mWeakRecyclerView;
    private boolean mNeedRemoveProgressBar;
    public void setNeedRemoveProgressBar(boolean needRemoveProgressBar) {
        mNeedRemoveProgressBar = needRemoveProgressBar;
    }

    public ClassAdapter(Activity activity, ClassAdapter.QueryFactory<T> queryFactory) {

        mActivity = activity;

        mQueryFactory = queryFactory;
        mOnQueryLoadListeners = new ArrayList<ClassAdapter.OnQueryLoadListener>();

        mPaginationEnabled = true;
        mReversed = false;
        mObjectsPerPage = 25;

        mIsLocalBuildable = false;
        mAlwaysNeedNetwork = false;

        mObjects = new ArrayList<T>();

        mLoading = false;
        mNeedLoadNextPage = false;
        mDoneWithInitialDownload = false;
        mHasAskedForMorePages = false;
        mNeedNetwork = false;

        mCurrentPage = 0;
        mLastLoadCount = -1;

        mScrollViewContentIsEstablished = true;

        mNeedRemoveProgressBar = false;

        mCacheSubsetKey = null;
        mCacheSubsetInverted = false;
        mCacheSubsetInPhoneBook = false;
        mCacheSubsetInSponsors = false;

        mSponsors = new ArrayList<String>();

        mEmptyTableViewRemoveCallForAction = false;

    }

    public interface QueryFactory<T extends ParseObject> {
        ParseQuery<T> create();
    }

    public Activity getActivity() {
        return mActivity;
    }

    public void setActivity(Activity activity) {
        mActivity = activity;
        if (mActivity == null) {
            mNeedRemoveProgressBar = false;
        }
    }

    public void setView(View view) {
        mView = view;
    }

    public List<T> getObjects() {
        return mObjects;
    }
    public ClassAdapter.QueryFactory<T> getQueryFactory() {
        return mQueryFactory;
    }

    public int getCurrentPage() {
        return mCurrentPage;
    }

    public int getObjectsPerPage() {
        return mObjectsPerPage;
    }

    public void setObjectsPerPage(int objectsPerPage) {
        mObjectsPerPage = objectsPerPage;
    }

    public void setLastLoadCount(int lastLoadCount) {
        mLastLoadCount = lastLoadCount;
    }

    public void setEmptyTableViewImageResource(Integer emptyTableViewImageResource) {
        mEmptyTableViewImageResource = emptyTableViewImageResource;
    }

    public void setEmptyTableViewLabelMessage(String emptyTableViewLabelMessage) {
        mEmptyTableViewLabelMessage = emptyTableViewLabelMessage;
    }

    public void setEmptyTableViewLabelTitle(String emptyTableViewLabelTitle) {
        mEmptyTableViewLabelTitle = emptyTableViewLabelTitle;
    }

    public void setEmptyTableViewRemoveCallForAction(boolean emptyTableViewRemoveCallForAction) {
        mEmptyTableViewRemoveCallForAction = emptyTableViewRemoveCallForAction;
    }

    public void setPaginationEnabled(boolean paginationEnabled) {
        mPaginationEnabled = paginationEnabled;
    }

    public void setReversed(boolean reversed) {
        mReversed = reversed;
    }

    public boolean getReversed() {
        return mReversed;
    }

    public boolean getNeedLoadNextPage() {
        return mNeedLoadNextPage;
    }

    public boolean getHasAskedForMorePages() {
        return mHasAskedForMorePages;
    }

    public void setAlwaysNeedNetwork(boolean alwaysNeedNetwork) {
        mAlwaysNeedNetwork = alwaysNeedNetwork;
    }

    public void setCacheSubsetKey(String cacheSubsetKey) {
        mCacheSubsetKey = cacheSubsetKey;
    }

    public void setCacheSubsetInverted(boolean cacheSubsetInverted) {
        mCacheSubsetInverted = cacheSubsetInverted;
    }

    public void setCacheSubsetInPhoneBook(boolean cacheSubsetInPhoneBook) {
        mCacheSubsetInPhoneBook = cacheSubsetInPhoneBook;
    }

    public void setCacheSubsetInSponsors(boolean cacheSubsetInSponsors) {
        mCacheSubsetInSponsors = cacheSubsetInSponsors;
    }

    public void setPinName(String pinName) {
        mPinName = pinName;
    }

    public void setKeyName(String keyName) {
        mKeyName = keyName;
    }

    public void setIsLocalBuildable(boolean isLocalBuildable) {
        mIsLocalBuildable = isLocalBuildable;
    }

    public void setWeakRecyclerView(WeakReference<RecyclerView> weakRecyclerView) {
        mWeakRecyclerView = weakRecyclerView;
        mWeakRecyclerView.get().addOnScrollListener(mOnScrollLister);
    }

    public WeakReference<RecyclerView> getWeakRecyclerView() {
        return mWeakRecyclerView;
    }

    public void clear() {
        mObjects.clear();
        notifyDataSetChanged();
        mCurrentPage = 0;
    }

    public T getItem(int position) {
        try {
            if (mReversed) {
                return mObjects.get(mObjects.size() - 1 - position);
            } else {
                return mObjects.get(position);
            }
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    public Task<Void> loadCache() { // Returns done after cache, even if keep going with network
        mNeedNetwork = !(mKeyName != null && !mAlwaysNeedNetwork) || NeedNetwork.getInstance().needNetworkForPinName(mKeyName);
        return loadObjects_clear_cache(0, true, true);
    }

    public Task<Void> loadNetwork() {
        return loadObjects_clear_cache(0, true, false);
    }

    private Task<Void> loadObjects_clear_cache(final int page, final boolean clear, final boolean cache) {

        if (mLoading) notifyOnLoadingListeners();
        mLoading = true;

        ParseQuery<T> query = mQueryFactory.create();
        completeQuery_forPage_andCache(query, page, cache);
        return query.findInBackground().continueWithTask(new Continuation<List<T>, Task<Void>>() {
            @Override
            public Task<Void> then(Task<List<T>> task) throws Exception {
                if (!task.isFaulted() && !task.isCancelled()) {
                    final List<T> foundObjects = task.getResult();

                    if ((foundObjects.size() > 0 || !mNeedNetwork) && cache) {
                        mNeedRemoveProgressBar = true;
                        if (mActivity != null) {
                            mActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mNeedRemoveProgressBar = false;
                                    ProgressBar progressBar = null;
                                    if (mView != null) {
                                        progressBar = (ProgressBar)mView.findViewById(R.id.progressBar);
                                    } else if (mActivity != null) {
                                        progressBar = (ProgressBar)mActivity.findViewById(R.id.progressBar);
                                    }
                                    if (progressBar != null) {
                                        progressBar.setVisibility(View.GONE);
                                    }
                                }
                            });
                        }
                    }

                    mCurrentPage = page;
                    mLastLoadCount = foundObjects.size();

                    Log.d("Check",String.format("Loading %s: %b %b: %d",mPinName,clear,cache,mLastLoadCount));

                    if (!cache && mCurrentPage == 0 && mPinName != null) {

                        mSponsors.clear();

                        for (ParseObject object : foundObjects) {
                            if (object instanceof GroupSelfie) {
                                GroupSelfie groupSelfie = (GroupSelfie)object;
                                groupSelfie.clear();
                            } else if (object instanceof Hashtag) {
                                Hashtag hashtag = (Hashtag)object;
                                hashtag.clear();
                            } else if (object instanceof Relationship) {
                                Relationship relationship = (Relationship)object;
                                relationship.clear();
                                if (relationship.getToType().equals(Constants.KEY_TYPE_EVENT) || relationship.getToType().equals(Constants.KEY_TYPE_ADS)) {
                                    mSponsors.add(relationship.getToUsername());
                                }
                            }
                        }

                        return ParseObject.unpinAllInBackground(mPinName, mObjects).continueWithTask(new Continuation<Void, Task<Void>>() {
                            @Override
                            public Task<Void> then(Task<Void> task) throws Exception {
                                return ParseObject.pinAllInBackground(mPinName, foundObjects).continueWithTask(new Continuation<Void, Task<Void>>() {
                                    @Override
                                    public Task<Void> then(Task<Void> task) throws Exception {
                                        PinsOnFile.getInstance().addPin(mPinName);

                                        if (mKeyName != null) {
                                            NeedNetwork.getInstance().addDone(mKeyName);
                                        }
                                        notifyOnDidCallNetworkListeners();

                                        mScrollViewContentIsEstablished = true;
                                        return loadObjects_clear_cache(0, true, true);
                                    }
                                });
                            }
                        });

                    } else {

                        if (clear) mObjects.clear();
                        mObjects.addAll(foundObjects);

                        mLoading = false;
                        notifyOnLoadedListeners(cache);

                        if (mActivity != null) {
                            mActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (clear) {
                                        notifyDataSetChanged();
                                    } else {
                                        if (mReversed) {
                                            notifyItemRangeInserted(0,mLastLoadCount);
                                        } else {
                                            notifyItemRangeInserted(mObjects.size()-mLastLoadCount,mObjects.size());
                                        }
                                    }
                                    notifyOnReloadDataListeners();
                                    if (!mNeedNetwork && !mNeedLoadNextPage) mDoneWithInitialDownload = true;
                                }
                            });
                        }

                        if (mNeedNetwork) {
                            mNeedNetwork = false;
                            loadNetwork();
                        } else if (mPaginationEnabled && mNeedLoadNextPage) {
                            mNeedLoadNextPage = false;
                            loadObjects_clear_cache(mCurrentPage + 1, false, false);
                        } else {
                            mNeedRemoveProgressBar = true;
                            if (mActivity != null) {
                                mActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        refreshEmptyView();
                                        mNeedRemoveProgressBar = false;
                                        ProgressBar progressBar = null;
                                        if (mView != null) {
                                            progressBar = (ProgressBar)mView.findViewById(R.id.progressBar);
                                        } else if (mActivity != null) {
                                            progressBar = (ProgressBar)mActivity.findViewById(R.id.progressBar);
                                        }
                                        if (progressBar != null) {
                                            progressBar.setVisibility(View.GONE);
                                        }
                                    }
                                });
                            }
                        }

                        Task<Void>.TaskCompletionSource source = Task.create();
                        source.setError(task.getError());
                        return source.getTask();

                    }

                } else {

                    mDoneWithInitialDownload = true;
                    mNeedRemoveProgressBar = true;
                    if (mActivity != null) {
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mNeedRemoveProgressBar = false;
                                ProgressBar progressBar;
                                if (mView != null) {
                                    progressBar = (ProgressBar)mView.findViewById(R.id.progressBar);
                                } else {
                                    progressBar = (ProgressBar)mActivity.findViewById(R.id.progressBar);
                                }
                                if (progressBar != null) {
                                    progressBar.setVisibility(View.GONE);
                                }
                            }
                        });
                    }

                    mLastLoadCount = -1;

                    Task<Void>.TaskCompletionSource source = Task.create();
                    source.setError(task.getError());
                    return source.getTask();

                }
            }
        });
    }

    private void completeQuery_forPage_andCache(ParseQuery<T> query, int page, boolean cache) {
        if (cache) {
            if (mCacheSubsetKey != null) {
                query.whereEqualTo(mCacheSubsetKey,!mCacheSubsetInverted);
            }
            if (mCacheSubsetInPhoneBook || mCacheSubsetInSponsors) {
                List<String> container = new ArrayList<String>();
                if (mCacheSubsetInPhoneBook) {
                    container.addAll(PhoneBook.getInstance().getNames().keySet());
                }
                if (mCacheSubsetInSponsors) {
                    container.addAll(mSponsors);
                }
                query.whereContainedIn(Constants.KEY_RELATIONSHIP_TO_USERNAME, container);
            }
            if (mPinName != null) {
                query.fromPin(mPinName);
            } else {
                query.fromLocalDatastore();
            }
        }
        if (cache && mIsLocalBuildable && mPinName != null) {
            query.addDescendingOrder(Constants.KEY_LOCAL_CREATED_AT);
        } else {
            query.addDescendingOrder(Constants.KEY_CREATED_AT);
        }
        if (cache) {
            query.setLimit(1000);
        } else {
            if (mPaginationEnabled && mObjectsPerPage != 0) {
                query.setLimit(mObjectsPerPage);
                query.setSkip(page * mObjectsPerPage);
            }
        }
    }

    public void refreshEmptyView() {

        if (getObjects().size() > 0) {

            destroyEmptyView();

        } else {

            if (mActivity != null && mEmptyView == null
                    && mEmptyTableViewImageResource != null
                    && mEmptyTableViewLabelTitle != null
                    && mEmptyTableViewLabelMessage != null) {

                RelativeLayout mainRelativeLayout;

                if (mView != null) {
                    mainRelativeLayout = (RelativeLayout) mView.findViewById(R.id.mainRelativeLayout);
                } else {
                    mainRelativeLayout = (RelativeLayout) mActivity.findViewById(R.id.mainRelativeLayout);
                }

                mEmptyView = (LinearLayout) LayoutInflater.from(getActivity()).inflate(R.layout.empty_view, mainRelativeLayout, false);

                ImageView emptyImage = (ImageView) mEmptyView.findViewById(R.id.emptyImage);
                emptyImage.setImageResource(mEmptyTableViewImageResource);
                TextView emptyTitle = (TextView) mEmptyView.findViewById(R.id.emptyTitle);
                emptyTitle.setText(mEmptyTableViewLabelTitle);
                TextView emptyMessage = (TextView) mEmptyView.findViewById(R.id.emptyMessage);
                emptyMessage.setText(mEmptyTableViewLabelMessage);

                if (mEmptyTableViewRemoveCallForAction) {
                    LinearLayout emptyAction = (LinearLayout) mEmptyView.findViewById(R.id.emptyAction);
                    ViewGroup viewParent = (ViewGroup) emptyAction.getParent();
                    viewParent.removeView(emptyAction);
                }

                mainRelativeLayout.addView(mEmptyView,0);

            }
        }
    }

    public void destroyEmptyView() {
        if (mEmptyView != null) {
            ViewGroup viewParent = (ViewGroup) mEmptyView.getParent();
            viewParent.removeView(mEmptyView);
            mEmptyView = null;
        }
    }

    public void checkProgressBarStatus() {

        if (mNeedRemoveProgressBar) {
            if (mActivity != null) {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mNeedRemoveProgressBar = false;
                        ProgressBar progressBar;
                        if (mView != null) {
                            progressBar = (ProgressBar)mView.findViewById(R.id.progressBar);
                        } else {
                            progressBar = (ProgressBar)mActivity.findViewById(R.id.progressBar);
                        }
                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }
                    }
                });
            }
        }

    }

    public void checkIfEnoughCells() {

        final int VISIBLE_THRESHOLD = 3;

        LinearLayoutManager linearLayoutManager = (LinearLayoutManager) mWeakRecyclerView.get().getLayoutManager();

        int totalItemCount = linearLayoutManager.getItemCount();
        int visibleItemCount = linearLayoutManager.getChildCount();
        int firstVisibleItem = linearLayoutManager.findFirstVisibleItemPosition();

        if ((totalItemCount - visibleItemCount <= firstVisibleItem + VISIBLE_THRESHOLD) && (mLastLoadCount == -1 || mLastLoadCount >= mObjectsPerPage)) {
            loadNetwork();
        }
    }

    RecyclerView.OnScrollListener mOnScrollLister = new RecyclerView.OnScrollListener() {

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {

            if (mPaginationEnabled && mDoneWithInitialDownload) {

                final int VISIBLE_THRESHOLD = 3;

                LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();

                int totalItemCount = linearLayoutManager.getItemCount();
                int visibleItemCount = linearLayoutManager.getChildCount();
                int firstVisibleItem = linearLayoutManager.findFirstVisibleItemPosition();

                if (!mScrollViewContentIsEstablished && (totalItemCount != mPreviousTotalItemCount)) {
                    mScrollViewContentIsEstablished = true;
                }
                mPreviousTotalItemCount = totalItemCount;
                if (!mLoading && mScrollViewContentIsEstablished) {
                    boolean check;
                    if (mReversed) {
                        check = firstVisibleItem <= VISIBLE_THRESHOLD;
                    } else {
                        check = totalItemCount - visibleItemCount <= firstVisibleItem + VISIBLE_THRESHOLD;
                    }
                    if (check) {
                        if (mLastLoadCount == -1 || mLastLoadCount >= mObjectsPerPage) {
                            if (!mReversed) {
                                if (mActivity != null) {
                                    Toast.makeText(mActivity, "Loading More ...", Toast.LENGTH_SHORT).show();
                                }
                            }
                            if (mCurrentPage == 0) {
                                mNeedLoadNextPage = true;
                                mNeedNetwork = false;
                                mScrollViewContentIsEstablished = false;
                                if (mObjects.size() > 0) mHasAskedForMorePages = true;
                                loadNetwork();
                            } else {
                                mScrollViewContentIsEstablished = false;
                                loadObjects_clear_cache((mCurrentPage + 1), false, false);
                            }
                        }
                    }
                }
            }

        }

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
        }
    };

    // Listeners

    public interface OnQueryLoadListener {
        void onLoading();
        void onLoaded(boolean cache);
        void onReloadData();
        void onDidCallNetwork();
    }

    public void addOnQueryLoadListener(ClassAdapter.OnQueryLoadListener listener) {
        mOnQueryLoadListeners.add(listener);
    }

    public void removeOnQueryLoadListener(ClassAdapter.OnQueryLoadListener listener) {
        mOnQueryLoadListeners.remove(listener);
    }

    private void notifyOnLoadingListeners() {
        Iterator i$ = mOnQueryLoadListeners.iterator();

        while(i$.hasNext()) {
            ClassAdapter.OnQueryLoadListener listener = (ClassAdapter.OnQueryLoadListener)i$.next();
            listener.onLoading();
        }

    }

    private void notifyOnLoadedListeners(boolean cache) {
        Iterator i$ = mOnQueryLoadListeners.iterator();

        while(i$.hasNext()) {
            ClassAdapter.OnQueryLoadListener listener = (ClassAdapter.OnQueryLoadListener)i$.next();
            listener.onLoaded(cache);
        }

    }

    private void notifyOnReloadDataListeners() {
        Iterator i$ = mOnQueryLoadListeners.iterator();

        while(i$.hasNext()) {
            ClassAdapter.OnQueryLoadListener listener = (ClassAdapter.OnQueryLoadListener)i$.next();
            listener.onReloadData();
        }

    }

    private void notifyOnDidCallNetworkListeners() {
        Iterator i$ = mOnQueryLoadListeners.iterator();

        while(i$.hasNext()) {
            ClassAdapter.OnQueryLoadListener listener = (ClassAdapter.OnQueryLoadListener)i$.next();
            listener.onDidCallNetwork();
        }

    }

    // Delegate

    public interface ClassAdapterDelegate {
        void onEmptyButtonClick();
    }

    public void setDelegate(ClassAdapter.ClassAdapterDelegate delegate) {
        mDelegate = delegate;
    }

    // Swipe-to-Dismiss

    public boolean canDismissItem(int position) {
        return false;
    }

    public void onItemDismiss(int position) {

    }

    public void onItemClicked(int position) {

    }

    // Network

    public boolean isNetworkAvailable() {

        if (getActivity() != null) {

            ConnectivityManager connectivityManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            boolean available = activeNetworkInfo != null && activeNetworkInfo.isConnected();

            if (!available) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(AppDelegate.getContext().getResources().getString(R.string.no_internet))
                        .setTitle(AppDelegate.getContext().getResources().getString(R.string.oops))
                        .setPositiveButton(android.R.string.ok, null);
                AlertDialog dialog = builder.create();
                dialog.show();
            }

            return available;

        } else {
            return false;
        }

    }


}