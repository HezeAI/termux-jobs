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
    private Presentation presentation=(text,active,stop,cancel)->{};
    AgentDictation(Activity a,EditText input,Supplier<String> owner,Consumer<String> notice){activity=a;this.input=input;this.owner=owner;this.notice=notice;}
    void presentation(Presentation value){presentation=value;}
    void start(){
        if(pendingOwner!=null)return;
        String id=owner.get();if(id==null||id.isEmpty()){notice.accept(tr("Откройте агентский чат для диктовки.","Open an agent chat to dictate."));return;}
        try{
            String configured=Settings.Secure.getString(activity.getContentResolver(),"voice_recognition_service");
            ComponentName component=configured==null?null:ComponentName.unflattenFromString(configured);
            if(component==null){notice.accept(tr("В Android не настроена системная служба распознавания речи. Выберите службу голосового ввода в настройках Android. Диктовка клавиатуры не подтверждает её наличие.","Android has no default speech recognition service configured. Select a voice input service in Android settings. Keyboard dictation does not confirm its availability."));return;}
            android.content.pm.ServiceInfo info=activity.getPackageManager().getServiceInfo(component,0);
            if(!info.enabled||!info.exported||!info.applicationInfo.enabled){notice.accept(tr("Системная служба распознавания отключена или недоступна для приложений. Проверьте службу голосового ввода в настройках Android.","The default speech service is disabled or unavailable to apps. Check voice input settings in Android."));return;}
            if(!SpeechRecognizer.isRecognitionAvailable(activity)){notice.accept(tr("Android не обнаружил доступной SpeechRecognizer-службы. Включите или установите совместимую службу и выберите её в настройках Android.","Android found no SpeechRecognizer service. Enable or install a compatible service and select it in Android settings."));return;}
        }catch(PackageManager.NameNotFoundException e){notice.accept(tr("Настроенная системная служба распознавания не установлена. Выберите доступную службу в настройках Android.","The configured default speech service is not installed. Select an available service in Android settings."));return;}
        catch(RuntimeException e){notice.accept(tr("Не удалось проверить системную службу распознавания. Черновик сохранён.","Could not check the default speech service. Draft retained."));return;}
        pendingOwner=id;original=input.getText().toString();
        if(activity.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)activity.requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO},AUDIO_REQUEST);else listen();
    }
    boolean permissionResult(int code){if(code!=AUDIO_REQUEST)return false;if(pendingOwner==null)return true;if(activity.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)listen();else fail(tr("Доступ к микрофону не разрешён. Черновик сохранён.","Microphone permission denied. Draft retained."));return true;}
    private boolean current(){return pendingOwner!=null&&pendingOwner.equals(owner.get())&&original.equals(input.getText().toString());}
    void ownerChanged(){if(pendingOwner!=null&&!current())clear();}
    void background(){if(recognizer!=null)clear();}
    private void state(String value){presentation.update(value,true,this::stop,this::clear);}
    private void listen(){
        if(!current()){clear();return;}
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
            recognizer.startListening(new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1).putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true));
        }catch(RuntimeException e){fail(tr("Не удалось подключиться к системной службе распознавания. Проверьте её настройки и разрешение микрофона. Черновик сохранён.","Could not connect to the default speech service. Check its settings and microphone permission. Draft retained."));}
    }
    private static String first(Bundle b){ArrayList<String> words=b==null?null:b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(words==null||words.isEmpty()||words.get(0)==null||words.get(0).trim().isEmpty())return null;return words.get(0).trim();}
    void stop(){if(recognizer==null)return;try{state(tr("Распознавание…","Recognizing…"));recognizer.stopListening();}catch(RuntimeException e){fail(tr("Не удалось завершить распознавание. Черновик сохранён.","Could not finish recognition. Draft retained."));}}
    void clear(){epoch++;pendingOwner=null;original=null;SpeechRecognizer old=recognizer;recognizer=null;if(old!=null){try{old.cancel();}catch(RuntimeException ignored){}try{old.destroy();}catch(RuntimeException ignored){}}presentation.update("",false,this::stop,this::clear);}
    private void fail(String message){clear();notice.accept(message);}
    private String errorText(int code){String reason=code==SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS?tr("Нет разрешения микрофона","Microphone permission missing"):code==SpeechRecognizer.ERROR_NETWORK||code==SpeechRecognizer.ERROR_NETWORK_TIMEOUT?tr("Ошибка сети службы распознавания","Speech service network error"):code==SpeechRecognizer.ERROR_NO_MATCH||code==SpeechRecognizer.ERROR_SPEECH_TIMEOUT?tr("Речь не распознана","No speech recognized"):code==SpeechRecognizer.ERROR_RECOGNIZER_BUSY?tr("Системная служба распознавания занята","Default speech service is busy"):tr("Ошибка системной службы распознавания","Default speech service error");return reason+" ("+code+"). "+tr("Черновик сохранён.","Draft retained.");}
    boolean result(int code,int result,Intent data){return false;}
    private String tr(String ru,String en){return WorkspaceLanguage.code(activity).equals("ru")?ru:en;}
}
