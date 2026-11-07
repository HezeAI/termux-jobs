package com.termux.app.workspace;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** App-owned, offline microphone capture. No platform recognition provider or network. */
final class OfflineDictation implements DictationBackend {
    interface Session extends AutoCloseable {
        void start()throws Exception;
        int read(short[] samples)throws Exception;
        boolean accept(short[] samples,int count);
        String result();
        String partial();
        String finish();
        void close();
    }
    interface SessionFactory { Session open(BooleanSupplier cancelled)throws Exception; }
    private static final Object AUDIO_LOCK=new Object();
    private final SessionFactory factory;
    private final Executor worker;
    private final Consumer<Runnable> ui;
    private final AtomicBoolean started=new AtomicBoolean(),cancelled=new AtomicBoolean(),stopped=new AtomicBoolean(),terminal=new AtomicBoolean();
    OfflineDictation(Context context){
        this(cancelled->new NativeSession(context,DictationModel.prepare(context,cancelled)),r->{Thread thread=new Thread(r,"offline-dictation");thread.setDaemon(true);thread.start();},r->new Handler(Looper.getMainLooper()).post(r));
    }
    OfflineDictation(SessionFactory factory,Executor worker,Consumer<Runnable> ui){this.factory=factory;this.worker=worker;this.ui=ui;}
    public void start(Listener listener){if(started.compareAndSet(false,true))worker.execute(()->run(listener));}
    public void stop(){stopped.set(true);}
    public void cancel(){cancelled.set(true);}
    private void run(Listener listener){
        synchronized(AUDIO_LOCK){
            if(cancelled.get())return;
            String stage="model";
            try(Session session=factory.open(cancelled::get)){
                if(cancelled.get())return;
                if(stopped.get()){complete(listener,"");return;}
                stage="microphone";session.start();
                ui.accept(()->{if(!cancelled.get()&&!stopped.get())listener.ready();});
                short[] samples=new short[2048];String lastPartial="";
                while(!cancelled.get()&&!stopped.get()){
                    int count=session.read(samples);
                    if(count<0)throw new IOException("AudioRecord.read="+count);
                    if(count==0){Thread.sleep(5);continue;}
                    if(cancelled.get())return;
                    stage="recognition";
                    if(session.accept(samples,count)){
                        String text=text(session.result(),"text");
                        if(!text.isEmpty()){complete(listener,text);return;}
                    }else{
                        String partial=text(session.partial(),"partial");
                        if(!partial.equals(lastPartial)){
                            lastPartial=partial;final String preview=partial;
                            ui.accept(()->{if(!cancelled.get()&&!stopped.get())listener.partial(preview);});
                        }
                    }
                    stage="microphone";
                }
                if(!cancelled.get())complete(listener,text(session.finish(),"text"));
            }catch(Exception|LinkageError e){
                if(!cancelled.get()&&terminal.compareAndSet(false,true)){
                    String message=stage+": "+e.getClass().getSimpleName()+(e.getMessage()==null?"":": "+e.getMessage());
                    ui.accept(()->{if(!cancelled.get())listener.error(message);});
                }
            }
        }
    }
    private void complete(Listener listener,String text){if(terminal.compareAndSet(false,true))ui.accept(()->{if(!cancelled.get())listener.result(text);});}
    private static String text(String json,String key)throws Exception{return new JSONObject(json).optString(key,"").trim();}
    private static final class NativeSession implements Session {
        private Model model;
        private Recognizer recognizer;
        private AudioRecord audio;
        NativeSession(Context context,File directory)throws Exception{
            try{
                model=new Model(directory.getAbsolutePath());recognizer=new Recognizer(model,16000f);
                if(context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)!=android.content.pm.PackageManager.PERMISSION_GRANTED)throw new SecurityException("RECORD_AUDIO denied");
                int minimum=AudioRecord.getMinBufferSize(16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
                if(minimum<=0)throw new IOException("AudioRecord.getMinBufferSize="+minimum);
                audio=new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,Math.max(4096,minimum));
                if(audio.getState()!=AudioRecord.STATE_INITIALIZED)throw new IOException("AudioRecord not initialized");
            }catch(Exception|LinkageError e){close();throw e;}
        }
        public void start()throws IOException{audio.startRecording();if(audio.getRecordingState()!=AudioRecord.RECORDSTATE_RECORDING)throw new IOException("AudioRecord not recording");}
        public int read(short[] samples){return audio.read(samples,0,samples.length);}
        public boolean accept(short[] samples,int count){return recognizer.acceptWaveForm(samples,count);}
        public String result(){return recognizer.getResult();}
        public String partial(){return recognizer.getPartialResult();}
        public String finish(){return recognizer.getFinalResult();}
        public void close(){
            if(audio!=null){try{if(audio.getRecordingState()==AudioRecord.RECORDSTATE_RECORDING)audio.stop();}catch(RuntimeException ignored){}finally{try{audio.release();}catch(RuntimeException ignored){}audio=null;}}
            try{if(recognizer!=null)recognizer.close();}finally{recognizer=null;if(model!=null){model.close();model=null;}}
        }
    }
}
