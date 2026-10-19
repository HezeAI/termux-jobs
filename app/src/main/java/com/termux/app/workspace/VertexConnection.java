package com.termux.app.workspace;

import android.app.*;
import android.content.*;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.function.*;

/** Non-secret Vertex defaults and a per-run, data-only startup response. */
final class VertexConnection {
    static final String PREFS="workspace-vertex-connection";
    static boolean selected(WorkspaceStore.Session p){return p!=null&&p.geminiAgent&&p.geminiAuth.equals("vertex");}
    static void applyDefaults(Context c,WorkspaceStore.Session p){
        if(!selected(p)||!p.geminiCloudProject.isEmpty())return;
        SharedPreferences prefs=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        String project=prefs.getString("project",""),location=prefs.getString("location","global");
        if(!project.isEmpty()&&GeminiCli.validCloudProject(project)&&GeminiCli.validCloudLocation(location)){p.geminiCloudProject=project;p.geminiCloudLocation=location;}
    }
    static boolean remember(Context c,String project,String location){
        if(project.isEmpty()||!GeminiCli.validCloudProject(project)||location.isEmpty()||!GeminiCli.validCloudLocation(location))return false;
        return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString("project",project).putString("location",location).commit();
    }
    static File file(Context c,WorkspaceStore.Session p,String suffix){
        if(!GeminiCli.validThread(p.geminiSocketId))throw new IllegalArgumentException("Invalid Vertex run");
        return new File(GeminiCli.directory(c.getFilesDir()),p.geminiSocketId+".vertex-"+suffix);
    }
    static boolean pending(Context c,WorkspaceStore.Session p){return selected(p)&&file(c,p,"request").isFile();}
    static String exports(WorkspaceStore.Session p){
        return "export JOBS_VERTEX_REQUEST="+SessionShell.quote(GeminiCli.GUEST+"/"+p.geminiSocketId+".vertex-request")+"; export JOBS_VERTEX_RESPONSE="+SessionShell.quote(GeminiCli.GUEST+"/"+p.geminiSocketId+".vertex-response")+"; ";
    }
    static void respond(Context c,WorkspaceStore.Session p,String project,String location)throws IOException{
        if(!selected(p)||!pending(c,p))throw new IOException("Vertex setup is no longer waiting");
        if(project.isEmpty()||!GeminiCli.validCloudProject(project)||location.isEmpty()||!GeminiCli.validCloudLocation(location))throw new IOException("Invalid Vertex metadata");
        File target=file(c,p,"response"),temp=File.createTempFile("vertex-",".tmp",target.getParentFile());
        try{if(!temp.setReadable(false,false)||!temp.setWritable(false,false)||!temp.setReadable(true,true)||!temp.setWritable(true,true))throw new IOException("Cannot protect Vertex metadata");Files.write(temp.toPath(),(project+"\n"+location+"\n").getBytes(StandardCharsets.UTF_8));if(!remember(c,project,location))throw new IOException("Cannot save Vertex settings");if(!temp.renameTo(target))throw new IOException("Cannot deliver Vertex settings");}finally{temp.delete();}
    }
    static AlertDialog prompt(Activity a,WorkspaceStore.Session p,BooleanSupplier active,Runnable completed){
        LinearLayout box=new LinearLayout(a);box.setOrientation(LinearLayout.VERTICAL);int pad=(int)(20*a.getResources().getDisplayMetrics().density);box.setPadding(pad,pad,pad,pad);
        TextView help=new TextView(a);help.setText(tr(a,"Учётные данные Google Cloud найдены. Для подключения Vertex AI укажите облачный проект. Это не рабочая папка и не код авторизации. Параметры сохранятся отдельно для следующих подключений Vertex; чат и пресет не создаются.","Google Cloud credentials were found. Enter a cloud project for Vertex AI. This is not a working folder or authorization code. Settings are saved separately for future Vertex connections; no chat or preset is created."));box.addView(help);
        EditText project=new EditText(a);project.setSingleLine(true);project.setHint(tr(a,"ID проекта Google Cloud","Google Cloud project ID"));project.setTag("vertex-setup-project");project.setText(p.geminiCloudProject);box.addView(project);
        String[] location={p.geminiCloudLocation.isEmpty()?"global":p.geminiCloudLocation};Button region=new Button(a);region.setText(tr(a,"Регион: ","Location: ")+location[0]);region.setOnClickListener(v->GeminiAuthUi.location(a,location[0],value->{location[0]=value;region.setText(tr(a,"Регион: ","Location: ")+value);}));box.addView(region);
        AlertDialog dialog=WorkspaceDialogs.builder(a).setTitle(tr(a,"Подключение Vertex AI","Vertex AI connection")).setView(box).setPositiveButton(tr(a,"Сохранить и продолжить","Save and continue"),null).setNegativeButton(tr(a,"Назад","Back"),null).create();dialog.setCanceledOnTouchOutside(false);
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{if(!active.getAsBoolean()||!pending(a,p)){dialog.dismiss();return;}String id=project.getText().toString().trim();if(id.isEmpty()||!GeminiCli.validCloudProject(id)){project.setError(tr(a,"Укажите ID проекта, например my-project-123","Enter a project ID such as my-project-123"));return;}try{respond(a,p,id,location[0]);dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);dialog.dismiss();completed.run();}catch(IOException e){project.setError(tr(a,"Не удалось сохранить параметры. Повторите.","Could not save settings. Try again."));}}));dialog.show();return dialog;
    }
    private static String tr(Context c,String ru,String en){return WorkspaceLanguage.code(c).equals("ru")?ru:en;}
}
