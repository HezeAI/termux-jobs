package com.termux.app.workspace;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
/** Managed, copied GGUF LoRA files. Binding to the base is user-declared, not proof of training. */
final class LocalAiLora {
 static final long MAX=256L*1024*1024;
 static void id(String id){if(!id.isEmpty()&&!id.matches("lora-[a-f0-9-]{36}"))throw new IllegalArgumentException("Invalid LoRA id");}
 static File metadata(LocalAiFiles files,String id)throws Exception{id(id);if(id.isEmpty())throw new IOException("No LoRA selected");File dir=new File(files.root,"adapters");return new File(dir,id+".json");}
 static JSONObject info(LocalAiFiles files,String id,JSONObject base)throws Exception{
  if(!base.optString("engine").equals("llama"))throw new IOException("LoRA requires llama.cpp CPU; LiteRT adapters are not verified");
  JSONObject m;try(InputStream in=new FileInputStream(metadata(files,id))){m=new JSONObject(LocalAiFiles.read(in,65536));}
  if(!m.getString("id").equals(id)||!m.getString("baseSha256").equals(base.getString("sha256")))throw new IOException("LoRA was selected for a different base model");return m;
 }
 static List<JSONObject> list(LocalAiFiles files,JSONObject base){List<JSONObject> out=new ArrayList<>();File[] entries=new File(files.root,"adapters").listFiles();if(entries!=null)for(File f:entries)if(f.getName().endsWith(".json"))try{String id=f.getName().substring(0,f.getName().length()-5);JSONObject m=info(files,id,base);if(new File(files.models,id+".gguf").isFile())out.add(m);}catch(Exception ignored){}return out;}
 static String hash(File f)throws Exception{MessageDigest md=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(f)){byte[] b=new byte[256*1024];int n;while((n=in.read(b))!=-1)md.update(b,0,n);}StringBuilder s=new StringBuilder();for(byte b:md.digest())s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();}
 static File checked(LocalAiFiles files,String id,JSONObject base)throws Exception{
  JSONObject m=info(files,id,base);File f=new File(files.models,id+".gguf");if(!f.isFile()||f.length()!=m.getLong("bytes")||!hash(f).equals(m.getString("sha256")))throw new IOException("LoRA integrity check failed");header(f);return f;
 }
 static JSONObject importFile(LocalAiFiles files,String name,long total,LocalAiFiles.Source source,JSONObject base,AtomicBoolean cancel,LocalAiFiles.Progress progress)throws Exception{
  if(!base.optString("engine").equals("llama")||!base.optString("sha256").matches("[a-f0-9]{64}"))throw new IOException("Select a llama.cpp base model first");if(!name.toLowerCase(Locale.ROOT).endsWith(".gguf")||total>MAX)throw new IOException("LoRA must be GGUF, at most 256 MiB");String id="lora-"+UUID.randomUUID();File part=new File(files.models,id+".part"),dest=new File(files.models,id+".gguf");boolean complete=false;
  try{long done=0;try(InputStream in=source.open();OutputStream out=new FileOutputStream(part)){byte[] b=new byte[256*1024];int n;while((n=in.read(b))!=-1){LocalAiFiles.check(cancel);done+=n;if(done>MAX||files.models.getUsableSpace()<32L*1024*1024)throw new IOException("LoRA size/storage limit");out.write(b,0,n);progress.update("import",done,total);}}
   LocalAiFiles.check(cancel);if(total>0&&total!=done)throw new IOException("Incomplete LoRA copy");header(part);String sha=hash(part);LocalAiFiles.check(cancel);JSONObject m=new JSONObject().put("id",id).put("name",name.substring(0,Math.min(name.length(),128))).put("baseSha256",base.getString("sha256")).put("sha256",sha).put("bytes",done).put("binding","user-declared");File meta=metadata(files,id);if(!meta.getParentFile().isDirectory()&&!meta.getParentFile().mkdirs())throw new IOException("Cannot create adapter registry");if(!part.renameTo(dest))throw new IOException("Cannot finalize LoRA copy");LocalAiFiles.atomic(meta,m.toString());complete=true;return m;
  }finally{part.delete();if(!complete)dest.delete();}
 }
 static void header(File file)throws Exception{try(RandomAccessFile f=new RandomAccessFile(file,"r")){Reader r=new Reader(f);if(r.u32()!=0x46554747L||r.u32()!=3)throw new IOException("Expected GGUF v3 adapter");long tensors=r.u64(),count=r.u64();if(tensors<1||tensors>10000||count>10000)throw new IOException("Invalid LoRA header counts");String type="",adapter="";for(int i=0;i<count;i++){String key=r.string();long kind=r.u32();if(key.equals("general.type")||key.equals("adapter.type")){if(kind!=8)throw new IOException("Invalid adapter metadata type");String value=r.string();if(key.equals("general.type"))type=value;else adapter=value;}else r.skip(kind,0);}if(!type.equals("adapter")||!adapter.equals("lora"))throw new IOException("This GGUF is not a LoRA adapter");}}
 static final class Reader {final RandomAccessFile f;final long limit;Reader(RandomAccessFile f)throws IOException{this.f=f;limit=Math.min(f.length(),16L*1024*1024);}void bytes(long n)throws IOException{if(n<0||n>limit-f.getFilePointer())throw new IOException("Invalid/truncated GGUF metadata");f.seek(f.getFilePointer()+n);}long u32()throws IOException{if(limit-f.getFilePointer()<4)throw new EOFException();return Integer.toUnsignedLong(Integer.reverseBytes(f.readInt()));}long u64()throws IOException{if(limit-f.getFilePointer()<8)throw new EOFException();long n=Long.reverseBytes(f.readLong());if(n<0)throw new IOException("Invalid GGUF length");return n;}String string()throws IOException{long n=u64();if(n>4096||n>limit-f.getFilePointer())throw new IOException("GGUF string too long");byte[] b=new byte[(int)n];f.readFully(b);return new String(b,StandardCharsets.UTF_8);}void skip(long t,int depth)throws IOException{if(depth>1)throw new IOException("Nested GGUF array");if(t==8){bytes(u64());return;}if(t==9){long kind=u32(),n=u64();if(n>1000000)throw new IOException("GGUF array too large");for(int i=0;i<n;i++)skip(kind,depth+1);return;}int n=t==0||t==1||t==7?1:t==2||t==3?2:t==4||t==5||t==6?4:t==10||t==11||t==12?8:0;if(n==0)throw new IOException("Unknown GGUF value type");bytes(n);}}
}
