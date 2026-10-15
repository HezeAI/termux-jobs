package com.termux.app.workspace;

import org.junit.Test;
import java.io.*;
import java.util.*;
import java.util.concurrent.CancellationException;
import static org.junit.Assert.*;

/** Controlled peer: window fill, out-of-order errors and cancellation independent of SSH availability. */
public class ServerSftpWindowTest {
    static final class Frame {final byte[] data;final boolean write;Frame(byte[] b,boolean w){data=b;write=w;}}
    static final class Peer extends InputStream {
        final Deque<Frame> replies=new ArrayDeque<>();Frame current;int position,writes,maxPending,pending,failAt=-1;
        long bytes;boolean reorder,requireWindow,firstWindow,invalidId,closed,published,removed;
        public int read()throws IOException{
            if(current==null||position==current.data.length){
                if(replies.isEmpty())throw new IOException("MISSING_REPLY");
                Frame next=replies.peekFirst();if(next.write){if(requireWindow&&!firstWindow){if(pending!=ServerSftp.WRITE_WINDOW)throw new IOException("WINDOW_NOT_FILLED");firstWindow=true;}if(reorder){next=replies.removeLast();}else next=replies.removeFirst();pending--;}else next=replies.removeFirst();current=next;position=0;
            }
            return current.data[position++]&255;
        }
        OutputStream output(){return new ByteArrayOutputStream(){public void flush()throws IOException{
            if(size()==0)return;DataInputStream in=new DataInputStream(new ByteArrayInputStream(toByteArray()));in.readInt();int type=in.readUnsignedByte();int id=type==1?0:in.readInt();ByteArrayOutputStream data=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(data);
            if(type==1){out.writeByte(2);out.writeInt(3);}
            else if(type==3){out.writeByte(102);out.writeInt(id);out.writeInt(1);out.writeByte(7);}
            else if(type==6){int n=in.readInt();in.skipBytes(n);long offset=in.readLong();int count=in.readInt();assertEquals(bytes,offset);byte[] content=new byte[count];in.readFully(content);bytes+=count;writes++;pending++;maxPending=Math.max(maxPending,pending);out.writeByte(101);out.writeInt(invalidId?-1:id);out.writeInt(writes==failAt?4:0);}
            else{if(type==18)published=true;if(type==13)removed=true;out.writeByte(101);out.writeInt(id);out.writeInt(type==7?2:0);}
            byte[] payload=data.toByteArray();ByteArrayOutputStream framed=new ByteArrayOutputStream();DataOutputStream packet=new DataOutputStream(framed);packet.writeInt(payload.length);packet.write(payload);replies.addLast(new Frame(framed.toByteArray(),type==6));reset();
        }};}
        ServerSftp client()throws Exception{return new ServerSftp(this,output(),()->closed=true);}
    }
    private ServerSftp.Info absent(){return new ServerSftp.Info(false,0,0,0);}
    @Test public void fillsBoundedWindowAndAcceptsOutOfOrderAcknowledgements()throws Exception{
        Peer peer=new Peer();peer.reorder=true;peer.requireWindow=true;byte[] input=new byte[32768*19+1003];long[] acknowledged={0};
        try(ServerSftp client=peer.client()){client.upload(new ByteArrayInputStream(input),"/fixture","file",absent(),bytes->{assertTrue(bytes>acknowledged[0]);assertTrue(bytes<=input.length);acknowledged[0]=bytes;});assertEquals(input.length,acknowledged[0]);assertEquals(ServerSftp.WRITE_WINDOW,peer.maxPending);assertEquals(0,peer.pending);assertTrue(peer.published);assertNull(client.pendingTemp());}
    }
    @Test public void failedWriteDrainsEveryReplyAndRemovesTemporaryWithoutPublishing()throws Exception{
        Peer peer=new Peer();peer.failAt=4;try(ServerSftp client=peer.client()){try{client.upload(new ByteArrayInputStream(new byte[32768*12]),"/fixture","keep",absent(),bytes->{});fail();}catch(IOException expected){assertEquals("SFTP_WRITE_FAILED",expected.getMessage());}assertEquals(0,peer.pending);assertFalse(peer.published);assertTrue(peer.removed);assertNull(client.pendingTemp());assertFalse(peer.closed);}
    }
    @Test public void malformedAcknowledgementClosesTransportAndRetainsCleanupPath()throws Exception{
        Peer peer=new Peer();peer.invalidId=true;try(ServerSftp client=peer.client()){try{client.upload(new ByteArrayInputStream(new byte[32768*9]),"/fixture","keep",absent(),bytes->{});fail();}catch(IOException expected){assertEquals("SFTP_ID",expected.getMessage());}assertTrue(peer.closed);assertFalse(peer.published);assertFalse(peer.removed);assertNotNull(client.pendingTemp());}
    }
    @Test public void sourceFailureWithOutstandingWritesDrainsBeforeCleanup()throws Exception{
        Peer peer=new Peer();InputStream input=new InputStream(){int blocks;public int read(){throw new UnsupportedOperationException();}public int read(byte[] b,int offset,int size)throws IOException{if(blocks++==3)throw new IOException("SOURCE_LOST");Arrays.fill(b,offset,offset+size,(byte)1);return size;}};
        try(ServerSftp client=peer.client()){try{client.upload(input,"/fixture","keep",absent(),bytes->{});fail();}catch(IOException expected){assertEquals("SOURCE_LOST",expected.getMessage());}assertEquals(3,peer.writes);assertEquals(0,peer.pending);assertTrue(peer.removed);assertFalse(peer.published);assertFalse(peer.closed);}
    }
    @Test public void cancellationWithPendingWritesDoesNotPublishOrReuseBrokenChannel()throws Exception{
        Peer peer=new Peer();try(ServerSftp client=peer.client()){try{client.upload(new ByteArrayInputStream(new byte[32768*12]),"/fixture","keep",absent(),bytes->{client.close();throw new CancellationException();});fail();}catch(CancellationException expected){}assertTrue(peer.closed);assertFalse(peer.published);assertNotNull(client.pendingTemp());}
    }
}
