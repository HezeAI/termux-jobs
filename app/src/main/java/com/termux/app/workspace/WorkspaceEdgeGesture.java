package com.termux.app.workspace;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Build;
import android.view.*;
import android.widget.TextView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.termux.shared.view.KeyboardUtils;
import java.util.*;
import java.util.function.*;

/** A touch gesture, not a remapping of Back. The IME decision is latched on DOWN. */
final class WorkspaceEdgeGesture {
    private final Activity activity;
    private final ViewGroup root;
    private final BooleanSupplier allowed;
    private final Supplier<WorkspaceSidebar> create;
    private final ViewTreeObserver.OnGlobalLayoutListener layout=this::updateExclusion;
    private WorkspaceSidebar drag;
    private boolean candidate,claimed,closed;
    private float startX,startY;
    private long started;
    private List<Rect> exclusion=Collections.emptyList();
    WorkspaceEdgeGesture(Activity a,ViewGroup root,BooleanSupplier allowed,Supplier<WorkspaceSidebar> create){this.activity=a;this.root=root;this.allowed=allowed;this.create=create;root.getViewTreeObserver().addOnGlobalLayoutListener(layout);root.setOnApplyWindowInsetsListener((v,insets)->{v.post(this::updateExclusion);return v.onApplyWindowInsets(insets);});updateExclusion();}
    private int dp(int n){return WorkspaceDialogs.dp(activity,n);}
    boolean keyboardVisible(){WindowInsetsCompat insets=ViewCompat.getRootWindowInsets(root);return insets!=null&&insets.isVisible(WindowInsetsCompat.Type.ime())||KeyboardUtils.isSoftKeyboardVisible(activity);}
    private boolean selection(View v){if(v instanceof TextView&&((TextView)v).hasSelection())return true;if(v instanceof ViewGroup)for(int n=0;n<((ViewGroup)v).getChildCount();n++){View child=((ViewGroup)v).getChildAt(n);if(child.getVisibility()==View.VISIBLE&&selection(child))return true;}return false;}
    void updateExclusion(){if(closed)return;if(drag!=null&&!drag.isPreview())drag=null;if((!allowed.getAsBoolean()||keyboardVisible())&&drag!=null){drag.dismiss();drag=null;candidate=false;claimed=false;}if(Build.VERSION.SDK_INT<29)return;
        // Android caps the vertical exclusion budget at 200dp per edge. Never exclude the right edge.
        List<Rect> next=Collections.emptyList();if(allowed.getAsBoolean()&&!keyboardVisible()&&!selection(root)&&root.getHeight()>0){int height=Math.min(dp(200),root.getHeight()),top=(root.getHeight()-height)/2;next=Collections.singletonList(new Rect(0,top,dp(24),top+height));}
        if(!next.equals(exclusion)){exclusion=next;root.setSystemGestureExclusionRects(next);}
    }
    boolean touch(MotionEvent event){if(closed)return false;int action=event.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN){if(drag!=null&&drag.isPreview())drag.dismiss();drag=null;candidate=false;claimed=false;updateExclusion();int[] location=new int[2];root.getLocationOnScreen(location);startX=event.getRawX();startY=event.getRawY();started=event.getEventTime();candidate=allowed.getAsBoolean()&&!keyboardVisible()&&!selection(root)&&event.getPointerCount()==1&&startX>=location[0]&&startX<=location[0]+dp(24)&&startY>=location[1]&&startY<location[1]+root.getHeight();return false;}
        boolean consumed=claimed;
        if(!candidate&&!claimed)return false;
        if(event.getPointerCount()!=1||!allowed.getAsBoolean()||keyboardVisible()||action==MotionEvent.ACTION_CANCEL){if(drag!=null)drag.finishDrag(false);drag=null;candidate=false;claimed=false;return consumed;}
        float dx=event.getRawX()-startX,dy=event.getRawY()-startY;
        if(action==MotionEvent.ACTION_MOVE){
            if(!claimed){int slop=ViewConfiguration.get(activity).getScaledTouchSlop();if(Math.abs(dy)>slop&&Math.abs(dy)>=Math.abs(dx)||dx < -slop){candidate=false;return false;}if(dx<=slop||dx<Math.abs(dy)*1.5f)return false;drag=create.get();drag.beginDrag(root);claimed=true;}
            drag.dragTo(Math.max(0,dx));return true;
        }
        if(action==MotionEvent.ACTION_UP){if(drag!=null){float speed=dx*1000f/Math.max(1,event.getEventTime()-started);drag.finishDrag(dx>=drag.panelWidth()*.35f||dx>dp(48)&&speed>dp(700));}candidate=false;claimed=false;return consumed;}
        return consumed;
    }
    boolean claimed(){return claimed;}
    void close(){closed=true;if(drag!=null&&drag.isPreview())drag.dismiss();drag=null;root.getViewTreeObserver().removeOnGlobalLayoutListener(layout);root.setOnApplyWindowInsetsListener(null);if(Build.VERSION.SDK_INT>=29)root.setSystemGestureExclusionRects(Collections.emptyList());}
}
