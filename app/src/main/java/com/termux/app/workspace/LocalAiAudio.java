package com.termux.app.workspace;

import android.content.Context;
import android.media.*;
import android.os.SystemClock;
import java.io.*;
import java.nio.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Decode at most five seconds to bounded mono16k PCM WAV, only in owned media cache. */
final class LocalAiAudio {
    static final int RATE=16000,SECONDS=5,MAX_SAMPLES=RATE*SECONDS;
    static byte[] wav(byte[] pcm)throws IOException{
        ByteArrayOutputStream out=new ByteArrayOutputStream();out.write("RIFF".getBytes(java.nio.charset.StandardCharsets.US_ASCII));le(out,36+pcm.length,4);out.write("WAVEfmt ".getBytes(java.nio.charset.StandardCharsets.US_ASCII));le(out,16,4);le(out,1,2);le(out,1,2);le(out,RATE,4);le(out,RATE*2,4);le(out,2,2);le(out,16,2);out.write("data".getBytes(java.nio.charset.StandardCharsets.US_ASCII));le(out,pcm.length,4);out.write(pcm);return out.toByteArray();
    }
    private static void le(OutputStream o,int n,int bytes)throws IOException{for(int i=0;i<bytes;i++)o.write((n>>>(i*8))&255);}
    static File prepare(Context c,File source,AtomicBoolean cancel)throws Exception{
        MediaExtractor extractor=new MediaExtractor();MediaCodec codec=null;File target=null;
        try{
            extractor.setDataSource(source.getPath());MediaFormat selected=null;
            for(int i=0;i<extractor.getTrackCount();i++){MediaFormat f=extractor.getTrackFormat(i);if(f.getString(MediaFormat.KEY_MIME).startsWith("audio/")){extractor.selectTrack(i);selected=f;break;}}
            if(selected==null)throw new IOException("UNSUPPORTED_AUDIO: no Android-decodable audio track");
            codec=MediaCodec.createDecoderByType(selected.getString(MediaFormat.KEY_MIME));codec.configure(selected,null,null,0);codec.start();
            int rate=selected.getInteger(MediaFormat.KEY_SAMPLE_RATE),channels=selected.getInteger(MediaFormat.KEY_CHANNEL_COUNT),encoding=AudioFormat.ENCODING_PCM_16BIT;
            if(rate<8000||rate>192000||channels<1||channels>8)throw new IOException("UNSUPPORTED_AUDIO_FORMAT");
            ByteArrayOutputStream pcm=new ByteArrayOutputStream();MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();boolean inputEnd=false,done=false;long frame=0,next=0,deadline=SystemClock.elapsedRealtime()+60000;
            while(!done&&pcm.size()<MAX_SAMPLES*2){
                LocalAiFiles.check(cancel);if(SystemClock.elapsedRealtime()>deadline)throw new IOException("AUDIO_DECODE_TIMEOUT");
                if(!inputEnd){int id=codec.dequeueInputBuffer(10000);if(id>=0){ByteBuffer in=codec.getInputBuffer(id);if(in==null)throw new IOException("Missing audio decoder input");in.clear();int n=extractor.readSampleData(in,0);if(n<0){codec.queueInputBuffer(id,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);inputEnd=true;}else{codec.queueInputBuffer(id,0,n,extractor.getSampleTime(),0);extractor.advance();}}}
                int id=codec.dequeueOutputBuffer(info,10000);
                if(id==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){MediaFormat f=codec.getOutputFormat();rate=f.getInteger(MediaFormat.KEY_SAMPLE_RATE);channels=f.getInteger(MediaFormat.KEY_CHANNEL_COUNT);encoding=f.containsKey(MediaFormat.KEY_PCM_ENCODING)?f.getInteger(MediaFormat.KEY_PCM_ENCODING):AudioFormat.ENCODING_PCM_16BIT;if(rate<8000||rate>192000||channels<1||channels>8||encoding!=AudioFormat.ENCODING_PCM_16BIT&&encoding!=AudioFormat.ENCODING_PCM_FLOAT)throw new IOException("UNSUPPORTED_AUDIO_PCM");}
                else if(id>=0){try{
                    ByteBuffer data=codec.getOutputBuffer(id);if(data!=null&&info.size>0){data.order(ByteOrder.LITTLE_ENDIAN);data.position(info.offset);data.limit(info.offset+info.size);int bytes=encoding==AudioFormat.ENCODING_PCM_FLOAT?4:2;
                        while(data.remaining()>=bytes*channels&&pcm.size()<MAX_SAMPLES*2){double sample=0;for(int ch=0;ch<channels;ch++)sample+=encoding==AudioFormat.ENCODING_PCM_FLOAT?data.getFloat()*32767:data.getShort();sample/=channels;
                            while(next*(long)rate<=frame*(long)RATE&&pcm.size()<MAX_SAMPLES*2){int v=Double.isNaN(sample)?0:(int)Math.max(-32768,Math.min(32767,sample));pcm.write(v&255);pcm.write((v>>>8)&255);next++;}frame++;
                        }
                    }
                    done=(info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0;
                }finally{codec.releaseOutputBuffer(id,false);}}
            }
            LocalAiFiles.check(cancel);if(pcm.size()==0)throw new IOException("EMPTY_AUDIO");target=File.createTempFile("audio-",".wav",LocalAiMedia.root(c));try(OutputStream stream=new FileOutputStream(target)){stream.write(wav(pcm.toByteArray()));}LocalAiFiles.check(cancel);File result=target;target=null;return result;
        }finally{if(codec!=null){try{codec.stop();}catch(Exception ignored){}codec.release();}extractor.release();if(target!=null)target.delete();}
    }
}
