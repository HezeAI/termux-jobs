package com.termux.app.workspace;

import android.os.*;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;
import com.termux.R;
import java.util.function.BooleanSupplier;

/** Local presentation only. Layout occupies its scaled size; no GPU View transform. */
final class AgentChatScale {
    static int clamp(int percent){return Math.max(50,Math.min(150,percent));}
    private static int size(int value,float factor){return value>0&&value<1000000?Math.max(1,Math.round(value*factor)):value;}
    private static final class Snapshot {
        final int left,top,right,bottom,minWidth,minHeight,width,height,ml,mt,mr,mb,textMinWidth,textMaxWidth,textMinHeight,textMaxHeight;
        final float textSize;
        int applied=100;
        Snapshot(View v){
            left=v.getPaddingLeft();top=v.getPaddingTop();right=v.getPaddingRight();bottom=v.getPaddingBottom();minWidth=v.getMinimumWidth();minHeight=v.getMinimumHeight();
            ViewGroup.LayoutParams lp=v.getLayoutParams();width=lp==null?-1:lp.width;height=lp==null?-1:lp.height;
            ViewGroup.MarginLayoutParams margins=lp instanceof ViewGroup.MarginLayoutParams?(ViewGroup.MarginLayoutParams)lp:null;
            ml=margins==null?0:margins.leftMargin;mt=margins==null?0:margins.topMargin;mr=margins==null?0:margins.rightMargin;mb=margins==null?0:margins.bottomMargin;
            TextView t=v instanceof TextView?(TextView)v:null;textSize=t==null?0:t.getTextSize();textMinWidth=t==null?0:t.getMinWidth();textMaxWidth=t==null?0:t.getMaxWidth();textMinHeight=t==null?0:t.getMinHeight();textMaxHeight=t==null?0:t.getMaxHeight();
        }
        void apply(View v,int percent){
            if(applied==percent)return;applied=percent;float factor=percent/100f;
            v.setPadding(size(left,factor),size(top,factor),size(right,factor),size(bottom,factor));v.setMinimumWidth(size(minWidth,factor));v.setMinimumHeight(size(minHeight,factor));
            ViewGroup.LayoutParams lp=v.getLayoutParams();if(lp!=null){boolean changed=false;if(width>0&&lp.width>0){lp.width=size(width,factor);changed=true;}if(height>0&&lp.height>0){lp.height=size(height,factor);changed=true;}
                if(lp instanceof ViewGroup.MarginLayoutParams){ViewGroup.MarginLayoutParams m=(ViewGroup.MarginLayoutParams)lp;m.setMargins(size(ml,factor),size(mt,factor),size(mr,factor),size(mb,factor));changed=true;}if(changed)v.setLayoutParams(lp);}
            if(v instanceof TextView){TextView t=(TextView)v;t.setTextSize(TypedValue.COMPLEX_UNIT_PX,textSize*factor);if(textMinWidth>=0)t.setMinWidth(size(textMinWidth,factor));if(textMaxWidth>=0)t.setMaxWidth(size(textMaxWidth,factor));if(textMinHeight>=0)t.setMinHeight(size(textMinHeight,factor));if(textMaxHeight>=0)t.setMaxHeight(size(textMaxHeight,factor));}
        }
    }
    static void apply(View v,int percent){
        Snapshot snapshot=(Snapshot)v.getTag(R.id.agent_chat_scale_snapshot);if(snapshot==null){snapshot=new Snapshot(v);v.setTag(R.id.agent_chat_scale_snapshot,snapshot);}snapshot.apply(v,clamp(percent));
        if(v instanceof ViewGroup){ViewGroup group=(ViewGroup)v;for(int i=0;i<group.getChildCount();i++)apply(group.getChildAt(i),percent);}
    }
    static void prepare(View v){if(v==null)return;apply(v,100);forget(v);}
    private static void forget(View v){v.setTag(R.id.agent_chat_scale_snapshot,null);if(v instanceof ViewGroup){ViewGroup group=(ViewGroup)v;for(int i=0;i<group.getChildCount();i++)forget(group.getChildAt(i));}}
    static void controls(LinearLayout body,WorkspaceStore.Session profile,BooleanSupplier owns,Runnable preview,Runnable save){
        TextView title=AgentStatusUi.info(body,"");title.setTag("agent-chat-scale-label");
        SeekBar slider=new SeekBar(body.getContext());slider.setTag("agent-chat-scale");slider.setContentDescription(AgentStatusUi.tr(body.getContext(),"Масштаб чата","Chat scale"));slider.setMax(100);slider.setProgress(clamp(profile.chatScale)-50);body.addView(slider,new LinearLayout.LayoutParams(-1,-2));
        Runnable label=()->title.setText(AgentStatusUi.tr(body.getContext(),"Масштаб чата: ","Chat scale: ")+clamp(profile.chatScale)+"%");label.run();
        Handler handler=new Handler(Looper.getMainLooper());boolean[] dirty={false};Runnable persist=()->{if(dirty[0]){dirty[0]=false;save.run();}};
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar bar,int value,boolean user){if(!owns.getAsBoolean())return;int next=clamp(value+50);if(profile.chatScale==next)return;profile.chatScale=next;label.run();preview.run();dirty[0]=true;handler.removeCallbacks(persist);handler.postDelayed(persist,250);}public void onStartTrackingTouch(SeekBar bar){}public void onStopTrackingTouch(SeekBar bar){handler.removeCallbacks(persist);persist.run();}});
        slider.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){public void onViewAttachedToWindow(View v){}public void onViewDetachedFromWindow(View v){handler.removeCallbacks(persist);persist.run();}});
        TextView reset=AgentStatusUi.row(body,"100%","chat-scale-reset",()->{if(owns.getAsBoolean()){slider.setProgress(50);handler.removeCallbacks(persist);persist.run();}});reset.setContentDescription(AgentStatusUi.tr(body.getContext(),"Сбросить масштаб чата до 100%","Reset chat scale to 100%"));
    }
}
