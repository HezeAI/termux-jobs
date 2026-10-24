package com.termux.app.workspace;

import android.app.Activity;
import android.app.AlertDialog;
import android.widget.*;

/** Irreversible deletion always needs a fresh, unchecked acknowledgement. */
final class StorageDeletionConsent {
    private static String t(Activity a,String ru,String en){return WorkspaceLanguage.code(a).equals("ru")?ru:en;}
    static AlertDialog show(Activity activity,String title,String summary,Runnable confirmed){
        LinearLayout body=new LinearLayout(activity);body.setOrientation(LinearLayout.VERTICAL);int pad=WorkspaceDialogs.dp(activity,16);body.setPadding(pad,0,pad,pad);
        TextView text=new TextView(activity);text.setText(summary);text.setTextColor(WorkspaceDialogs.TEXT);text.setTextSize(14);body.addView(text);
        CheckBox consent=new CheckBox(activity);consent.setText(t(activity,"Данные могут быть потеряны навсегда","Data may be permanently lost"));consent.setTextColor(WorkspaceDialogs.DANGER);body.addView(consent);
        ScrollView scroll=new ScrollView(activity);scroll.addView(body);
        AlertDialog dialog=WorkspaceDialogs.builder(activity).blockOutsideDismiss().setTitle(title).setView(scroll)
            .setNegativeButton(t(activity,"Отмена","Cancel"),null).setPositiveButton(t(activity,"Удалить","Delete"),null).create();
        dialog.setCancelable(false);
        dialog.show();
            Button button=dialog.getButton(AlertDialog.BUTTON_POSITIVE);button.setTextColor(WorkspaceDialogs.DANGER);button.setEnabled(false);
            consent.setOnCheckedChangeListener((v,checked)->button.setEnabled(checked));
            button.setOnClickListener(v->{if(!consent.isChecked())return;dialog.dismiss();confirmed.run();});
        return dialog;
    }
}
