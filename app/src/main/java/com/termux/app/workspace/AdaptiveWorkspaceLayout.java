package com.termux.app.workspace;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;

/** Keep independent panels usable when the keyboard reduces the viewport. */
public final class AdaptiveWorkspaceLayout extends ScrollView {
    public AdaptiveWorkspaceLayout(Context context) {
        super(context);
        setFillViewport(true);
    }

    @Override protected void measureChildWithMargins(View child, int parentWidthSpec, int widthUsed,
            int parentHeightSpec, int heightUsed) {
        MarginLayoutParams params = (MarginLayoutParams) child.getLayoutParams();
        if (params.height >= 0) {
            int widthSpec = ViewGroup.getChildMeasureSpec(parentWidthSpec,
                getPaddingLeft() + getPaddingRight() + params.leftMargin + params.rightMargin + widthUsed,
                params.width);
            child.measure(widthSpec, MeasureSpec.makeMeasureSpec(params.height, MeasureSpec.EXACTLY));
        } else super.measureChildWithMargins(child, parentWidthSpec, widthUsed, parentHeightSpec, heightUsed);
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        if (getChildCount() > 0 && MeasureSpec.getMode(heightSpec) != MeasureSpec.UNSPECIFIED) {
            LinearLayout body = (LinearLayout) getChildAt(0);
            int width = Math.max(0, MeasureSpec.getSize(widthSpec) - getPaddingLeft() - getPaddingRight()
                - body.getPaddingLeft() - body.getPaddingRight());
            int minimum = body.getPaddingTop() + body.getPaddingBottom();
            int panelHeight = Math.round(160 * getResources().getDisplayMetrics().density
                * Math.max(1f, getResources().getConfiguration().fontScale));
            for (int i = 0; i < body.getChildCount(); i++) {
                View child = body.getChildAt(i);
                if (child.getVisibility() == GONE) continue;
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) child.getLayoutParams();
                minimum += params.topMargin + params.bottomMargin;
                if (params.weight > 0) minimum += panelHeight;
                else {
                    int childWidth = ViewGroup.getChildMeasureSpec(
                        MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                        params.leftMargin + params.rightMargin, params.width);
                    int childHeight = params.height >= 0
                        ? MeasureSpec.makeMeasureSpec(params.height, MeasureSpec.EXACTLY)
                        : MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
                    child.measure(childWidth, childHeight);
                    minimum += child.getMeasuredHeight();
                }
            }
            body.getLayoutParams().height = Math.max(minimum, MeasureSpec.getSize(heightSpec)
                - getPaddingTop() - getPaddingBottom());
        }
        super.onMeasure(widthSpec, heightSpec);
    }
}
