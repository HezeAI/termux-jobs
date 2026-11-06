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
    private int epoch;
    private android.speech.SpeechRecognizer recognizer;
    private android.app.AlertDialog listening;
    private android.content.ComponentName service;
    private static final int AUDIO_REQUEST=7543;
    AgentDictation(Activity activity,EditText input,Supplier<String> owner,Consumer<String> notice){this.activity=activity;this.input=input;this.owner=owner;this.notice=notice;}
    void start(){
        if(pendingOwner!=null)return;String id=owner.get();if(id==null||id.isEmpty()){notice.accept(tr("Откройте агентский чат для диктовки. Черновик сохранён.","Open an agent chat to dictate. Draft retained."));return;}
        pendingOwner=id;original=input.getText().toString();
        Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1).putExtra(RecognizerIntent.EXTRA_PROMPT,tr("Диктовка в черновик","Dictate into draft"));
        try{activity.startActivityForResult(intent,REQUEST);}catch(ActivityNotFoundException failure){chooseService();}
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
    void clear(){epoch++;pendingOwner=null;original=null;if(recognizer!=null){recognizer.destroy();recognizer=null;}if(listening!=null){listening.dismiss();listening=null;}}
    private void chooseService(){
        final int choiceEpoch=epoch;
        java.util.List<android.content.pm.ResolveInfo> found=activity.getPackageManager().queryIntentServices(new Intent(android.speech.RecognitionService.SERVICE_INTERFACE),0);
        java.util.ArrayList<android.content.pm.ServiceInfo> usable=new java.util.ArrayList<>();
        for(android.content.pm.ResolveInfo row:found){android.content.pm.ServiceInfo info=row.serviceInfo;if(info!=null&&info.enabled&&info.exported&&"android.permission.BIND_RECOGNITION_SERVICE".equals(info.permission))usable.add(info);}
        if(usable.isEmpty()){clear();notice.accept(tr("Android не предоставляет доступное окно или службу распознавания. Включите службу голосового ввода в системных настройках либо установите совместимый провайдер Android SpeechRecognizer. Можно использовать диктовку клавиатуры. Черновик сохранён.","Android provides no accessible speech activity or service. Enable a voice input service in system settings or install an Android SpeechRecognizer provider. Keyboard dictation is also available. Draft retained."));return;}
        String[] labels=new String[usable.size()];for(int i=0;i<labels.length;i++)labels[i]=usable.get(i).loadLabel(activity.getPackageManager()).toString()+" · "+usable.get(i).packageName;
        WorkspaceDialogs.builder(activity).setTitle(tr("Служба распознавания речи","Speech recognition service")).setItems(labels,(d,index)->{if(pendingOwner==null||choiceEpoch!=epoch)return;android.content.pm.ServiceInfo info=usable.get(index);service=new android.content.ComponentName(info.packageName,info.name);if(activity.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)!=android.content.pm.PackageManager.PERMISSION_GRANTED)activity.requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO},AUDIO_REQUEST);else startService();}).setNegativeButton(tr("Отмена","Cancel"),(d,w)->clear()).setOnCancelListener(d->clear()).show();
    }
    boolean permissionResult(int code){if(code!=AUDIO_REQUEST)return false;if(pendingOwner==null)return true;if(activity.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)==android.content.pm.PackageManager.PERMISSION_GRANTED)startService();else{clear();notice.accept(tr("Доступ к микрофону не разрешён. Черновик сохранён.","Microphone permission was denied. Draft retained."));}return true;}
    private void startService(){
        if(pendingOwner==null||!pendingOwner.equals(owner.get())||!original.equals(input.getText().toString())){clear();return;}
        try{
            recognizer=android.speech.SpeechRecognizer.createSpeechRecognizer(activity,service);
            final int ticket=epoch;recognizer.setRecognitionListener(new android.speech.RecognitionListener(){
                public void onReadyForSpeech(android.os.Bundle b){} public void onBeginningOfSpeech(){} public void onRmsChanged(float v){} public void onBufferReceived(byte[] b){} public void onEndOfSpeech(){} public void onPartialResults(android.os.Bundle b){} public void onEvent(int t,android.os.Bundle b){}
                public void onError(int error){if(pendingOwner==null||ticket!=epoch)return;clear();notice.accept(tr("Распознавание не завершено. Код Android: ","Speech recognition failed. Android code: ")+error+tr(". Проверьте службу, язык, сеть и разрешение микрофона. Черновик сохранён.",". Check the provider, language, network and microphone permission. Draft retained."));}
                public void onResults(android.os.Bundle b){if(ticket!=epoch)return;Intent data=new Intent();data.putStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS,b.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION));result(REQUEST,Activity.RESULT_OK,data);}
            });
            listening=WorkspaceDialogs.builder(activity).setTitle(tr("Голосовой ввод","Voice input")).setMessage(tr("Слушаю…","Listening…")).setNegativeButton(tr("Отмена","Cancel"),(d,w)->clear()).setOnCancelListener(d->clear()).create();listening.show();
            recognizer.startListening(new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1));
        }catch(RuntimeException error){clear();notice.accept(tr("Не удалось подключиться к выбранной службе распознавания. Черновик сохранён.","Could not connect to the selected speech service. Draft retained."));}
    }
    private String tr(String ru,String en){return WorkspaceLanguage.code(activity).equals("ru")?ru:en;}
}
