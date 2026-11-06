package com.termux.app.workspace;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;

/** Bounded native touch feedback; Android owns hotspot, scroll cancellation and release. */
public final class WorkspaceRipple {
    private WorkspaceRipple(){}
    private static final java.util.Set<android.view.View> installed=java.util.Collections.newSetFromMap(new java.util.WeakHashMap<android.view.View,Boolean>());
    public static void install(android.view.Window window){
        if(window==null)return;android.view.View root=window.getDecorView();if(!installed.add(root))return;
        java.util.WeakHashMap<android.view.View,Drawable> seen=new java.util.WeakHashMap<>();
        root.getViewTreeObserver().addOnGlobalLayoutListener(()->styleTree(root,seen));
    }
    private static void styleTree(android.view.View view,java.util.WeakHashMap<android.view.View,Drawable> seen){
        if(view instanceof com.termux.view.TerminalView||view instanceof android.view.SurfaceView||view instanceof android.webkit.WebView||view.getClass().getName().startsWith("com.termux.x11."))return;
        if(!(view instanceof android.widget.EditText)&&!(view instanceof android.widget.AbsSeekBar)&&!(view instanceof android.widget.CompoundButton)
            &&!(view instanceof android.widget.TextView&&((android.widget.TextView)view).isTextSelectable())&&view.hasOnClickListeners()){
            Drawable old=view.getBackground();
            if(!seen.containsKey(view)||seen.get(view)!=old){
                if(old instanceof RippleDrawable){RippleDrawable ripple=(RippleDrawable)old;ripple.setColor(ColorStateList.valueOf(0x18ecece5));
                    if(ripple.findDrawableByLayerId(android.R.id.mask)==null){GradientDrawable mask=new GradientDrawable();mask.setColor(android.graphics.Color.WHITE);mask.setCornerRadius(WorkspaceDialogs.dp(view.getContext(),10));int layer=ripple.addLayer(mask);ripple.setId(layer,android.R.id.mask);}
                }else{float radius=old instanceof GradientDrawable?((GradientDrawable)old).getCornerRadius()/view.getResources().getDisplayMetrics().density:10;
                    int left=view.getPaddingLeft(),top=view.getPaddingTop(),right=view.getPaddingRight(),bottom=view.getPaddingBottom();
                    view.setBackground(background(view.getContext(),old,radius));view.setPadding(left,top,right,bottom);}
                seen.put(view,view.getBackground());
            }
        }
        if(view instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)view;for(int i=0;i<group.getChildCount();i++)styleTree(group.getChildAt(i),seen);}
    }
    static Drawable background(Context context,Drawable content,float radius){
        GradientDrawable mask=new GradientDrawable();mask.setColor(android.graphics.Color.WHITE);mask.setCornerRadius(WorkspaceDialogs.dp(context,radius));
        if(!ValueAnimator.areAnimatorsEnabled()){
            GradientDrawable focus=new GradientDrawable();focus.setColor(0x18ecece5);focus.setCornerRadius(WorkspaceDialogs.dp(context,radius));
            StateListDrawable states=new StateListDrawable();
            Drawable feedback=content==null?focus:new android.graphics.drawable.LayerDrawable(new Drawable[]{content,focus});
            states.addState(new int[]{android.R.attr.state_pressed},feedback);
            states.addState(new int[]{android.R.attr.state_focused},feedback);
            states.addState(new int[]{},content==null?new android.graphics.drawable.ColorDrawable(0):content);
            return states;
        }
        return new RippleDrawable(ColorStateList.valueOf(0x18ecece5),content,mask);
    }
}
