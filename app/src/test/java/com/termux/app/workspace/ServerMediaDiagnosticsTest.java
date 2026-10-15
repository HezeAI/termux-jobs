package com.termux.app.workspace;

import android.app.Application;
import androidx.media3.common.PlaybackException;
import com.termux.R;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class ServerMediaDiagnosticsTest {
    @Test public void networkErrorReportsCodeWithoutLeakingCauseMessageOrUrl(){
        String secret="http://127.0.0.1:1234/"+String.join("",java.util.Collections.nCopies(64,"a"));PlaybackException e=new PlaybackException("PRIVATE "+secret,new java.net.ConnectException(secret),PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED);String text=ServerMediaDiagnostics.describe(e,true);assertTrue(text.contains("SSH"));assertTrue(text.contains("ERROR_CODE_IO_NETWORK_CONNECTION_FAILED"));assertFalse(text.contains(secret));assertFalse(text.contains("PRIVATE"));
    }
    @Test public void formatAndTimeoutErrorsGiveDifferentGuidance(){
        String timeout=ServerMediaDiagnostics.describe(new PlaybackException("secret",new java.net.SocketTimeoutException("secret"),PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT),false);assertTrue(timeout.contains("timed out"));assertFalse(timeout.contains("secret"));
        String format=ServerMediaDiagnostics.describe(new PlaybackException("secret",null,PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED),true);assertTrue(format.contains("Декодер"));assertFalse(format.contains("secret"));
    }
    @Test public void networkPolicyAllowsOnlyExactLoopbackAndDeniesOtherHttp()throws Exception{
        android.content.res.XmlResourceParser parser=RuntimeEnvironment.application.getResources().getXml(R.xml.workspace_network_security);int base=0,domainConfig=0,domains=0;
        try{for(int event=parser.getEventType();event!=org.xmlpull.v1.XmlPullParser.END_DOCUMENT;event=parser.next()){if(event!=org.xmlpull.v1.XmlPullParser.START_TAG)continue;String tag=parser.getName();if(tag.equals("base-config")){base++;assertFalse(parser.getAttributeBooleanValue(null,"cleartextTrafficPermitted",true));}if(tag.equals("domain-config")){domainConfig++;assertTrue(parser.getAttributeBooleanValue(null,"cleartextTrafficPermitted",false));}if(tag.equals("domain")){domains++;assertFalse(parser.getAttributeBooleanValue(null,"includeSubdomains",true));assertEquals("127.0.0.1",parser.nextText().trim());}}}finally{parser.close();}
        assertEquals(1,base);assertEquals(1,domainConfig);assertEquals(1,domains);
        String manifest=new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("src/main/AndroidManifest.xml")),"UTF-8");assertTrue(manifest.contains("android:networkSecurityConfig=\"@xml/workspace_network_security\""));assertFalse(manifest.contains("android:usesCleartextTraffic=\"true\""));
    }
}
