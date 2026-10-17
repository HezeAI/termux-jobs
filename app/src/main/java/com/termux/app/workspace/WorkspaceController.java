package com.termux.app.workspace;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.Editable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.app.activities.SettingsActivity;
import com.termux.shared.shell.TermuxSession;
import com.termux.shared.termux.TermuxConstants;
import com.termux.terminal.TerminalSession;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Native workspace shell around the original TerminalView and foreground service. */
public final class WorkspaceController {
    private static final int ACCENT = 0xff79dfcd, TEXT = 0xffedf5f6, MUTED = 0xff91a6af;
    private static final int PANEL = 0xff0b1319, BORDER = 0xff213039;
    private enum Screen { HOME, SESSIONS, COMMANDS, TERMINAL, SERVERS }
    private final TermuxActivity activity;
    private final WorkspaceStore store;
    private final WorkspaceServers servers;
    private final LocalAiUi localAi;
    private WorkspaceStore.Session selectedLocal;
    private final LinearLayout header, navigation, tabs, inputRow, codexStatus;
    private final EditText terminalInput;
    private LinearLayout commandResults, noteResults;
    private String terminalInputHandle;
    private final java.util.Map<String, InputDraft> terminalDrafts = new java.util.HashMap<>();
    private static final class InputDraft {
        final String text; final int start, end;
        InputDraft(EditText input) {
            text=input.getText().toString(); start=input.getSelectionStart(); end=input.getSelectionEnd();
        }
    }
    private final FrameLayout content;
    private final List<WorkspacePackages> packageBrowsers=new ArrayList<>();
    private final List<WorkspaceFileBrowser> fileBrowsers=new ArrayList<>();
    private final ExecutorService reader = Executors.newSingleThreadExecutor();
    private final android.os.Handler codexHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final ExecutorService codexReader = Executors.newSingleThreadExecutor();
    private boolean codexLoading;
    private int codexApplyGeneration;
    private final java.util.Map<String,String[]> codexConfirmedChoices=new java.util.HashMap<>();
    private AlertDialog codexApplyDialog;
    private final java.util.Map<String, CodexStatus> codexStatuses = new java.util.HashMap<>();
    private final Runnable codexPoll = new Runnable() { public void run() {
        if (closed) return;
        WorkspaceStore.Session profile = currentCodex();
        if (screen == Screen.TERMINAL && profile != null) {
            loadCodexStatus(profile, null);
        }
        codexHandler.postDelayed(this, 30000);
    }};
    private Screen screen = Screen.HOME;
    private String query = "", journalQuery = "", commandCategory = "", noteCategory = "";
    private boolean noteFavorites, favorites, savedOnly, closed, creating, baseCreating;
    private long lastBaseRestart, baseStartedAt;
    private int baseRestartFailures;
    private int toolbarVisibility = View.GONE;
    private boolean interactiveInput;
    private float swipeStart;
    private final Set<String> pendingCloses = new java.util.HashSet<>();
    private final java.util.Map<String, WorkspaceStore.Session> pendingRestarts = new java.util.HashMap<>();

    private final java.util.Map<String, char[]> pendingSshPasswords = new java.util.HashMap<>();
    public WorkspaceController(TermuxActivity activity) {
        this.activity = activity;
        prepareKeyboardWindow();
        store = new WorkspaceStore(activity, message -> activity.showToast(message, true));
        localAi=new LocalAiUi(activity);
        servers = new WorkspaceServers(activity, (name, command) -> {
            WorkspaceStore.Session s = new WorkspaceStore.Session(); s.name = name + " · SSH";
            s.environment = "termux"; s.directory = TermuxConstants.TERMUX_HOME_DIR_PATH;
            s.startup = command; launchNew(s, true);
        });
        interactiveInput = activity.getSharedPreferences("workspace-input", Context.MODE_PRIVATE)
            .getBoolean("interactive-input", false);
        activity.getTerminalView().setSoftKeyboardInputEnabled(interactiveInput);
        header = activity.findViewById(R.id.workspace_header);
        navigation = activity.findViewById(R.id.workspace_navigation);
        tabs = activity.findViewById(R.id.workspace_tabs);
        codexStatus = activity.findViewById(R.id.workspace_codex_status);
        localAi.stateChanged=()->activity.runOnUiThread(()->{if(!closed&&selectedLocal!=null)renderCodexStatus();});
        localAi.profileChanged=()->{if(!closed){renderTabs();if(screen!=Screen.TERMINAL)renderContent();}};
        content = activity.findViewById(R.id.workspace_content);
        inputRow = activity.findViewById(R.id.workspace_input);
        inputRow.setPadding(dp(12), dp(6), dp(12), dp(6));
        terminalInput = field(WorkspaceText.tr("Введите команду…"), "", true);
        terminalInput.setTypeface(Typeface.MONOSPACE); terminalInput.setTextSize(12);
        terminalInput.setMinLines(1); terminalInput.setMinHeight(dp(48));
        terminalInput.setHorizontallyScrolling(false);
        terminalInput.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_NONE
            | android.view.inputmethod.EditorInfo.IME_FLAG_NO_ENTER_ACTION
            | android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        inputRow.addView(terminalInput, new LinearLayout.LayoutParams(0, -2, 1));
        TextView commands = button("★", false, this::chooseInputCommand);
        commands.setTextColor(ACCENT); commands.setTextSize(22);
        commands.setContentDescription(WorkspaceText.tr("Вставить сохранённую команду"));
        LinearLayout.LayoutParams commandsParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        commandsParams.setMarginEnd(dp(8)); inputRow.addView(commands, 0, commandsParams);
        TextView send = button("▷", true, this::sendInput); send.setContentDescription(WorkspaceText.tr("Отправить в активный терминал"));
        LinearLayout.LayoutParams sendParams = new LinearLayout.LayoutParams(dp(48), dp(48)); sendParams.setMarginStart(dp(8)); inputRow.addView(send, sendParams);
        activity.getWindow().setStatusBarColor(Color.BLACK);
        activity.getWindow().setNavigationBarColor(Color.BLACK);
        tabs.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) swipeStart = event.getX();
            if (selectedLocal==null && event.getAction() == MotionEvent.ACTION_UP && Math.abs(event.getX() - swipeStart) > dp(64)) {
                activity.getTermuxTerminalSessionClient().switchToSession(event.getX() < swipeStart);
                v.performClick(); return true;
            }
            return true;
        });
        toolbarVisibility = activity.getTerminalToolbarViewPager().getVisibility();
        show(Screen.HOME, false);
        codexHandler.postDelayed(codexPoll, 1000);
    }
    public boolean localAiActivityResult(int request,int result,android.content.Intent data){return localAi.activityResult(request,result,data);}
    public void onStop(){WorkspaceUpdates.background(activity);servers.pause();}
    public void onResume(){servers.resume();WorkspaceUpdates.automatic(activity,()->{if(!closed)renderHeader();});}
    public void refreshLanguage() {
        terminalInput.setHint(WorkspaceText.tr("Введите команду…"));
        inputRow.getChildAt(0).setContentDescription(WorkspaceText.tr("Вставить сохранённую команду"));
        inputRow.getChildAt(inputRow.getChildCount()-1).setContentDescription(WorkspaceText.tr("Отправить в активный терминал"));
        renderHeader(); renderNavigation(); renderTabs(); renderContent(); renderCodexStatus();
    }
    private int dp(float value) { return Math.round(value * activity.getResources().getDisplayMetrics().density); }
    private LinearLayout column() { LinearLayout v = new LinearLayout(activity); v.setOrientation(LinearLayout.VERTICAL); return v; }
    private LinearLayout row() { LinearLayout v = new LinearLayout(activity); v.setOrientation(LinearLayout.HORIZONTAL); v.setGravity(Gravity.CENTER_VERTICAL); return v; }
    private TextView text(String value, int size, int color) {
        TextView t = new TextView(activity); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        t.setFontFeatureSettings("kern"); return t;
    }
    private GradientDrawable shape(int color, int stroke, int radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius));
        d.setStroke(dp(1), stroke); return d;
    }
    private TextView button(String label, boolean active, Runnable click) {
        TextView b = new WorkspaceIconButton(activity);b.setText(label);b.setTextSize(13);b.setTextColor(active?Color.BLACK:TEXT);
        b.setGravity(Gravity.CENTER); b.setPadding(dp(13), dp(8), dp(13), dp(8)); b.setMinHeight(dp(48));
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x4479dfcd),
            shape(active ? ACCENT : PANEL, active ? ACCENT : BORDER, 12), null));
        int icon = iconFor(label.split("\n", 2)[0]);
        if (icon != 0) {
            android.graphics.drawable.Drawable drawable = activity.getDrawable(icon).mutate();
            drawable.setTint(active ? Color.BLACK : TEXT); drawable.setBounds(0, 0, dp(22), dp(22));
            if (label.contains("\n")) {
                b.setText(label.substring(label.indexOf('\n') + 1));
                b.setCompoundDrawables(null, drawable, null, null); b.setCompoundDrawablePadding(dp(4));
            } else {
                ((WorkspaceIconButton)b).setIcon(drawable,dp(22)); b.setContentDescription(label);
                b.setMinWidth(dp(48));
            }
        }
        b.setOnClickListener(v -> click.run()); return b;
    }
    private int iconFor(String label) {
        switch (label) {
            case "⌂": return R.drawable.workspace_home;
            case "▤": return R.drawable.workspace_sessions;
            case ">_": return R.drawable.workspace_commands;
            case "▣": return R.drawable.workspace_terminal;
            case "⌕": return R.drawable.workspace_search;
            case "+": return R.drawable.workspace_plus;
            case "★": return R.drawable.workspace_star;
            case "▱": return R.drawable.workspace_folder;
            case "📦": return R.drawable.workspace_packages;
            case "▷": return R.drawable.workspace_play;
            case "⋮": return R.drawable.workspace_more;
            case "⚙": return R.drawable.workspace_settings;
            case "◎": return R.drawable.workspace_language;
            case "⌖": return R.drawable.workspace_interactive_input;
            case "⌨": return R.drawable.workspace_extra_keys;
            case "›": return R.drawable.workspace_next;
            default: return 0;
        }
    }
    private void tintButton(TextView button, int color) {
        button.setTextColor(color);
        for (android.graphics.drawable.Drawable d : button.getCompoundDrawables()) if (d != null) d.setTint(color);
    }
    private void section(LinearLayout parent, String title, String action, Runnable click) {
        LinearLayout r = row(); r.setPadding(0, dp(22), 0, dp(10)); r.setMinimumHeight(dp(80));
        TextView t = text(title, 17, TEXT); t.setTypeface(null, Typeface.BOLD);
        r.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
        if (action != null) { TextView a = button(action, false, click); a.setTextColor(ACCENT); r.addView(a); }
        parent.addView(r);
    }
    private void show(Screen target, boolean animate) {
        if (screen == Screen.TERMINAL && target != Screen.TERMINAL) {
            toolbarVisibility = activity.getTerminalToolbarViewPager().getVisibility();
        }
        if (screen != target) {
            View focused = activity.getCurrentFocus();
            if (focused != null) ((InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(focused.getWindowToken(), 0);
            header.setFocusableInTouchMode(true); header.requestFocus();
        }
        screen = target;
        servers.visibility(screen == Screen.SERVERS);
        android.widget.RelativeLayout.LayoutParams contentParams=(android.widget.RelativeLayout.LayoutParams)content.getLayoutParams();
        contentParams.addRule(android.widget.RelativeLayout.BELOW,screen==Screen.TERMINAL&&selectedLocal!=null?R.id.workspace_terminal_tools:R.id.workspace_header);content.setLayoutParams(contentParams);
        boolean terminal = screen == Screen.TERMINAL;
        boolean local=terminal&&selectedLocal!=null;
        activity.getDrawer().setVisibility(terminal&&!local ? View.VISIBLE : View.INVISIBLE);
        activity.getTerminalToolbarViewPager().setVisibility(terminal&&!local ? toolbarVisibility : View.GONE);
        activity.findViewById(R.id.workspace_tab_scroll).setVisibility(terminal ? View.VISIBLE : View.GONE);
        content.setVisibility(terminal&&!local ? View.GONE : View.VISIBLE);
        inputRow.setVisibility(terminal ? View.VISIBLE : View.GONE);
        terminalInput.setHint(local?localAi.tr("Сообщение…","Message…"):WorkspaceText.tr("Введите команду…"));
        renderHeader(); renderNavigation(); renderTabs(); renderContent(); renderCodexStatus();
        if (animate && !terminal) { content.setAlpha(0); content.animate().alpha(1).setDuration(180).start(); }
        if (terminal&&!local) activity.getTerminalView().requestFocus();
    }
    private void renderHeader() {
        // Keep the navigation container stable while rebuilding its surrounding header.
        if (navigation.getParent() != null) ((ViewGroup) navigation.getParent()).removeView(navigation);
        header.removeAllViews(); header.setPadding(dp(8), dp(4), dp(8), dp(4));
        LinearLayout top = row();
        TextView name = text("Termux Job’s", 17, TEXT); name.setTypeface(null, Typeface.BOLD);
        name.setSingleLine(); name.setEllipsize(TextUtils.TruncateAt.END);
        top.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
        if(screen==Screen.TERMINAL&&selectedLocal==null){
            TextView direct=button("⌖",interactiveInput,()->{setInteractiveInput(!interactiveInput);renderHeader();});
            direct.setContentDescription(WorkspaceText.tr("Интерактивный ввод"));direct.setSelected(interactiveInput);
            LinearLayout.LayoutParams directParams=new LinearLayout.LayoutParams(dp(48),dp(48));directParams.setMarginEnd(dp(8));top.addView(direct,directParams);
            boolean keysVisible=toolbarVisibility==View.VISIBLE;
            TextView keys=button("⌨",keysVisible,()->{activity.toggleTerminalToolbar();toolbarVisibility=activity.getTerminalToolbarViewPager().getVisibility();renderHeader();});
            keys.setContentDescription(WorkspaceText.tr("Дополнительные клавиши"));keys.setSelected(keysVisible);
            LinearLayout.LayoutParams keyParams=new LinearLayout.LayoutParams(dp(48),dp(48));keyParams.setMarginEnd(dp(8));top.addView(keys,keyParams);
        }
        TextView settings=button("⚙",false,this::globalSettings);settings.setContentDescription(WorkspaceText.tr("Настройки приложения"));
        if(settings instanceof WorkspaceIconButton)((WorkspaceIconButton)settings).setBadge(WorkspaceUpdates.available(activity));
        top.addView(settings,new LinearLayout.LayoutParams(dp(48),dp(48)));
        header.addView(top, new LinearLayout.LayoutParams(-1, dp(48)));
        LinearLayout.LayoutParams navigationParams = new LinearLayout.LayoutParams(-1, -2);
        navigationParams.topMargin = dp(4);
        header.addView(navigation, navigationParams);
        if (screen == Screen.COMMANDS || screen == Screen.SESSIONS) {
            LinearLayout tools = row(); tools.setPadding(dp(8), dp(26), dp(8), dp(6));
            if (screen == Screen.SESSIONS) {
                TextView title = text(WorkspaceText.tr("Сессии"), 17, TEXT); title.setTypeface(null, Typeface.BOLD);
                tools.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
            }
            if (screen == Screen.COMMANDS) {
                EditText search = field(WorkspaceText.tr("Поиск команд и/или заметок"), query, false);
                search.setContentDescription(WorkspaceText.tr("Поиск команд и заметок")); search.setTextSize(13);
                search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
                    | android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI);
                android.graphics.drawable.Drawable icon = activity.getDrawable(R.drawable.workspace_search).mutate();
                icon.setTint(MUTED); icon.setBounds(0, 0, dp(18), dp(18));
                search.setCompoundDrawables(icon, null, null, null); search.setCompoundDrawablePadding(dp(6));
                LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(0, -2, 1);
                search.setMinHeight(dp(48)); tools.addView(search, searchParams);
                search.addTextChangedListener(new TextWatcher() {
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        query = s.toString(); renderLibraryResults();
                    }
                    public void afterTextChanged(Editable s) {}
                });
                search.setOnEditorActionListener((v, actionId, event) -> {
                    if (actionId != android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) return false;
                    ((InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE))
                        .hideSoftInputFromWindow(search.getWindowToken(), 0);
                    return true;
                });
            }
            TextView add = button("+", true, () -> { if (screen == Screen.COMMANDS) editCommand(null); else editSession(null); });
            add.setContentDescription(screen == Screen.COMMANDS ? WorkspaceText.tr("Новая команда") : WorkspaceText.tr("Новая сессия"));
            if (screen == Screen.SESSIONS) tools.addView(add, new LinearLayout.LayoutParams(dp(48), dp(48)));
            header.addView(tools);
        }
    }
    private String currentDirectory() {
        TerminalSession s = activity.getCurrentSession();
        return s == null ? WorkspaceText.tr("Нет активной сессии") : compact(s.getCwd());
    }
    private String compact(String path) {
        if (path == null || path.isEmpty()) return "~";
        String home = TermuxConstants.TERMUX_HOME_DIR_PATH;
        return path.equals(home) || path.startsWith(home + "/") ? "~" + path.substring(home.length()) : path;
    }
    private void renderNavigation() {
        navigation.removeAllViews(); navigation.setPadding(dp(8), 0, dp(8), 0);
        String[] labels = {WorkspaceText.tr("Главная"), WorkspaceText.tr("Команды/Заметки"), WorkspaceText.tr("Терминал")};
        Screen[] targets = {Screen.HOME, Screen.COMMANDS, Screen.TERMINAL};
        for (int i = 0; i < targets.length; i++) {
            Screen target = targets[i]; TextView nav = button("", false, () -> show(target, true));
            nav.setTextSize(11); nav.setPadding(0, dp(4), 0, dp(4)); nav.setMinWidth(0); nav.setMinHeight(dp(56)); nav.setMaxLines(2);
            int resource = target == Screen.HOME ? R.drawable.workspace_home
                : target == Screen.COMMANDS ? R.drawable.workspace_library : R.drawable.workspace_commands;
            android.graphics.drawable.Drawable icon = activity.getDrawable(resource).mutate();
            ((WorkspaceIconButton)nav).setIcon(icon,dp(28));
            nav.setContentDescription(labels[i]);
            tintButton(nav, target == screen ? ACCENT : MUTED);
            nav.setBackground(new RippleDrawable(ColorStateList.valueOf(0x4479dfcd),
                shape(target == screen ? 0xff102522 : PANEL, target == screen ? ACCENT : BORDER, 12), null));
            LinearLayout.LayoutParams tileParams = new LinearLayout.LayoutParams(0, dp(56), 1);
            if (i < targets.length - 1) tileParams.setMarginEnd(dp(8));
            navigation.addView(nav, tileParams);
        }
    }
    private List<TermuxSession> live() {
        TermuxService service = activity.getTermuxService();
        return service == null ? Collections.emptyList() : new ArrayList<>(service.getTermuxSessions());
    }
    private TerminalSession liveFor(WorkspaceStore.Session profile) {
        for (TermuxSession t : live()) if (t.getTerminalSession().mHandle.equals(profile.handle)) return t.getTerminalSession();
        return null;
    }
    private void syncTerminalInput() {
        TerminalSession current=activity.getCurrentSession();
        String handle=selectedLocal!=null?"local:"+selectedLocal.id:current==null?null:current.mHandle;
        if(!java.util.Objects.equals(handle,terminalInputHandle)) {
            if(terminalInputHandle!=null) terminalDrafts.put(terminalInputHandle,new InputDraft(terminalInput));
            terminalInputHandle=handle;
            InputDraft draft=terminalDrafts.get(handle);
            terminalInput.clearComposingText();terminalInput.setText(draft==null?"":draft.text);
            if(draft!=null) terminalInput.setSelection(Math.max(0,Math.min(draft.start,terminalInput.length())),Math.max(0,Math.min(draft.end,terminalInput.length())));
        } else return;
        if(activity.getTermuxService()!=null) {
            Set<String> handles=new java.util.HashSet<>();
            for(TermuxSession session:live()) handles.add(session.getTerminalSession().mHandle);
            for(WorkspaceStore.Session p:store.sessions)if(p.localAi&&localAi.active(p.id))handles.add("local:"+p.id);
            terminalDrafts.keySet().retainAll(handles);
        }
        if(terminalInput.hasFocus())((InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE)).restartInput(terminalInput);
    }
    private void renderTabs() {
        syncTerminalInput(); renderCodexStatus(); tabs.removeAllViews();
        tabs.setPadding(dp(10),dp(4),dp(10),dp(8));tabs.setGravity(Gravity.TOP);
        TerminalSession current=activity.getCurrentSession();
        String name=selectedLocal!=null?selectedLocal.name:current==null?WorkspaceText.tr("Сессии"):current.mSessionName==null?"Bash":current.mSessionName;
        WorkspaceStore.Session profile=selectedLocal!=null?selectedLocal:current==null?null:store.findByHandle(current.mHandle);
        String subtitle=profile==null?"":profile.localAi?localAi.tr("Локальный ИИ","Local AI"):ProjectFiles.environmentLabel(profile)+" · "+compact(profile.directory);
        TextView selector=button(name+"  ▾"+(subtitle.isEmpty()?"":"\n"+subtitle),false,()->{});
        selector.setTextSize(13);selector.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);selector.setMaxLines(2);selector.setEllipsize(TextUtils.TruncateAt.END);
        selector.setContentDescription(name);selector.setOnClickListener(v->showSessionStack(selector));
        selector.setOnLongClickListener(v->{WorkspaceStore.Session selected=selectedLocal!=null?selectedLocal:activity.getCurrentSession()==null?null:store.findByHandle(activity.getCurrentSession().mHandle);if(selected!=null)sessionMenu(selected,true);return true;});
        if(SessionShell.hasCancellableStartup(profile)){
            FrameLayout panel=new FrameLayout(activity);panel.addView(selector,new FrameLayout.LayoutParams(-1,-1));
            selector.setPadding(selector.getPaddingLeft(),selector.getPaddingTop(),dp(56),selector.getPaddingBottom());
            TextView cancel=button("",false,()->confirmCancelStartup(profile));cancel.setTag("cancel-startup");cancel.setContentDescription(WorkspaceText.tr("Отменить команды запуска"));
            android.graphics.drawable.Drawable icon=activity.getDrawable(R.drawable.workspace_cancel_startup).mutate();icon.setTint(MUTED);((WorkspaceIconButton)cancel).setIcon(icon,dp(22));
            cancel.setBackground(new RippleDrawable(ColorStateList.valueOf(0x4479dfcd),shape(Color.TRANSPARENT,Color.TRANSPARENT,12),null));
            panel.addView(cancel,new FrameLayout.LayoutParams(dp(48),dp(48),Gravity.END|Gravity.CENTER_VERTICAL));tabs.addView(panel,new LinearLayout.LayoutParams(0,dp(48),1));
        }else tabs.addView(selector,new LinearLayout.LayoutParams(0,dp(48),1));
        LinearLayout serverTools=row();LinearLayout toolIcons=serverTools;
        TextView packages=button("📦",false,this::showCurrentPackages);packages.setContentDescription(WorkspaceText.tr("Пакеты"));toolIcons.addView(packages,new LinearLayout.LayoutParams(dp(48),dp(48)));
        TextView files=button("▱",false,this::showProjectFiles);files.setContentDescription(WorkspaceText.tr("Файлы проекта"));LinearLayout.LayoutParams fileParams=new LinearLayout.LayoutParams(dp(48),dp(48));fileParams.setMarginStart(dp(10));toolIcons.addView(files,fileParams);
        String serverLabel=WorkspaceLanguage.code(activity).equals("ru")?"Сервер + Agent":"Server + Agent";
        FrameLayout statusRow=activity.findViewById(R.id.workspace_terminal_tools);
        statusRow.setVisibility(screen==Screen.TERMINAL?View.VISIBLE:View.GONE);
        View oldServer=statusRow.findViewWithTag("server-entry");if(oldServer!=null)statusRow.removeView(oldServer);
        TextView server=button(serverLabel,false,()->show(Screen.SERVERS,true));server.setTextSize(12);server.setPadding(dp(4),0,dp(4),0);server.setContentDescription(serverLabel);server.setTag("server-entry");
        FrameLayout.LayoutParams serverParams=new FrameLayout.LayoutParams(dp(106),dp(48),Gravity.END|Gravity.CENTER_VERTICAL);statusRow.addView(server,serverParams);

        LinearLayout.LayoutParams toolsParams=new LinearLayout.LayoutParams(dp(106),-2);toolsParams.setMarginStart(dp(6));tabs.addView(serverTools,toolsParams);
    }
    private TextView sessionStackItem(String label,boolean active,int icon,Runnable click){
        TextView item=new TextView(activity);item.setText(label);item.setTextSize(14);item.setTextColor(active?Color.BLACK:TEXT);item.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);item.setPadding(dp(13),dp(8),dp(13),dp(8));item.setMinHeight(dp(48));item.setMaxLines(3);item.setEllipsize(TextUtils.TruncateAt.END);item.setSelected(active);
        item.setBackground(new RippleDrawable(ColorStateList.valueOf(0x4479dfcd),shape(active?ACCENT:PANEL,active?ACCENT:BORDER,12),null));
        android.graphics.drawable.Drawable image=activity.getDrawable(icon).mutate();image.setTint(active?Color.BLACK:TEXT);image.setBounds(0,0,dp(22),dp(22));item.setCompoundDrawables(image,null,null,null);item.setCompoundDrawablePadding(dp(8));item.setOnClickListener(v->click.run());
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(6);item.setLayoutParams(lp);return item;
    }
    private android.widget.PopupWindow showSessionStack(View anchor) {
        LinearLayout body=column();body.setPadding(dp(8),dp(8),dp(8),dp(8));body.setBackground(shape(PANEL,BORDER,14));
        TextView heading=text(WorkspaceText.tr("Открытые сессии"),14,TEXT);heading.setPadding(dp(6),dp(4),dp(6),dp(10));
        ScrollView scroll=new ScrollView(activity);LinearLayout list=column();scroll.addView(list);
        android.widget.PopupWindow popup=new android.widget.PopupWindow(body,Math.min(dp(340),activity.getResources().getDisplayMetrics().widthPixels-dp(24)),-2,true);
        popup.setBackgroundDrawable(shape(PANEL,BORDER,14));popup.setOutsideTouchable(true);popup.setElevation(dp(8));
        TextView create=sessionStackItem(WorkspaceText.tr("Создать новую сессию"),false,R.drawable.workspace_plus,()->{popup.dismiss();editSession(null);});
        create.setContentDescription(WorkspaceText.tr("Создать новую сессию"));create.setTag("session-stack-create");list.addView(create);
        list.addView(heading);
        for(TermuxSession ts:live()) {
            TerminalSession terminal=ts.getTerminalSession();WorkspaceStore.Session profile=store.findByHandle(terminal.mHandle);
            String title=terminal.mSessionName==null?"Bash":terminal.mSessionName;
            String info=profile==null?"":ProjectFiles.environmentLabel(profile)+" · "+compact(profile.directory);
            boolean active=selectedLocal==null&&activity.getCurrentSession()==terminal;
            TextView item=sessionStackItem(title+(active?"  ✓":"")+"\n"+info,active,R.drawable.workspace_terminal,()->{popup.dismiss();selectedLocal=null;activity.getTermuxTerminalSessionClient().setCurrentSession(terminal);show(Screen.TERMINAL,false);});
            item.setTag("session-stack-"+terminal.mHandle);item.setContentDescription(WorkspaceText.tr("Сессия ")+title+" · "+info);
            item.setOnLongClickListener(v->{popup.dismiss();if(profile!=null)sessionMenu(profile,true);return true;});list.addView(item);
        }
        int localCount=0;
        for(WorkspaceStore.Session p:store.sessions)if(p.localAi&&localAi.active(p.id)){
            localCount++;TextView item=sessionStackItem(p.name+(selectedLocal==p?" ✓":"")+"\n"+localAi.sessionModelName(p),selectedLocal==p,R.drawable.workspace_local_robot,()->{popup.dismiss();launch(p);});
            item.setTag("session-stack-local-"+p.id);item.setContentDescription(p.name+" · "+localAi.sessionModelName(p));item.setOnLongClickListener(v->{popup.dismiss();sessionMenu(p,true);return true;});list.addView(item);
        }
        if(live().isEmpty()&&localCount==0)list.addView(text(WorkspaceText.tr("Нет открытых сессий. Создайте новую сессию."),13,MUTED));
        body.addView(scroll,new LinearLayout.LayoutParams(-1,Math.min(dp(360),Math.max(dp(140),dp(86)+(live().size()+localCount)*dp(76)))));
        body.addView(text(WorkspaceText.tr("Долгий тап — действия сессии"),12,MUTED));
        View cancel=tabs.findViewWithTag("cancel-startup");if(cancel!=null){cancel.setVisibility(View.INVISIBLE);popup.setOnDismissListener(()->cancel.setVisibility(View.VISIBLE));}
        popup.showAsDropDown(anchor,0,dp(4));return popup;
    }
    private void showCurrentPackages() {
        if(selectedLocal!=null){activity.showToast(localAi.tr("Локальному чату не нужны пакеты Termux","Local chat does not need Termux packages"),false);return;}
        TerminalSession terminal=activity.getCurrentSession();WorkspaceStore.Session profile=terminal==null?null:store.findByHandle(terminal.mHandle);
        if(profile==null){activity.showToast(WorkspaceText.tr("Сначала откройте сессию"),false);return;}
        showPackages(profile);
    }
    private void showProjectFiles() {
        if(selectedLocal!=null){if(!localAi.validSelection(new LocalAiUi.Selection(selectedLocal)))return;WorkspaceStore.Session location=LocalAiUi.projectProfile(selectedLocal);Runnable open=()->{if(!closed)showFileBrowser(location,location.directory,null);};if(LocalAiUi.sharedProjectPath(location.directory))activity.requestWorkspaceStorageAccess(open);else open.run();return;}
        TerminalSession terminal=activity.getCurrentSession();WorkspaceStore.Session profile=terminal==null?null:store.findByHandle(terminal.mHandle);
        if(profile==null){activity.showToast(WorkspaceText.tr("Сначала откройте сессию"),false);return;}
        showFileBrowser(profile,profile.directory,null);
    }
    private void showFileBrowser(WorkspaceStore.Session profile,String path,WorkspaceFileBrowser.DirectoryChoice choice){
        WorkspaceFileBrowser browser=new WorkspaceFileBrowser(activity,profile,path,choice);browser.setOnClosed(()->fileBrowsers.remove(browser));fileBrowsers.add(browser);browser.show();
    }
    private void renderContent() {
        if (screen == Screen.TERMINAL) {
            if(selectedLocal!=null){
                LocalAiUi.Panel panel=localAi.panel(selectedLocal,store::save);
                content.setVisibility(View.VISIBLE);activity.getDrawer().setVisibility(View.INVISIBLE);activity.getTerminalToolbarViewPager().setVisibility(View.GONE);
                if(panel.root.getParent()!=content){content.removeAllViews();if(panel.root.getParent()!=null)((ViewGroup)panel.root.getParent()).removeView(panel.root);content.addView(panel.root,new FrameLayout.LayoutParams(-1,-1));}
            }else{content.setVisibility(View.GONE);activity.getDrawer().setVisibility(View.VISIBLE);}
            return;
        }
        commandResults = null; noteResults = null;
        content.removeAllViews();
        LinearLayout body = column(); body.setPadding(dp(16), 0, dp(16), dp(12));
        if (screen == Screen.SERVERS) {
            content.addView(body, new FrameLayout.LayoutParams(-1,-1)); servers.attach(body); return;
        }
        if (screen == Screen.HOME || screen == Screen.COMMANDS) {
            AdaptiveWorkspaceLayout viewport = new AdaptiveWorkspaceLayout(activity);
            viewport.setContentDescription(WorkspaceText.tr("Адаптивная область списков"));
            viewport.addView(body); content.addView(viewport, new android.widget.FrameLayout.LayoutParams(-1, -1));
            if (screen == Screen.HOME) home(body); else commands(body);
        } else {
            ScrollView scroll = new ScrollView(activity); scroll.setFillViewport(true);
            scroll.addView(body); content.addView(scroll);
            if (screen == Screen.COMMANDS) commands(body); else sessions(body);
        }
    }

    private void home(LinearLayout body) {
        section(body, WorkspaceText.tr("Недавние команды"), store.recentCommands.isEmpty() ? null : WorkspaceText.tr("Очистить"), () -> {
            if (!store.isWritable()) { activity.showToast(WorkspaceText.tr("Журнал недоступен для записи"), true); return; }
            confirm(WorkspaceText.tr("Очистить весь журнал недавних команд?"), () -> { store.clearRecentCommands(); renderContent(); });
        });
        EditText search = field(WorkspaceText.tr("Поиск по журналу"), journalQuery, false);
        search.setContentDescription(WorkspaceText.tr("Поиск по журналу"));
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(-1, -2);
        search.setMinHeight(dp(48)); searchParams.setMargins(0, 0, 0, dp(12)); body.addView(search, searchParams);
        ScrollView journal = new WorkspacePanelScrollView(activity);
        journal.setBackground(shape(PANEL, BORDER, 16)); journal.setClipToOutline(true);
        journal.setContentDescription(WorkspaceText.tr("Журнал последних 50 команд"));
        LinearLayout entries = column(); entries.setPadding(dp(12), dp(12), dp(12), dp(12));
        journal.addView(entries); body.addView(journal, new LinearLayout.LayoutParams(-1, 0, 1));
        TextView count = text("", 11, MUTED); count.setPadding(0, dp(8), 0, 0); body.addView(count);
        renderJournal(entries, count);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int after) {
                journalQuery = s.toString(); renderJournal(entries, count); journal.scrollTo(0, 0);
            }
            public void afterTextChanged(Editable s) {}
        });
        section(body, WorkspaceText.tr("Пресеты запуска"), "+", () -> editSession(null, false));
        ScrollView saved = new WorkspacePanelScrollView(activity);
        saved.setBackground(shape(PANEL, BORDER, 16)); saved.setClipToOutline(true);
        saved.setContentDescription(WorkspaceText.tr("Список пресетов запуска"));
        LinearLayout profiles = column(); profiles.setPadding(dp(12), dp(12), dp(12), dp(12));
        saved.addView(profiles); body.addView(saved, new LinearLayout.LayoutParams(-1, 0, 1));
        int savedCount = 0;
        for (WorkspaceStore.Session profile : sortedSessions()) if (profile.saved) { sessionCard(profiles, profile); savedCount++; }
        if (savedCount == 0) empty(profiles, WorkspaceText.tr("Пресетов запуска нет"), WorkspaceText.tr("Нажмите +, чтобы подготовить команду запуска."));
    }
    private void renderJournal(LinearLayout entries, TextView count) {
        entries.removeAllViews(); int shown = 0;
        if (store.recentCommands.isEmpty()) {
            entries.addView(text(WorkspaceText.tr("Команд пока нет"), 16, TEXT));
            TextView hint = text(WorkspaceText.tr("Здесь появятся команды, отправленные из поля ввода терминала."), 13, MUTED);
            hint.setPadding(0, dp(8), 0, 0); entries.addView(hint);
        }
        for (WorkspaceStore.RecentCommand recent : store.recentCommands) {
            if (!recent.command.toLowerCase(java.util.Locale.ROOT).contains(journalQuery.toLowerCase(java.util.Locale.ROOT))) continue;
            shown++;
            LinearLayout card = card(entries); card.setBackground(shape(Color.BLACK, BORDER, 12));
            LinearLayout labels = column(); TextView entry = text(recent.command, 13, TEXT);
            entry.setTypeface(Typeface.MONOSPACE); labels.addView(entry);
            String time = recent.sentAt > 0 ? new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                .format(new java.util.Date(recent.sentAt)) : WorkspaceText.tr("Ранее");
            TextView timestamp = text(time, 11, MUTED); timestamp.setPadding(0, dp(6), 0, 0); labels.addView(timestamp);
            Runnable insert = () -> recentMenu(recent);
            entry.setOnClickListener(v -> insert.run());
            labels.setOnClickListener(v -> insert.run()); card.setOnClickListener(v -> insert.run());
            card.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        }
        if (shown == 0 && !store.recentCommands.isEmpty()) entries.addView(text(WorkspaceText.tr("Ничего не найдено"), 14, MUTED));
        count.setText(store.recentCommands.size() + " / " + WorkspaceStore.MAX_RECENT_COMMANDS
            + (journalQuery.isEmpty() ? "" : WorkspaceText.tr(" · найдено: ") + shown));
    }
    private void recentMenu(WorkspaceStore.RecentCommand recent) {
        hideDialogKeyboard();
        LinearLayout body = previewBody(recent.command, true);
        TextView time = text(recent.sentAt > 0 ? new java.text.SimpleDateFormat("dd.MM · HH:mm", java.util.Locale.getDefault())
            .format(new java.util.Date(recent.sentAt)) : WorkspaceText.tr("Ранее"), 12, MUTED);
        time.setPadding(0, 0, 0, dp(10)); body.addView(time, 0);
        AlertDialog dialog = WorkspaceDialogs.builder(activity).setTitle(WorkspaceText.tr("Недавняя команда"))
            .setView(formScroll(body)).setPositiveButton(WorkspaceText.tr("Закрыть"), null).create();
        previewAction(body, WorkspaceText.tr("Вставить в терминал"), R.drawable.workspace_commands, () -> {
            dialog.dismiss(); WorkspaceStore.Command command = new WorkspaceStore.Command(); command.command = recent.command; insertCommand(command);
        });
        previewAction(body, WorkspaceText.tr("Сохранить в команды"), R.drawable.workspace_save, () -> { dialog.dismiss(); editCommand(null, recent.command); });
        previewAction(body, WorkspaceText.tr("Скопировать"), R.drawable.workspace_copy, () -> {
            ((android.content.ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE))
                .setPrimaryClip(android.content.ClipData.newPlainText(WorkspaceText.tr("Команда"), recent.command));
            dialog.dismiss(); activity.showToast(WorkspaceText.tr("Команда скопирована"), false);
        });
        dialog.show();
    }
    private void hideDialogKeyboard() {
        ((InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE))
            .hideSoftInputFromWindow(content.getWindowToken(), 0);
    }
    private LinearLayout previewBody(String value, boolean code) {
        LinearLayout body = column(); ScrollView pane = new ScrollView(activity); pane.setFillViewport(true);
        pane.setContentDescription(code ? WorkspaceText.tr("Полный текст команды") : WorkspaceText.tr("Полный текст заметки"));
        pane.setBackground(shape(0xff0b1319, BORDER, 12)); pane.setClipToOutline(true);
        TextView full = text(value, 14, TEXT); full.setPadding(dp(14), dp(14), dp(14), dp(14));
        full.setTextIsSelectable(true); if (code) full.setTypeface(Typeface.MONOSPACE);
        pane.addView(full); pane.setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(event.getActionMasked() != MotionEvent.ACTION_UP
                && event.getActionMasked() != MotionEvent.ACTION_CANCEL); return false;
        });
        int available = activity.getResources().getDisplayMetrics().heightPixels;
        int height = Math.min(dp(240), Math.max(dp(96), available / 3));
        body.addView(pane, new LinearLayout.LayoutParams(-1, height)); return body;
    }
    private void previewAction(LinearLayout body, String label, int icon, Runnable click) {
        TextView action = WorkspaceDialogs.action(activity, label, icon);
        action.setOnClickListener(v -> click.run()); LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(10); body.addView(action, params);
    }
    private void previewMetadata(LinearLayout body, String category, boolean favorite) {
        TextView label = text(category + (favorite ? "   ★" : ""), 12, favorite ? ACCENT : MUTED);
        label.setPadding(0, 0, 0, dp(12)); body.addView(label, 0);
    }
    private void openCommand(WorkspaceStore.Command command) {
        hideDialogKeyboard(); LinearLayout body = previewBody(command.command, true);
        previewMetadata(body, command.category, command.favorite);
        AlertDialog dialog = WorkspaceDialogs.builder(activity).setTitle(command.name)
            .setView(formScroll(body)).setPositiveButton(WorkspaceText.tr("Закрыть"), null).create();
        previewAction(body, WorkspaceText.tr("Вставить в терминал"), R.drawable.workspace_commands, () -> { dialog.dismiss(); insertCommand(command); });
        previewAction(body,command.favorite?WorkspaceText.tr("Убрать из избранного"):WorkspaceText.tr("В избранное"),R.drawable.workspace_star,()->{
            if(!store.isWritable()){activity.showToast(WorkspaceText.tr("Библиотека недоступна для записи"),true);return;}command.favorite=!command.favorite;store.save();dialog.dismiss();renderContent();
        });
        previewAction(body,WorkspaceText.tr("Редактировать"),R.drawable.workspace_edit,()->{dialog.dismiss();editCommand(command);});
        previewAction(body,WorkspaceText.tr("Удалить"),R.drawable.workspace_delete,()->{
            if(!store.isWritable()){activity.showToast(WorkspaceText.tr("Библиотека недоступна для записи"),true);return;}dialog.dismiss();confirm(WorkspaceText.tr("Удалить команду «")+command.name+"»?",()->{store.commands.remove(command);store.save();renderContent();});
        });
        dialog.show();
    }
    private void chips(LinearLayout body, String[] options, String selected, java.util.function.Consumer<String> click) {
        HorizontalScrollView scroller = new HorizontalScrollView(activity); scroller.setHorizontalScrollBarEnabled(false);
        LinearLayout items = row(); items.setPadding(0, dp(10), 0, dp(10));
        for (String option : options) {
            TextView chip = button(option, option.equals(selected), () -> click.accept(option));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2); lp.setMarginEnd(dp(8)); items.addView(chip, lp);
        }
        scroller.addView(items); body.addView(scroller);
    }
    private void searchSummary(LinearLayout body) {
        if (!query.isEmpty()) body.addView(button(WorkspaceText.tr("Поиск: ") + query + "  ×", false, () -> { query = ""; renderContent(); }));
    }
    private void commands(LinearLayout body) {
        if (!commandCategory.isEmpty() && !commandCategories().contains(commandCategory)) commandCategory = "";
        if (!noteCategory.isEmpty() && !noteCategories().contains(noteCategory)) noteCategory = "";
        libraryFilters(body, false);
        commandResults = libraryPanel(body, WorkspaceText.tr("Список сохранённых команд"));
        libraryFilters(body, true);
        noteResults = libraryPanel(body, WorkspaceText.tr("Список сохранённых заметок"));
        renderLibraryResults();
    }
    private void renderLibraryResults() {
        if (screen != Screen.COMMANDS || commandResults == null || noteResults == null) return;
        commandResults.removeAllViews(); noteResults.removeAllViews();
        int count = 0;
        for (WorkspaceStore.Command c : store.commands) {
            if ((!favorites || c.favorite) && (commandCategory.isEmpty() || commandCategory.equals(c.category))
                && matches(c.name + " " + c.command + " " + c.category)) { commandCard(commandResults, c); count++; }
        }
        if (count == 0) empty(commandResults, WorkspaceText.tr("Команд пока нет"), WorkspaceText.tr("Нажмите +, чтобы сохранить команду, или измените фильтры."));
        count = 0;
        for (WorkspaceStore.Note n : store.notes) {
            if ((!noteFavorites || n.favorite) && (noteCategory.isEmpty() || noteCategory.equals(n.category))
                && matches(n.name + " " + n.text + " " + n.category)) { noteCard(noteResults, n); count++; }
        }
        if (count == 0) empty(noteResults, WorkspaceText.tr("Заметок пока нет"), WorkspaceText.tr("Нажмите +, чтобы сохранить заметку, или измените фильтры."));
    }
    private void libraryFilters(LinearLayout body, boolean notes) {
        LinearLayout line = row(); line.setPadding(0, dp(10), 0, dp(10));
        line.setContentDescription(notes ? WorkspaceText.tr("Фильтры заметок") : WorkspaceText.tr("Фильтры команд"));
        LinearLayout items = row();
        String all = notes ? WorkspaceText.tr("Все заметки") : WorkspaceText.tr("Все команды");
        for (String option : new String[]{all, WorkspaceText.tr("Избранное")}) {
            boolean active = (notes ? noteCategory : commandCategory).isEmpty() && (notes ? noteFavorites : favorites) == option.equals(WorkspaceText.tr("Избранное"));
            TextView chip = button(option, active, () -> {
                if (notes) { noteFavorites = option.equals(WorkspaceText.tr("Избранное")); noteCategory = ""; } else { favorites = option.equals(WorkspaceText.tr("Избранное")); commandCategory = ""; } renderContent();
            });
            chip.setSelected(active);
            compactFilter(chip);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1); params.setMarginEnd(dp(4)); items.addView(chip, params);
        }
        String selected = notes ? noteCategory : commandCategory;
        TextView category = button((selected.isEmpty() ? WorkspaceText.tr("Категории") : selected) + " ▾", !selected.isEmpty(), () -> {
            if (notes) chooseNoteCategory(); else chooseCommandCategory();
        });
        category.setContentDescription(notes ? WorkspaceText.tr("Фильтр категорий заметок") : WorkspaceText.tr("Фильтр категорий команд"));
        category.setSelected(!selected.isEmpty());
        compactFilter(category);
        items.addView(category, new LinearLayout.LayoutParams(0, -2, 1));
        line.addView(items, new LinearLayout.LayoutParams(0, -2, 1));
        TextView add = button("+", true, () -> { if (notes) editNote(null); else editCommand(null); });
        add.setContentDescription(notes ? WorkspaceText.tr("Новая заметка") : WorkspaceText.tr("Новая команда"));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(48), dp(48)); params.setMarginStart(dp(8));
        line.addView(add, params); body.addView(line);
    }
    private void compactFilter(TextView filter) {
        filter.setPadding(dp(4), dp(8), dp(4), dp(8));
        filter.setMinWidth(0); filter.setMaxLines(2);
        filter.setEllipsize(TextUtils.TruncateAt.END);
    }
    private LinearLayout libraryPanel(LinearLayout body, String description) {
        ScrollView panel = new WorkspacePanelScrollView(activity);
        panel.setBackground(shape(PANEL, BORDER, 16)); panel.setClipToOutline(true); panel.setContentDescription(description);
        LinearLayout entries = column(); entries.setPadding(dp(12), dp(12), dp(12), dp(12));
        panel.addView(entries); body.addView(panel, new LinearLayout.LayoutParams(-1, 0, 1)); return entries;
    }
    private List<String> noteCategories() {
        Set<String> names = new LinkedHashSet<>();
        for (WorkspaceStore.Note note : store.notes) if (!note.category.isEmpty()) names.add(note.category);
        return new ArrayList<>(names);
    }
    private void chooseNoteCategory() {
        List<String> categories = noteCategories();
        List<String> labels = new ArrayList<>(); labels.add(WorkspaceText.tr("Все категории")); labels.addAll(categories);
        int selected = noteCategory.isEmpty() ? 0 : categories.indexOf(noteCategory) + 1;
        WorkspaceDialogs.builder(activity).setTitle(WorkspaceText.tr("Категории заметок"))
            .setSingleChoiceItems(labels.toArray(new String[0]), Math.max(0, selected), (dialog, which) -> {
                noteCategory = which == 0 ? "" : categories.get(which - 1); noteFavorites = false; dialog.dismiss(); renderContent();
            }).setNegativeButton(WorkspaceText.tr("Отмена"), null).show();
    }
    private void noteCard(LinearLayout parent, WorkspaceStore.Note note) {
        LinearLayout card = card(parent); card.setBackground(shape(Color.BLACK, BORDER, 12));
        cardLabels(card, note.name, note.text, note.category + (note.favorite ? "  ★" : ""), false);
        ((TextView) card.getChildAt(0)).setText("≡");
        card.setContentDescription(WorkspaceText.tr("Заметка ") + note.name); card.setOnClickListener(v -> openNote(note));

    }
    private void openNote(WorkspaceStore.Note note) {
        hideDialogKeyboard();LinearLayout body=previewBody(note.text,false);previewMetadata(body,note.category,note.favorite);
        AlertDialog dialog=WorkspaceDialogs.builder(activity).setTitle(note.name).setView(formScroll(body)).setPositiveButton(WorkspaceText.tr("Закрыть"),null).create();
        previewAction(body,note.favorite?WorkspaceText.tr("Убрать из избранного"):WorkspaceText.tr("В избранное"),R.drawable.workspace_star,()->{
            if(!store.isWritable()){activity.showToast(WorkspaceText.tr("Библиотека недоступна для записи"),true);return;}note.favorite=!note.favorite;store.save();dialog.dismiss();renderContent();
        });
        previewAction(body,WorkspaceText.tr("Редактировать"),R.drawable.workspace_edit,()->{dialog.dismiss();editNote(note);});
        previewAction(body,WorkspaceText.tr("Удалить"),R.drawable.workspace_delete,()->{
            if(!store.isWritable()){activity.showToast(WorkspaceText.tr("Библиотека недоступна для записи"),true);return;}dialog.dismiss();confirm(WorkspaceText.tr("Удалить заметку «")+note.name+"»?",()->{store.notes.remove(note);store.save();renderContent();});
        });dialog.show();
    }
    private void editNote(WorkspaceStore.Note existing) {
        if (!store.isWritable()) { activity.showToast(WorkspaceText.tr("Библиотека недоступна для записи"), true); return; }
        WorkspaceStore.Note draft = existing == null ? new WorkspaceStore.Note() : existing;
        LinearLayout form = column(); EditText name = labeled(form, WorkspaceText.tr("Название"), draft.name, false);
        EditText text = labeled(form, WorkspaceText.tr("Текст заметки"), draft.text, true);
        EditText category = categoryField(form,draft.category,noteCategories());
        CheckBox favorite = new CheckBox(activity); favorite.setText(WorkspaceText.tr("Добавить в избранное")); favorite.setTextColor(TEXT);
        favorite.setChecked(draft.favorite); form.addView(favorite);
        AlertDialog dialog = WorkspaceDialogs.builder(activity).setTitle(existing == null ? WorkspaceText.tr("Новая заметка") : WorkspaceText.tr("Редактировать заметку"))
            .setView(formScroll(form)).setPositiveButton(WorkspaceText.tr("Сохранить"), null).setNegativeButton(WorkspaceText.tr("Отмена"), null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (name.getText().toString().trim().isEmpty()) { name.setError(WorkspaceText.tr("Укажите название")); return; }
            if (text.getText().toString().trim().isEmpty()) { text.setError(WorkspaceText.tr("Укажите текст")); return; }
            draft.name = name.getText().toString().trim(); draft.text = text.getText().toString();
            draft.category = category.getText().toString().trim(); if (draft.category.isEmpty()) draft.category = WorkspaceText.tr("Общее");
            draft.favorite = favorite.isChecked(); if (existing == null) store.notes.add(draft);
            store.save(); dialog.dismiss(); renderContent();
        })); dialog.show();
    }
    private List<String> commandCategories() {
        Set<String> names = new LinkedHashSet<>();
        for (WorkspaceStore.Command command : store.commands) if (!command.category.isEmpty()) names.add(command.category);
        return new ArrayList<>(names);
    }
    private void chooseCommandCategory() {
        List<String> categories = commandCategories();
        List<String> labels = new ArrayList<>(); labels.add(WorkspaceText.tr("Все категории")); labels.addAll(categories);
        int selected = commandCategory.isEmpty() ? 0 : categories.indexOf(commandCategory) + 1;
        WorkspaceDialogs.builder(activity).setTitle(WorkspaceText.tr("Категории команд"))
            .setSingleChoiceItems(labels.toArray(new String[0]), Math.max(0, selected), (dialog, which) -> {
                commandCategory = which == 0 ? "" : categories.get(which - 1); favorites = false;
                dialog.dismiss(); renderContent();
            }).setNegativeButton(WorkspaceText.tr("Отмена"), null).show();
    }
    private boolean matches(String value) { return value.toLowerCase(java.util.Locale.ROOT).contains(query.toLowerCase(java.util.Locale.ROOT)); }
    private List<WorkspaceStore.Session> sortedSessions() {
        List<WorkspaceStore.Session> result = new ArrayList<>(store.sessions);
        result.sort((a, b) -> Long.compare(b.updated, a.updated)); return result;
    }
    private void sessions(LinearLayout body) {
        chips(body, new String[]{WorkspaceText.tr("Активные"), WorkspaceText.tr("Пресеты")}, savedOnly ? WorkspaceText.tr("Пресеты") : WorkspaceText.tr("Активные"), value -> { savedOnly = value.equals(WorkspaceText.tr("Пресеты")); renderContent(); });
        searchSummary(body); int count = 0;
        for (WorkspaceStore.Session s : sortedSessions()) {
            if ((savedOnly ? s.saved : liveFor(s) != null) && matches(s.name + " " + s.directory + " " + android.text.TextUtils.join(" ", s.startupCommands()))) { sessionCard(body, s); count++; }
        }
        if (count == 0) empty(body, savedOnly ? WorkspaceText.tr("Пресетов запуска нет") : WorkspaceText.tr("Нет активных сессий"), WorkspaceText.tr("Создайте сессию или запустите сохранённую."));
        body.addView(button(WorkspaceText.tr("+  Новая сессия"), true, () -> editSession(null)));
    }
    private void empty(LinearLayout parent, String title, String subtitle) {
        LinearLayout card = column(); card.setPadding(dp(18), dp(24), dp(18), dp(24)); card.setBackground(shape(PANEL, BORDER, 16));
        card.addView(text(title, 16, TEXT)); TextView sub = text(subtitle, 13, MUTED); sub.setPadding(0, dp(8), 0, 0); card.addView(sub); parent.addView(card);
    }
    private LinearLayout card(LinearLayout parent) {
        LinearLayout card = row(); card.setPadding(dp(12), dp(12), dp(8), dp(12)); card.setBackground(shape(PANEL, BORDER, 16));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, 0, 0, dp(8)); parent.addView(card, lp); return card;
    }
    private void cardLabels(LinearLayout card, String title, String subtitle, String status, boolean terminal) {
        TextView icon = text(terminal ? ">_" : "$", 18, ACCENT); icon.setGravity(Gravity.CENTER); icon.setTypeface(Typeface.MONOSPACE);
        icon.setBackground(shape(0xff102b27, 0xff183c36, 12)); LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(dp(42), dp(48)); ilp.setMarginEnd(dp(12)); card.addView(icon, ilp);
        LinearLayout labels = column(); TextView name = text(title, 15, TEXT); name.setTypeface(null, Typeface.BOLD); name.setSingleLine(); name.setEllipsize(TextUtils.TruncateAt.END); labels.addView(name);
        TextView sub = text(subtitle, 11, MUTED); sub.setSingleLine(); sub.setEllipsize(TextUtils.TruncateAt.MIDDLE); sub.setPadding(0, dp(4), 0, dp(4)); labels.addView(sub);
        TextView state = text(status, 10, terminal ? ACCENT : MUTED); labels.addView(state);
        card.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
    }
    private void commandCard(LinearLayout parent, WorkspaceStore.Command c) {
        LinearLayout card = card(parent); card.setBackground(shape(Color.BLACK, BORDER, 12));
        cardLabels(card, c.name, c.command, c.category + (c.favorite ? "  ★" : ""), false);
        card.setOnClickListener(v -> openCommand(c));
        card.setContentDescription(WorkspaceText.tr("Команда ")+c.name);
    }
    private void sessionCard(LinearLayout parent, WorkspaceStore.Session s) {
        TerminalSession running = liveFor(s); boolean active = s.localAi?localAi.active(s.id):running != null && running.isRunning();
        LinearLayout card = card(parent); cardLabels(card, s.name, s.startupCommands().isEmpty() ? compact(s.directory) : android.text.TextUtils.join(" → ", s.startupCommands()), active ? WorkspaceText.tr("● Активна") : running != null ? WorkspaceText.tr("Завершена") : WorkspaceText.tr("Сохранена"), true);
        if (screen == Screen.HOME) card.setBackground(shape(Color.BLACK, BORDER, 12));
        card.setOnClickListener(v -> openSavedSession(s));
        TextView run = button(active ? "›" : "▷", false, () -> openSavedSession(s)); run.setContentDescription(active ? WorkspaceText.tr("Открыть ") + s.name : WorkspaceText.tr("Запустить ") + s.name); card.addView(run,new LinearLayout.LayoutParams(dp(48),dp(48)));
        TextView more = button("⋮", false, () -> sessionMenu(s)); more.setContentDescription(WorkspaceText.tr("Действия с сессией ") + s.name); LinearLayout.LayoutParams moreParams=new LinearLayout.LayoutParams(dp(48),dp(48)); moreParams.setMarginStart(dp(8)); card.addView(more,moreParams);
    }
    private void openSavedSession(WorkspaceStore.Session profile) {
        if(!profile.environment.equals("codex")||liveFor(profile)!=null&&liveFor(profile).isRunning()){launch(profile);return;}
        LinearLayout body=column();body.addView(text(WorkspaceText.tr("Папка: ")+profile.directory,14,TEXT));body.addView(text(WorkspaceText.tr("Среда: ")+ProjectFiles.environmentLabel(profile),13,MUTED));
        body.addView(text(WorkspaceText.tr("Чат: ")+(profile.codexThreadName.isEmpty()?WorkspaceText.tr("Новый чат"):profile.codexThreadName),14,TEXT));
        body.addView(text(WorkspaceText.tr("Новый чат не заменяет прежнюю историю. Сохранение — по вашему выбору."),12,MUTED));
        WorkspaceDialogs.builder(activity).setTitle(profile.name).setView(body)
            .setPositiveButton(profile.codexChat.equals("id")?WorkspaceText.tr("Продолжить чат"):WorkspaceText.tr("Начать чат"),(d,w)->launch(profile))
            .setNeutralButton(WorkspaceText.tr("Новый чат"),(d,w)->{WorkspaceStore.Session next=profile.copy();next.name=profile.name;next.codexChat="new";next.codexThread="";next.codexThreadName="";next.codexSourceDirectory="";next.saved=false;launchNew(next,true);})
            .setNegativeButton(WorkspaceText.tr("Отмена"),null).show();
    }
    private void search() {
        EditText input = field(WorkspaceText.tr("Название, команда или каталог"), query, false);
        WorkspaceDialogs.builder(activity).setTitle(WorkspaceText.tr("Поиск")).setView(input)
            .setPositiveButton(WorkspaceText.tr("Найти"), (d, which) -> { query = input.getText().toString().trim(); renderContent(); })
            .setNegativeButton(WorkspaceText.tr("Отмена"), null).show();
    }
    private EditText field(String hint, String value, boolean multiline) {
        EditText e = new EditText(activity); e.setHint(hint); e.setText(value); e.setTextColor(TEXT); e.setHintTextColor(MUTED); e.setTextSize(14);
        e.setPadding(dp(12), dp(10), dp(12), dp(10)); e.setBackground(shape(PANEL, BORDER, 10));
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | (multiline ? InputType.TYPE_TEXT_FLAG_MULTI_LINE : 0));
        e.setShowSoftInputOnFocus(true);
        e.setOnClickListener(v->{prepareKeyboardWindow();e.requestFocus();e.post(()->((InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(e,0));});
        e.setOnFocusChangeListener((v,focused)->{if(focused)prepareKeyboardWindow();});
        e.setSingleLine(!multiline); if (multiline) { e.setMinLines(2); e.setMaxLines(6); e.setGravity(Gravity.TOP); }
        return e;
    }
    private EditText labeled(LinearLayout form, String label, String value, boolean multiline) {
        TextView title = text(label, 12, MUTED); title.setPadding(0, dp(12), 0, dp(6)); form.addView(title);
        EditText e = field(label, value, multiline); form.addView(e, new LinearLayout.LayoutParams(-1, -2)); return e;
    }
    private EditText categoryField(LinearLayout form,String value,List<String> categories){
        TextView title=text(WorkspaceText.tr("Категория"),12,MUTED);title.setPadding(0,dp(12),0,dp(6));form.addView(title);
        android.widget.AutoCompleteTextView input=new android.widget.AutoCompleteTextView(activity);input.setTextColor(TEXT);input.setHintTextColor(MUTED);input.setTextSize(14);input.setSingleLine(true);input.setThreshold(1);
        input.setPadding(dp(12),dp(10),dp(12),dp(10));input.setBackground(shape(PANEL,BORDER,10));input.setDropDownBackgroundDrawable(shape(PANEL,BORDER,10));
        input.setAdapter(new android.widget.ArrayAdapter<String>(activity,android.R.layout.simple_dropdown_item_1line,categories){
            @Override public View getView(int position,View recycled,ViewGroup parent){TextView row=(TextView)super.getView(position,recycled,parent);row.setTextColor(TEXT);return row;}
        });input.setText(value,false);input.setHint(WorkspaceText.tr("Категория"));input.setOnClickListener(v->{if(!categories.isEmpty())input.showDropDown();});
        LinearLayout line=row();line.addView(input,new LinearLayout.LayoutParams(0,-2,1));
        TextView choose=button("▾",false,()->{
            input.dismissDropDown();((InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(input.getWindowToken(),0);
            AlertDialog.Builder picker=WorkspaceDialogs.builder(activity).setTitle(WorkspaceText.tr("Выбрать категорию"));
            if(categories.isEmpty())picker.setMessage(WorkspaceText.tr("Категорий пока нет. Введите новую в поле."));
            else picker.setItems(categories.toArray(new String[0]),(dialog,index)->{input.setText(categories.get(index),false);input.setSelection(input.length());});
            picker.setNegativeButton(WorkspaceText.tr("Отмена"),null).show();
        });choose.setContentDescription(WorkspaceText.tr("Выбрать категорию"));LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(dp(48),dp(48));params.setMarginStart(dp(8));line.addView(choose,params);form.addView(line,new LinearLayout.LayoutParams(-1,-2));return input;
    }
    private ScrollView formScroll(LinearLayout form) {
        form.setPadding(dp(20), dp(4), dp(20), dp(18)); ScrollView scroll = new ScrollView(activity); scroll.addView(form); return scroll;
    }
    private void editCommand(WorkspaceStore.Command existing) {
        editCommand(existing, "");
    }
    private void editCommand(WorkspaceStore.Command existing, String initialCommand) {
        if (!store.isWritable()) { activity.showToast(WorkspaceText.tr("Библиотека недоступна для записи"), true); return; }
        WorkspaceStore.Command draft = existing == null ? new WorkspaceStore.Command() : existing;
        if (existing == null) draft.command = initialCommand;
        LinearLayout form = column(); EditText name = labeled(form, WorkspaceText.tr("Название"), draft.name, false);
        EditText command = labeled(form, WorkspaceText.tr("Команда"), draft.command, true);
        EditText categoryInput = categoryField(form,draft.category,commandCategories());
        CheckBox favorite = new CheckBox(activity); favorite.setText(WorkspaceText.tr("Добавить в избранное")); favorite.setTextColor(TEXT); favorite.setChecked(draft.favorite); form.addView(favorite);
        AlertDialog dialog = WorkspaceDialogs.builder(activity).setTitle(existing == null ? WorkspaceText.tr("Новая команда") : WorkspaceText.tr("Редактировать команду"))
            .setView(formScroll(form)).setPositiveButton(WorkspaceText.tr("Сохранить"), null).setNegativeButton(WorkspaceText.tr("Отмена"), null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (name.getText().toString().trim().isEmpty()) { name.setError(WorkspaceText.tr("Укажите название")); return; }
            if (command.getText().toString().trim().isEmpty()) { command.setError(WorkspaceText.tr("Укажите команду")); return; }
            draft.name = name.getText().toString().trim(); draft.command = command.getText().toString();
            draft.category = categoryInput.getText().toString().trim(); if (draft.category.isEmpty()) draft.category = WorkspaceText.tr("Общее");
            draft.favorite = favorite.isChecked();
            if (existing == null) store.commands.add(draft); store.save(); dialog.dismiss(); renderContent();
        })); dialog.show();
    }
    private boolean validDirectoryInput(EditText field) {
        String value = field.getText().toString().trim();
        if (value.indexOf('\0') >= 0 || value.contains("\n")) { field.setError(WorkspaceText.tr("Некорректный каталог")); return false; }
        if (!value.isEmpty() && !value.startsWith("/") && !value.equals("~") && !value.startsWith("~/")) {
            field.setError(WorkspaceText.tr("Используйте абсолютный путь или ~/каталог")); return false;
        }
        return true;
    }
    private void unavailableAgentScope(LinearLayout form){
        CheckBox scope=new CheckBox(activity);scope.setText(localAi.tr("Работать только в пределах проекта · недоступно","Work only within the project · unavailable"));scope.setTextColor(MUTED);scope.setEnabled(false);scope.setChecked(false);form.addView(scope);
        form.addView(text(localAi.tr("Для этого агента ограничение папкой не реализовано в нашей среде Android/proot. Рабочий каталог не является защитной границей; действуют штатные права агента.","Project-folder enforcement is not implemented for this agent in our Android/proot environment. The working directory is not a security boundary; native agent permissions apply."),12,MUTED));
    }
    private void editSession(WorkspaceStore.Session existing) { editSession(existing, true); }
    private void editSession(WorkspaceStore.Session existing, boolean launchAfterSave) {
        if(existing!=null&&existing.localAi&&!com.termux.BuildConfig.LOCAL_AI_ENABLED){localAiUnavailable();return;}
        if (!store.isWritable()) { activity.showToast(WorkspaceText.tr("Библиотека недоступна для записи"), true); return; }
        WorkspaceStore.Session draft = existing == null ? new WorkspaceStore.Session() : existing;
        LinearLayout form = column(); if(draft.restoreReview)form.addView(text(WorkspaceLanguage.code(activity).equals("ru")?"Импортированный пресет: проверьте среду, команды и пути. Отдельная восстановленная Ubuntu доступна своим пресетом запуска. Сохраните настройки перед запуском.":"Imported preset: review environment, commands and paths. A separate restored Ubuntu has its own launch preset. Save settings before launching.",14,MUTED)); EditText name = labeled(form, WorkspaceText.tr("Название сессии"), existing == null ? "" : draft.name, false);
        EditText directory = labeled(form, WorkspaceText.tr("Рабочий каталог"), existing == null ? TermuxConstants.TERMUX_HOME_DIR_PATH : draft.directory, false);
        String[] chosenEnvironment = {draft.localAi?"local":existing == null ? "termux" : (draft.aiAgent.equals("opencode") || draft.claudeAgent || draft.geminiAgent) ? "codex" : draft.environment.equals("legacy")?"termux":draft.environment};
        String[] chosenAgent={draft.geminiAgent?"gemini":draft.claudeAgent?"claude":draft.aiAgent.equals("opencode")?"opencode":"codex"};
        TextView chooseDirectory=button("▱",false,()->{});
        chooseDirectory.setContentDescription(WorkspaceText.tr("Выбрать рабочий каталог"));
        form.removeView(directory);LinearLayout directoryRow=row();directoryRow.addView(directory,new LinearLayout.LayoutParams(0,-2,1));directoryRow.addView(chooseDirectory,new LinearLayout.LayoutParams(dp(48),dp(48)));form.addView(directoryRow);

        String[] environments = {"termux", "ubuntu", "ssh", "codex", "local"};
        String[] environmentNames = {"Termux", "Ubuntu", WorkspaceText.tr("SSH · сервер"), "AI Agents", localAi.tr("Локальный ИИ","Local AI")};
        LocalAiUi.Selection localSelection=new LocalAiUi.Selection(draft);if(existing==null){localSelection.environment=LocalAiEnvironment.UBUNTU;localSelection.location="ubuntu";localSelection.project="/root/projects";}
        LinearLayout localFields=localAi.settings(localSelection,()->{});
        TextView environment = button("", false, () -> {});
        environment.setContentDescription(WorkspaceText.tr("Среда запуска"));
        LinearLayout sshFields = column();
        EditText sshHost = labeled(sshFields, WorkspaceText.tr("Адрес сервера"), draft.sshHost, false);
        EditText sshUser = labeled(sshFields, WorkspaceText.tr("Пользователь"), draft.sshUser, false);
        EditText sshPort = labeled(sshFields, WorkspaceText.tr("Порт"), Integer.toString(draft.sshPort), false);
        sshPort.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        String[] authModes = {"auto", "key", "password"};
        String[] authNames = {WorkspaceText.tr("Автоматически"), WorkspaceText.tr("Файл SSH-ключа"), WorkspaceText.tr("По паролю")};
        String[] chosenAuth = {draft.sshAuth};
        TextView auth = button("", false, () -> {}); auth.setContentDescription(WorkspaceText.tr("Авторизация SSH")); sshFields.addView(auth);
        LinearLayout keyFields = column(); sshFields.addView(keyFields);
        EditText identity = labeled(keyFields, WorkspaceText.tr("Файл SSH-ключа"), draft.sshIdentity, false);
        identity.setContentDescription(WorkspaceText.tr("Файл SSH-ключа"));
        keyFields.addView(button(WorkspaceText.tr("Выбрать ключ"), false, () -> chooseSshKey(identity)));
        LinearLayout passwordFields = column(); sshFields.addView(passwordFields);
        EditText password = labeled(passwordFields, WorkspaceText.tr("Пароль"), "", false); password.setContentDescription(WorkspaceText.tr("Пароль SSH"));
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        password.setSaveEnabled(false); password.setLongClickable(false);
        if (!draft.sshEncryptedPassword.isEmpty()) password.setHint(WorkspaceText.tr("Сохранён; оставьте пустым, чтобы не менять"));
        CheckBox showPassword = new CheckBox(activity); showPassword.setText(WorkspaceText.tr("Показать пароль")); showPassword.setTextColor(TEXT);
        showPassword.setOnCheckedChangeListener((b, checked) -> {
            password.setTransformationMethod(checked ? null : android.text.method.PasswordTransformationMethod.getInstance());
            password.setSelection(password.length());
        }); passwordFields.addView(showPassword);
        CheckBox rememberPassword = new CheckBox(activity); rememberPassword.setText(WorkspaceText.tr("Запомнить пароль")); rememberPassword.setTextColor(TEXT);
        rememberPassword.setChecked(!draft.sshEncryptedPassword.isEmpty()); passwordFields.addView(rememberPassword);
        passwordFields.addView(text(WorkspaceText.tr("Сохранение через Android Keystore. После удаления приложения пароль потребуется ввести заново."), 12, MUTED));
        Runnable updateAuth = () -> {
            auth.setText(WorkspaceText.tr("Авторизация: ") + authNames[Math.max(0, java.util.Arrays.asList(authModes).indexOf(chosenAuth[0]))]);
            keyFields.setVisibility(chosenAuth[0].equals("key") ? View.VISIBLE : View.GONE);
            passwordFields.setVisibility(chosenAuth[0].equals("password") ? View.VISIBLE : View.GONE);
        }; updateAuth.run();
        auth.setOnClickListener(v -> WorkspaceDialogs.builder(activity).setTitle(WorkspaceText.tr("Авторизация SSH"))
            .setSingleChoiceItems(authNames, java.util.Arrays.asList(authModes).indexOf(chosenAuth[0]), (d, which) -> {
                chosenAuth[0] = authModes[which]; updateAuth.run(); d.dismiss();
            }).setNegativeButton(WorkspaceText.tr("Отмена"), null).show());
        android.widget.CheckBox sshKeepOpen = new android.widget.CheckBox(activity);
        sshKeepOpen.setText(WorkspaceText.tr("Оставить подключение открытым")); sshKeepOpen.setTextColor(TEXT);
        sshKeepOpen.setChecked(draft.sshKeepOpen); sshFields.addView(sshKeepOpen);
        LinearLayout agentFields=column();agentFields.addView(text(localAi.tr("Права применяются при следующем запуске. Сохранение настроек не меняет работающий агент.","Permissions apply on next launch. Saving settings does not change a running agent."),12,MUTED));
        TextView agentButton=button("",false,()->{});agentFields.addView(agentButton);
        LinearLayout geminiFields=column();agentFields.addView(geminiFields);
        String[] geminiModel={draft.geminiModel},geminiMode={draft.geminiMode},geminiChat={draft.geminiChat},geminiThread={draft.geminiThread};
        String[] geminiAuth={draft.geminiAuth};
        String[] geminiAuthModes={"google","api","auto"};
        String[] geminiAuthNames={WorkspaceText.tr("Google-аккаунт"),WorkspaceText.tr("API-ключ"),WorkspaceText.tr("Выбрать в терминале")};
        String[] geminiAuthDescriptions={WorkspaceText.tr("Вход только через Google. Переключение на API-ключ в этой сессии заблокировано; при ошибке Google покажет причину."),WorkspaceText.tr("Вход только по Gemini API-ключу. Условия и оплата API отдельные от входа через Google."),WorkspaceText.tr("Штатное меню Gemini. Используется сохранённый в CLI способ входа; его можно изменить в терминале.")};
        TextView geminiAuthButton=button("",false,()->{});geminiFields.addView(geminiAuthButton);
        TextView geminiAuthDescription=text("",12,MUTED);geminiFields.addView(geminiAuthDescription);
        TextView geminiModelButton=button("",false,()->{});geminiFields.addView(geminiModelButton);
        TextView geminiModeButton=button("",false,()->{});geminiFields.addView(geminiModeButton);
        TextView geminiChatButton=button("",false,()->{});geminiFields.addView(geminiChatButton);
        String[] geminiModes={"default","auto_edit","plan"};
        String[] geminiModeNames={WorkspaceText.tr("Подтверждать действия"),WorkspaceText.tr("Разрешать редактирование"),WorkspaceText.tr("Планирование")};
        String[] geminiModeDescriptions={WorkspaceText.tr("Gemini запрашивает разрешения на действия в терминале."),WorkspaceText.tr("Gemini автоматически разрешает инструменты редактирования; остальные действия требуют подтверждения."),WorkspaceText.tr("Анализ и план без выполнения изменений. Это режим Gemini, а не системная песочница.")};
        TextView geminiAccessDetails=text("",12,MUTED);geminiFields.addView(geminiAccessDetails);unavailableAgentScope(geminiFields);
        Runnable updateGemini=()->{
            int authIndex=java.util.Arrays.asList(geminiAuthModes).indexOf(geminiAuth[0]);geminiAuthButton.setText(WorkspaceText.tr("Вход Gemini: ")+geminiAuthNames[authIndex]);geminiAuthDescription.setText(geminiAuthDescriptions[authIndex]);
            geminiModelButton.setText(WorkspaceText.tr("Модель: ")+(geminiModel[0].isEmpty()?WorkspaceText.tr("По умолчанию"):geminiModel[0]));
            geminiModeButton.setText(localAi.tr("Права: ","Permissions: ")+geminiModeNames[java.util.Arrays.asList(geminiModes).indexOf(geminiMode[0])]);
            geminiAccessDetails.setText(geminiModeDescriptions[java.util.Arrays.asList(geminiModes).indexOf(geminiMode[0])]);
            geminiChatButton.setText(WorkspaceText.tr("Чат: ")+(geminiChat[0].equals("new")?WorkspaceText.tr("Новый чат"):geminiChat[0].equals("choose")?WorkspaceText.tr("Выбрать чат"):geminiThread[0]));
        };updateGemini.run();
        geminiAuthButton.setOnClickListener(v->WorkspaceDialogs.builder(activity).setTitle(WorkspaceText.tr("Способ входа Gemini"))
            .setSingleChoiceItems(new String[]{geminiAuthNames[0]+"\n"+geminiAuthDescriptions[0],geminiAuthNames[1]+"\n"+geminiAuthDescriptions[1],geminiAuthNames[2]+"\n"+geminiAuthDescriptions[2]},java.util.Arrays.asList(geminiAuthModes).indexOf(geminiAuth[0]),(d,w)->{geminiAuth[0]=geminiAuthModes[w];updateGemini.run();d.dismiss();}).setNegativeButton(WorkspaceText.tr("Отмена"),null).show());
        geminiModelButton.setOnClickListener(v->WorkspaceDialogs.builder(activity).setTitle(WorkspaceText.tr("Модель Gemini CLI"))
            .setSingleChoiceItems(new String[]{WorkspaceText.tr("По умолчанию"),"Auto","Flash","Pro"},java.util.Arrays.asList("","auto","flash","pro").indexOf(geminiModel[0]),(d,w)->{geminiModel[0]=new String[]{"","auto","flash","pro"}[w];updateGemini.run();d.dismiss();}).setNegativeButton(WorkspaceText.tr("Отмена"),null).show());
        geminiModeButton.setOnClickListener(v->WorkspaceDialogs.builder(activity).setTitle(WorkspaceText.tr("Доступ Gemini CLI"))
            .setSingleChoiceItems(new String[]{geminiModeNames[0]+"\n"+geminiModeDescriptions[0],geminiModeNames[1]+"\n"+geminiModeDescriptions[1],geminiModeNames[2]+"\n"+geminiModeDescriptions[2]},java.util.Arrays.asList(geminiModes).indexOf(geminiMode[0]),(d,w)->{geminiMode[0]=geminiModes[w];updateGemini.run();d.dismiss();}).setNegativeButton(WorkspaceText.tr("Отмена"),null).show());
        geminiChatButton.setOnClickListener(v->WorkspaceDialogs.builder(activity).setTitle(WorkspaceText.tr("Чат Gemini CLI"))
            .setItems(new String[]{WorkspaceText.tr("Новый чат"),WorkspaceText.tr("Выбрать чат"),WorkspaceText.tr("Ввести ID чата")},(d,w)->{
                if(w<2){geminiChat[0]=w==0?"new":"choose";updateGemini.run();return;}
                EditText id=field(WorkspaceText.tr("ID чата Gemini CLI"),geminiChat[0].equals("id")?geminiThread[0]:"",false);
                AlertDialog picker=WorkspaceDialogs.builder(activity).setTitle(WorkspaceText.tr("ID чата Gemini CLI")).setView(id).setPositiveButton(WorkspaceText.tr("Выбрать"),null).setNegativeButton(WorkspaceText.tr("Отмена"),null).create();
                picker.setOnShowListener(x->picker.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(y->{String value=id.getText().toString().trim();if(!GeminiCli.validThread(value)){id.setError(WorkspaceText.tr("Некорректный ID"));return;}geminiThread[0]=value;geminiChat[0]="id";updateGemini.run();picker.dismiss();}));picker.show();
            }).setNegativeButton(WorkspaceText.tr("Отмена"),null).show());
        geminiFields.addView(text(WorkspaceText.tr("Gemini CLI запускается в Ubuntu. При первом запуске устанавливаются отдельные компоненты. Для бесплатной проверки выберите вход через личный Google-аккаунт; доступ зависит от региона и аккаунта. История отдельная и привязана к каталогу проекта. «Выбрать чат» открывает список в терминале после входа. Для продолжения выберите прежний каталог."),12,MUTED));
        LinearLayout claudeFields=column();agentFields.addView(claudeFields);
        String[] claudeModel={draft.claudeModel},claudeMode={draft.claudeMode},claudeChat={draft.claudeChat},claudeThread={draft.claudeThread};
        TextView claudeModelButton=button("",false,()->{});claudeFields.addView(claudeModelButton);
        TextView claudeModeButton=button("",false,()->{});claudeFields.addView(claudeModeButton);
        TextView claudeChatButton=button("",false,()->{});claudeFields.addView(claudeChatButton);
        String[] claudeModes={"manual","acceptEdits","plan"};
        String[] claudeModeNames={WorkspaceText.tr("Подтверждать действия"),WorkspaceText.tr("Разрешать редактирование"),WorkspaceText.tr("Планирование")};
        String[] claudeModeDescriptions={WorkspaceText.tr("Claude запрашивает разрешения на действия в терминале."),WorkspaceText.tr("Правки файлов и обычные файловые команды разрешаются автоматически; остальные действия требуют разрешения."),WorkspaceText.tr("Анализ и план без выполнения изменений. Это режим Claude, а не системная песочница.")};
        TextView claudeAccessDetails=text("",12,MUTED);claudeFields.addView(claudeAccessDetails);unavailableAgentScope(claudeFields);
        Runnable updateClaude=()->{
            claudeModelButton.setText(WorkspaceText.tr("Модель: ")+(claudeModel[0].isEmpty()?WorkspaceText.tr("По умолчанию"):claudeModel[0]));
            claudeModeButton.setText(localAi.tr("Права: ","Permissions: ")+claudeModeNames[java.util.Arrays.asList(claudeModes).indexOf(claudeMode[0])]);
            claudeAccessDetails.setText(claudeModeDescriptions[java.util.Arrays.asList(claudeModes).indexOf(claudeMode[0])]);
            claudeChatButton.setText(WorkspaceText.tr("Чат: ")+(claudeChat[0].equals("new")?WorkspaceText.tr("Новый чат"):claudeChat[0].equals("choose")?WorkspaceText.tr("Выбрать чат"):claudeThread[0]));
        };updateClaude.run();
        claudeModelButton.setOnClickListener(v->WorkspaceDialogs.builder(activity).setTitle(WorkspaceText.tr("Модель Claude Code"))
            .setSingleChoiceItems(new String[]{WorkspaceText.tr("По умолчанию"),"Sonnet","Opus","Haiku"},java.util.Arrays.asList("","sonnet","opus","haiku").indexOf(claudeModel[0]),(d,w)->{claudeModel[0]=new String[]{"","sonnet","opus","haiku"}[w];updateClaude.run();d.dismiss();}).setNegativeButton(WorkspaceText.tr("Отмена"),null).show());
        claudeModeButton.setOnClickListener(v->WorkspaceDialogs.builder(activity).setTitle(WorkspaceText.tr("Доступ Claude Code"))
            .setSingleChoiceItems(new String[]{claudeModeNames[0]+"\n"+claudeModeDescriptions[0],claudeModeNames[1]+"\n"+claudeModeDescriptions[1],claudeModeNames[2]+"\n"+claudeModeDescriptions[2]},java.util.Arrays.asList(claudeModes).indexOf(claudeMode[0]),(d,w)->{claudeMode[0]=claudeModes[w];updateClaude.run();d.dismiss();}).setNegativeButton(WorkspaceText.tr("Отмена"),null).show());
        claudeChatButton.setOnClickListener(v->WorkspaceDialogs.builder(activity).setTitle(WorkspaceText.tr("Чат Claude Code"))
            .setItems(new String[]{WorkspaceText.tr("Новый чат"),WorkspaceText.tr("Выбрать чат"),WorkspaceText.tr("Ввести ID чата")},(d,w)->{
                if(w<2){claudeChat[0]=w==0?"new":"choose";updateClaude.run();return;}
                EditText id=field(WorkspaceText.tr("ID чата Claude Code"),claudeChat[0].equals("id")?claudeThread[0]:"",false);
                AlertDialog picker=WorkspaceDialogs.builder(activity).setTitle(WorkspaceText.tr("ID чата Claude Code")).setView(id).setPositiveButton(WorkspaceText.tr("Выбрать"),null).setNegativeButton(WorkspaceText.tr("Отмена"),null).create();
                picker.setOnShowListener(x->picker.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(y->{String value=id.getText().toString().trim();if(!ClaudeCode.validThread(value)){id.setError(WorkspaceText.tr("Некорректный ID"));return;}claudeThread[0]=value;claudeChat[0]="id";updateClaude.run();picker.dismiss();}));picker.show();
            }).setNegativeButton(WorkspaceText.tr("Отмена"),null).show());
        claudeFields.addView(text(WorkspaceText.tr("Claude Code запускается в Ubuntu. Компоненты устанавливаются при первом запуске в отдельный каталог. Нужна подписка Claude или оплачиваемый API; ChatGPT Plus не подходит. Авторизация и история отдельные. «Выбрать чат» открывает список Claude в терминале. Доступность моделей зависит от аккаунта."),12,MUTED));
        LinearLayout openCodeFields=column();agentFields.addView(openCodeFields);
        CheckBox aiProjectScope=new CheckBox(activity);aiProjectScope.setText(localAi.tr("Ограничивать внешние пути · OpenCode","Restrict external paths · OpenCode"));aiProjectScope.setTextColor(TEXT);aiProjectScope.setChecked(draft.aiProjectOnly);aiProjectScope.setContentDescription("OpenCode project scope");
        String[] aiConnection={draft.aiConnection},aiModel={draft.aiModel},aiVariant={draft.aiVariant},aiMode={draft.aiMode},aiAccess={draft.aiAccess},aiChat={draft.aiChat},aiThread={draft.aiThread};
        TextView connectionButton=button("",false,()->{});openCodeFields.addView(connectionButton);
        TextView modelButton=button("",false,()->{});openCodeFields.addView(modelButton);
        TextView modeButton=button("",false,()->{});openCodeFields.addView(modeButton);
        TextView modeDescription=text("",12,MUTED);openCodeFields.addView(modeDescription);
        TextView aiAccessButton=button("",false,()->{});openCodeFields.addView(aiAccessButton);
        TextView aiAccessDescription=text("",12,MUTED);openCodeFields.addView(aiAccessDescription);openCodeFields.addView(aiProjectScope);
        openCodeFields.addView(text(localAi.tr("Включено: правила OpenCode запрещают внешние каталоги, shell-команды, субагентов, MCP и неизвестные инструменты. Доступны файловые инструменты проекта/рабочего дерева OpenCode в рамках выбранных прав. Это правила инструментов: они не гарантируют защиту от символьных ссылок и не создают системную песочницу. Выключено: внешние пути регулируются штатными правами OpenCode.","On: OpenCode rules deny external directories, shell, subagents, MCP and unknown tools. File tools operate in the OpenCode project/worktree according to selected permissions. This tool policy does not guarantee protection against symlink escapes and is not an OS sandbox. Off: native OpenCode permissions govern external paths."),12,MUTED));
        TextView chatButton=button("",false,()->{});openCodeFields.addView(chatButton);
        openCodeFields.addView(text(WorkspaceText.tr("OpenCode запускается в Ubuntu. При «Создать и запустить» компоненты проверяются и при необходимости устанавливаются в этой же вкладке. Node.js устанавливается отдельно от системного. Подключения и история отдельные от Codex; разрешения запрашиваются в терминале."),12,MUTED));
        Runnable updateOpenCode=()->{connectionButton.setText(WorkspaceText.tr("Подключение: ")+AiAgentsUi.title(activity,aiConnection[0]));modelButton.setText(WorkspaceText.tr("Модель: ")+(aiModel[0].isEmpty()?WorkspaceText.tr("По умолчанию"):aiModel[0]));modeButton.setText(WorkspaceText.tr("Режим: ")+ (aiMode[0].equals("build")?"Build":"Plan"));modeDescription.setText(AiAgentsUi.modeDescription(aiMode[0]));aiAccessButton.setText(localAi.tr("Права: ","Permissions: ")+AiAgentsUi.accessLabel(aiAccess[0]));aiAccessDescription.setText(AiAgentsUi.accessDescription(aiAccess[0]));chatButton.setText(WorkspaceText.tr("Чат: ")+(aiChat[0].equals("new")?WorkspaceText.tr("Новый чат"):aiThread[0]));};updateOpenCode.run();
        connectionButton.setOnClickListener(v->AiAgentsUi.chooseConnection(activity,aiConnection[0],(id,title)->{aiConnection[0]=id;aiModel[0]="";aiVariant[0]="";aiChat[0]="new";aiThread[0]="ses"+java.util.UUID.randomUUID().toString().replace("-","");updateOpenCode.run();}));
        modelButton.setOnClickListener(v->AiAgentsUi.chooseModel(activity,aiConnection[0],null,(provider,model,variant)->{aiModel[0]=provider+"/"+model;aiVariant[0]=variant;updateOpenCode.run();}));
        modeButton.setOnClickListener(v->WorkspaceDialogs.builder(activity).setTitle(WorkspaceText.tr("Режим OpenCode")).setSingleChoiceItems(new String[]{"Build\n"+AiAgentsUi.modeDescription("build"),"Plan\n"+AiAgentsUi.modeDescription("plan")},aiMode[0].equals("plan")?1:0,(d,w)->{aiMode[0]=w==0?"build":"plan";updateOpenCode.run();d.dismiss();}).setNegativeButton(WorkspaceText.tr("Отмена"),null).show());
        aiAccessButton.setOnClickListener(v->WorkspaceDialogs.builder(activity).setTitle(WorkspaceText.tr("Доступ OpenCode")).setSingleChoiceItems(new String[]{AiAgentsUi.accessLabel("full")+"\n"+AiAgentsUi.accessDescription("full"),AiAgentsUi.accessLabel("ask")+"\n"+AiAgentsUi.accessDescription("ask"),AiAgentsUi.accessLabel("read")+"\n"+AiAgentsUi.accessDescription("read")},java.util.Arrays.asList("full","ask","read").indexOf(aiAccess[0]),(d,w)->{String selected=new String[]{"full","ask","read"}[w];d.dismiss();if(selected.equals("full"))WorkspaceDialogs.builder(activity).setTitle(AiAgentsUi.accessLabel("full")).setMessage(AiAgentsUi.accessDescription("full")).setPositiveButton(WorkspaceText.tr("Выбрать"),(confirm,index)->{aiAccess[0]=selected;updateOpenCode.run();}).setNegativeButton(WorkspaceText.tr("Отмена"),null).show();else{aiAccess[0]=selected;updateOpenCode.run();}}).setNegativeButton(WorkspaceText.tr("Отмена"),null).show());
        chatButton.setOnClickListener(v->WorkspaceDialogs.builder(activity).setTitle(WorkspaceText.tr("Чаты OpenCode")).setItems(new String[]{WorkspaceText.tr("Новый чат"),WorkspaceText.tr("Выбрать чат"),WorkspaceText.tr("Ввести ID чата")},(d,w)->{if(w==0){aiChat[0]="new";aiThread[0]="ses"+java.util.UUID.randomUUID().toString().replace("-","");updateOpenCode.run();}else if(w==1)AiAgentsUi.chooseChat(activity,aiConnection[0],(id,title)->{aiThread[0]=id;aiChat[0]="resume";updateOpenCode.run();});else{EditText id=field("ID OpenCode",aiThread[0],false);AlertDialog picker=WorkspaceDialogs.builder(activity).setTitle("ID OpenCode").setView(id).setPositiveButton(WorkspaceText.tr("Выбрать"),null).setNegativeButton(WorkspaceText.tr("Отмена"),null).create();picker.setOnShowListener(x->picker.getButton(-1).setOnClickListener(y->{if(!AiAgents.validThread(id.getText().toString().trim())){id.setError(WorkspaceText.tr("Некорректный ID"));return;}aiThread[0]=id.getText().toString().trim();aiChat[0]="resume";updateOpenCode.run();picker.dismiss();}));picker.show();}}).setNegativeButton(WorkspaceText.tr("Отмена"),null).show());
        LinearLayout codexFields = column();
        String[] codexEnvironment = {draft.codexEnvironment}, codexAccess = {draft.codexAccess}, codexChat = {draft.codexChat};
        String[] codexThread = {draft.codexThread}, codexThreadName = {draft.codexThreadName};
        String[] codexSourceDirectory={draft.codexSourceDirectory};
        String[] codexConfirmedDirectory={existing!=null&&!draft.codexSourceDirectory.isEmpty()?draft.directory:""};
        TextView codexEnvButton = button("", false, () -> {}); codexFields.addView(codexEnvButton);
        TextView codexAccessButton = button("", false, () -> {}); codexFields.addView(codexAccessButton);
        TextView codexChatButton = button("", false, () -> {}); codexFields.addView(codexChatButton);
        codexEnvButton.setContentDescription(WorkspaceText.tr("Среда Codex")); codexAccessButton.setContentDescription(WorkspaceText.tr("Доступ Codex")); codexChatButton.setContentDescription(WorkspaceText.tr("Чат Codex"));
        String[] accessModes = {"full", "project", "read"};
        String[] accessNames = {WorkspaceText.tr("Полный доступ · без подтверждений"), WorkspaceText.tr("В пределах проекта · с подтверждениями"), WorkspaceText.tr("Только чтение · с подтверждениями")};
        String[] codexAccessDetails={
            localAi.tr("Без файловой песочницы Codex: файлы и команды доступны в пределах прав среды. Подтверждения настраиваются отдельно. Это не root Android.","No Codex filesystem sandbox: files and commands are available within environment permissions. Approvals are configured separately. This is not Android root."),
            localAi.tr("Штатный workspace-write: запись в проект и разрешённые служебные каталоги. Чтение вне проекта не обязательно запрещено. Подтверждения регулируются отдельно; поддержка песочницы зависит от Android/proot.","Native workspace-write: writes to the project and runtime-managed writable directories. Reads outside the project are not necessarily blocked. Approvals are separate; sandbox support depends on Android/proot."),
            localAi.tr("Штатный read-only Codex запрещает запись внутри песочницы; команды чтения могут быть доступны. Это не режим «только чат». Выход за ограничения зависит от политики подтверждений и поддержки Android/proot.","Native Codex read-only blocks sandbox writes; read commands may be available. This is not chat-only mode. Escalation depends on approval policy and Android/proot support.")};
        TextView codexAccessDescription=text("",12,MUTED);codexFields.addView(codexAccessDescription);
        boolean[] codexWriteScope={draft.codexAccess.equals("project")};
        CheckBox codexScope=new CheckBox(activity);codexScope.setText(localAi.tr("Ограничивать запись проектом · Codex","Restrict writes to the project · Codex"));codexScope.setTextColor(TEXT);codexScope.setChecked(codexWriteScope[0]);codexScope.setContentDescription("Codex project write scope");codexFields.addView(codexScope);
        codexFields.addView(text(localAi.tr("Штатный workspace-write ограничивает запись, но не всё чтение вне проекта. Возможны служебные разрешённые каталоги. Поддержка песочницы зависит от Android/proot; при ошибке запуска приложение не переключается на полный доступ. При включённых подтверждениях Codex может запросить выход за ограничения.","Native workspace-write restricts writes, not all reads outside the project; runtime-managed writable directories may exist. Sandbox support depends on Android/proot; startup errors never trigger a full-access fallback. With confirmations enabled, Codex may request approval to exceed restrictions."),12,MUTED));
        CheckBox codexConfirm=new CheckBox(activity);codexConfirm.setText(localAi.tr("Подтверждать действия · Codex","Confirm actions · Codex"));codexConfirm.setTextColor(TEXT);codexConfirm.setChecked(CodexSessionShell.approval(draft).equals("on-request"));codexConfirm.setContentDescription("Codex action confirmations");codexFields.addView(codexConfirm);
        codexFields.addView(text(localAi.tr("on-request: подтверждения запрашивает сам Codex по своей политике, не перед каждым действием. Выключено: never, запросы повышения прав не разрешаются; выбранная песочница сохраняется.","on-request: Codex requests approvals under its own policy, not before every action. Off: never, no escalation approvals; the selected sandbox remains in force."),12,MUTED));

        Runnable updateCodex = () -> {
            codexEnvButton.setText(WorkspaceText.tr("Среда Codex: ") + (codexEnvironment[0].equals("ubuntu") ? "Ubuntu" : "Termux"));
            codexAccessButton.setText(localAi.tr("Права: ","Permissions: ")+(codexAccess[0].equals("read")?localAi.tr("Чтение · read-only","Read · read-only"):localAi.tr("Чтение и запись","Read and write")));
            codexScope.setEnabled(!codexAccess[0].equals("read"));
            codexAccessDescription.setText(codexAccessDetails[Math.max(0,java.util.Arrays.asList(accessModes).indexOf(codexAccess[0]))]);
            codexChatButton.setText(WorkspaceText.tr("Чат: ") + (codexChat[0].equals("new") ? WorkspaceText.tr("Новый") : codexChat[0].equals("last") ? WorkspaceText.tr("Выберите конкретный чат") : codexChat[0].equals("id") ? codexThreadName[0] : WorkspaceText.tr("Выбрать при запуске")));
        }; updateCodex.run();
        codexEnvButton.setOnClickListener(v -> WorkspaceDialogs.builder(activity).setTitle(WorkspaceText.tr("Среда Codex"))
            .setSingleChoiceItems(new String[]{"Termux", "Ubuntu"}, codexEnvironment[0].equals("ubuntu") ? 1 : 0, (d, which) -> {
                codexEnvironment[0] = which == 0 ? "termux" : "ubuntu"; codexChat[0] = "new"; codexThread[0] = ""; codexThreadName[0] = "";codexSourceDirectory[0]="";codexConfirmedDirectory[0]="";
                if (which == 1 && directory.getText().toString().equals(TermuxConstants.TERMUX_HOME_DIR_PATH)) directory.setText("/root");
                else if (which == 0 && directory.getText().toString().equals("/root")) directory.setText(TermuxConstants.TERMUX_HOME_DIR_PATH);
                updateCodex.run(); d.dismiss();
            }).setNegativeButton(WorkspaceText.tr("Отмена"), null).show());
        codexAccessButton.setOnClickListener(v -> WorkspaceDialogs.builder(activity).setTitle(localAi.tr("Права Codex","Codex permissions"))
            .setSingleChoiceItems(new String[]{localAi.tr("Чтение · read-only","Read · read-only"),localAi.tr("Чтение и запись","Read and write")},codexAccess[0].equals("read")?0:1,(d,w)->{codexAccess[0]=w==0?"read":codexWriteScope[0]?"project":"full";updateCodex.run();d.dismiss();}).setNegativeButton(WorkspaceText.tr("Отмена"),null).show());
        codexScope.setOnCheckedChangeListener((b,on)->{codexWriteScope[0]=on;if(!codexAccess[0].equals("read"))codexAccess[0]=on?"project":"full";updateCodex.run();});
        codexChatButton.setOnClickListener(v -> WorkspaceDialogs.builder(activity).setTitle(WorkspaceText.tr("Чат Codex"))
            .setItems(new String[]{WorkspaceText.tr("Новый чат"), WorkspaceText.tr("Выбрать чат"), WorkspaceText.tr("Ввести ID чата")}, (d, which) -> {
                if (which == 0) { codexChat[0] = "new"; codexThread[0] = ""; codexThreadName[0]="";codexSourceDirectory[0]="";codexConfirmedDirectory[0]=""; updateCodex.run(); }
                else if (which == 1) chooseCodexChat(codexEnvironment[0], directory.getText().toString().trim(), (id, title, source) -> {
                    codexChat[0] = id.isEmpty() ? "new" : "id"; codexThread[0] = id; codexThreadName[0] = title;codexSourceDirectory[0]=source;codexConfirmedDirectory[0]=directory.getText().toString().trim(); updateCodex.run();
                });
                else {
                    EditText id = field(WorkspaceText.tr("ID чата Codex"), codexThread[0], false);
                    AlertDialog picker = WorkspaceDialogs.builder(activity).setTitle(WorkspaceText.tr("ID чата Codex")).setView(id).setPositiveButton(WorkspaceText.tr("Выбрать"), null).setNegativeButton(WorkspaceText.tr("Отмена"), null).create();
                    picker.setOnShowListener(x -> picker.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(y -> {
                        String value = id.getText().toString().trim(); if (!CodexSessionShell.validThread(value)) { id.setError(WorkspaceText.tr("Некорректный ID")); return; }
                        String env=codexEnvironment[0],cwd=directory.getText().toString().trim();
                        verifyCodexChat(env,cwd,value,(title,source)->{if(!picker.isShowing()||!env.equals(codexEnvironment[0])||!cwd.equals(directory.getText().toString().trim()))return;codexChat[0]="id";codexThread[0]=value;codexThreadName[0]=title;codexSourceDirectory[0]=source;codexConfirmedDirectory[0]=cwd;updateCodex.run();picker.dismiss();});
                    })); picker.show();
                }
            }).setNegativeButton(WorkspaceText.tr("Отмена"), null).show());
        chooseDirectory.setOnClickListener(v->{
            WorkspaceStore.Session location=new WorkspaceStore.Session();location.id=draft.id;location.environment=chosenEnvironment[0].equals("codex")&&!chosenAgent[0].equals("codex")?"ubuntu":chosenEnvironment[0];location.codexEnvironment=codexEnvironment[0];
            location.sshHost=sshHost.getText().toString().trim();location.sshUser=sshUser.getText().toString().trim();location.sshAuth=chosenAuth[0];location.sshIdentity=identity.getText().toString().trim();location.sshEncryptedPassword=draft.sshEncryptedPassword;
            try{location.sshPort=Integer.parseInt(sshPort.getText().toString());}catch(NumberFormatException e){sshPort.setError(WorkspaceText.tr("Порт от 1 до 65535"));return;}
            showFileBrowser(location,directory.getText().toString().trim(),path->{directory.setText(path);if(name.getText().toString().trim().isEmpty())name.setText(new File(path).getName());});
        });
        directory.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence t,int start,int count,int after){}
            public void onTextChanged(CharSequence t,int start,int before,int count){
                if(codexChat[0].equals("id")){codexConfirmedDirectory[0]="";updateCodex.run();}
            }
            public void afterTextChanged(Editable t){}
        });
        codexFields.addView(text(WorkspaceText.tr("Авторизация и история берутся из выбранной среды. Если вход не выполнен, Codex предложит авторизоваться. Для карточки управления нужна версия Codex с app-server и --remote. Полный доступ действует в пределах доступных среде файлов. На Android ограничения sandbox зависят от поддержки Codex; при ошибке запуск остановится, права не повышаются автоматически."), 12, MUTED));
        Runnable updateEnvironment = () -> {
            boolean local=chosenEnvironment[0].equals("local");
            for(int i=0;i<form.getChildCount();i++){View child=form.getChildAt(i);if(child instanceof TextView&&!(child instanceof EditText)&&((TextView)child).getText().toString().equals(WorkspaceText.tr("Рабочий каталог")))child.setVisibility(local?View.GONE:View.VISIBLE);}
            localFields.setVisibility(local?View.VISIBLE:View.GONE);directoryRow.setVisibility(local?View.GONE:View.VISIBLE);chooseDirectory.setVisibility(local?View.GONE:View.VISIBLE);
            sshFields.setVisibility(chosenEnvironment[0].equals("ssh") ? View.VISIBLE : View.GONE);
            agentFields.setVisibility(chosenEnvironment[0].equals("codex") ? View.VISIBLE : View.GONE);
            codexFields.setVisibility(chosenEnvironment[0].equals("codex") && chosenAgent[0].equals("codex") ? View.VISIBLE : View.GONE);
            openCodeFields.setVisibility(chosenAgent[0].equals("opencode") ? View.VISIBLE : View.GONE);
            geminiFields.setVisibility(chosenAgent[0].equals("gemini")?View.VISIBLE:View.GONE);
            claudeFields.setVisibility(chosenAgent[0].equals("claude")?View.VISIBLE:View.GONE);
            agentButton.setText(WorkspaceText.tr("Агент: ")+(chosenAgent[0].equals("gemini")?"Gemini CLI":chosenAgent[0].equals("claude")?"Claude Code":chosenAgent[0].equals("codex")?"Codex":"OpenCode"));
            int index = java.util.Arrays.asList(environments).indexOf(chosenEnvironment[0]);
            environment.setText(WorkspaceText.tr("Среда запуска: ") + environmentNames[Math.max(0, index)]);
        };
        updateEnvironment.run(); form.addView(environment); form.addView(sshFields); form.addView(agentFields); form.addView(codexFields);form.addView(localFields);
        agentButton.setOnClickListener(v->WorkspaceDialogs.builder(activity).setTitle(WorkspaceText.tr("Выбрать агента")).setItems(new String[]{"Codex","OpenCode","Claude Code","Gemini CLI"},(d,w)->{
            chosenAgent[0]=new String[]{"codex","opencode","claude","gemini"}[w];
            if(w>0){if(java.util.Arrays.asList("","Codex","OpenCode","Claude Code","Gemini CLI").contains(name.getText().toString().trim()))name.setText(w==1?"OpenCode":w==2?"Claude Code":"Gemini CLI");codexEnvironment[0]="ubuntu";if(directory.getText().toString().equals(TermuxConstants.TERMUX_HOME_DIR_PATH))directory.setText("/root");}
            updateEnvironment.run();
        }).setNegativeButton(WorkspaceText.tr("Отмена"),null).show());
        environment.setOnClickListener(v -> WorkspaceDialogs.builder(activity).setTitle(WorkspaceText.tr("Среда запуска"))
            .setSingleChoiceItems(WorkspaceDialogs.choiceAdapter(activity,environmentNames,java.util.Arrays.asList(environments).indexOf(chosenEnvironment[0]),position->position!=4||com.termux.BuildConfig.LOCAL_AI_ENABLED), java.util.Arrays.asList(environments).indexOf(chosenEnvironment[0]), (d, which) -> {
                if(which==4&&!com.termux.BuildConfig.LOCAL_AI_ENABLED)return;
                chosenEnvironment[0] = environments[which];
                if ((which == 1 || (which == 3 && (!chosenAgent[0].equals("codex") || codexEnvironment[0].equals("ubuntu")))) && directory.getText().toString().equals(TermuxConstants.TERMUX_HOME_DIR_PATH)) directory.setText("/root");
                else if (which == 2 && (directory.getText().toString().equals(TermuxConstants.TERMUX_HOME_DIR_PATH) || directory.getText().toString().equals("/root"))) directory.setText("~");
                else if ((which == 0 || (which == 3 && chosenAgent[0].equals("codex") && codexEnvironment[0].equals("termux"))) && directory.getText().toString().equals("/root")) directory.setText(TermuxConstants.TERMUX_HOME_DIR_PATH);
                if (which == 3 && name.getText().toString().trim().isEmpty()) name.setText("Codex");
                if(which==4&&java.util.Arrays.asList("","Bash").contains(name.getText().toString().trim()))name.setText(localAi.tr("Локальный чат","Local chat"));
                updateEnvironment.run(); d.dismiss();
            }).setNegativeButton(WorkspaceText.tr("Отмена"), null).show());
        LinearLayout commandFields = column(); form.addView(commandFields);
        List<EditText> startupFields = new ArrayList<>();
        startupFields.add(labeled(commandFields, WorkspaceText.tr("Команда 1 (необязательно)"), draft.startup, true));
        if (!draft.startup2.isEmpty() || !draft.startup3.isEmpty()) startupFields.add(labeled(commandFields, WorkspaceText.tr("Команда 2"), draft.startup2, true));
        if (!draft.startup3.isEmpty()) startupFields.add(labeled(commandFields, WorkspaceText.tr("Команда 3"), draft.startup3, true));
        TextView addCommand = button(WorkspaceText.tr("+ Команда"), false, () -> {});
        addCommand.setContentDescription(WorkspaceText.tr("Добавить команду запуска"));
        addCommand.setOnClickListener(v -> {
            if (startupFields.size() >= 3) return;
            startupFields.add(labeled(commandFields, WorkspaceText.tr("Команда ") + (startupFields.size() + 1), "", true));
            if (startupFields.size() == 3) addCommand.setVisibility(View.GONE);
        });
        if (startupFields.size() == 3) addCommand.setVisibility(View.GONE);
        form.addView(addCommand);
        Runnable toggleCommands = () -> {
            commandFields.setVisibility((chosenEnvironment[0].equals("codex")||chosenEnvironment[0].equals("local")) ? View.GONE : View.VISIBLE);
            addCommand.setVisibility(chosenEnvironment[0].equals("codex") || chosenEnvironment[0].equals("local") || startupFields.size() >= 3 ? View.GONE : View.VISIBLE);
        }; toggleCommands.run();
        environment.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence x, int a, int b, int c) {} public void onTextChanged(CharSequence x, int a, int b, int c) { toggleCommands.run(); }
            public void afterTextChanged(Editable x) {}
        });
        TextView hint = text(WorkspaceText.tr("Termux / Ubuntu: команды запускаются после готовности Bash в выбранной среде. Вход в Ubuntu выполняется автоматически — команду proot-distro login вводить не нужно. До трёх шагов, при ошибке остановка. После запуска Codex следующие шаги ждут его выхода. SSH: каталог и все команды выполняются на сервере с Bash. Пароль и подтверждение ключа — в терминале; следующая команда ждёт завершения предыдущей."), 12, MUTED);
        hint.setPadding(0, dp(16), 0, 0); form.addView(hint);
        environment.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence x,int a,int b,int c){}public void onTextChanged(CharSequence x,int a,int b,int c){hint.setVisibility(chosenEnvironment[0].equals("local")?View.GONE:View.VISIBLE);}public void afterTextChanged(Editable x){}});hint.setVisibility(chosenEnvironment[0].equals("local")?View.GONE:View.VISIBLE);
        CheckBox extendedDebug=new CheckBox(activity);extendedDebug.setText(WorkspaceText.tr("Расширенный дебаг-журнал"));extendedDebug.setTextColor(TEXT);extendedDebug.setChecked(draft.extendedDebug);form.addView(extendedDebug);
        form.addView(text(WorkspaceText.tr("Базовый журнал ведётся автоматически. Расширенный: события и коды ошибок, для Gemini также штатный debug с запуска. Без записи ввода и диалога. Изменение действует при следующем запуске."),12,MUTED));
        AlertDialog dialog = WorkspaceDialogs.builder(activity).setTitle(existing == null ? WorkspaceText.tr(launchAfterSave ? "Создать сессию" : "Создать пресет") : WorkspaceText.tr("Настройки сессии"))
            .setView(formScroll(form)).setPositiveButton(existing == null && launchAfterSave ? WorkspaceText.tr("Создать и запустить") : WorkspaceText.tr("Сохранить"), null).setNegativeButton(WorkspaceText.tr("Отмена"), null).create();
        if (existing == null && launchAfterSave) dialog.setButton(AlertDialog.BUTTON_NEUTRAL, WorkspaceText.tr("Пресеты"), (d, w) -> chooseSavedSession());
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (name.getText().toString().trim().isEmpty()) { name.setError(WorkspaceText.tr("Укажите название")); return; }
            if(chosenEnvironment[0].equals("local")){
                if(!localAi.validSelection(localSelection,()->dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()))return;
                if(existing!=null&&liveFor(existing)!=null&&liveFor(existing).isRunning()){activity.showToast(localAi.tr("Сначала закройте терминал","Close the terminal first"),true);return;}
                if(!localSelection.access.equals("chat")&&(localSelection.project.isEmpty()||localSelection.project.equals("/"))){activity.showToast(localAi.tr("Выберите папку проекта ниже корня файловой системы","Select a project folder below filesystem root"),true);return;}
                if(localAi.active(draft.id)){activity.showToast(localAi.tr("Сначала закройте локальную сессию","Close the local session first"),true);return;}
                localSelection.copyPrompts(draft);draft.localAccess=localSelection.access;draft.localProject=localSelection.project;draft.localEnvironment=localSelection.environment;draft.localCheckPaths=localSelection.checkPaths;draft.localConfirmActions=localSelection.confirmActions;
                draft.localLocation=localSelection.location;draft.localThreads=localSelection.threads;draft.localCpu=localSelection.cpu;draft.localContext=localSelection.context;draft.localMaxTokens=localSelection.maxTokens;draft.localAi=true;draft.localModel=localSelection.model;draft.localBackend=localSelection.backend;draft.localChat=localSelection.chat;
                draft.environment="termux";draft.name=name.getText().toString().trim();draft.directory="";draft.startup="";draft.startup2="";draft.startup3="";draft.handle="";draft.aiAgent="";draft.claudeAgent=false;draft.geminiAgent=false;draft.restoreReview=false;
                if(existing==null){draft.saved=!launchAfterSave;store.sessions.add(draft);}store.save();dialog.dismiss();if(launchAfterSave)launch(draft);else refresh();return;
            }
            if(existing!=null&&existing.localAi&&localAi.active(existing.id)){activity.showToast(localAi.tr("Сначала закройте локальную сессию","Close the local session first"),true);return;}
            draft.localAi=false;
            if (!validDirectoryInput(directory)) return;
            boolean selectedGemini=chosenEnvironment[0].equals("codex")&&chosenAgent[0].equals("gemini");
            if(existing!=null&&liveFor(existing)!=null&&liveFor(existing).isRunning()&&(existing.geminiAgent||selectedGemini)
                &&(existing.geminiAgent!=selectedGemini||!existing.geminiAuth.equals(geminiAuth[0])||!existing.geminiModel.equals(geminiModel[0])||!existing.geminiMode.equals(geminiMode[0])||!existing.geminiChat.equals(geminiChat[0])||!existing.geminiThread.equals(geminiThread[0]))) {
                activity.showToast(WorkspaceText.tr("Сначала закройте сессию, чтобы сменить агента, подключение или уровень доступа."),true);return;
            }
            boolean selectedClaude=chosenEnvironment[0].equals("codex")&&chosenAgent[0].equals("claude");
            if(existing!=null&&liveFor(existing)!=null&&liveFor(existing).isRunning()&&(existing.claudeAgent||selectedClaude)
                &&(existing.claudeAgent!=selectedClaude||!existing.claudeModel.equals(claudeModel[0])||!existing.claudeMode.equals(claudeMode[0])||!existing.claudeChat.equals(claudeChat[0])||!existing.claudeThread.equals(claudeThread[0]))) {
                activity.showToast(WorkspaceText.tr("Сначала закройте сессию, чтобы сменить агента, подключение или уровень доступа."),true);return;
            }
            if(existing!=null&&liveFor(existing)!=null&&liveFor(existing).isRunning()&&(!existing.aiAgent.isEmpty()||chosenAgent[0].equals("opencode"))&&(!existing.aiAgent.equals(chosenEnvironment[0].equals("codex")&&chosenAgent[0].equals("opencode")?"opencode":"")||!existing.aiConnection.equals(aiConnection[0])||!existing.aiAccess.equals(aiAccess[0])||!existing.environment.equals(chosenAgent[0].equals("opencode")&&chosenEnvironment[0].equals("codex")?"ubuntu":chosenEnvironment[0]))){activity.showToast(WorkspaceText.tr("Сначала закройте сессию, чтобы сменить агента, подключение или уровень доступа."),true);return;}
            if(chosenEnvironment[0].equals("codex")&&chosenAgent[0].equals("codex")&&codexChat[0].equals("id")&&!codexConfirmedDirectory[0].equals(directory.getText().toString().trim())) {
                String env=codexEnvironment[0],cwd=directory.getText().toString().trim(),thread=codexThread[0];
                verifyCodexChat(env,cwd,thread,(title,source)->{if(!dialog.isShowing()||!env.equals(codexEnvironment[0])||!cwd.equals(directory.getText().toString().trim())||!thread.equals(codexThread[0]))return;codexSourceDirectory[0]=source;codexConfirmedDirectory[0]=cwd;codexThreadName[0]=title;dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();});return;
            }

            int port = 22;
            if (chosenEnvironment[0].equals("ssh")) {
                if (!SshSessionShell.validHost(sshHost.getText().toString().trim())) { sshHost.setError(WorkspaceText.tr("Укажите адрес без пробелов, пользователя и порта")); return; }
                if (!SshSessionShell.validUser(sshUser.getText().toString().trim())) { sshUser.setError(WorkspaceText.tr("Укажите пользователя SSH")); return; }
                try { port = Integer.parseInt(sshPort.getText().toString()); } catch (NumberFormatException e) { port = 0; }
                if (!SshSessionShell.validPort(port)) { sshPort.setError(WorkspaceText.tr("Порт от 1 до 65535")); return; }
            }
            boolean sshPasswordMode = chosenEnvironment[0].equals("ssh") && chosenAuth[0].equals("password");
            if (chosenEnvironment[0].equals("ssh") && chosenAuth[0].equals("key")) {
                String path = identity.getText().toString().trim();
                if (!SshSessionShell.validIdentity(path) || !new File(resolve(path)).isFile() || !new File(resolve(path)).canRead()) {
                    identity.setError(WorkspaceText.tr("Укажите доступный файл ключа: /путь или ~/путь")); return;
                }
            }
            boolean sameConnection = draft.sshHost.equals(sshHost.getText().toString().trim())
                && draft.sshUser.equals(sshUser.getText().toString().trim()) && draft.sshPort == port;
            String encrypted = sshPasswordMode && rememberPassword.isChecked() && sameConnection ? draft.sshEncryptedPassword : "";
            char[] secret = new char[password.length()]; android.text.TextUtils.getChars(password.getText(), 0, secret.length, secret, 0);
            if (sshPasswordMode && (password.getText().toString().contains("\n") || password.getText().toString().contains("\r") || password.getText().toString().indexOf('\0') >= 0 || secret.length > 256)) {
                java.util.Arrays.fill(secret, '\0'); password.setError(WorkspaceText.tr("Пароль до 256 символов, без переносов строки")); return;
            }
            if (sshPasswordMode && rememberPassword.isChecked() && encrypted.isEmpty() && secret.length == 0) {
                password.setError(WorkspaceText.tr("Введите пароль для сохранения")); return;
            }
            if (sshPasswordMode && rememberPassword.isChecked() && secret.length > 0) {
                WorkspaceStore.Session binding = new WorkspaceStore.Session(); binding.id = draft.id;
                binding.sshHost = sshHost.getText().toString().trim(); binding.sshUser = sshUser.getText().toString().trim(); binding.sshPort = port;
                try { encrypted = SshPasswordVault.encrypt(binding, secret); }
                catch (java.security.GeneralSecurityException | RuntimeException e) {
                    java.util.Arrays.fill(secret, '\0'); password.setError(WorkspaceText.tr("Keystore недоступен. Отключите запоминание или повторите.")); return;
                }
            }
            char[] oldSecret = pendingSshPasswords.remove(draft.id); if (oldSecret != null) java.util.Arrays.fill(oldSecret, '\0');
            if (sshPasswordMode && secret.length > 0 && !rememberPassword.isChecked()) pendingSshPasswords.put(draft.id, secret.clone());
            java.util.Arrays.fill(secret, '\0'); password.setText("");
            draft.extendedDebug=extendedDebug.isChecked();
            draft.sshAuth = chosenAuth[0]; draft.sshIdentity = identity.getText().toString().trim(); draft.sshEncryptedPassword = encrypted;
            draft.sshHost = sshHost.getText().toString().trim(); draft.sshUser = sshUser.getText().toString().trim();
            draft.sshPort = port; draft.sshKeepOpen = sshKeepOpen.isChecked();
            draft.geminiAgent=selectedGemini;draft.geminiAuth=geminiAuth[0];draft.geminiModel=geminiModel[0];draft.geminiMode=geminiMode[0];draft.geminiChat=geminiChat[0];draft.geminiThread=geminiThread[0];
            draft.claudeAgent=selectedClaude;draft.claudeModel=claudeModel[0];draft.claudeMode=claudeMode[0];draft.claudeChat=claudeChat[0];draft.claudeThread=claudeThread[0];
            draft.aiAgent=chosenEnvironment[0].equals("codex")&&chosenAgent[0].equals("opencode")?"opencode":"";
            if(draft.aiAgent.equals("opencode"))draft.environment="ubuntu";
            if(!draft.aiConnection.equals(aiConnection[0]) || aiChat[0].equals("new") || !draft.aiSavedThread.equals(aiThread[0]))draft.aiSavedThread="";
            draft.aiConnection=aiConnection[0];draft.aiModel=aiModel[0];draft.aiVariant=aiVariant[0];draft.aiMode=aiMode[0];draft.aiAccess=aiAccess[0];draft.aiChat=aiChat[0];draft.aiThread=aiThread[0];
            draft.codexConfirmActions=codexConfirm.isChecked();draft.aiProjectOnly=aiProjectScope.isChecked();draft.codexEnvironment = codexEnvironment[0]; draft.codexAccess = codexAccess[0]; draft.codexChat = codexChat[0]; draft.codexThread = codexThread[0]; draft.codexThreadName = codexThreadName[0];draft.codexSourceDirectory=codexSourceDirectory[0];
            draft.environment = (draft.aiAgent.equals("opencode")||draft.claudeAgent||draft.geminiAgent)?"ubuntu":chosenEnvironment[0]; draft.name = name.getText().toString().trim(); draft.directory = directory.getText().toString().trim(); draft.startup = startupFields.get(0).getText().toString();
            draft.startup2 = startupFields.size() > 1 ? startupFields.get(1).getText().toString() : "";
            draft.startup3 = startupFields.size() > 2 ? startupFields.get(2).getText().toString() : ""; if(draft.aiAgent.equals("opencode")||draft.claudeAgent||draft.geminiAgent){draft.startup="";draft.startup2="";draft.startup3="";}
            draft.updated = System.currentTimeMillis();
            draft.restoreReview=false;
            if (existing == null) { draft.saved = !launchAfterSave; store.sessions.add(draft); }
            TerminalSession live = liveFor(draft); if (live != null) live.mSessionName = draft.name;
            store.save(); dialog.dismiss(); refresh();
            if (existing == null && launchAfterSave) launch(draft);
            else if (live != null) activity.showToast(localAi.tr("Имя обновлено. Каталог, команды и права применятся при следующем запуске; текущая сессия не изменена.","Name updated. Directory, commands and permissions apply on next launch; the current session is unchanged."), true);
        }));
        dialog.setOnDismissListener(d -> password.setText("")); dialog.show();
    }
    private interface ChatChoice {void chosen(String id,String name,String sourceDirectory);}
    private void chooseCodexChat(String environment,String directory,ChatChoice choice) {
        String cwd=CodexProjects.directory(environment,directory);
        if(cwd.isEmpty()){activity.showToast(WorkspaceText.tr("Укажите абсолютный рабочий каталог"),true);return;}
        LinearLayout body=column();body.setPadding(dp(12),dp(4),dp(12),dp(8));
        TextView loading=text(WorkspaceText.tr("Загрузка истории…"),13,MUTED);body.addView(loading);
        EditText search=field(WorkspaceText.tr("Найти чат или каталог"),"",false);body.addView(search);
        LinearLayout filters=row();body.addView(filters);
        TextView all=button(WorkspaceText.tr("Все чаты"),false,()->{}),project=button(WorkspaceText.tr("Чаты этой папки"),false,()->{});all.setTextSize(12);project.setTextSize(12);all.setTextColor(ACCENT);
        filters.addView(all,new LinearLayout.LayoutParams(0,-2,1));LinearLayout.LayoutParams projectParams=new LinearLayout.LayoutParams(0,-2,1);projectParams.setMarginStart(dp(6));filters.addView(project,projectParams);
        ScrollView scroll=new ScrollView(activity);LinearLayout rows=column();scroll.addView(rows);body.addView(scroll,new LinearLayout.LayoutParams(-1,dp(280)));
        AlertDialog dialog=WorkspaceDialogs.builder(activity).setTitle(WorkspaceText.tr("Выбрать чат Codex")).setView(body)
            .setPositiveButton(WorkspaceText.tr("Продолжить выбранный чат"),null).setNeutralButton(WorkspaceText.tr("Новый чат в этой папке"),(d,w)->choice.chosen("",WorkspaceText.tr("Новый чат"),""))
            .setNegativeButton(WorkspaceText.tr("Отмена"),null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false));dialog.show();
        codexReader.execute(()->{
            List<CodexChatCatalog.Entry> chats=new ArrayList<>();String failure=null;
            WorkspaceStore.Session profile=new WorkspaceStore.Session();profile.codexEnvironment=environment;
            try(CodexRpc rpc=CodexRpc.history(activity,profile)) {
                String cursor=null;for(int page=0;page<10;page++) {
                    org.json.JSONObject data=rpc.call("thread/list",CodexChatCatalog.params(cursor));org.json.JSONArray entries=data.getJSONArray("data");
                    for(int i=0;i<entries.length();i++){CodexChatCatalog.Entry entry=CodexChatCatalog.parse(entries.getJSONObject(i));if(entry!=null)chats.add(entry);}
                    cursor=data.isNull("nextCursor")?null:data.optString("nextCursor",null);if(cursor==null||cursor.isEmpty())break;
                }
            }catch(Exception e){failure=WorkspaceText.tr("Не удалось прочитать всю историю. Проверьте Codex и авторизацию в выбранной среде.");}
            String error=failure;activity.runOnUiThread(()->{
                if(closed||!dialog.isShowing())return;
                loading.setText(error!=null?error:(environment.equals("ubuntu")?"Ubuntu":"Termux")+WorkspaceText.tr(" · новый каталог: ")+cwd+(chats.isEmpty()?WorkspaceText.tr("\nСохранённых чатов пока нет."):""));
                int[] selected={-1};boolean[] projectOnly={false};Runnable[] render=new Runnable[1];
                render[0]=()->{
                    rows.removeAllViews();all.setTextColor(projectOnly[0]?MUTED:ACCENT);project.setTextColor(projectOnly[0]?ACCENT:MUTED);int shown=0;
                    for(int i=0;i<chats.size();i++) {
                        CodexChatCatalog.Entry entry=chats.get(i);if(!CodexChatCatalog.visible(entry,environment,cwd,projectOnly[0],search.getText().toString()))continue;
                        final int index=i;String subtitle=entry.cwd.isEmpty()?WorkspaceText.tr("Каталог неизвестен"):entry.cwd;
                        if(entry.updated>0)subtitle+=" · "+android.text.format.DateFormat.format("dd.MM HH:mm",entry.updated*1000);
                        TextView item=button(entry.name+(selected[0]==i?"  ✓":"")+"\n"+subtitle,false,()->{selected[0]=index;dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);render[0].run();});
                        item.setTextSize(13);item.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);item.setTextColor(selected[0]==i?ACCENT:TEXT);item.setMaxLines(3);item.setEllipsize(TextUtils.TruncateAt.END);
                        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(6);rows.addView(item,lp);shown++;
                    }
                    if(shown==0)rows.addView(text(projectOnly[0]?WorkspaceText.tr("Чатов этой папки нет. Выберите «Все чаты»."):WorkspaceText.tr("Нет подходящих чатов"),13,MUTED));
                    if(selected[0]>=0&&!CodexChatCatalog.visible(chats.get(selected[0]),environment,cwd,projectOnly[0],search.getText().toString()))selected[0]=-1;
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(selected[0]>=0);
                };
                all.setOnClickListener(v->{projectOnly[0]=false;render[0].run();});project.setOnClickListener(v->{projectOnly[0]=true;render[0].run();});
                search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence t,int a,int c,int f){}public void onTextChanged(CharSequence t,int a,int b,int c){render[0].run();}public void afterTextChanged(Editable t){}});render[0].run();
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{if(selected[0]<0)return;CodexChatCatalog.Entry entry=chats.get(selected[0]);verifyCodexChat(environment,cwd,entry.id,(title,source)->{if(dialog.isShowing()){choice.chosen(entry.id,title,source);dialog.dismiss();}});});
            });
        });
    }
    private void confirmCodexDirectory(String environment,String target,String source,Runnable accepted) {
        if(CodexProjects.matches(environment,target,source)){accepted.run();return;}
        WorkspaceDialogs.builder(activity).setTitle(WorkspaceText.tr("Продолжить чат в другой папке?"))
            .setMessage(WorkspaceText.tr("Исходный каталог:\n")+source+WorkspaceText.tr("\n\nНовый рабочий каталог:\n")+CodexProjects.directory(environment,target)+WorkspaceText.tr("\n\nИстория останется прежней. В ней могут быть старые задачи и пути. Файлы не переносятся; права остаются выбранными в настройках сессии."))
            .setPositiveButton(WorkspaceText.tr("Продолжить в новой папке"),(d,w)->accepted.run()).setNegativeButton(WorkspaceText.tr("Отмена"),null).show();
    }
    private void verifyCodexChat(String environment,String cwd,String id,java.util.function.BiConsumer<String,String> choice) {
        TextView waiting=text(WorkspaceText.tr("Проверка истории и каталога…"),14,MUTED);waiting.setPadding(dp(16),dp(16),dp(16),dp(16));
        AlertDialog progress=WorkspaceDialogs.builder(activity).setTitle(WorkspaceText.tr("Проверка чата")).setView(waiting).setNegativeButton(WorkspaceText.tr("Отмена"),null).show();
        codexReader.execute(()->{
            WorkspaceStore.Session profile=new WorkspaceStore.Session();profile.codexEnvironment=environment;String title=null,source=null;
            try(CodexRpc rpc=CodexRpc.history(activity,profile)){
                org.json.JSONObject chat=rpc.call("thread/read",new org.json.JSONObject().put("threadId",id).put("includeTurns",false)).getJSONObject("thread");
                String original=chat.optString("cwd","");if(original.startsWith("/")&&!CodexProjects.directory(environment,original).isEmpty()){source=original;title=chat.optString("name",id);if(title.isEmpty()||title.equals("null"))title=id;}
            }catch(Exception ignored){}
            String result=title,origin=source;activity.runOnUiThread(()->{if(closed||!progress.isShowing())return;progress.dismiss();if(result==null)activity.showToast(WorkspaceText.tr("Чат недоступен или исходный каталог неизвестен. Проверьте ID и выбранную среду."),true);else confirmCodexDirectory(environment,cwd,origin,()->{if(!closed)choice.accept(result,origin);});});
        });
    }
    private WorkspaceStore.Session currentCodex() {
        if(selectedLocal!=null)return null;
        TerminalSession live = activity.getCurrentSession();
        if (live == null || !live.isRunning()) return null;
        WorkspaceStore.Session profile = store.findByHandle(live.mHandle);
        return profile != null && profile.environment.equals("codex") ? profile : null;
    }
    private void renderCodexStatus() {
        if(selectedLocal!=null){codexStatus.removeAllViews();codexStatus.setVisibility(screen==Screen.TERMINAL?View.VISIBLE:View.GONE);LocalAiUi.Panel p=localAi.existing(selectedLocal.id);if(p!=null){View control=p.start;if(control.getParent()!=null)((ViewGroup)control.getParent()).removeView(control);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,dp(40));lp.topMargin=dp(4);lp.bottomMargin=dp(4);codexStatus.addView(control,lp);View dots=p.activityDots;if(dots.getParent()!=null)((ViewGroup)dots.getParent()).removeView(dots);codexStatus.addView(dots,new LinearLayout.LayoutParams(0,dp(40),1));}return;}
        if (codexStatus == null) return;
        codexStatus.removeAllViews();
        TerminalSession aiLive=activity.getCurrentSession();WorkspaceStore.Session ai=aiLive==null?null:store.findByHandle(aiLive.mHandle);
        if(ai!=null&&ai.geminiAgent&&aiLive.isRunning()) {
            codexStatus.setVisibility(screen==Screen.TERMINAL?View.VISIBLE:View.GONE);
            TextView chip=button("Gemini CLI · "+WorkspaceText.tr("При запуске: ")+(ai.geminiModel.isEmpty()?WorkspaceText.tr("По умолчанию"):ai.geminiModel)+" · "+ai.geminiMode,false,()->WorkspaceDialogs.builder(activity).setTitle("Gemini CLI").setMessage(WorkspaceText.tr("Плашка показывает параметры запуска Gemini. Текущую модель смотрите в терминале; /model меняет модель, /stats показывает статистику, /auth открывает вход, /resume — историю. Для продолжения выберите тот же каталог проекта. Бесплатные квоты зависят от аккаунта; живые лимиты здесь не считываются.")).setPositiveButton(WorkspaceText.tr("Закрыть"),null).show());
            chip.setTextSize(12);chip.setMaxLines(2);chip.setEllipsize(TextUtils.TruncateAt.END);chip.setTextColor(ACCENT);
            LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-2,-2);params.topMargin=dp(6);params.bottomMargin=dp(6);codexStatus.addView(chip,params);return;
        }
        if(ai!=null&&ai.claudeAgent&&aiLive.isRunning()) {
            codexStatus.setVisibility(screen==Screen.TERMINAL?View.VISIBLE:View.GONE);
            TextView chip=button("Claude Code · "+WorkspaceText.tr("При запуске: ")+(ai.claudeModel.isEmpty()?WorkspaceText.tr("По умолчанию"):ai.claudeModel)+" · "+ai.claudeMode,false,()->WorkspaceDialogs.builder(activity).setTitle("Claude Code").setMessage(WorkspaceText.tr("Модель и разрешения задаются при запуске. Текущее состояние: /status; смена модели: /model; разрешения: /permissions; вход: /login. Эти команды вводятся в терминале Claude. История сохраняется самим Claude; выберите чат или укажите ID при новом запуске.")).setPositiveButton(WorkspaceText.tr("Закрыть"),null).show());
            chip.setTextSize(12);chip.setMaxLines(2);chip.setEllipsize(TextUtils.TruncateAt.END);chip.setTextColor(ACCENT);
            LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-2,-2);params.topMargin=dp(6);params.bottomMargin=dp(6);codexStatus.addView(chip,params);return;
        }
        if(ai!=null&&ai.aiAgent.equals("opencode")&&aiLive.isRunning()){codexStatus.setVisibility(screen==Screen.TERMINAL?View.VISIBLE:View.GONE);TextView chip=button("OpenCode · "+AiAgentsUi.title(activity,ai.aiConnection)+" · "+ai.aiMode,false,()->AiAgentsUi.active(activity,ai,()->{store.save();renderCodexStatus();}));LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-2,-2);params.topMargin=dp(6);params.bottomMargin=dp(6);codexStatus.addView(chip,params);return;}
        WorkspaceStore.Session profile = currentCodex();
        CodexStatus prior = profile == null ? null : codexStatuses.get(profile.id);
        boolean ended = profile != null && prior != null && prior.updated > 0 && liveFor(profile) == null;
        codexStatus.setVisibility(screen == Screen.TERMINAL && profile != null && !ended ? View.VISIBLE : View.GONE);
        if (profile == null) return;
        CodexStatus status = codexStatuses.get(profile.id);
        String label = status == null ? WorkspaceText.tr("Codex · подключение…") : !status.error.isEmpty() ? WorkspaceText.tr("Codex · нет данных ›") : (status.model.isEmpty()?"Codex":status.model)+(status.effort.isEmpty()?"":" · "+effortName(status.effort))+"  "+status.summary()+" ›";
        TextView chip = button(label, false, () -> showCodexStatus(profile)); chip.setMinHeight(dp(34)); chip.setTextSize(12); chip.setTextColor(ACCENT);
        chip.setMaxLines(2);chip.setEllipsize(TextUtils.TruncateAt.END);chip.setPadding(dp(12), dp(4), dp(12), dp(4)); chip.setContentDescription(WorkspaceText.tr("Статус и модель Codex"));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2); params.topMargin=dp(6);params.bottomMargin = dp(6); codexStatus.addView(chip, params);
    }
    private void loadCodexStatus(WorkspaceStore.Session profile, Runnable done) {
        if (closed || codexLoading) return;
        codexLoading = true;
        String socketAtRequest=profile.codexSocketId,handleAtRequest=profile.handle;
        codexReader.execute(() -> {
            CodexStatus result;
            try { result = CodexStatus.local(activity.getFilesDir(), profile); }
            catch (Exception e) { result = new CodexStatus(); result.error = CodexRpc.diagnostic(e); }
            CodexStatus status = result;
            activity.runOnUiThread(() -> {
                codexLoading = false; if (closed||!socketAtRequest.equals(profile.codexSocketId)||!handleAtRequest.equals(profile.handle)) return;
                TerminalSession terminal=liveFor(profile);
                if(terminal!=null && status.effort.isEmpty() && !status.model.isEmpty()) {
                    String changed=CodexModelSelection.acknowledgement(codexScreen(terminal));
                    String prefix="Model changed to "+status.model+" ";
                    if(changed.startsWith(prefix))status.effort=changed.substring(prefix.length());
                }
                String[] choice=codexConfirmedChoices.get(profile.codexSocketId);
                if(choice!=null && choice[0].equals(status.thread) && status.error.isEmpty()) {
                    String changed=terminal==null?"":CodexModelSelection.acknowledgement(codexScreen(terminal));
                    if(!changed.isEmpty() && !changed.equals(choice[3])) {
                        String[] parts=changed.substring("Model changed to ".length()).split(" ",2);
                        if(parts.length==2) {choice[1]=parts[0];choice[2]=parts[1];choice[3]=changed;}
                    }
                    status.model=choice[1];status.effort=choice[2];
                } else if(choice!=null)codexConfirmedChoices.remove(profile.codexSocketId);
                if(CodexProjects.bind(profile,status)){store.save();renderContent();}
                codexStatuses.put(profile.id, status); renderCodexStatus(); if (done != null) done.run();
            });
        });
    }
    private String codexResetTime(CodexStatus status, String window) {
        org.json.JSONObject limit = status.limits.optJSONObject(window);
        long reset = limit == null ? 0 : limit.optLong("resetsAt");
        return reset > 0 ? WorkspaceText.tr("Обновится ") + android.text.format.DateFormat.format("dd.MM HH:mm", reset * 1000) : WorkspaceText.tr("Время обновления недоступно");
    }
    private void showCodexStatus(WorkspaceStore.Session profile) {
        CodexStatus status = codexStatuses.get(profile.id);
        AlertDialog[] shown = {null};
        LinearLayout form = column();
        if (status == null || !status.error.isEmpty()) {
            form.addView(text(status == null ? WorkspaceText.tr("Получение статуса Codex…") : WorkspaceText.statusError(status.error), 14, MUTED));
        } else {
            form.addView(button(WorkspaceText.tr("Модель: ") + (status.model.isEmpty() ? WorkspaceText.tr("нет данных") : status.model) + " ›", false, () -> { if (shown[0] != null) shown[0].dismiss(); chooseCodexModel(profile, status); }));
            form.addView(button(WorkspaceText.tr("Рассуждение: ") + (status.effort.isEmpty() ? WorkspaceText.tr("нет данных") : effortName(status.effort)) + " ›", false, () -> { if (shown[0] != null) shown[0].dismiss(); chooseCodexEffort(profile, status); }));
            form.addView(text("", 12, MUTED));
            for (String window : new String[]{"primary", "secondary"}) {
                String title = window.equals("primary") ? WorkspaceText.tr("Пятичасовой лимит") : WorkspaceText.tr("Недельный лимит");
                form.addView(text(CodexStatus.limit(status.limits, window, title), 14, TEXT));
                org.json.JSONObject limit = status.limits.optJSONObject(window);
                if (limit != null && limit.has("usedPercent")) {
                    android.widget.ProgressBar bar = new android.widget.ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
                    bar.setMax(100); bar.setProgress(Math.max(0, Math.min(100, (int)Math.round(100-limit.optDouble("usedPercent", 100)))));
                    bar.setProgressTintList(ColorStateList.valueOf(ACCENT)); form.addView(bar, new LinearLayout.LayoutParams(-1, dp(18)));
                }
                TextView time = text(codexResetTime(status, window), 12, MUTED); time.setPadding(0, dp(4), 0, dp(14)); form.addView(time);
            }
            form.addView(text(WorkspaceText.tr("Общий лимит аккаунта. Доступ: ") + (profile.codexAccess.equals("full") ? WorkspaceText.tr("полный") : profile.codexAccess.equals("read") ? WorkspaceText.tr("только чтение") : WorkspaceText.tr("в пределах проекта")) +
                WorkspaceText.tr("\nСтатус обновляется раз в 30 секунд. Выбор модели и рассуждения применяется через штатное меню Codex только к этой сессии. Если Codex отвечает, приложение дождётся завершения. Поле ввода сохраняется."), 12, MUTED));
        }
        AlertDialog dialog = WorkspaceDialogs.builder(activity).setTitle(WorkspaceText.tr("Статус Codex")).setView(formScroll(form))
            .setPositiveButton(WorkspaceText.tr("Обновить"), null).setNegativeButton(WorkspaceText.tr("Закрыть"), null).create();
        shown[0] = dialog;
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (codexLoading) return;
            loadingCodexDialog(profile, dialog);
        })); dialog.show();
        if (status == null && !codexLoading) loadingCodexDialog(profile, dialog);
    }
    private void loadingCodexDialog(WorkspaceStore.Session profile, AlertDialog dialog) {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        loadCodexStatus(profile, () -> { if (dialog.isShowing()) { dialog.dismiss(); showCodexStatus(profile); } });
    }
    private void chooseCodexModel(WorkspaceStore.Session profile, CodexStatus status) {
        if (status.models.length()==0) {activity.showToast(WorkspaceText.tr("Каталог моделей недоступен. После этого обновления запустите новый терминал Codex с прежним ID чата."), true);return;}
        String[] labels=new String[status.models.length()];int[] selected={0};
        for(int i=0;i<labels.length;i++) {org.json.JSONObject model=status.models.optJSONObject(i);labels[i]=model==null?"—":model.optString("displayName",model.optString("model"));if(model!=null && model.optString("model").equals(status.model))selected[0]=i;}
        WorkspaceDialogs.builder(activity).setTitle(WorkspaceText.tr("Модель Codex"))
            .setSingleChoiceItems(labels,selected[0],(d,w)-> {d.dismiss();chooseCodexEffortFor(profile,status,status.models.optJSONObject(w));})
            .setNegativeButton(WorkspaceText.tr("Отмена"),null).show();
    }
    private void chooseCodexEffort(WorkspaceStore.Session profile, CodexStatus status) {
        for(int i=0;i<status.models.length();i++) {org.json.JSONObject model=status.models.optJSONObject(i);if(model!=null && model.optString("model").equals(status.model)) {chooseCodexEffortFor(profile,status,model);return;}}
        chooseCodexModel(profile,status);
    }
    private String effortName(String effort) {
        switch(effort) {case "none":return WorkspaceText.tr("Без рассуждения");case "minimal":return WorkspaceText.tr("Минимальный");case "low":return WorkspaceText.tr("Низкий");case "medium":return WorkspaceText.tr("Средний");case "high":return WorkspaceText.tr("Высокий");case "xhigh":return WorkspaceText.tr("Очень высокий");case "max":return WorkspaceText.tr("Максимальный");case "ultra":return WorkspaceText.tr("Ультра");default:return effort;}
    }
    private void chooseCodexEffortFor(WorkspaceStore.Session profile,CodexStatus status,org.json.JSONObject model) {
        if(model==null)return;
        org.json.JSONArray options=model.optJSONArray("supportedReasoningEfforts");
        List<String> values=new ArrayList<>(),labels=new ArrayList<>();
        if(options!=null)for(int i=0;i<options.length();i++) {org.json.JSONObject option=options.optJSONObject(i);String effort=option==null?"":option.optString("reasoningEffort");if(!CodexModelSelection.effortLabel(effort).isEmpty()) {values.add(effort);labels.add(effortName(effort));}}
        if(values.isEmpty()) {String effort=model.optString("defaultReasoningEffort");if(!CodexModelSelection.effortLabel(effort).isEmpty()) {values.add(effort);labels.add(effortName(effort));}}
        if(values.isEmpty()) {activity.showToast(WorkspaceText.tr("Codex не сообщил доступные уровни для этой модели"),true);return;}
        String current=model.optString("model").equals(status.model)?status.effort:model.optString("defaultReasoningEffort");int[] selected={Math.max(0,values.indexOf(current))};
        WorkspaceDialogs.builder(activity).setTitle(model.optString("displayName",model.optString("model"))+WorkspaceText.tr(" · поведение"))
            .setSingleChoiceItems(labels.toArray(new String[0]),selected[0],(d,w)-> {d.dismiss();confirmCodexModel(profile,model,values.get(w),values.size()==1);})
            .setNegativeButton(WorkspaceText.tr("Отмена"),null).show();
    }
    private void confirmCodexModel(WorkspaceStore.Session profile,org.json.JSONObject model,String effort,boolean singleEffort) {
        WorkspaceDialogs.builder(activity).setTitle(WorkspaceText.tr("Применить параметры Codex?"))
            .setMessage(WorkspaceText.tr("Модель: ")+model.optString("displayName",model.optString("model"))+WorkspaceText.tr("\nПоведение: ")+effortName(effort)+WorkspaceText.tr("\n\nТолько для текущей сессии."))
            .setPositiveButton(WorkspaceText.tr("Применить"),(d,w)->applyCodexModel(profile,model,effort,singleEffort))
            .setNegativeButton(WorkspaceText.tr("Отмена"),null).show();
    }
    private String codexScreen(TerminalSession terminal) {
        com.termux.terminal.TerminalEmulator emulator=terminal.getEmulator();
        return emulator==null?"":emulator.getScreen().getSelectedText(0,0,emulator.mColumns,emulator.mRows-1);
    }
    private void applyCodexModel(WorkspaceStore.Session profile,org.json.JSONObject model,String effort,boolean singleEffort) {
        if(codexApplyDialog!=null && codexApplyDialog.isShowing())return;
        TerminalSession terminal=liveFor(profile);
        if(terminal==null || !terminal.isRunning()) {activity.showToast(WorkspaceText.tr("Сессия уже завершена"),false);return;}
        String id=model.optString("model");
        String wanted="Model changed to "+id+" "+effort;
        CodexStatus cached=codexStatuses.get(profile.id);
        if(cached!=null && cached.model.equals(id) && (cached.effort.equals(effort) || CodexModelSelection.acknowledgement(codexScreen(terminal)).equals(wanted))) {activity.showToast(WorkspaceText.tr("Эти параметры уже выбраны в Codex"),false);return;}
        ((InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(terminalInput.getWindowToken(),0);
        int generation=++codexApplyGeneration;
        AlertDialog dialog=WorkspaceDialogs.builder(activity).setTitle(WorkspaceText.tr("Параметры Codex")).setMessage(WorkspaceText.tr("Проверяем состояние сессии…"))
            .setNegativeButton(WorkspaceText.tr("Отмена"),(d,w)->codexApplyGeneration++).create();codexApplyDialog=dialog;
        dialog.setOnCancelListener(d->codexApplyGeneration++);dialog.show();
        long until=System.currentTimeMillis()+300000;
        Runnable waiting=new Runnable() {public void run() {
            if(closed || generation!=codexApplyGeneration || !dialog.isShowing())return;
            if(!terminal.isRunning() || System.currentTimeMillis()>until) {dialog.dismiss();activity.showToast(WorkspaceText.tr("Не удалось дождаться свободного Codex"),true);return;}
            if(codexLoading) {codexHandler.postDelayed(this,500);return;}
            loadCodexStatus(profile,()-> {
                if(closed || generation!=codexApplyGeneration || !dialog.isShowing())return;
                CodexStatus status=codexStatuses.get(profile.id);
                if(status==null || !status.error.isEmpty()) {dialog.dismiss();activity.showToast(status==null?WorkspaceText.tr("Статус Codex недоступен"):status.error,true);return;}
                if(!status.state.equals("idle")) {dialog.setMessage(WorkspaceText.tr("Ожидаем завершения ответа Codex…"));codexHandler.postDelayed(this,3000);return;}
                dialog.setMessage(WorkspaceText.tr("Применяем через штатное меню Codex…"));
                runCodexModelSelection(terminal,profile,model,effort,singleEffort,generation,dialog);
            });
        }};waiting.run();
    }
    private void runCodexModelSelection(TerminalSession terminal,WorkspaceStore.Session profile,org.json.JSONObject model,String effort,boolean singleEffort,int generation,AlertDialog dialog) {
        CodexModelSelection selection=new CodexModelSelection(model.optString("model"),model.optString("displayName",model.optString("model")),effort);selection.singleEffort(singleEffort);
        CodexModelSelection.Terminal adapter=new CodexModelSelection.Terminal() {
            public String screen() {return codexScreen(terminal);}
            public boolean menuOpen() {return terminal.getEmulator()!=null && !terminal.getEmulator().isCursorEnabled();}
            public void write(String keys) {
                if(keys.equals("/model\r") && terminal.getEmulator()!=null) {terminal.getEmulator().paste("/model");terminal.write("\r");}
                else terminal.write(keys);
            }
            public boolean emptyPrompt() {
                com.termux.terminal.TerminalEmulator emulator=terminal.getEmulator();if(emulator==null || !emulator.isCursorEnabled() || emulator.getCursorCol()>2)return false;
                String line=emulator.getScreen().getSelectedText(0,emulator.getCursorRow(),emulator.mColumns,emulator.getCursorRow()).trim();
                if(!line.startsWith("›"))return false;String content=line.substring(1).trim();return content.isEmpty() || content.startsWith("Ask Codex");
            }
        };
        Runnable tick=new Runnable() {public void run() {
            if(closed || generation!=codexApplyGeneration || !dialog.isShowing())return;
            if(!terminal.isRunning()) {dialog.dismiss();activity.showToast(WorkspaceText.tr("Сессия завершена"),false);return;}
            selection.step(adapter,System.currentTimeMillis());
            if(!selection.finished) {codexHandler.postDelayed(this,200);return;}
            dialog.dismiss();
            if(selection.confirmed) {CodexStatus status=codexStatuses.get(profile.id);if(status!=null) {status.model=model.optString("model");status.effort=effort;codexConfirmedChoices.put(profile.codexSocketId,new String[]{status.thread,status.model,effort,"Model changed to "+status.model+" "+effort});renderCodexStatus();}activity.showToast(WorkspaceText.tr("Codex подтвердил: ")+model.optString("displayName",model.optString("model"))+" · "+effortName(effort),true);}
            else activity.showToast(selection.error,true);
        }};tick.run();
    }
    private void finishWorkspaceSession(TerminalSession terminal) {
        WorkspaceStore.Session profile = store.findByHandle(terminal.mHandle);
        if(profile!=null&&profile.geminiAgent){String runId=profile.geminiSocketId;codexReader.execute(()->{try{GeminiCli.stop(activity,runId);}catch(Exception ignored){}activity.runOnUiThread(()->{if(terminal.isRunning())terminal.finishIfRunning();});});return;}
        if(profile!=null&&profile.claudeAgent){String runId=profile.claudeSocketId;codexReader.execute(()->{try{ClaudeCode.stop(activity,runId);}catch(Exception ignored){}activity.runOnUiThread(()->{if(terminal.isRunning())terminal.finishIfRunning();});});return;}
        if(profile!=null&&profile.aiAgent.equals("opencode")){codexReader.execute(()->{try{AiAgents.call(activity,profile.aiConnection,profile,AiAgentsUi.op("stop"));}catch(Exception ignored){}activity.runOnUiThread(()->{if(terminal.isRunning())terminal.finishIfRunning();});});return;}
        if (profile == null || !profile.environment.equals("codex")) { terminal.finishIfRunning(); return; }
        codexReader.execute(() -> {
            CodexRpc.stopOwnedServer(activity.getFilesDir(), profile);
            activity.runOnUiThread(() -> { if (terminal.isRunning()) terminal.finishIfRunning(); });
        });
    }
    private void chooseSshKey(EditText identity) {
        File[] files = new File(TermuxConstants.TERMUX_HOME_DIR_PATH, ".ssh").listFiles();
        List<File> keys = new ArrayList<>();
        if (files != null) for (File file : files) {
            String n = file.getName();
            if (file.isFile() && file.canRead() && !n.endsWith(".pub") && !n.equals("config") && !n.startsWith("known_hosts") && !n.startsWith("authorized_keys")) keys.add(file);
        }
        Collections.sort(keys, (a, b) -> a.getName().compareTo(b.getName()));
        AlertDialog.Builder picker = WorkspaceDialogs.builder(activity).setTitle(WorkspaceText.tr("Файл SSH-ключа"));
        if (keys.isEmpty()) picker.setMessage(WorkspaceText.tr("Ключей в ~/.ssh нет. Укажите путь к своему файлу в поле."));
        else { String[] names = new String[keys.size()]; for (int i = 0; i < names.length; i++) names[i] = keys.get(i).getName();
            picker.setItems(names, (d, which) -> identity.setText(keys.get(which).getAbsolutePath())); }
        picker.setNegativeButton(WorkspaceText.tr("Закрыть"), null).show();
    }
    private void requestSshPassword(WorkspaceStore.Session session, boolean add) {
        LinearLayout form = column(); EditText password = labeled(form, WorkspaceText.tr("Пароль SSH"), "", false);
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD); password.setSaveEnabled(false);
        AlertDialog dialog = WorkspaceDialogs.builder(activity).setTitle(session.sshUser + "@" + session.sshHost)
            .setView(formScroll(form)).setPositiveButton(WorkspaceText.tr("Подключиться"), null).setNegativeButton(WorkspaceText.tr("Отмена"), null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (password.length() == 0) { password.setError(WorkspaceText.tr("Введите пароль")); return; }
            char[] secret = new char[password.length()]; TextUtils.getChars(password.getText(), 0, secret.length, secret, 0);
            char[] old = pendingSshPasswords.put(session.id, secret); if (old != null) java.util.Arrays.fill(old, '\0');
            password.setText(""); dialog.dismiss(); launchNew(session, add);
        })); dialog.setOnDismissListener(d -> password.setText("")); dialog.show();
    }
    private void showPackages(WorkspaceStore.Session profile) {
        String environment=ProjectFiles.environment(profile);
        if(!environment.equals("termux")&&!environment.equals("ubuntu")){activity.showToast(WorkspaceText.tr("Пакеты пока доступны только для Termux и Ubuntu."),true);return;}
        String heldHandle=profile.handle;
        WorkspacePackages browser=new WorkspacePackages(activity,profile,(operation,selectedSession)->{
            if(closed)return;
            if(!selectedSession){launchNew(operation,true);return;}
            TerminalSession target=liveFor(profile);
            if(target==null||!target.isRunning()||!PackageCatalog.matchesTarget(profile,heldHandle,target.mHandle,operation.environment)){
                activity.showToast(WorkspaceText.tr("Выбранная сессия закрыта или изменилась. Откройте меню пакетов заново либо выберите новую сессию."),true);return;
            }
            activity.getTermuxTerminalSessionClient().setCurrentSession(target);show(Screen.TERMINAL,true);
            if(target.getEmulator()!=null){target.getEmulator().paste(operation.startup);target.write("\r");}else target.write(operation.startup+"\r");
        });browser.setOnClosed(()->packageBrowsers.remove(browser));packageBrowsers.add(browser);browser.show();
    }
    private void sessionMenu(WorkspaceStore.Session s) { sessionMenu(s, false); }
    private void sessionMenu(WorkspaceStore.Session s, boolean terminalTab) {
        if(s.localAi&&!com.termux.BuildConfig.LOCAL_AI_ENABLED){localAiUnavailable();return;}
        if(s.localAi){
            List<String> actions=new ArrayList<>();if(!s.saved)actions.add(localAi.tr("Добавить в пресеты запуска","Add to launch presets"));if(terminalTab&&localAi.active(s.id)){actions.add(localAi.tr("Новый чат","New chat"));actions.add(localAi.tr("Выбрать чат","Choose chat"));}if(!terminalTab)actions.add(localAi.tr("Закрыть сессию","Close session"));actions.add(localAi.tr("Настройки","Settings"));if(terminalTab)actions.add(localAi.tr("Диагностика сессии","Session diagnostics"));if(terminalTab)actions.add(localAi.tr("Закрыть сессию","Close session"));if(s.saved)actions.add(localAi.tr("Удалить пресет","Delete preset"));
            WorkspaceDialogs.builder(activity).setTitle(s.name).setItems(actions.toArray(new String[0]),(d,w)->{String action=actions.get(w);
                if(action.equals(localAi.tr("Добавить в пресеты запуска","Add to launch presets"))){s.saved=true;store.save();}
                else if(action.equals(localAi.tr("Новый чат","New chat")))localAi.panel(s,store::save).newChat();
                else if(action.equals(localAi.tr("Выбрать чат","Choose chat")))localAi.panel(s,store::save).chatMenu();
                else if(action.equals(localAi.tr("Диагностика сессии","Session diagnostics")))localAi.panel(s,store::save).diagnostics();
                else if(action.equals(localAi.tr("Настройки","Settings"))){if(localAi.active(s.id))localAi.panel(s,store::save).settingsDialog();else editSession(s,false);}
                else if(action.equals(localAi.tr("Закрыть сессию","Close session")))closeSession(s);
                else confirm(localAi.tr("Удалить пресет? История чата останется.","Delete preset? Chat history is kept."),()->{localAi.closePanel(s.id);if(selectedLocal==s)selectedLocal=null;store.sessions.remove(s);store.save();show(Screen.TERMINAL,false);});
            }).show();return;
        }
        if(WorkspaceStore.isBase(s)) {
            WorkspaceDialogs.builder(activity).setTitle(WorkspaceText.tr("Terminal · базовая сессия"))
                .setPositiveButton(WorkspaceText.tr("Перезапустить"),(d,w)->confirm(WorkspaceText.tr("Перезапустить базовую оболочку?"),()->{TerminalSession current=liveFor(s);if(current!=null&&current.isRunning())current.finishIfRunning();else launch(s);}))
                .setItems(new String[]{WorkspaceText.tr("Диагностика сессии")},(d,w)->showDiagnostics(s))
                .setNegativeButton(WorkspaceText.tr("Закрыть меню"),null).show();return;
        }
        List<String> actions = new ArrayList<>();
        if (terminalTab && !s.saved) actions.add(WorkspaceText.tr("Добавить в пресеты запуска"));
        if (terminalTab) Collections.addAll(actions, WorkspaceText.tr("Перезапустить"), WorkspaceText.tr("Настройки"), WorkspaceText.tr("Закрыть сессию"));
        else Collections.addAll(actions, WorkspaceText.tr("Закрыть сессию"), WorkspaceText.tr("Настройки"), WorkspaceText.tr("Удалить сессию"));
        if(terminalTab)actions.add(WorkspaceText.tr("Диагностика сессии"));
        WorkspaceDialogs.builder(activity).setTitle(s.name).setItems(actions.toArray(new String[0]), (d, which) -> {
            String action = actions.get(which);
            if(action.equals(WorkspaceText.tr("Диагностика сессии"))){showDiagnostics(s);return;}
            if (action.equals(WorkspaceText.tr("Добавить в пресеты запуска"))) {
                if (!store.isWritable()) { activity.showToast(WorkspaceText.tr("Библиотека недоступна для записи"), true); return; }
                saveSessionProfile(s);return;
            }
            if (action.equals(WorkspaceText.tr("Настройки"))) editSession(s);
            else if (action.equals(WorkspaceText.tr("Перезапустить"))) confirm(WorkspaceText.tr("Остановить текущий процесс и повторно выполнить стартовую команду?"), () -> {
                TerminalSession live = liveFor(s);
                if (live != null && live.isRunning()) {
                    if (live.getPid() <= 0) { activity.showToast(WorkspaceText.tr("Откройте терминал и дождитесь запуска"), false); return; }
                    pendingRestarts.put(live.mHandle, s); store.save(); finishWorkspaceSession(live);
                } else {
                    store.save(); launch(s);
                }
            });
            else if (action.equals(WorkspaceText.tr("Закрыть сессию"))) closeSession(s);
            else confirm(WorkspaceText.tr("Удалить настройки и историю сессии «") + s.name + WorkspaceText.tr("»? Активный терминал сначала нужно остановить."), () -> {
                TerminalSession live = liveFor(s);
                if (live != null && live.isRunning()) { activity.showToast(WorkspaceText.tr("Сначала остановите сессию"), true); return; }
                if (live != null) activity.getTermuxTerminalSessionClient().removeFinishedSession(live);
                store.sessions.remove(s); store.save();
                eraseSessionFiles(s);
                renderContent();
            });
        }).show();
    }
    private void chooseSavedSession() {
        List<WorkspaceStore.Session> saved = new ArrayList<>();
        for (WorkspaceStore.Session session : sortedSessions()) if (session.saved) saved.add(session);
        AlertDialog.Builder picker = WorkspaceDialogs.builder(activity).setTitle(WorkspaceText.tr("Пресеты запуска"));
        if (saved.isEmpty()) picker.setMessage(WorkspaceText.tr("Пресетов запуска нет. Удерживайте вкладку терминала и выберите «Добавить в пресеты запуска»."));
        else {
            String[] names = new String[saved.size()];
            for (int i = 0; i < names.length; i++) names[i] = saved.get(i).name;
            picker.setItems(names, (d, which) -> launch(saved.get(which)));
        }
        picker.setNegativeButton(WorkspaceText.tr("Закрыть"), null).show();
    }
    private void saveSessionProfile(WorkspaceStore.Session profile) {
        if(profile.environment.equals("codex")&&profile.codexChat.equals("new")&&liveFor(profile)!=null&&liveFor(profile).isRunning()) {
            if(codexLoading){activity.showToast(WorkspaceText.tr("Дождитесь обновления статуса и повторите сохранение"),false);return;}
            loadCodexStatus(profile,()->{if(!store.sessions.contains(profile))return;if(profile.codexChat.equals("id"))saveSessionProfile(profile);else activity.showToast(WorkspaceText.tr("ID чата ещё не получен. Дождитесь запуска Codex и повторите сохранение; нужен помощник новой версии."),true);});return;
        }
        commitSavedProfile(profile);
    }
    private void commitSavedProfile(WorkspaceStore.Session profile){
        profile.saved=true;profile.updated=System.currentTimeMillis();store.save();activity.showToast(WorkspaceText.tr("Пресет запуска добавлен"),false);renderContent();
    }
    private void closeSession(WorkspaceStore.Session profile) {
        if(!store.isWritable()){activity.showToast(WorkspaceText.tr("Библиотека недоступна для записи"),true);return;}
        if(profile.localAi){
            if(profile.saved){confirm(localAi.tr("Остановить модель и закрыть локальную сессию? История сохранится.","Stop the model and close local session? History is kept."),()->closeLocal(profile,true,false));return;}
            LinearLayout body=column();CheckBox keep=new CheckBox(activity);keep.setText(localAi.tr("Сохранить историю","Keep history"));keep.setTextColor(TEXT);keep.setChecked(true);keep.setTag("close-keep-history");body.addView(keep);body.addView(text(localAi.tr("История чата хранится отдельно от пресета. Без галочки текущая локальная история будет удалена. Добавление в пресеты всегда сохраняет историю.","Chat history is separate from a preset. Uncheck to delete the current local history. Adding a preset always keeps history."),13,MUTED));
            WorkspaceDialogs.builder(activity).setTitle(localAi.tr("Закрыть сессию?","Close session?")).setView(body).setNegativeButton(localAi.tr("Закрыть","Close"),(d,w)->closeLocal(profile,keep.isChecked(),false)).setPositiveButton(localAi.tr("Добавить в пресеты и закрыть","Add preset and close"),(d,w)->closeLocal(profile,true,true)).show();return;
        }
        if(WorkspaceStore.isBase(profile))return;
        TerminalSession target = liveFor(profile);
        if (target == null) { activity.showToast(WorkspaceText.tr("Сессия уже закрыта"), false); return; }
        if (!store.isWritable()) { activity.showToast(WorkspaceText.tr("Библиотека недоступна для записи"), true); return; }
        AlertDialog.Builder dialog = WorkspaceDialogs.builder(activity).setTitle(WorkspaceText.tr("Закрыть сессию «") + profile.name + "»?");
        if (profile.saved) {
            dialog.setMessage(WorkspaceText.tr("Процесс будет остановлен, вкладка терминала закрыта. Сохранённые настройки и история останутся."))
                .setPositiveButton(WorkspaceText.tr("Закрыть сессию"), (d, which) -> finishClose(profile, target, true));
        } else {
            LinearLayout body=column();CheckBox keep=new CheckBox(activity);keep.setText(localAi.tr("Сохранить историю","Keep history"));keep.setTextColor(TEXT);keep.setChecked(true);keep.setTag("close-keep-history");body.addView(keep);body.addView(text(localAi.tr("Галочка сохраняет историю оболочки на стороне приложения. Собственные чаты Codex и других CLI-агентов не удаляются; для удаления используйте «Истории чатов». Пресет сохраняет настройки запуска и историю.","The checkbox keeps app-side shell history. Native Codex/CLI conversations are not deleted; use Chat histories for deletion. A preset keeps launch settings and history."),13,MUTED));
            dialog.setView(body).setPositiveButton(localAi.tr("Добавить в пресеты и закрыть","Add preset and close"), (d, which) -> {profile.keepHistory=true;finishClose(profile, target, true);})
                .setNegativeButton(localAi.tr("Закрыть","Close"), (d, which) -> {profile.keepHistory=keep.isChecked();finishClose(profile, target, false);});
        }
        dialog.show();
    }
    private void closeLocal(WorkspaceStore.Session profile,boolean keep,boolean preset){
        localAi.closeWithHistory(profile,keep,()->{if(selectedLocal==profile)selectedLocal=null;if(preset){profile.saved=true;profile.keepHistory=true;profile.updated=System.currentTimeMillis();}if(!keep){String chat=profile.localChat;for(WorkspaceStore.Session other:store.sessions)if(other.localAi&&other.localChat.equals(chat))other.localChat="";}if(!profile.saved)store.sessions.remove(profile);store.save();show(Screen.TERMINAL,false);});
    }
    private void finishClose(WorkspaceStore.Session profile, TerminalSession target, boolean save) {finishClose(profile,target,save,false);}
    private void finishClose(WorkspaceStore.Session profile, TerminalSession target, boolean save,boolean aiChecked) {
        if(WorkspaceStore.isBase(profile))return;
        if (liveFor(profile) != target) return;
        if(save&&!profile.saved&&profile.environment.equals("codex")&&profile.codexChat.equals("new")&&target.isRunning()) {
            if(codexLoading){activity.showToast(WorkspaceText.tr("Дождитесь обновления статуса и повторите закрытие"),false);return;}
            loadCodexStatus(profile,()->{if(profile.codexChat.equals("id"))finishClose(profile,target,true);else activity.showToast(WorkspaceText.tr("ID чата ещё не получен. Дождитесь запуска либо выберите закрытие без сохранения."),true);});return;
        }
        if (target.isRunning() && target.getPid() <= 0) {
            activity.showToast(WorkspaceText.tr("Дождитесь запуска сессии"), false); return;
        }
        if (save) { profile.saved = true; profile.updated = System.currentTimeMillis(); }
        store.save(); pendingRestarts.remove(target.mHandle);
        if (target.isRunning()) { pendingCloses.add(target.mHandle); finishWorkspaceSession(target); }
        else {
            retireProfile(profile);
            activity.getTermuxTerminalSessionClient().removeFinishedSession(target); refresh();
        }
    }
    /** Only after the process exits: it must not recreate the history while we remove it. */
    private void retireProfile(WorkspaceStore.Session profile) {
        if(WorkspaceStore.isBase(profile))return;
        profile.handle = "";
        if (!profile.saved) { store.sessions.remove(profile); if(!profile.keepHistory)eraseSessionFiles(profile); }
        store.save();
    }
    private void eraseSessionFiles(WorkspaceStore.Session profile) {
        char[] secret = pendingSshPasswords.remove(profile.id); if (secret != null) java.util.Arrays.fill(secret, '\0');
        File dir = SessionShell.directory(activity.getFilesDir(), profile.id);
        reader.execute(() -> {
            File[] files = dir.listFiles(); boolean removed = true;
            if (files != null) for (File f : files) if (!f.delete()) removed = false;
            if (dir.exists() && !dir.delete()) removed = false;
            if (!removed) activity.runOnUiThread(() -> activity.showToast(WorkspaceText.tr("Не удалось полностью удалить историю сессии"), true));
        });
    }
    private void confirm(String message, Runnable action) {
        WorkspaceDialogs.builder(activity).setMessage(message).setPositiveButton(WorkspaceText.tr("Продолжить"), (d, w) -> action.run()).setNegativeButton(WorkspaceText.tr("Отмена"), null).show();
    }
    private void sendInput() {
        if(selectedLocal!=null){if(localAi.closing(selectedLocal.id))return;if(localAi.panel(selectedLocal,store::save).send(terminalInput.getText().toString()))terminalInput.setText("");return;}
        TerminalSession current = activity.getCurrentSession();
        if (current == null || !current.isRunning() || current.getPid() <= 0) { activity.showToast(WorkspaceText.tr("Нет работающего терминала"), false); return; }
        String command = terminalInput.getText().toString();
        if (command.trim().isEmpty()) return;
        if (current.getEmulator() != null) {
            current.getEmulator().paste(command); current.write("\r");
        } else current.write(command + "\r");
        store.recordCommand(command);
        terminalInput.setText("");
        terminalDrafts.remove(current.mHandle);
        if (interactiveInput) activity.getTerminalView().requestFocus();
        else terminalInput.requestFocus();
    }
    private void openInputEntry(Object item, Runnable back) {
        boolean isNote = item instanceof WorkspaceStore.Note;
        WorkspaceStore.Note note = isNote ? (WorkspaceStore.Note) item : null;
        WorkspaceStore.Command command = isNote ? null : (WorkspaceStore.Command) item;
        String name = isNote ? note.name : command.name;
        LinearLayout body = previewBody(isNote ? note.text : command.command, !isNote);
        previewMetadata(body, isNote ? note.category : command.category, isNote ? note.favorite : command.favorite);
        AlertDialog preview = WorkspaceDialogs.builder(activity).setTitle(name)
            .setView(formScroll(body)).setPositiveButton(WorkspaceText.tr("Назад"), (dialog, which) -> back.run()).create();
        preview.setOnCancelListener(dialog -> back.run());
        previewAction(body, WorkspaceText.tr("Вставить в сессию"), R.drawable.workspace_commands, () -> {
            preview.dismiss();
            WorkspaceStore.Command value = new WorkspaceStore.Command();
            value.command = isNote ? note.text : command.command;
            insertIntoCurrent(value);
        });
        previewAction(body, WorkspaceText.tr("Редактировать"), R.drawable.workspace_edit, () -> {
            preview.dismiss(); if (isNote) editNote(note); else editCommand(command);
        });
        previewAction(body, WorkspaceText.tr("Удалить"), R.drawable.workspace_delete, () -> {
            if (!store.isWritable()) { activity.showToast(WorkspaceText.tr("Библиотека недоступна для записи"), true); return; }
            preview.dismiss();
            confirm((isNote ? WorkspaceText.tr("Удалить заметку «") : WorkspaceText.tr("Удалить команду «")) + name + "»?", () -> {
                if (isNote) store.notes.remove(note); else store.commands.remove(command);
                store.save(); renderContent();
            });
        });
        preview.show();
    }
    private void chooseInputCommand() {
        hideDialogKeyboard();
        List<WorkspaceStore.Command> commands = new ArrayList<>(store.commands);
        Collections.sort(commands, (a, b) -> Boolean.compare(b.favorite, a.favorite));
        List<WorkspaceStore.Note> notes = new ArrayList<>(store.notes);
        Collections.sort(notes, (a, b) -> Boolean.compare(b.favorite, a.favorite));
        boolean[] noteTab = {false};
        String[] queries = {"", ""};
        List<Object> visible = new ArrayList<>(commands);
        android.widget.BaseAdapter adapter = new android.widget.BaseAdapter() {
            public int getCount() { return visible.size(); }
            public Object getItem(int position) { return visible.get(position); }
            public long getItemId(int position) { return position; }
            public View getView(int position, View recycled, ViewGroup parent) {
                FrameLayout outer = new FrameLayout(activity); outer.setPadding(dp(16), dp(4), dp(16), dp(4));
                Object item = getItem(position);
                WorkspaceStore.Command preview;
                if (item instanceof WorkspaceStore.Note) {
                    WorkspaceStore.Note note = (WorkspaceStore.Note) item;
                    preview = new WorkspaceStore.Command(); preview.name = note.name; preview.command = note.text;
                    preview.category = note.category; preview.favorite = note.favorite;
                } else preview = (WorkspaceStore.Command) item;
                LinearLayout row = WorkspaceDialogs.commandRow(activity, preview); row.setDuplicateParentStateEnabled(true);
                outer.addView(row, new FrameLayout.LayoutParams(-1, -2)); return outer;
            }
        };
        LinearLayout heading = column(); heading.setPadding(dp(20), dp(18), dp(20), dp(12));
        heading.setFocusableInTouchMode(true);
        LinearLayout tabs = row();
        TextView commandTab = button(WorkspaceText.tr("Сохранённые команды"), true, () -> {});
        TextView notesTab = button(WorkspaceText.tr("Заметки"), false, () -> {});
        commandTab.setMaxLines(2); notesTab.setMaxLines(2);
        commandTab.setPadding(dp(6), dp(8), dp(6), dp(8));
        LinearLayout.LayoutParams tabParams = new LinearLayout.LayoutParams(0, -2, 1.5f); tabParams.setMarginEnd(dp(8));
        tabs.addView(commandTab, tabParams); tabs.addView(notesTab, new LinearLayout.LayoutParams(0, -2, 1)); heading.addView(tabs);
        TextView hint = text(WorkspaceText.tr("Нажми на карточку, чтобы прочитать полностью"), 12, MUTED); hint.setPadding(0, dp(8), 0, dp(12)); heading.addView(hint);
        EditText search = field(WorkspaceText.tr("Найти команду"), "", false); search.setContentDescription(WorkspaceText.tr("Поиск в меню звёздочки"));
        search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH | android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        search.setMinHeight(dp(48)); heading.addView(search, new LinearLayout.LayoutParams(-1, -2));
        TextView empty = text(commands.isEmpty() ? WorkspaceText.tr("Сохраните команду в библиотеке или нажмите «Сохранить ввод».") : WorkspaceText.tr("Ничего не найдено"), 12, MUTED);
        empty.setPadding(0, dp(12), 0, 0); empty.setVisibility(commands.isEmpty() ? View.VISIBLE : View.GONE); heading.addView(empty);
        AlertDialog.Builder builder = WorkspaceDialogs.builder(activity).setCustomTitle(heading)
            .setAdapter(adapter, (dialog, index) -> {
                hideDialogKeyboard();
                AlertDialog list = (AlertDialog) dialog;
                int position = list.getListView().getFirstVisiblePosition();
                View first = list.getListView().getChildAt(0);
                int top = first == null ? 0 : first.getTop();
                openInputEntry(visible.get(index), () -> {
                    list.show();
                    list.getListView().setSelectionFromTop(position, top);
                });
            })
            .setNegativeButton(WorkspaceText.tr("Закрыть"), null);
        String input = terminalInput.getText().toString();
        if (!input.trim().isEmpty() && store.isWritable()) builder.setNeutralButton(WorkspaceText.tr("Сохранить ввод"), (dialog, which) -> editCommand(null, input));
        Runnable refresh = () -> {
            String key = search.getText().toString().toLowerCase(java.util.Locale.ROOT); visible.clear();
            if (noteTab[0]) {
                for (WorkspaceStore.Note note : notes) if ((note.name + " " + note.text + " " + note.category)
                    .toLowerCase(java.util.Locale.ROOT).contains(key)) visible.add(note);
            } else {
                for (WorkspaceStore.Command command : commands) if ((command.name + " " + command.command + " " + command.category)
                    .toLowerCase(java.util.Locale.ROOT).contains(key)) visible.add(command);
            }
            boolean libraryEmpty = noteTab[0] ? notes.isEmpty() : commands.isEmpty();
            empty.setText(libraryEmpty ? (noteTab[0] ? WorkspaceText.tr("Сохраните заметку в библиотеке.") : WorkspaceText.tr("Сохраните команду в библиотеке или нажмите «Сохранить ввод».")) : WorkspaceText.tr("Ничего не найдено"));
            empty.setVisibility(visible.isEmpty() ? View.VISIBLE : View.GONE); adapter.notifyDataSetChanged();
            hint.setText(WorkspaceText.tr("Нажми на карточку, чтобы прочитать полностью"));
            search.setHint(noteTab[0] ? WorkspaceText.tr("Найти заметку") : WorkspaceText.tr("Найти команду"));
            for (TextView tab : new TextView[]{commandTab, notesTab}) {
                boolean selected = (tab == notesTab) == noteTab[0]; tab.setSelected(selected);
                tintButton(tab, selected ? ACCENT : TEXT);
                tab.setBackground(new RippleDrawable(ColorStateList.valueOf(0x4479dfcd), shape(selected ? 0xff102522 : PANEL, selected ? ACCENT : BORDER, 12), null));
            }
        };
        commandTab.setOnClickListener(v -> { noteTab[0] = false; search.setText(queries[0]); refresh.run(); });
        notesTab.setOnClickListener(v -> { noteTab[0] = true; search.setText(queries[1]); refresh.run(); });
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence value, int start, int count, int after) {}
            public void onTextChanged(CharSequence value, int start, int before, int count) {
                queries[noteTab[0] ? 1 : 0] = value.toString(); refresh.run();
            }
            public void afterTextChanged(Editable value) {}
        });
        refresh.run();
        search.setOnEditorActionListener((v, action, event) -> {
            if (action != android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) return false;
            ((InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search.getWindowToken(), 0); return true;
        });
        AlertDialog picker = builder.create();
        Runnable openSearchKeyboard=()-> {
            search.requestFocus();
            search.post(()-> {if(picker.isShowing() && search.hasFocus())((InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(search,InputMethodManager.SHOW_IMPLICIT);});
        };
        search.setShowSoftInputOnFocus(true);
        search.setOnClickListener(v->openSearchKeyboard.run());
        search.setOnFocusChangeListener((v,focused)-> {if(focused)openSearchKeyboard.run();});
        picker.setOnShowListener(d -> {
            // AlertController does not inspect customTitle for text editors.
            picker.getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
            picker.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
            heading.requestFocus();
        });picker.show();
    }
    private void settingsRow(LinearLayout form,String label,Runnable action){TextView row=WorkspaceDialogs.action(activity,label,0);row.setOnClickListener(v->action.run());form.addView(row);}
    private void globalSettings(){
        LinearLayout form=column();int padding=dp(12);form.setPadding(padding,padding,padding,padding);
        AlertDialog[] menu={null};
        settingsRow(form,WorkspaceText.tr("Язык / Language"),()->{menu[0].dismiss();WorkspaceLanguage.showPicker(activity,this::refreshLanguage);});
        form.addView(WorkspaceUpdates.menuRow(activity,()->{menu[0].dismiss();WorkspaceUpdates.check(activity);}));
        settingsRow(form,WorkspaceBackupUi.label(activity),()->{menu[0].dismiss();WorkspaceBackupUi.show(activity,store,()->{refreshLanguage();},command->{WorkspaceStore.Session installation=new WorkspaceStore.Session();installation.name=WorkspaceLanguage.code(activity).equals("ru")?"Пакеты из бэкапа":"Backup packages";installation.environment="termux";installation.directory=TermuxConstants.TERMUX_HOME_DIR_PATH;installation.startup=command;launchNew(installation,true);});});
        settingsRow(form,localAi.tr("Истории чатов","Chat histories"),()->{menu[0].dismiss();new WorkspaceChatHistories(activity,store,localAi,reader,e->{if(e.kind.equals("local"))return localAi.chatActive(e.id);for(WorkspaceStore.Session p:store.sessions)if((p.environment.equals("codex")||p.aiAgent.equals("codex"))&&!p.claudeAgent&&!p.geminiAgent&&p.codexEnvironment.equals(e.environment)&&liveFor(p)!=null&&(p.codexThread.equals(e.id)||!p.codexChat.equals("id")))return true;return false;},()->{}).show();});
        settingsRow(form,WorkspaceErrorLibraryUi.label(activity),()->{menu[0].dismiss();WorkspaceErrorLibraryUi.show(activity,text->{WorkspaceStore.Command command=new WorkspaceStore.Command();command.command=text;insertCommand(command);});});
        settingsRow(form,WorkspaceText.tr("Настройки приложения"),()->{menu[0].dismiss();activity.startActivity(new android.content.Intent(activity,SettingsActivity.class));});
        menu[0]=WorkspaceDialogs.builder(activity).setTitle(WorkspaceText.tr("Настройки приложения")).setView(formScroll(form)).setNegativeButton(WorkspaceText.tr("Закрыть"),null).create();menu[0].show();
    }
    private void showDiagnostics(WorkspaceStore.Session profile){if(profile.localAi){localAi.panel(profile,store::save).diagnostics();return;}WorkspaceDiagnostics.register(profile,false);WorkspaceDiagnosticsUi.show(activity,profile,()->liveFor(profile));}
    private void confirmCancelStartup(WorkspaceStore.Session profile){
        if(!SessionShell.hasCancellableStartup(profile))return;
        StringBuilder commands=new StringBuilder();int step=0;for(String command:profile.startupCommands())commands.append(++step).append(". ").append(command).append("\n\n");
        LinearLayout body=previewBody(commands.toString().trim(),true);
        TextView explanation=text(WorkspaceText.tr("Отменить следующие шаги запуска? Ниже все настроенные команды. Уже выполняемая команда продолжит работу."),13,MUTED);body.addView(explanation,0);
        WorkspaceDialogs.builder(activity).setTitle(WorkspaceText.tr("Отменить команды запуска"))
            .setView(formScroll(body)).setPositiveButton(WorkspaceText.tr("Да"),(d,w)->cancelStartup(profile))
            .setNegativeButton(WorkspaceText.tr("Нет"),null).show();
    }
    private void cancelStartup(WorkspaceStore.Session profile){
        if(!SessionShell.hasCancellableStartup(profile))return;
        try {SessionShell.cancel(activity.getFilesDir(),profile.id);activity.showToast(WorkspaceText.tr("Следующие команды отменены. Текущий процесс продолжает работу."),true);}
        catch(IOException e){activity.showToast(WorkspaceText.tr("Не удалось отменить команды"),true);}
    }
    public void prepareKeyboardWindow() {
        activity.getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        activity.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED);
    }
    public void toggleKeyboard() {
        prepareKeyboardWindow();
        InputMethodManager keyboard = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        View target = interactiveInput ? activity.getTerminalView() : terminalInput;
        if (target.hasFocus() && com.termux.shared.view.KeyboardUtils.isSoftKeyboardVisible(activity)) {
            keyboard.hideSoftInputFromWindow(target.getWindowToken(), 0);
        } else {
            target.requestFocus(); target.post(()->keyboard.showSoftInput(target,0));
        }
    }
    private void setInteractiveInput(boolean enabled) {
        prepareKeyboardWindow();
        interactiveInput = enabled;
        activity.getSharedPreferences("workspace-input", Context.MODE_PRIVATE).edit()
            .putBoolean("interactive-input", enabled).apply();
        activity.getTerminalView().setSoftKeyboardInputEnabled(enabled);
        InputMethodManager keyboard = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        keyboard.restartInput(activity.getTerminalView());
        if (enabled) {
            activity.getTerminalView().requestFocus();
            keyboard.showSoftInput(activity.getTerminalView(), InputMethodManager.SHOW_IMPLICIT);
        } else {
            terminalInput.requestFocus();
            keyboard.hideSoftInputFromWindow(terminalInput.getWindowToken(), 0);
        }
    }
    private void insertCommand(WorkspaceStore.Command command) {
        List<TerminalSession> targets = new ArrayList<>();
        for (TermuxSession session : live()) if (session.getTerminalSession().isRunning()) targets.add(session.getTerminalSession());
        if (targets.size() > 1) {
            String[] labels = new String[targets.size()];
            for (int i = 0; i < targets.size(); i++) {
                TerminalSession target = targets.get(i);
                labels[i] = (target.mSessionName == null ? WorkspaceText.tr("Терминал ") + (i + 1) : target.mSessionName)
                    + " · " + compact(target.getCwd());
            }
            WorkspaceDialogs.builder(activity).setTitle(WorkspaceText.tr("В какой терминал вставить?")).setItems(labels, (dialog, index) -> {
                TerminalSession target = targets.get(index);
                boolean present = false;
                for (TermuxSession session : live()) if (session.getTerminalSession() == target) present = true;
                if (!present || !target.isRunning()) { activity.showToast(WorkspaceText.tr("Этот терминал уже завершён"), false); return; }
                activity.getTermuxTerminalSessionClient().setCurrentSession(target); insertIntoCurrent(command);
            }).setNegativeButton(WorkspaceText.tr("Отмена"), null).show();
            return;
        }
        if (targets.size() == 1) activity.getTermuxTerminalSessionClient().setCurrentSession(targets.get(0));
        insertIntoCurrent(command);
    }
    private void insertIntoCurrent(WorkspaceStore.Command command) {
        show(Screen.TERMINAL, true);
        terminalInput.setText(command.command);
        terminalInput.setSelection(terminalInput.length());
        terminalInput.requestFocus();
    }
    private String resolve(String directory) {
        if (directory.isEmpty() || directory.equals("~")) return TermuxConstants.TERMUX_HOME_DIR_PATH;
        return directory.startsWith("~/") ? TermuxConstants.TERMUX_HOME_DIR_PATH + directory.substring(1) : directory;
    }
    private void launch(WorkspaceStore.Session s) {
        if(s.localAi&&!com.termux.BuildConfig.LOCAL_AI_ENABLED){localAiUnavailable();return;}
        if(WorkspaceChatHistories.deleting(s)||localAi.closing(s.id)){activity.showToast(localAi.tr("Удаление истории ещё выполняется","History deletion is still running"),true);return;}
        if(s.localAi){boolean first=!localAi.active(s.id);selectedLocal=s;LocalAiUi.Panel panel=localAi.panel(s,store::save);show(Screen.TERMINAL,false);if(first)panel.prepare();return;}
        selectedLocal=null;
        if(s.restoreReview){editSession(s,false);return;}
        if(WorkspaceStore.isBase(s))baseRestartFailures=0;
        TerminalSession existing = liveFor(s);
        if (existing != null && existing.isRunning()) {
            activity.getTermuxTerminalSessionClient().setCurrentSession(existing); show(Screen.TERMINAL, true); return;
        }
        if (existing != null) { store.save(); }
        launchNew(s, false);
    }
    private void localAiUnavailable(){WorkspaceDialogs.builder(activity).setTitle(localAi.tr("Локальные модели","Local models")).setMessage(localAi.tr("Недоступны в публичной сборке. Сохранённые чаты, модели и пресеты остаются на месте.","Unavailable in the public build. Saved chats, models and presets are retained.")).setPositiveButton("OK",null).show();}
    private void launchNew(WorkspaceStore.Session s, boolean add) {
        if(s.localAi&&!com.termux.BuildConfig.LOCAL_AI_ENABLED){localAiUnavailable();return;}
        if(s.localAi){if(add&&!store.sessions.contains(s))store.sessions.add(s);store.save();launch(s);return;}
        selectedLocal=null;
        if (s.environment.equals("ssh") && s.sshAuth.equals("password") && s.sshEncryptedPassword.isEmpty() && !pendingSshPasswords.containsKey(s.id)) {
            requestSshPassword(s, add); return;
        }
        if (activity.getTermuxService() == null) { activity.showToast(WorkspaceText.tr("Терминальный сервис ещё запускается"), false); return; }
        if (!store.isWritable()) { activity.showToast(WorkspaceText.tr("Библиотека недоступна для записи"), true); return; }
        activity.prepareWorkspaceBootstrap(() -> {
            if (closed || activity.isFinishing()) return;
            TerminalSession previous = liveFor(s);
            TerminalSession live = create(s, add, previous != null && !previous.isRunning() ? previous : null);
            if (live != null) { activity.getTermuxTerminalSessionClient().setCurrentSession(live); show(Screen.TERMINAL, true); }
        });
    }
    /** Used by the original new-session action after bootstrap setup. */
    public TermuxSession createDefault(String name, String directory) {
        WorkspaceStore.Session profile=null;for(WorkspaceStore.Session stored:store.sessions)if(WorkspaceStore.isBase(stored)&&(liveFor(stored)==null||!liveFor(stored).isRunning())){profile=stored;break;}
        boolean add=profile==null;if(add)profile=new WorkspaceStore.Session();
        profile.name=name==null||name.trim().isEmpty()?"Bash":name;
        if(!hasBaseProfile()||WorkspaceStore.isBase(profile)){profile.id=WorkspaceStore.BASE_TERMINAL_ID;profile.name="Terminal";profile.environment="termux";}
        profile.directory=WorkspaceStore.isBase(profile)||directory==null?TermuxConstants.TERMUX_HOME_DIR_PATH:directory;
        TerminalSession previous=liveFor(profile);TerminalSession session=create(profile,add,previous);
        if (session == null) return null;
        return activity.getTermuxService().getTermuxSession(activity.getTermuxService().getIndexOfSession(session));
    }
    private boolean hasBaseProfile(){for(WorkspaceStore.Session s:store.sessions)if(WorkspaceStore.isBase(s))return true;return false;}
    public boolean isBaseTerminal(TerminalSession terminal){return terminal!=null&&WorkspaceStore.isBase(store.findByHandle(terminal.mHandle));}
    public void ensureBaseTerminal() {
        TermuxService service=activity.getTermuxService();
        if(closed||creating||baseCreating||!store.isWritable()||service==null||service.wantsToStop()||!new File(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH+"/bash").isFile())return;
        WorkspaceStore.Session base=null;for(WorkspaceStore.Session s:store.sessions)if(WorkspaceStore.isBase(s)){base=s;break;}
        if(base!=null&&liveFor(base)!=null)return;
        if(service.getTermuxSessionsSize()>=8)return;
        boolean add=base==null;if(add){base=new WorkspaceStore.Session();base.id=WorkspaceStore.BASE_TERMINAL_ID;}
        base.name="Terminal";base.environment="termux";base.directory=TermuxConstants.TERMUX_HOME_DIR_PATH;base.saved=false;
        baseCreating=true;try{TerminalSession created=create(base,add);if(created!=null&&activity.getCurrentSession()==null)activity.getTermuxTerminalSessionClient().setCurrentSession(created);}finally{baseCreating=false;}
    }
    private TerminalSession create(WorkspaceStore.Session profile, boolean add) {
        return create(profile, add, null);
    }
    private TerminalSession create(WorkspaceStore.Session profile, boolean add, TerminalSession replaced) {
        if(profile.restoreReview){editSession(profile,false);return null;}
        TermuxService service = activity.getTermuxService();
        if (service == null) return null;
        if(WorkspaceStore.isBase(profile))baseStartedAt=System.currentTimeMillis();
        boolean replacing = replaced != null && !replaced.isRunning() && service.getIndexOfSession(replaced) >= 0;
        if (service.getTermuxSessionsSize() >= 8 && !replacing) { activity.showToast(WorkspaceText.tr("Максимум 8 терминалов. Закройте завершённую сессию."), true); return null; }
        String directory = (profile.environment.equals("ubuntu") || profile.environment.equals("ssh") || (profile.environment.equals("codex") && profile.codexEnvironment.equals("ubuntu"))) ? TermuxConstants.TERMUX_HOME_DIR_PATH : resolve(profile.directory);
        if (!new File(directory).isDirectory() || !new File(directory).canExecute()) {
            activity.showToast(WorkspaceText.tr("Каталог недоступен: ") + directory, true); return null;
        }
        SshPasswordBroker broker = null;
        try {
            String askpass = null, socket = null;
            if (profile.environment.equals("ssh") && profile.sshAuth.equals("password")) {
                char[] secret = pendingSshPasswords.remove(profile.id);
                if (secret == null && !profile.sshEncryptedPassword.isEmpty()) secret = SshPasswordVault.decrypt(profile);
                if (secret == null) { activity.showToast(WorkspaceText.tr("Введите пароль в настройках сессии"), true); return null; }
                try {
                    askpass = SessionShell.prepareAskpass(activity.getFilesDir(), profile, activity.getApplicationInfo().sourceDir).getAbsolutePath();
                    broker = new SshPasswordBroker(activity, secret); socket = broker.address;
                } finally { java.util.Arrays.fill(secret, '\0'); }
            }
            if(profile.geminiAgent){GeminiCli.prepare(activity);profile.geminiSocketId=java.util.UUID.randomUUID().toString();if(profile.geminiChat.equals("new"))profile.geminiThread=java.util.UUID.randomUUID().toString();}
            if(profile.claudeAgent){ClaudeCode.prepare(activity);profile.claudeSocketId=java.util.UUID.randomUUID().toString();if(profile.claudeChat.equals("new"))profile.claudeThread=java.util.UUID.randomUUID().toString();}
            if(profile.aiAgent.equals("opencode")){AiAgents.prepare(activity);AiAgents.prepareLaunch(profile);}
            if (profile.environment.equals("codex")) { profile.codexSocketId = java.util.UUID.randomUUID().toString(); CodexSessionShell.prepareBridge(activity); }
            if (profile.environment.equals("codex") && (profile.codexChat.equals("choose") || profile.codexChat.equals("last"))) { activity.showToast(WorkspaceText.tr("Выберите конкретный чат или введите его ID в настройках сессии"), true); return null; }
            File rc = SessionShell.prepare(activity.getFilesDir(), profile, askpass, socket);
            codexStatuses.remove(profile.id);
            // service notifications are synchronous; register the new profile before they fire.
            if (add) store.sessions.add(profile);
            creating = true;
            TermuxSession created;
            try { created = service.createTermuxSession(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/bash",
                new String[]{"--rcfile", rc.getAbsolutePath(), "-i"}, null, directory, false, profile.name);
            } finally { creating = false; }
            if (created == null) { if (broker != null) broker.close(); if (add) store.sessions.remove(profile); activity.showToast(WorkspaceText.tr("Не удалось создать терминал"), true); return null; }
            TerminalSession session = created.getTerminalSession();
            // The synchronous callback may have adopted it; discard that temporary record.
            WorkspaceStore.Session adopted = store.findByHandle(session.mHandle);
            if (adopted != null && adopted != profile) store.sessions.remove(adopted);
            profile.handle = session.mHandle; profile.updated = System.currentTimeMillis();
            WorkspaceDiagnostics.register(profile,true);
            // Create first: removing the last old session would otherwise stop the service.
            if (replacing) service.removeTermuxSession(replaced);
            store.save(); refresh();
            if (profile.environment.equals("codex")) codexHandler.postDelayed(() -> { if (!closed) loadCodexStatus(profile, null); }, 1500);
            return session;
        } catch (java.security.GeneralSecurityException e) {
            if (broker != null) broker.close(); activity.showToast(WorkspaceText.tr("Сохранённый пароль недоступен. Введите его заново в настройках сессии."), true); return null;
        } catch (IOException | RuntimeException e) {
            WorkspaceDiagnostics.failure(profile,e);
            if (broker != null) broker.close(); activity.showToast(WorkspaceText.tr("Не удалось подготовить сессию"), true); return null;
        }
    }
    public void refresh() {
        if (closed || creating) return;
        boolean changed = false;
        for (TermuxSession live : live()) {
            TerminalSession terminal = live.getTerminalSession();
            if (store.findByHandle(terminal.mHandle) == null) {
                WorkspaceStore.Session profile = new WorkspaceStore.Session(); profile.handle = terminal.mHandle;
                profile.name = terminal.mSessionName == null ? "Bash" : terminal.mSessionName;
                profile.directory = live.getExecutionCommand().workingDirectory == null ? TermuxConstants.TERMUX_HOME_DIR_PATH : live.getExecutionCommand().workingDirectory;
                profile.updated = System.currentTimeMillis(); store.sessions.add(profile); changed = true;
            } else {
                WorkspaceStore.Session profile = store.findByHandle(terminal.mHandle);
                if (terminal.mSessionName != null && !profile.name.equals(terminal.mSessionName)) {
                    profile.name = terminal.mSessionName; changed = true;
                }
            }
        }
        if (activity.getTermuxService() != null && store.isWritable()) {
            for (WorkspaceStore.Session profile : new ArrayList<>(store.sessions)) {
                if (!WorkspaceStore.isBase(profile) && !profile.saved && !profile.handle.isEmpty() && liveFor(profile) == null) {
                    store.sessions.remove(profile); if(!profile.keepHistory)eraseSessionFiles(profile); changed = true;
                }
            }
        }
        for(WorkspaceStore.Session profile:store.sessions)if(!profile.handle.isEmpty())WorkspaceDiagnostics.register(profile,false);
        if (changed) store.save();
        renderTabs(); renderHeader(); if(screen!=Screen.SERVERS)renderContent();
    }
    public boolean onFinished(TerminalSession session) {
        WorkspaceStore.Session profile = store.findByHandle(session.mHandle);
        WorkspaceDiagnostics.finished(session);
        if (profile != null) { store.save(); }
        if(WorkspaceStore.isBase(profile)&&activity.getTermuxService()!=null&&!activity.getTermuxService().wantsToStop()) {
            if(System.currentTimeMillis()-baseStartedAt<5000)baseRestartFailures++;else baseRestartFailures=0;
            if(baseRestartFailures>3){activity.showToast(WorkspaceText.tr("Terminal сразу завершается. Проверьте shell-настройки и перезапустите вручную."),true);return true;}
            boolean selected=activity.getCurrentSession()==session;
            long delay=Math.max(300,3000-(System.currentTimeMillis()-lastBaseRestart));lastBaseRestart=System.currentTimeMillis();
            codexHandler.postDelayed(()->{if(closed||activity.getTermuxService()==null||activity.getTermuxService().wantsToStop()||liveFor(profile)!=session)return;TerminalSession next=create(profile,false,session);if(next!=null){if(selected)activity.getTermuxTerminalSessionClient().setCurrentSession(next);refresh();}},delay);
            return true;
        }
        if (pendingCloses.remove(session.mHandle)) {
            if (profile != null) retireProfile(profile);
            activity.getTermuxTerminalSessionClient().removeFinishedSession(session); refresh(); return true;
        }
        WorkspaceStore.Session restart = pendingRestarts.remove(session.mHandle);
        if (restart == null || activity.getTermuxService() == null || activity.getTermuxService().wantsToStop()) return false;
        TerminalSession next = create(restart, false, session);
        if (next != null) {
            activity.getTermuxTerminalSessionClient().setCurrentSession(next); show(Screen.TERMINAL, true);
        } else {
            store.save(); refresh(); show(Screen.SESSIONS, true);
        }
        return true;
    }
    public boolean onBack() {
        if (screen == Screen.SERVERS && servers.back()) return true;
        if (screen == Screen.HOME) return false;
        show(Screen.HOME, true); return true;
    }
    public void close() {
        servers.close();localAi.close();
        terminalDrafts.clear();
        for(WorkspacePackages browser:new ArrayList<>(packageBrowsers))browser.close();packageBrowsers.clear();
        for(WorkspaceFileBrowser browser:new ArrayList<>(fileBrowsers))browser.close();fileBrowsers.clear();
        for (char[] secret : pendingSshPasswords.values()) java.util.Arrays.fill(secret, '\0'); pendingSshPasswords.clear();
        closed = true; codexApplyGeneration++; if(codexApplyDialog!=null)codexApplyDialog.dismiss(); codexHandler.removeCallbacksAndMessages(null); codexReader.shutdown(); reader.shutdown(); store.close();
    }
}
