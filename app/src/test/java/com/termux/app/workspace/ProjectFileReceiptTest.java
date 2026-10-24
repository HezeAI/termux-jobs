package com.termux.app.workspace;

import org.junit.Test;
import java.io.*;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

public class ProjectFileReceiptTest {
    private static final String ID="0123456789abcdef0123456789abcdef";
    private void write(ProjectFileReceipt receipt,String text)throws IOException{receipt.write(text.getBytes(StandardCharsets.UTF_8));}
    private void incomplete(ProjectFileReceipt receipt)throws Exception{
        try{receipt.requireComplete();fail("unconfirmed operation reported complete");}catch(IOException expected){assertEquals("TRANSFER_NOT_CONFIRMED",expected.getMessage());}
    }
    @Test public void fragmentedOrderedRecordsRequireFinalReceipt()throws Exception{
        ProjectFileReceipt receipt=new ProjectFileReceipt(ID,2);
        String text=receipt.progress(1)+"\n"+receipt.progress(2)+"\n";
        for(byte b:text.getBytes(StandardCharsets.UTF_8))receipt.write(b);
        assertEquals(2,receipt.acknowledged());incomplete(receipt);write(receipt,receipt.completion()+"\n");receipt.requireComplete();
    }
    @Test public void legacyAndForeignRecordsDoNotAcknowledgeThisInvocation()throws Exception{
        ProjectFileReceipt receipt=new ProjectFileReceipt(ID,1),foreign=new ProjectFileReceipt("11111111111111111111111111111111",1);
        write(receipt,"DONE\nUNDONE\n"+foreign.progress(1)+"\n"+foreign.completion()+"\n");assertEquals(0,receipt.acknowledged());incomplete(receipt);
    }
    @Test public void truncatedCompletionIsNeverAccepted()throws Exception{
        ProjectFileReceipt template=new ProjectFileReceipt(ID,1);String end=template.completion()+"\n";
        for(int cut=0;cut<end.length();cut++){
            ProjectFileReceipt receipt=new ProjectFileReceipt(ID,1);write(receipt,receipt.progress(1)+"\n"+end.substring(0,cut));assertEquals(1,receipt.acknowledged());incomplete(receipt);
        }
    }
    @Test public void truncatedProgressDoesNotIncreaseLowerBound()throws Exception{
        ProjectFileReceipt template=new ProjectFileReceipt(ID,1);String line=template.progress(1)+"\n";
        for(int cut=0;cut<line.length();cut++){ProjectFileReceipt receipt=new ProjectFileReceipt(ID,1);write(receipt,line.substring(0,cut));assertEquals(0,receipt.acknowledged());incomplete(receipt);}
    }
    @Test public void duplicateReorderedAndPrematureCompletionStayInvalid()throws Exception{
        ProjectFileReceipt template=new ProjectFileReceipt(ID,2);
        for(String broken:new String[]{template.progress(1)+"\n"+template.progress(1),template.progress(2),template.completion()}){
            ProjectFileReceipt receipt=new ProjectFileReceipt(ID,2);write(receipt,broken+"\n"+template.progress(1)+"\n"+template.progress(2)+"\n"+template.completion()+"\n");incomplete(receipt);assertTrue(receipt.acknowledged()<=1);
        }
    }
    @Test public void repeatedCompletionOrOwnTrailingFragmentInvalidatesReceipt()throws Exception{
        for(String tail:new String[]{"full","fragment"}){
            ProjectFileReceipt receipt=new ProjectFileReceipt(ID,1);write(receipt,receipt.progress(1)+"\n"+receipt.completion()+"\n"+receipt.completion()+(tail.equals("full")?"\n":""));incomplete(receipt);
        }
    }
    @Test public void unrelatedDiagnosticsAreIgnoredWithinBound()throws Exception{
        ProjectFileReceipt receipt=new ProjectFileReceipt(ID,1);write(receipt,"banner\nфайл DONE\n"+receipt.progress(1)+"\nnotice\n"+receipt.completion()+"\n");receipt.requireComplete();assertEquals(1,receipt.acknowledged());
    }
    @Test public void oversizedOutputCannotAllocateWithoutBound()throws Exception{
        ProjectFileReceipt receipt=new ProjectFileReceipt(ID,1);receipt.write(new byte[ProjectFileReceipt.MAX_OUTPUT]);
        try{receipt.write(1);fail();}catch(IOException expected){assertEquals("TRANSFER_OUTPUT_LIMIT",expected.getMessage());}
        assertEquals(0,receipt.acknowledged());incomplete(receipt);
    }
    @Test public void fiftyProgressRecordsFitAndRemainOrdered()throws Exception{
        ProjectFileReceipt receipt=new ProjectFileReceipt(ID,50);for(int i=1;i<=50;i++)write(receipt,receipt.progress(i)+"\n");write(receipt,receipt.completion()+"\n");receipt.requireComplete();assertEquals(50,receipt.acknowledged());
    }
    @Test public void invalidTokensCountsAndOrdinalsAreRejected()throws Exception{
        for(String id:new String[]{"bad",ID+"'",ID+"\n",ID.toUpperCase()})try{new ProjectFileReceipt(id,1);fail();}catch(IllegalArgumentException expected){}
        for(int count:new int[]{0,-1,51})try{new ProjectFileReceipt(ID,count);fail();}catch(IllegalArgumentException expected){}
        ProjectFileReceipt receipt=new ProjectFileReceipt(ID,1);for(int ordinal:new int[]{0,2})try{receipt.progress(ordinal);fail();}catch(IllegalArgumentException expected){}
    }
}
