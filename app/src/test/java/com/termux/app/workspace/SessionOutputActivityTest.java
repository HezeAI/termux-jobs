package com.termux.app.workspace;
import org.junit.Test;
import static org.junit.Assert.*;
public class SessionOutputActivityTest {
    @Test public void scrollingClearsOldOutputAndIgnoresRedrawWithoutDelayedFlash(){
        SessionOutputActivity s=new SessionOutputActivity();s.output("a",100);assertTrue(s.active("a",101));
        s.viewport(200);assertFalse(s.active("a",201));s.output("a",300);assertFalse(s.active("a",1800));
        s.output("a",1801);assertTrue(s.active("a",1802));assertFalse(s.active("a",3401));
    }
    @Test public void repeatedScrollExtendsSuppressionAndHandlesStayIndependent(){
        SessionOutputActivity s=new SessionOutputActivity();s.viewport(100);s.viewport(1600);s.output("a",1800);assertFalse(s.active("a",3300));
        s.output("b",3300);assertTrue(s.active("b",3301));assertFalse(s.active("a",3301));
    }
}
