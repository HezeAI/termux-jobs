package com.termux.app.workspace;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.termux.R;

/** Shared appearance for workspace dialogs; no shell execution or persistent state. */
final class WorkspaceDialogs {
    static final int ACCENT = 0xffecece5, TEXT = 0xfff0f0ee, MUTED = 0xffa5a5a2, DANGER = 0xffff8f8f;
    static final int BG=0xff191a1c, SURFACE=0xff232426, LINE=0xff393a3c, TINT=0xff343531, USER=0xff303132;
    private WorkspaceDialogs() {}
    static int dp(Context c, float n) { return Math.round(c.getResources().getDisplayMetrics().density * n); }
    static Builder builder(Context c) { return new Builder(c); }
    static final class Builder extends AlertDialog.Builder {
        private boolean blockOutside;
        Builder(Context c) { super(c, R.style.WorkspaceDialog); }
        Builder blockOutsideDismiss(){blockOutside=true;return this;}
        @Override public AlertDialog create(){AlertDialog dialog=super.create();WorkspaceRipple.install(dialog.getWindow());dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE|android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);dialog.getWindow().getDecorView().addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){public void onViewAttachedToWindow(View v){v.post(()->editorWindow(dialog));}public void onViewDetachedFromWindow(View v){}});if(blockOutside)dialog.setCanceledOnTouchOutside(false);return dialog;}
        @Override public AlertDialog.Builder setItems(CharSequence[] items, DialogInterface.OnClickListener listener) {
            return super.setAdapter(new Rows(getContext(), items, -1), listener);
        }
        @Override public AlertDialog.Builder setSingleChoiceItems(CharSequence[] items, int selected, DialogInterface.OnClickListener listener) {
            return super.setSingleChoiceItems(new Rows(getContext(), items, selected), selected, listener);
        }
    }
    static void centerLibrary(AlertDialog dialog){
        android.view.Window window=dialog.getWindow();if(window==null)return;Context c=dialog.getContext();
        GradientDrawable background=new GradientDrawable();background.setColor(BG);background.setCornerRadius(dp(c,24));background.setStroke(dp(c,1),LINE);window.setBackgroundDrawable(background);
        window.setWindowAnimations(com.termux.R.style.WorkspaceLibraryAnimation);window.setGravity(Gravity.CENTER);
        Runnable resize=()->{int width=Math.max(dp(c,1),Math.min(dp(c,560),c.getResources().getDisplayMetrics().widthPixels-dp(c,24)));if(window.getAttributes().width!=width)window.setLayout(width,-2);};resize.run();
        window.getDecorView().addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->resize.run());
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE|android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED);window.getDecorView().setPadding(0,0,0,0);
    }
    static void editorWindow(AlertDialog dialog){if(dialog.getWindow()==null)return;if((dialog.getWindow().getAttributes().flags&android.view.WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)!=0)dialog.getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);int mode=(dialog.getWindow().getAttributes().softInputMode&android.view.WindowManager.LayoutParams.SOFT_INPUT_MASK_STATE)|android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;if(dialog.getWindow().getAttributes().softInputMode!=mode)dialog.getWindow().setSoftInputMode(mode);}
    static WorkspaceIconButton headerButton(Context c,int icon,String label,Runnable action){WorkspaceIconButton button=new WorkspaceIconButton(c);button.setIcon(c.getDrawable(icon),dp(c,21));button.setMinWidth(dp(c,48));button.setMinHeight(dp(c,48));button.setContentDescription(label);button.setTooltipText(label);button.setOnClickListener(v->action.run());headerState(button,false);return button;}
    static void headerAttention(WorkspaceIconButton button,WorkspaceTransferBorder border){
        Context c=button.getContext();GradientDrawable circle=new GradientDrawable();circle.setShape(GradientDrawable.OVAL);circle.setColor(SURFACE);circle.setStroke(dp(c,1),LINE);
        android.graphics.drawable.LayerDrawable content=new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{circle,border});
        // Install a single ripple root. Wrapping the current background afterward
        // lets View.setBackground clear the child callback owned by that wrapper.
        button.setBackground(WorkspaceRipple.background(c,content,24));border.follow(button);
    }
    static void headerState(TextView button,boolean selected){button.setSelected(selected);button.setTextColor(selected?BG:TEXT);GradientDrawable circle=new GradientDrawable();circle.setShape(GradientDrawable.OVAL);circle.setColor(selected?ACCENT:SURFACE);circle.setStroke(dp(button.getContext(),1),selected?ACCENT:LINE);button.setBackground(WorkspaceRipple.background(button.getContext(),circle,24));}
    /** Arm IME before EditText processes the first down, rather than after its first focus-taking click. */
    static void firstTouchEditor(android.widget.EditText editor,Runnable prepare){
        editor.setShowSoftInputOnFocus(false);final boolean[] pending={false},tap={false};final float[] down={0,0};
        Runnable show=()->{if(pending[0]&&editor.hasFocus()&&editor.hasWindowFocus()){pending[0]=false;((android.view.inputmethod.InputMethodManager)editor.getContext().getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(editor,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);}};
        editor.setOnTouchListener((v,event)->{int action=event.getActionMasked();if(action==android.view.MotionEvent.ACTION_DOWN){tap[0]=true;down[0]=event.getX();down[1]=event.getY();prepare.run();editor.setShowSoftInputOnFocus(true);editor.setCursorVisible(true);}else if(action==android.view.MotionEvent.ACTION_MOVE){int slop=android.view.ViewConfiguration.get(editor.getContext()).getScaledTouchSlop();if(Math.abs(event.getX()-down[0])>slop||Math.abs(event.getY()-down[1])>slop)tap[0]=false;}else if(action==android.view.MotionEvent.ACTION_CANCEL){tap[0]=false;pending[0]=false;}else if(action==android.view.MotionEvent.ACTION_UP&&tap[0]){pending[0]=true;editor.post(show);}return false;});
        // Accessibility/keyboard activation can click without a preceding pointer DOWN.
        editor.setOnClickListener(v->{prepare.run();editor.setShowSoftInputOnFocus(true);editor.requestFocus();pending[0]=true;editor.post(show);});
        editor.getViewTreeObserver().addOnWindowFocusChangeListener(focused->{if(focused)editor.post(show);});
        editor.setOnFocusChangeListener((v,focused)->{if(!focused)pending[0]=false;});
    }
    static Drawable cardBackground(Context c){GradientDrawable panel=new GradientDrawable();panel.setColor(SURFACE);panel.setCornerRadius(dp(c,20));panel.setStroke(dp(c,1),LINE);return panel;}
    static android.widget.ListAdapter choiceAdapter(Context c,CharSequence[] items,int selected,java.util.function.IntPredicate enabled){return new Rows(c,items,selected,enabled);}
    private static final class Rows extends ArrayAdapter<CharSequence> {
        private final int selected;private final java.util.function.IntPredicate enabled;
        Rows(Context c,CharSequence[] items,int selected){this(c,items,selected,position->true);}
        Rows(Context c,CharSequence[] items,int selected,java.util.function.IntPredicate enabled){super(c,0,items);this.selected=selected;this.enabled=enabled;}
        @Override public boolean isEnabled(int position){return enabled.test(position);}
        @Override public boolean areAllItemsEnabled(){for(int i=0;i<getCount();i++)if(!isEnabled(i))return false;return true;}
        @Override public View getView(int position, View recycled, ViewGroup parent) {
            Context c = getContext(); FrameLayout outer = new FrameLayout(c);
            outer.setPadding(dp(c, 16), dp(c, 4), dp(c, 16), dp(c, 4));
            String label = getItem(position).toString();
            TextView row = action(c, label, label.equals(WorkspaceText.tr("Удалить")) ? R.drawable.workspace_delete
                : label.equals(WorkspaceText.tr("Редактировать")) ? R.drawable.workspace_edit
                : label.contains(WorkspaceText.tr("избранное")) || label.contains(WorkspaceText.tr("избранного")) ? R.drawable.workspace_star : 0);
            row.setBackground(background(c, position == selected)); row.setDuplicateParentStateEnabled(true);
            outer.addView(row, new FrameLayout.LayoutParams(-1, -2));outer.setEnabled(isEnabled(position));outer.setAlpha(isEnabled(position)?1f:.4f);return outer;
        }
    }
    static TextView action(Context c, String label, int icon) {
        TextView row = new TextView(c); row.setText(label); row.setTextSize(15);
        row.setGravity(Gravity.CENTER_VERTICAL); row.setMinHeight(dp(c, 54));
        row.setPadding(dp(c, 16), dp(c, 12), dp(c, 16), dp(c, 12));
        int color = label.equals(WorkspaceText.tr("Удалить")) ? DANGER : TEXT; row.setTextColor(color);
        if (icon != 0) {
            Drawable image = c.getDrawable(icon).mutate(); image.setTint(color);
            image.setBounds(0, 0, dp(c, 22), dp(c, 22)); row.setCompoundDrawables(image, null, null, null);
            row.setCompoundDrawablePadding(dp(c, 14));
        }
        row.setBackground(background(c, false)); return row;
    }
    static Drawable background(Context c, boolean selected) {
        return WorkspaceRipple.background(c,panel(c,selected),12);
    }
    private static Drawable panel(Context c, boolean highlight) {
        GradientDrawable panel = new GradientDrawable(); panel.setColor(highlight ? 0xff343531 : 0xff232426);
        panel.setCornerRadius(dp(c, 12)); panel.setStroke(dp(c, 1), highlight ? ACCENT : 0xff393a3c);
        if (!highlight) return panel;
        GradientDrawable marker = new GradientDrawable(); marker.setColor(ACCENT); marker.setCornerRadius(dp(c, 2));
        LayerDrawable layers = new LayerDrawable(new Drawable[]{panel, marker});
        layers.setLayerSize(1, dp(c, 4), dp(c, 32)); layers.setLayerGravity(1, Gravity.LEFT | Gravity.CENTER_VERTICAL);
        return layers;
    }
    static LinearLayout commandRow(Context c, WorkspaceStore.Command command) {
        LinearLayout row = new LinearLayout(c); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(c, 16), dp(c, 14), dp(c, 16), dp(c, 14)); row.setBackground(background(c, false));
        LinearLayout labels = new LinearLayout(c); labels.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(c); title.setText(command.name); title.setTextSize(16); title.setTextColor(TEXT);
        title.setTypeface(null, Typeface.BOLD); labels.addView(title);
        TextView preview = new TextView(c); preview.setText(command.command); preview.setTextSize(12); preview.setTextColor(MUTED);
        preview.setTypeface(Typeface.MONOSPACE); preview.setMaxLines(2); preview.setEllipsize(android.text.TextUtils.TruncateAt.END);
        preview.setPadding(0, dp(c, 6), 0, dp(c, 6)); labels.addView(preview);
        TextView category = new TextView(c); category.setText(command.category); category.setTextSize(11); category.setTextColor(MUTED); labels.addView(category);
        row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        if (command.favorite) { TextView star = new TextView(c); star.setText("★"); star.setTextSize(24); star.setTextColor(ACCENT);
            star.setPadding(dp(c, 12), 0, 0, 0); star.setContentDescription(WorkspaceText.tr("Избранная команда")); row.addView(star); }
        return row;
    }
}
