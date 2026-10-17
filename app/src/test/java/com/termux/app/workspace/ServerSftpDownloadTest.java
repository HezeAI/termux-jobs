package com.termux.app.workspace;
import org.junit.*;import java.io.*;import java.util.*;import static org.junit.Assert.*;
public class ServerSftpDownloadTest {
    static final class Peer extends InputStream {
        final byte[] source=new byte[32768*10+91];final ArrayDeque<byte[]> frames=new ArrayDeque<>();final ArrayDeque<byte[]> readFrames=new ArrayDeque<>();byte[] frame;int at,maxPending;boolean closed,shortRead,targetExists,moved;long lastOffset;
        Peer(){for(int i=0;i<source.length;i++)source[i]=(byte)i;}
        public int read()throws IOException{if(frame==null||at==frame.length){if(frames.isEmpty()){while(!readFrames.isEmpty())frames.add(readFrames.removeLast());}frame=frames.poll();at=0;if(frame==null)throw new IOException("NO_REPLY");}return frame[at++]&255;}
        static void string(DataOutputStream out,String s)throws IOException{byte[] b=s.getBytes(java.nio.charset.StandardCharsets.UTF_8);out.writeInt(b.length);out.write(b);}
        static String string(DataInputStream in)throws IOException{byte[] b=new byte[in.readInt()];in.readFully(b);return new String(b,java.nio.charset.StandardCharsets.UTF_8);}
        OutputStream output(){return new ByteArrayOutputStream(){public void flush()throws IOException{
            if(size()==0)return;DataInputStream in=new DataInputStream(new ByteArrayInputStream(toByteArray()));in.readInt();int type=in.readUnsignedByte(),id=type==1?0:in.readInt();ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(b);
            if(type==1){out.writeByte(2);out.writeInt(3);}else if(type==7){String path=string(in);if(path.equals("/to/file")&&!targetExists){out.writeByte(101);out.writeInt(id);out.writeInt(2);}else{out.writeByte(105);out.writeInt(id);out.writeInt(1|4|8);out.writeLong(source.length);out.writeInt(0100600);out.writeInt(1);out.writeInt(2);}}
            else if(type==3){string(in);assertEquals(1,in.readInt());out.writeByte(102);out.writeInt(id);out.writeInt(1);out.writeByte(7);}
            else if(type==5){int n=in.readInt();in.skipBytes(n);long offset=in.readLong();int length=in.readInt();assertTrue(offset>=0&&offset+length<=source.length);out.writeByte(103);out.writeInt(id);int count=shortRead?length-1:length;out.writeInt(count);out.write(source,(int)offset,count);lastOffset=offset;}
            else{if(type==18)moved=true;out.writeByte(101);out.writeInt(id);out.writeInt(0);}
            ByteArrayOutputStream packed=new ByteArrayOutputStream();DataOutputStream f=new DataOutputStream(packed);f.writeInt(b.size());f.write(b.toByteArray());if(type==5){readFrames.add(packed.toByteArray());maxPending=Math.max(maxPending,readFrames.size());}else frames.add(packed.toByteArray());reset();
        }};}
        ServerSftp client()throws Exception{return new ServerSftp(this,output(),()->closed=true);}
        ServerSftp.Info info(){return new ServerSftp.Info(true,source.length,0100600,2);}
    }
    @Test public void downloadUsesBoundedWindowAndOrdersReplies()throws Exception{Peer p=new Peer();ByteArrayOutputStream output=new ByteArrayOutputStream();try(ServerSftp c=p.client()){long[] received={0};c.download("/file",output,p.info(),n->{assertTrue(n>received[0]);received[0]=n;});assertArrayEquals(p.source,output.toByteArray());assertEquals(p.source.length,received[0]);assertEquals(ServerSftp.WRITE_WINDOW,p.maxPending);}}
    @Test public void shortReadFailsWithoutPublishingAndClosesOwnedChannel()throws Exception{Peer p=new Peer();p.shortRead=true;try(ServerSftp c=p.client()){try{c.download("/file",new ByteArrayOutputStream(),p.info(),n->{});fail();}catch(IOException expected){assertEquals("SFTP_SHORT_READ",expected.getMessage());}assertTrue(p.closed);}}
    @Test public void cancellationClosesChannelWithPendingReads()throws Exception{Peer p=new Peer();try(ServerSftp c=p.client()){try{c.download("/file",new ByteArrayOutputStream(),p.info(),n->{throw new java.util.concurrent.CancellationException();});fail();}catch(java.util.concurrent.CancellationException expected){}assertTrue(p.closed);}}
    @Test public void moveRefusesCollisionAndOwnSubdirectory()throws Exception{Peer p=new Peer();p.targetExists=true;try(ServerSftp c=p.client()){try{c.move("/file","/to",p.info());fail();}catch(IOException expected){assertEquals("DESTINATION_EXISTS",expected.getMessage());}assertFalse(p.moved);try{c.move("/file","/file/nested",p.info());fail();}catch(IOException expected){assertEquals("MOVE_INTO_SELF",expected.getMessage());}}}
    @Test public void moveWithoutCollisionUsesNonOverwritingRename()throws Exception{Peer p=new Peer();try(ServerSftp c=p.client()){c.move("/file","/to",p.info());assertTrue(p.moved);}}
}
