package com.termux.app.workspace;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;

/** Native v35 anchored menu. Opening it never executes an item callback. */
final class WorkspacePopover {
    private final PopupWindow window;
    private final LinearLayout items;
    private final Context context;
    private Runnable onDismiss=()->{};
    WorkspacePopover(Context c){context=c;items=new LinearLayout(c);items.setOrientation(LinearLayout.VERTICAL);items.setPadding(dp(5),dp(5),dp(5),dp(5));
        ScrollView scroll=new ScrollView(c);scroll.setFillViewport(true);scroll.addView(items);
        window=new PopupWindow(scroll,dp(210),-2,true);GradientDrawable background=new GradientDrawable();background.setColor(WorkspaceDialogs.SURFACE);background.setCornerRadius(dp(13));background.setStroke(dp(1),WorkspaceDialogs.LINE);window.setBackgroundDrawable(background);window.setElevation(dp(8));window.setOutsideTouchable(true);window.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING|WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED);
    }
    private int dp(int n){return WorkspaceDialogs.dp(context,n);}
    void item(String title,int icon,Runnable action){TextView row=new TextView(context);row.setText(title);row.setTextSize(12);row.setTextColor(WorkspaceDialogs.TEXT);row.setMinHeight(dp(44));row.setPadding(dp(9),dp(9),dp(9),dp(9));row.setGravity(Gravity.CENTER_VERTICAL);if(icon!=0){android.graphics.drawable.Drawable image=context.getDrawable(icon).mutate();image.setTint(WorkspaceDialogs.MUTED);image.setBounds(0,0,dp(18),dp(18));row.setCompoundDrawables(image,null,null,null);row.setCompoundDrawablePadding(dp(9));}row.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(WorkspaceDialogs.TINT),null,null));row.setOnClickListener(v->{dismiss();action.run();});items.addView(row,new LinearLayout.LayoutParams(-1,-2));}
    void dismissed(Runnable action){onDismiss=action;window.setOnDismissListener(action::run);}
    void show(View anchor){show(anchor,true);}
    void showBelow(View anchor){show(anchor,false);}
    private void show(View anchor,boolean above){android.graphics.Rect visible=new android.graphics.Rect();anchor.getWindowVisibleDisplayFrame(visible);int[] location=new int[2];anchor.getLocationOnScreen(location);int width=Math.min(dp(210),Math.max(dp(1),visible.width()-dp(16)));int aboveSpace=Math.max(0,location[1]-visible.top-dp(14)),belowSpace=Math.max(0,visible.bottom-location[1]-anchor.getHeight()-dp(14));if(!above&&belowSpace<dp(44)&&aboveSpace>belowSpace)above=true;int available=above?aboveSpace:belowSpace;if(available==0){onDismiss.run();return;}items.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));int height=Math.min(items.getMeasuredHeight(),available);window.setWidth(width);window.setHeight(height);int left=Math.max(visible.left+dp(8),Math.min(location[0],visible.right-width-dp(8))),top=above?location[1]-height-dp(6):location[1]+anchor.getHeight()+dp(6);int[] rootLocation=new int[2];anchor.getRootView().getLocationOnScreen(rootLocation);window.showAtLocation(anchor,Gravity.TOP|Gravity.LEFT,left-rootLocation[0],top-rootLocation[1]);}
    void dismiss(){window.dismiss();}
    boolean isShowing(){return window.isShowing();}
}
