package com.termux.app.workspace;

import android.content.Context;
import android.view.MotionEvent;
import android.widget.ScrollView;

/** A gesture starting in a panel belongs to that panel, even over clickable rows. */
public final class WorkspacePanelScrollView extends ScrollView {
    public WorkspacePanelScrollView(Context context) {
        super(context);
        setFillViewport(true);
        // Never pass this panel's unconsumed scroll to the surrounding viewport.
        setNestedScrollingEnabled(false);
    }
    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN && getParent() != null)
            getParent().requestDisallowInterceptTouchEvent(true);
        boolean handled = super.dispatchTouchEvent(event);
        if ((event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_CANCEL)
                && getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
        return handled;
    }
}
