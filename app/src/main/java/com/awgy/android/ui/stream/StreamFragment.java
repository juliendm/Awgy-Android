package com.awgy.android.ui.stream;

import android.support.v4.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.awgy.android.AppDelegate;
import com.awgy.android.adapters.StreamAdapter;
import com.awgy.android.models.GroupSelfie;
import com.awgy.android.ui.camera.SetUpActivity;
import com.awgy.android.utils.BaseActivity;
import com.awgy.android.utils.ClassAdapter;
import com.awgy.android.utils.Constants;

import com.awgy.android.utils.SwipeableRecyclerView;

import com.awgy.android.R;

import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;

import bolts.Continuation;
import bolts.Task;

public class StreamFragment extends Fragment implements ClassAdapter.ClassAdapterDelegate {

    private static StreamAdapter staticStreamAdapter;
    public static void setStaticStreamAdapter(StreamAdapter streamAdapter) {
        staticStreamAdapter = streamAdapter;
    }
    public static StreamAdapter getStaticStreamAdapter() {
        return staticStreamAdapter;
    }

    private View mView;

    private SwipeableRecyclerView mSwipeableRecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private Timer mTimer;

    private boolean isShowing;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Notification
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(getActivity());
        manager.registerReceiver(mPhonebookPhonebookHasBeenUpdatedNotification, new IntentFilter(Constants.PHONEBOOK_PHONEBOOK_HAS_BEEN_UPDATED_NOTIFICATION));
        manager.registerReceiver(mGroupSelfieHasBeenDeletedNotification, new IntentFilter(Constants.GROUPSELFIE_GROUPSELFIE_HAS_BEEN_DELETED_NOTIFICATION));
        manager.registerReceiver(mMessageHasBeenSavedNotification, new IntentFilter(Constants.ACTIVITY_MESSAGE_HAS_BEEN_SAVED_NOTIFICATION));
        manager.registerReceiver(mLoveIdsHasBeenUpdatedNotification, new IntentFilter(Constants.GROUPSELFIE_LOVE_IDS_HAS_BEEN_UPDATED_NOTIFICATION));
        manager.registerReceiver(mSeenIdsHasBeenUpdatedNotification, new IntentFilter(Constants.GROUPSELFIE_SEEN_IDS_HAS_BEEN_UPDATED_NOTIFICATION));
        manager.registerReceiver(mStreamFragmentNeedLoadCache, new IntentFilter(Constants.STREAM_ACTIVITY_NEED_LOAD_CACHE));

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

            mView = inflater.inflate(R.layout.fragment_stream, container, false);

            mSwipeableRecyclerView = (SwipeableRecyclerView) mView.findViewById(R.id.swipeableRecyclerView);

            mSwipeRefreshLayout = (SwipeRefreshLayout) mView.findViewById(R.id.swipeRefreshLayout);
            mSwipeRefreshLayout.setOnRefreshListener(mOnRefreshListener);

            if (staticStreamAdapter != null) {
                staticStreamAdapter.setActivity(getActivity());
                staticStreamAdapter.setView(mView);
                staticStreamAdapter.setDelegate(this);
                mSwipeableRecyclerView.setClassAdapter(staticStreamAdapter);
                staticStreamAdapter.setWeakRecyclerView(new WeakReference<RecyclerView>(mSwipeableRecyclerView));
                staticStreamAdapter.checkProgressBarStatus();
            }

        }

        return mView;

    }

    @Override
    public void onResume() {
        super.onResume();

        isShowing = true;

        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (staticStreamAdapter != null) staticStreamAdapter.refreshInfoLabel();
                    }
                });
            }
        }, 30000, 30000);

        if (staticStreamAdapter != null && AppDelegate.getApplicationWillEnterForeground()) {
            AppDelegate.setApplicationWillEnterForeground(false);
            staticStreamAdapter.loadNetwork();
        }

    }

    @Override
    public void onPause() {
        super.onPause();

        isShowing = false;

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
        manager.unregisterReceiver(mMessageHasBeenSavedNotification);
        manager.unregisterReceiver(mLoveIdsHasBeenUpdatedNotification);
        manager.unregisterReceiver(mSeenIdsHasBeenUpdatedNotification);
        manager.unregisterReceiver(mStreamFragmentNeedLoadCache);

    }

    // Delegate

    @Override
    public void onEmptyButtonClick() {

    }

    // Notification

    private BroadcastReceiver mPhonebookPhonebookHasBeenUpdatedNotification = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (staticStreamAdapter != null) staticStreamAdapter.notifyDataSetChanged();
        }
    };

    private BroadcastReceiver mGroupSelfieHasBeenDeletedNotification = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!isShowing && staticStreamAdapter != null) staticStreamAdapter.loadCache();
        }
    };

    private BroadcastReceiver mMessageHasBeenSavedNotification = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (staticStreamAdapter != null) staticStreamAdapter.loadCache();
        }
    };

    private BroadcastReceiver mLoveIdsHasBeenUpdatedNotification = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (staticStreamAdapter != null) staticStreamAdapter.loadCache();
        }
    };

    private BroadcastReceiver mSeenIdsHasBeenUpdatedNotification = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (staticStreamAdapter != null) staticStreamAdapter.loadCache();
        }
    };

    private BroadcastReceiver mStreamFragmentNeedLoadCache = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (staticStreamAdapter != null)staticStreamAdapter.loadCache();
        }
    };

    // Listeners

    protected SwipeRefreshLayout.OnRefreshListener mOnRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            if (mSwipeRefreshLayout.isRefreshing()) mSwipeRefreshLayout.setRefreshing(false);
            if (staticStreamAdapter != null) staticStreamAdapter.loadNetwork();
        }
    };

}
