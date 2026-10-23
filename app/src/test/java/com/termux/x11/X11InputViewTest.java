package com.termux.x11;
import android.view.*;
import android.view.inputmethod.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(manifest=Config.NONE,sdk=28)
public class X11InputViewTest {
 private final List<String> events=new ArrayList<>();
 private X11InputView view(){return new X11InputView(RuntimeEnvironment.application,new X11InputView.Sink(){public void text(String text){events.add(text);}public void key(int code,boolean down){events.add(code+":"+down);}});}
 @Test public void disabledEditorHasNoConnectionAndOldConnectionsCannotTypeAfterRearming(){X11InputView v=view();assertFalse(v.onCheckIsTextEditor());assertNull(v.onCreateInputConnection(new EditorInfo()));v.active(true);InputConnection old=v.onCreateInputConnection(new EditorInfo());old.commitText("hello",1);v.active(false);v.active(true);old.commitText("stale",1);old.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_ENTER));v.onCreateInputConnection(new EditorInfo()).commitText("новое",1);assertEquals(Arrays.asList("hello","новое"),events);}
 @Test public void compositionIsVisibleImmediatelyAndCommitDoesNotRepeatIt(){X11InputView v=view();v.active(true);EditorInfo info=new EditorInfo();InputConnection c=v.onCreateInputConnection(info);c.setComposingText("пр",1);c.setComposingText("привет",1);assertEquals(Arrays.asList("пр","ивет"),events);c.commitText("привет",1);c.finishComposingText();assertEquals(Arrays.asList("пр","ивет"),events);assertTrue((info.imeOptions&EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING)!=0);}
 @Test public void deletionAndEnterAreForwardedButBackRemainsAndroidOwned(){X11InputView v=view();v.active(true);InputConnection c=v.onCreateInputConnection(new EditorInfo());c.deleteSurroundingText(1,1);c.performEditorAction(EditorInfo.IME_ACTION_DONE);assertEquals(Arrays.asList("67:true","67:false","112:true","112:false","66:true","66:false"),events);assertFalse(c.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_BACK)));assertEquals(6,events.size());v.active(false);c.deleteSurroundingText(1,1);assertEquals(6,events.size());}
 @Test public void closingEditorReleasesHeldModifiersBeforeRejectingOldConnection(){X11InputView v=view();v.active(true);InputConnection c=v.onCreateInputConnection(new EditorInfo());c.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_SHIFT_LEFT));v.active(false);c.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_SHIFT_LEFT));assertEquals(Arrays.asList("59:true","59:false"),events);}
 @Test public void lateCompositionFromClosedEditorIsIgnoredAndDeletionBounded(){X11InputView v=view();v.active(true);InputConnection c=v.onCreateInputConnection(new EditorInfo());c.setComposingText("stale",1);assertEquals(Arrays.asList("stale"),events);events.clear();v.active(false);c.setComposingText("late",1);c.finishComposingText();assertTrue(events.isEmpty());v.active(true);v.onCreateInputConnection(new EditorInfo()).deleteSurroundingText(Integer.MAX_VALUE,-1);assertEquals(512,events.size());}

 private X11InputView screen(StringBuilder out){return new X11InputView(RuntimeEnvironment.application,new X11InputView.Sink(){public void text(String text){out.append(text);}public void key(int code,boolean down){if(down&&code==KeyEvent.KEYCODE_DEL&&out.length()>0)out.setLength(out.offsetByCodePoints(out.length(),-1));}});}
 @Test public void correctionReplacesOnlyCurrentWordAndFinishNeverReplaysIt(){StringBuilder out=new StringBuilder();X11InputView v=screen(out);v.active(true);InputConnection c=v.onCreateInputConnection(new EditorInfo());c.commitText("prefix ",1);c.setComposingText("hello",1);assertEquals("prefix hello",out.toString());c.setComposingText("hallo",1);assertEquals("prefix hallo",out.toString());c.commitText("Hello",1);c.finishComposingText();assertEquals("prefix Hello",out.toString());c.commitText(" world",1);assertEquals("prefix Hello world",out.toString());}
 @Test public void deletionKeepsCompositionInSyncAndDoesNotDuplicateOnCommit(){StringBuilder out=new StringBuilder();X11InputView v=screen(out);v.active(true);InputConnection c=v.onCreateInputConnection(new EditorInfo());c.setComposingText("abcd",1);c.deleteSurroundingText(3,0);assertEquals("a",out.toString());c.commitText("a",1);c.finishComposingText();assertEquals("a",out.toString());c.setComposingText("bc",1);c.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DEL));c.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_DEL));c.commitText("b",1);assertEquals("ab",out.toString());}
 @Test public void surrogatePairsAreReplacedAsWholeCharacters(){StringBuilder out=new StringBuilder();X11InputView v=screen(out);v.active(true);InputConnection c=v.onCreateInputConnection(new EditorInfo());c.setComposingText("😀x",1);assertEquals("😀x",out.toString());c.setComposingText("😁x",1);assertEquals("😁x",out.toString());c.deleteSurroundingText(2,0);assertEquals("",out.toString());c.finishComposingText();assertEquals("",out.toString());}
 @Test public void enterFinishesCurrentCompositionBeforeNextWord(){StringBuilder out=new StringBuilder();X11InputView v=screen(out);v.active(true);InputConnection c=v.onCreateInputConnection(new EditorInfo());c.setComposingText("one",1);c.performEditorAction(EditorInfo.IME_ACTION_DONE);c.setComposingText("two",1);c.finishComposingText();assertEquals("onetwo",out.toString());}
 @Test public void newDesktopFieldInvalidatesOldCompositionWithoutEditingTheNewField(){StringBuilder out=new StringBuilder();X11InputView v=screen(out);v.active(true);InputConnection old=v.onCreateInputConnection(new EditorInfo());old.setComposingText("previous",1);out.setLength(0);out.append("target:");v.restart();old.commitText("previous fixed",1);old.finishComposingText();InputConnection fresh=v.onCreateInputConnection(new EditorInfo());fresh.setComposingText("новое",1);fresh.finishComposingText();assertEquals("target:новое",out.toString());}
 @Test public void imeCommittedNewlineTabAndCrlfUseKeysRatherThanUnicodeControlText(){
  X11InputView v=view();v.active(true);InputConnection c=v.onCreateInputConnection(new EditorInfo());
  c.setComposingText("hello",1);c.commitText("hello\n",1);c.commitText("мир\r\nnext\tend",1);c.finishComposingText();
  assertEquals(Arrays.asList("hello","66:true","66:false","мир","66:true","66:false","next","61:true","61:false","end"),events);
 }
 @Test public void physicalEnterStillProducesOnlyOneDownUpPair(){
  X11InputView v=view();v.active(true);InputConnection c=v.onCreateInputConnection(new EditorInfo());c.setComposingText("abc",1);
  c.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_ENTER));c.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_ENTER));c.finishComposingText();
  assertEquals(Arrays.asList("abc","66:true","66:false"),events);
 }

}
