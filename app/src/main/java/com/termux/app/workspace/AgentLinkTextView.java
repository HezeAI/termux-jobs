package com.termux.app.workspace;

import android.content.Context;
import android.text.Layout;
import android.text.Spanned;
import android.text.style.URLSpan;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import androidx.appcompat.widget.AppCompatTextView;

/** Short link taps coexist with Android's selectable-text movement method. */
final class AgentLinkTextView extends AppCompatTextView {
    private AgentTextReveal reveal;
    private AgentCardSize cardSize;
    void smoothSize(AgentChatView owner,boolean enabled){if(cardSize==null)cardSize=new AgentCardSize(this,owner);cardSize.enabled=enabled;}
    void seedCardHeight(int height){if(cardSize!=null)cardSize.seed(height);}
    @Override protected void onMeasure(int width,int height){super.onMeasure(width,height);if(cardSize!=null)setMeasuredDimension(getMeasuredWidth(),cardSize.measure(getMeasuredHeight()));}
    private boolean animateNext;
    private java.util.function.Function<String,Runnable> local;
    void localLinks(java.util.function.Function<String,Runnable> resolver){local=resolver;}
    void animateNextText(boolean enabled,Runnable before){
        if(reveal==null)reveal=new AgentTextReveal(this,text->AgentLinkTextView.super.setText(text,BufferType.SPANNABLE));
        animateNext=enabled;reveal.beforeChange=before;
    }
    @Override public void setText(CharSequence text,BufferType type){
        boolean animate=animateNext;animateNext=false;
        if(local!=null&&text instanceof Spanned){android.text.SpannableString bound=new android.text.SpannableString(text);AgentMarkdown.bindLocal(bound,local);text=bound;}
        if(reveal!=null)reveal.update(text,animate);else super.setText(text,type);
    }
    @Override protected void onVisibilityChanged(android.view.View changed,int visibility){super.onVisibilityChanged(changed,visibility);if(reveal!=null){if(visibility!=VISIBLE)reveal.finish();else reveal.attached();}}
    private URLSpan downLink, pendingLink;
    private CharSequence downText;
    private float downX, downY;
    private long downTime;
    private final int slop;

    AgentLinkTextView(Context context) {
        super(context);
        slop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    private URLSpan linkAt(float x, float y) {
        Layout layout = getLayout();
        if (!isEnabled() || !getLinksClickable() || layout == null || !(getText() instanceof Spanned)) return null;
        if (x < getTotalPaddingLeft() || x >= getWidth() - getTotalPaddingRight()
                || y < getTotalPaddingTop() || y >= getHeight() - getTotalPaddingBottom()) return null;
        x += getScrollX() - getTotalPaddingLeft();
        y += getScrollY() - getTotalPaddingTop();
        if (y < 0 || y >= layout.getHeight()) return null;
        int line = layout.getLineForVertical((int) y);
        if (x < layout.getLineLeft(line) || x >= layout.getLineRight(line)) return null;
        int offset = layout.getOffsetForHorizontal(line, x);
        Spanned text = (Spanned) getText();
        for (URLSpan link : text.getSpans(offset, offset, URLSpan.class)) {
            if (offset >= text.getSpanStart(link) && offset < text.getSpanEnd(link)) return link;
        }
        return null;
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if(event.getActionMasked()==MotionEvent.ACTION_DOWN&&reveal!=null)reveal.finish();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downLink = event.getPointerCount() == 1 ? linkAt(event.getX(), event.getY()) : null;
                downText = getText(); downX = event.getX(); downY = event.getY(); downTime = event.getEventTime();
                break;
            case MotionEvent.ACTION_MOVE:
                if (Math.abs(event.getX() - downX) > slop || Math.abs(event.getY() - downY) > slop) downLink = null;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_CANCEL:
                downLink = null; downText = null;
                break;
            case MotionEvent.ACTION_UP:
                URLSpan link = downLink;
                boolean tap = link != null && downText == getText() && event.getPointerCount() == 1
                        && event.getEventTime() - downTime < ViewConfiguration.getLongPressTimeout()
                        && Math.abs(event.getX() - downX) <= slop && Math.abs(event.getY() - downY) <= slop
                        && linkAt(event.getX(), event.getY()) == link;
                downLink = null; downText = null;
                if (tap) {
                    // Clear TextView's pending press/selection gesture before the link action.
                    MotionEvent cancel = MotionEvent.obtain(event); cancel.setAction(MotionEvent.ACTION_CANCEL);
                    try { super.onTouchEvent(cancel); } finally { cancel.recycle(); }
                    pendingLink = link;
                    return performClick();
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();if(reveal!=null)reveal.attached();}

    @Override protected void onDetachedFromWindow() {
        if(reveal!=null)reveal.finish();
        if(cardSize!=null)cardSize.detached();
        downLink = null; pendingLink = null; downText = null;
        super.onDetachedFromWindow();
    }

    @Override public boolean performClick() {
        URLSpan link = pendingLink; pendingLink = null;
        if (link == null) return super.performClick();
        if(link instanceof AgentMarkdown.LocalLink)link.onClick(this);else AgentMarkdown.openLink(this, link.getURL());
        return true;
    }
}
