package com.termux.app.workspace;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class UpdateConnectionTest {
    private static final String URL="https://github.com/release";
    static class Connection extends HttpURLConnection {
        boolean disconnected; int code=200; String redirect; IOException failure;
        Connection()throws Exception{super(new URL(URL));}
        public void disconnect(){disconnected=true;}
        public boolean usingProxy(){return false;}
        public void connect(){}
        public int getResponseCode()throws IOException{if(failure!=null)throw failure;return code;}
        public String getHeaderField(String key){return redirect;}
    }
    @Test public void failuresCloseOwnedConnection()throws Exception{
        Connection c=new Connection();c.failure=new IOException("synthetic");UpdateConnection request=new UpdateConnection(url->c);
        try{request.connect(URL);fail();}catch(IOException expected){}assertTrue(c.disconnected);
    }
    @Test public void cancellationBeforeOpenDoesNotConnect()throws Exception{
        UpdateConnection request=new UpdateConnection(url->{throw new AssertionError("opened after cancel");});request.cancel();
        try{request.connect(URL);fail();}catch(InterruptedIOException expected){}
    }
    @Test public void cancellationDuringFactoryClosesWithoutRequest()throws Exception{
        Connection c=new Connection();UpdateConnection[] request={null};
        request[0]=new UpdateConnection(url->{request[0].cancel();return c;});
        try{request[0].connect(URL);fail();}catch(InterruptedIOException expected){}assertTrue(c.disconnected);
    }
    @Test public void cancellationDisconnectsBlockedResponseAndPreventsSuccess()throws Exception{
        CountDownLatch entered=new CountDownLatch(1),closed=new CountDownLatch(1);
        Connection c=new Connection(){
            public int getResponseCode()throws IOException{entered.countDown();try{if(!closed.await(5,TimeUnit.SECONDS))throw new IOException("fixture timeout");}catch(InterruptedException e){throw new IOException(e);}return 200;}
            public void disconnect(){super.disconnect();closed.countDown();}
        };
        UpdateConnection request=new UpdateConnection(url->c);ExecutorService executor=Executors.newSingleThreadExecutor();
        try{Future<?> worker=executor.submit(()->{try{request.connect(URL);fail("cancelled response returned");}catch(InterruptedIOException expected){}catch(IOException e){throw new AssertionError(e);}});
            assertTrue(entered.await(5,TimeUnit.SECONDS));request.cancel();worker.get(5,TimeUnit.SECONDS);assertTrue(c.disconnected);
        }finally{closed.countDown();executor.shutdownNow();}
    }
    @Test public void everyRedirectIsCheckedBeforeOpening()throws Exception{
        for(String destination:new String[]{"http://github.com/a","https://evil.example/a","https://github.com.evil/a","https://user@github.com/a","https://github.com:443/a"}){
            Connection c=new Connection();c.code=302;c.redirect=destination;int[] opens={0};
            UpdateConnection request=new UpdateConnection(url->{opens[0]++;return c;});
            try{request.connect(URL);fail(destination);}catch(IOException expected){}assertEquals(1,opens[0]);assertTrue(c.disconnected);
        }
    }
    @Test public void trustedRedirectClosesOldAndRetainsNewUntilRelease()throws Exception{
        Connection first=new Connection(),last=new Connection();first.code=302;first.redirect="https://release-assets.githubusercontent.com/asset";
        Deque<Connection> queue=new ArrayDeque<>(Arrays.asList(first,last));UpdateConnection request=new UpdateConnection(url->queue.removeFirst());
        assertSame(last,request.connect(URL));assertTrue(first.disconnected);assertFalse(last.disconnected);
        assertFalse(last.getInstanceFollowRedirects());assertEquals(15000,last.getConnectTimeout());assertEquals(20000,last.getReadTimeout());
        request.release(last);assertTrue(last.disconnected);
    }
    @Test public void redirectLoopIsBoundedAndAllConnectionsClose()throws Exception{
        List<Connection> opened=new ArrayList<>();
        UpdateConnection request=new UpdateConnection(url->{try{Connection c=new Connection();c.code=302;c.redirect="/again";opened.add(c);return c;}catch(Exception e){throw new IOException(e);}});
        try{request.connect(URL);fail();}catch(IOException expected){}assertEquals(6,opened.size());for(Connection c:opened)assertTrue(c.disconnected);
    }
    @Test public void cancellingOneOperationLeavesOtherConnectionUntouched()throws Exception{
        Connection a=new Connection(),b=new Connection();UpdateConnection first=new UpdateConnection(url->a),second=new UpdateConnection(url->b);
        first.connect(URL);second.connect(URL);first.cancel();assertTrue(a.disconnected);assertFalse(b.disconnected);assertFalse(second.isCancelled());second.release(b);
    }
}
