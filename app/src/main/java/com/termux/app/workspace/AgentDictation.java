package com.termux.app.workspace;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.widget.EditText;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Shared field dictation for every agent; no agent requests or automatic send. */
final class AgentDictation {
    static final int REQUEST=7542, AUDIO_REQUEST=7543;
    interface Presentation {void update(String text,boolean active,Runnable stop,Runnable cancel);}
    private final Activity activity;
    private final EditText input;
    private final Supplier<String> owner;
    private final Consumer<String> notice;
    private final DictationBackend.Factory factory;
    private String pendingOwner,original;
    private int epoch;
    private DictationBackend capture;
    private Presentation presentation=(text,active,stop,cancel)->{};
    AgentDictation(Activity a,EditText input,Supplier<String> owner,Consumer<String> notice){
        this(a,input,owner,notice,()->new OfflineDictation(a.getApplicationContext()));
    }
    AgentDictation(Activity a,EditText input,Supplier<String> owner,Consumer<String> notice,DictationBackend.Factory factory){
        activity=a;this.input=input;this.owner=owner;this.notice=notice;this.factory=factory;
    }
    void presentation(Presentation value){presentation=value;}
    void start(){
        if(pendingOwner!=null)return;
        String id=owner.get();
        if(id==null||id.isEmpty()){notice.accept(tr("Откройте агентский чат для диктовки.","Open an agent chat to dictate."));return;}
        pendingOwner=id;original=input.getText().toString();
        if(activity.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
            try{activity.requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO},AUDIO_REQUEST);}
            catch(RuntimeException e){fail(tr("Не удалось запросить микрофон: ","Could not request microphone: ")+e.getClass().getSimpleName());}
        }else listen();
    }
    boolean permissionResult(int code){
        if(code!=AUDIO_REQUEST)return false;
        if(pendingOwner==null)return true;
        if(activity.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)listen();
        else fail(tr("Разрешение RECORD_AUDIO отклонено (PERMISSION_DENIED=-1). Настройки Android → Приложения → Termux Job’s → Разрешения → Микрофон.","RECORD_AUDIO denied (PERMISSION_DENIED=-1). Android Settings → Apps → Termux Job’s → Permissions → Microphone."));
        return true;
    }
    private boolean current(){return pendingOwner!=null&&pendingOwner.equals(owner.get())&&original.equals(input.getText().toString());}
    private boolean active(int ticket){if(ticket!=epoch)return false;if(!current()){clear();return false;}return true;}
    void ownerChanged(){if(pendingOwner!=null&&!current())clear();}
    void background(){if(capture!=null)clear();}
    private void state(String text){presentation.update(text,true,this::stop,this::clear);}
    private void listen(){
        if(!current()){clear();return;}
        if(capture!=null)return; // A duplicate permission callback must not start a second microphone.
        final int ticket=epoch;
        try{
            capture=factory.create();
            state(tr("Подготовка распознавания…","Preparing dictation…"));
            capture.start(new DictationBackend.Listener(){
                public void ready(){if(active(ticket))state(tr("Слушаю…","Listening…"));}
                public void partial(String text){if(active(ticket))state(text.isEmpty()?tr("Слушаю…","Listening…"):tr("Слушаю: ","Listening: ")+text);}
                public void result(String text){
                    if(!active(ticket))return;
                    text=text==null?"":text.trim();
                    if(text.isEmpty()){fail(tr("Речь не распознана.","No speech recognized."));return;}
                    String held=original;clear();
                    input.append((held.isEmpty()||Character.isWhitespace(held.charAt(held.length()-1))?"":" ")+text);
                    input.setSelection(input.length());
                }
                public void error(String detail){if(active(ticket))fail(tr("Локальная диктовка: ","Offline dictation: ")+detail);}
            });
        }catch(RuntimeException|LinkageError e){if(ticket==epoch)fail(tr("Не удалось запустить диктовку: ","Could not start dictation: ")+e.getClass().getSimpleName());}
    }
    void stop(){
        if(capture==null)return;
        try{state(tr("Распознавание…","Recognizing…"));capture.stop();}
        catch(RuntimeException e){fail(tr("Не удалось завершить диктовку: ","Could not finish dictation: ")+e.getClass().getSimpleName());}
    }
    void clear(){
        epoch++;pendingOwner=null;original=null;DictationBackend old=capture;capture=null;
        if(old!=null)old.cancel();
        presentation.update("",false,this::stop,this::clear);
    }
    private void fail(String message){clear();notice.accept(message+" "+tr("Черновик сохранён.","Draft retained."));}
    boolean result(int code,int result,Intent data){return false;}
    private String tr(String ru,String en){return WorkspaceLanguage.code(activity).equals("ru")?ru:en;}
}
