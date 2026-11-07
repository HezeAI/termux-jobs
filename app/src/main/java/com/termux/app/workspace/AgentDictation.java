package com.termux.app.workspace;

import android.app.Activity;
import android.content.Intent;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.speech.*;
import android.os.Bundle;
import android.widget.EditText;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** One in-app capture through Android's configured default service. Never sends a chat request. */
final class AgentDictation {
    static final int REQUEST=7542, AUDIO_REQUEST=7543;
    interface Presentation {void update(String text,boolean active,Runnable stop,Runnable cancel);}
    private final Activity activity;private final EditText input;private final Supplier<String> owner;private final Consumer<String> notice;
    private String pendingOwner,original;private int epoch;
    private SpeechRecognizer recognizer;
    private String serviceState="";
    private Presentation presentation=(text,active,stop,cancel)->{};
    AgentDictation(Activity a,EditText input,Supplier<String> owner,Consumer<String> notice){activity=a;this.input=input;this.owner=owner;this.notice=notice;}
    void presentation(Presentation value){presentation=value;}
    void start(){
        if(pendingOwner!=null)return;
        String id=owner.get();if(id==null||id.isEmpty()){notice.accept(tr("Откройте агентский чат для диктовки.","Open an agent chat to dictate."));return;}
        pendingOwner=id;original=input.getText().toString();
        if(activity.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
            try{activity.requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO},AUDIO_REQUEST);}
            catch(RuntimeException e){fail(tr("Не удалось запросить микрофон: ","Could not request microphone: ")+e.getClass().getSimpleName()+". "+tr("Черновик сохранён.","Draft retained."));}
        }else listen();
    }
    boolean permissionResult(int code){if(code!=AUDIO_REQUEST)return false;if(pendingOwner==null)return true;if(activity.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)listen();else fail(tr("Разрешение RECORD_AUDIO отклонено (PERMISSION_DENIED=-1). Настройки Android → Приложения → Termux Job’s → Разрешения → Микрофон. Черновик сохранён.","RECORD_AUDIO denied (PERMISSION_DENIED=-1). Android Settings → Apps → Termux Job’s → Permissions → Microphone. Draft retained."));return true;}
    private boolean current(){return pendingOwner!=null&&pendingOwner.equals(owner.get())&&original.equals(input.getText().toString());}
    void ownerChanged(){if(pendingOwner!=null&&!current())clear();}
    void background(){if(recognizer!=null)clear();}
    private void state(String value){presentation.update(value,true,this::stop,this::clear);}
    private void listen(){
        if(!current()){clear();return;}
        serviceState=serviceState();
        String stage="createSpeechRecognizer";
        try{
            final int ticket=epoch;
            recognizer=SpeechRecognizer.createSpeechRecognizer(activity);
            recognizer.setRecognitionListener(new RecognitionListener(){
                public void onReadyForSpeech(Bundle b){if(ticket==epoch)state(tr("Слушаю…","Listening…"));}
                public void onBeginningOfSpeech(){} public void onRmsChanged(float v){} public void onBufferReceived(byte[] b){} public void onEvent(int t,Bundle b){}
                public void onEndOfSpeech(){if(ticket==epoch)state(tr("Распознавание…","Recognizing…"));}
                public void onPartialResults(Bundle b){if(ticket!=epoch)return;if(!current()){clear();return;}String text=first(b);if(text!=null)state(tr("Слушаю: ","Listening: ")+text);}
                public void onResults(Bundle b){if(ticket!=epoch)return;String text=first(b);if(!current()){clear();return;}if(text==null){fail(tr("Текст не распознан. Черновик сохранён.","No speech recognized. Draft retained."));return;}String held=original;clear();input.append((held.isEmpty()||Character.isWhitespace(held.charAt(held.length()-1))?"":" ")+text);input.setSelection(input.length());}
                public void onError(int error){if(ticket!=epoch)return;fail(errorText(error));}
            });
            state(tr("Подключение микрофона…","Connecting microphone…"));
            stage="startListening";
            recognizer.startListening(new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1).putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true));
        }catch(RuntimeException e){fail(stage+": "+e.getClass().getSimpleName()+". "+tr("Черновик сохранён.","Draft retained.")+"\n\n"+serviceState);}
    }
    private static String first(Bundle b){ArrayList<String> words=b==null?null:b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(words==null||words.isEmpty()||words.get(0)==null||words.get(0).trim().isEmpty())return null;return words.get(0).trim();}
    void stop(){if(recognizer==null)return;try{state(tr("Распознавание…","Recognizing…"));recognizer.stopListening();}catch(RuntimeException e){fail(tr("Не удалось завершить распознавание. Черновик сохранён.","Could not finish recognition. Draft retained."));}}
    void clear(){epoch++;pendingOwner=null;original=null;SpeechRecognizer old=recognizer;recognizer=null;if(old!=null){try{old.cancel();}catch(RuntimeException ignored){}try{old.destroy();}catch(RuntimeException ignored){}}presentation.update("",false,this::stop,this::clear);}
    private void fail(String message){clear();notice.accept(message);}
    private String errorText(int code){
        String name;
        switch(code){
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:name="ERROR_NETWORK_TIMEOUT";break;
            case SpeechRecognizer.ERROR_NETWORK:name="ERROR_NETWORK";break;
            case SpeechRecognizer.ERROR_AUDIO:name="ERROR_AUDIO";break;
            case SpeechRecognizer.ERROR_SERVER:name="ERROR_SERVER";break;
            case SpeechRecognizer.ERROR_CLIENT:name="ERROR_CLIENT";break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:name="ERROR_SPEECH_TIMEOUT";break;
            case SpeechRecognizer.ERROR_NO_MATCH:name="ERROR_NO_MATCH";break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:name="ERROR_RECOGNIZER_BUSY";break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:name="ERROR_INSUFFICIENT_PERMISSIONS";break;
            case SpeechRecognizer.ERROR_TOO_MANY_REQUESTS:name="ERROR_TOO_MANY_REQUESTS";break;
            case SpeechRecognizer.ERROR_SERVER_DISCONNECTED:name="ERROR_SERVER_DISCONNECTED";break;
            case SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED:name="ERROR_LANGUAGE_NOT_SUPPORTED";break;
            case SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE:name="ERROR_LANGUAGE_UNAVAILABLE";break;
            default:name="UNKNOWN_ERROR";
        }
        return "SpeechRecognizer.onError: "+code+" ("+name+"). "+tr("Черновик сохранён.","Draft retained.")+"\n\n"+serviceState;
    }
    /** Facts from the registered Activity; observation never blocks the platform start attempt. */
    private String serviceState(){
        StringBuilder state=new StringBuilder();
        try{String selected=Settings.Secure.getString(activity.getContentResolver(),"voice_recognition_service");
            state.append("voice_recognition_service: ").append(selected==null?"<null>":selected.isEmpty()?"<empty>":selected);
            if(selected!=null&&!selected.isEmpty()){
                ComponentName component=ComponentName.unflattenFromString(selected);
                if(component==null)state.append(" (invalid ComponentName)");
                else try{android.content.pm.ServiceInfo info=activity.getPackageManager().getServiceInfo(component,PackageManager.MATCH_DISABLED_COMPONENTS);state.append("; enabled=").append(info.enabled&&info.applicationInfo.enabled).append("; exported=").append(info.exported);}
                catch(PackageManager.NameNotFoundException e){state.append(" (not installed or not visible)");}
            }
        }catch(RuntimeException e){state.append("voice_recognition_service: ").append(e.getClass().getSimpleName());}
        try{state.append("\nisRecognitionAvailable: ").append(SpeechRecognizer.isRecognitionAvailable(activity));}catch(RuntimeException e){state.append("\nisRecognitionAvailable: ").append(e.getClass().getSimpleName());}
        state.append("\nRECORD_AUDIO: ").append(activity.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED?"GRANTED":"DENIED");
        try{java.util.List<android.content.pm.ResolveInfo> services=activity.getPackageManager().queryIntentServices(new Intent("android.speech.RecognitionService"),0);
            state.append("\nRecognitionService: ");if(services.isEmpty())state.append(tr("не найдены","none found"));
            for(int i=0;i<services.size();i++){android.content.pm.ServiceInfo info=services.get(i).serviceInfo;if(i>0)state.append(", ");state.append(new ComponentName(info.packageName,info.name).flattenToShortString());}
        }catch(RuntimeException e){state.append("\nRecognitionService: ").append(e.getClass().getSimpleName());}
        return state.toString();
    }
    boolean result(int code,int result,Intent data){return false;}
    private String tr(String ru,String en){return WorkspaceLanguage.code(activity).equals("ru")?ru:en;}
}
