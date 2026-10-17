package com.termux.app.workspace;
import android.app.Application;
import android.text.Spanned;
import android.text.style.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class LocalAiPresentationTest {
 @Test public void emptyThinkingIsHidden(){LocalAiText.Parts p=LocalAiText.split("<think>\n</think>\n**Answer**");assertEquals("**Answer**",p.answer);assertEquals("",p.thinking);assertFalse(p.pending);}
 @Test public void thinkingIsPreservedSeparately(){String raw="<think>проверяю</think>Ответ";LocalAiText.Parts p=LocalAiText.split(raw);assertEquals("проверяю",p.thinking);assertEquals("Ответ",p.answer);assertEquals("<think>проверяю</think>Ответ",raw);}
 @Test public void streamingDoesNotLeakTagsOrReasoning(){assertEquals("",LocalAiText.split("<thi").answer);LocalAiText.Parts p=LocalAiText.split("<think>secret</thi");assertEquals("",p.answer);assertEquals("secret",p.thinking);assertTrue(p.pending);assertEquals("answer",LocalAiText.split("<think>x</think>answer<thi").answer);}
 @Test public void multipleThinkingBlocksAndUnfinishedBlock(){LocalAiText.Parts p=LocalAiText.split("a<think>one</think>b<think>two");assertEquals("ab",p.answer);assertEquals("one\ntwo",p.thinking);assertTrue(p.pending);}
 @Test public void userTextIsNotParsedByThinkRenderer(){LocalAiText.Parts p=new LocalAiText.Parts("<think>literal</think>","",false);assertEquals("<think>literal</think>",p.answer);}
 @Test public void markdownIsOfflineAndFormatsCodeBoldAndSafeLinks(){Spanned out=(Spanned)LocalAiMarkdown.render(RuntimeEnvironment.application,"# Title\n**Bold**\n```sh\necho hi\n```\n[x](https://example.org)\n[y](javascript:alert(1))\n<script>literal</script>");assertTrue(out.toString().contains("Bold"));assertFalse(out.toString().contains("**"));assertTrue(out.getSpans(0,out.length(),StyleSpan.class).length>=2);assertTrue(out.getSpans(0,out.length(),TypefaceSpan.class).length>=1);URLSpan[] urls=out.getSpans(0,out.length(),URLSpan.class);assertEquals(1,urls.length);assertEquals("https://example.org",urls[0].getURL());assertTrue(out.toString().contains("<script>literal</script>"));}
}
