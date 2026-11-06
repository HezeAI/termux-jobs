package com.termux.app.workspace;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.widget.EditText;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Visible Android recognizer activity; the result only edits the original unchanged draft. */
final class AgentDictation {
    static final int REQUEST=7542;
    private final Activity activity;private final EditText input;private final Supplier<String> owner;private final Consumer<String> notice;
    private String pendingOwner,original;
    AgentDictation(Activity activity,EditText input,Supplier<String> owner,Consumer<String> notice){this.activity=activity;this.input=input;this.owner=owner;this.notice=notice;}
    void start(){
        if(pendingOwner!=null)return;String id=owner.get();if(id==null||id.isEmpty()){notice.accept(tr("Откройте агентский чат для диктовки. Черновик сохранён.","Open an agent chat to dictate. Draft retained."));return;}
        pendingOwner=id;original=input.getText().toString();
        Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1).putExtra(RecognizerIntent.EXTRA_PROMPT,tr("Диктовка в черновик","Dictate into draft"));
        try{activity.startActivityForResult(intent,REQUEST);}catch(ActivityNotFoundException failure){clear();notice.accept(tr("На устройстве нет доступного приложения для окна распознавания речи Android. Включите или установите совместимое приложение распознавания либо используйте диктовку клавиатуры. Черновик сохранён.","No app is available for the Android speech recognition screen. Enable or install a compatible recognition app, or use keyboard dictation. Draft retained."));}
        catch(SecurityException failure){clear();notice.accept(tr("Android отклонил запуск распознавания. Проверьте разрешения приложения распознавания. Черновик сохранён.","Android denied speech recognition. Check the recognition app’s permissions. Draft retained."));}
    }
    boolean result(int code,int result,Intent data){
        if(code!=REQUEST)return false;
        String id=pendingOwner,held=original;clear();
        if(id==null)return true;
        if(result==Activity.RESULT_CANCELED)return true;
        if(result!=Activity.RESULT_OK){notice.accept(tr("Диктовка отменена или не завершена. Черновик сохранён.","Dictation cancelled or incomplete. Draft retained."));return true;}
        ArrayList<String> choices=data==null?null:data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        if(choices==null||choices.isEmpty()||choices.get(0)==null||choices.get(0).trim().isEmpty()){notice.accept(tr("Текст не распознан. Черновик сохранён.","No speech recognized. Draft retained."));return true;}
        if(!id.equals(owner.get())||!held.equals(input.getText().toString())){notice.accept(tr("Чат или черновик изменился. Результат диктовки не вставлен.","Chat or draft changed. Dictation was not inserted."));return true;}
        String text=choices.get(0).trim();
        String separator=input.length()==0||Character.isWhitespace(input.getText().charAt(input.length()-1))?"":" ";
        input.append(separator+text);input.setSelection(input.length());
        return true;
    }
    void clear(){pendingOwner=null;original=null;}
    private String tr(String ru,String en){return WorkspaceLanguage.code(activity).equals("ru")?ru:en;}
}
