package com.termux.terminal;

import org.junit.Test;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.Assert.*;

public class SynchronizedFrameBufferTest {
    private static final String BEGIN="\033[?2026h", END="\033[?2026l";
    private SynchronizedFrameBuffer buffer=new SynchronizedFrameBuffer();
    private ByteArrayOutputStream output=new ByteArrayOutputStream();
    private List<byte[]> writes=new ArrayList<>();
    private void accept(byte[] b){output.write(b,0,b.length);writes.add(b);}
    private void add(String s,long time){byte[] b=s.getBytes(StandardCharsets.UTF_8);buffer.append(b,b.length,time,this::accept);}
    private String text(){return new String(output.toByteArray(),StandardCharsets.UTF_8);}
    @Test public void splitMarkersAndUtf8NeverPublishPartialFrame(){
        byte[] bytes=(BEGIN+"Привет 😀"+END).getBytes(StandardCharsets.UTF_8);
        for(int i=0;i<bytes.length;i++){buffer.append(new byte[]{bytes[i]},1,i,this::accept);if(i<bytes.length-1)assertEquals(0,output.size());}
        assertEquals("Привет 😀",text());assertEquals(1,writes.size());assertEquals(-1,buffer.deadline());
    }
    @Test public void ordinaryAnsiAndNonmatchingPrefixesArePreserved(){
        String s="hello\033[31mred\033[0m\033[?2004h\033\033[?2025h";
        for(byte b:s.getBytes(StandardCharsets.UTF_8))buffer.append(new byte[]{b},1,10,this::accept);
        assertEquals(s,text());assertEquals(-1,buffer.deadline());
    }
    @Test public void surroundingTextAndSeveralFramesRemainOrdered(){
        add("before"+BEGIN+"one"+END+"middle"+BEGIN+"two"+END+"after",0);
        assertEquals("beforeonemiddletwoafter",text());assertEquals(-1,buffer.deadline());
    }
    @Test public void timeoutIsAnchoredAndRepeatedBeginDoesNotExtendIt(){
        add(BEGIN+"one",10);add(BEGIN+"two",150);assertEquals(210,buffer.deadline());
        buffer.expire(209,this::accept);assertEquals("",text());buffer.expire(210,this::accept);
        assertEquals("onetwo",text());add("tail"+END,220);assertEquals("onetwotail",text());assertEquals(-1,buffer.deadline());
    }
    @Test public void partialMarkerTimeoutAndClosePreserveBytes(){
        add("\033[?20",1);buffer.expire(201,this::accept);assertEquals("\033[?20",text());
        add("26h",202);assertEquals("\033[?2026h",text());add(BEGIN+"unfinished\033[?",203);buffer.flush(this::accept);
        assertEquals("\033[?2026hunfinished\033[?",text());assertEquals(-1,buffer.deadline());
    }
    @Test public void oversizedFrameFailsOpenAndFollowingFrameStillWorks(){
        String large=String.join("",Collections.nCopies(SynchronizedFrameBuffer.MAX_BYTES+500,"x"));
        add(BEGIN+large,0);assertEquals(large,text());assertEquals(-1,buffer.deadline());
        add(END+BEGIN+"next"+END,1);assertEquals(large+"next",text());
    }
    @Test public void sizeLimitCrossingInsideNonmatchingEscapeDoesNotDuplicateBytes(){
        String data=String.join("",Collections.nCopies(SynchronizedFrameBuffer.MAX_BYTES-2,"x"))+"\033[?2025hrest";
        add(BEGIN+data+END,0);assertEquals(data,text());assertEquals(-1,buffer.deadline());
    }
    @Test public void differentSessionsNeverShareHeldFrames(){
        add(BEGIN+"held",0);SynchronizedFrameBuffer other=new SynchronizedFrameBuffer();
        byte[] plain="other".getBytes(StandardCharsets.UTF_8);other.append(plain,plain.length,0,this::accept);
        assertEquals("other",text());add(END,1);assertEquals("otherheld",text());
    }
    @Test public void randomChunkSizesPreservePayloadAndOrder(){
        Random random=new Random(17);String payload="Пример\n\033[48;2;42;42;42mgray\033[0m";
        byte[] bytes=("first"+BEGIN+payload+END+"last").getBytes(StandardCharsets.UTF_8);
        for(int i=0;i<bytes.length;){int n=Math.min(bytes.length-i,1+random.nextInt(13));buffer.append(Arrays.copyOfRange(bytes,i,i+n),n,1,this::accept);i+=n;}
        assertEquals("first"+payload+"last",text());
    }
}
