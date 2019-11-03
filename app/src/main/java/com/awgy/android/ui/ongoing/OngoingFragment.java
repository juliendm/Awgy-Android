package com.awgy.android.ui.ongoing;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.awgy.android.AppDelegate;
import com.awgy.android.adapters.OngoingAdapter;
import com.awgy.android.adapters.StreamAdapter;
import com.awgy.android.adapters.UsersAdapter;
import com.awgy.android.models.GroupSelfie;
import com.awgy.android.ui.camera.SetUpActivity;
import com.awgy.android.utils.BaseActivity;
import com.awgy.android.utils.ClassAdapter;

import com.awgy.android.R;
import com.awgy.android.utils.Constants;
import com.awgy.android.utils.PushBroadcastReceiver;
import com.awgy.android.utils.SwipeableRecyclerView;

import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;

import bolts.Continuation;
import bolts.Task;

public class OngoingFragment extends Fragment implements ClassAdapter.ClassAdapterDelegate {

    private static OngoingAdapter staticOngoingAdapter;
    public static void setStaticOngoingAdapter(OngoingAdapter ongoingAdapter) {
        staticOngoingAdapter = ongoingAdapter;
    }
    public static OngoingAdapter getStaticOngoingAdapter() {
        return staticOngoingAdapter;
    }

    private View mView;

    private SwipeableRecyclerView mSwipeableRecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private Timer mTimer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (staticOngoingAdapter != null) staticOngoingAdapter.destroyEmptyView();

        // Notification
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(getActivity());
        manager.registerReceiver(mPhonebookPhonebookHasBeenUpdatedNotification, new IntentFilter(Constants.PHONEBOOK_PHONEBOOK_HAS_BEEN_UPDATED_NOTIFICATION));
        manager.registerReceiver(mGroupSelfieHasBeenDeletedNotification, new IntentFilter(Constants.GROUPSELFIE_GROUPSELFIE_HAS_BEEN_DELETED_NOTIFICATION));
        manager.registerReceiver(mSeenIdsHasBeenUpdatedNotification, new IntentFilter(Constants.GROUPSELFIE_SEEN_IDS_HAS_BEEN_UPDATED_NOTIFICATION));

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        super.onCreateView(inflater, container, savedInstanceState);

        ActionBar actionBar = ((BaseActivity)getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setCustomView(R.layout.main_action_bar);
        }

        if (mView == null) {

            mView = inflater.inflate(R.layout.fragment_ongoing, container, false);

            mSwipeableRecyclerView = (SwipeableRecyclerView) mView.findViewById(R.id.swipeableRecyclerView);

            mSwipeRefreshLayout = (SwipeRefreshLayout) mView.findViewById(R.id.swipeRefreshLayout);
            mSwipeRefreshLayout.setOnRefreshListener(mOnRefreshListener);

            if (staticOngoingAdapter != null) {
                staticOngoingAdapter.setActivity(getActivity());
                staticOngoingAdapter.setView(mView);
                staticOngoingAdapter.setDelegate(this);
                mSwipeableRecyclerView.setClassAdapter(staticOngoingAdapter);
                staticOngoingAdapter.setWeakRecyclerView(new WeakReference<RecyclerView>(mSwipeableRecyclerView));
                staticOngoingAdapter.checkProgressBarStatus();
                staticOngoingAdapter.refreshEmptyView();
            }

        }

        return mView;

    }

    @Override
    public void onResume() {
        super.onResume();

        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (staticOngoingAdapter != null) staticOngoingAdapter.refreshRemainingsLabel();
                    }
                });
            }
        }, 30000, 30000);

        if (staticOngoingAdapter != null && AppDelegate.getApplicationWillEnterForeground()) {
            AppDelegate.setApplicationWillEnterForeground(false);
            staticOngoingAdapter.loadNetwork();
        }

    }

    @Override
    public void onPause() {
        super.onPause();

        mTimer.cancel();
        mTimer = null;

        // Notifications are not missed when in background; therefore, don't need to clear NeedNetwork

        if (mSwipeableRecyclerView != null) {
            if (mSwipeableRecyclerView.getSwipeListener() != null) {
                mSwipeableRecyclerView.getSwipeListener().processPendingDismisses();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }

        // Notification
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(getActivity());
        manager.unregisterReceiver(mPhonebookPhonebookHasBeenUpdatedNotification);
        manager.unregisterReceiver(mGroupSelfieHasBeenDeletedNotification);
        manager.unregisterReceiver(mSeenIdsHasBeenUpdatedNotification);

    }

    // Delegate

    @Override
    public void onEmptyButtonClick() {

    }

    // Notification

    private BroadcastReceiver mPhonebookPhonebookHasBeenUpdatedNotification = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (staticOngoingAdapter != null) staticOngoingAdapter.notifyDataSetChanged();
        }
    };

    private BroadcastReceiver mGroupSelfieHasBeenDeletedNotification = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (staticOngoingAdapter != null) staticOngoingAdapter.loadCache();
        }
    };

    private BroadcastReceiver mSeenIdsHasBeenUpdatedNotification = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (staticOngoingAdapter != null) staticOngoingAdapter.loadCache();
        }
    };

    // Listeners

    protected SwipeRefreshLayout.OnRefreshListener mOnRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            if (mSwipeRefreshLayout.isRefreshing()) mSwipeRefreshLayout.setRefreshing(false);
            if (staticOngoingAdapter != null) staticOngoingAdapter.loadNetwork();
        }
    };


}
