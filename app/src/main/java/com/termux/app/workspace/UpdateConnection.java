package com.termux.app.workspace;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.URL;

/** One updater operation owns its connection, including redirects and cancellation. */
final class UpdateConnection {
    interface Factory { HttpURLConnection open(URL url) throws IOException; }
    private final Factory factory;
    private boolean cancelled;
    private HttpURLConnection active;

    UpdateConnection(){this(url -> (HttpURLConnection) url.openConnection());}
    UpdateConnection(Factory factory){this.factory=factory;}
    synchronized boolean isCancelled(){return cancelled;}
    void cancel(){
        HttpURLConnection connection;
        synchronized(this){cancelled=true;connection=active;}
        if(connection!=null)connection.disconnect();
    }
    synchronized void check()throws InterruptedIOException{
        if(cancelled)throw new InterruptedIOException("Update cancelled");
    }
    void release(HttpURLConnection connection){
        synchronized(this){if(active==connection)active=null;}
        connection.disconnect();
    }
    static boolean trusted(URL url){
        String host=url.getHost();
        return url.getProtocol().equals("https")&&url.getUserInfo()==null&&url.getPort()==-1
            &&(host.equals("api.github.com")||host.equals("github.com")
            ||host.equals("release-assets.githubusercontent.com")||host.equals("objects.githubusercontent.com"));
    }
    HttpURLConnection connect(String address)throws IOException{
        URL url=new URL(address);
        for(int hop=0;hop<6;hop++){
            check();
            if(!trusted(url))throw new IOException("Untrusted download host");
            HttpURLConnection connection=factory.open(url);
            boolean returned=false;
            try{
                synchronized(this){check();active=connection;}
                connection.setInstanceFollowRedirects(false);
                connection.setConnectTimeout(15000);connection.setReadTimeout(20000);
                connection.setRequestProperty("User-Agent","Termux-Jobs-Updater");
                connection.setRequestProperty("Accept",url.getHost().equals("api.github.com")?"application/vnd.github+json":"application/octet-stream");
                check();int status=connection.getResponseCode();check();
                if(status>=300&&status<400){
                    String location=connection.getHeaderField("Location");
                    if(location==null)throw new IOException("Missing redirect");
                    url=new URL(url,location);
                }else{
                    if(status!=200)throw new IOException("GitHub HTTP "+status);
                    returned=true;return connection;
                }
            }finally{if(!returned)release(connection);}
        }
        throw new IOException("Too many redirects");
    }
}
