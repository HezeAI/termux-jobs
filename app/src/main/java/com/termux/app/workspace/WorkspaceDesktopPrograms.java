package com.termux.app.workspace;

import android.app.AlertDialog;
import android.os.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import com.termux.app.TermuxActivity;
import com.termux.R;
import com.termux.x11.DesktopPrograms;
import java.util.*;
import java.util.function.BiConsumer;

/** Local navigation and paged catalog; search updates only the program list. */
final class WorkspaceDesktopPrograms {
    private final TermuxActivity activity;
    private final BiConsumer<String,String> request;
    private final Runnable back;
    private final java.util.function.Consumer<String> maintenance;
    private TextView repairButton,activeCard;
    private final Set<CheckBox> dnsChecks=Collections.newSetFromMap(new WeakHashMap<CheckBox,Boolean>());
    private final Set<CheckBox> compatChecks=Collections.newSetFromMap(new WeakHashMap<CheckBox,Boolean>());
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Map<String,TextView> buttons=new LinkedHashMap<>();
    private final Map<TextView,DesktopProgramProgress> inline=new WeakHashMap<>();
    private final Map<TextView,ImageButton> trashButtons=new WeakHashMap<>();
    private final Map<TextView,String> cardIds=new WeakHashMap<>();
    private final List<Bundle> rows=new ArrayList<>();
    private AlertDialog dialog;private DesktopInstallUi progress;
    private TextView message,errorDetails,refresh,recommendedButton,installedButton,catalogButton,pageLabel;
    private ImageButton previous,next;
    private EditText search;private BaseAdapter adapter;
    private Bundle state=new Bundle(),items=new Bundle();
    private WorkspaceStore.Session profile;
    private boolean recommended,installedOnly,catalogView,awaiting;private int expectedJob;private String query="",installName="";private int offset,total,revision=-1;
    private final Runnable searchTask=()->loadPage();
    WorkspaceDesktopPrograms(TermuxActivity activity,BiConsumer<String,String> request,Runnable back){this(activity,request,back,op->{});}
    WorkspaceDesktopPrograms(TermuxActivity activity,BiConsumer<String,String> request,Runnable back,java.util.function.Consumer<String> maintenance){this.activity=activity;this.request=request;this.back=back;this.maintenance=maintenance;}
    void openInstalled(WorkspaceStore.Session p,Bundle current){close();awaiting=false;installName="";profile=p;state=new Bundle();items=new Bundle();query="";offset=0;revision=-1;if(current!=null&&target().equals(current.getString("target","")))state=new Bundle(current);if(state.getBundle("items")!=null)items=new Bundle(state.getBundle("items"));show(false,true);if(!busy())loadInstalled();}
    WorkspaceStore.Session profile(){return profile;}
    private String target(){return profile==null?"":WorkspaceDesktop.target(profile);}
    boolean busy(){String p=state.getString("phase","");return p.equals("checking")||p.equals("installing")||p.equals("cancelling")||p.equals("cataloguing")||p.equals("removing");}
    boolean visible(){return dialog!=null&&dialog.isShowing();}
    boolean polling(){return visible()&&busy();}
    void open(WorkspaceStore.Session p,Bundle current){
        close();awaiting=false;installName="";profile=p;state=new Bundle();items=new Bundle();query="";offset=0;revision=-1;
        if(current!=null&&target().equals(current.getString("target","")))state=new Bundle(current);
        if(state.getBundle("items")!=null)items=new Bundle(state.getBundle("items"));
        show(false);
        // Opening the menu is navigation, not a catalog scan.
        render();
    }
    private void show(boolean recommended){show(recommended,false);}
    private void openCatalog(){query="";offset=0;show(false,false,true);if(state.getBoolean("catalogReady",false))loadPage();else if(!busy())catalog();}
    private void show(boolean recommended,boolean installed){show(recommended,installed,false);}
    private void show(boolean recommended,boolean installed,boolean catalog){
        if(dialog!=null)dialog.dismiss();main.removeCallbacks(searchTask);this.recommended=recommended;this.installedOnly=installed;this.catalogView=catalog;refresh=null;recommendedButton=null;installedButton=null;catalogButton=null;adapter=null;search=null;previous=null;next=null;pageLabel=null;if(!recommended)revision=-1;buttons.clear();inline.clear();cardIds.clear();trashButtons.clear();rows.clear();repairButton=null;activeCard=null;compatChecks.clear();dnsChecks.clear();
        LinearLayout body=AgentStatusUi.body(activity);
        AgentStatusUi.info(body,tr("Окружение: ","Environment: ")+WorkspaceEnvironments.label(target()));
        message=AgentStatusUi.info(body,"");message.setTag("desktop-programs-status");
        errorDetails=AgentStatusUi.row(body,tr("Подробнее","Details"),"programs-error-details",()->{LinearLayout content=AgentStatusUi.body(activity);AgentStatusUi.info(content,state.getString("detail",""));ScrollView scroll=AgentStatusUi.scroll(content);WorkspaceDialogs.builder(activity).setTitle(tr("Причина ошибки","Error details")).setView(scroll).setPositiveButton(tr("Назад","Back"),null).show();});errorDetails.setVisibility(View.GONE);
        if(!recommended&&!installedOnly&&!catalogView){
            Bundle pending=state.getBundle("resumePrograms");Set<String> ids=new LinkedHashSet<>();
            if(busy()&&state.getString("action","").equals("install"))ids.add(state.getString("program",""));
            if(pending!=null)ids.addAll(pending.keySet());
            for(String id:ids){String name=id.startsWith("pkg:")?id.substring(4):DesktopPrograms.title(id,ru(),target());
                TextView button=card(body,name,"",id.startsWith("pkg:")?"desktop-package-"+id.substring(4):"desktop-program-"+id,()->confirm(id,name,""));buttons.put(id,button);activeCard=button;}
        }
        if(recommended){
            AgentStatusUi.info(body,tr("Рекомендуемые программы. Статус учитывает необходимые компоненты; новые программы требуют проверки.","Recommended programs. Status includes required components; new applications need testing."));
            refresh=AgentStatusUi.row(body,tr("Проверить установленные программы","Check installed programs"),"programs-refresh",this::check);
            for(String id:DesktopPrograms.RECOMMENDED){TextView button=card(body,DesktopPrograms.title(id,ru(),target()),description(id),"desktop-program-"+id,()->{if(!canResume(id)&&!items.containsKey(id))check();else confirm(id,DesktopPrograms.title(id,ru(),target()),description(id));});buttons.put(id,button);}
        }else if(!installedOnly&&!catalogView){
            recommendedButton=AgentStatusUi.row(body,tr("Рекомендуемые ›","Recommended ›"),"programs-recommended",()->{show(true);if(!busy()&&items.isEmpty())check(false);});
            installedButton=AgentStatusUi.row(body,tr("Установленные программы ›","Installed programs ›"),"programs-installed",()->{query="";offset=0;show(false,true);if(busy())loadPage();else loadInstalled();});
            catalogButton=AgentStatusUi.row(body,tr("Поиск и установка программ ›","Search and install programs ›"),"programs-catalog",this::openCatalog);
        }else{
            if(installedOnly&&DesktopPrograms.supportsMaintenance(target())){
                repairButton=AgentStatusUi.row(body,tr("Исправить права ярлыков","Repair launcher permissions"),"programs-repair-launchers",()->{if(!busy())maintenance.accept("repair-launchers");});
            }
            refresh=AgentStatusUi.row(body,installedOnly?tr("Проверить установленные программы","Check installed programs"):tr("Подготовить и обновить каталог","Prepare and refresh catalog"),"programs-refresh",()->{if(installedOnly)loadInstalled(true);else confirmSetup();});
            search=new EditText(activity);search.setSingleLine(true);search.setTextColor(WorkspaceDialogs.TEXT);search.setHintTextColor(WorkspaceDialogs.MUTED);search.setHint(tr("Поиск по названию и описанию","Search names and descriptions"));search.setTag("desktop-program-search");search.setFilters(new InputFilter[]{new InputFilter.LengthFilter(200)});search.setText(query);body.addView(search,new LinearLayout.LayoutParams(-1,-2));
            search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){query=s.toString();offset=0;rows.clear();total=0;adapter.notifyDataSetChanged();render();main.removeCallbacks(searchTask);main.postDelayed(searchTask,250);}public void afterTextChanged(Editable e){}});
            ListView list=new ListView(activity);list.setTag("desktop-program-catalog-list");list.setDividerHeight(dp(8));
            adapter=new BaseAdapter(){public int getCount(){return rows.size();}public Object getItem(int i){return rows.get(i);}public long getItemId(int i){return i;}
                public View getView(int i,View old,ViewGroup parent){Bundle entry=rows.get(i);LinearLayout holder=new LinearLayout(activity);holder.setOrientation(LinearLayout.VERTICAL);
                    String pkg=entry.getString("package","");String description=entry.getString("summary","")+"\n"+pkg+"\n"+(entry.getBoolean("snap")?tr("Требует Snap — установка здесь недоступна.","Requires Snap — installation is unavailable here."):tr("Работа в нашем окружении не проверена.","Not tested in our environment."));
                    TextView button=card(holder,entry.getString("name",pkg),description,"desktop-package-"+pkg,()->{if(!busy())confirm("pkg:"+pkg,entry.getString("name",pkg),description);});
                    if(installedOnly){button.setText(tr("Установлено","Installed"));AgentStatusUi.available(button,false);updateTrash(button,true);return holder;}
                    button.setText(entry.getBoolean("installed")?tr("Установлено","Installed"):entry.getBoolean("available")?tr("Установить","Install"):tr("Недоступно","Unavailable"));AgentStatusUi.available(button,!busy()&&!entry.getBoolean("installed")&&entry.getBoolean("available"));updateCard(button);updateTrash(button,entry.getBoolean("installed"));return holder;}};
            list.setAdapter(adapter);body.addView(list,new LinearLayout.LayoutParams(-1,dp(340)));
            LinearLayout pages=new LinearLayout(activity);pages.setGravity(Gravity.CENTER_VERTICAL);
            previous=arrow(false);previous.setContentDescription(tr("Предыдущие программы","Previous programs"));previous.setOnClickListener(v->{offset=Math.max(0,offset-30);loadPage();});pages.addView(previous,new LinearLayout.LayoutParams(dp(48),dp(48)));
            pageLabel=new TextView(activity);pageLabel.setTextColor(WorkspaceDialogs.MUTED);pageLabel.setGravity(Gravity.CENTER);pages.addView(pageLabel,new LinearLayout.LayoutParams(0,-2,1));
            next=arrow(true);next.setContentDescription(tr("Следующие программы","Next programs"));next.setOnClickListener(v->{offset+=30;loadPage();});pages.addView(next,new LinearLayout.LayoutParams(dp(48),dp(48)));body.addView(pages);
        }
        dialog=WorkspaceDialogs.builder(activity).setTitle(recommended?tr("Рекомендуемые","Recommended"):installedOnly?tr("Установленные программы","Installed programs"):catalogView?tr("Поиск и установка программ","Search and install programs"):tr("Программы рабочего стола","Desktop programs")).setView(recommended?AgentStatusUi.scroll(body):body).setNegativeButton(tr("Назад","Back"),(d,w)->{if(this.recommended||this.installedOnly||this.catalogView){query="";offset=0;show(false);}else{close();back.run();}}).create();dialog.show();render();
    }
    private ImageButton arrow(boolean forward){
        ImageButton button=new ImageButton(activity);button.setImageResource(R.drawable.workspace_next);button.setRotation(forward?0:180);
        button.setScaleType(ImageView.ScaleType.CENTER_INSIDE);button.setPadding(dp(12),dp(12),dp(12),dp(12));button.setBackground(WorkspaceDialogs.background(activity,false));
        button.setTag(forward?"desktop-program-next":"desktop-program-previous");return button;
    }
    private void available(View button,boolean enabled){button.setEnabled(enabled);button.setAlpha(enabled?1f:.45f);}
    private TextView card(LinearLayout body,String name,String description,String tag,Runnable click){
        LinearLayout card=new LinearLayout(activity);card.setGravity(Gravity.CENTER_VERTICAL);card.setPadding(dp(12),dp(10),dp(12),dp(10));card.setBackground(WorkspaceDialogs.background(activity,false));
        LinearLayout labels=new LinearLayout(activity);labels.setOrientation(LinearLayout.VERTICAL);
        TextView title=new TextView(activity);title.setText(name);title.setTextColor(WorkspaceDialogs.TEXT);title.setTextSize(14);labels.addView(title);
        TextView detail=new TextView(activity);detail.setText(description);detail.setTextColor(WorkspaceDialogs.MUTED);detail.setTextSize(12);detail.setPadding(0,dp(4),dp(8),0);labels.addView(detail);card.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
        TextView button=WorkspaceDialogs.action(activity,tr("Проверка…","Checking…"),0);button.setTextSize(12);button.setGravity(Gravity.CENTER);button.setMinHeight(dp(44));button.setTag(tag);button.setOnClickListener(v->{if(installingCard(tag)){stopInstall();}else click.run();});
        LinearLayout statusRow=new LinearLayout(activity);statusRow.setGravity(Gravity.CENTER_VERTICAL);
        statusRow.addView(button,new LinearLayout.LayoutParams(0,-2,1));
        String cardId=tag.startsWith("desktop-program-")?tag.substring(16):"pkg:"+tag.substring(16);
        String pkg=cardId.startsWith("pkg:")?cardId.substring(4):DesktopPrograms.mainPackage(cardId,target());
        if(!pkg.isEmpty()){
            ImageButton trash=new ImageButton(activity);trash.setImageResource(R.drawable.workspace_delete);trash.setScaleType(ImageView.ScaleType.CENTER_INSIDE);trash.setPadding(dp(10),dp(10),dp(10),dp(10));trash.setBackground(WorkspaceDialogs.background(activity,false));trash.setTag("desktop-remove-"+pkg);trash.setContentDescription(tr("Удалить ","Remove ")+pkg);trash.setVisibility(View.GONE);trash.setOnClickListener(v->confirmRemove(pkg,pkg));trashButtons.put(button,trash);statusRow.addView(trash,new LinearLayout.LayoutParams(dp(44),dp(44)));
        }
        if(DesktopPrograms.supportsFirefox(target())&&(tag.equals("desktop-program-browser")||tag.equals("desktop-package-firefox"))){
            LinearLayout actions=new LinearLayout(activity);actions.setOrientation(LinearLayout.VERTICAL);actions.setGravity(Gravity.CENTER_HORIZONTAL);actions.addView(statusRow,new LinearLayout.LayoutParams(-1,-2));
            CheckBox check=new CheckBox(activity);check.setText("PRoot");check.setTextSize(11);check.setTextColor(WorkspaceDialogs.TEXT);check.setButtonTintList(android.content.res.ColorStateList.valueOf(WorkspaceDialogs.TEXT));check.setMinHeight(dp(44));check.setTag("desktop-firefox-compat-checkbox");check.setContentDescription(tr("Совместимость Firefox с PRoot","Firefox PRoot compatibility"));check.setTooltipText(check.getContentDescription());
            check.setChecked(state.getBoolean("firefoxCompat",false));available(check,!busy()&&state.getBoolean("firefoxCompatKnown",false));check.setOnClickListener(v->{check.setChecked(state.getBoolean("firefoxCompat",false));if(!busy())maintenance.accept(state.getBoolean("firefoxCompat",false)?"firefox-compat-disable":"firefox-compat-enable");});compatChecks.add(check);actions.addView(check,new LinearLayout.LayoutParams(-2,-2));if(DesktopPrograms.supportsFirefox(target())){
                CheckBox dns=new CheckBox(activity);dns.setText("DNS HTTPS");dns.setTextSize(11);dns.setTextColor(WorkspaceDialogs.TEXT);dns.setButtonTintList(android.content.res.ColorStateList.valueOf(WorkspaceDialogs.TEXT));dns.setMinHeight(dp(44));dns.setTag("desktop-firefox-dns-checkbox");dns.setContentDescription(tr("DNS Firefox через HTTPS","Firefox DNS over HTTPS"));dns.setTooltipText(dns.getContentDescription());dns.setChecked(state.getBoolean("firefoxDns",false));available(dns,!busy()&&state.getBoolean("firefoxDnsKnown",false));dns.setOnClickListener(v->{dns.setChecked(state.getBoolean("firefoxDns",false));if(!busy())maintenance.accept(state.getBoolean("firefoxDns",false)?"firefox-dns-disable":"firefox-dns-enable");});dnsChecks.add(dns);actions.addView(dns,new LinearLayout.LayoutParams(-2,-2));
            }card.addView(actions,new LinearLayout.LayoutParams(dp(128),-2));
        }else card.addView(statusRow,new LinearLayout.LayoutParams(dp(128),-2));
        LinearLayout wrapper=new LinearLayout(activity);wrapper.setOrientation(LinearLayout.VERTICAL);wrapper.setBackground(WorkspaceDialogs.background(activity,false));card.setBackground(null);wrapper.addView(card,new LinearLayout.LayoutParams(-1,-2));
        DesktopProgramProgress line=new DesktopProgramProgress(activity);line.view.setPadding(dp(12),0,dp(12),dp(8));wrapper.addView(line.view,new LinearLayout.LayoutParams(-1,-2));inline.put(button,line);
        String id=tag.startsWith("desktop-program-")?tag.substring(16):"pkg:"+tag.substring(16);cardIds.put(button,id);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(8);body.addView(wrapper,lp);updateCard(button);return button;
    }
    private void loadInstalled(){loadInstalled(false);}
    private void loadInstalled(boolean refresh){if(busy())return;if(!refresh&&state.getBoolean("installedReady",false)){loadPage();return;}revision=-1;offset=0;rows.clear();if(adapter!=null)adapter.notifyDataSetChanged();start("checking",refresh?"programs-installed-refresh":"programs-installed","");}
    void setDns(boolean enabled){if(busy())return;installName=tr("DNS Firefox через HTTPS","Firefox DNS over HTTPS");start("installing",enabled?"programs-firefox-dns-on":"programs-firefox-dns-off","browser");}
    void setCompatibility(boolean enabled){if(busy())return;installName=tr("Совместимость Firefox","Firefox compatibility");start("installing",enabled?"programs-firefox-compat-on":"programs-firefox-compat-off","browser");}
    private void check(){check(true);}
    private void check(boolean refresh){if(busy())return;items=new Bundle();start("checking",refresh?"programs-check-refresh":"programs-check","");}
    private void catalog(){catalog(false);}
    private void catalog(boolean refresh){if(busy())return;revision=-1;offset=0;rows.clear();if(adapter!=null)adapter.notifyDataSetChanged();start("cataloguing",refresh?"programs-catalog-refresh":"programs-catalog","");}
    private void loadPage(){
        if((!catalogView&&!installedOnly)||!state.getBoolean(installedOnly?"installedReady":"catalogReady",false)||dialog==null||!dialog.isShowing())return;
        try{if(!busy())revision=state.getInt("catalogRevision",0);org.json.JSONObject q=new org.json.JSONObject();q.put("query",query);q.put("offset",offset);q.put("installed",installedOnly);request.accept("programs-page",q.toString());}catch(org.json.JSONException e){throw new IllegalStateException(e);}
    }
    private void confirmRemove(String pkg,String name){
        if(busy()||!DesktopPrograms.removable(pkg))return;
        AlertDialog confirm=WorkspaceDialogs.builder(activity).setTitle(tr("Удалить ","Remove ")+name+"?")
            .setMessage(tr("Окружение: ","Environment: ")+WorkspaceEnvironments.label(target())+"\n"+pkg+(target().equals("termux")&&pkg.equals("evince")?tr("\nСправка evince-help также будет удалена, если установлена.","\nThe evince-help documentation will also be removed if installed."):target().equals("opensuse")&&pkg.equals("evince")?tr("\nPDF-модуль evince-plugin-pdfdocument также будет удалён, если установлен.","\nThe evince-plugin-pdfdocument PDF module will also be removed if installed."):"")+"\n\n"+(pkg.equals("termux-jobs-gpu-test")?tr("Будут удалены Jobs GPU Test и его ярлыки. Отчёты и общие зависимости сохранятся. Перед удалением закройте тест.","Jobs GPU Test and its shortcuts will be removed. Reports and shared dependencies are retained. Close the test first."):tr("Будет удалён пакет программы. Общие зависимости, настройки и ваши документы сохранятся. Если пакет нужен другим программам, удаление будет отклонено. Перед удалением закройте эту программу.","The application package will be removed. Shared dependencies, settings and your documents are retained. Removal is refused if other packages require it. Close the application first.")))
            .setNegativeButton(tr("Назад","Back"),null).setPositiveButton(tr("Удалить","Remove"),(d,w)->{installName=name;start("removing","programs-remove","pkg:"+pkg);}).create();confirm.setCanceledOnTouchOutside(false);confirm.show();
    }
    private boolean nativeCatalog(){return target().equals("alpine")||target().equals("termux")||target().equals("fedora")||target().equals("opensuse")||target().equals("void")||target().equals("archlinux");}
    private String description(String id){
        if(id.equals("browser")&&nativeCatalog())return tr("Firefox из репозитория выбранного окружения. При падении вкладок можно включить совместимость PRoot.","Firefox from the selected environment's repository. Enable PRoot compatibility if tabs crash.");
        return DesktopPrograms.description(id,ru());
    }
    private String archNotice(){return target().equals("archlinux")?tr("\nArch Linux: будут также обновлены пакеты этого окружения (pacman -Syu).","\nArch Linux: this environment’s packages will also be upgraded (pacman -Syu). "):"";}
    private void confirmSetup(){
        if(busy())return;
        WorkspaceDialogs.builder(activity).setTitle(tr("Подготовить и обновить каталог?","Prepare and refresh catalog?"))
            .setMessage((nativeCatalog()?tr("Будут обновлены списки пакетов выбранного окружения. Каталог содержит программы и библиотеки; установка выбирается отдельно.","Refresh the selected environment package lists. The catalog includes applications and libraries; installation is separate."):tr("При необходимости в выбранное окружение будут установлены AppStream и привязки Python для AppStream и APT. Уже подготовленные компоненты переустанавливаться не будут. Затем обновятся метаданные программ из её репозиториев. Нужны интернет и свободное место. Это не устанавливает сами программы каталога.","Missing AppStream, Python and APT bindings will be installed in the selected environment. Existing components are retained, then repository application metadata is refreshed. Internet and free storage are required. Catalog applications themselves will not be installed."))+archNotice())
            .setNegativeButton(tr("Назад","Back"),null).setPositiveButton(tr("Продолжить","Continue"),(d,w)->{installName=DesktopPrograms.title("catalog-tools",ru());start("installing","programs-catalog-install","catalog-tools");}).show();
    }
    private boolean canResume(String id){Bundle pending=state.getBundle("resumePrograms");return pending!=null&&pending.containsKey(id);}
    private void confirm(String id,String name,String description){
        if(busy()||items.getBoolean(id,false))return;
        if(canResume(id)){installName=name;start("installing","programs-install",id);return;}
        AlertDialog confirm=WorkspaceDialogs.builder(activity).setTitle(tr("Установить ","Install ")+name+"?")
            .setMessage(description+"\n\n"+tr("Окружение: ","Environment: ")+WorkspaceEnvironments.label(target())+".\n"+tr("Потребуются интернет и место для программы и её зависимостей. После отмены может понадобиться восстановление пакетов.","Internet and storage for the program and dependencies are required. Cancelling may require package repair.")+archNotice())
            .setNegativeButton(tr("Назад","Back"),null).setPositiveButton(tr("Установить","Install"),(d,w)->{installName=name;start("installing","programs-install",id);}).create();confirm.setCanceledOnTouchOutside(false);confirm.show();
    }
    private void start(String phase,String op,String id){awaiting=true;expectedJob=state.getInt("job",0)+1;Bundle next=new Bundle(state);next.remove("catalogPage");next.remove("downloadBytes");next.remove("downloadTotal");next.putString("stage","preparing");next.putString("phase",phase);next.putString("action",op.startsWith("programs-")?op.substring(9):op);next.putString("program",id);next.putString("detail","");next.putInt("percent",-1);next.putLong("eta",-1);state=next;render();request.accept(op,id);}
    void accept(Bundle b){
        if(dialog==null||!dialog.isShowing()||!target().equals(b.getString("target","")))return;
        int job=b.getInt("job",0);if(job<state.getInt("job",0)||(awaiting&&job<expectedJob))return;awaiting=false;
        String phase=b.getString("phase","");boolean controlsChanged=!phase.equals(state.getString("phase",""))||job!=state.getInt("job",0);Bundle result=b.getBundle("items");if(result!=null)items=new Bundle(result);
        if(phase.equals("cancelled")||phase.equals("error")){String action=b.getString("action",state.getString("action",""));if(result==null&&(action.equals("install")||action.equals("remove")||action.equals("catalog-install")))items=new Bundle();}
        state=new Bundle(b);Bundle page=b.getBundle("catalogPage");boolean rebuild=false;
        if(page!=null&&(catalogView||installedOnly)&&installedOnly==page.getBoolean("installed",false)&&query.equals(page.getString("query",""))&&offset==page.getInt("offset",-1)){
            ArrayList<Bundle> values=page.getParcelableArrayList("rows");rows.clear();if(values!=null)rows.addAll(values);total=page.getInt("total",0);rebuild=true;
        }
        render();if(adapter!=null&&(rebuild||controlsChanged))adapter.notifyDataSetChanged();
        if(installedOnly&&phase.equals("complete")&&(b.getString("action","").equals("remove")||!state.getBoolean("installedReady",false))){loadInstalled();return;}
        if(recommended&&phase.equals("complete")&&items.isEmpty()&&!b.getString("action","").equals("check")){check(false);return;}
        int version=b.getInt("catalogRevision",0);if((catalogView||installedOnly)&&!busy()&&b.getBoolean(installedOnly?"installedReady":"catalogReady",false)&&version!=revision){revision=version;loadPage();}
    }
    private boolean installingCard(String tag){
        String id=tag.startsWith("desktop-program-")?tag.substring(16):"pkg:"+tag.substring(16);
        return busy()&&state.getString("action","").equals("install")&&id.equals(state.getString("program",""));
    }
    private void stopInstall(){if(!busy()||state.getString("phase","").equals("cancelling"))return;state.putString("phase","cancelling");render();request.accept("programs-cancel","");}
    private void updateTrash(TextView button,boolean installed){
        ImageButton trash=trashButtons.get(button);if(trash==null)return;
        String id=cardIds.get(button);String pkg=id.startsWith("pkg:")?id.substring(4):DesktopPrograms.mainPackage(id,target());
        trash.setVisibility(installed?View.VISIBLE:View.GONE);available(trash,installed&&!busy()&&DesktopPrograms.removable(pkg));
    }
    private void updateCard(TextView button){
        DesktopProgramProgress line=inline.get(button);if(line==null)return;
        boolean current=busy()&&state.getString("action","").equals("install")&&cardIds.get(button).equals(state.getString("program",""));
        line.update(state,current);
        if(recommended)updateTrash(button,items.getBoolean(cardIds.get(button),false));
        else if(trashButtons.containsKey(button)&&busy())available(trashButtons.get(button),false);
        if(current){boolean stopping=state.getString("phase","").equals("cancelling");setText(button,stopping?tr("Остановка…","Stopping…"):tr("Стоп","Stop"));AgentStatusUi.available(button,!stopping);}
        else if(busy())AgentStatusUi.available(button,false);
        else if(canResume(cardIds.get(button))&&!items.getBoolean(cardIds.get(button),false)){setText(button,tr("Продолжить","Continue"));AgentStatusUi.available(button,true);updateTrash(button,false);}
        else if(!recommended){
            String id=cardIds.get(button);boolean justInstalled=state.getString("phase","").equals("complete")&&state.getString("action","").equals("install")&&id.equals(state.getString("program",""));
            boolean installed=justInstalled||items.getBoolean(id,false);
            if(installed){setText(button,tr("Установлено","Installed"));AgentStatusUi.available(button,false);updateTrash(button,true);}
            else if(button==activeCard){setText(button,items.containsKey(id)?tr("Установить","Install"):tr("Проверить","Check"));AgentStatusUi.available(button,true);updateTrash(button,false);}
        }
    }
    private void render(){
        if(dialog==null)return;String phase=state.getString("phase","");boolean active=busy();
        if(repairButton!=null)AgentStatusUi.available(repairButton,!active);for(CheckBox check:compatChecks){check.setChecked(state.getBoolean("firefoxCompat",false));available(check,!active&&state.getBoolean("firefoxCompatKnown",false));}
        for(CheckBox check:dnsChecks){check.setChecked(state.getBoolean("firefoxDns",false));available(check,!active&&state.getBoolean("firefoxDnsKnown",false));}
        if(recommended){for(Map.Entry<String,TextView> e:buttons.entrySet()){boolean known=items.containsKey(e.getKey()),installed=items.getBoolean(e.getKey(),false);setText(e.getValue(),installed?tr("Установлено","Installed"):active?tr("Проверка…","Checking…"):known?tr("Установить","Install"):tr("Проверить","Check"));AgentStatusUi.available(e.getValue(),!active&&!installed);}}
        else if(installedOnly||catalogView){
            search.setEnabled(!active&&state.getBoolean(installedOnly?"installedReady":"catalogReady",false));available(previous,!active&&offset>0);available(next,!active&&offset+30<total);
            setText(pageLabel,rows.isEmpty()?"0 / "+total:(offset+1)+"–"+(offset+rows.size())+" / "+total);
        }
        if(refresh!=null){setText(refresh,active?tr("Выполняется…","Working…"):recommended||installedOnly?tr("Проверить установленные программы","Check installed programs"):tr("Подготовить и обновить каталог","Prepare and refresh catalog"));AgentStatusUi.available(refresh,!active);}
        String text=phase.equals("needs-tools")?tr("Каталог ещё не готов. В разделе «Поиск и установка программ» нажмите «Подготовить и обновить каталог». Рекомендуемые доступны отдельно.","Open Search and install programs, then select Prepare and refresh catalog. Recommended programs are available separately."):phase.equals("error")?tr("Не удалось выполнить действие. Причина указана ниже.","The action failed. The reason is shown below."):phase.equals("cancelled")?tr("Установка остановлена. Нажмите «Продолжить» в карточке программы. При необходимости восстановите пакеты.","Installation stopped. Select Continue on the program card. Repair packages if necessary."):phase.equals("cataloguing")?tr("Загрузка каталога выбранного окружения…","Loading the selected environment's catalog…"):phase.equals("checking")?tr("Проверка установленных программ…","Checking installed programs…"):installedOnly?tr("Установленные графические программы выбранного окружения. Базовые компоненты защищены от удаления.","Installed graphical programs in the selected environment. Core desktop components are protected."):recommended?tr("Состояние программ проверено.","Program status checked."):(nativeCatalog()?tr("Программы и пакеты выбранного окружения, включая библиотеки. Совместимость с X11 требует проверки.","Environment applications and packages, including libraries. X11 compatibility requires testing."):tr("Дополнительные программы из репозиториев. Совместимость с proot/X11 требует проверки.","Additional repository applications. Compatibility with proot/X11 requires testing."));
        boolean error=phase.equals("error");String detail=state.getString("detail","");
        if(error&&(detail.contains("Failed to fetch")||detail.contains("Error reading from server")))text=tr("Не удалось скачать пакеты из репозитория. Проверьте соединение или выберите другое зеркало и повторите установку. Подробности — ниже.","Could not download repository packages. Check the connection or select another mirror, then retry installation. Details below.");
        setText(message,text);errorDetails.setVisibility(error&&!detail.isEmpty()?View.VISIBLE:View.GONE);
        for(TextView button:new ArrayList<>(inline.keySet()))updateCard(button);
        boolean inlineInstall=state.getString("action","").equals("install");
        if((phase.equals("installing")||phase.equals("removing")||phase.equals("cancelling"))&&!inlineInstall){
            if(progress==null)progress=new DesktopInstallUi(activity,phase.equals("removing")?tr("Удаление программы","Removing program"):tr("Установка программы","Installing program"),()->request.accept("programs-cancel",""));
            String id=state.getString("program","");String title=installName.isEmpty()?(id.startsWith("pkg:")?id.substring(4):DesktopPrograms.title(id.isEmpty()?"catalog-tools":id,ru())):installName;
            progress.update(phase.equals("cancelling")?tr("Отмена…","Cancelling…"):(phase.equals("removing")?tr("Удаление: ","Removing: "):tr("Установка: ","Installing: "))+title,state.getString("detail",""),state.getInt("percent",-1),state.getLong("eta",-1),state.getLong("sampleTime",0),state.getString("stage","preparing"),state.getLong("downloadBytes",-1),state.getLong("downloadTotal",-1));
        }else if(progress!=null){progress.close();progress=null;}
    }
    private void setText(TextView v,String value){if(!v.getText().toString().equals(value))v.setText(value);}
    void failed(){awaiting=false;Bundle b=new Bundle();b.putInt("job",state.getInt("job",0));b.putString("target",target());b.putString("phase","error");accept(b);}
    void close(){main.removeCallbacksAndMessages(null);if(progress!=null){progress.close();progress=null;}if(dialog!=null){dialog.dismiss();dialog=null;}}
    private int dp(int n){return WorkspaceDialogs.dp(activity,n);}
    private boolean ru(){return WorkspaceLanguage.code(activity).equals("ru");}
    private String tr(String ru,String en){return AgentStatusUi.tr(activity,ru,en);}
}
