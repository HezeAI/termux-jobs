package com.termux.app.workspace;

import android.app.Activity;
import com.termux.shared.shell.TermuxShellUtils;
import com.termux.shared.termux.TermuxConstants;
import org.json.*;
import java.io.*;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

/** All commands run through independent OpenSSH processes, pinned host keys, bounded I/O. */
final class ServerConnection implements AutoCloseable {
    private static final Set<ServerConnection> LIVE=ConcurrentHashMap.newKeySet();
    private static final Map<Activity,Set<String>> DISCONNECTED=new WeakHashMap<>();
    static synchronized boolean disconnected(Activity activity,String id){Set<String> ids=DISCONNECTED.get(activity);return ids!=null&&ids.contains(id);}
    static synchronized void allow(Activity activity,String id){Set<String> ids=DISCONNECTED.get(activity);if(ids!=null)ids.remove(id);}
    static void disconnectAll(Activity activity,String id){synchronized(ServerConnection.class){DISCONNECTED.computeIfAbsent(activity,a->new HashSet<>()).add(id);}for(ServerConnection c:LIVE)if(c.activity==activity&&c.profile.id.equals(id))c.close();}
    private final Activity activity;final ServerProfiles.Profile profile;
    private volatile Process process,tunnel;private volatile boolean closed;
    private final ScheduledExecutorService watchdog=Executors.newSingleThreadScheduledExecutor();
    ServerConnection(Activity a,ServerProfiles.Profile p){activity=a;profile=p;ServerDiagnostics.configure(p);synchronized(ServerConnection.class){LIVE.add(this);if(disconnected(a,p.id))close();}}
    File knownHosts(){return new File(activity.getFilesDir(),"server-host-"+profile.id);}
    boolean trusted(){return knownHosts().isFile();}
    private String bin(String name){return TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH+"/"+name;}
    private ProcessBuilder builder(List<String> command)throws Exception{
        if(closed||disconnected(activity,profile.id))throw new IOException("CLOSED");ProcessBuilder b=new ProcessBuilder(command);b.directory(new File(TermuxConstants.TERMUX_HOME_DIR_PATH));b.environment().clear();
        for(String item:TermuxShellUtils.buildEnvironment(activity,false,TermuxConstants.TERMUX_HOME_DIR_PATH)){int i=item.indexOf('=');if(i>0)b.environment().put(item.substring(0,i),item.substring(i+1));}
        b.environment().put("LC_ALL","C");b.redirectError(new File("/dev/null"));return b;
    }
    private SshPasswordBroker password(ProcessBuilder b)throws Exception{
        if(!profile.auth.equals("password")&&!(profile.auth.equals("key")&&!profile.encrypted.isEmpty()))return null;
        if(profile.encrypted.isEmpty())throw new IOException("PASSWORD_REQUIRED");
        char[] secret=SshPasswordVault.decrypt(profile.session());SshPasswordBroker broker;
        try{broker=new SshPasswordBroker(activity,secret,profile.auth.equals("key")?identity():null);}finally{Arrays.fill(secret,'\0');}
        try{File askpass=SessionShell.prepareAskpass(activity.getFilesDir(),profile.session(),activity.getApplicationInfo().sourceDir);
            b.environment().put("SSH_ASKPASS",askpass.getAbsolutePath());b.environment().put("SSH_ASKPASS_REQUIRE","force");b.environment().put("DISPLAY",":0");b.environment().put("TERMUX_JOBS_SSH_SOCKET",broker.address);return broker;
        }catch(Exception e){broker.close();throw e;}
    }
    List<String> ssh(){
        if(!ServerProfiles.valid(profile))throw new IllegalArgumentException("INVALID_PROFILE");
        List<String> c=new ArrayList<>(Arrays.asList(bin("ssh"),"-F","/dev/null","-T","-o","ConnectTimeout=10","-o","ServerAliveInterval=10","-o","ServerAliveCountMax=2","-o","StrictHostKeyChecking=yes","-o","UserKnownHostsFile="+knownHosts(),"-o","GlobalKnownHostsFile=/dev/null","-o","ControlMaster=no","-o","ControlPath=none","-o","ForwardAgent=no","-o","ClearAllForwardings=yes","-o","PermitLocalCommand=no"));
        c.addAll(Arrays.asList("-o",(profile.auth.equals("password")||profile.auth.equals("key")&&!profile.encrypted.isEmpty())?"BatchMode=no":"BatchMode=yes"));
        if(profile.auth.equals("password"))c.addAll(Arrays.asList("-o","PreferredAuthentications=password","-o","PubkeyAuthentication=no","-o","KbdInteractiveAuthentication=no","-o","NumberOfPasswordPrompts=1"));
        if(profile.auth.equals("key")){c.addAll(Arrays.asList("-o","IdentitiesOnly=yes","-i",identity(),"-o","PreferredAuthentications=publickey","-o","PasswordAuthentication=no","-o","KbdInteractiveAuthentication=no","-o","NumberOfPasswordPrompts=1"));}
        c.addAll(Arrays.asList("-p",Integer.toString(profile.port),"-l",profile.user));return c;
    }
    private String identity(){return profile.identity.startsWith("~/")?TermuxConstants.TERMUX_HOME_DIR_PATH+profile.identity.substring(1):profile.identity;}
    private byte[] execute(List<String> args,byte[] input,int limit,int timeout)throws Exception{
        ProcessBuilder b=builder(args);SshPasswordBroker broker=password(b);Process owned=null;ScheduledFuture<?> timer=null;
        try{
            owned=b.start();process=owned;final Process child=owned;timer=watchdog.schedule(child::destroy,timeout,TimeUnit.SECONDS);
            if(closed)owned.destroy();try(OutputStream out=owned.getOutputStream()){if(input!=null)out.write(input);}
            ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=owned.getInputStream()){byte[] buf=new byte[16384];int n;while((n=in.read(buf))!=-1){if(out.size()+n>limit)throw new IOException("RESPONSE_LIMIT");out.write(buf,0,n);}}
            int code=owned.waitFor();byte[] result=out.toByteArray();
            if(code!=0&&result.length==0)throw new IOException("SSH_FAILED");return result;
        }finally{if(timer!=null)timer.cancel(false);if(owned!=null)owned.destroy();process=null;if(broker!=null)broker.close();}
    }
    String[] scan()throws Exception{
        if(!new File(bin("ssh-keyscan")).canExecute())throw new IOException("OPENSSH_REQUIRED");
        List<String> cmd=Arrays.asList(bin("ssh-keyscan"),"-T","8","-t","ed25519","-p",Integer.toString(profile.port),profile.host);
        byte[] bytes=execute(cmd,null,16384,15);StringBuilder keys=new StringBuilder(),prints=new StringBuilder();
        for(String line:new String(bytes,StandardCharsets.UTF_8).split("\n")){
            if(line.startsWith("#")||line.trim().isEmpty())continue;String[] a=line.trim().split("\\s+");if(a.length!=3||!a[1].equals("ssh-ed25519"))continue;
            byte[] key=android.util.Base64.decode(a[2],android.util.Base64.DEFAULT);String digest=android.util.Base64.encodeToString(MessageDigest.getInstance("SHA-256").digest(key),android.util.Base64.NO_WRAP|android.util.Base64.NO_PADDING);
            keys.append(a[0]).append(' ').append(a[1]).append(' ').append(a[2]).append('\n');prints.append("ED25519 SHA256:").append(digest).append('\n');
        }
        if(keys.length()==0)throw new IOException("HOST_KEY_UNAVAILABLE");return new String[]{keys.toString(),prints.toString()};
    }
    void trust(String keys)throws Exception{try(OutputStream out=new FileOutputStream(knownHosts())){out.write(keys.getBytes(StandardCharsets.UTF_8));}knownHosts().setReadable(false,false);knownHosts().setReadable(true,true);knownHosts().setWritable(false,false);knownHosts().setWritable(true,true);}
    JSONObject remote(ServerDiagnostics.Op op,String command,byte[] input,int limit)throws Exception{
        ServerDiagnostics.Trace trace=ServerDiagnostics.begin(profile,op);long network=0,parse=0,size=-1,networkStart=0,parseStart=0;Exception failure=null;
        try{
            if(!trusted())throw new IOException("TRUST_REQUIRED");List<String> args=ssh();args.add("--");args.add(profile.host);args.add(command);
            networkStart=System.nanoTime();byte[] data=execute(args,input,limit,40);network=(System.nanoTime()-networkStart)/1000000;networkStart=0;size=data.length;parseStart=System.nanoTime();
            JSONObject result;try{result=new JSONObject(new String(data,StandardCharsets.UTF_8));}catch(JSONException e){throw new IOException("PYTHON_OR_AGENT_UNAVAILABLE");}
            if(result.has("ok")&&!result.optBoolean("ok"))throw new IOException(result.optString("error","AGENT_ERROR"));
            if(op==ServerDiagnostics.Op.FILE_DELETE&&result.optJSONObject("result")!=null){String code=result.getJSONObject("result").optString("error");if(!code.isEmpty())failure=new IOException(code);}
            return result;
        }catch(Exception e){failure=e;throw e;}finally{if(networkStart!=0)network=(System.nanoTime()-networkStart)/1000000;if(parseStart!=0)parse=(System.nanoTime()-parseStart)/1000000;trace.finish(network,parse,size,closed?new CancellationException():failure);}
    }
    JSONObject diagnose()throws Exception{
        JSONObject result=new JSONObject();
        try(java.net.Socket socket=new java.net.Socket()){socket.connect(new java.net.InetSocketAddress(profile.host,profile.port),8000);result.put("network",true);}catch(Exception e){return result.put("network",false);}
        if(!trusted())return result.put("trusted",false);result.put("trusted",true);
        try{return result.put("probe",probe());}catch(Exception e){String code=e.getMessage();return result.put("error",code!=null&&code.matches("[A-Z_]{1,80}")?code:"SSH_OR_PYTHON_FAILED");}
    }
    JSONObject probe()throws Exception{
        String script="import json,os,platform,shutil,sys; print(json.dumps({'supported':sys.version_info >= (3,8),'python':platform.python_version(),'home':os.path.expanduser('~'),'user':os.getuid(),'agent':os.path.isfile(os.path.expanduser('~/.local/share/termux-jobs-server/agent.py'))}))";
        return remote(ServerDiagnostics.Op.PROBE,"python3 -I -c "+SessionShell.quote(script),null,16384);
    }
    JSONObject rpc(JSONObject request)throws Exception{
        JSONObject r=remote(ServerDiagnostics.operation(request.optString("op")),"python3 -I \"$HOME/.local/share/termux-jobs-server/agent.py\" --rpc",request.toString().getBytes(StandardCharsets.UTF_8),2*1024*1024);
        if(!r.optBoolean("ok"))throw new IOException(r.optString("error","AGENT_ERROR"));return r.getJSONObject("result");
    }
    JSONObject schedules(JSONObject request)throws Exception{
        String code;try(InputStream in=activity.getAssets().open("workspace-server-schedules.py")){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);code=out.toString("UTF-8");}
        JSONObject reply=remote(ServerDiagnostics.Op.SCHEDULE,"python3 -I -c "+SessionShell.quote(code),request.toString().getBytes(StandardCharsets.UTF_8),2*1024*1024);
        if(!reply.optBoolean("ok"))throw new IOException(reply.optString("error","SCHEDULE_ERROR"));return reply.getJSONObject("result");
    }
    static java.util.Map<String,JSONArray> deletionGroups(JSONArray entries)throws Exception{
        if(entries.length()<1||entries.length()>500)throw new IOException("INVALID_SELECTION");
        java.util.Map<String,JSONArray> groups=new java.util.LinkedHashMap<>();java.util.Set<String> seen=new java.util.HashSet<>();
        for(int i=0;i<entries.length();i++){JSONObject e=entries.getJSONObject(i);String path=e.getString("path");int slash=path.lastIndexOf('/');
            if(!path.startsWith("/")||path.contains("\0")||path.endsWith("/")||path.contains("//")||java.util.Arrays.asList(path.split("/")).contains("..")||java.util.Arrays.asList(path.split("/")).contains(".")||!seen.add(path))throw new IOException("INVALID_PATH");
            String parent=slash==0?"/":path.substring(0,slash);JSONArray group=groups.get(parent);if(group==null){group=new JSONArray();groups.put(parent,group);}group.put(e);
        }return groups;
    }
    JSONObject deleteFiles(JSONArray entries)throws Exception{
        java.util.Map<String,JSONArray> groups=deletionGroups(entries);JSONArray deleted=new JSONArray();long nodes=0;String error="";
        for(java.util.Map.Entry<String,JSONArray> group:groups.entrySet()){JSONObject result=deleteFiles(group.getKey(),group.getValue());JSONArray done=result.getJSONArray("deleted");for(int i=0;i<done.length();i++)deleted.put(done.get(i));nodes+=result.optLong("removedNodes");error=result.optString("error");if(!error.isEmpty())break;}
        return new JSONObject().put("deleted",deleted).put("removedNodes",nodes).put("error",error);
    }
    JSONObject deleteFiles(String directory,JSONArray entries)throws Exception{
        String code;try(InputStream in=activity.getAssets().open("workspace-server-delete.py")){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);code=out.toString("UTF-8");}
        JSONObject request=new JSONObject().put("directory",directory).put("entries",entries);
        JSONObject reply=remote(ServerDiagnostics.Op.FILE_DELETE,"python3 -I -c "+SessionShell.quote(code),request.toString().getBytes(StandardCharsets.UTF_8),131072);
        return reply.getJSONObject("result");
    }
    JSONObject search(String path,String query)throws Exception{
        String code;try(InputStream in=activity.getAssets().open("workspace-server-search.py")){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);code=out.toString("UTF-8");}
        JSONObject reply=remote(ServerDiagnostics.Op.SEARCH,"python3 -I -c "+SessionShell.quote(code),new JSONObject().put("path",path).put("query",query).toString().getBytes(StandardCharsets.UTF_8),2*1024*1024);
        if(!reply.optBoolean("ok"))throw new IOException("SEARCH_FAILED");return reply.getJSONObject("result");
    }
    JSONObject components()throws Exception{
        String code="import json,os,shutil; print(json.dumps({'ffmpeg':bool(shutil.which('ffmpeg')),'ffprobe':bool(shutil.which('ffprobe')),'apt':bool(shutil.which('apt-get')),'sudo':bool(shutil.which('sudo')),'uid':os.getuid(),'home':os.path.expanduser('~'),'agent':os.path.isfile(os.path.expanduser('~/.local/share/termux-jobs-server/agent.py'))}))";
        return remote(ServerDiagnostics.Op.COMPONENTS,"python3 -I -c "+SessionShell.quote(code),null,16384);
    }
    JSONObject properties(String path)throws Exception{
        if(ServerFileInfo.media(path)){JSONObject result=rpc(new JSONObject().put("op","properties").put("path",path));result.put("components",components());return result;}
        String code="import json,os,sys; p=os.path.realpath(os.path.expanduser(sys.argv[1])); s=os.stat(p); print(json.dumps({'path':p,'size':s.st_size,'modified':s.st_mtime}))";
        return remote(ServerDiagnostics.Op.PROPERTIES,"python3 -I -c "+SessionShell.quote(code)+" "+SessionShell.quote(path),null,16384);
    }
    boolean tunnelActive(){Process owned=tunnel;if(owned==null)return false;try{owned.exitValue();return false;}catch(IllegalThreadStateException e){return true;}}
    void install()throws Exception{
        String setup="import os,pathlib,sys,json,hashlib; assert sys.version_info >= (3,8); os.umask(0o077); root=pathlib.Path.home()/'.local/share/termux-jobs-server'; parents=[pathlib.Path.home()/'.local',pathlib.Path.home()/'.local/share',root]; "
            +"exec(\"for p in parents:\\n if p.is_symlink(): raise RuntimeError('SYMLINK_DIRECTORY')\\n p.mkdir(mode=0o700,exist_ok=True)\\n if p.stat().st_uid!=os.getuid(): raise RuntimeError('DIRECTORY_OWNER')\"); "
            +"data=sys.stdin.buffer.read(262145); assert len(data)==int(sys.argv[1]) and hashlib.sha256(data).hexdigest()==sys.argv[2]; "
            +"compile(data,'agent.py','exec'); target=root/'agent.py'; tmp=root/('.install-'+os.urandom(8).hex()); "
            +"fd=os.open(tmp,os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600); "
            +"exec(\"try:\\n with os.fdopen(fd,'wb') as f: f.write(data); f.flush(); os.fsync(f.fileno())\\n os.link(tmp,target)\\nfinally:\\n tmp.unlink()\"); "
            +"print(json.dumps({'installed':True}))";
        byte[] data;try(InputStream in=activity.getAssets().open("workspace-server-agent.py")){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);data=out.toByteArray();}
        StringBuilder digest=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(data))digest.append(String.format(Locale.ROOT,"%02x",b&255));
        remote(ServerDiagnostics.Op.INSTALL,"python3 -I -c "+SessionShell.quote(setup)+" "+data.length+" "+digest,data,16384);rpc(new JSONObject().put("op","status"));
    }
    Process openTunnel(int remotePort,int[] localPort)throws Exception{
        if(remotePort<1||remotePort>65535)throw new IOException("INVALID_PORT");stopTunnel();
        try(ServerSocket socket=new ServerSocket(0,0,java.net.InetAddress.getByName("127.0.0.1"))){localPort[0]=socket.getLocalPort();}
        List<String> args=ssh();int index=args.indexOf("ClearAllForwardings=yes");args.set(index,"ClearAllForwardings=no");args.addAll(Arrays.asList("-N","-o","ExitOnForwardFailure=yes","-L","127.0.0.1:"+localPort[0]+":127.0.0.1:"+remotePort,"--",profile.host));
        ProcessBuilder b=builder(args);SshPasswordBroker broker=password(b);try{tunnel=b.start();final Process child=tunnel;if(closed){child.destroy();throw new IOException("CLOSED");}watchdog.schedule(child::destroy,30,TimeUnit.MINUTES);
            for(int i=0;i<100;i++){try{child.exitValue();throw new IOException("TUNNEL_FAILED");}catch(IllegalThreadStateException expected){}
                try(java.net.Socket check=new java.net.Socket()){check.connect(new java.net.InetSocketAddress("127.0.0.1",localPort[0]),100);return child;}catch(IOException notReady){Thread.sleep(100);}}
            child.destroy();throw new IOException("TUNNEL_FAILED");
        }catch(Exception e){stopTunnel();throw e;}finally{if(broker!=null)broker.close();}
    }
    void stopTunnel(){Process owned=tunnel;if(owned!=null)owned.destroy();tunnel=null;}
    String terminalCommand(String directory){WorkspaceStore.Session s=profile.session();s.directory=directory==null||directory.isEmpty()?"~":directory;String cmd=SshSessionShell.command(s);return cmd.replaceFirst("ssh -t",java.util.regex.Matcher.quoteReplacement("ssh -t -o StrictHostKeyChecking=yes -o "+SessionShell.quote("UserKnownHostsFile="+knownHosts())+" -o GlobalKnownHostsFile=/dev/null -o ForwardAgent=no"));}
    interface DownloadProgress {void received(long bytes);}
    void download(String path,File target,long max)throws Exception{download(path,target,max,bytes->{});}
    void download(String path,File target,long max,DownloadProgress progress)throws Exception{
        try(OutputStream out=new FileOutputStream(target)){download(path,out,max,progress);}catch(Exception e){target.delete();throw e;}
    }
    void download(String path,OutputStream out,long max,DownloadProgress progress)throws Exception{
        if(path.indexOf('\0')>=0||path.indexOf('\n')>=0)throw new IOException("INVALID_PATH");
        List<String> args=ssh();args.addAll(Arrays.asList("--",profile.host,"exec cat -- "+SessionShell.quote(path)));
        ProcessBuilder b=builder(args);SshPasswordBroker broker=password(b);Process owned=null;ScheduledFuture<?> timer=null;
        try{owned=b.start();process=owned;Process child=owned;timer=watchdog.schedule(child::destroy,10,TimeUnit.MINUTES);if(closed)owned.destroy();long total=0;
            try(OutputStream unused=owned.getOutputStream();InputStream in=owned.getInputStream()){unused.close();byte[] buf=new byte[65536];int n;while((n=in.read(buf))!=-1){if(closed)throw new IOException("CANCELLED");total+=n;if(total>max)throw new IOException("FILE_LIMIT");out.write(buf,0,n);progress.received(total);}}
            if(owned.waitFor()!=0||closed)throw new IOException("DOWNLOAD_FAILED");
        }finally{if(timer!=null)timer.cancel(false);if(owned!=null)owned.destroy();process=null;if(broker!=null)broker.close();}
    }
    ServerSftp sftp()throws Exception{return sftp(30);}
    ServerSftp sftp(long timeoutMinutes)throws Exception{
        if(timeoutMinutes<1||timeoutMinutes>360)throw new IOException("INVALID_TIMEOUT");
        ServerDiagnostics.Trace trace=ServerDiagnostics.begin(profile,ServerDiagnostics.Op.SFTP_CONNECT);Exception failure=null;
        try{
        if(!trusted())throw new IOException("TRUST_REQUIRED");List<String> args=ssh();args.addAll(Arrays.asList("-s","--",profile.host,"sftp"));ProcessBuilder b=builder(args);SshPasswordBroker broker=password(b);Process child=null;
        try{child=b.start();process=child;if(closed){child.destroy();throw new IOException("CLOSED");}final Process owned=child;ScheduledFuture<?> timer=watchdog.schedule(owned::destroy,timeoutMinutes,TimeUnit.MINUTES);return new ServerSftp(owned.getInputStream(),owned.getOutputStream(),()->{timer.cancel(false);owned.destroy();if(broker!=null)broker.close();});}
        catch(Exception e){if(child!=null)child.destroy();if(broker!=null)broker.close();throw e;}
        }catch(Exception e){failure=e;throw e;}finally{trace.finish(trace.elapsed(),0,-1,closed?new CancellationException():failure);}
    }
    public void close(){closed=true;LIVE.remove(this);Process owned=process;if(owned!=null)owned.destroy();stopTunnel();watchdog.shutdownNow();}
}
