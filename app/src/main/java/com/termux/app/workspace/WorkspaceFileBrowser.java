package com.termux.app.workspace;

import android.app.AlertDialog;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Environment;
import android.text.*;
import android.view.*;
import android.widget.*;
import androidx.core.content.FileProvider;
import com.termux.app.TermuxActivity;
import com.termux.R;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/** Project viewer and directory picker share one environment-aware backend. */
final class WorkspaceFileBrowser {
    interface DirectoryChoice {void selected(String path);}
    interface FileChoice {void selected(WorkspaceStore.Session source,ProjectFiles.Entry entry);}
    private FileChoice fileChoice;
    private String rootBoundary;
    void setRootBoundary(String root){rootBoundary=root;}
    private java.util.function.BiConsumer<WorkspaceStore.Session,List<ProjectFiles.Entry>> fileChoices;
    private final LinkedHashMap<String,ProjectFiles.Entry> selectedFiles=new LinkedHashMap<>();
    private boolean chatChoices,choosingMultiple;private int selectionLimit=50;
    void setChatFileChoices(java.util.function.BiConsumer<WorkspaceStore.Session,List<ProjectFiles.Entry>> value,int limit){fileChoices=value;chatChoices=true;selectionLimit=Math.max(1,Math.min(ChatAttachments.MAX_COUNT,limit));}
    private final LinkedHashMap<String,ProjectFiles.Entry> transferSelection=new LinkedHashMap<>();
    private TextView transferButton,clearTransferButton;
    private boolean transfersEnabled(){return choice==null&&fileChoice==null&&fileChoices==null;}
    private void updateTransfer(){if(transferButton==null)return;boolean selected=!transferSelection.isEmpty();transferButton.setEnabled(selected&&!busy&&listing!=null);transferButton.setAlpha(selected?1f:.4f);transferButton.setSelected(selected);transferButton.setBackground(WorkspaceDialogs.background(activity,selected));clearTransferButton.setVisibility(selected?View.VISIBLE:View.GONE);clearTransferButton.setText("× "+transferSelection.size());clearTransferButton.setEnabled(!busy);if(dialog!=null&&transfersEnabled()){Button all=dialog.getButton(AlertDialog.BUTTON_NEUTRAL);if(all!=null){all.setVisibility(selected?View.VISIBLE:View.GONE);all.setEnabled(selected&&!busy&&listing!=null);}}}
    private long transferEpoch;
    private AlertDialog transferChoice;
    private void invalidateTransferChoice(){
        transferEpoch++;AlertDialog old=transferChoice;transferChoice=null;if(old!=null)old.dismiss();
    }
    private void bindTransferChoice(AlertDialog choice,long epoch){
        transferChoice=choice;choice.setOnDismissListener(d->{if(transferChoice==choice){transferChoice=null;if(transferEpoch==epoch)transferEpoch++;}});
    }
    private boolean acceptTransferChoice(long epoch){
        if(closed||busy||epoch!=transferEpoch||transferChoice==null)return false;
        transferChoice=null;return true;
    }
    private void toggleTransfer(ProjectFiles.Entry entry){if(busy)return;invalidateTransferChoice();if(transferSelection.containsKey(entry.path))transferSelection.remove(entry.path);else if(transferSelection.size()<ProjectFileTransfer.MAX_SELECTED)transferSelection.put(entry.path,entry);else{message.setText(t("Можно выбрать до 50 объектов","Select up to 50 entries"));return;}ScrollView scroll=(ScrollView)rows.getParent();int y=scroll.getScrollY();render();scroll.post(()->scroll.scrollTo(0,y));}

    private boolean visibleEntry(ProjectFiles.Entry e){return !(choice!=null&&!e.directory)&&ProjectFiles.visible(e,filter)&&e.name.toLowerCase(Locale.ROOT).contains(search.getText().toString().toLowerCase(Locale.ROOT));}
    private void selectAllVisible(){if(busy||listing==null||transferSelection.isEmpty())return;invalidateTransferChoice();boolean limited=false;for(ProjectFiles.Entry e:listing.entries){if(!visibleEntry(e)||transferSelection.containsKey(e.path))continue;if(transferSelection.size()>=ProjectFileTransfer.MAX_SELECTED){limited=true;break;}transferSelection.put(e.path,e);}ScrollView scroll=(ScrollView)rows.getParent();int y=scroll.getScrollY();render();scroll.post(()->scroll.scrollTo(0,y));if(limited)message.setText(t("Достигнут лимит: 50 выбранных объектов","Selection limit reached: 50 entries"));}

    void setFileChoices(java.util.function.BiConsumer<WorkspaceStore.Session,List<ProjectFiles.Entry>> value){fileChoices=value;}
    private String t(String ru,String en){return WorkspaceLanguage.code(activity).equals("ru")?ru:en;}
    private void updateSelection(){if(dialog!=null&&fileChoices!=null){Button select=dialog.getButton(AlertDialog.BUTTON_POSITIVE);select.setText(t("Загрузить выбранные","Upload selected")+" ("+selectedFiles.size()+")");select.setVisibility(!chatChoices||choosingMultiple?View.VISIBLE:View.GONE);select.setEnabled(!selectedFiles.isEmpty()&&!busy);}}
    private void chooseFile(ProjectFiles.Entry entry){
        if(fileChoices!=null&&chatChoices&&!choosingMultiple){fileChoices.accept(files.profile,Collections.singletonList(entry));dialog.dismiss();return;}
        if(fileChoices!=null){if(selectedFiles.containsKey(entry.path))selectedFiles.remove(entry.path);else if(selectedFiles.size()<selectionLimit)selectedFiles.put(entry.path,entry);else{message.setText(t("Можно выбрать до ","Select up to ")+selectionLimit+t(" файлов"," files"));return;}ScrollView viewport=(ScrollView)rows.getParent();int y=viewport.getScrollY();render();viewport.post(()->viewport.scrollTo(0,y));updateSelection();}
        else if(fileChoice!=null){fileChoice.selected(files.profile,entry);dialog.dismiss();}else openFile(entry);
    }
    void setFileChoice(FileChoice value){fileChoice=value;}
    private final TermuxActivity activity;
    private final ProjectFiles files;
    interface TransferBackend {
        boolean ready(List<ProjectFiles.Entry> entries,String destination,boolean move,boolean rename)throws Exception;
        void run(List<ProjectFiles.Entry> entries,String destination,boolean move,boolean rename)throws Exception;
        ProjectFiles.Listing list(String destination)throws Exception;
    }
    private final TransferBackend transferBackend;
    private final DirectoryChoice choice;
    private final WorkspaceMediaThumbnails thumbnails;private ScrollView viewport;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private Runnable onClosed;
    void setOnClosed(Runnable callback){onClosed=callback;}
    private AlertDialog dialog,sortDialog;
    private final List<WorkspaceTextEditor> editors=new ArrayList<>();
    private LinearLayout rows,filters;private TextView pathLabel,message;private EditText search;
    private ProjectFiles.Listing listing;private String path,filter=WorkspaceText.tr("Все");private volatile boolean closed;private boolean busy,transferring;
    WorkspaceFileBrowser(TermuxActivity activity,WorkspaceStore.Session profile,String path,DirectoryChoice choice) {
        this.activity=activity;this.files=new ProjectFiles(activity,profile);this.path=path;this.choice=choice;
        this.transferBackend=new TransferBackend(){
            public boolean ready(List<ProjectFiles.Entry> e,String d,boolean m,boolean r)throws Exception{return files.transferReady(e,d,m,r);}
            public void run(List<ProjectFiles.Entry> e,String d,boolean m,boolean r)throws Exception{files.transfer(e,d,m,r);}
            public ProjectFiles.Listing list(String d)throws Exception{return files.list(d);}
        };this.thumbnails=new WorkspaceMediaThumbnails(activity,profile);
    }
    void setServerKnownHosts(File file){files.setServerKnownHosts(file);}
    private int dp(int n){return WorkspaceDialogs.dp(activity,n);}
    private LinearLayout column(){LinearLayout v=new LinearLayout(activity);v.setOrientation(LinearLayout.VERTICAL);return v;}
    private LinearLayout row(){LinearLayout v=new LinearLayout(activity);v.setOrientation(LinearLayout.HORIZONTAL);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
    private TextView text(String label,int size){TextView t=new TextView(activity);t.setText(label);t.setTextSize(size);t.setTextColor(WorkspaceDialogs.TEXT);return t;}
    private TextView action(String label,Runnable run){
        TextView t;
        if(label.equals("↑")||label.equals("⌂")){
            WorkspaceIconButton icon=new WorkspaceIconButton(activity);icon.setTextColor(WorkspaceDialogs.TEXT);icon.setMinHeight(dp(48));icon.setBackground(WorkspaceDialogs.background(activity,false));
            icon.setIcon(activity.getDrawable(label.equals("↑")?R.drawable.workspace_server_back:R.drawable.workspace_home).mutate(),dp(22));icon.setContentDescription(label.equals("↑")?WorkspaceText.tr("Родительская папка"):WorkspaceText.tr("Домашний каталог"));t=icon;
        }else t=WorkspaceDialogs.action(activity,label,0);
        t.setOnClickListener(v->run.run());return t;
    }
    static void styleFilter(Context context,TextView item,boolean selected){
        item.setSelected(selected);item.setBackground(WorkspaceDialogs.background(context,selected));item.setTextColor(selected?WorkspaceDialogs.ACCENT:WorkspaceDialogs.MUTED);item.setTypeface(null,selected?Typeface.BOLD:Typeface.NORMAL);
        item.setContentDescription(WorkspaceText.tr("Фильтр файлов: ")+item.getText()+(selected?WorkspaceText.tr(", выбран"):""));
    }
    void show() {
        LinearLayout body=column();body.setPadding(dp(12),0,dp(12),dp(8));
        body.addView(text(ProjectFiles.environmentLabel(files.profile),12));
        LinearLayout navigation=row();navigation.addView(action("↑",()->load(parent(path))),new LinearLayout.LayoutParams(dp(48),-2));
        pathLabel=text(path,12);pathLabel.setMaxLines(2);pathLabel.setEllipsize(TextUtils.TruncateAt.MIDDLE);navigation.addView(pathLabel,new LinearLayout.LayoutParams(0,-2,1));
        navigation.addView(action("⌂",()->load("~")),new LinearLayout.LayoutParams(dp(48),-2));
        WorkspaceIconButton sort=new WorkspaceIconButton(activity);sort.setIcon(activity.getDrawable(R.drawable.workspace_sort).mutate(),dp(22));sort.setTextColor(WorkspaceDialogs.TEXT);sort.setBackground(WorkspaceDialogs.background(activity,false));sort.setTag("workspace-file-sort");sort.setContentDescription(t("Сортировка файлов","Sort files"));sort.setOnClickListener(v->sortMenu());navigation.addView(sort,new LinearLayout.LayoutParams(dp(48),dp(48)));body.addView(navigation);
        search=new EditText(activity);search.setTextColor(WorkspaceDialogs.TEXT);search.setHintTextColor(WorkspaceDialogs.MUTED);search.setTextSize(14);search.setSingleLine(true);search.setHint(choice==null?WorkspaceText.tr("Найти файл или папку"):WorkspaceText.tr("Найти папку"));LinearLayout searchBar=row();searchBar.addView(search,new LinearLayout.LayoutParams(0,-2,1));
        if(transfersEnabled()){clearTransferButton=action("",()->{invalidateTransferChoice();transferSelection.clear();render();});clearTransferButton.setContentDescription(t("Снять всё выделение","Clear selection"));searchBar.addView(clearTransferButton,new LinearLayout.LayoutParams(-2,dp(48)));WorkspaceIconButton down=new WorkspaceIconButton(activity);down.setIcon(activity.getDrawable(R.drawable.workspace_diag_download).mutate(),dp(22));down.setTextColor(WorkspaceDialogs.ACCENT);down.setContentDescription(t("Копировать или переместить сюда","Copy or move here"));down.setOnClickListener(v->transferMenu());transferButton=down;searchBar.addView(down,new LinearLayout.LayoutParams(dp(48),dp(48)));}body.addView(searchBar);
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int b){}public void onTextChanged(CharSequence s,int a,int b,int c){render();}public void afterTextChanged(Editable e){}});
        if(choice==null){filters=row();HorizontalScrollView scroll=new HorizontalScrollView(activity);scroll.setHorizontalScrollBarEnabled(false);scroll.addView(filters);body.addView(scroll);for(String name:new String[]{WorkspaceText.tr("Все"),WorkspaceText.tr("Текст"),WorkspaceText.tr("Изображения"),"APK"}){TextView item=action(name,()->{filter=name;render();});item.setTextSize(13);item.setMinHeight(dp(42));item.setPadding(dp(12),dp(8),dp(12),dp(8));styleFilter(activity,item,name.equals(filter));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.setMarginEnd(dp(4));filters.addView(item,lp);}}
        message=text(WorkspaceText.tr("Загрузка…"),12);body.addView(message);rows=column();ScrollView scroll=new ScrollView(activity);viewport=scroll;scroll.addView(rows);scroll.setOnScrollChangeListener((v,x,y,ox,oy)->thumbnails.schedule(scroll));scroll.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->thumbnails.schedule(scroll));
        body.addView(scroll,new LinearLayout.LayoutParams(-1,Math.max(dp(160),Math.min(dp(400),activity.getResources().getDisplayMetrics().heightPixels/2))));
        AlertDialog.Builder builder=WorkspaceDialogs.builder(activity).setTitle(choice==null?WorkspaceText.tr("Файлы · ")+files.profile.name:WorkspaceText.tr("Рабочий каталог")).setView(body).setNegativeButton(WorkspaceText.tr("Закрыть"),null);
        if(transfersEnabled())builder.setNeutralButton(t("Выбрать всё","Select all"),null);
        if(fileChoices!=null)builder.setPositiveButton(t("Загрузить выбранные","Upload selected"),null);
        if(choice!=null)builder.setPositiveButton(WorkspaceText.tr("Выбрать папку"),null).setNeutralButton(WorkspaceText.tr("Создать папку"),null);
        dialog=builder.create();dialog.setOnDismissListener(d->close());dialog.setOnShowListener(d->{
            if(transfersEnabled())dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->selectAllVisible());
            if(choice!=null){dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{if(!busy&&listing!=null){choice.selected(listing.path);dialog.dismiss();}});dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->createFolder());}
            if(fileChoices!=null){dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{if(!busy&&!selectedFiles.isEmpty()){fileChoices.accept(files.profile,new ArrayList<>(selectedFiles.values()));dialog.dismiss();}});updateSelection();}
        });dialog.show();updateTransfer();
        if(ProjectFiles.environment(files.profile).equals("ssh")&&files.profile.sshAuth.equals("password")&&files.profile.sshEncryptedPassword.isEmpty())askPassword();else load(path);
    }
    private void askPassword(){EditText input=new EditText(activity);input.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);input.setSaveEnabled(false);
        WorkspaceDialogs.builder(activity).setTitle(WorkspaceText.tr("Пароль SSH для файлов")).setView(input).setPositiveButton(WorkspaceText.tr("Подключиться"),(d,w)->{char[] secret=new char[input.length()];TextUtils.getChars(input.getText(),0,input.length(),secret,0);files.setPassword(secret);Arrays.fill(secret,'\0');input.setText("");load(path);}).setNegativeButton(WorkspaceText.tr("Отмена"),(d,w)->dialog.dismiss()).show();}
    private interface Work {void run() throws Exception;}
    private void execute(Work work){if(closed||busy)return;busy=true;updateSelection();updateTransfer();message.setText(transferring?t("Выполняется операция с файлами…","Processing files…"):WorkspaceText.tr("Загрузка…"));if(choice!=null)dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        worker.execute(()->{try{work.run();}catch(Exception e){activity.runOnUiThread(()->{if(!closed)message.setText(e.getMessage()==null?WorkspaceText.tr("Не удалось открыть файлы"):e.getMessage());});}finally{activity.runOnUiThread(()->{busy=false;if(transferring){transferring=false;if(!closed){dialog.setCancelable(true);dialog.setCanceledOnTouchOutside(true);dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(true);}}if(!closed&&choice!=null)dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(listing!=null);if(!closed){updateSelection();updateTransfer();}});}});}
    private void load(String requested){if(closed||busy)return;invalidateTransferChoice();if(rootBoundary!=null){try{String root=new File(rootBoundary).getCanonicalPath();String candidate=new File(requested.equals("~")?rootBoundary:requested).getCanonicalPath();requested=candidate.equals(root)||candidate.startsWith(root+"/")?candidate:rootBoundary;}catch(IOException e){return;}}if(busy)return;thumbnails.reset();listing=null;rows.removeAllViews();final String targetPath=requested;execute(()->{ProjectFiles.Listing result=files.list(targetPath);activity.runOnUiThread(()->{if(closed)return;listing=result;path=result.path;pathLabel.setText(path);render();});});}
    static String parent(String path){int slash=path.lastIndexOf('/');return slash<=0?"/":path.substring(0,slash);}
    private void render(){
        if(filters!=null)for(int i=0;i<filters.getChildCount();i++){TextView t=(TextView)filters.getChildAt(i);styleFilter(activity,t,t.getText().toString().equals(filter));}
        if(rows==null||listing==null)return;thumbnails.reset();rows.removeAllViews();int count=0;
        for(ProjectFiles.Entry e:FileSort.ordered(listing.entries,FileSort.read(activity))){if(!visibleEntry(e))continue;
            TextView item=action((e.directory?"▸  ":"▤  ")+e.name+(e.directory?"":"\n"+android.text.format.Formatter.formatShortFileSize(activity,e.size)),()->{if(e.directory)load(e.path);else chooseFile(e);});
            if(!e.directory&&fileChoices!=null&&(!chatChoices||choosingMultiple)){item.setText((selectedFiles.containsKey(e.path)?"☑  ":"☐  ")+e.name+"\n"+android.text.format.Formatter.formatShortFileSize(activity,e.size));item.setSelected(selectedFiles.containsKey(e.path));item.setBackground(WorkspaceDialogs.background(activity,item.isSelected()));}
            item.setContentDescription((e.directory?WorkspaceText.tr("Папка "):WorkspaceText.tr("Файл "))+e.name);
            LinearLayout line=row();line.setOnClickListener(v->{if(e.directory)load(e.path);else chooseFile(e);});
            if(chatChoices&&choosingMultiple&&!e.directory){
                item.setText(e.name+"\n"+android.text.format.Formatter.formatShortFileSize(activity,e.size));CheckBox selected=new CheckBox(activity);selected.setChecked(selectedFiles.containsKey(e.path));selected.setContentDescription(t("Выбрать ","Select ")+e.name);selected.setOnClickListener(v->{chooseFile(e);selected.setChecked(selectedFiles.containsKey(e.path));});line.addView(selected,new LinearLayout.LayoutParams(dp(44),dp(48)));
            }
            if(chatChoices&&!e.directory){
                View.OnLongClickListener mark=v->{if(busy)return true;choosingMultiple=true;chooseFile(e);return true;};item.setOnLongClickListener(mark);line.setOnLongClickListener(mark);
            }
            if(transfersEnabled()){boolean checked=transferSelection.containsKey(e.path);item.setSelected(checked);item.setBackground(WorkspaceDialogs.background(activity,checked));item.setOnLongClickListener(v->{toggleTransfer(e);return true;});line.setOnLongClickListener(v->{toggleTransfer(e);return true;});
                if(!transferSelection.isEmpty()){CheckBox check=new CheckBox(activity);check.setChecked(checked);check.setContentDescription(t("Выбрать ","Select ")+e.name);check.setOnClickListener(v->toggleTransfer(e));line.addView(check,new LinearLayout.LayoutParams(dp(44),dp(48)));}
            }
            if(thumbnails.enabled()&&WorkspaceMediaThumbnails.media(e)){
                FrameLayout frame=new FrameLayout(activity);ImageView preview=new ImageView(activity);boolean video=ProjectFiles.mime(e.name).startsWith("video/");preview.setImageResource(video?R.drawable.workspace_media_video:R.drawable.workspace_media_photo);preview.setScaleType(ImageView.ScaleType.CENTER_CROP);preview.setContentDescription(t("Миниатюра ","Thumbnail ")+e.name);frame.addView(preview,new FrameLayout.LayoutParams(-1,-1));
                GradientDrawable shape=new GradientDrawable();shape.setColor(0xff152521);shape.setCornerRadius(dp(8));frame.setBackground(shape);frame.setClipToOutline(true);
                if(video){ImageView play=new ImageView(activity);play.setImageResource(R.drawable.workspace_play);play.setColorFilter(0xffffffff);play.setPadding(dp(3),dp(3),dp(3),dp(3));GradientDrawable circle=new GradientDrawable();circle.setColor(0xcc000000);circle.setShape(GradientDrawable.OVAL);play.setBackground(circle);FrameLayout.LayoutParams overlay=new FrameLayout.LayoutParams(dp(22),dp(22),Gravity.BOTTOM|Gravity.END);overlay.setMargins(dp(3),dp(3),dp(3),dp(3));frame.addView(play,overlay);}
                LinearLayout.LayoutParams tile=new LinearLayout.LayoutParams(dp(64),dp(64));tile.setMargins(dp(2),dp(4),dp(6),dp(4));line.addView(frame,tile);thumbnails.bind(line,preview,e);
            }
            line.addView(item,new LinearLayout.LayoutParams(0,-2,1));rows.addView(line);count++;}
        if(viewport!=null)thumbnails.schedule(viewport);
        updateTransfer();message.setText(count==0?WorkspaceText.tr("Нет подходящих файлов или папок"):WorkspaceText.tr("Открыт каталог выбранной среды · ")+count);
    }
    private void sortMenu(){
        if(closed)return;if(sortDialog!=null)sortDialog.dismiss();
        sortDialog=WorkspaceDialogs.builder(activity).setTitle(t("Сортировка","Sort files"))
            .setSingleChoiceItems(FileSort.labels(activity),FileSort.read(activity).ordinal(),(d,which)->{
                if(closed)return;FileSort.save(activity,FileSort.Order.values()[which]);d.dismiss();render();if(viewport!=null)viewport.scrollTo(0,0);
            }).setNegativeButton(t("Отмена","Cancel"),null).create();sortDialog.show();
    }
    private void transferMenu(){
        if(busy||listing==null||transferSelection.isEmpty())return;
        invalidateTransferChoice();final long epoch=transferEpoch;
        final List<ProjectFiles.Entry> entries=new ArrayList<>(transferSelection.values());final String destination=listing.path;
        AlertDialog menu=WorkspaceDialogs.builder(activity).setTitle(t("В текущую папку","Into current folder"))
            .setMessage(destination+"\n"+t("Выбрано: ","Selected: ")+entries.size())
            .setPositiveButton(t("Копировать сюда","Copy here"),(d,w)->{if(acceptTransferChoice(epoch))prepareTransfer(entries,destination,false,epoch);})
            .setNeutralButton(t("Переместить сюда","Move here"),(d,w)->{if(acceptTransferChoice(epoch))prepareTransfer(entries,destination,true,epoch);})
            .setNegativeButton(t("Назад","Back"),null).create();bindTransferChoice(menu,epoch);menu.setCanceledOnTouchOutside(false);menu.show();
    }
    private void prepareTransfer(List<ProjectFiles.Entry> entries,String destination,boolean move,long epoch){execute(()->{
        boolean ready=transferBackend.ready(entries,destination,move,false);boolean renameReady=ready||transferBackend.ready(entries,destination,move,true);
        activity.runOnUiThread(()->{if(closed||epoch!=transferEpoch)return;if(!renameReady){message.setText(t("Операция недоступна. Проверьте доступность путей и права. Нельзя выбирать папку вместе с её содержимым или помещать её внутрь самой себя.","Operation unavailable. Check paths and permissions. A folder cannot be selected together with its contents or placed inside itself."));return;}
            String explanation=destination+"\n\n"+(ready?t("Существующие объекты не заменяются. При прерывании уже завершённые действия сохраняются.","Existing entries are not replaced. Completed actions remain if interrupted."):t("Есть совпадающие имена. Добавить к новым именам (2), (3)…? Существующие объекты останутся без изменений.","Some names already exist. Add (2), (3)… to new names? Existing entries will remain unchanged."));
            AlertDialog confirm=WorkspaceDialogs.builder(activity).setTitle(move?t("Переместить выбранное?","Move selection?"):t("Копировать выбранное?","Copy selection?")).setMessage(explanation)
                .setPositiveButton(t("Продолжить","Continue"),(d,w)->{if(acceptTransferChoice(epoch)){transferEpoch++;performTransfer(entries,destination,move,!ready);}}).setNegativeButton(t("Назад","Back"),null).create();bindTransferChoice(confirm,epoch);confirm.setCanceledOnTouchOutside(false);confirm.show();
        });
    });}
    private void performTransfer(List<ProjectFiles.Entry> entries,String destination,boolean move,boolean rename){if(closed||busy)return;transferring=true;dialog.setCancelable(false);dialog.setCanceledOnTouchOutside(false);dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);execute(()->{
        String outcome;try{transferBackend.run(entries,destination,move,rename);outcome=move?t("Перемещено: ","Moved: ")+entries.size():t("Скопировано: ","Copied: ")+entries.size();}
        catch(ProjectFiles.TransferFailure failure){outcome=t("Завершение операции не подтверждено. Получено подтверждений: ","Operation completion is unconfirmed. Acknowledged entries: ")+failure.acknowledged+" / "+entries.size()+t(". Действий могло завершиться больше; операция может ещё выполняться в окружении или на сервере. Перед повтором проверьте исходную и конечную папки. Автоматического повтора нет.",". More actions may have completed; work may still be running in the environment or on the server. Check both source and destination before retrying. There is no automatic retry.");}
        ProjectFiles.Listing refreshed=null;try{refreshed=transferBackend.list(destination);}catch(Exception ignored){}final ProjectFiles.Listing result=refreshed;final String status=outcome;
        activity.runOnUiThread(()->{if(closed)return;transferSelection.clear();if(result!=null){listing=result;path=result.path;pathLabel.setText(path);}render();message.setText(status);});
    });}
    private void createFolder(){if(busy||listing==null)return;EditText input=new EditText(activity);input.setSingleLine(true);input.setHint(WorkspaceText.tr("Название новой папки"));
        AlertDialog create=WorkspaceDialogs.builder(activity).setTitle(WorkspaceText.tr("Новая пустая папка")).setView(input).setPositiveButton(WorkspaceText.tr("Создать"),null).setNegativeButton(WorkspaceText.tr("Отмена"),null).create();
        create.setOnShowListener(d->create.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String name=input.getText().toString();if(name.trim().isEmpty()||name.contains("/")||name.equals(".")||name.equals("..")){input.setError(WorkspaceText.tr("Введите имя одной папки"));return;}create.dismiss();execute(()->{ProjectFiles.Listing result=files.createDirectory(path,name);activity.runOnUiThread(()->{if(!closed){listing=result;path=result.path;pathLabel.setText(path);render();}});});}));create.show();}
    private void openFile(ProjectFiles.Entry e){String mime=ProjectFiles.mime(e.name);List<String> actions=new ArrayList<>();if(mime.equals("text/plain")||mime.startsWith("image/"))actions.add(WorkspaceText.tr("Просмотреть"));if(mime.equals("text/plain"))actions.add(WorkspaceText.tr("Редактировать"));Collections.addAll(actions,WorkspaceText.tr("Поделиться"),WorkspaceText.tr("Копировать путь"),WorkspaceText.tr("В Download"));
        WorkspaceDialogs.builder(activity).setCustomTitle(fileHeading(e)).setItems(actions.toArray(new String[0]),(d,w)->{String a=actions.get(w);if(a.equals(WorkspaceText.tr("Копировать путь"))){((android.content.ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText(WorkspaceText.tr("Путь файла"),e.path));activity.showToast(WorkspaceText.tr("Путь скопирован"),false);}else if(a.equals(WorkspaceText.tr("Просмотреть")))preview(e,mime);else if(a.equals(WorkspaceText.tr("Редактировать")))edit(e);else export(e,a.equals(WorkspaceText.tr("В Download")));}).setNegativeButton(WorkspaceText.tr("Закрыть"),null).show();}
    private View fileHeading(ProjectFiles.Entry e){LinearLayout heading=column();heading.setPadding(dp(20),dp(16),dp(20),dp(8));heading.addView(text(e.name,17));heading.addView(text(android.text.format.Formatter.formatShortFileSize(activity,e.size),12));return heading;}
    String serverProfileId(){return files.profile.id;}
    void close(){if(closed)return;closed=true;invalidateTransferChoice();if(sortDialog!=null)sortDialog.dismiss();thumbnails.close();if(dialog!=null&&dialog.isShowing())dialog.dismiss();for(WorkspaceTextEditor editor:editors)editor.close();editors.clear();try{files.close();}finally{worker.shutdownNow();listing=null;selectedFiles.clear();transferSelection.clear();Runnable callback=onClosed;onClosed=null;if(callback!=null)callback.run();}}
    private void preview(ProjectFiles.Entry e,String mime){execute(()->{
        if(mime.startsWith("image/")){if(e.size>16L*1024*1024)throw new IOException(WorkspaceText.tr("Изображение больше 16 МБ; используйте экспорт"));File temporary=cache(e.name);files.export(e,temporary,16L*1024*1024);
            BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;BitmapFactory.decodeFile(temporary.getPath(),bounds);BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=1;while(bounds.outWidth/options.inSampleSize>2048||bounds.outHeight/options.inSampleSize>2048)options.inSampleSize*=2;
            Bitmap bitmap=BitmapFactory.decodeFile(temporary.getPath(),options);temporary.delete();if(bitmap==null)throw new IOException(WorkspaceText.tr("Не удалось прочитать изображение"));activity.runOnUiThread(()->{if(closed){bitmap.recycle();return;}ImageView image=new ImageView(activity);image.setAdjustViewBounds(true);image.setImageBitmap(bitmap);AlertDialog view=WorkspaceDialogs.builder(activity).setTitle(e.name).setView(image).setNegativeButton(WorkspaceText.tr("Закрыть"),null).create();view.setOnDismissListener(d->{image.setImageDrawable(null);bitmap.recycle();});view.show();});
        }else{String value=new String(files.preview(e),StandardCharsets.UTF_8);activity.runOnUiThread(()->{if(closed)return;TextView t=text(value,13);t.setTypeface(Typeface.MONOSPACE);t.setTextIsSelectable(true);t.setPadding(dp(14),dp(12),dp(14),dp(12));ScrollView scroll=new ScrollView(activity);scroll.addView(t);WorkspaceDialogs.builder(activity).setTitle(e.name).setView(scroll).setNegativeButton(WorkspaceText.tr("Закрыть"),null).show();});}
        activity.runOnUiThread(()->{if(!closed)message.setText(WorkspaceText.tr("Просмотр: ")+e.name);});});}
    private void edit(ProjectFiles.Entry entry){execute(()->{
        TextFileEdits.Snapshot snapshot=files.editRead(entry);
        activity.runOnUiThread(()->{if(closed)return;WorkspaceTextEditor editor=new WorkspaceTextEditor(activity,files,worker,snapshot,()->{if(!closed)load(path);});editors.add(editor);editor.show();});
    });}
    private File cache(String name)throws IOException {File dir=new File(activity.getCacheDir(),"workspace-share/"+UUID.randomUUID());if(!dir.mkdirs())throw new IOException(WorkspaceText.tr("Не удалось создать временный файл"));return new File(dir,name);}
    private static void releaseCache(File file){file.delete();file.getParentFile().delete();}
    private void export(ProjectFiles.Entry e,boolean download){execute(()->{
        File temporary=cache(e.name);boolean sharePending=false;
        try{
            files.export(e,temporary,ProjectFiles.MAX_EXPORT);if(closed)return;
            if(download){
                final File destination;
                File directory=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                try(InputStream input=new FileInputStream(temporary)){
                    destination=ProjectFileExport.save(input,directory,e.name,ProjectFiles.MAX_EXPORT,()->closed);
                }
                activity.runOnUiThread(()->{if(!closed){message.setText(WorkspaceText.tr("Сохранено: ")+destination.getName());activity.showToast(WorkspaceText.tr("Файл сохранён в Download"),false);}});
            }else{
                Uri uri=FileProvider.getUriForFile(activity,activity.getPackageName()+".workspace.files",temporary);
                activity.runOnUiThread(()->{
                    if(closed){releaseCache(temporary);return;}
                    Intent intent=new Intent(Intent.ACTION_SEND).setType(ProjectFiles.mime(e.name)).putExtra(Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.setClipData(ClipData.newRawUri(WorkspaceText.tr("Файл проекта"),uri));
                    try{activity.startActivity(Intent.createChooser(intent,WorkspaceText.tr("Поделиться файлом")));}
                    catch(ActivityNotFoundException error){releaseCache(temporary);message.setText(WorkspaceText.tr("Нет приложения для отправки файла"));}
                });
                // Retain a successful share for the recipient's later URI read.
                sharePending=true;
            }
        }finally{if(!sharePending)releaseCache(temporary);}
    });}
}
