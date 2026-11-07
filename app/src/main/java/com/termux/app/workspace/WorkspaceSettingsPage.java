package com.termux.app.workspace;

import android.content.Context;
import android.graphics.drawable.*;
import android.content.res.ColorStateList;
import android.view.*;
import android.widget.*;

/** Native settings cards; callbacks retain the existing managers and persistence. */
final class WorkspaceSettingsPage {
    static TextView text(Context c,String value,float size,int color){TextView t=new TextView(c);t.setText(value);t.setTextSize(size);t.setTextColor(color);return t;}
    static LinearLayout group(LinearLayout parent,String name){Context c=parent.getContext();TextView title=text(c,name,17.6f,WorkspaceDialogs.TEXT);title.setTypeface(null,android.graphics.Typeface.BOLD);title.setPadding(0,dp(c,20),0,dp(c,9));parent.addView(title);LinearLayout card=new LinearLayout(c);card.setOrientation(LinearLayout.VERTICAL);GradientDrawable background=new GradientDrawable();background.setColor(WorkspaceDialogs.SURFACE);background.setCornerRadius(dp(c,20));background.setStroke(dp(c,1),WorkspaceDialogs.LINE);card.setBackground(background);card.setClipToOutline(true);parent.addView(card,new LinearLayout.LayoutParams(-1,-2));return card;}
    static LinearLayout row(LinearLayout card,String title,String description,int icon,Runnable action){Context c=card.getContext();separator(card);LinearLayout row=new LinearLayout(c);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(c,14),dp(c,14),dp(c,14),dp(c,14));row.setMinimumHeight(dp(c,56));row.setClickable(true);row.setFocusable(true);row.setBackground(WorkspaceRipple.background(c,null,0));
        if(icon!=0){ImageView image=new ImageView(c);image.setImageResource(icon);image.setColorFilter(WorkspaceDialogs.MUTED);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(c,18),dp(c,18));p.setMargins(0,0,dp(c,12),0);row.addView(image,p);}
        LinearLayout labels=new LinearLayout(c);labels.setOrientation(LinearLayout.VERTICAL);TextView name=text(c,title,16.4f,WorkspaceDialogs.TEXT);labels.addView(name);if(!description.isEmpty()){TextView subtitle=text(c,description,12.8f,WorkspaceDialogs.MUTED);subtitle.setPadding(0,dp(c,4),0,0);labels.addView(subtitle);}row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));TextView arrow=text(c,"›",16,WorkspaceDialogs.MUTED);arrow.setPadding(dp(c,8),0,0,0);row.addView(arrow);row.setOnClickListener(v->action.run());card.addView(row,new LinearLayout.LayoutParams(-1,-2));return row;}
    static void toggle(LinearLayout card,String title,String description,boolean checked,java.util.function.Consumer<Boolean> changed){LinearLayout row=row(card,title,description,0,()->{});row.removeViewAt(row.getChildCount()-1);Switch toggle=new Switch(card.getContext());toggle.setTag("automatic-app-updates");toggle.setContentDescription(title);toggle.setChecked(checked);toggle.setMinimumHeight(dp(card.getContext(),44));toggle.setThumbTintList(ColorStateList.valueOf(WorkspaceDialogs.ACCENT));toggle.setOnCheckedChangeListener((v,on)->changed.accept(on));row.addView(toggle,new LinearLayout.LayoutParams(-2,-2));row.setOnClickListener(v->toggle.toggle());}
    static void separator(LinearLayout card){if(card.getChildCount()==0)return;View line=new View(card.getContext());line.setBackgroundColor(WorkspaceDialogs.LINE);card.addView(line,new LinearLayout.LayoutParams(-1,dp(card.getContext(),1)));}
    private static int dp(Context c,int n){return WorkspaceDialogs.dp(c,n);}
}
