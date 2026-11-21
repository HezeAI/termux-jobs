package com.termux.app.workspace;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Shader;
import android.os.SystemClock;
import androidx.appcompat.widget.AppCompatTextView;

/** A live action label. Only visible running work schedules animation frames. */
final class AgentActionTextView extends AppCompatTextView {
    private boolean running;
    private String target = "";
    private int revision;

    // A visible grey base makes the passing white band perceptible on the dark surface.
    private static final int SHIMMER_BASE = 0xffb4b4b1;
    private static final int SHIMMER_PEAK = 0xffffffff;
    private LinearGradient shimmerShader;
    private final Matrix shimmerMatrix = new Matrix();
    private int shaderWidth;
    private boolean framePosted;
    private final Runnable frame = () -> {
        framePosted = false;
        if (canShimmer()) invalidate();
    };

    AgentActionTextView(Context context) { super(context); }

    void present(String text, boolean active) {
        running = active;
        if (!target.equals(text)) {
            target = text;
            int next = ++revision;
            animate().cancel();
            setAlpha(1f);
            if (isAttachedToWindow() && isShown() && length() > 0 && ValueAnimator.areAnimatorsEnabled()) {
                animate().alpha(0f).setDuration(100).withEndAction(() -> {
                    if (next != revision) return;
                    setText(target);
                    animate().alpha(1f).setDuration(160).start();
                }).start();
            } else setText(target);
        }
        if (!active) settle();
        invalidate();
    }

    private boolean canShimmer() {
        return running && isAttachedToWindow() && isShown() && getWindowVisibility() == VISIBLE
            && ValueAnimator.areAnimatorsEnabled() && getWidth() > 0;
    }

    private void stopFrame() {
        if (framePosted) removeCallbacks(frame);
        framePosted = false;
        getPaint().setShader(null);
    }

    @Override protected void onDraw(Canvas canvas) {
        boolean shimmer = canShimmer();
        if (shimmer) {
            float width = getWidth();
            if (shimmerShader == null || shaderWidth != getWidth()) {
                shaderWidth = getWidth();
                shimmerShader = new LinearGradient(-width * .28f, 0, width * .28f, 0,
                    new int[]{SHIMMER_BASE, SHIMMER_PEAK, SHIMMER_BASE},
                    new float[]{0, .5f, 1}, Shader.TileMode.CLAMP);
            }
            float center = ((SystemClock.uptimeMillis() % 1600L) / 1600f * 1.6f - .3f) * width;
            shimmerMatrix.setTranslate(center, 0);
            shimmerShader.setLocalMatrix(shimmerMatrix);
            getPaint().setShader(shimmerShader);
        } else stopFrame();
        try {
            super.onDraw(canvas);
        } finally {
            getPaint().setShader(null);
        }
        if (shimmer && !framePosted) {
            framePosted = true;
            postOnAnimation(frame);
        }
    }

    @Override protected void onVisibilityChanged(android.view.View view, int visibility) {
        super.onVisibilityChanged(view, visibility);
        if (visibility != VISIBLE) settle();
        else invalidate();
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        invalidate();
    }

    @Override protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility != VISIBLE) settle();
        else invalidate();
    }

    private void settle() {
        ++revision;
        animate().cancel();
        setAlpha(1f);
        // Visibility callbacks may arrive from the superclass constructor.
        if (target != null) setText(target);
        stopFrame();
    }

    @Override protected void onDetachedFromWindow() {
        settle();
        super.onDetachedFromWindow();
    }
}
