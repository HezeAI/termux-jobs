package com.termux.x11;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class X11FocusStreamTest {
 @Test public void fragmentedFramesOnlyRecognizeAuthenticatedFocusMetadata(){List<String> seen=new ArrayList<>();X11FocusStream f=new X11FocusStream("token",(key,in)->seen.add(key+":"+in));f.feed("JOBS_X11_FOCUS_V1 tok");f.feed("en FocusIn :1.5 /input/1\nJOBS_X11_FOCUS_V1 token FocusOut :1.5 /input/1\n");assertEquals(Arrays.asList(":1.5/input/1:true",":1.5/input/1:false"),seen);}
 @Test public void textArbitraryOperationsAndWrongTokensAreIgnored(){List<String> seen=new ArrayList<>();X11FocusStream f=new X11FocusStream("token",(key,in)->seen.add(key));for(String line:new String[]{"user text", "JOBS_X11_FOCUS_V1 other FocusIn :1.5 /input/1", "JOBS_X11_FOCUS_V1 token CommitString :1.5 /input/1", "JOBS_X11_FOCUS_V1 token FocusIn ../bad /input/1", "JOBS_X11_FOCUS_V1 token FocusIn :1.5 /input/1 extra"})f.feed(line+"\n");assertTrue(seen.isEmpty());}
 @Test public void startupMarkersRequireExactCurrentTokenAndRecoverFromNoise(){List<Boolean> ready=new ArrayList<>();X11FocusStream f=new X11FocusStream("nonce",(key,in)->{},ready::add);f.feed("JOBS_X11_DESKTOP_READY wrong\nJOBS_X11_DESKTOP_READY nonce extra\nJOBS_X11_DESKTOP_RE");f.feed("ADY nonce\nJOBS_X11_DESKTOP_TIMEOUT nonce\n");assertEquals(Arrays.asList(true,false),ready);}
 @Test public void oversizedLineIsDiscardedAndParserRecovers(){List<Boolean> seen=new ArrayList<>();X11FocusStream f=new X11FocusStream("t",(key,in)->seen.add(in));f.feed(new String(new char[800]).replace('\0','x')+"JOBS_X11_FOCUS_V1 t FocusIn :1.1 /ic/1\n");f.feed("JOBS_X11_FOCUS_V1 t DestroyIC :1.1 /ic/1\n");assertEquals(Arrays.asList(false),seen);}
 @Test public void retiredKeyboardFramesHaveNoEffect(){List<Boolean> events=new ArrayList<>();X11FocusStream f=new X11FocusStream("abc",(c,in)->events.add(in),events::add);f.feed("JOBS_X11_TOUCH_KEYBOARD abc\n");assertTrue(events.isEmpty());}
}
