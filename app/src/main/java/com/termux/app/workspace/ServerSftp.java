package com.termux.app.workspace;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Bounded-window SFTP v3 client over the existing pinned SSH transport. No shell filenames. */
final class ServerSftp implements AutoCloseable {
    interface Progress {void sent(long bytes);}
    private final DataInputStream in;private final DataOutputStream out;private final AutoCloseable transport;
    private volatile String pendingTemp;
    String pendingTemp(){return pendingTemp;}
    void removeTemporary(String path)throws IOException{if(path==null||!path.startsWith("/")||path.indexOf(0)>=0||!new java.io.File(path).getName().matches("\\.termux-jobs-upload-[a-f0-9-]{36}"))throw new IOException("INVALID_PATH");if(stat(path).exists)ok(request(13,b->string(b,path)));}
    private int sequence;private boolean posixRename;private volatile boolean closed;
    static final int WRITE_WINDOW=8;
    static final long MAX_BYTES=2L*1024*1024*1024;
    static final class Info {
        final boolean exists;final long size;final int permissions,mtime;
        Info(boolean e,long s,int p,int m){exists=e;size=s;permissions=p;mtime=m;}
        boolean directory(){return (permissions&0170000)==0040000;}
        boolean regular(){return (permissions&0170000)==0100000;}
        boolean same(Info other){return exists==other.exists&&(!exists||size==other.size&&permissions==other.permissions&&mtime==other.mtime);}
    }
    ServerSftp(InputStream input,OutputStream output,AutoCloseable transport)throws Exception{
        in=new DataInputStream(input);out=new DataOutputStream(output);this.transport=transport;
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream b=new DataOutputStream(bytes);b.writeInt(3);packet(1,bytes.toByteArray());
        DataInputStream version=response();if(version.readUnsignedByte()!=2||version.readInt()!=3)throw new IOException("SFTP_VERSION");
        while(version.available()>0){String name=string(version),value=string(version);if(name.equals("posix-rename@openssh.com")&&value.equals("1"))posixRename=true;}
    }
    static boolean validName(String name){if(name==null||name.isEmpty()||name.equals(".")||name.equals("..")||name.getBytes(StandardCharsets.UTF_8).length>255)return false;for(int i=0;i<name.length();i++)if(name.charAt(i)=='/'||Character.isISOControl(name.charAt(i))||Character.isSurrogate(name.charAt(i))&&(i+1>=name.length()||!Character.isSurrogatePair(name.charAt(i),name.charAt(++i))))return false;return true;}
    private static void string(DataOutputStream b,String value)throws IOException{blob(b,value.getBytes(StandardCharsets.UTF_8));}
    private static void blob(DataOutputStream b,byte[] value)throws IOException{b.writeInt(value.length);b.write(value);}
    private static byte[] blob(DataInputStream b)throws IOException{int n=b.readInt();if(n<0||n>65536||n>b.available())throw new IOException("SFTP_PACKET");byte[] bytes=new byte[n];b.readFully(bytes);return bytes;}
    private static String string(DataInputStream b)throws IOException{return new String(blob(b),StandardCharsets.UTF_8);}
    private void packet(int type,byte[] data)throws IOException{if(closed)throw new IOException("CANCELLED");out.writeInt(data.length+1);out.writeByte(type);out.write(data);out.flush();}
    private DataInputStream response()throws IOException{int n=in.readInt();if(n<1||n>262144)throw new IOException("SFTP_PACKET");byte[] data=new byte[n];in.readFully(data);return new DataInputStream(new ByteArrayInputStream(data));}
    private interface Body {void write(DataOutputStream b)throws IOException;}
    private int send(int type,Body body)throws IOException{ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream b=new DataOutputStream(bytes);int id=++sequence;b.writeInt(id);body.write(b);try{packet(type,bytes.toByteArray());}catch(IOException failure){close();throw failure;}return id;}
    private DataInputStream request(int type,Body body)throws IOException{int id=send(type,body);DataInputStream r=response();int responseType=r.readUnsignedByte();if(r.readInt()!=id)throw new IOException("SFTP_ID");ByteArrayOutputStream answer=new ByteArrayOutputStream();answer.write(responseType);byte[] rest=new byte[r.available()];r.readFully(rest);answer.write(rest);return new DataInputStream(new ByteArrayInputStream(answer.toByteArray()));}
    private int acknowledgeWrite(Map<Integer,Integer> pending)throws IOException{
        final int count,status;
        try{DataInputStream r=response();int type=r.readUnsignedByte(),id=r.readInt();Integer size=pending.remove(id);if(type!=101||size==null)throw new IOException("SFTP_ID");count=size;status=r.readInt();}
        catch(IOException broken){close();throw broken;}
        if(status!=0)throw new IOException("SFTP_WRITE_FAILED");return count;
    }
    private void writeFile(InputStream source,byte[] handle,Progress progress)throws Exception{
        Map<Integer,Integer> pending=new LinkedHashMap<>();byte[] buffer=new byte[32768];long sent=0,acknowledged=0;boolean eof=false;
        try{
            while(!eof||!pending.isEmpty()){
                while(!eof&&pending.size()<WRITE_WINDOW){
                    if(closed)throw new IOException("CANCELLED");int count=source.read(buffer);if(count<0){eof=true;break;}if(count==0)continue;if(sent+count>MAX_BYTES)throw new IOException("FILE_LIMIT");
                    final long offset=sent;final int size=count;
                    int id=send(6,b->{blob(b,handle);b.writeLong(offset);b.writeInt(size);b.write(buffer,0,size);});pending.put(id,size);sent+=size;
                }
                if(!pending.isEmpty()){acknowledged+=acknowledgeWrite(pending);progress.sent(acknowledged);}
            }
        }catch(Exception failure){
            // Consume every known outstanding reply before CLOSE/REMOVE. Never reuse a corrupt stream.
            while(!closed&&!pending.isEmpty())try{acknowledgeWrite(pending);}catch(IOException drain){failure.addSuppressed(drain);}
            throw failure;
        }
    }
    private static void ok(DataInputStream r)throws IOException{if(r.readUnsignedByte()!=101||r.readInt()!=0)throw new IOException("SFTP_FAILED");}
    String canonical(String directory)throws IOException{DataInputStream r=request(16,b->string(b,directory));if(r.readUnsignedByte()!=104||r.readInt()!=1)throw new IOException("SFTP_DIRECTORY");String path=string(r);if(!path.startsWith("/")||path.indexOf('\0')>=0)throw new IOException("SFTP_DIRECTORY");return path;}
    Info stat(String path)throws IOException{
        DataInputStream r=request(7,b->string(b,path));int type=r.readUnsignedByte();if(type==101){if(r.readInt()==2)return new Info(false,0,0,0);throw new IOException("SFTP_STAT");}if(type!=105)throw new IOException("SFTP_STAT");
        return attributes(r);
    }
    private static Info attributes(DataInputStream r)throws IOException{
        int flags=r.readInt();if((flags&~15)!=0)throw new IOException("SFTP_ATTRIBUTES");long size=(flags&1)!=0?r.readLong():0;if(size<0)throw new IOException("SFTP_SIZE");if((flags&2)!=0){r.readInt();r.readInt();}int mode=(flags&4)!=0?r.readInt():0;int modified=0;if((flags&8)!=0){r.readInt();modified=r.readInt();}return new Info(true,size,mode,modified);
    }
    static final long MAX_DOWNLOAD_BYTES=10L*1024*1024*1024;
    static final class Entry {final String name;final Info info;Entry(String n,Info i){name=n;info=i;}}
    List<Entry> list(String path)throws IOException{
        DataInputStream opened=request(11,b->string(b,path));if(opened.readUnsignedByte()!=102)throw new IOException("SFTP_DIRECTORY");byte[] handle=blob(opened);List<Entry> entries=new ArrayList<>();
        try{while(true){DataInputStream r=request(12,b->blob(b,handle));int type=r.readUnsignedByte();if(type==101){if(r.readInt()==1)break;throw new IOException("SFTP_READDIR");}if(type!=104)throw new IOException("SFTP_READDIR");int count=r.readInt();if(count<0||count>20000||entries.size()+count>20002)throw new IOException("DIRECTORY_LIMIT");for(int i=0;i<count;i++){String name=string(r);string(r);Info info=attributes(r);if(name.equals(".")||name.equals(".."))continue;if(!validName(name))throw new IOException("UNSAFE_NAME");entries.add(new Entry(name,info));}}return entries;}
        finally{if(!closed)try{ok(request(4,b->blob(b,handle)));}catch(IOException error){close();}}
    }
    void download(String path,OutputStream target,Info expected,Progress progress)throws Exception{
        if(!expected.regular()||expected.size>MAX_DOWNLOAD_BYTES)throw new IOException("FILE_LIMIT_OR_TYPE");if(!expected.same(stat(path)))throw new IOException("REMOTE_CHANGED");
        DataInputStream r=request(3,b->{string(b,path);b.writeInt(1);b.writeInt(0);});if(r.readUnsignedByte()!=102)throw new IOException("SFTP_OPEN");byte[] handle=blob(r);boolean success=false;
        try{long received=0,next=0;Map<Integer,Long> pending=new HashMap<>();TreeMap<Long,byte[]> ready=new TreeMap<>();
            while(received<expected.size){
                while(next<expected.size&&pending.size()+ready.size()<WRITE_WINDOW){final long offset=next;final int length=(int)Math.min(32768,expected.size-next);int id=send(5,b->{blob(b,handle);b.writeLong(offset);b.writeInt(length);});pending.put(id,offset);next+=length;}
                DataInputStream data=response();int type=data.readUnsignedByte(),id=data.readInt();Long offset=pending.remove(id);if(type!=103||offset==null)throw new IOException("SFTP_READ");byte[] chunk=blob(data);int wanted=(int)Math.min(32768,expected.size-offset);if(chunk.length!=wanted)throw new IOException("SFTP_SHORT_READ");ready.put(offset,chunk);
                while(ready.containsKey(received)){byte[] current=ready.remove(received);target.write(current);received+=current.length;progress.sent(received);}
            }success=true;}

        catch(Exception error){close();throw error;}
        finally{if(!closed)ok(request(4,b->blob(b,handle)));}
        if(success&&!expected.same(stat(path)))throw new IOException("REMOTE_CHANGED");
    }

    void move(String source,String directory,Info expected)throws Exception{
        String name=new java.io.File(source).getName();if(!source.startsWith("/")||!validName(name)||!directory.startsWith("/"))throw new IOException("INVALID_PATH");String target=directory+(directory.equals("/")?"":"/")+name;
        if(!expected.regular()&&!expected.directory())throw new IOException("UNSUPPORTED_TYPE");if(target.equals(source)||directory.equals(source)||directory.startsWith(source+"/"))throw new IOException("MOVE_INTO_SELF");if(!expected.same(stat(source)))throw new IOException("REMOTE_CHANGED");if(stat(target).exists)throw new IOException("DESTINATION_EXISTS");ok(request(18,b->{string(b,source);string(b,target);}));
    }
    boolean canReplace(){return posixRename;}
    void upload(InputStream source,String directory,String name,Info expected,Progress progress)throws Exception{
        if(!validName(name)||!directory.startsWith("/"))throw new IOException("INVALID_PATH");if(expected.exists&&(!expected.regular()||!posixRename))throw new IOException("SFTP_REPLACE_UNSUPPORTED");
        String target=directory+(directory.equals("/")?"":"/")+name,temp=directory+(directory.equals("/")?"":"/")+".termux-jobs-upload-"+UUID.randomUUID();
        pendingTemp=temp;byte[] handle=null;boolean published=false;
        try{
            DataInputStream r=request(3,b->{string(b,temp);b.writeInt(2|8|32);b.writeInt(4);b.writeInt(0600);});if(r.readUnsignedByte()!=102)throw new IOException("SFTP_OPEN");handle=blob(r);final byte[] h=handle;
            writeFile(source,h,progress);
            if(expected.exists)ok(request(10,b->{blob(b,h);b.writeInt(4);b.writeInt(expected.permissions&0777);}));
            ok(request(4,b->blob(b,h)));handle=null;
            if(!expected.same(stat(target)))throw new IOException("REMOTE_CHANGED");
            if(expected.exists)ok(request(200,b->{string(b,"posix-rename@openssh.com");string(b,temp);string(b,target);}));
            else ok(request(18,b->{string(b,temp);string(b,target);}));
            published=true;pendingTemp=null;
        }finally{
            if(handle!=null&&!closed){final byte[] h=handle;try{ok(request(4,b->blob(b,h)));}catch(Exception ignored){}}
            if(!published&&!closed)try{ok(request(13,b->string(b,temp)));pendingTemp=null;}catch(Exception ignored){}
        }
    }
    @Override public void close(){closed=true;try{transport.close();}catch(Exception ignored){}try{in.close();}catch(Exception ignored){}try{out.close();}catch(Exception ignored){}}
}
