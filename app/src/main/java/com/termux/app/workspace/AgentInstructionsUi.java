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

/** General settings editor. Chat data is used only to suggest the project directory. */
final class AgentInstructionsUi {
    static String label(android.content.Context c){return tr(c,"Инструкции агентов","Agent instructions");}
    static String tr(android.content.Context c,String ru,String en){return WorkspaceLanguage.code(c).equals("ru")?ru:en;}
    static String saved(android.content.Context c){return tr(c,"Сохранено. Выполняющийся запрос не изменяется. Общие правила могут примениться уже к следующему запросу. Для повторного чтения общих и проектных правил перезапустите агента с продолжением диалога.", "Saved. The running request is unchanged. Shared rules may apply to the next request. To reread both shared and project rules, restart the agent and resume the conversation.");}
    static String suggested(WorkspaceStore.Session profile){String path=profile!=null&&profile.codexEnvironment.equals("ubuntu-ai")?CodexProjects.directory("ubuntu-ai",profile.directory):"/root";return path.isEmpty()?"/root":path;}
    static void show(TermuxActivity a,WorkspaceStore.Session profile){new Editor(a,suggested(profile),q->AgentInstructions.request(a,q)).show();}
    static final class Editor {
        final TermuxActivity activity;final AgentInstructions.Backend backend;
        final ExecutorService worker=Executors.newSingleThreadExecutor();final Handler handler=new Handler(Looper.getMainLooper());
        final String initialProject;String project,scope="general",name="AGENTS.md";JSONObject document;
        android.app.Dialog dialog;LinearLayout root,body,tabs;EditText editor;TextView status,save,close;boolean busy;volatile boolean closed;int epoch;
        WorkspaceFileBrowser browser;WorkspaceLocalFilePicker importPicker;volatile ProjectFiles importFiles;
        interface ImportReader {String read(WorkspaceStore.Session source,ProjectFiles.Entry file)throws Exception;}
        ImportReader importReader=this::readImportFile;
        String readImportFile(WorkspaceStore.Session source,ProjectFiles.Entry entry)throws Exception{
            ProjectFiles files=new ProjectFiles(activity,source);synchronized(this){if(closed){files.close();throw new java.io.IOException("INSTRUCTIONS_UNAVAILABLE");}importFiles=files;}
            try{return files.editRead(entry).text;}finally{files.close();synchronized(this){if(importFiles==files)importFiles=null;}}
        }
        Editor(TermuxActivity a,String path,AgentInstructions.Backend b){activity=a;project=initialProject=path;backend=b;}
        String t(String ru,String en){return tr(activity,ru,en);}
        int dp(int n){return WorkspaceDialogs.dp(activity,n);}
        LinearLayout column(){LinearLayout b=new LinearLayout(activity);b.setOrientation(LinearLayout.VERTICAL);return b;}
        TextView text(String s){TextView v=new TextView(activity);v.setText(s);v.setTextColor(WorkspaceDialogs.TEXT);v.setTextSize(13);v.setPadding(dp(6),dp(6),dp(6),dp(6));return v;}
        TextView action(LinearLayout b,String s,String tag,Runnable r){TextView v=WorkspaceDialogs.action(activity,s,0);v.setTag(tag);v.setOnClickListener(w->{if(!busy)r.run();});b.addView(v,new LinearLayout.LayoutParams(-1,-2));return v;}
        JSONObject request(String op){try{return new JSONObject().put("operation",op).put("scope",scope).put("project",project).put("name",name);}catch(JSONException e){throw new IllegalStateException(e);}}
        void show(){
            root=column();root.setPadding(dp(10),dp(6),dp(10),dp(8));root.addView(text("Codex → Ubuntu AI"));
            tabs=new LinearLayout(activity);
            for(String tab:new String[]{"general","project","diagnostics"}){
                TextView v=WorkspaceDialogs.action(activity,tab.equals("general")?t("Общие","General"):tab.equals("project")?t("Проект","Project"):t("Диагностика","Diagnostics"),0);
                v.setTag("instructions-tab-"+tab);v.setOnClickListener(w->{if(!busy)guard(()->{scope=tab;load();});});tabs.addView(v,new LinearLayout.LayoutParams(-2,-1));
            }
            selectTabs();root.addView(horizontal(tabs,"instructions-tabs-scroll"));status=text("");status.setTag("instructions-status");
            ScrollView statusScroll=new ScrollView(activity){@Override protected void onMeasure(int w,int h){super.onMeasure(w,View.MeasureSpec.makeMeasureSpec(Math.min(dp(90),View.MeasureSpec.getSize(h)),View.MeasureSpec.AT_MOST));}};
            statusScroll.addView(status);root.addView(statusScroll);
            body=column();root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
            close=WorkspaceDialogs.action(activity,t("Отмена / Закрыть","Cancel / Close"),0);close.setTag("instructions-close");close.setOnClickListener(v->{if(!busy)dialog.dismiss();});root.addView(close);
            dialog=new android.app.Dialog(activity,com.termux.R.style.WorkspaceDialog){
                @Override public void cancel(){requestClose();}
                @Override public void onBackPressed(){if(!hideKeyboard())requestClose();}
            };dialog.setTitle(label(activity));dialog.setContentView(root,new ViewGroup.LayoutParams(-1,-1));
            dialog.setOnDismissListener(d->{closed=true;epoch++;if(browser!=null)browser.close();if(importPicker!=null)importPicker.close();synchronized(this){if(importFiles!=null)importFiles.close();}worker.shutdown();handler.removeCallbacksAndMessages(null);});
            dialog.setCanceledOnTouchOutside(true);dialog.show();
            dialog.getWindow().setLayout((int)(activity.getResources().getDisplayMetrics().widthPixels*.95),(int)(activity.getResources().getDisplayMetrics().heightPixels*.9));
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE|WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
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
        void selectTabs(){if(tabs==null)return;for(int i=0;i<tabs.getChildCount();i++){TextView v=(TextView)tabs.getChildAt(i);boolean active=("instructions-tab-"+scope).equals(v.getTag());selected(v,active);if(active)v.post(()->v.requestRectangleOnScreen(new android.graphics.Rect(0,0,v.getWidth(),v.getHeight()),true));}}
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
        void guard(Runnable r){
            if(!dirty()){r.run();return;}
            WorkspaceDialogs.builder(activity).setTitle(t("Несохранённые изменения","Unsaved changes"))
                .setMessage(t("Перейти без сохранения текущего текста?","Continue without saving the current text?"))
                .setPositiveButton(t("Не сохранять","Discard"),(d,w)->r.run()).setNegativeButton(t("Отмена","Cancel"),null).show();
        }
        void call(JSONObject q,java.util.function.Consumer<JSONObject> done){operation(()->backend.call(q),done);}
        void operation(java.util.concurrent.Callable<JSONObject> work,java.util.function.Consumer<JSONObject> done){
            if(closed||busy)return;busy=true;int ticket=++epoch;status.setText(t("Выполняется операция с файлом…","File operation running…"));
            dialog.setCancelable(false);close.setEnabled(false);
            if(save!=null)save.setEnabled(false);if(editor!=null)editor.setEnabled(false);
            worker.execute(()->{
                JSONObject result=null;String failure=null;try{result=work.call();}catch(Exception e){failure=e instanceof java.io.IOException?e.getMessage():"INSTRUCTIONS_UNAVAILABLE";}
                final JSONObject value=result;final String error=failure;
                handler.post(()->{
                    if(closed||epoch!=ticket||activity.isFinishing()||activity.isDestroyed())return;
                    busy=false;dialog.setCancelable(true);close.setEnabled(true);
                    if(editor!=null)editor.setEnabled(true);if(save!=null)save.setEnabled(true);
                    if(error==null){status.setText("");done.accept(value);}else status.setText(error(error));
                });
            });
        }
        String error(String code){
            if("EDIT_CONFLICT".equals(code)||"SOURCE_CHANGED".equals(code))return t("Файл или источник изменился. Ваш черновик сохранён в редакторе. Скопируйте его, затем откройте файл заново и сравните; чужая редакция не перезаписана.","The file or source changed. Your draft remains in the editor. Copy it, reopen and compare; the other revision was not overwritten.");
            if("IMPORT_TEXT_INVALID".equals(code))return t("Для импорта нужен корректный текст UTF-8 до 256 КБ без бинарных данных. Файл инструкций не изменён.","Import requires valid UTF-8 text up to 256 KB without binary data. The instruction file was not changed.");
            if("IMPORT_READ_FAILED".equals(code))return t("Не удалось прочитать выбранный файл. Проверьте путь, права и доступ к хранилищу. Файл инструкций не изменён.","Could not read the selected file. Check its path, permissions and storage access. The instruction file was not changed.");
            return t("Не удалось выполнить операцию. Ubuntu AI должна быть установлена и доступна, с Python 3. Файл не заменён автоматически. Код: ","Operation unavailable. Ubuntu AI must be installed and accessible with Python 3. No automatic replacement. Code: ")+safe(code);
        }
        String safe(String value){return value!=null&&value.matches("[A-Z_]{1,80}")?value:"INSTRUCTIONS_UNAVAILABLE";}
        void load(){selectTabs();editor=null;document=null;save=null;body.removeAllViews();if(scope.equals("diagnostics"))call(request("diagnostics"),this::diagnostics);else call(request("read"),this::render);}
        void render(JSONObject d){
            document=d;body.removeAllViews();
            LinearLayout fields=column();ScrollView content=new ScrollView(activity);content.setTag("instructions-content-scroll");content.setFillViewport(true);content.addView(fields);body.addView(content,new LinearLayout.LayoutParams(-1,0,1));
            if(scope.equals("project")){
                TextView location=text(t("Проект: ","Project: ")+project);location.setTag("instructions-project-path");fields.addView(location);
                LinearLayout buttons=new LinearLayout(activity);
                for(String target:new String[]{"AGENTS.md","AGENTS.override.md"}){
                    TextView v=WorkspaceDialogs.action(activity,target,0);v.setTag("instructions-file-"+target);selected(v,target.equals(name));v.setOnClickListener(w->{if(!busy)guard(()->{name=target;load();});v.post(()->v.requestRectangleOnScreen(new android.graphics.Rect(0,0,v.getWidth(),v.getHeight()),true));});buttons.addView(v,new LinearLayout.LayoutParams(-2,-1));
                }fields.addView(horizontal(buttons,"instructions-files-scroll"));
                TextView explanation=text(t("AGENTS.md — основные правила проекта. AGENTS.override.md — переопределение: если он содержит инструкции, Codex использует его вместо AGENTS.md в этой же папке. Общие инструкции продолжают действовать.","AGENTS.md contains the main project rules. AGENTS.override.md overrides it: when it contains instructions, Codex uses it instead of AGENTS.md in the same folder. Shared instructions continue to apply."));
                explanation.setTag("instructions-project-explanation");explanation.setTextSize(12);fields.addView(explanation);
                String selected=d.isNull("projectSource")?"":d.optString("projectSource","");
                TextView active=text(selected.isEmpty()?t("В этой папке проектные инструкции отсутствуют.","No project instructions in this folder."):t("Codex читает в этой папке: ","Codex reads in this folder: ")+new java.io.File(selected).getName());
                active.setTag("instructions-project-active");active.setTextSize(12);fields.addView(active);
                action(fields,t("Выбрать другой проект","Choose another project"),"instructions-choose-project",()->guard(this::chooseProject));
            }
            TextView source=text((scope.equals("project")?t("Редактирование: ","Editing: "):(d.optBoolean("exists")?t("Файл: ","File: "):t("Файл ещё не создан: ","File not created yet: ")))+d.optString("path"));source.setTag("instructions-source");source.setTextIsSelectable(true);source.setMaxLines(3);fields.addView(source);
            if(!d.isNull("maskedBy")&&!d.optString("maskedBy").isEmpty())fields.addView(text(t("Этот файл перекрыт: ","This file is overridden by: ")+d.optString("maskedBy")));
            if(scope.equals("general")&&d.optBoolean("templateAvailable"))action(fields,t("Просмотреть встроенный шаблон","Review built-in template"),"instructions-template-offer",this::template);
            editor=new EditText(activity);editor.setTag("instructions-editor");editor.setTextColor(WorkspaceDialogs.TEXT);editor.setTextSize(14);editor.setTypeface(Typeface.MONOSPACE);
            editor.setGravity(Gravity.TOP|Gravity.START);editor.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            editor.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);editor.setHorizontallyScrolling(false);editor.setText(d.optString("text"));
            editor.setMinLines(6);editor.setMaxLines(14);fields.addView(editor,new LinearLayout.LayoutParams(-1,-2));
            LinearLayout controls=new LinearLayout(activity);
            TextView menu=WorkspaceDialogs.action(activity,t("Меню","Menu"),0);menu.setTag("instructions-menu");menu.setOnClickListener(v->{if(!busy)menu();});controls.addView(menu,new LinearLayout.LayoutParams(0,-2,1));
            save=WorkspaceDialogs.action(activity,t("Сохранить","Save"),0);save.setTag("instructions-save");save.setOnClickListener(v->{if(!busy)save(editor.getText().toString());});controls.addView(save,new LinearLayout.LayoutParams(0,-2,1));body.addView(controls);
        }
        void save(String text){save(text,null);}
        void save(String text,Runnable after){
            JSONObject q=request("save");try{q.put("path",document.getString("path")).put("revision",document.getString("revision")).put("text",text);}catch(JSONException impossible){throw new IllegalStateException(impossible);}
            call(q,result->{render(result);status.setText(saved(activity));if(after!=null)after.run();});
        }
        void menu(){
            String[] options=scope.equals("general")?new String[]{t("История редакций","Revision history"),t("Восстановить предыдущую версию","Restore previous version"),t("Просмотреть встроенный шаблон","View built-in template"),t("Дополнение: хранение файлов","Addition: file storage"),t("Открыть файл заново","Reload file"),t("Импортировать инструкции","Import instructions")}:new String[]{t("История редакций","Revision history"),t("Восстановить предыдущую версию","Restore previous version"),t("Открыть файл заново","Reload file"),t("Импортировать AGENTS.md","Import AGENTS.md")};
            WorkspaceDialogs.builder(activity).setTitle(t("Редакции и шаблон","Revisions and template")).setItems(options,(d,w)->{
                if(w==options.length-1)importInstructions();else if(w==0)history(false);else if(w==1)history(true);else if(scope.equals("general")&&w==2)template();else if(scope.equals("general")&&w==3)storageAddition();else guard(this::load);
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
        JSONObject importTarget(String op){JSONObject q=request(op);try{if(scope.equals("project"))q.put("name","AGENTS.md");}catch(JSONException e){throw new IllegalStateException(e);}return q;}
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
                showPreview(projectImport?t("Импортировать AGENTS.md","Import AGENTS.md"):t("Импортировать инструкции","Import instructions"),value,notice,t("Импортировать и сохранить","Import and save"),()->{
                    JSONObject q;try{q=new JSONObject(target.toString()).put("operation","save").put("path",snapshot.getString("path")).put("revision",snapshot.getString("revision")).put("text",value);}catch(JSONException e){throw new IllegalStateException(e);}
                    call(q,savedFile->{if(projectImport)name="AGENTS.md";render(savedFile);status.setText(saved(activity));});
                },header);
            });
        }
        void storageAddition(){languagePreview(true);}
        void template(){languagePreview(false);}
        void languagePreview(boolean addition){
            final String current=editor.getText().toString();
            if(addition&&AgentInstructions.hasStorageAddition(current)){status.setText(t("Дополнение уже есть в тексте (RU или EN).","The addition is already in the text (RU or EN)."));return;}
            final String[] values=new String[2];
            try{for(int n=0;n<2;n++){String value=AgentInstructions.builtInTemplate(activity,n==0?"ru":"en");values[n]=addition?AgentInstructions.storageAddition(value):value;}}
            catch(java.io.IOException e){status.setText(error("TEMPLATE_UNAVAILABLE"));return;}
            final int[] choice={AgentInstructions.language(activity).equals("ru")?0:1};
            LinearLayout header=column();LinearLayout languages=new LinearLayout(activity);TextView[] options=new TextView[2];
            TextView selected=text("");selected.setTag("instructions-preview-language");
            for(int n=0;n<2;n++){
                TextView button=WorkspaceDialogs.action(activity,n==0?"RU":"EN",0);button.setTag("instructions-preview-"+(n==0?"ru":"en"));
                button.setSingleLine(true);options[n]=button;languages.addView(button,new LinearLayout.LayoutParams(-2,-2));
            }
            languages.addView(selected,new LinearLayout.LayoutParams(0,-2,1));header.addView(languages);
            TextView target=text(t(addition?"Добавление в: ":"Заменит файл: ",addition?"Append to: ":"Replace file: ")+document.optString("path"));target.setTag("instructions-preview-target");target.setMaxLines(3);target.setTextIsSelectable(true);header.addView(target);
            String notice=addition?t("Раздел будет добавлен в конец текущего текста редактора. Существующий текст сохранится, предыдущая редакция файла останется в истории. Автоочистка не включается.","Append this section to the current editor text. Existing text is retained; the previous file revision remains in history. Automatic cleanup is not enabled."):
                t("Применение заменит текст выбранного файла. Предыдущая редакция останется в истории. Несохранённый черновик будет заменён.","Applying replaces the selected file text. Its previous revision stays in history. An unsaved draft will be replaced.");
            AlertDialog preview=showPreview(addition?t("Хранение файлов и временных данных","File and temporary data storage"):t("Встроенный шаблон · Ubuntu AI","Built-in template · Ubuntu AI"),values[choice[0]],notice,
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
                t("Применение заменит текст выбранного файла. Предыдущая редакция останется в истории. Несохранённый черновик будет заменён.","Applying replaces the selected file text. Its previous revision stays in history. An unsaved draft will be replaced."),apply,()->save(value));
        }
        AlertDialog showPreview(String title,String value,String notice,String apply,Runnable commit){return showPreview(title,value,notice,apply,commit,null);}
        AlertDialog showPreview(String title,String value,String notice,String apply,Runnable commit,View header){
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
                .setPositiveButton(apply,(d,w)->{if(!closed&&!busy)commit.run();}).setNegativeButton(t("Отмена","Cancel"),null).create();
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
            LinearLayout form=column();EditText path=new EditText(activity);path.setText(project);path.setSingleLine(true);path.setTextColor(WorkspaceDialogs.TEXT);path.setTag("instructions-project-input");form.addView(path);
            AlertDialog chooser=WorkspaceDialogs.builder(activity).setTitle(t("Проект Ubuntu AI","Ubuntu AI project")).setView(form).setPositiveButton(t("Открыть","Open"),null)
                .setNeutralButton(t("Папка","Folder"),(d,w)->{
                    WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment="proot:ubuntu-ai";
                    browser=new WorkspaceFileBrowser(activity,p,project,picked->{if(!closed){project=picked;load();}});browser.show();
                }).setNegativeButton(t("Отмена","Cancel"),null).create();chooser.show();
            chooser.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String chosen=path.getText().toString();if(!chosen.startsWith("/")||chosen.indexOf('\0')>=0){path.setError(t("Нужен абсолютный путь","Absolute path required"));return;}chooser.dismiss();project=chosen;load();});
        }
        void diagnostics(JSONObject result){
            body.removeAllViews();ScrollView scroll=new ScrollView(activity);LinearLayout rows=column();scroll.addView(rows);body.addView(scroll,new LinearLayout.LayoutParams(-1,-1));
            JSONObject cli=result.optJSONObject("cli");String version=cli==null||cli.isNull("version")?t("не определена","unknown"):cli.optString("version");
            rows.addView(text("Codex · "+version));
            boolean verified=cli!=null&&cli.optBoolean("behaviorVerified");
            rows.addView(text(verified?t("Для этой версии проверено: общий файл перечитывается между запросами; проектные правила — при новом запуске с продолжением истории.","Tested for this version: shared rules reload between requests; project rules reload in a new process that resumes history."):t("Поведение этой версии не проверено. Сохранение текста доступно; для повторного чтения правил перезапустите агента с продолжением диалога. Не рассчитывайте на применение в уже работающем чате.","This version's behavior is unverified. Saving remains available; restart the agent and resume history to reread rules. Do not rely on live changes in the current chat.")));

            rows.addView(text(t("Общие правила берутся из CODEX_HOME выбранной Ubuntu AI. Проектные правила уточняют их по дереву проекта; AGENTS.override.md имеет приоритет в своём каталоге. Это механизм AGENTS, а не developerInstructions.","Shared rules come from CODEX_HOME in the selected Ubuntu AI. Project rules refine them along the project tree; AGENTS.override.md takes precedence in its directory. This is AGENTS, not developerInstructions.")));
            rows.addView(text(t("Полный набор файлов читается новым процессом агента, в том числе при возобновлении истории. Выполняющийся запрос не изменяется. На Codex 0.163/0.164 проверено перечитывание общего файла между запросами; проектные правила сохранялись до нового запуска. Другие версии могут отличаться. Редактор не меняет внутренние правила сервиса и модели.","A new agent process reads the complete file set, including when resuming history. A running request is unchanged. Tests on Codex 0.163/0.164 observed shared-rule reload between requests; project rules stayed until a new launch. Other versions may differ. The editor does not change internal service or model rules.")));
            rows.addView(text(t("Проверка показывает файловые источники нового стандартного запуска Ubuntu AI. Настройки config.toml, выбранные профили и пользовательская команда подключения могут дополнительно влиять на запуск. Здесь отмечается наличие настроек, без раскрытия их содержимого; параметры уже работающего процесса не подменяются.","This checks file sources for a new standard Ubuntu AI launch. config.toml settings, selected profiles and a custom connection executable can additionally affect startup. Only setting presence is shown, without their contents; a running process is not reconfigured.")));
            TextView details=text("");details.setTextIsSelectable(true);details.setVisibility(View.GONE);try{details.setText(result.toString(2));}catch(JSONException e){details.setText(t("Сведения недоступны","Details unavailable"));}
            action(rows,t("Пути и технические сведения ▾","Paths and technical details ▾"),"instructions-details-toggle",()->details.setVisibility(details.getVisibility()==View.GONE?View.VISIBLE:View.GONE));rows.addView(details);
        }
    }
}
