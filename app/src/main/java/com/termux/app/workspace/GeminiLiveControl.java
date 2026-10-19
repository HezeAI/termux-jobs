package com.termux.app.workspace;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Small private Unix protocol; no subprocess, credentials, terminal key injection or cloud polling. */
final class GeminiLiveControl {
    static boolean validModel(String s){return s!=null&&s.matches("(?:auto|flash|pro|gemini-[A-Za-z0-9][A-Za-z0-9._-]{0,120})");}
    static final class Failure extends IOException {
        final String code;
        Failure(String code){super(code);this.code=code;}
    }
    // Android LocalSocketImpl.setOption requires an existing fd; connect creates it.
    static void connect(LocalSocket socket,String path) throws IOException {
        socket.connect(new LocalSocketAddress(path,LocalSocketAddress.Namespace.FILESYSTEM));
        socket.setSoTimeout(15000);
    }
    static JSONObject request(File files,String run,JSONObject value) throws Exception {
        if(!GeminiCli.validThread(run))throw new Failure("INVALID_RUN");
        File socket=new File(GeminiCli.directory(files),run+".sock");String stage="CONNECT_FAILED";
        try(LocalSocket client=new LocalSocket()){
            connect(client,socket.getAbsolutePath());stage="PROTOCOL_FAILED";
            boolean chat=value.optString("op").startsWith("chat-")||value.optString("op").startsWith("terminal-");
            byte[] data=(value.toString()+"\n").getBytes(StandardCharsets.UTF_8);if(data.length>(chat?65536:4096))throw new Failure("REQUEST_TOO_LARGE");
            client.getOutputStream().write(data);client.getOutputStream().flush();
            JSONObject response=new JSONObject(readReply(client.getInputStream(),chat?524288:65536));
            if(!response.optBoolean("ok")){
                String error=response.optString("error");
                throw new Failure(Arrays.asList("NOT_READY","CONTROL_TIMEOUT","RESTARTED","ENDED","CONTROL_FAILED","INVALID_MODEL","INVALID_MODE","THINKING_UNAVAILABLE","THINKING_BUSY","MODE_UNAVAILABLE","MODE_BUSY","STALE_PERMISSION","PERMISSION_FAILED","INVALID_PERMISSION").contains(error)?error:"CONTROL_FAILED");
            }
            return response;
        }catch(Failure e){throw e;}catch(java.io.InterruptedIOException e){throw new Failure("CONTROL_TIMEOUT");}
        catch(Exception e){throw new Failure(stage);}
    }
    static String readReply(InputStream input,int limit)throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;
        while((n=input.read(buffer))!=-1){int length=n;for(int i=0;i<n;i++)if(buffer[i]=='\n'){length=i;break;}if(out.size()+length>limit)throw new Failure("RESPONSE_TOO_LARGE");out.write(buffer,0,length);if(length<n)break;}
        return out.toString("UTF-8");
    }
    static List<String> models(JSONObject response) throws Exception {
        JSONArray array=response.getJSONArray("models");if(array.length()>150)throw new IOException("CATALOG_TOO_LARGE");
        List<String> out=new ArrayList<>();for(int i=0;i<array.length();i++){String id=array.getJSONObject(i).getString("id");if(validModel(id)&&!out.contains(id))out.add(id);}return out;
    }
}
