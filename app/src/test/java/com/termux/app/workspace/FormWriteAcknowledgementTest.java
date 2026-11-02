package com.termux.app.workspace;

import android.app.AlertDialog;
import android.util.AtomicFile;
import android.view.*;
import android.widget.EditText;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import org.junit.Test;
import org.robolectric.shadows.ShadowAlertDialog;
import static org.junit.Assert.*;

/** Uses the actual form and FIFO writer, with an isolated failing AtomicFile. */
public class FormWriteAcknowledgementTest extends AgentSettingsPresentationTest {
    void fields(View view,List<EditText> result){if(view instanceof EditText)result.add((EditText)view);if(view instanceof ViewGroup)for(int n=0;n<((ViewGroup)view).getChildCount();n++)fields(((ViewGroup)view).getChildAt(n),result);}
    @Test public void failedCommandAndNoteSaveKeepsFormAndRetryDoesNotDuplicate()throws Exception{
        Field f=WorkspaceController.class.getDeclaredField("store");f.setAccessible(true);WorkspaceStore original=(WorkspaceStore)f.get(controller);original.close();
        boolean[] broken={true};File target=new File(activity.getFilesDir(),"isolated-form-"+UUID.randomUUID()+".json");
        WorkspaceStore store=new WorkspaceStore(activity,x->{},new AtomicFile(target){@Override public FileOutputStream startWrite()throws IOException{if(broken[0])throw new IOException("fixture disk full");return super.startWrite();}});f.set(controller,store);
        try{for(boolean note:new boolean[]{false,true}){
            broken[0]=true;Method edit=WorkspaceController.class.getDeclaredMethod(note?"editNote":"editCommand",note?new Class[]{WorkspaceStore.Note.class}:new Class[]{WorkspaceStore.Command.class,String.class});edit.setAccessible(true);if(note)edit.invoke(controller,new Object[]{null});else edit.invoke(controller,null,"");idle();editor=ShadowAlertDialog.getLatestAlertDialog();
            List<EditText> inputs=new ArrayList<>();fields(editor.getWindow().getDecorView(),inputs);inputs.get(0).setText("keep title");inputs.get(1).setText("keep exact draft\nsecond line");
            editor.getButton(-1).performClick();store.awaitWrites();idle();assertTrue(editor.isShowing());assertEquals("keep exact draft\nsecond line",inputs.get(1).getText().toString());assertTrue(editor.getButton(-1).isEnabled());
            ShadowAlertDialog.getLatestAlertDialog().getButton(-1).performClick();broken[0]=false;editor.getButton(-1).performClick();store.awaitWrites();idle();assertFalse(editor.isShowing());assertEquals(1,note?store.notes.size():store.commands.size());
        }}finally{store.close();}
    }
}
