package com.termux.app.workspace;

import android.app.Activity;
import android.app.Dialog;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.*;
import android.widget.*;
import com.termux.R;
import java.util.function.BooleanSupplier;

/** Presentation only: moves the existing host, never starts/stops an X11 process. */
final class DesktopFullscreen {
    private final Activity activity;
    private final Runnable keyboard;
    private final BooleanSupplier keyboardEnabled;
    private WorkspaceIconButton inputButton,keysButton;
    private final Runnable extraKeys;
    private final BooleanSupplier extraKeysEnabled;
    private LinearLayout controlsRow;
    private final View decor;
    private final ViewTreeObserver.OnPreDrawListener imeObserver;
    private final View.OnLayoutChangeListener layout=(v,l,t,r,b,ol,ot,or,ob)->schedule();
    private View content;
    private ViewGroup home;
    private ViewGroup.LayoutParams homeParams;
    private int homeIndex,originalOrientation;
    private boolean observing,suppressed,moving,queued,closed;
    private Dialog dialog;
    private FrameLayout canvas;
    private Dialog controls;

    DesktopFullscreen(Activity activity,Runnable keyboard,BooleanSupplier keyboardEnabled,Runnable observeIme){
        this(activity,keyboard,keyboardEnabled,observeIme,()->{},()->false);
    }
    DesktopFullscreen(Activity activity,Runnable keyboard,BooleanSupplier keyboardEnabled,Runnable observeIme,Runnable extraKeys,BooleanSupplier extraKeysEnabled){
        this.activity=activity;this.keyboard=keyboard;this.keyboardEnabled=keyboardEnabled;this.extraKeys=extraKeys;this.extraKeysEnabled=extraKeysEnabled;
        imeObserver=()->{observeIme.run();refreshKeyboard();return true;};
        decor=activity.getWindow().getDecorView();
    }
    void sync(View next){
        if(closed)return;
        if(next!=content){leave();content=next;suppressed=false;}
        if(content==null){stopObserving();return;}
        if(!observing){
            observing=true;originalOrientation=activity.getRequestedOrientation();
            decor.addOnLayoutChangeListener(layout);
            // Limited to the visible desktop. Restore the application's policy on exit.
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
        }
        update();
    }
    private void schedule(){
        if(queued||closed||!observing)return;queued=true;
        decor.post(()->{queued=false;if(!closed&&observing)update();});
    }
    private void update(){
        if(moving||content==null||activity.isFinishing()||activity.isDestroyed())return;
        boolean landscape=activity.getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE;
        if(!landscape){suppressed=false;leave();}
        else if(!suppressed&&dialog==null&&content.getParent() instanceof ViewGroup)enter();
    }
    private void enter(){
        moving=true;
        home=(ViewGroup)content.getParent();homeIndex=home.indexOfChild(content);homeParams=content.getLayoutParams();
        home.removeView(content);
        canvas=new FrameLayout(activity);canvas.setBackgroundColor(Color.BLACK);
        canvas.setOnApplyWindowInsetsListener((v,insets)->{positionControls(insets);return insets;});
        canvas.addView(content,new FrameLayout.LayoutParams(-1,-1));
        Dialog next=new Dialog(activity);dialog=next;next.requestWindowFeature(Window.FEATURE_NO_TITLE);
        next.setContentView(canvas);next.setCanceledOnTouchOutside(false);
        next.setOnCancelListener(d->{if(dialog==next){suppressed=true;leave();}});
        next.setOnDismissListener(d->{if(dialog==next){suppressed=true;leave();}});
        Window window=next.getWindow();
        window.setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        if(android.os.Build.VERSION.SDK_INT>=28){WindowManager.LayoutParams attributes=window.getAttributes();attributes.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;window.setAttributes(attributes);}
        // Immersive desktop; the separate input control is shown after this window.
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        next.show();window.setLayout(-1,-1);
        window.getDecorView().getViewTreeObserver().addOnPreDrawListener(imeObserver);
        showControls(window.getDecorView());moving=false;
    }
    private void showControls(View anchor){
        LinearLayout row=new LinearLayout(activity);controlsRow=row;row.setPadding(dp(2),dp(2),dp(2),dp(2));
        row.setBackground(WorkspaceDialogs.background(activity,false));
        inputButton=addControl(row,R.drawable.workspace_interactive_input,tr("Интерактивный ввод","Interactive input"),()->{keyboard.run();refreshKeyboard();});
        keysButton=addControl(row,R.drawable.workspace_extra_keys,tr("Дополнительные клавиши","Additional keys"),()->{extraKeys.run();refreshKeyboard();});
        refreshKeyboard();
        // A sibling application window is above the embedded Z-on-top SurfaceView.
        // A non-focusable PopupWindow subwindow can remain below that surface.
        // Keep focus/IME ownership in the desktop window and pass outside touches through.
        controls=new Dialog(activity);controls.requestWindowFeature(Window.FEATURE_NO_TITLE);
        controls.setContentView(row);controls.setCancelable(false);controls.setCanceledOnTouchOutside(false);
        Window window=controls.getWindow();
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_FULLSCREEN|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        window.getDecorView().setSystemUiVisibility(anchor.getSystemUiVisibility());
        WindowManager.LayoutParams attributes=window.getAttributes();
        attributes.gravity=Gravity.TOP|Gravity.RIGHT;attributes.alpha=.65f;
        int[] offset=controlOffset(anchor.getRootWindowInsets());attributes.x=offset[0];attributes.y=offset[1];
        if(android.os.Build.VERSION.SDK_INT>=28)attributes.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        window.setAttributes(attributes);controls.show();window.setLayout(dp(84),dp(44));
    }
    private int[] controlOffset(WindowInsets insets){
        int right=0,top=0;
        if(android.os.Build.VERSION.SDK_INT>=28&&insets!=null&&insets.getDisplayCutout()!=null){right=insets.getDisplayCutout().getSafeInsetRight();top=insets.getDisplayCutout().getSafeInsetTop();}
        return new int[]{dp(8)+right,dp(48)+top};
    }
    private void positionControls(WindowInsets insets){
        if(controls==null||!controls.isShowing())return;
        int[] offset=controlOffset(insets);Window window=controls.getWindow();WindowManager.LayoutParams attributes=window.getAttributes();
        if(attributes.x==offset[0]&&attributes.y==offset[1])return;
        attributes.x=offset[0];attributes.y=offset[1];window.setAttributes(attributes);
    }
    private void refreshKeyboard(){
        if(inputButton==null)return;
        boolean enabled=keyboardEnabled.getAsBoolean();
        boolean keys=extraKeysEnabled.getAsBoolean();
        if(inputButton.isSelected()==enabled&&keysButton!=null&&keysButton.isSelected()==keys)return;
        inputButton.setSelected(enabled);inputButton.setBackground(WorkspaceDialogs.background(activity,enabled));
        if(keysButton!=null){keysButton.setSelected(keys);keysButton.setBackground(WorkspaceDialogs.background(activity,keys));}
        controlsRow.setBackground(WorkspaceDialogs.background(activity,enabled||keys));
    }
    private WorkspaceIconButton addControl(LinearLayout row,int icon,String description,Runnable action){
        WorkspaceIconButton button=new WorkspaceIconButton(activity);button.setTextColor(WorkspaceDialogs.ACCENT);
        button.setIcon(androidx.appcompat.content.res.AppCompatResources.getDrawable(activity,icon).mutate(),dp(22));button.setContentDescription(description);
        button.setOnClickListener(v->action.run());row.addView(button,new LinearLayout.LayoutParams(dp(40),dp(40)));return button;
    }
    boolean back(){if(dialog==null)return false;suppressed=true;leave();return true;}
    boolean isFullscreen(){return dialog!=null;}
    private void leave(){
        if(dialog==null)return;moving=true;
        Dialog old=dialog;dialog=null;
        if(controls!=null){controls.dismiss();controls=null;}inputButton=null;keysButton=null;controlsRow=null;
        // Do not resurrect a host that its owner has already removed/closed.
        if(content!=null&&content.getParent()==canvas){
            canvas.removeView(content);
            if(home!=null)home.addView(content,Math.min(homeIndex,home.getChildCount()),homeParams);
        }
        ViewTreeObserver observer=old.getWindow().getDecorView().getViewTreeObserver();
        if(observer.isAlive())observer.removeOnPreDrawListener(imeObserver);
        old.setOnCancelListener(null);old.setOnDismissListener(null);old.dismiss();
        canvas=null;home=null;homeParams=null;moving=false;
    }
    private void stopObserving(){
        if(!observing)return;observing=false;decor.removeOnLayoutChangeListener(layout);
        activity.setRequestedOrientation(originalOrientation);
    }
    void close(){if(closed)return;leave();content=null;stopObserving();closed=true;}
    private int dp(int n){return WorkspaceDialogs.dp(activity,n);}
    private String tr(String ru,String en){return WorkspaceLanguage.code(activity).equals("ru")?ru:en;}
}
