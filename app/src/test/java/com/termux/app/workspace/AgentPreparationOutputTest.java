package com.termux.app.workspace;

import org.junit.Test;
import java.io.*;
import java.util.*;
import static org.junit.Assert.*;

public class AgentPreparationOutputTest {
    @Test public void drainsLargeOutputAndRejectsPartialAndOversizedRecords()throws Exception{
        String input="STAGE:connecting-image-download\r\n"+String.join("",Collections.nCopies(100000,"SECRET"))+"\nPREPARATION:checking-cli\nERROR:IMAGE_DOWNLOAD_NETWORK";
        List<String> lines=new ArrayList<>();AgentPreparationOutput.drain(new StringReader(input),lines::add);
        assertEquals(Arrays.asList("STAGE:connecting-image-download","PREPARATION:checking-cli"),lines);
    }
    @Test public void splitDelimitersAndMultilineSecretsCannotEnterInventory()throws Exception{
        String text="-----BEGIN PRIVATE KEY-----\nSECRET_BODY\n-----END PRIVATE KEY-----\nREADY:node\r\nCHECK:done\nERROR:secret";
        AgentPreparation.Plan p=new AgentPreparation.Plan();
        Reader oneCharacter=new FilterReader(new StringReader(text)){public int read(char[] b,int o,int n)throws IOException{return super.read(b,o,Math.min(1,n));}};
        AgentPreparationOutput.drain(oneCharacter,p::record);p.finish(0);
        assertTrue(p.ready());assertEquals(Collections.singleton("node"),p.ready);assertTrue(p.missing.isEmpty());assertEquals("",p.error);
    }
    @Test public void unknownErrorsAndStagesNeverEchoRawInput(){
        assertEquals("",AgentPreparationOutput.stage("STAGE:secret"));assertEquals("",AgentPreparationOutput.error("ERROR:secret"));
        assertEquals("IMAGE_DOWNLOAD_NETWORK",AgentPreparationOutput.error("ERROR:IMAGE_DOWNLOAD_NETWORK"));
        assertFalse(AgentPreparationOutput.description("secret",false).contains("secret"));
        assertFalse(AgentPreparationOutput.description("secret",true).contains("secret"));
    }
    @Test public void readFailureIsPropagatedWithoutCreatingAReport()throws Exception{
        Reader broken=new Reader(){public int read(char[] c,int o,int n)throws IOException{throw new IOException("SECRET_ERROR");}public void close(){}};
        try{AgentPreparationOutput.drain(broken,line->fail("no record"));fail("read must fail");}catch(IOException expected){}
    }
}
