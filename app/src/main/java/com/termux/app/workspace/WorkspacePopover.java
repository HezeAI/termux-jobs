package com.termux.app.workspace;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;

/** Native v35 anchored menu. Opening it never executes an item callback. */
final class WorkspacePopover {
    private final AnimatedWindow window;
    private final ScrollView surface;
    private boolean animated,closing,rising,dropping;
    private int menuWidth=210;
    private boolean largeItems;
    WorkspacePopover largeItems(){largeItems=true;menuWidth=300;return this;}
    private android.animation.ValueAnimator riseAnimation;
    private final class AnimatedWindow extends PopupWindow {
        AnimatedWindow(View content,int width){super(content,width,-2,true);}
        @Override public void dismiss(){WorkspacePopover.this.dismiss();}
        void finishDismiss(){super.dismiss();}
    }
    private final LinearLayout items;
    private final Context context;
    private Runnable onDismiss=()->{};
    WorkspacePopover(Context c){context=c;items=new LinearLayout(c);items.setOrientation(LinearLayout.VERTICAL);items.setPadding(dp(5),dp(5),dp(5),dp(5));
        surface=new ScrollView(c);surface.setFillViewport(true);surface.setVerticalScrollBarEnabled(false);surface.addView(items);
        window=new AnimatedWindow(surface,dp(210));GradientDrawable background=new GradientDrawable();background.setColor(WorkspaceDialogs.SURFACE);background.setCornerRadius(dp(13));background.setStroke(dp(1),WorkspaceDialogs.LINE);surface.setBackground(background);surface.setClipToOutline(true);window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));window.setAnimationStyle(0);window.setOnDismissListener(()->{surface.animate().cancel();resetItems();closing=false;onDismiss.run();});window.setElevation(dp(8));window.setOutsideTouchable(true);window.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING|WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED);
    }
    private int dp(int n){return WorkspaceDialogs.dp(context,n);}
    void item(String title,int icon,Runnable action){TextView row=new TextView(context);row.setText(title);row.setTextSize(largeItems?20:12);row.setTextColor(WorkspaceDialogs.TEXT);row.setMinHeight(dp(largeItems?59:44));int pad=dp(largeItems?12:9);row.setPadding(pad,pad,pad,pad);row.setGravity(Gravity.CENTER_VERTICAL);if(icon!=0){android.graphics.drawable.Drawable image=context.getDrawable(icon).mutate();image.setTint(WorkspaceDialogs.MUTED);image.setBounds(0,0,dp(largeItems?24:18),dp(largeItems?24:18));row.setCompoundDrawables(image,null,null,null);row.setCompoundDrawablePadding(dp(largeItems?12:9));}row.setBackground(WorkspaceRipple.background(context,null,8));row.setOnClickListener(v->{if(closing||!window.isShowing())return;dismiss();action.run();});items.addView(row,new LinearLayout.LayoutParams(-1,-2));}
    void choices(String negative,String positive,Runnable action){
        LinearLayout row=new LinearLayout(context);row.setBaselineAligned(false);
        for(int i=0;i<2;i++){
            final boolean accept=i==1;TextView button=new TextView(context);
            button.setText(accept?positive:negative);button.setTextSize(12);button.setTextColor(WorkspaceDialogs.TEXT);
            button.setGravity(Gravity.CENTER);button.setMinHeight(dp(44));button.setPadding(dp(8),dp(8),dp(8),dp(8));
            button.setBackground(WorkspaceRipple.background(context,null,8));
            button.setOnClickListener(v->{if(closing||!window.isShowing())return;dismiss();if(accept)action.run();});
            row.addView(button,new LinearLayout.LayoutParams(0,-2,1));
        }
        items.addView(row,new LinearLayout.LayoutParams(-1,-2));
    }
    void label(String title){TextView text=new TextView(context);text.setText(title);text.setTextSize(12);text.setTextColor(WorkspaceDialogs.TEXT);text.setPadding(dp(9),dp(9),dp(9),dp(9));items.addView(text,new LinearLayout.LayoutParams(-1,-2));}
    void dismissed(Runnable action){onDismiss=action;}
    WorkspacePopover animated(){animated=true;return this;}
    WorkspacePopover rise(){animated=true;rising=true;return this;}
    private void resetItems(){cancelRise();for(int i=0;i<items.getChildCount();i++){View item=items.getChildAt(i);item.animate().cancel();item.setAlpha(1);item.setTranslationY(0);}}
    private void cancelRise(){if(riseAnimation!=null){riseAnimation.cancel();riseAnimation=null;}}
    private void revealItems(){
        long duration=Math.max(220,Math.max(0,items.getChildCount()-1)*35L+160);
        android.view.animation.DecelerateInterpolator ease=new android.view.animation.DecelerateInterpolator();
        for(int i=0;i<items.getChildCount();i++){items.getChildAt(i).setAlpha(0);items.getChildAt(i).setTranslationY(dp(6));}
        riseAnimation=android.animation.ValueAnimator.ofFloat(0,1);riseAnimation.setDuration(duration);riseAnimation.setInterpolator(new android.view.animation.LinearInterpolator());
        riseAnimation.addUpdateListener(frame->{float time=(float)frame.getAnimatedValue()*duration;float panel=ease.getInterpolation(Math.min(1,time/220));surface.setAlpha(panel);surface.setTranslationY(dp(8)*(1-panel));
            for(int i=0;i<items.getChildCount();i++){View item=items.getChildAt(i);float progress=ease.getInterpolation(Math.max(0,Math.min(1,(time-i*35)/160)));item.setAlpha(progress);item.setTranslationY(dp(6)*(1-progress));}});
        riseAnimation.start();
    }

    void show(View anchor){show(anchor,true);}
    void showBelow(View anchor){show(anchor,false);}
    void showDropdown(View anchor){
        dropping=true;
        anchor.requestRectangleOnScreen(new android.graphics.Rect(0,0,anchor.getWidth(),anchor.getHeight()+dp(200)),true);
        anchor.post(()->{if(anchor.isAttachedToWindow())show(anchor,false);});
    }
    private void show(View anchor,boolean above){android.graphics.Rect visible=new android.graphics.Rect();anchor.getWindowVisibleDisplayFrame(visible);int[] location=new int[2];anchor.getLocationOnScreen(location);int width=Math.min(dp(menuWidth),Math.max(dp(1),visible.width()-dp(16)));if(largeItems){int wanted=0;for(int i=0;i<items.getChildCount();i++){View item=items.getChildAt(i);if(item instanceof TextView){TextView text=(TextView)item;wanted=Math.max(wanted,(int)Math.ceil(text.getPaint().measureText(text.getText().toString()))+text.getCompoundPaddingLeft()+text.getCompoundPaddingRight());}}width=Math.min(width,Math.max(dp(180),wanted+items.getPaddingLeft()+items.getPaddingRight()+dp(4)));}int aboveSpace=Math.max(0,location[1]-visible.top-dp(14)),belowSpace=Math.max(0,visible.bottom-location[1]-anchor.getHeight()-dp(14));if(!above&&!dropping&&belowSpace<dp(44)&&aboveSpace>belowSpace)above=true;int available=above?aboveSpace:belowSpace;if(available==0){onDismiss.run();return;}items.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));int height=Math.min(items.getMeasuredHeight(),available);window.setWidth(width);window.setHeight(height);int left=Math.max(visible.left+dp(8),Math.min(location[0],visible.right-width-dp(8))),top=above?location[1]-height-dp(rising?10:6):location[1]+anchor.getHeight()+dp(6);int[] rootLocation=new int[2];anchor.getRootView().getLocationOnScreen(rootLocation);closing=false;surface.animate().cancel();resetItems();surface.setTranslationY(0);window.setTouchable(true);surface.setAlpha(1);surface.setScaleX(1);surface.setScaleY(1);window.showAtLocation(anchor,Gravity.TOP|Gravity.LEFT,left-rootLocation[0],top-rootLocation[1]);surface.setPivotX(Math.max(0,Math.min(width,location[0]+anchor.getWidth()/2f-left)));surface.setPivotY(Math.max(0,Math.min(height,location[1]+anchor.getHeight()/2f-top)));if(animated&&android.animation.ValueAnimator.areAnimatorsEnabled()){surface.setAlpha(0);if(rising){surface.setTranslationY(dp(8));revealItems();}else if(dropping){surface.setTranslationY(-dp(8));surface.animate().alpha(1).translationY(0).setDuration(180).start();}else{surface.setScaleX(.97f);surface.setScaleY(.97f);surface.animate().alpha(1).scaleX(1).scaleY(1).setDuration(150).start();}}}
    void dismiss(){if(closing||!window.isShowing())return;closing=true;surface.animate().cancel();cancelRise();for(int i=0;i<items.getChildCount();i++)items.getChildAt(i).animate().cancel();if(animated&&android.animation.ValueAnimator.areAnimatorsEnabled()){window.setTouchable(false);window.update();surface.animate().alpha(0).setDuration(150).withEndAction(window::finishDismiss).start();}else window.finishDismiss();}
    void dismissImmediately(){surface.animate().cancel();resetItems();if(window.isShowing())window.finishDismiss();}
    boolean isShowing(){return window.isShowing();}
}
