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
    static final int ACCENT = 0xff79dfcd, TEXT = 0xffedf5f6, MUTED = 0xff91a6af, DANGER = 0xffff8f8f;
    private WorkspaceDialogs() {}
    static int dp(Context c, float n) { return Math.round(c.getResources().getDisplayMetrics().density * n); }
    static Builder builder(Context c) { return new Builder(c); }
    static final class Builder extends AlertDialog.Builder {
        private boolean blockOutside;
        Builder(Context c) { super(c, R.style.WorkspaceDialog); }
        Builder blockOutsideDismiss(){blockOutside=true;return this;}
        @Override public AlertDialog create(){AlertDialog dialog=super.create();if(blockOutside)dialog.setCanceledOnTouchOutside(false);return dialog;}
        @Override public AlertDialog.Builder setItems(CharSequence[] items, DialogInterface.OnClickListener listener) {
            return super.setAdapter(new Rows(getContext(), items, -1), listener);
        }
        @Override public AlertDialog.Builder setSingleChoiceItems(CharSequence[] items, int selected, DialogInterface.OnClickListener listener) {
            return super.setSingleChoiceItems(new Rows(getContext(), items, selected), selected, listener);
        }
    }
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
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, panel(c, true));
        states.addState(new int[]{android.R.attr.state_focused}, panel(c, true));
        states.addState(new int[]{}, panel(c, selected)); return states;
    }
    private static Drawable panel(Context c, boolean highlight) {
        GradientDrawable panel = new GradientDrawable(); panel.setColor(highlight ? 0xff123b36 : 0xff0b1319);
        panel.setCornerRadius(dp(c, 12)); panel.setStroke(dp(c, 1), highlight ? ACCENT : 0xff213039);
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
