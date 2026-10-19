package com.termux.app.workspace;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import static org.junit.Assert.*;
public class GeminiReplyReadTest {
 @Test public void readsSplitUtf8InBatchesAndStopsAtResponseNewline()throws Exception{
  String json="{\"text\":\""+new String(new char[7000]).replace("\0","я")+"\"}";byte[] bytes=(json+"\nignored").getBytes(StandardCharsets.UTF_8);int[] calls={0};InputStream in=new ByteArrayInputStream(bytes){@Override public synchronized int read(byte[] b,int off,int len){calls[0]++;return super.read(b,off,Math.min(223,len));}};assertEquals(json,GeminiLiveControl.readReply(in,30000));assertTrue(calls[0]<100);
 }
 @Test public void rejectsOversizedReplies()throws Exception{try{GeminiLiveControl.readReply(new ByteArrayInputStream("abcdef\n".getBytes(StandardCharsets.UTF_8)),5);fail();}catch(GeminiLiveControl.Failure expected){assertEquals("RESPONSE_TOO_LARGE",expected.code);}}
}
