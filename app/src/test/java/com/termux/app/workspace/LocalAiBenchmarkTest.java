package com.termux.app.workspace;
import android.app.Application;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class LocalAiBenchmarkTest {
 static JSONObject row(boolean complete)throws Exception{return new JSONObject().put("trial",1).put("trials",8).put("input",512).put("processed",complete?512:256).put("count",256).put("threads",4).put("batch",256).put("ubatch",128).put("context",8192).put("decodeNs",1000000000L).put("cpuNs",3000000000L).put("batchNs",500000000L).put("batchCpuNs",1500000000L).put("elapsedNs",1100000000L).put("complete",complete);}
 @Test public void reportsActualLengthsCpuAndWallWithoutInventingTokenGeneration()throws Exception{LocalAiBenchmark b=new LocalAiBenchmark();b.begin("qwen35-2b","CPU / optimized / threads=4",0);b.accept(row(false).toString());b.accept(row(true).toString());assertEquals(2,b.batches.size());assertEquals(1,b.results.size());String report=b.report(false);assertTrue(report.contains("512 tok · threads 4"));assertTrue(report.contains("1000.00 / 1100.00 / 3000.00"));assertTrue(report.contains("512.0 tok/s"));assertTrue(report.contains("no history"));b.finish("completed","");assertTrue(b.report(false).contains("completed"));assertFalse(b.running);}
 @Test public void malformedAndNegativeMetadataDoNotReplaceValidProgress()throws Exception{LocalAiBenchmark b=new LocalAiBenchmark();b.begin("model","CPU",1);b.accept(row(false).toString());for(String key:new String[]{"processed","count","cpuNs","batchNs","elapsedNs"}){JSONObject bad=row(false).put(key,-1);b.accept(bad.toString());}b.accept(row(false).put("threads",9).toString());b.accept(row(true).put("processed",256).toString());assertEquals(1,b.batches.size());assertEquals(256,b.current.getInt("processed"));assertEquals(0,b.results.size());}
 @Test public void cancellationKeepsPartialResultsAndNewTestResetsOnlyBenchmark()throws Exception{LocalAiBenchmark b=new LocalAiBenchmark();b.begin("model","CPU",2);b.accept(row(true).toString());b.requestStop();assertTrue(b.summary(false).contains("stopping"));b.finish("stopped","IllegalStateException");assertEquals(1,b.results.size());assertTrue(b.report(true).contains("остановлен"));assertFalse(b.running);b.accept(row(true).toString());assertEquals(1,b.batches.size());b.begin("next-model","CPU",0);assertEquals(0,b.results.size());assertEquals(0,b.batches.size());assertNull(b.current);assertFalse(b.stopRequested);}
 @Test public void modesAreExplicitAndInvalidModeIsRejected(){assertTrue(LocalAiBenchmark.name(1,false).contains("2/4/6/8"));assertTrue(LocalAiBenchmark.name(2,true).contains("512/256"));try{new LocalAiBenchmark().begin("m","cpu",3);fail();}catch(IllegalArgumentException expected){}}
}
