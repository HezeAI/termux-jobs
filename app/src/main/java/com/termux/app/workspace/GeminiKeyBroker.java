package com.termux.app.workspace;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.Credentials;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** One run, one same-UID descendant, one key. No plaintext file or profile-wide cleanup. */
final class GeminiKeyBroker implements AutoCloseable {
    static String address(String run) {
        if (!GeminiCli.validThread(run)) throw new IllegalArgumentException("Invalid Gemini run");
        return "termux-jobs-gemini-key-" + run;
    }
    interface Processes { Identity read(int pid); }
    interface Keys { char[] read() throws java.security.GeneralSecurityException; }
    static final class Identity {
        final int pid, parent; final String start;
        Identity(int pid,int parent,String start){this.pid=pid;this.parent=parent;this.start=start;}
        static Identity parse(int pid,String stat){
            try {int end=stat.lastIndexOf(')');if(!stat.startsWith(pid+" (")||end<0)return null;
                String[] fields=stat.substring(end+2).split(" ");
                if(fields[0].equals("Z")||fields[0].equals("X")||!fields[19].matches("[0-9]+"))return null;
                return new Identity(pid,Integer.parseInt(fields[1]),fields[19]);
            }catch(RuntimeException e){return null;}
        }
    }
    static Identity read(int pid){
        if(pid<=0)return null;
        try{return Identity.parse(pid,new String(Files.readAllBytes(new File("/proc/"+pid+"/stat").toPath()),StandardCharsets.UTF_8));}
        catch(IOException|SecurityException e){return null;}
    }
    static boolean belongs(Identity owner,int peer,Processes processes){
        if(owner==null)return false;Identity live=processes.read(owner.pid);
        if(live==null||!owner.start.equals(live.start))return false;
        for(int n=0;n<64&&peer>0;n++){
            if(peer==owner.pid)return true;Identity p=processes.read(peer);
            if(p==null||p.parent==peer)return false;peer=p.parent;
        }
        return false;
    }
    private final LocalServerSocket server;
    private final Processes processes;
    private final String run;
    private final AtomicBoolean closed=new AtomicBoolean();
    private final CountDownLatch attached=new CountDownLatch(1);
    private volatile Identity owner;
    private volatile java.util.function.IntSupplier ownerPid;
    private volatile LocalSocket connection;
    private Keys keys;
    GeminiKeyBroker(String run,Keys keys)throws IOException {this(run,keys,GeminiKeyBroker::read);}
    GeminiKeyBroker(String run,Keys keys,Processes processes)throws IOException {
        if(keys==null)throw new IOException("Invalid Gemini key source");
        this.processes=processes;this.run=run;server=new LocalServerSocket(address(run));this.keys=keys;
        Thread worker=new Thread(this::serve,"gemini-key-once");worker.setDaemon(true);worker.start();
        Thread guard=new Thread(()->{
            try{if(!attached.await(30,TimeUnit.SECONDS)){close();return;}
                while(!closed.get()){int pid=ownerPid==null?-1:ownerPid.getAsInt();if(pid<0){close();return;}if(pid>0){bindOwner();if(!belongs(owner,pid,processes)){close();return;}}Thread.sleep(1000);}
            }catch(InterruptedException e){Thread.currentThread().interrupt();close();}
        },"gemini-key-owner");guard.setDaemon(true);guard.start();
    }
    synchronized void attach(java.util.function.IntSupplier pid) {
        if(ownerPid!=null||closed.get())throw new IllegalStateException("Gemini key owner unavailable");
        ownerPid=pid;bindOwner();attached.countDown();
    }
    private synchronized void bindOwner(){if(owner==null&&ownerPid!=null)owner=processes.read(ownerPid.getAsInt());}
    private void serve(){
        try{
            while(!closed.get()){
                LocalSocket accepted=server.accept();
                try(LocalSocket peer=accepted){
                    connection=peer;peer.setSoTimeout(10000);Credentials credentials=peer.getPeerCredentials();
                    if(credentials.getUid()!=android.os.Process.myUid())continue;
                    DataInputStream in=new DataInputStream(peer.getInputStream());
                    if(in.readUnsignedShort()!=36)continue;byte[] id=new byte[36];in.readFully(id);
                    if(!run.equals(new String(id,StandardCharsets.US_ASCII)))continue;
                    if(!attached.await(10,TimeUnit.SECONDS))continue;bindOwner();
                    if(!belongs(owner,credentials.getPid(),processes))continue;
                    Keys selected;
                    synchronized(this){if(closed.get()||keys==null)return;selected=keys;keys=null;}
                    char[] secret=null;byte[] value=null;
                    try{
                        secret=selected.read();if(secret==null||!GeminiApiKeyVault.valid(secret))throw new java.security.GeneralSecurityException("Invalid Gemini key");
                        if(closed.get()||!belongs(owner,credentials.getPid(),processes))return;
                        value=new byte[secret.length];for(int i=0;i<secret.length;i++)value[i]=(byte)secret[i];
                        DataOutputStream out=new DataOutputStream(peer.getOutputStream());out.writeInt(value.length);out.write(value);out.flush();
                    }catch(java.security.GeneralSecurityException unavailable){new DataOutputStream(peer.getOutputStream()).writeInt(-1);}
                    finally{if(secret!=null)Arrays.fill(secret,'\0');if(value!=null)Arrays.fill(value,(byte)0);}
                    return;
                }catch(IOException invalidPeer){/* A foreign or incomplete request must not consume this run. */}
                finally{connection=null;}
            }
        }catch(IOException ignored){/* No authentication content in diagnostics. */}
        catch(InterruptedException e){Thread.currentThread().interrupt();}
        finally{close();}
    }
    @Override public void close(){
        if(!closed.compareAndSet(false,true))return;attached.countDown();
        synchronized(this){keys=null;}
        try{server.close();}catch(IOException ignored){}
        LocalSocket peer=connection;if(peer!=null)try{peer.close();}catch(IOException ignored){}
    }
}
