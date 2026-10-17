package com.termux.app.workspace;

import android.content.Context;
import android.net.Uri;
import android.database.Cursor;
import android.provider.OpenableColumns;
import android.util.AtomicFile;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;

/** Model files only: never executes imports or touches Termux HOME/PREFIX. */
final class LocalAiFiles {
    interface Progress { void update(String phase, long done, long total); }
    final Context context;
    final File root, models, chats;
    LocalAiFiles(Context context) {
        this.context=context.getApplicationContext(); root=new File(context.getFilesDir(),"local-ai");
        models=new File(root,"models");chats=new File(root,"chats");models.mkdirs();chats.mkdirs();
    }
    static String safeId(String id) {
        if(id==null||!id.matches("[A-Za-z0-9_-]{1,90}"))throw new IllegalArgumentException("Invalid local ID");return id;
    }
    File modelFile(JSONObject m) throws JSONException {return new File(models,safeId(m.getString("id"))+ (m.getString("engine").equals("llama")?".gguf":".litertlm"));}
    List<JSONObject> catalog() throws Exception {
        List<JSONObject> result=new ArrayList<>();
        try(InputStream in=context.getAssets().open("workspace-local-ai-models.json")) {
            JSONArray a=new JSONObject(read(in,1024*1024)).getJSONArray("models");for(int i=0;i<a.length();i++)result.add(a.getJSONObject(i));
        }
        File[] files=models.listFiles((d,n)->n.endsWith(".json"));if(files!=null)for(File f:files)try{result.add(new JSONObject(read(new FileInputStream(f),65536)));}catch(Exception ignored){}
        return result;
    }
    JSONObject find(String id) throws Exception {for(JSONObject m:catalog())if(m.getString("id").equals(id))return m;throw new IOException("Model not found");}
    static String read(InputStream input,int limit) throws IOException {
        try(InputStream in=input; ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>limit)throw new IOException("File too large");out.write(b,0,n);}return out.toString("UTF-8");
        }
    }
    static void atomic(File f,String value) throws IOException {
        AtomicFile a=new AtomicFile(f);FileOutputStream out=null;try{out=a.startWrite();out.write(value.getBytes(StandardCharsets.UTF_8));a.finishWrite(out);}catch(IOException e){if(out!=null)a.failWrite(out);throw e;}
    }
    static void check(AtomicBoolean cancelled) {if(cancelled.get()||Thread.currentThread().isInterrupted())throw new CancellationException();}
    static String engine(byte[] header,String name) throws IOException {
        if(header.length>=4&&header[0]=='G'&&header[1]=='G'&&header[2]=='U'&&header[3]=='F')return "llama";
        // Container validation is followed by LiteRT-LM's model parser at initialization.
        if(header.length>=16&&new String(header,0,8,StandardCharsets.US_ASCII).equals("LITERTLM"))return "litert";
        throw new IOException("Expected GGUF or .litertlm model");
    }
    static String digest(File f,AtomicBoolean cancel,Progress progress) throws Exception {
        MessageDigest md=MessageDigest.getInstance("SHA-256");long done=0;
        try(InputStream in=new FileInputStream(f)){byte[] b=new byte[1024*1024];int n;while((n=in.read(b))!=-1){check(cancel);md.update(b,0,n);done+=n;progress.update("SHA-256",done,f.length());}}
        StringBuilder h=new StringBuilder();for(byte b:md.digest())h.append(String.format(Locale.ROOT,"%02x",b&255));return h.toString();
    }
    void download(JSONObject m,AtomicBoolean cancel,Progress progress) throws Exception {
        File dest=modelFile(m),part=new File(dest+".part");long expected=m.getLong("bytes");
        if(dest.exists())return;
        long offset=part.exists()?part.length():0;
        if(offset>expected){if(!part.delete())throw new IOException("Cannot reset partial download");offset=0;}
        if(models.getUsableSpace()<expected-offset+256L*1024*1024)throw new IOException("Not enough free storage");
        String url="https://huggingface.co/"+m.getString("repository")+"/resolve/"+m.getString("revision")+"/"+m.getString("file");
        if(offset<expected){
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(20000);c.setReadTimeout(20000);if(offset>0)c.setRequestProperty("Range","bytes="+offset+"-");
        try {
            int code=c.getResponseCode();if(code!=200&&code!=206)throw new IOException("HTTP "+code);
            if(offset>0&&code==206&&(c.getHeaderField("Content-Range")==null||!c.getHeaderField("Content-Range").startsWith("bytes "+offset+"-")))throw new IOException("Invalid download range");
            if(code==200)offset=0;
            try(InputStream in=c.getInputStream();OutputStream out=new FileOutputStream(part,offset>0)){
                long done=offset;byte[] b=new byte[256*1024];int n;
                while((n=in.read(b))!=-1){check(cancel);done+=n;if(done>expected)throw new IOException("Unexpected model size");out.write(b,0,n);progress.update("download",done,expected);}
            }
        }finally{c.disconnect();}
        }
        check(cancel);if(part.length()!=expected)throw new IOException("Incomplete model; retry to resume");
        if(!digest(part,cancel,progress).equals(m.getString("sha256"))){part.delete();throw new IOException("SHA-256 mismatch");}
        check(cancel);if(!part.renameTo(dest))throw new IOException("Cannot finalize model");
    }
    JSONObject importModel(Uri uri,AtomicBoolean cancel,Progress progress) throws Exception {
        String name="model";long total=-1;
        try(Cursor c=context.getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE},null,null,null)){
            if(c!=null&&c.moveToFirst()){int ni=c.getColumnIndex(OpenableColumns.DISPLAY_NAME),si=c.getColumnIndex(OpenableColumns.SIZE);if(ni>=0)name=c.getString(ni);if(si>=0&&!c.isNull(si))total=c.getLong(si);}
        }
        return importModel(name,total,()->context.getContentResolver().openInputStream(uri),cancel,progress);
    }
    interface Source {InputStream open() throws Exception;}
    JSONObject importModel(String name,long total,Source source,AtomicBoolean cancel,Progress progress) throws Exception {
        if(name==null)name="model";
        String id="import-"+UUID.randomUUID();File part=new File(models,id+".part"),dest=null;
        try {
            if(total>0&&models.getUsableSpace()<total+256L*1024*1024)throw new IOException("Not enough free storage");
            MessageDigest md=MessageDigest.getInstance("SHA-256");byte[] header=new byte[32];int used=0;long done=0;
            try(InputStream in=source.open();OutputStream out=new FileOutputStream(part)){
                if(in==null)throw new IOException("Cannot open selected file");byte[] b=new byte[256*1024];int n;
                while((n=in.read(b))!=-1){check(cancel);if(models.getUsableSpace()<32L*1024*1024)throw new IOException("Not enough free storage");int h=Math.min(n,32-used);if(h>0){System.arraycopy(b,0,header,used,h);used+=h;}out.write(b,0,n);md.update(b,0,n);done+=n;progress.update("import",done,total);}
            }
            check(cancel);if(total>=0&&done!=total)throw new IOException("Incomplete import");
            String engine=engine(Arrays.copyOf(header,used),name);StringBuilder sha=new StringBuilder();for(byte b:md.digest())sha.append(String.format(Locale.ROOT,"%02x",b&255));
            JSONObject m=new JSONObject().put("id",id).put("name",name.substring(0,Math.min(name.length(),160))).put("engine",engine).put("backend","auto").put("bytes",done).put("sha256",sha.toString()).put("imported",true).put("experimental",true);
            // Names are hints, never proof of NPU compatibility.
            String lower=name.toLowerCase(Locale.ROOT);if(lower.contains("qualcomm")||lower.matches(".*_sm[0-9].*")||lower.contains("qcs8275"))m.put("backend","npu");
            if(LocalAiNpu.verifiedModel(m))m.put("backend","npu").put("name","Gemma 4 E2B · SM8750 NPU (import)").put("vision",false);
            dest=modelFile(m);if(!part.renameTo(dest))throw new IOException("Cannot finalize import");
            atomic(new File(models,id+".json"),m.toString());return m;
        }catch(Exception e){part.delete();if(dest!=null)dest.delete();throw e;}
    }
    void deleteModel(JSONObject m) throws Exception {File f=modelFile(m);if(f.exists()&&!f.delete())throw new IOException("Cannot remove model");new File(f+".part").delete();if(m.optBoolean("imported"))new File(models,safeId(m.getString("id"))+".json").delete();}
    JSONArray history(String id) throws Exception {File f=new File(chats,safeId(id)+".json");return f.exists()?new JSONArray(read(new AtomicFile(f).openRead(),8*1024*1024)):new JSONArray();}
    void saveChatMetadata(WorkspaceStore.Session p)throws Exception{
        if(p.localChat.isEmpty())return;File dir=new File(root,"chat-metadata");if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Cannot create history metadata");atomic(new File(dir,safeId(p.localChat)+".json"),new JSONObject().put("name",p.name).put("project",p.localProject).put("model",p.localModel).toString());
    }
    JSONObject chatMetadata(String id)throws Exception{File f=new File(root,"chat-metadata/"+safeId(id)+".json");return f.isFile()?new JSONObject(read(new FileInputStream(f),65536)):new JSONObject();}
    void deleteHistory(String id)throws Exception{File base=new File(chats,safeId(id)+".json");for(File f:new File[]{new File(base+".new"),new File(base+".bak"),base})if(f.exists()&&!f.delete())throw new IOException("Cannot delete chat history");File meta=new File(root,"chat-metadata/"+safeId(id)+".json");if(meta.exists()&&!meta.delete())throw new IOException("History removed but metadata deletion failed");}
    void saveHistory(String id,JSONArray messages) throws Exception {atomic(new File(chats,safeId(id)+".json"),messages.toString());}
}
