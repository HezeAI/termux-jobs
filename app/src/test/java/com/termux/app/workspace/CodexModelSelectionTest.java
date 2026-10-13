package com.termux.app.workspace;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class CodexModelSelectionTest {
    private static class Fake implements CodexModelSelection.Terminal {
        String screen="› Ask Codex to do anything";boolean empty=true;List<String> keys=new ArrayList<>();
        public String screen(){return screen;} public boolean menuOpen(){return screen.contains("Select ") || screen.contains("Advanced Reasoning");} public boolean emptyPrompt(){return empty;} public void write(String value){keys.add(value);}
    }
    @Test public void selectsOnlyRecognizedRowsAndRequiresFreshNativeAcknowledgement() {
        Fake f=new Fake();CodexModelSelection s=new CodexModelSelection("gpt-6-astra","GPT-6 Astra","high");
        s.step(f,0);assertEquals("/model\r",f.keys.get(0));
        f.screen="Select Model and Effort\n› 1. GPT-6.1-Sol (current)  Workhorse\n  2. GPT-6-Astra  Frontier";
        s.step(f,200);assertEquals("\u001b[B",f.keys.get(1));
        f.screen="Select Model and Effort\n  1. GPT-6.1-Sol (current)  Workhorse\n› 2. GPT-6-Astra  Frontier";
        s.step(f,400);assertEquals("\r",f.keys.get(2));
        f.screen="Select Reasoning Level for GPT-6-Astra\n  1. Low (default)  Fast\n› 2. High  Greater depth\nenter default · s session · esc back";
        s.step(f,600);assertEquals("s",f.keys.get(3));assertFalse(s.confirmed);
        f.screen="Model changed to gpt-6-astra high\n› Ask Codex to do anything";s.step(f,800);
        assertTrue(s.confirmed);assertTrue(s.finished);
    }
    @Test public void draftAndUnknownPromptAreNeverOverwritten() {
        Fake f=new Fake();f.empty=false;f.screen="› my unsent draft";
        CodexModelSelection s=new CodexModelSelection("gpt-6-astra","GPT-6 Astra","high");s.step(f,0);
        assertTrue(s.finished);assertFalse(s.confirmed);assertTrue(f.keys.isEmpty());
    }
    @Test public void menuMismatchAndMissingSessionActionDoNotBlindlyAccept() {
        Fake f=new Fake();f.screen="Select Model and Effort\n› 1. Unrelated model";
        CodexModelSelection s=new CodexModelSelection("gpt-6-astra","GPT-6 Astra","high");s.step(f,0);for(int i=1;i<23;i++)s.step(f,i*200);
        assertTrue(s.finished);assertFalse(f.keys.contains("\r"));
        Fake g=new Fake();g.screen="Select Model and Effort\n› 1. GPT-6-Astra  Frontier";
        CodexModelSelection t=new CodexModelSelection("gpt-6-astra","GPT-6 Astra","high");t.step(g,0);t.step(g,200);
        g.screen="Select Reasoning Level for GPT-6-Astra\n› 1. High  Reasoning\nenter default · esc back";t.step(g,400);
        assertTrue(t.finished);assertFalse(t.confirmed);assertEquals(1,g.keys.size());
    }
    @Test public void oldSameTextIsNotProofOfAnAppliedChange() {
        Fake f=new Fake();f.screen="Model changed to gpt-6-astra high\nSelect Model and Effort\n› 1. GPT-6-Astra  Frontier";
        CodexModelSelection s=new CodexModelSelection("gpt-6-astra","GPT-6 Astra","high");s.step(f,0);s.step(f,200);
        f.screen="Select Reasoning Level for GPT-6-Astra\n› 1. High  Reasoning\nenter default · s session · esc back";s.step(f,400);
        f.screen="Model changed to gpt-6-astra high";s.step(f,600);assertFalse(s.confirmed);s.step(f,16000);assertTrue(s.finished);assertFalse(s.confirmed);
    }
    @Test public void advancedEffortUsesSecondPickerInsteadOfSendingArbitraryCommands() {
        Fake f=new Fake();f.screen="Select Model and Effort\n› 1. GPT-6-Astra  Frontier";
        CodexModelSelection s=new CodexModelSelection("gpt-6-astra","GPT-6 Astra","max");s.step(f,0);s.step(f,200);
        f.screen="Select Reasoning Level for GPT-6-Astra\n  1. Low (default)  Fast\n› 2. More reasoning…  Max and Ultra consume limits faster";s.step(f,400);
        f.screen="Advanced Reasoning for GPT-6-Astra\n› 1. Max  Deep\n  2. Ultra  More\nenter default · s session · esc back";s.step(f,600);
        assertEquals("s",f.keys.get(f.keys.size()-1));
        f.screen="Model changed to gpt-6-astra max";s.step(f,800);assertTrue(s.confirmed);
    }
}
