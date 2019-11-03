package com.awgy.android.utils;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

import com.awgy.android.R;
import com.hudomju.swipe.OnItemClickListener;
import com.hudomju.swipe.SwipeToDismissTouchListener;
import com.hudomju.swipe.SwipeableItemClickListener;
import com.hudomju.swipe.adapter.RecyclerViewAdapter;

public class SwipeableRecyclerView extends RecyclerView {

    private SwipeToDismissTouchListener<RecyclerViewAdapter> mSwipeListener;

    public SwipeableRecyclerView(Context context) {
        super(context);
        setHasFixedSize(true);
        setLayoutManager(new LinearLayoutManager(getContext()));
    }

    public SwipeableRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setHasFixedSize(true);
        setLayoutManager(new LinearLayoutManager(getContext()));
    }

    public SwipeableRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setHasFixedSize(true);
        setLayoutManager(new LinearLayoutManager(getContext()));
    }

    public SwipeToDismissTouchListener<RecyclerViewAdapter> getSwipeListener() {
        return mSwipeListener;
    }

    public void setClassAdapter(final ClassAdapter adapter) {

        setAdapter(adapter);

        mSwipeListener = new SwipeToDismissTouchListener<RecyclerViewAdapter>(
                new RecyclerViewAdapter(this),
                new SwipeToDismissTouchListener.DismissCallbacks<RecyclerViewAdapter>() {
                    @Override
                    public boolean canDismiss(int position) {
                        return adapter.canDismissItem(position);
                    }

                    @Override
                    public void onDismiss(RecyclerViewAdapter view, int position) {
                        adapter.onItemDismiss(position);
                    }
                });

        setOnTouchListener(mSwipeListener);
        setOnScrollListener((RecyclerView.OnScrollListener) mSwipeListener.makeScrollListener());
        addOnItemTouchListener(
                new SwipeableItemClickListener(getContext(),
                        new OnItemClickListener() {
                            @Override
                            public void onItemClick(View view, int position) {
                                if (view.getId() == R.id.delete) {
                                    mSwipeListener.processPendingDismisses();
                                } else if (view.getId() == R.id.undo) {
                                    mSwipeListener.undoPendingDismiss();
                                } else {
                                    adapter.onItemClicked(position);
                                }
                            }
                        }) {
                    @Override
                    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

                    }
                });

    }

}