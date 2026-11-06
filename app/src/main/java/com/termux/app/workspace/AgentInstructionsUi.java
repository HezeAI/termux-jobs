package com.termux.app.workspace;

import android.app.AlertDialog;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.*;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import com.termux.app.TermuxActivity;
import org.json.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

/** General settings editor. Legacy sessions retain their runtime; the project folder is a suggestion. */
final class AgentInstructionsUi {
    static String label(android.content.Context c){return tr(c,"Инструкции агентов","Agent instructions");}
    static String tr(android.content.Context c,String ru,String en){return WorkspaceLanguage.code(c).equals("ru")?ru:en;}
    static String saved(android.content.Context c){return tr(c,"Сохранено. Выполняющийся запрос не изменяется. Общие правила могут примениться уже к следующему запросу. Для повторного чтения общих и проектных правил перезапустите агента с продолжением диалога.", "Saved. The running request is unchanged. Shared rules may apply to the next request. To reread both shared and project rules, restart the agent and resume the conversation.");}
    static String runtime(WorkspaceStore.Session p){if(p!=null){if(!AgentPreparation.agent(p).isEmpty()&&(AgentEnvironments.ubuntu(p.codexEnvironment)||p.codexEnvironment.equals("termux")))return p.codexEnvironment;String alias=WorkspaceEnvironments.alias(p);if(AgentEnvironments.ubuntu(alias))return alias;}return "ubuntu-ai";}
    static WorkspaceStore.Session fileProfile(String env){WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment=env.equals("termux")?"termux":"proot:"+env;p.codexEnvironment=env;return p;}
    static String runtimeNotice(android.content.Context c,String env){return tr(c,"Среда: ","Environment: ")+AgentEnvironments.label(env)+(env.equals("ubuntu-ai")?"":tr(c,". Сохранена среда старой сессии. Редактор работает с её файлами; инструкции Ubuntu AI не изменяются.",". The legacy session environment is retained. The editor uses its files; Ubuntu AI instructions are unchanged."));}
    static String suggested(WorkspaceStore.Session p){String env=runtime(p);String path=CodexProjects.directory(env,p!=null&&(!AgentPreparation.agent(p).isEmpty()||p.codexEnvironment.equals("ubuntu-ai")||WorkspaceEnvironments.alias(p).equals(env))?p.directory:env.equals("termux")?"~":"/root");return path.isEmpty()?"/root":path;}
    static void show(TermuxActivity a,WorkspaceStore.Session profile){Editor e=new Editor(a,suggested(profile),q->AgentInstructions.request(a,q));e.projectExplicit=false;e.selectedEnvironment=runtime(profile);if(profile!=null&&profile.claudeAgent){e.agent="claude";e.name="CLAUDE.md";}e.show();}
    static void showProject(TermuxActivity a,WorkspaceStore.Session profile){
        Editor e=new Editor(a,suggested(profile),q->AgentInstructions.request(a,q));e.scope="project";e.projectExplicit=false;e.selectedEnvironment=runtime(profile);
        if(profile!=null&&profile.claudeAgent){e.agent="claude";e.name="CLAUDE.md";}e.show();
    }
    static Editor sessionEditor(TermuxActivity a,WorkspaceStore.Session source,AgentInstructions.Backend backend){
        AgentSessionResourcesUi.Context context=AgentSessionResourcesUi.context(a,source);return context==null?null:context.editor(backend);
    }
    static final class Editor {
        final TermuxActivity activity;final AgentInstructions.Backend backend;
        final ExecutorService worker=Executors.newSingleThreadExecutor();final Handler handler=new Handler(Looper.getMainLooper());
        final String initialProject;String project,scope="general",name="AGENTS.md",agent="codex",selectedEnvironment="ubuntu-ai";JSONObject document;
        android.app.Dialog dialog;AgentManagerLayout frame;LinearLayout root,body,tabs,agents;TextView environmentLabel,runtimeLabel,projectCreate,projectTemplate;boolean projectExplicit,projectTemplateEditing;ImageButton refresh;EditText editor;TextView status,save,close;boolean busy;volatile boolean closed;int epoch;
        boolean createRequested,createReading,createGuard;String createContext="";AlertDialog creationDialog;String[] creationDraft;String creationDraftContext="";
        WorkspaceFileBrowser browser;WorkspaceLocalFilePicker importPicker;volatile ProjectFiles importFiles;
        interface ImportReader {String read(WorkspaceStore.Session source,ProjectFiles.Entry file)throws Exception;}
        ImportReader importReader=this::readImportFile;
        String readImportFile(WorkspaceStore.Session source,ProjectFiles.Entry entry)throws Exception{
            ProjectFiles files=new ProjectFiles(activity,source);synchronized(this){if(closed){files.close();throw new java.io.IOException("INSTRUCTIONS_UNAVAILABLE");}importFiles=files;}
            try{return files.editRead(entry).text;}finally{files.close();synchronized(this){if(importFiles==files)importFiles=null;}}
        }
        Editor(TermuxActivity a,String path,AgentInstructions.Backend b){activity=a;project=initialProject=path;projectExplicit=path!=null&&path.startsWith("/");backend=b;}
        String t(String ru,String en){return tr(activity,ru,en);}
        int dp(int n){return WorkspaceDialogs.dp(activity,n);}
        LinearLayout column(){LinearLayout b=new LinearLayout(activity);b.setOrientation(LinearLayout.VERTICAL);return b;}
        TextView text(String s){TextView v=new TextView(activity);v.setText(s);v.setTextColor(WorkspaceDialogs.TEXT);v.setTextSize(13);v.setPadding(dp(6),dp(6),dp(6),dp(6));return v;}
        TextView action(LinearLayout b,String s,String tag,Runnable r){TextView v=WorkspaceDialogs.action(activity,s,0);v.setTag(tag);v.setOnClickListener(w->{if(!busy)r.run();});b.addView(v,new LinearLayout.LayoutParams(-1,-2));return v;}
        JSONObject request(String op){try{return new JSONObject().put("operation",op).put("agent",agent).put("environment",environment()).put("scope",scope).put("project",project).put("name",name).put("memoryPath",memoryPath());}catch(JSONException e){throw new IllegalStateException(e);}}
        String environment(){return selectedEnvironment;}
        String agentLabel(){return agent.equals("claude")?"Claude Code":"Codex";}
        String baseName(){return agent.equals("claude")?"CLAUDE.md":"AGENTS.md";}
        String memoryKey(){return environment().equals("ubuntu-ai")?project:environment()+"|"+project;}
        String memoryPath(){return activity.getSharedPreferences("agent-instructions-memory-paths",0).getString(memoryKey(),"");}
        void selectAgents(){if(agents!=null)for(int n=0;n<agents.getChildCount();n++){TextView v=(TextView)agents.getChildAt(n);selected(v,("instructions-agent-"+agent).equals(v.getTag()));}if(environmentLabel!=null)environmentLabel.setText(t("Посмотреть шаблон приложения","View app template"));if(runtimeLabel!=null)runtimeLabel.setText(runtimeNotice(activity,environment()));}
        void switchAgent(String chosen){if(busy)return;guard(()->{String old=environment();agent=chosen;if(!old.equals(environment()))project="/root";name=baseName();selectAgents();load();});}

        boolean memory(){return scope.equals("project")&&name.equals("LEARNING.md");}
        String savedNotice(){return memory()?t("Сохранено. Агент читает память по указанию в инструкциях проекта; сохранение не запускает автоматическое обучение.","Saved. The agent reads memory when instructed by project rules; saving does not start automatic learning."):agent.equals("claude")?t("Сохранено. Выполняющийся запрос не изменяется. Для надёжного перечитывания правил перезапустите только Claude Code с продолжением диалога; поведение зависит от версии CLI.","Saved. The running request is unchanged. To reliably reread rules, restart only Claude Code and resume the conversation; behavior depends on the CLI version."):saved(activity);}
        void chooseMemory(){
            if(importPicker!=null)importPicker.close();importPicker=new WorkspaceLocalFilePicker(activity);
            importPicker.choose(t("Выбрать существующий LEARNING.md в выбранной Ubuntu","Choose existing LEARNING.md in the selected Ubuntu"),this::selectMemoryFile);
        }
        void selectMemoryFile(WorkspaceStore.Session source,ProjectFiles.Entry entry){
                if(closed||busy)return;
                if(!source.environment.equals(fileProfile(environment()).environment)||entry.directory||!entry.name.equals("LEARNING.md")){
                    status.setText(t("Выберите LEARNING.md внутри выбранной Ubuntu. Импорт копирует текст; выбор пути открывает исходный файл.","Select LEARNING.md inside the selected Ubuntu. Import copies text; path selection opens the original file."));return;
                }
                JSONObject q=request("read");try{q.put("memoryPath",entry.path);}catch(JSONException e){throw new IllegalStateException(e);}
                call(q,result->{activity.getSharedPreferences("agent-instructions-memory-paths",0).edit().putString(memoryKey(),entry.path).apply();render(result);});
        }
        String fileExplanation(){
            if(agent.equals("claude")&&!name.equals("LEARNING.md"))return name.equals("CLAUDE.local.md")?t("Личные инструкции проекта. Дополняют CLAUDE.md, а не заменяют его; не являются аналогом AGENTS.override.md.","Personal project instructions. They supplement CLAUDE.md rather than replacing it; they are not equivalent to AGENTS.override.md."):t("Инструкции Claude Code выбранного проекта. Загружаются вместе с общими, родительскими и локальными правилами. Создание проектного файла Claude может отключить загрузку AGENTS.md по умолчанию. Настройки CLI могут менять это поведение.","Claude Code instructions for the selected project. Loaded with shared, ancestor and local rules. Creating a project Claude file can disable default AGENTS.md loading. CLI settings can change this behavior.");
            if(name.equals("LEARNING.md"))return t("Подтверждённые уроки и особенности проекта. Агент читает файл по указанию в инструкциях. Это не автоматическое обучение и не замена инструкций проекта.","Confirmed project lessons and specifics. The agent reads this file when directed by instructions. This is not automatic learning or a replacement for project instructions.");
            if(name.equals("AGENTS.override.md"))return t("Приоритетный файл инструкций в этой папке. Если он содержит инструкции, поддерживаемый Codex читает его вместо AGENTS.md этой папки. Общие правила сохраняются.","Priority instruction file in this folder. When it contains instructions, supported Codex reads it instead of this folder's AGENTS.md. Shared rules remain in effect.");
            return t("Основные инструкции выбранного проекта. Дополняют общие правила.","Main instructions for the selected project. They supplement the shared rules.");
        }
        void show(){
            if(closed||dialog!=null&&dialog.isShowing())return;
            frame=new AgentManagerLayout(activity);root=frame;agents=new LinearLayout(activity);for(String a:new String[]{"codex","claude"}){TextView button=WorkspaceDialogs.action(activity,a.equals("codex")?"Codex":"Claude Code",0);button.setTag("instructions-agent-"+a);button.setOnClickListener(v->switchAgent(a));agents.addView(button,new LinearLayout.LayoutParams(-2,-2));}frame.content.addView(horizontal(agents,"instructions-agents-scroll"));
            environmentLabel=WorkspaceDialogs.action(activity,"",0);environmentLabel.setTag("instructions-template-view");environmentLabel.setOnClickListener(v->viewTemplate());frame.content.addView(environmentLabel);projectTemplate=WorkspaceDialogs.action(activity,t("Шаблон проектных инструкций","Project instructions template"),0);projectTemplate.setTag("instructions-project-template");projectTemplate.setOnClickListener(v->viewProjectTemplate());frame.content.addView(projectTemplate);projectCreate=WorkspaceDialogs.action(activity,t("Создание инструкций проекта","Create project instructions"),0);projectCreate.setTag("instructions-project-create");projectCreate.setOnClickListener(v->createProjectInstructions());frame.content.addView(projectCreate);runtimeLabel=text("");runtimeLabel.setTag("instructions-environment");frame.content.addView(runtimeLabel);selectAgents();
            tabs=new LinearLayout(activity);
            for(String tab:new String[]{"general","project","diagnostics"}){
                TextView v=WorkspaceDialogs.action(activity,tab.equals("general")?t("Общие","General"):tab.equals("project")?t("Проектные","Project"):t("Диагностика","Diagnostics"),0);
                v.setTag("instructions-tab-"+tab);v.setOnClickListener(w->{if(!busy)guard(()->{scope=tab;load();});});tabs.addView(v,new LinearLayout.LayoutParams(-2,-1));
            }
            selectTabs();frame.header.addView(horizontal(tabs,"instructions-tabs-scroll"));status=AgentManagerLayout.status(activity);status.setTextColor(WorkspaceDialogs.TEXT);status.setTextSize(13);status.setPadding(dp(6),dp(6),dp(6),dp(6));status.setVisibility(View.GONE);status.setTag("instructions-status");frame.content.addView(status);
            body=column();frame.content.addView(body,new LinearLayout.LayoutParams(-1,0,1));
            close=WorkspaceDialogs.action(activity,t("Отмена / Закрыть","Cancel / Close"),0);close.setTag("instructions-close");close.setOnClickListener(v->requestClose());frame.actions(close);
            dialog=new AlertDialog(activity,com.termux.R.style.WorkspaceDialog){
                @Override public void cancel(){requestClose();}
                @Override public void onBackPressed(){if(!hideKeyboard())requestClose();}
            };
            LinearLayout title=new LinearLayout(activity);title.setGravity(Gravity.CENTER_VERTICAL);title.setPadding(dp(16),dp(6),dp(10),dp(6));
            TextView heading=text(label(activity));heading.setTextSize(20);heading.setTypeface(null,Typeface.BOLD);title.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
            refresh=new ImageButton(activity);refresh.setTag("instructions-refresh");refresh.setImageResource(com.termux.R.drawable.workspace_server_refresh);refresh.setBackground(WorkspaceDialogs.background(activity,false));refresh.setPadding(dp(12),dp(12),dp(12),dp(12));refresh.setContentDescription(t("Обновить","Refresh"));
            refresh.setTooltipText(t("Обновить","Refresh"));
            refresh.setOnClickListener(v->reloadSelectedFile());title.addView(refresh,new LinearLayout.LayoutParams(dp(48),dp(48)));((AlertDialog)dialog).setCustomTitle(title);((AlertDialog)dialog).setView(root,0,0,0,0);selectTabs();
            dialog.setOnDismissListener(d->{closed=true;epoch++;if(creationDialog!=null){creationDialog.dismiss();}if(browser!=null)browser.close();if(importPicker!=null)importPicker.close();synchronized(this){if(importFiles!=null)importFiles.close();}worker.shutdown();handler.removeCallbacksAndMessages(null);});
            dialog.setCanceledOnTouchOutside(true);dialog.show();
            AgentManagerLayout.keyboard(dialog);
            root.setFocusableInTouchMode(true);root.requestFocus();load();
        }
        void selected(TextView view,boolean selected){
            view.setSelected(selected);view.setBackground(WorkspaceDialogs.background(activity,selected));
            view.setTextColor(selected?WorkspaceDialogs.ACCENT:WorkspaceDialogs.TEXT);view.setTypeface(null,selected?Typeface.BOLD:Typeface.NORMAL);
            view.setSingleLine(true);view.setEllipsize(null);view.setGravity(Gravity.CENTER);
            if(android.os.Build.VERSION.SDK_INT>=30)view.setStateDescription(selected?t("Выбрано","Selected"):null);
            view.setContentDescription(view.getText()+(selected?t(", выбрано",", selected"):""));
        }
        HorizontalScrollView horizontal(LinearLayout row,String tag){HorizontalScrollView scroll=new HorizontalScrollView(activity);scroll.setTag(tag);scroll.setFillViewport(true);scroll.addView(row,new ViewGroup.LayoutParams(-2,-2));
            scroll.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{for(int i=0;i<row.getChildCount();i++){View child=row.getChildAt(i);if(!child.isSelected())continue;int left=child.getLeft(),right=child.getRight(),x=scroll.getScrollX();if(left<x||child.getWidth()>scroll.getWidth())scroll.scrollTo(left,0);else if(right>x+scroll.getWidth())scroll.scrollTo(right-scroll.getWidth(),0);}});return scroll;}
        void selectTabs(){if(projectTemplate!=null)projectTemplate.setVisibility(scope.equals("project")?View.VISIBLE:View.GONE);if(refresh!=null)refresh.setVisibility(scope.equals("diagnostics")?View.GONE:View.VISIBLE);if(projectCreate!=null)projectCreate.setVisibility(scope.equals("project")?View.VISIBLE:View.GONE);if(environmentLabel!=null)environmentLabel.setVisibility(scope.equals("general")?View.VISIBLE:View.GONE);if(tabs==null)return;for(int i=0;i<tabs.getChildCount();i++){TextView v=(TextView)tabs.getChildAt(i);boolean active=("instructions-tab-"+scope).equals(v.getTag());selected(v,active);if(active)v.post(()->v.requestRectangleOnScreen(new android.graphics.Rect(0,0,v.getWidth(),v.getHeight()),true));}}
        boolean dirty(){return editor!=null&&document!=null&&!editor.getText().toString().equals(document.optString("text"));}
        boolean hideKeyboard(){
            if(android.os.Build.VERSION.SDK_INT>=30){
                android.view.WindowInsets insets=root.getRootWindowInsets();
                if(insets==null||!insets.isVisible(android.view.WindowInsets.Type.ime()))return false;
            }else{
                android.graphics.Rect visible=new android.graphics.Rect();root.getWindowVisibleDisplayFrame(visible);
                if(root.getRootView().getHeight()-visible.bottom<dp(120))return false;
            }
            android.view.inputmethod.InputMethodManager ime=(android.view.inputmethod.InputMethodManager)activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if(ime!=null)ime.hideSoftInputFromWindow(root.getWindowToken(),0);return true;
        }
        void requestClose(){
            if(closed||busy)return;
            if(!dirty()){dialog.dismiss();return;}
            WorkspaceDialogs.builder(activity).setTitle(t("Закрыть без сохранения?","Close without saving?"))
                .setMessage(t("Черновик будет потерян. Продолжить редактирование?","The draft will be lost. Continue editing?"))
                .setPositiveButton(t("Не сохранять","Discard"),(d,w)->{if(!busy&&!closed)dialog.dismiss();})
                .setNegativeButton(t("Продолжить редактирование","Continue editing"),null).show();
        }
        void guard(Runnable r){guard(r,()->{});}
        void guard(Runnable r,Runnable cancelled){
            if(!dirty()){r.run();return;}
            WorkspaceDialogs.builder(activity).setTitle(t("Несохранённые изменения","Unsaved changes"))
                .setMessage(t("Перейти без сохранения текущего текста?","Continue without saving the current text?"))
                .setPositiveButton(t("Не сохранять","Discard"),(d,w)->r.run()).setNegativeButton(t("Отмена","Cancel"),(d,w)->cancelled.run()).setOnCancelListener(d->cancelled.run()).show();
        }
        void call(JSONObject q,java.util.function.Consumer<JSONObject> done){operation(()->backend.call(q),done);}
        void operation(java.util.concurrent.Callable<JSONObject> work,java.util.function.Consumer<JSONObject> done){
            if(closed||busy)return;busy=true;int ticket=++epoch;status.setText(t("Выполняется операция с файлом…","File operation running…"));
            dialog.setCancelable(false);close.setEnabled(false);if(refresh!=null)refresh.setEnabled(false);if(projectTemplate!=null)projectTemplate.setEnabled(false);
            if(save!=null)save.setEnabled(false);if(editor!=null)editor.setEnabled(false);
            worker.execute(()->{
                JSONObject result=null;String failure=null;try{result=work.call();}catch(Exception e){failure=e instanceof java.io.IOException?e.getMessage():"INSTRUCTIONS_UNAVAILABLE";}
                final JSONObject value=result;final String error=failure;
                handler.post(()->{
                    if(closed||epoch!=ticket||activity.isFinishing()||activity.isDestroyed())return;
                    busy=false;dialog.setCancelable(true);close.setEnabled(true);if(refresh!=null)refresh.setEnabled(true);if(projectTemplate!=null)projectTemplate.setEnabled(true);
                    if(editor!=null)editor.setEnabled(true);if(save!=null)save.setEnabled(true);
                    if(error==null){status.setText("");done.accept(value);if(createRequested&&!createReading&&!createGuard&&!busy)continueProjectCreation();}else {createRequested=false;createReading=false;createGuard=false;status.setText(error(error));if(memory()&&editor==null)action(body,t("Выбрать существующий файл памяти","Choose existing memory file"),"instructions-memory-path",this::chooseMemory);}
                });
            });
        }
        String error(String code){
            if("EDIT_CONFLICT".equals(code)||"SOURCE_CHANGED".equals(code))return t("Файл или источник изменился. Ваш черновик сохранён в редакторе. Скопируйте его, затем откройте файл заново и сравните; чужая редакция не перезаписана.","The file or source changed. Your draft remains in the editor. Copy it, reopen and compare; the other revision was not overwritten.");
            if("MEMORY_PATH_REQUIRED".equals(code))return t("В инструкциях указано несколько файлов памяти. Выберите существующий файл явно.","Instructions reference multiple memory files. Explicitly choose the existing file.");
            if("IMPORT_TEXT_INVALID".equals(code))return t("Для импорта нужен корректный текст UTF-8 до 256 КБ без бинарных данных. Файл инструкций не изменён.","Import requires valid UTF-8 text up to 256 KB without binary data. The instruction file was not changed.");
            if("IMPORT_READ_FAILED".equals(code))return t("Не удалось прочитать выбранный файл. Проверьте путь, права и доступ к хранилищу. Файл инструкций не изменён.","Could not read the selected file. Check its path, permissions and storage access. The instruction file was not changed.");
            return t("Не удалось выполнить операцию. Показанная среда должна быть доступна, с Python 3. Файл не заменён автоматически. Код: ","Operation unavailable. The displayed environment must be accessible with Python 3. No automatic replacement. Code: ")+safe(code);
        }
        String safe(String value){return value!=null&&value.matches("[A-Z_]{1,80}")?value:"INSTRUCTIONS_UNAVAILABLE";}
        void load(){selectAgents();selectTabs();editor=null;document=null;save=null;updateProjectAction();body.removeAllViews();frame.actions(close);if(environment().equals("termux")&&agent.equals("claude")){status.setText(t("Claude Code в этой сохранённой среде не поддерживается редактором. Среда не переключалась; файлы Ubuntu AI не изменены.","The editor does not support Claude Code in this retained environment. The environment was not switched; Ubuntu AI files are unchanged."));return;}if(scope.equals("diagnostics"))call(request("diagnostics"),this::diagnostics);else call(request("read"),this::render);}
        void render(JSONObject d){
            projectTemplateEditing=false;document=d;updateProjectAction();body.removeAllViews();
            LinearLayout fields=column();frame.scroll.setTag("instructions-content-scroll");body.addView(fields,new LinearLayout.LayoutParams(-1,-1));
            if(scope.equals("project")){
                TextView location=text(t("Проект: ","Project: ")+project);location.setTag("instructions-project-path");fields.addView(location);
                LinearLayout buttons=new LinearLayout(activity);
                for(String target:(agent.equals("claude")?new String[]{"CLAUDE.md","CLAUDE.local.md",".claude/CLAUDE.md","LEARNING.md"}:new String[]{"AGENTS.md","AGENTS.override.md","LEARNING.md"})){
                    TextView v=WorkspaceDialogs.action(activity,target,0);v.setTag("instructions-file-"+target);selected(v,target.equals(name));v.setOnClickListener(w->{if(!busy)guard(()->{name=target;load();});v.post(()->v.requestRectangleOnScreen(new android.graphics.Rect(0,0,v.getWidth(),v.getHeight()),true));});buttons.addView(v,new LinearLayout.LayoutParams(-2,-1));
                }fields.addView(horizontal(buttons,"instructions-files-scroll"));
                TextView explanation=text(fileExplanation());
                explanation.setTag("instructions-project-explanation");explanation.setTextSize(12);fields.addView(explanation);
                String selected=d.isNull("projectSource")?"":d.optString("projectSource","");
                TextView active=text(agent.equals("claude")?t("Claude читает несколько файлов совместно. Полный список и ограничения — в диагностике; выбор плашки меняет только редактируемый файл.","Claude combines multiple files. See diagnostics for sources and limits; a tab selects only the edit target."):selected.isEmpty()?t("В этой папке проектные инструкции отсутствуют.","No project instructions in this folder."):t("Codex читает в этой папке: ","Codex reads in this folder: ")+new java.io.File(selected).getName());
                active.setTag("instructions-project-active");active.setTextSize(12);fields.addView(active);
                action(fields,t("Выбрать другой проект","Choose another project"),"instructions-choose-project",()->guard(this::chooseProject));
            }
            if(scope.equals("project")&&!memory()&&!d.optBoolean("exists")){
                TextView absent=text(t("Файл отсутствует. Используйте «Создание инструкций проекта». Открытие ничего не создаёт.","The file is absent. Use Create project instructions. Opening creates nothing."));absent.setTag("instructions-project-missing");fields.addView(absent);
            }
            if(memory()){
                action(fields,t("Выбрать существующий файл памяти","Choose existing memory file"),"instructions-memory-path",()->guard(this::chooseMemory));
                if(!d.optBoolean("exists")){
                    fields.addView(text(t("Файл памяти отсутствует. Открытие редактора его не создаёт. Проверьте путь в инструкциях проекта перед созданием.","The memory file is absent. Opening the editor does not create it. Check the path in project instructions before creating it.")));
                    action(fields,t("Создать из шаблона","Create from template"),"instructions-memory-template",this::memoryPreview);
                }
            }
            TextView source=text((scope.equals("project")?t("Редактирование: ","Editing: "):(d.optBoolean("exists")?t("Файл: ","File: "):t("Файл ещё не создан: ","File not created yet: ")))+d.optString("path"));source.setTag("instructions-source");source.setTextIsSelectable(true);source.setMaxLines(3);fields.addView(source);
            if(!d.isNull("maskedBy")&&!d.optString("maskedBy").isEmpty())fields.addView(text(t("Этот файл перекрыт: ","This file is overridden by: ")+d.optString("maskedBy")));

            editor=new EditText(activity);editor.setTag("instructions-editor");editor.setTextColor(WorkspaceDialogs.TEXT);editor.setTextSize(14);editor.setTypeface(Typeface.MONOSPACE);
            editor.setGravity(Gravity.TOP|Gravity.START);editor.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            editor.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);editor.setHorizontallyScrolling(false);editor.setText(d.optString("text"));
            editor.setMinLines(6);editor.setMaxLines(Integer.MAX_VALUE);fields.addView(editor,new LinearLayout.LayoutParams(-1,0,1));
            LinearLayout controls=new LinearLayout(activity);
            TextView menu=WorkspaceDialogs.action(activity,t("Меню","Menu"),0);menu.setTag("instructions-menu");menu.setOnClickListener(v->{if(!busy)menu();});controls.addView(menu,new LinearLayout.LayoutParams(0,-2,1));
            save=WorkspaceDialogs.action(activity,t("Сохранить","Save"),0);save.setTag("instructions-save");save.setOnClickListener(v->{if(!busy){if(memory())confirmMemorySave();else save(editor.getText().toString());}});controls.addView(save,new LinearLayout.LayoutParams(0,-2,1));frame.actions(menu,save,close);
        }
        void confirmMemorySave(){
            showPreview(t("Сохранить LEARNING.md","Save LEARNING.md"),editor.getText().toString(),t("Точный путь: ","Exact path: ")+document.optString("path"),t("Сохранить","Save"),()->save(editor.getText().toString()));
        }
        void memoryPreview(){
            try{preview(t("Шаблон памяти · LEARNING.md","Memory template · LEARNING.md"),AgentInstructions.memoryTemplate(activity,AgentInstructions.language(activity)),t("Создать и сохранить","Create and save"));}
            catch(java.io.IOException e){status.setText(error("TEMPLATE_UNAVAILABLE"));}
        }
        void save(String text){save(text,null);}
        void save(String text,Runnable after){
            if(projectTemplateEditing){confirmProjectTemplateSave(text,after);return;}
            write(text,after);
        }
        private void write(String text,Runnable after){
            JSONObject q=request("save");try{q.put("path",document.getString("path")).put("revision",document.getString("revision")).put("text",text);}catch(JSONException impossible){throw new IllegalStateException(impossible);}
            call(q,result->{render(result);status.setText(savedNotice());if(after!=null)after.run();});
        }
        void reloadSelectedFile(){if(!closed&&!busy&&!scope.equals("diagnostics"))guard(this::load);}
        void menu(){
            String[] options=new String[]{t("История редакций","Revision history"),t("Восстановить предыдущую версию","Restore previous version"),scope.equals("general")?t("Импортировать инструкции","Import instructions"):memory()?t("Импортировать LEARNING.md","Import LEARNING.md"):t("Импортировать "+baseName(),"Import "+baseName())};
            WorkspaceDialogs.builder(activity).setTitle(t("Редакции и импорт","Revisions and import")).setItems(options,(d,w)->{
                if(w==0)history(false);else if(w==1)history(true);else importInstructions();
            }).show();
        }
        void importInstructions(){
            if(closed||busy||scope.equals("diagnostics"))return;
            if(!dirty()){chooseImport();return;}
            WorkspaceDialogs.builder(activity).setTitle(t("Несохранённый черновик","Unsaved draft"))
                .setMessage(t("Перед импортом сохраните черновик текущего файла или явно откажитесь от него. Отмена оставит черновик в редакторе.","Before importing, save the current file draft or explicitly discard it. Cancel keeps the draft in the editor."))
                .setPositiveButton(t("Сохранить черновик","Save draft"),(d,w)->save(editor.getText().toString(),this::chooseImport))
                .setNeutralButton(t("Отказаться от черновика","Discard draft"),(d,w)->{render(document);chooseImport();})
                .setNegativeButton(t("Отмена","Cancel"),null).show();
        }
        void chooseImport(){
            if(closed||busy)return;if(importPicker!=null)importPicker.close();importPicker=new WorkspaceLocalFilePicker(activity);
            importPicker.choose(t("Импорт инструкций · выбрать текстовый файл","Import instructions · choose a text file"),this::importSelected);
        }
        JSONObject importTarget(String op){JSONObject q=request(op);try{if(scope.equals("project")&&!memory())q.put("name",baseName());}catch(JSONException e){throw new IllegalStateException(e);}return q;}
        void importSelected(WorkspaceStore.Session source,ProjectFiles.Entry entry){
            if(closed||busy||scope.equals("diagnostics"))return;
            final JSONObject target=importTarget("read");
            operation(()->{
                if(entry.directory||entry.size<0||entry.size>TextFileEdits.MAX_BYTES)throw new java.io.IOException("IMPORT_TEXT_INVALID");
                String content;try{content=importReader.read(source,entry);}catch(Exception e){throw new java.io.IOException("IMPORT_READ_FAILED");}
                try{TextFileEdits.encode(content);}catch(Exception e){throw new java.io.IOException("IMPORT_TEXT_INVALID");}
                if(closed)throw new java.io.IOException("INSTRUCTIONS_UNAVAILABLE");
                return new JSONObject().put("target",backend.call(target)).put("text",content);
            },result->{
                JSONObject snapshot=result.optJSONObject("target");String value=result.optString("text");boolean projectImport=target.optString("scope").equals("project");
                LinearLayout header=column();TextView path=text(t("Файл назначения: ","Destination file: ")+snapshot.optString("path"));path.setTag("instructions-import-target");path.setTextIsSelectable(true);path.setMaxLines(3);header.addView(path);
                String warning="";
                if(projectImport&&!snapshot.isNull("maskedBy")&&!snapshot.optString("maskedBy").isEmpty())warning=t("В этой папке действует AGENTS.override.md. Импортированный AGENTS.md останется перекрыт им; override не изменится.","AGENTS.override.md is active in this folder. The imported AGENTS.md will remain overridden; the override will not change.");
                if(!projectImport&&snapshot.optString("path").endsWith("/AGENTS.override.md"))warning=t("Заменяется действующий общий AGENTS.override.md.","This replaces the active shared AGENTS.override.md.");
                if(!warning.isEmpty()){TextView note=text(warning);note.setTag("instructions-import-warning");header.addView(note);}
                String notice=(snapshot.optBoolean("exists")?t("Текст заменит существующий файл. Предыдущая редакция сохранится в истории.","This text replaces the existing file. Its previous revision stays in history."):t("Будет создан новый файл инструкций.","A new instruction file will be created."))+"\n"+t("Импортируется только содержимое выбранного файла, без перевода. До подтверждения файл назначения не меняется.","Only the selected file contents are imported, without translation. The destination remains unchanged until confirmation.");
                showPreview(projectImport?(memory()?t("Импортировать LEARNING.md","Import LEARNING.md"):t("Импортировать "+baseName(),"Import "+baseName())):t("Импортировать инструкции","Import instructions"),value,notice,t("Импортировать и сохранить","Import and save"),()->{
                    JSONObject q;try{q=new JSONObject(target.toString()).put("operation","save").put("path",snapshot.getString("path")).put("revision",snapshot.getString("revision")).put("text",value);}catch(JSONException e){throw new IllegalStateException(e);}
                    call(q,savedFile->{if(projectImport)name=target.optString("name");render(savedFile);status.setText(savedNotice());});
                },header);
            });
        }
        void updateProjectAction(){
            if(projectCreate==null)return;
            boolean exists=document!=null&&document.optBoolean("projectBaseExists",name.equals(baseName())&&document.optBoolean("exists"));
            projectCreate.setText(document==null?t("Открыть инструкции проекта","Open project instructions"):exists?t("Редактировать ","Edit ")+baseName():t("Создание инструкций проекта","Create project instructions"));
            projectCreate.setText(t("Открыть инструкцию проекта","Open project instruction"));
            projectCreate.setContentDescription(projectCreate.getText());
        }
        void focusProjectEditor(){
            if(editor==null)return;editor.requestFocus();
            android.graphics.Rect caret=new android.graphics.Rect();editor.getFocusedRect(caret);editor.requestRectangleOnScreen(caret,true);
            ((android.view.inputmethod.InputMethodManager)activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)).showSoftInput(editor,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            editor.announceForAccessibility(t("Файл уже открыт для редактирования: ","File already open for editing: ")+document.optString("path"));
        }
        String creationContext(){return agent+"\n"+environment()+"\n"+project+"\n"+scope;}
        void createProjectInstructions(){
            if(closed||!scope.equals("project"))return;
            if(creationDialog!=null&&creationDialog.isShowing())return;
            if(createRequested)return;
            createRequested=true;createContext=creationContext();
            if(busy){status.setText(t("Загрузка файла… Инструкции откроются после проверки.","Loading file… Instructions will open after the check."));return;}
            continueProjectCreation();
        }
        void continueProjectCreation(){
            if(closed||busy||createReading||createGuard||!createRequested)return;
            if(!createContext.equals(creationContext())){createRequested=false;status.setText(t("Проект или агент изменился. Откройте форму для выбранного проекта ещё раз.","Project or agent changed. Open the form again for the selected project."));return;}
            if(name.equals(baseName())&&document!=null&&document.optBoolean("exists")){createRequested=false;focusProjectEditor();return;}
            createGuard=true;guard(()->{
                createGuard=false;if(closed||!createContext.equals(creationContext())){createRequested=false;return;}
                if(name.equals(baseName())&&document!=null){createRequested=false;projectCreationForm();return;}
                JSONObject q=request("read");try{q.put("name",baseName()).put("memoryPath","");}catch(JSONException e){throw new IllegalStateException(e);}
                createReading=true;call(q,result->{createReading=false;createRequested=false;name=baseName();render(result);if(result.optBoolean("exists"))focusProjectEditor();else projectCreationForm();});
            },()->{createGuard=false;createRequested=false;});
        }
        void projectCreationForm(){
            if(closed||creationDialog!=null&&creationDialog.isShowing())return;
            final String sourceContext=creationContext();final JSONObject target;try{target=new JSONObject(request("save").toString()).put("name",baseName()).put("path",document.getString("path")).put("revision",document.getString("revision"));}catch(JSONException e){throw new IllegalStateException(e);}
            final boolean[] discarded={false};
            AgentManagerLayout box=new AgentManagerLayout(activity);
            String[] titles={t("Назначение проекта","Project purpose"),t("Рабочие пути","Working paths"),t("Ограничения проекта","Project constraints"),t("Способы проверки результата","Result verification"),t("Документация и расположение LEARNING.md","Documentation and LEARNING.md location")};
            EditText[] inputs=new EditText[titles.length];
            box.content.addView(text(t("Все разделы необязательны. Указывайте только известные сведения. Открытие формы ничего не записывает; LEARNING.md и другие документы не создаются.","All sections are optional. Enter only known facts. Opening this form writes nothing; LEARNING.md and other documents are not created.")));
            box.content.addView(text(t("Точный путь: ","Exact path: ")+document.optString("path")));
            for(int i=0;i<inputs.length;i++){box.content.addView(text(titles[i]));EditText input=new EditText(activity);input.setTextColor(WorkspaceDialogs.TEXT);input.setGravity(Gravity.TOP);input.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);input.setMinLines(2);input.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);input.setTag("instructions-create-field-"+i);inputs[i]=input;if(creationDraft!=null&&sourceContext.equals(creationDraftContext)&&i<creationDraft.length)input.setText(creationDraft[i]);box.content.addView(input,new LinearLayout.LayoutParams(-1,-2));}
            TextView feedback=AgentManagerLayout.status(activity);feedback.setTextColor(WorkspaceDialogs.TEXT);feedback.setTag("instructions-create-status");box.content.addView(feedback);
            android.text.TextWatcher observer=new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){feedback.setText(s);}public void afterTextChanged(android.text.Editable e){}};
            status.addTextChangedListener(observer);
            final AlertDialog[] holder={null};Runnable dismiss=()->{if(busy)return;boolean dirty=false;for(EditText input:inputs)dirty|=input.length()>0;if(!dirty){holder[0].dismiss();return;}WorkspaceDialogs.builder(activity).setTitle(t("Несохранённые изменения","Unsaved changes")).setMessage(t("Закрыть форму без сохранения?","Close the form without saving?"))
                .setPositiveButton(t("Не сохранять","Discard"),(d,w)->{discarded[0]=true;holder[0].dismiss();}).setNegativeButton(t("Продолжить редактирование","Continue editing"),null).show();};
            AlertDialog form=new AlertDialog(activity,com.termux.R.style.WorkspaceDialog){@Override public void cancel(){dismiss.run();}@Override public void onBackPressed(){dismiss.run();}};holder[0]=form;
            form.setTitle(t("Создание инструкций проекта","Create project instructions"));form.setView(box,0,0,0,0);
            TextView review=WorkspaceDialogs.action(activity,t("Просмотреть","Review"),0),cancel=WorkspaceDialogs.action(activity,t("Отмена","Cancel"),0);box.actions(review,cancel);cancel.setOnClickListener(v->dismiss.run());
            review.setOnClickListener(v->{if(busy||closed)return;StringBuilder value=new StringBuilder(t("# Инструкции проекта\n","# Project instructions\n"));for(int i=0;i<inputs.length;i++){String body=inputs[i].getText().toString();if(!body.trim().isEmpty())value.append("\n## ").append(titles[i]).append("\n\n").append(body).append(body.endsWith("\n")?"":"\n");}
                showPreview(t("Создание инструкций проекта","Create project instructions"),value.toString(),t("Точный путь: ","Exact path: ")+target.optString("path"),t("Создать и сохранить","Create and save"),()->{
                    if(!sourceContext.equals(creationContext())){status.setText(error("SOURCE_CHANGED"));return;}
                    JSONObject q;try{q=new JSONObject(target.toString()).put("text",value.toString());}catch(JSONException e){throw new IllegalStateException(e);}
                    call(q,result->{name=target.optString("name");render(result);status.setText(savedNotice());discarded[0]=true;form.dismiss();});
                });});
            creationDialog=form;form.setOnDismissListener(d->{if(!discarded[0]){creationDraft=new String[inputs.length];for(int i=0;i<inputs.length;i++)creationDraft[i]=inputs[i].getText().toString();creationDraftContext=sourceContext;}else if(sourceContext.equals(creationDraftContext)){creationDraft=null;creationDraftContext="";}status.removeTextChangedListener(observer);if(creationDialog==form)creationDialog=null;});
            try{form.show();AgentManagerLayout.keyboard(form);}catch(RuntimeException e){status.removeTextChangedListener(observer);creationDialog=null;status.setText(t("Не удалось открыть окно. Код: ","Could not open window. Code: ")+e.getClass().getSimpleName());}
        }
        void viewProjectTemplate(){
            if(closed||busy||!scope.equals("project"))return;
            if(!projectExplicit||project==null||!project.startsWith("/")){guard(this::chooseProject);return;}
            final String context=creationContext(),targetName=baseName();JSONObject q=request("read");try{q.put("name",targetName).put("memoryPath","");}catch(JSONException e){throw new IllegalStateException(e);}
            call(q,snapshot->{
                if(!context.equals(creationContext())){status.setText(error("SOURCE_CHANGED"));return;}
                if(snapshot.optBoolean("exists"))WorkspaceDialogs.builder(activity).setTitle(targetName+t(" уже существует"," already exists"))
                    .setMessage(t("Точный путь: ","Exact path: ")+snapshot.optString("path")+"\n\n"+t("Откройте существующий файл или отредактируйте шаблон. Замена потребует отдельного подтверждения при сохранении.","Open the existing file or edit the template. Replacement requires separate confirmation when saving."))
                    .setPositiveButton(t("Открыть существующий файл","Open existing file"),(d,w)->guard(()->{if(context.equals(creationContext())){name=targetName;render(snapshot);}else status.setText(error("SOURCE_CHANGED"));}))
                    .setNeutralButton(t("Посмотреть шаблон","View template"),(d,w)->projectTemplatePreview(snapshot,context)).setNegativeButton(t("Отмена","Cancel"),null).show();
                else projectTemplatePreview(snapshot,context);
            });
        }
        void projectTemplatePreview(JSONObject snapshot,String context){
            if(closed||busy||!context.equals(creationContext()))return;
            final String targetName=baseName();final String value;try{value=AgentInstructions.projectTemplate(activity);}catch(java.io.IOException e){status.setText(error("TEMPLATE_UNAVAILABLE"));return;}
            showPreview(t("Шаблон проектных инструкций","Project instructions template"),value,
                t("Точный путь: ","Exact path: ")+snapshot.optString("path")+"\n\n"+t("Шаблон на русском языке. Поля [в скобках] заполните в редакторе. Просмотр ничего не записывает. Общие инструкции и другие проекты не меняются.","This supplied template is in Russian. Fill [bracketed fields] in the editor. Viewing writes nothing. Shared rules and other projects stay unchanged."),
                t("Редактировать шаблон","Edit template"),()->guard(()->{if(!context.equals(creationContext())){status.setText(error("SOURCE_CHANGED"));return;}name=targetName;render(snapshot);editor.setText(value);projectTemplateEditing=true;status.setText(t("Шаблон открыт как несохранённый черновик. Проверьте текст и путь перед сохранением.","Template opened as an unsaved draft. Review text and path before saving."));}));
        }
        void confirmProjectTemplateSave(String value,Runnable after){
            if(closed||busy||!projectTemplateEditing||!scope.equals("project")||!name.equals(baseName()))return;
            boolean exists=document.optBoolean("exists");
            showPreview(t("Сохранить проектные инструкции","Save project instructions"),value,
                t("Точный путь: ","Exact path: ")+document.optString("path")+"\n\n"+(exists?t("Подтверждение заменит существующий ","Confirmation replaces the existing ")+baseName()+t(" этим текстом. Предыдущая редакция останется в истории."," with this text. Its previous revision stays in history."):t("Будет создан новый файл ","A new file will be created: ")+baseName()+t(". До подтверждения файл не создаётся.",". Nothing is written before confirmation.")),
                exists?t("Заменить ","Replace ")+baseName():t("Создать и сохранить","Create and save"),()->write(value,after));
        }
        void storageAddition(){languagePreview(true);}
        void template(){languagePreview(false);}
        void viewTemplate(){if(scope.equals("general"))template();}
        void languagePreview(boolean addition){
            if(!scope.equals("general"))return;
            final String current=editor==null?"":editor.getText().toString();
            if(addition&&AgentInstructions.hasStorageAddition(current)){status.setText(t("Дополнение уже есть в тексте (RU или EN).","The addition is already in the text (RU or EN)."));return;}
            final String[] values=new String[2];
            try{for(int n=0;n<2;n++){String value=AgentInstructions.builtInTemplate(activity,n==0?"ru":"en",agent);values[n]=addition?AgentInstructions.storageAddition(value):value;}}
            catch(java.io.IOException e){status.setText(error("TEMPLATE_UNAVAILABLE"));return;}
            final int[] choice={AgentInstructions.language(activity).equals("ru")?0:1};
            LinearLayout header=column();LinearLayout languages=new LinearLayout(activity);TextView[] options=new TextView[2];
            TextView selected=text("");selected.setTag("instructions-preview-language");
            for(int n=0;n<2;n++){
                TextView button=WorkspaceDialogs.action(activity,n==0?"RU":"EN",0);button.setTag("instructions-preview-"+(n==0?"ru":"en"));
                button.setSingleLine(true);options[n]=button;languages.addView(button,new LinearLayout.LayoutParams(-2,-2));
            }
            languages.addView(selected,new LinearLayout.LayoutParams(0,-2,1));header.addView(languages);
            TextView target=text(t(addition?"Добавление в: ":"Заменит файл: ",addition?"Append to: ":"Replace file: ")+(document==null?"":document.optString("path")));target.setTag("instructions-preview-target");target.setMaxLines(3);target.setTextIsSelectable(true);header.addView(target);
            String notice=addition?t("Раздел будет добавлен в конец текущего текста редактора. Существующий текст сохранится, предыдущая редакция файла останется в истории. Автоочистка не включается.","Append this section to the current editor text. Existing text is retained; the previous file revision remains in history. Automatic cleanup is not enabled."):
                t("Применение заменит текст выбранного файла. Предыдущая редакция останется в истории. Несохранённый черновик будет заменён.","Applying replaces the selected file text. Its previous revision stays in history. An unsaved draft will be replaced.");
            AlertDialog preview=showPreview(addition?t("Хранение файлов и временных данных","File and temporary data storage"):t("Встроенный шаблон · "+agentLabel(),"Built-in template · "+agentLabel()),values[choice[0]],notice,
                addition?t("Добавить и сохранить","Append and save"):t("Применить шаблон","Apply template"),()->{
                    String value=values[choice[0]];
                    if(addition&&(AgentInstructions.hasStorageAddition(current)||current.contains(value))){status.setText(t("Дополнение уже есть в тексте.","The addition is already in the text."));return;}
                    save(addition?current+(current.isEmpty()?"":current.endsWith("\n")?"\n":"\n\n")+value:value);
                },header);
            TextView value=preview.getWindow().getDecorView().findViewWithTag("instructions-preview-text");
            Runnable refresh=()->{
                value.setText(values[choice[0]]);selected.setText(t("Язык: ","Language: ")+(choice[0]==0?"RU":"EN"));
                for(int n=0;n<2;n++){options[n].setSelected(n==choice[0]);options[n].setBackground(WorkspaceDialogs.background(activity,n==choice[0]));}
            };
            for(int n=0;n<2;n++){final int index=n;options[n].setOnClickListener(v->{choice[0]=index;refresh.run();ScrollView scroll=preview.getWindow().getDecorView().findViewWithTag("instructions-preview-scroll");scroll.scrollTo(0,0);});}
            refresh.run();
        }
        void preview(String title,String value,String apply){
            showPreview(title,value,
                t("Точный путь: ","Exact path: ")+document.optString("path")+"\n"+t("Применение заменит текст выбранного файла. Предыдущая редакция останется в истории. Несохранённый черновик будет заменён.","Applying replaces the selected file text. Its previous revision stays in history. An unsaved draft will be replaced."),apply,()->save(value));
        }
        AlertDialog showPreview(String title,String value,String notice,String apply,Runnable commit){return showPreview(title,value,notice,apply,commit,null);}
        AlertDialog showPreview(String title,String value,String notice,String apply,Runnable commit,View header){
            final String sourceIdentity=agent+"|"+environment()+"|"+scope+"|"+project+"|"+(document==null?"":document.optString("path"));
            LinearLayout content=column();
            // Keep the language switches available; long paths and warnings belong
            // to the scrollable preview, rather than consuming its fixed header.
            if(header instanceof ViewGroup){ViewGroup group=(ViewGroup)header;for(String tag:new String[]{"instructions-preview-target","instructions-import-target","instructions-import-warning"}){View item=group.findViewWithTag(tag);if(item!=null&&item.getParent()==group){group.removeView(item);content.addView(item);}}}
            content.addView(text(notice));
            TextView view=text(value);view.setTag("instructions-preview-text");view.setTextIsSelectable(true);view.setTypeface(Typeface.MONOSPACE);content.addView(view);
            ScrollView scroll=new ScrollView(activity);scroll.setTag("instructions-preview-scroll");scroll.addView(content);
            // Keep both the notice and long text in the bounded middle panel. AlertDialog
            // reserves the title and button panel before measuring this scroll container.
            LinearLayout panel=new LinearLayout(activity){
                @Override protected void onMeasure(int widthSpec,int heightSpec){
                    // The alert allocates the middle panel after title/buttons. Fill
                    // that bounded space so a weighted scroll child cannot collapse.
                    if(View.MeasureSpec.getMode(heightSpec)==View.MeasureSpec.AT_MOST)heightSpec=View.MeasureSpec.makeMeasureSpec(View.MeasureSpec.getSize(heightSpec),View.MeasureSpec.EXACTLY);
                    super.onMeasure(widthSpec,heightSpec);
                }
            };panel.setOrientation(LinearLayout.VERTICAL);if(header!=null)panel.addView(header);panel.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
            AlertDialog preview=WorkspaceDialogs.builder(activity).setTitle(title).setView(panel)
                .setPositiveButton(apply,(d,w)->{if(apply!=null&&commit!=null&&!closed&&!busy){String now=agent+"|"+environment()+"|"+scope+"|"+project+"|"+(document==null?"":document.optString("path"));if(sourceIdentity.equals(now))commit.run();else status.setText(error("SOURCE_CHANGED"));}}).setNegativeButton(apply==null?t("Закрыть","Close"):t("Отмена","Cancel"),null).create();
            preview.setOnShowListener(d->{
                android.graphics.Rect visible=new android.graphics.Rect();dialog.getWindow().getDecorView().getWindowVisibleDisplayFrame(visible);
                int width=visible.width()>0?visible.width():activity.getResources().getDisplayMetrics().widthPixels;
                int height=visible.height()>0?visible.height():activity.getResources().getDisplayMetrics().heightPixels;
                preview.getWindow().setLayout((int)(width*.95),(int)(height*.9));
                preview.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE|WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
            });
            preview.show();return preview;
        }
        void history(boolean previous){
            call(request("history"),result->{
                JSONArray rows=result.optJSONArray("history");if(rows==null||rows.length()==0){status.setText(t("Предыдущих редакций нет.","No previous revisions."));return;}
                java.util.function.IntConsumer open=index->{JSONObject q=request("previous");try{q.put("id",rows.getJSONObject(index).getString("id"));}catch(JSONException e){throw new IllegalStateException(e);}
                    call(q,value->preview(t("Предыдущая редакция","Previous revision"),value.optString("text"),t("Восстановить и сохранить","Restore and save")));};
                if(previous){open.accept(0);return;}
                String[] labels=new String[rows.length()];for(int n=0;n<labels.length;n++){JSONObject row=rows.optJSONObject(n);labels[n]=android.text.format.DateFormat.format("yyyy-MM-dd HH:mm:ss",(long)(row.optDouble("time")*1000)).toString()+" · "+row.optString("sha256").substring(0,8);}
                WorkspaceDialogs.builder(activity).setTitle(t("История редакций","Revision history")).setItems(labels,(d,w)->open.accept(w)).setNegativeButton(t("Закрыть","Close"),null).show();
            });
        }
        void chooseProject(){
            LinearLayout form=column();EditText path=new EditText(activity);path.setText(projectExplicit?project:"");path.setHint(t("Абсолютный путь проекта","Absolute project path"));path.setSingleLine(true);path.setTextColor(WorkspaceDialogs.TEXT);path.setTag("instructions-project-input");form.addView(path);
            AlertDialog chooser=WorkspaceDialogs.builder(activity).setTitle(environment().equals("ubuntu-ai")?t("Проект Ubuntu AI","Ubuntu AI project"):t("Проект · ","Project · ")+AgentEnvironments.label(environment())).setView(form).setPositiveButton(t("Открыть","Open"),null)
                .setNeutralButton(t("Папка","Folder"),(d,w)->{
                    WorkspaceStore.Session p=fileProfile(environment());
                    browser=new WorkspaceFileBrowser(activity,p,project,picked->{if(!closed){project=picked;projectExplicit=true;load();}});browser.show();
                }).setNegativeButton(t("Отмена","Cancel"),null).create();chooser.show();
            chooser.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String chosen=path.getText().toString();if(!chosen.startsWith("/")||chosen.indexOf('\0')>=0){path.setError(t("Нужен абсолютный путь","Absolute path required"));return;}chooser.dismiss();project=chosen;projectExplicit=true;load();});
        }
        void claudeDiagnostics(JSONObject result){
            body.removeAllViews();LinearLayout rows=column();body.addView(rows,new LinearLayout.LayoutParams(-1,-2));
            JSONObject cli=result.optJSONObject("cli");rows.addView(text("Claude Code · "+(cli==null||cli.isNull("version")?t("версия неизвестна","unknown version"):cli.optString("version"))));
            rows.addView(text(t("Общие инструкции: ","Shared instructions: ")+result.optString("generalPath")));
            rows.addView(text(t("CLAUDE.md, .claude/CLAUDE.md и CLAUDE.local.md дополняют друг друга; локальный файл не заменяет основные правила. AGENTS.override.md не используется. Загрузка AGENTS.md в новых версиях зависит от встроенного плагина и настроек; при наличии проектных файлов Claude он по умолчанию не читается.","CLAUDE.md, .claude/CLAUDE.md and CLAUDE.local.md supplement each other; local rules do not replace the main file. AGENTS.override.md is not used. Newer versions load AGENTS.md according to the built-in plugin and settings; project Claude files disable its default fallback.")));
            rows.addView(text(cli!=null&&cli.optBoolean("behaviorVerified")?t("Проверено на Claude Code 2.1.296: сохранённые изменения не перечитывались в следующем запросе того же процесса; перезапуск с продолжением и новый диалог загружали новые правила.","Tested on Claude Code 2.1.296: the next request in the same process retained old instructions; restart/resume and a fresh conversation loaded the new rules."):t("Поведение этой версии не проверено. Не рассчитывайте на применение без перезапуска Claude с продолжением истории.","This version's behavior is unverified. Do not rely on changes applying without restarting Claude and resuming history.")));
            rows.addView(text(t("LEARNING.md — общий файл проекта, не встроенная auto memory Claude (MEMORY.md). Редактор не меняет настройки auto memory, права или подключение. Для применения правил используйте перезапуск Claude с продолжением истории; текущий запрос не меняется. Источники показаны для стандартного запуска: пользовательский запускатель может менять среду и каталог конфигурации.","LEARNING.md is shared project memory, distinct from Claude's built-in auto memory (MEMORY.md). This editor does not change auto-memory settings, permissions or connectivity. Restart Claude and resume history to apply rules; the current request is unchanged. Sources describe the standard launch; a custom launcher can change the environment/config directory.")));
            TextView details=text("");try{details.setText(result.toString(2));}catch(JSONException e){details.setText("{}");}details.setTag("instructions-claude-details");details.setTextIsSelectable(true);details.setVisibility(View.GONE);action(rows,t("Пути и технические сведения ▾","Paths and technical details ▾"),"instructions-details-toggle",()->details.setVisibility(details.getVisibility()==View.GONE?View.VISIBLE:View.GONE));rows.addView(details);
        }
        void diagnostics(JSONObject result){
            if(agent.equals("claude")){claudeDiagnostics(result);return;}
            body.removeAllViews();LinearLayout rows=column();body.addView(rows,new LinearLayout.LayoutParams(-1,-2));
            JSONObject cli=result.optJSONObject("cli");String version=cli==null||cli.isNull("version")?t("не определена","unknown"):cli.optString("version");
            rows.addView(text("Codex · "+version));
            boolean verified=cli!=null&&cli.optBoolean("behaviorVerified");
            rows.addView(text(verified?t("Для этой версии проверено: общий файл перечитывается между запросами; проектные правила — при новом запуске с продолжением истории.","Tested for this version: shared rules reload between requests; project rules reload in a new process that resumes history."):t("Поведение этой версии не проверено. Сохранение текста доступно; для повторного чтения правил перезапустите агента с продолжением диалога. Не рассчитывайте на применение в уже работающем чате.","This version's behavior is unverified. Saving remains available; restart the agent and resume history to reread rules. Do not rely on live changes in the current chat.")));

            rows.addView(text(t("Общие правила берутся из CODEX_HOME показанной среды. Проектные правила уточняют их по дереву проекта; AGENTS.override.md имеет приоритет в своём каталоге. Это механизм AGENTS, а не developerInstructions.","Shared rules come from CODEX_HOME in the displayed environment. Project rules refine them along the project tree; AGENTS.override.md takes precedence in its directory. This is AGENTS, not developerInstructions.")));
            rows.addView(text(t("Полный набор файлов читается новым процессом агента, в том числе при возобновлении истории. Выполняющийся запрос не изменяется. На Codex 0.163/0.164 проверено перечитывание общего файла между запросами; проектные правила сохранялись до нового запуска. Другие версии могут отличаться. Редактор не меняет внутренние правила сервиса и модели.","A new agent process reads the complete file set, including when resuming history. A running request is unchanged. Tests on Codex 0.163/0.164 observed shared-rule reload between requests; project rules stayed until a new launch. Other versions may differ. The editor does not change internal service or model rules.")));
            rows.addView(text(t("Проверка показывает файловые источники нового стандартного запуска в показанной среде. Настройки config.toml, выбранные профили и пользовательская команда подключения могут дополнительно влиять на запуск. Здесь отмечается наличие настроек, без раскрытия их содержимого; параметры уже работающего процесса не подменяются.","This checks file sources for a new standard launch in the displayed environment. config.toml settings, selected profiles and a custom connection executable can additionally affect startup. Only setting presence is shown, without their contents; a running process is not reconfigured.")));
            TextView details=text("");details.setTextIsSelectable(true);details.setVisibility(View.GONE);try{details.setText(result.toString(2));}catch(JSONException e){details.setText(t("Сведения недоступны","Details unavailable"));}
            action(rows,t("Пути и технические сведения ▾","Paths and technical details ▾"),"instructions-details-toggle",()->details.setVisibility(details.getVisibility()==View.GONE?View.VISIBLE:View.GONE));rows.addView(details);
        }
    }
}
