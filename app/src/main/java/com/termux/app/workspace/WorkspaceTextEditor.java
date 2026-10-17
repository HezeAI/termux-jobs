package com.termux.app.workspace;

import android.app.AlertDialog;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import com.termux.app.TermuxActivity;
import java.util.concurrent.ExecutorService;

/** In-memory text editor: no autosave of project contents or secrets. */
final class WorkspaceTextEditor {
    private final TermuxActivity activity;private final ProjectFiles files;private final ExecutorService worker;
    private final TextFileEdits.Snapshot original;private final Runnable saved,dismissed;
    private AlertDialog dialog;private EditText input;private TextView status;private boolean closed,saving;
    WorkspaceTextEditor(TermuxActivity activity,ProjectFiles files,ExecutorService worker,TextFileEdits.Snapshot original,Runnable saved){this(activity,files,worker,original,saved,()->{});}
    WorkspaceTextEditor(TermuxActivity activity,ProjectFiles files,ExecutorService worker,TextFileEdits.Snapshot original,Runnable saved,Runnable dismissed){this.activity=activity;this.files=files;this.worker=worker;this.original=original;this.saved=saved;this.dismissed=dismissed;}
    private int dp(int n){return WorkspaceDialogs.dp(activity,n);}
    void show(){
        LinearLayout body=new LinearLayout(activity);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(16),0,dp(16),dp(12));
        TextView path=new TextView(activity);path.setText(original.path);path.setTextColor(WorkspaceDialogs.MUTED);path.setTextSize(12);path.setMaxLines(2);body.addView(path);
        input=new EditText(activity);input.setTextColor(WorkspaceDialogs.TEXT);input.setHintTextColor(WorkspaceDialogs.MUTED);input.setTextSize(14);input.setTypeface(Typeface.MONOSPACE);input.setGravity(Gravity.TOP);
        input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);input.setImeOptions(EditorInfo.IME_ACTION_NONE|EditorInfo.IME_FLAG_NO_EXTRACT_UI);input.setSingleLine(false);input.setHorizontallyScrolling(false);input.setVerticalScrollBarEnabled(true);input.setSaveEnabled(false);
        input.setContentDescription(WorkspaceText.tr("Текст файла"));input.setText(original.text);body.addView(input,new LinearLayout.LayoutParams(-1,Math.min(dp(360),activity.getResources().getDisplayMetrics().heightPixels/3)));
        status=new TextView(activity);status.setTextColor(WorkspaceDialogs.MUTED);status.setTextSize(12);status.setText(WorkspaceText.tr("UTF-8 · до 256 КБ · Enter переносит строку"));body.addView(status);
        dialog=WorkspaceDialogs.builder(activity).setTitle(WorkspaceText.tr("Редактировать")+" · "+new java.io.File(original.path).getName()).setView(body).setPositiveButton(WorkspaceText.tr("Сохранить"),null).setNegativeButton(WorkspaceText.tr("Отмена"),null).create();
        dialog.setCanceledOnTouchOutside(false);dialog.setOnKeyListener((d,key,event)->{if(key!=KeyEvent.KEYCODE_BACK)return false;if(event.getAction()==KeyEvent.ACTION_UP)requestClose();return true;});dialog.setOnDismissListener(d->close());
        dialog.setOnShowListener(d->{dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->save());dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v->requestClose());});dialog.show();
    }
    private void requestClose(){
        if(saving){status.setText(WorkspaceText.tr("Дождитесь завершения сохранения"));return;}
        if(input.getText().toString().equals(original.text)){close();return;}
        WorkspaceDialogs.builder(activity).setTitle(WorkspaceText.tr("Закрыть без сохранения?"))
            .setMessage(WorkspaceText.tr("Изменения текста будут потеряны."))
            .setPositiveButton(WorkspaceText.tr("Закрыть без сохранения"),(d,w)->close()).setNegativeButton(WorkspaceText.tr("Продолжить редактирование"),null).show();
    }
    private void save(){
        if(closed||saving)return;String text=input.getText().toString();try{TextFileEdits.encode(text);}catch(Exception e){status.setText(e.getMessage());return;}
        if(text.equals(original.text)){close();return;}saving=true;input.setEnabled(false);dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);status.setText(WorkspaceText.tr("Сохранение…"));
        worker.execute(()->{String error=null;try{files.editSave(original,text);}catch(Exception e){error=e.getMessage();}String result=error;activity.runOnUiThread(()->{if(closed)return;saving=false;if(result!=null){input.setEnabled(true);dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(true);status.setText(result);return;}close();saved.run();activity.showToast(WorkspaceText.tr("Файл сохранён"),false);});});
    }
    String serverProfileId(){return files.profile.id;}
    void close(){if(closed)return;closed=true;if(dialog!=null&&dialog.isShowing())dialog.dismiss();dismissed.run();}
}
