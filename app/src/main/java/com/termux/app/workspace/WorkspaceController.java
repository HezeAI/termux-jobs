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
    private enum Screen { HOME, SESSIONS, COMMANDS, TERMINAL }
    private final TermuxActivity activity;
    private final WorkspaceStore store;
    private final LinearLayout header, navigation, tabs, inputRow, codexStatus;
    private final EditText terminalInput;
    private String terminalInputHandle;
    private final java.util.Map<String, InputDraft> terminalDrafts = new java.util.HashMap<>();
    private static final class InputDraft {
        final String text; final int start, end;
        InputDraft(EditText input) {
            text=input.getText().toString(); start=input.getSelectionStart(); end=input.getSelectionEnd();
        }
    }
    private final FrameLayout content;
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
    private boolean noteFavorites, favorites, savedOnly, closed, creating;
    private int toolbarVisibility = View.GONE;
    private boolean interactiveInput;
    private float swipeStart;
    private final Set<String> pendingCloses = new java.util.HashSet<>();
    private final java.util.Map<String, WorkspaceStore.Session> pendingRestarts = new java.util.HashMap<>();

    private final java.util.Map<String, char[]> pendingSshPasswords = new java.util.HashMap<>();
    public WorkspaceController(TermuxActivity activity) {
        this.activity = activity;
        store = new WorkspaceStore(activity, message -> activity.showToast(message, true));
        interactiveInput = activity.getSharedPreferences("workspace-input", Context.MODE_PRIVATE)
            .getBoolean("interactive-input", false);
        activity.getTerminalView().setSoftKeyboardInputEnabled(interactiveInput);
        header = activity.findViewById(R.id.workspace_header);
        navigation = activity.findViewById(R.id.workspace_navigation);
        tabs = activity.findViewById(R.id.workspace_tabs);
        codexStatus = activity.findViewById(R.id.workspace_codex_status);
        content = activity.findViewById(R.id.workspace_content);
        inputRow = activity.findViewById(R.id.workspace_input);
        inputRow.setPadding(dp(12), dp(6), dp(12), dp(6));
        terminalInput = field("Введите команду…", "", true);
        terminalInput.setTypeface(Typeface.MONOSPACE); terminalInput.setTextSize(12);
        terminalInput.setMinLines(1); terminalInput.setMinHeight(dp(48));
        terminalInput.setHorizontallyScrolling(false);
        terminalInput.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_NONE
            | android.view.inputmethod.EditorInfo.IME_FLAG_NO_ENTER_ACTION
            | android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        inputRow.addView(terminalInput, new LinearLayout.LayoutParams(0, -2, 1));
        TextView commands = button("★", false, this::chooseInputCommand);
        commands.setTextColor(ACCENT); commands.setTextSize(22);
        commands.setContentDescription("Вставить сохранённую команду");
        LinearLayout.LayoutParams commandsParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        commandsParams.setMarginEnd(dp(8)); inputRow.addView(commands, 0, commandsParams);
        TextView send = button("▷", true, this::sendInput); send.setContentDescription("Отправить в активный терминал");
        LinearLayout.LayoutParams sendParams = new LinearLayout.LayoutParams(dp(48), dp(48)); sendParams.setMarginStart(dp(8)); inputRow.addView(send, sendParams);
        activity.getWindow().setStatusBarColor(Color.BLACK);
        activity.getWindow().setNavigationBarColor(Color.BLACK);
        tabs.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) swipeStart = event.getX();
            if (event.getAction() == MotionEvent.ACTION_UP && Math.abs(event.getX() - swipeStart) > dp(64)) {
                activity.getTermuxTerminalSessionClient().switchToSession(event.getX() < swipeStart);
                v.performClick(); return true;
            }
            return true;
        });
        toolbarVisibility = activity.getTerminalToolbarViewPager().getVisibility();
        show(Screen.HOME, false);
        codexHandler.postDelayed(codexPoll, 1000);
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
        TextView b = text(label, 13, active ? Color.BLACK : TEXT);
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
                b.setText(""); b.setCompoundDrawables(drawable, null, null, null); b.setContentDescription(label);
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
            case "▷": return R.drawable.workspace_play;
            case "⋮": return R.drawable.workspace_more;
            case "⚙": return R.drawable.workspace_settings;
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
        boolean terminal = screen == Screen.TERMINAL;
        activity.getDrawer().setVisibility(terminal ? View.VISIBLE : View.INVISIBLE);
        activity.getTerminalToolbarViewPager().setVisibility(terminal ? toolbarVisibility : View.GONE);
        activity.findViewById(R.id.workspace_tab_scroll).setVisibility(terminal ? View.VISIBLE : View.GONE);
        content.setVisibility(terminal ? View.GONE : View.VISIBLE);
        inputRow.setVisibility(terminal ? View.VISIBLE : View.GONE);
        renderHeader(); renderNavigation(); renderTabs(); renderContent(); renderCodexStatus();
        if (animate && !terminal) { content.setAlpha(0); content.animate().alpha(1).setDuration(180).start(); }
        if (terminal) activity.getTerminalView().requestFocus();
    }
    private void renderHeader() {
        // Keep the navigation container stable while rebuilding its surrounding header.
        if (navigation.getParent() != null) ((ViewGroup) navigation.getParent()).removeView(navigation);
        header.removeAllViews(); header.setPadding(dp(8), dp(4), dp(8), dp(4));
        LinearLayout top = row();
        TextView name = text("Termux Job’s", 17, TEXT); name.setTypeface(null, Typeface.BOLD);
        name.setSingleLine(); name.setEllipsize(TextUtils.TruncateAt.END);
        top.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
        TextView action;
        if (screen == Screen.HOME || screen == Screen.SESSIONS) {
            action = button("⚙", false, () -> activity.startActivity(new android.content.Intent(activity, SettingsActivity.class)));
            action.setContentDescription("Настройки приложения");
        } else if (screen == Screen.TERMINAL) {
            action = button("⚙", false, this::terminalMenu); action.setContentDescription("Меню терминала");
        } else {
            action = button("⚙", false, () -> activity.startActivity(new android.content.Intent(activity, SettingsActivity.class)));
            action.setContentDescription("Настройки приложения");
        }
        action.setPadding(dp(10), dp(8), dp(10), dp(8)); action.setMinWidth(dp(48));
        for (android.graphics.drawable.Drawable drawable : action.getCompoundDrawables())
            if (drawable != null) drawable.setBounds(0, 0, dp(28), dp(28));
        action.setBackground(new RippleDrawable(ColorStateList.valueOf(0x4479dfcd), shape(Color.BLACK, Color.BLACK, 10), null));
        top.addView(action, new LinearLayout.LayoutParams(dp(48), dp(48)));
        header.addView(top, new LinearLayout.LayoutParams(-1, dp(48)));
        LinearLayout.LayoutParams navigationParams = new LinearLayout.LayoutParams(-1, -2);
        navigationParams.topMargin = dp(4);
        header.addView(navigation, navigationParams);
        if (screen == Screen.COMMANDS || screen == Screen.SESSIONS) {
            LinearLayout tools = row(); tools.setPadding(dp(8), dp(26), dp(8), dp(6));
            if (screen == Screen.SESSIONS) {
                TextView title = text("Сессии", 17, TEXT); title.setTypeface(null, Typeface.BOLD);
                tools.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
            }
            if (screen == Screen.COMMANDS) {
                EditText search = field("Поиск команд и/или заметок", query, false);
                search.setContentDescription("Поиск команд и заметок"); search.setTextSize(13);
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
                        query = s.toString(); renderContent();
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
            add.setContentDescription(screen == Screen.COMMANDS ? "Новая команда" : "Новая сессия");
            if (screen == Screen.SESSIONS) tools.addView(add, new LinearLayout.LayoutParams(dp(48), dp(48)));
            header.addView(tools);
        }
    }
    private String currentDirectory() {
        TerminalSession s = activity.getCurrentSession();
        return s == null ? "Нет активной сессии" : compact(s.getCwd());
    }
    private String compact(String path) {
        if (path == null || path.isEmpty()) return "~";
        String home = TermuxConstants.TERMUX_HOME_DIR_PATH;
        return path.equals(home) || path.startsWith(home + "/") ? "~" + path.substring(home.length()) : path;
    }
    private void renderNavigation() {
        navigation.removeAllViews(); navigation.setPadding(dp(8), 0, dp(8), 0);
        String[] labels = {"⌂\nГлавная", ">_\nКоманды/Заметки", "▣\nТерминал"};
        Screen[] targets = {Screen.HOME, Screen.COMMANDS, Screen.TERMINAL};
        for (int i = 0; i < targets.length; i++) {
            Screen target = targets[i]; TextView nav = button(labels[i], false, () -> show(target, true));
            nav.setTextSize(11); nav.setPadding(0, dp(4), 0, dp(4)); nav.setMinWidth(0); nav.setMinHeight(dp(56)); nav.setMaxLines(2);
            int resource = target == Screen.HOME ? R.drawable.workspace_home
                : target == Screen.COMMANDS ? R.drawable.workspace_library : R.drawable.workspace_commands;
            android.graphics.drawable.Drawable icon = activity.getDrawable(resource).mutate();
            icon.setBounds(0, 0, dp(28), dp(28)); nav.setCompoundDrawables(null, icon, null, null);
            nav.setContentDescription(labels[i].substring(labels[i].indexOf('\n') + 1));
            tintButton(nav, target == screen ? ACCENT : MUTED);
            nav.setBackground(new RippleDrawable(ColorStateList.valueOf(0x4479dfcd),
                shape(target == screen ? 0xff102522 : PANEL, target == screen ? ACCENT : BORDER, 12), null));
            LinearLayout.LayoutParams tileParams = new LinearLayout.LayoutParams(0, -2, 1);
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
        String handle=current==null?null:current.mHandle;
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
            terminalDrafts.keySet().retainAll(handles);
        }
        if(terminalInput.hasFocus())((InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE)).restartInput(terminalInput);
    }
    private void renderTabs() {
        syncTerminalInput();
        renderCodexStatus();
        tabs.removeAllViews();
        View selected = null;
        for (TermuxSession ts : live()) {
            TerminalSession s = ts.getTerminalSession();
            boolean active = activity.getCurrentSession() == s;
            LinearLayout item = column();
            TextView label = text(s.mSessionName == null ? "Bash" : s.mSessionName, 17, active ? ACCENT : MUTED);
            label.setGravity(Gravity.CENTER); label.setSingleLine(); label.setMaxWidth(dp(180));
            label.setEllipsize(TextUtils.TruncateAt.END); label.setPadding(dp(16), dp(2), dp(16), 0);
            if (active) label.setTypeface(null, Typeface.BOLD);
            item.addView(label, new LinearLayout.LayoutParams(-1, 0, 1));
            View underline = new View(activity); underline.setBackgroundColor(active ? ACCENT : Color.TRANSPARENT);
            item.addView(underline, new LinearLayout.LayoutParams(-1, dp(2)));
            item.setBackground(new RippleDrawable(ColorStateList.valueOf(0x4479dfcd), shape(Color.BLACK, Color.BLACK, 0), null));
            item.setContentDescription(label.getText()); item.setFocusable(true);
            item.setOnClickListener(v -> { activity.getTermuxTerminalSessionClient().setCurrentSession(s); show(Screen.TERMINAL, false); });
            item.setOnLongClickListener(v -> { WorkspaceStore.Session p = store.findByHandle(s.mHandle); if (p != null) sessionMenu(p, true); return true; });
            tabs.addView(item, new LinearLayout.LayoutParams(-2, dp(48)));
            if (active) selected = item;
        }
        TextView add = button("+", false, () -> editSession(null)); add.setContentDescription("Новая сессия");
        add.setBackground(new RippleDrawable(ColorStateList.valueOf(0x4479dfcd), shape(Color.BLACK, Color.BLACK, 0), null));
        tabs.addView(add, new LinearLayout.LayoutParams(dp(48), dp(48)));
        if (selected != null) {
            View activeTab = selected;
            tabs.post(() -> { if (activeTab.getParent() == tabs) activeTab.requestRectangleOnScreen(new android.graphics.Rect(0, 0, activeTab.getWidth(), activeTab.getHeight()), true); });
        }
    }
    private void renderContent() {
        if (screen == Screen.TERMINAL) return;
        content.removeAllViews();
        LinearLayout body = column(); body.setPadding(dp(16), 0, dp(16), dp(12));
        if (screen == Screen.HOME || screen == Screen.COMMANDS) {
            AdaptiveWorkspaceLayout viewport = new AdaptiveWorkspaceLayout(activity);
            viewport.setContentDescription("Адаптивная область списков");
            viewport.addView(body); content.addView(viewport, new android.widget.FrameLayout.LayoutParams(-1, -1));
            if (screen == Screen.HOME) home(body); else commands(body);
        } else {
            ScrollView scroll = new ScrollView(activity); scroll.setFillViewport(true);
            scroll.addView(body); content.addView(scroll);
            if (screen == Screen.COMMANDS) commands(body); else sessions(body);
        }
    }

    private void home(LinearLayout body) {
        section(body, "Недавние команды", store.recentCommands.isEmpty() ? null : "Очистить", () -> {
            if (!store.isWritable()) { activity.showToast("Журнал недоступен для записи", true); return; }
            confirm("Очистить весь журнал недавних команд?", () -> { store.clearRecentCommands(); renderContent(); });
        });
        EditText search = field("Поиск по журналу", journalQuery, false);
        search.setContentDescription("Поиск по журналу");
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(-1, -2);
        search.setMinHeight(dp(48)); searchParams.setMargins(0, 0, 0, dp(12)); body.addView(search, searchParams);
        ScrollView journal = new WorkspacePanelScrollView(activity);
        journal.setBackground(shape(PANEL, BORDER, 16)); journal.setClipToOutline(true);
        journal.setContentDescription("Журнал последних 50 команд");
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
        section(body, "Сохранённые сессии", "+", () -> editSession(null, false));
        ScrollView saved = new WorkspacePanelScrollView(activity);
        saved.setBackground(shape(PANEL, BORDER, 16)); saved.setClipToOutline(true);
        saved.setContentDescription("Список сохранённых сессий");
        LinearLayout profiles = column(); profiles.setPadding(dp(12), dp(12), dp(12), dp(12));
        saved.addView(profiles); body.addView(saved, new LinearLayout.LayoutParams(-1, 0, 1));
        int savedCount = 0;
        for (WorkspaceStore.Session profile : sortedSessions()) if (profile.saved) { sessionCard(profiles, profile); savedCount++; }
        if (savedCount == 0) empty(profiles, "Сохранённых сессий нет", "Нажмите +, чтобы подготовить команду запуска.");
    }
    private void renderJournal(LinearLayout entries, TextView count) {
        entries.removeAllViews(); int shown = 0;
        if (store.recentCommands.isEmpty()) {
            entries.addView(text("Команд пока нет", 16, TEXT));
            TextView hint = text("Здесь появятся команды, отправленные из поля ввода терминала.", 13, MUTED);
            hint.setPadding(0, dp(8), 0, 0); entries.addView(hint);
        }
        for (WorkspaceStore.RecentCommand recent : store.recentCommands) {
            if (!recent.command.toLowerCase(java.util.Locale.ROOT).contains(journalQuery.toLowerCase(java.util.Locale.ROOT))) continue;
            shown++;
            LinearLayout card = card(entries); card.setBackground(shape(Color.BLACK, BORDER, 12));
            LinearLayout labels = column(); TextView entry = text(recent.command, 13, TEXT);
            entry.setTypeface(Typeface.MONOSPACE); labels.addView(entry);
            String time = recent.sentAt > 0 ? new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                .format(new java.util.Date(recent.sentAt)) : "Ранее";
            TextView timestamp = text(time, 11, MUTED); timestamp.setPadding(0, dp(6), 0, 0); labels.addView(timestamp);
            Runnable insert = () -> recentMenu(recent);
            entry.setOnClickListener(v -> insert.run());
            labels.setOnClickListener(v -> insert.run()); card.setOnClickListener(v -> insert.run());
            card.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        }
        if (shown == 0 && !store.recentCommands.isEmpty()) entries.addView(text("Ничего не найдено", 14, MUTED));
        count.setText(store.recentCommands.size() + " / " + WorkspaceStore.MAX_RECENT_COMMANDS
            + (journalQuery.isEmpty() ? "" : " · найдено: " + shown));
    }
    private void recentMenu(WorkspaceStore.RecentCommand recent) {
        hideDialogKeyboard();
        LinearLayout body = previewBody(recent.command, true);
        TextView time = text(recent.sentAt > 0 ? new java.text.SimpleDateFormat("dd.MM · HH:mm", java.util.Locale.getDefault())
            .format(new java.util.Date(recent.sentAt)) : "Ранее", 12, MUTED);
        time.setPadding(0, 0, 0, dp(10)); body.addView(time, 0);
        AlertDialog dialog = WorkspaceDialogs.builder(activity).setTitle("Недавняя команда")
            .setView(formScroll(body)).setPositiveButton("Закрыть", null).create();
        previewAction(body, "Вставить в терминал", R.drawable.workspace_commands, () -> {
            dialog.dismiss(); WorkspaceStore.Command command = new WorkspaceStore.Command(); command.command = recent.command; insertCommand(command);
        });
        previewAction(body, "Сохранить в команды", R.drawable.workspace_save, () -> { dialog.dismiss(); editCommand(null, recent.command); });
        previewAction(body, "Скопировать", R.drawable.workspace_copy, () -> {
            ((android.content.ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE))
                .setPrimaryClip(android.content.ClipData.newPlainText("Команда", recent.command));
            dialog.dismiss(); activity.showToast("Команда скопирована", false);
        });
        dialog.show();
    }
    private void hideDialogKeyboard() {
        ((InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE))
            .hideSoftInputFromWindow(content.getWindowToken(), 0);
    }
    private LinearLayout previewBody(String value, boolean code) {
        LinearLayout body = column(); ScrollView pane = new ScrollView(activity); pane.setFillViewport(true);
        pane.setContentDescription(code ? "Полный текст команды" : "Полный текст заметки");
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
            .setView(formScroll(body)).setPositiveButton("Закрыть", null).create();
        previewAction(body, "Вставить в терминал", R.drawable.workspace_commands, () -> { dialog.dismiss(); insertCommand(command); });
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
        if (!query.isEmpty()) body.addView(button("Поиск: " + query + "  ×", false, () -> { query = ""; renderContent(); }));
    }
    private void commands(LinearLayout body) {
        if (!commandCategory.isEmpty() && !commandCategories().contains(commandCategory)) commandCategory = "";
        if (!noteCategory.isEmpty() && !noteCategories().contains(noteCategory)) noteCategory = "";
        libraryFilters(body, false);
        LinearLayout commandEntries = libraryPanel(body, "Список сохранённых команд");
        int count = 0;
        for (WorkspaceStore.Command c : store.commands) {
            if ((!favorites || c.favorite) && (commandCategory.isEmpty() || commandCategory.equals(c.category))
                && matches(c.name + " " + c.command + " " + c.category)) { commandCard(commandEntries, c); count++; }
        }
        if (count == 0) empty(commandEntries, "Команд пока нет", "Нажмите +, чтобы сохранить команду, или измените фильтры.");
        libraryFilters(body, true);
        LinearLayout noteEntries = libraryPanel(body, "Список сохранённых заметок"); count = 0;
        for (WorkspaceStore.Note n : store.notes) {
            if ((!noteFavorites || n.favorite) && (noteCategory.isEmpty() || noteCategory.equals(n.category))
                && matches(n.name + " " + n.text + " " + n.category)) { noteCard(noteEntries, n); count++; }
        }
        if (count == 0) empty(noteEntries, "Заметок пока нет", "Нажмите +, чтобы сохранить заметку, или измените фильтры.");
    }
    private void libraryFilters(LinearLayout body, boolean notes) {
        LinearLayout line = row(); line.setPadding(0, dp(10), 0, dp(10));
        line.setContentDescription(notes ? "Фильтры заметок" : "Фильтры команд");
        LinearLayout items = row();
        String all = notes ? "Все заметки" : "Все команды";
        for (String option : new String[]{all, "Избранное"}) {
            TextView chip = button(option, (notes ? noteFavorites : favorites) == option.equals("Избранное"), () -> {
                if (notes) noteFavorites = option.equals("Избранное"); else favorites = option.equals("Избранное"); renderContent();
            });
            compactFilter(chip);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1); params.setMarginEnd(dp(4)); items.addView(chip, params);
        }
        String selected = notes ? noteCategory : commandCategory;
        TextView category = button((selected.isEmpty() ? "Категории" : selected) + " ▾", !selected.isEmpty(), () -> {
            if (notes) chooseNoteCategory(); else chooseCommandCategory();
        });
        category.setContentDescription(notes ? "Фильтр категорий заметок" : "Фильтр категорий команд");
        compactFilter(category);
        items.addView(category, new LinearLayout.LayoutParams(0, -2, 1));
        line.addView(items, new LinearLayout.LayoutParams(0, -2, 1));
        TextView add = button("+", true, () -> { if (notes) editNote(null); else editCommand(null); });
        add.setContentDescription(notes ? "Новая заметка" : "Новая команда");
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
        List<String> labels = new ArrayList<>(); labels.add("Все категории"); labels.addAll(categories);
        int selected = noteCategory.isEmpty() ? 0 : categories.indexOf(noteCategory) + 1;
        WorkspaceDialogs.builder(activity).setTitle("Категории заметок")
            .setSingleChoiceItems(labels.toArray(new String[0]), Math.max(0, selected), (dialog, which) -> {
                noteCategory = which == 0 ? "" : categories.get(which - 1); dialog.dismiss(); renderContent();
            }).setNegativeButton("Отмена", null).show();
    }
    private void noteCard(LinearLayout parent, WorkspaceStore.Note note) {
        LinearLayout card = card(parent); card.setBackground(shape(Color.BLACK, BORDER, 12));
        cardLabels(card, note.name, note.text, note.category + (note.favorite ? "  ★" : ""), false);
        ((TextView) card.getChildAt(0)).setText("≡");
        card.setContentDescription("Заметка " + note.name); card.setOnClickListener(v -> openNote(note));
        TextView more = button("⋮", false, () -> noteMenu(note));
        more.setContentDescription("Действия с заметкой " + note.name); card.addView(more);
    }
    private void openNote(WorkspaceStore.Note note) {
        hideDialogKeyboard(); LinearLayout body = previewBody(note.text, false); previewMetadata(body, note.category, note.favorite);
        WorkspaceDialogs.builder(activity).setTitle(note.name).setView(formScroll(body)).setPositiveButton("Закрыть", null).show();
    }
    private LinearLayout contextHeading(String name, String command, String category, boolean favorite) {
        LinearLayout heading = column(); heading.setPadding(dp(20), dp(18), dp(20), dp(12));
        TextView title = text(name, 20, TEXT); title.setTypeface(null, Typeface.BOLD); heading.addView(title);
        if (!command.isEmpty()) {
            TextView preview = text(command, 12, MUTED); preview.setTypeface(Typeface.MONOSPACE);
            preview.setMaxLines(2); preview.setEllipsize(TextUtils.TruncateAt.END);
            preview.setPadding(0, dp(8), 0, 0); heading.addView(preview);
        }
        TextView label = text(category + (favorite ? "   ★" : ""), 12, MUTED);
        label.setPadding(0, dp(8), 0, 0); heading.addView(label); return heading;
    }
    private void noteMenu(WorkspaceStore.Note note) {
        WorkspaceDialogs.builder(activity).setCustomTitle(contextHeading(note.name, "", note.category, note.favorite))
            .setItems(new String[]{note.favorite ? "Убрать из избранного" : "В избранное", "Редактировать", "Удалить"}, (d, which) -> {
                if (!store.isWritable()) { activity.showToast("Библиотека недоступна для записи", true); return; }
                if (which == 0) { note.favorite = !note.favorite; store.save(); renderContent(); }
                else if (which == 1) editNote(note);
                else confirm("Удалить заметку «" + note.name + "»?", () -> { store.notes.remove(note); store.save(); renderContent(); });
            }).setNegativeButton("Закрыть", null).show();
    }
    private void editNote(WorkspaceStore.Note existing) {
        if (!store.isWritable()) { activity.showToast("Библиотека недоступна для записи", true); return; }
        WorkspaceStore.Note draft = existing == null ? new WorkspaceStore.Note() : existing;
        LinearLayout form = column(); EditText name = labeled(form, "Название", draft.name, false);
        EditText text = labeled(form, "Текст заметки", draft.text, true);
        EditText category = labeled(form, "Категория", draft.category, false);
        CheckBox favorite = new CheckBox(activity); favorite.setText("Добавить в избранное"); favorite.setTextColor(TEXT);
        favorite.setChecked(draft.favorite); form.addView(favorite);
        AlertDialog dialog = WorkspaceDialogs.builder(activity).setTitle(existing == null ? "Новая заметка" : "Редактировать заметку")
            .setView(formScroll(form)).setPositiveButton("Сохранить", null).setNegativeButton("Отмена", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (name.getText().toString().trim().isEmpty()) { name.setError("Укажите название"); return; }
            if (text.getText().toString().trim().isEmpty()) { text.setError("Укажите текст"); return; }
            draft.name = name.getText().toString().trim(); draft.text = text.getText().toString();
            draft.category = category.getText().toString().trim(); if (draft.category.isEmpty()) draft.category = "Общее";
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
        List<String> labels = new ArrayList<>(); labels.add("Все категории"); labels.addAll(categories);
        int selected = commandCategory.isEmpty() ? 0 : categories.indexOf(commandCategory) + 1;
        WorkspaceDialogs.builder(activity).setTitle("Категории команд")
            .setSingleChoiceItems(labels.toArray(new String[0]), Math.max(0, selected), (dialog, which) -> {
                commandCategory = which == 0 ? "" : categories.get(which - 1);
                dialog.dismiss(); renderContent();
            }).setNegativeButton("Отмена", null).show();
    }
    private boolean matches(String value) { return value.toLowerCase(java.util.Locale.ROOT).contains(query.toLowerCase(java.util.Locale.ROOT)); }
    private List<WorkspaceStore.Session> sortedSessions() {
        List<WorkspaceStore.Session> result = new ArrayList<>(store.sessions);
        result.sort((a, b) -> Long.compare(b.updated, a.updated)); return result;
    }
    private void sessions(LinearLayout body) {
        chips(body, new String[]{"Активные", "Сохранённые"}, savedOnly ? "Сохранённые" : "Активные", value -> { savedOnly = value.equals("Сохранённые"); renderContent(); });
        searchSummary(body); int count = 0;
        for (WorkspaceStore.Session s : sortedSessions()) {
            if ((savedOnly ? s.saved : liveFor(s) != null) && matches(s.name + " " + s.directory + " " + android.text.TextUtils.join(" ", s.startupCommands()))) { sessionCard(body, s); count++; }
        }
        if (count == 0) empty(body, savedOnly ? "Сохранённых сессий нет" : "Нет активных сессий", "Создайте сессию или запустите сохранённую.");
        body.addView(button("+  Новая сессия", true, () -> editSession(null)));
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
        TextView more = button("⋮", false, () -> commandMenu(c)); more.setContentDescription("Действия с командой " + c.name); card.addView(more);
    }
    private void sessionCard(LinearLayout parent, WorkspaceStore.Session s) {
        TerminalSession running = liveFor(s); boolean active = running != null && running.isRunning();
        LinearLayout card = card(parent); cardLabels(card, s.name, s.startupCommands().isEmpty() ? compact(s.directory) : android.text.TextUtils.join(" → ", s.startupCommands()), active ? "● Активна" : running != null ? "Завершена" : "Сохранена", true);
        if (screen == Screen.HOME) card.setBackground(shape(Color.BLACK, BORDER, 12));
        card.setOnClickListener(v -> launch(s));
        TextView run = button(active ? "›" : "▷", false, () -> launch(s)); run.setContentDescription(active ? "Открыть " + s.name : "Запустить " + s.name); card.addView(run);
        TextView more = button("⋮", false, () -> sessionMenu(s)); more.setContentDescription("Действия с сессией " + s.name); card.addView(more);
    }
    private void search() {
        EditText input = field("Название, команда или каталог", query, false);
        WorkspaceDialogs.builder(activity).setTitle("Поиск").setView(input)
            .setPositiveButton("Найти", (d, which) -> { query = input.getText().toString().trim(); renderContent(); })
            .setNegativeButton("Отмена", null).show();
    }
    private EditText field(String hint, String value, boolean multiline) {
        EditText e = new EditText(activity); e.setHint(hint); e.setText(value); e.setTextColor(TEXT); e.setHintTextColor(MUTED); e.setTextSize(14);
        e.setPadding(dp(12), dp(10), dp(12), dp(10)); e.setBackground(shape(PANEL, BORDER, 10));
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | (multiline ? InputType.TYPE_TEXT_FLAG_MULTI_LINE : 0));
        e.setSingleLine(!multiline); if (multiline) { e.setMinLines(2); e.setMaxLines(6); e.setGravity(Gravity.TOP); }
        return e;
    }
    private EditText labeled(LinearLayout form, String label, String value, boolean multiline) {
        TextView title = text(label, 12, MUTED); title.setPadding(0, dp(12), 0, dp(6)); form.addView(title);
        EditText e = field(label, value, multiline); form.addView(e, new LinearLayout.LayoutParams(-1, -2)); return e;
    }
    private ScrollView formScroll(LinearLayout form) {
        form.setPadding(dp(20), dp(4), dp(20), dp(18)); ScrollView scroll = new ScrollView(activity); scroll.addView(form); return scroll;
    }
    private void editCommand(WorkspaceStore.Command existing) {
        editCommand(existing, "");
    }
    private void editCommand(WorkspaceStore.Command existing, String initialCommand) {
        if (!store.isWritable()) { activity.showToast("Библиотека недоступна для записи", true); return; }
        WorkspaceStore.Command draft = existing == null ? new WorkspaceStore.Command() : existing;
        if (existing == null) draft.command = initialCommand;
        LinearLayout form = column(); EditText name = labeled(form, "Название", draft.name, false);
        EditText command = labeled(form, "Команда", draft.command, true);
        EditText categoryInput = labeled(form, "Категория", draft.category, false);
        CheckBox favorite = new CheckBox(activity); favorite.setText("Добавить в избранное"); favorite.setTextColor(TEXT); favorite.setChecked(draft.favorite); form.addView(favorite);
        AlertDialog dialog = WorkspaceDialogs.builder(activity).setTitle(existing == null ? "Новая команда" : "Редактировать команду")
            .setView(formScroll(form)).setPositiveButton("Сохранить", null).setNegativeButton("Отмена", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (name.getText().toString().trim().isEmpty()) { name.setError("Укажите название"); return; }
            if (command.getText().toString().trim().isEmpty()) { command.setError("Укажите команду"); return; }
            draft.name = name.getText().toString().trim(); draft.command = command.getText().toString();
            draft.category = categoryInput.getText().toString().trim(); if (draft.category.isEmpty()) draft.category = "Общее";
            draft.favorite = favorite.isChecked();
            if (existing == null) store.commands.add(draft); store.save(); dialog.dismiss(); renderContent();
        })); dialog.show();
    }
    private boolean validDirectoryInput(EditText field) {
        String value = field.getText().toString().trim();
        if (value.indexOf('\0') >= 0 || value.contains("\n")) { field.setError("Некорректный каталог"); return false; }
        if (!value.isEmpty() && !value.startsWith("/") && !value.equals("~") && !value.startsWith("~/")) {
            field.setError("Используйте абсолютный путь или ~/каталог"); return false;
        }
        return true;
    }
    private void editSession(WorkspaceStore.Session existing) { editSession(existing, true); }
    private void editSession(WorkspaceStore.Session existing, boolean launchAfterSave) {
        if (!store.isWritable()) { activity.showToast("Библиотека недоступна для записи", true); return; }
        WorkspaceStore.Session draft = existing == null ? new WorkspaceStore.Session() : existing;
        LinearLayout form = column(); EditText name = labeled(form, "Название сессии", existing == null ? "" : draft.name, false);
        EditText directory = labeled(form, "Рабочий каталог", existing == null ? TermuxConstants.TERMUX_HOME_DIR_PATH : draft.directory, false);
        String[] chosenEnvironment = {existing == null ? "termux" : draft.environment};
        String[] environments = {"legacy", "termux", "ubuntu", "ssh", "codex"};
        String[] environmentNames = {"Как раньше", "Termux · ждать Bash", "Ubuntu · ждать Bash", "SSH · сервер", "Сессия Codex"};
        TextView environment = button("", false, () -> {});
        environment.setContentDescription("Среда запуска");
        LinearLayout sshFields = column();
        EditText sshHost = labeled(sshFields, "Адрес сервера", draft.sshHost, false);
        EditText sshUser = labeled(sshFields, "Пользователь", draft.sshUser, false);
        EditText sshPort = labeled(sshFields, "Порт", Integer.toString(draft.sshPort), false);
        sshPort.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        String[] authModes = {"auto", "key", "password"};
        String[] authNames = {"Автоматически", "Файл SSH-ключа", "По паролю"};
        String[] chosenAuth = {draft.sshAuth};
        TextView auth = button("", false, () -> {}); auth.setContentDescription("Авторизация SSH"); sshFields.addView(auth);
        LinearLayout keyFields = column(); sshFields.addView(keyFields);
        EditText identity = labeled(keyFields, "Файл SSH-ключа", draft.sshIdentity, false);
        identity.setContentDescription("Файл SSH-ключа");
        keyFields.addView(button("Выбрать ключ", false, () -> chooseSshKey(identity)));
        LinearLayout passwordFields = column(); sshFields.addView(passwordFields);
        EditText password = labeled(passwordFields, "Пароль", "", false); password.setContentDescription("Пароль SSH");
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        password.setSaveEnabled(false); password.setLongClickable(false);
        if (!draft.sshEncryptedPassword.isEmpty()) password.setHint("Сохранён; оставьте пустым, чтобы не менять");
        CheckBox showPassword = new CheckBox(activity); showPassword.setText("Показать пароль"); showPassword.setTextColor(TEXT);
        showPassword.setOnCheckedChangeListener((b, checked) -> {
            password.setTransformationMethod(checked ? null : android.text.method.PasswordTransformationMethod.getInstance());
            password.setSelection(password.length());
        }); passwordFields.addView(showPassword);
        CheckBox rememberPassword = new CheckBox(activity); rememberPassword.setText("Запомнить пароль"); rememberPassword.setTextColor(TEXT);
        rememberPassword.setChecked(!draft.sshEncryptedPassword.isEmpty()); passwordFields.addView(rememberPassword);
        passwordFields.addView(text("Сохранение через Android Keystore. После удаления приложения пароль потребуется ввести заново.", 12, MUTED));
        Runnable updateAuth = () -> {
            auth.setText("Авторизация: " + authNames[Math.max(0, java.util.Arrays.asList(authModes).indexOf(chosenAuth[0]))]);
            keyFields.setVisibility(chosenAuth[0].equals("key") ? View.VISIBLE : View.GONE);
            passwordFields.setVisibility(chosenAuth[0].equals("password") ? View.VISIBLE : View.GONE);
        }; updateAuth.run();
        auth.setOnClickListener(v -> WorkspaceDialogs.builder(activity).setTitle("Авторизация SSH")
            .setSingleChoiceItems(authNames, java.util.Arrays.asList(authModes).indexOf(chosenAuth[0]), (d, which) -> {
                chosenAuth[0] = authModes[which]; updateAuth.run(); d.dismiss();
            }).setNegativeButton("Отмена", null).show());
        android.widget.CheckBox sshKeepOpen = new android.widget.CheckBox(activity);
        sshKeepOpen.setText("Оставить подключение открытым"); sshKeepOpen.setTextColor(TEXT);
        sshKeepOpen.setChecked(draft.sshKeepOpen); sshFields.addView(sshKeepOpen);
        LinearLayout codexFields = column();
        String[] codexEnvironment = {draft.codexEnvironment}, codexAccess = {draft.codexAccess}, codexChat = {draft.codexChat};
        String[] codexThread = {draft.codexThread}, codexThreadName = {draft.codexThreadName};
        TextView codexEnvButton = button("", false, () -> {}); codexFields.addView(codexEnvButton);
        TextView codexAccessButton = button("", false, () -> {}); codexFields.addView(codexAccessButton);
        TextView codexChatButton = button("", false, () -> {}); codexFields.addView(codexChatButton);
        codexEnvButton.setContentDescription("Среда Codex"); codexAccessButton.setContentDescription("Доступ Codex"); codexChatButton.setContentDescription("Чат Codex");
        String[] accessModes = {"full", "project", "read"};
        String[] accessNames = {"Полный доступ · без подтверждений", "В пределах проекта · с подтверждениями", "Только чтение · с подтверждениями"};
        Runnable updateCodex = () -> {
            codexEnvButton.setText("Среда Codex: " + (codexEnvironment[0].equals("ubuntu") ? "Ubuntu" : "Termux"));
            codexAccessButton.setText("Доступ: " + accessNames[Math.max(0, java.util.Arrays.asList(accessModes).indexOf(codexAccess[0]))]);
            codexChatButton.setText("Чат: " + (codexChat[0].equals("new") ? "Новый" : codexChat[0].equals("last") ? "Выберите конкретный чат" : codexChat[0].equals("id") ? codexThreadName[0] : "Выбрать при запуске"));
        }; updateCodex.run();
        codexEnvButton.setOnClickListener(v -> WorkspaceDialogs.builder(activity).setTitle("Среда Codex")
            .setSingleChoiceItems(new String[]{"Termux", "Ubuntu"}, codexEnvironment[0].equals("ubuntu") ? 1 : 0, (d, which) -> {
                codexEnvironment[0] = which == 0 ? "termux" : "ubuntu"; codexChat[0] = "new"; codexThread[0] = ""; codexThreadName[0] = "";
                if (which == 1 && directory.getText().toString().equals(TermuxConstants.TERMUX_HOME_DIR_PATH)) directory.setText("/root");
                else if (which == 0 && directory.getText().toString().equals("/root")) directory.setText(TermuxConstants.TERMUX_HOME_DIR_PATH);
                updateCodex.run(); d.dismiss();
            }).setNegativeButton("Отмена", null).show());
        codexAccessButton.setOnClickListener(v -> WorkspaceDialogs.builder(activity).setTitle("Доступ Codex")
            .setSingleChoiceItems(accessNames, java.util.Arrays.asList(accessModes).indexOf(codexAccess[0]), (d, which) -> {
                codexAccess[0] = accessModes[which]; updateCodex.run(); d.dismiss();
            }).setNegativeButton("Отмена", null).show());
        codexChatButton.setOnClickListener(v -> WorkspaceDialogs.builder(activity).setTitle("Чат Codex")
            .setItems(new String[]{"Новый чат", "Выбрать чат", "Ввести ID чата"}, (d, which) -> {
                if (which == 0) { codexChat[0] = "new"; codexThread[0] = ""; updateCodex.run(); }
                else if (which == 1) chooseCodexChat(codexEnvironment[0], (id, title) -> {
                    codexChat[0] = id.isEmpty() ? "choose" : "id"; codexThread[0] = id; codexThreadName[0] = title; updateCodex.run();
                });
                else {
                    EditText id = field("ID чата Codex", codexThread[0], false);
                    AlertDialog picker = WorkspaceDialogs.builder(activity).setTitle("ID чата Codex").setView(id).setPositiveButton("Выбрать", null).setNegativeButton("Отмена", null).create();
                    picker.setOnShowListener(x -> picker.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(y -> {
                        String value = id.getText().toString().trim(); if (!CodexSessionShell.validThread(value)) { id.setError("Некорректный ID"); return; }
                        codexChat[0] = "id"; codexThread[0] = value; codexThreadName[0] = value; updateCodex.run(); picker.dismiss();
                    })); picker.show();
                }
            }).setNegativeButton("Отмена", null).show());
        codexFields.addView(text("Авторизация и история берутся из выбранной среды. Если вход не выполнен, Codex предложит авторизоваться. Для карточки управления нужна версия Codex с app-server и --remote. Полный доступ действует в пределах доступных среде файлов. На Android ограничения sandbox зависят от поддержки Codex; при ошибке запуск остановится, права не повышаются автоматически.", 12, MUTED));
        Runnable updateEnvironment = () -> {
            sshFields.setVisibility(chosenEnvironment[0].equals("ssh") ? View.VISIBLE : View.GONE);
            codexFields.setVisibility(chosenEnvironment[0].equals("codex") ? View.VISIBLE : View.GONE);
            int index = java.util.Arrays.asList(environments).indexOf(chosenEnvironment[0]);
            environment.setText("Среда запуска: " + environmentNames[Math.max(0, index)]);
        };
        updateEnvironment.run(); form.addView(environment); form.addView(sshFields); form.addView(codexFields);
        environment.setOnClickListener(v -> WorkspaceDialogs.builder(activity).setTitle("Среда запуска")
            .setSingleChoiceItems(environmentNames, java.util.Arrays.asList(environments).indexOf(chosenEnvironment[0]), (d, which) -> {
                chosenEnvironment[0] = environments[which];
                if ((which == 2 || (which == 4 && codexEnvironment[0].equals("ubuntu"))) && directory.getText().toString().equals(TermuxConstants.TERMUX_HOME_DIR_PATH)) directory.setText("/root");
                else if (which == 3 && (directory.getText().toString().equals(TermuxConstants.TERMUX_HOME_DIR_PATH) || directory.getText().toString().equals("/root"))) directory.setText("~");
                else if ((which < 2 || (which == 4 && codexEnvironment[0].equals("termux"))) && directory.getText().toString().equals("/root")) directory.setText(TermuxConstants.TERMUX_HOME_DIR_PATH);
                if (which == 4 && name.getText().toString().trim().isEmpty()) name.setText("Codex");
                updateEnvironment.run(); d.dismiss();
            }).setNegativeButton("Отмена", null).show());
        LinearLayout commandFields = column(); form.addView(commandFields);
        List<EditText> startupFields = new ArrayList<>();
        startupFields.add(labeled(commandFields, "Команда 1 (необязательно)", draft.startup, true));
        if (!draft.startup2.isEmpty() || !draft.startup3.isEmpty()) startupFields.add(labeled(commandFields, "Команда 2", draft.startup2, true));
        if (!draft.startup3.isEmpty()) startupFields.add(labeled(commandFields, "Команда 3", draft.startup3, true));
        TextView addCommand = button("+ Команда", false, () -> {});
        addCommand.setContentDescription("Добавить команду запуска");
        addCommand.setOnClickListener(v -> {
            if (startupFields.size() >= 3) return;
            startupFields.add(labeled(commandFields, "Команда " + (startupFields.size() + 1), "", true));
            if (startupFields.size() == 3) addCommand.setVisibility(View.GONE);
        });
        if (startupFields.size() == 3) addCommand.setVisibility(View.GONE);
        form.addView(addCommand);
        Runnable toggleCommands = () -> {
            commandFields.setVisibility(chosenEnvironment[0].equals("codex") ? View.GONE : View.VISIBLE);
            addCommand.setVisibility(chosenEnvironment[0].equals("codex") || startupFields.size() >= 3 ? View.GONE : View.VISIBLE);
        }; toggleCommands.run();
        environment.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence x, int a, int b, int c) {} public void onTextChanged(CharSequence x, int a, int b, int c) { toggleCommands.run(); }
            public void afterTextChanged(Editable x) {}
        });
        TextView hint = text("Termux / Ubuntu: команды запускаются после готовности Bash в выбранной среде. Вход в Ubuntu выполняется автоматически — команду proot-distro login вводить не нужно. До трёх шагов, при ошибке остановка. После запуска Codex следующие шаги ждут его выхода. SSH: каталог и все команды выполняются на сервере с Bash. Пароль и подтверждение ключа — в терминале; следующая команда ждёт завершения предыдущей.", 12, MUTED);
        hint.setPadding(0, dp(16), 0, 0); form.addView(hint);
        AlertDialog dialog = WorkspaceDialogs.builder(activity).setTitle(existing == null ? "Новая сессия" : "Настройки сессии")
            .setView(formScroll(form)).setPositiveButton(existing == null && launchAfterSave ? "Создать и запустить" : "Сохранить", null).setNegativeButton("Отмена", null).create();
        if (existing == null && launchAfterSave) dialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Сохранённые", (d, w) -> chooseSavedSession());
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (name.getText().toString().trim().isEmpty()) { name.setError("Укажите название"); return; }
            if (!validDirectoryInput(directory)) return;
            int port = 22;
            if (chosenEnvironment[0].equals("ssh")) {
                if (!SshSessionShell.validHost(sshHost.getText().toString().trim())) { sshHost.setError("Укажите адрес без пробелов, пользователя и порта"); return; }
                if (!SshSessionShell.validUser(sshUser.getText().toString().trim())) { sshUser.setError("Укажите пользователя SSH"); return; }
                try { port = Integer.parseInt(sshPort.getText().toString()); } catch (NumberFormatException e) { port = 0; }
                if (!SshSessionShell.validPort(port)) { sshPort.setError("Порт от 1 до 65535"); return; }
            }
            boolean sshPasswordMode = chosenEnvironment[0].equals("ssh") && chosenAuth[0].equals("password");
            if (chosenEnvironment[0].equals("ssh") && chosenAuth[0].equals("key")) {
                String path = identity.getText().toString().trim();
                if (!SshSessionShell.validIdentity(path) || !new File(resolve(path)).isFile() || !new File(resolve(path)).canRead()) {
                    identity.setError("Укажите доступный файл ключа: /путь или ~/путь"); return;
                }
            }
            boolean sameConnection = draft.sshHost.equals(sshHost.getText().toString().trim())
                && draft.sshUser.equals(sshUser.getText().toString().trim()) && draft.sshPort == port;
            String encrypted = sshPasswordMode && rememberPassword.isChecked() && sameConnection ? draft.sshEncryptedPassword : "";
            char[] secret = new char[password.length()]; android.text.TextUtils.getChars(password.getText(), 0, secret.length, secret, 0);
            if (sshPasswordMode && (password.getText().toString().contains("\n") || password.getText().toString().contains("\r") || password.getText().toString().indexOf('\0') >= 0 || secret.length > 256)) {
                java.util.Arrays.fill(secret, '\0'); password.setError("Пароль до 256 символов, без переносов строки"); return;
            }
            if (sshPasswordMode && rememberPassword.isChecked() && encrypted.isEmpty() && secret.length == 0) {
                password.setError("Введите пароль для сохранения"); return;
            }
            if (sshPasswordMode && rememberPassword.isChecked() && secret.length > 0) {
                WorkspaceStore.Session binding = new WorkspaceStore.Session(); binding.id = draft.id;
                binding.sshHost = sshHost.getText().toString().trim(); binding.sshUser = sshUser.getText().toString().trim(); binding.sshPort = port;
                try { encrypted = SshPasswordVault.encrypt(binding, secret); }
                catch (java.security.GeneralSecurityException | RuntimeException e) {
                    java.util.Arrays.fill(secret, '\0'); password.setError("Keystore недоступен. Отключите запоминание или повторите."); return;
                }
            }
            char[] oldSecret = pendingSshPasswords.remove(draft.id); if (oldSecret != null) java.util.Arrays.fill(oldSecret, '\0');
            if (sshPasswordMode && secret.length > 0 && !rememberPassword.isChecked()) pendingSshPasswords.put(draft.id, secret.clone());
            java.util.Arrays.fill(secret, '\0'); password.setText("");
            draft.sshAuth = chosenAuth[0]; draft.sshIdentity = identity.getText().toString().trim(); draft.sshEncryptedPassword = encrypted;
            draft.sshHost = sshHost.getText().toString().trim(); draft.sshUser = sshUser.getText().toString().trim();
            draft.sshPort = port; draft.sshKeepOpen = sshKeepOpen.isChecked();
            draft.codexEnvironment = codexEnvironment[0]; draft.codexAccess = codexAccess[0]; draft.codexChat = codexChat[0]; draft.codexThread = codexThread[0]; draft.codexThreadName = codexThreadName[0];
            draft.environment = chosenEnvironment[0]; draft.name = name.getText().toString().trim(); draft.directory = directory.getText().toString().trim(); draft.startup = startupFields.get(0).getText().toString();
            draft.startup2 = startupFields.size() > 1 ? startupFields.get(1).getText().toString() : "";
            draft.startup3 = startupFields.size() > 2 ? startupFields.get(2).getText().toString() : ""; draft.updated = System.currentTimeMillis();
            if (existing == null) { draft.saved = !launchAfterSave; store.sessions.add(draft); }
            TerminalSession live = liveFor(draft); if (live != null) live.mSessionName = draft.name;
            store.save(); dialog.dismiss(); refresh();
            if (existing == null && launchAfterSave) launch(draft);
            else if (live != null) activity.showToast("Имя обновлено. Каталог и команда применятся при следующем запуске.", true);
        }));
        dialog.setOnDismissListener(d -> password.setText("")); dialog.show();
    }
    private interface ChatChoice { void chosen(String id, String name); }
    private void chooseCodexChat(String environment, ChatChoice choice) {
        TextView loading = text("Загрузка чатов из " + (environment.equals("ubuntu") ? "Ubuntu…" : "Termux…"), 14, MUTED);
        loading.setPadding(dp(20), dp(20), dp(20), dp(20));
        AlertDialog dialog = WorkspaceDialogs.builder(activity).setTitle("Выбрать чат Codex").setView(loading)
            .setNegativeButton("Отмена", null).show();
        codexReader.execute(() -> {
            List<String> ids = new ArrayList<>(), titles = new ArrayList<>(); String failure = null;
            WorkspaceStore.Session profile = new WorkspaceStore.Session(); profile.codexEnvironment = environment;
            try (CodexRpc rpc = CodexRpc.history(activity, profile)) {
                String cursor = null;
                for (int page = 0; page < 10; page++) {
                    org.json.JSONObject params = new org.json.JSONObject().put("limit", 50).put("sortKey", "updated_at").put("archived", false).put("modelProviders", new org.json.JSONArray());
                    if (cursor != null) params.put("cursor", cursor);
                    org.json.JSONObject data = rpc.call("thread/list", params);
                    org.json.JSONArray chats = data.getJSONArray("data");
                    for (int n = 0; n < chats.length(); n++) {
                        org.json.JSONObject chat = chats.getJSONObject(n); String id = chat.getString("id");
                        if (!CodexSessionShell.validThread(id)) continue;
                        ids.add(id); String title = chat.optString("name", "");
                        if (title.isEmpty() || title.equals("null")) title = compact(chat.optString("preview", "Чат Codex"));
                        long date = chat.optLong("updatedAt");
                        titles.add(title + " · " + chat.optString("cwd", "") + (date > 0 ? " · " + android.text.format.DateFormat.format("dd.MM.yy", date * 1000) : ""));
                    }
                    cursor = data.isNull("nextCursor") ? null : data.optString("nextCursor", null); if (cursor == null || cursor.isEmpty()) break;
                }
            } catch (Exception e) { failure = "Не удалось прочитать чаты. Проверьте установку и вход Codex в этой среде. Можно ввести ID чата в настройках сессии."; }
            final String error = failure;
            activity.runOnUiThread(() -> {
                if (closed || !dialog.isShowing()) return;
                if (error != null || ids.isEmpty()) { loading.setText(error == null ? "Сохранённых чатов пока нет. Создайте новый чат." : error); return; }
                dialog.dismiss();
                WorkspaceDialogs.builder(activity).setTitle("Чаты Codex")
                    .setItems(titles.toArray(new String[0]), (d, n) -> choice.chosen(ids.get(n), titles.get(n)))
                    .setNegativeButton("Отмена", null).show();
            });
        });
    }
    private WorkspaceStore.Session currentCodex() {
        TerminalSession live = activity.getCurrentSession();
        if (live == null || !live.isRunning()) return null;
        WorkspaceStore.Session profile = store.findByHandle(live.mHandle);
        return profile != null && profile.environment.equals("codex") ? profile : null;
    }
    private void renderCodexStatus() {
        if (codexStatus == null) return;
        codexStatus.removeAllViews(); WorkspaceStore.Session profile = currentCodex();
        CodexStatus prior = profile == null ? null : codexStatuses.get(profile.id);
        boolean ended = profile != null && prior != null && prior.updated > 0 && liveFor(profile) == null;
        codexStatus.setVisibility(screen == Screen.TERMINAL && profile != null && !ended ? View.VISIBLE : View.GONE);
        if (profile == null) return;
        CodexStatus status = codexStatuses.get(profile.id);
        String label = status == null ? "Codex · подключение…" : !status.error.isEmpty() ? "Codex · нет данных ›" : status.summary() + " ›";
        TextView chip = button(label, false, () -> showCodexStatus(profile)); chip.setMinHeight(dp(34)); chip.setTextSize(12); chip.setTextColor(ACCENT);
        chip.setPadding(dp(12), dp(4), dp(12), dp(4)); chip.setContentDescription("Статус и модель Codex");
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2); params.bottomMargin = dp(6); codexStatus.addView(chip, params);
    }
    private void loadCodexStatus(WorkspaceStore.Session profile, Runnable done) {
        if (closed || codexLoading) return;
        codexLoading = true;
        codexReader.execute(() -> {
            CodexStatus result;
            try { result = CodexStatus.local(activity.getFilesDir(), profile); }
            catch (Exception e) { result = new CodexStatus(); result.error = CodexRpc.diagnostic(e); }
            CodexStatus status = result;
            activity.runOnUiThread(() -> {
                codexLoading = false; if (closed) return;
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
                codexStatuses.put(profile.id, status); renderCodexStatus(); if (done != null) done.run();
            });
        });
    }
    private String codexResetTime(CodexStatus status, String window) {
        org.json.JSONObject limit = status.limits.optJSONObject(window);
        long reset = limit == null ? 0 : limit.optLong("resetsAt");
        return reset > 0 ? "Обновится " + android.text.format.DateFormat.format("dd.MM HH:mm", reset * 1000) : "Время обновления недоступно";
    }
    private void showCodexStatus(WorkspaceStore.Session profile) {
        CodexStatus status = codexStatuses.get(profile.id);
        AlertDialog[] shown = {null};
        LinearLayout form = column();
        if (status == null || !status.error.isEmpty()) {
            form.addView(text(status == null ? "Получение статуса Codex…" : status.error, 14, MUTED));
        } else {
            form.addView(button("Модель: " + (status.model.isEmpty() ? "нет данных" : status.model) + " ›", false, () -> { if (shown[0] != null) shown[0].dismiss(); chooseCodexModel(profile, status); }));
            form.addView(button("Рассуждение: " + (status.effort.isEmpty() ? "нет данных" : effortName(status.effort)) + " ›", false, () -> { if (shown[0] != null) shown[0].dismiss(); chooseCodexEffort(profile, status); }));
            form.addView(text("", 12, MUTED));
            for (String window : new String[]{"primary", "secondary"}) {
                String title = window.equals("primary") ? "Пятичасовой лимит" : "Недельный лимит";
                form.addView(text(CodexStatus.limit(status.limits, window, title), 14, TEXT));
                org.json.JSONObject limit = status.limits.optJSONObject(window);
                if (limit != null && limit.has("usedPercent")) {
                    android.widget.ProgressBar bar = new android.widget.ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
                    bar.setMax(100); bar.setProgress(Math.max(0, Math.min(100, (int)Math.round(100-limit.optDouble("usedPercent", 100)))));
                    bar.setProgressTintList(ColorStateList.valueOf(ACCENT)); form.addView(bar, new LinearLayout.LayoutParams(-1, dp(18)));
                }
                TextView time = text(codexResetTime(status, window), 12, MUTED); time.setPadding(0, dp(4), 0, dp(14)); form.addView(time);
            }
            form.addView(text("Общий лимит аккаунта. Доступ: " + (profile.codexAccess.equals("full") ? "полный" : profile.codexAccess.equals("read") ? "только чтение" : "в пределах проекта") +
                "\nСтатус обновляется раз в 30 секунд. Выбор модели и рассуждения применяется через штатное меню Codex только к этой сессии. Если Codex отвечает, приложение дождётся завершения. Поле ввода сохраняется.", 12, MUTED));
        }
        AlertDialog dialog = WorkspaceDialogs.builder(activity).setTitle("Статус Codex").setView(formScroll(form))
            .setPositiveButton("Обновить", null).setNegativeButton("Закрыть", null).create();
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
        if (status.models.length()==0) {activity.showToast("Каталог моделей недоступен. После этого обновления запустите новый терминал Codex с прежним ID чата.", true);return;}
        String[] labels=new String[status.models.length()];int[] selected={0};
        for(int i=0;i<labels.length;i++) {org.json.JSONObject model=status.models.optJSONObject(i);labels[i]=model==null?"—":model.optString("displayName",model.optString("model"));if(model!=null && model.optString("model").equals(status.model))selected[0]=i;}
        WorkspaceDialogs.builder(activity).setTitle("Модель Codex")
            .setSingleChoiceItems(labels,selected[0],(d,w)-> {d.dismiss();chooseCodexEffortFor(profile,status,status.models.optJSONObject(w));})
            .setNegativeButton("Отмена",null).show();
    }
    private void chooseCodexEffort(WorkspaceStore.Session profile, CodexStatus status) {
        for(int i=0;i<status.models.length();i++) {org.json.JSONObject model=status.models.optJSONObject(i);if(model!=null && model.optString("model").equals(status.model)) {chooseCodexEffortFor(profile,status,model);return;}}
        chooseCodexModel(profile,status);
    }
    private String effortName(String effort) {
        switch(effort) {case "none":return "Без рассуждения";case "minimal":return "Минимальный";case "low":return "Низкий";case "medium":return "Средний";case "high":return "Высокий";case "xhigh":return "Очень высокий";case "max":return "Максимальный";case "ultra":return "Ультра";default:return effort;}
    }
    private void chooseCodexEffortFor(WorkspaceStore.Session profile,CodexStatus status,org.json.JSONObject model) {
        if(model==null)return;
        org.json.JSONArray options=model.optJSONArray("supportedReasoningEfforts");
        List<String> values=new ArrayList<>(),labels=new ArrayList<>();
        if(options!=null)for(int i=0;i<options.length();i++) {org.json.JSONObject option=options.optJSONObject(i);String effort=option==null?"":option.optString("reasoningEffort");if(!CodexModelSelection.effortLabel(effort).isEmpty()) {values.add(effort);labels.add(effortName(effort));}}
        if(values.isEmpty()) {String effort=model.optString("defaultReasoningEffort");if(!CodexModelSelection.effortLabel(effort).isEmpty()) {values.add(effort);labels.add(effortName(effort));}}
        if(values.isEmpty()) {activity.showToast("Codex не сообщил доступные уровни для этой модели",true);return;}
        String current=model.optString("model").equals(status.model)?status.effort:model.optString("defaultReasoningEffort");int[] selected={Math.max(0,values.indexOf(current))};
        WorkspaceDialogs.builder(activity).setTitle(model.optString("displayName",model.optString("model"))+" · поведение")
            .setSingleChoiceItems(labels.toArray(new String[0]),selected[0],(d,w)-> {d.dismiss();confirmCodexModel(profile,model,values.get(w),values.size()==1);})
            .setNegativeButton("Отмена",null).show();
    }
    private void confirmCodexModel(WorkspaceStore.Session profile,org.json.JSONObject model,String effort,boolean singleEffort) {
        WorkspaceDialogs.builder(activity).setTitle("Применить параметры Codex?")
            .setMessage("Модель: "+model.optString("displayName",model.optString("model"))+"\nПоведение: "+effortName(effort)+"\n\nТолько для текущей сессии.")
            .setPositiveButton("Применить",(d,w)->applyCodexModel(profile,model,effort,singleEffort))
            .setNegativeButton("Отмена",null).show();
    }
    private String codexScreen(TerminalSession terminal) {
        com.termux.terminal.TerminalEmulator emulator=terminal.getEmulator();
        return emulator==null?"":emulator.getScreen().getSelectedText(0,0,emulator.mColumns,emulator.mRows-1);
    }
    private void applyCodexModel(WorkspaceStore.Session profile,org.json.JSONObject model,String effort,boolean singleEffort) {
        if(codexApplyDialog!=null && codexApplyDialog.isShowing())return;
        TerminalSession terminal=liveFor(profile);
        if(terminal==null || !terminal.isRunning()) {activity.showToast("Сессия уже завершена",false);return;}
        String id=model.optString("model");
        String wanted="Model changed to "+id+" "+effort;
        CodexStatus cached=codexStatuses.get(profile.id);
        if(cached!=null && cached.model.equals(id) && (cached.effort.equals(effort) || CodexModelSelection.acknowledgement(codexScreen(terminal)).equals(wanted))) {activity.showToast("Эти параметры уже выбраны в Codex",false);return;}
        ((InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(terminalInput.getWindowToken(),0);
        int generation=++codexApplyGeneration;
        AlertDialog dialog=WorkspaceDialogs.builder(activity).setTitle("Параметры Codex").setMessage("Проверяем состояние сессии…")
            .setNegativeButton("Отмена",(d,w)->codexApplyGeneration++).create();codexApplyDialog=dialog;
        dialog.setOnCancelListener(d->codexApplyGeneration++);dialog.show();
        long until=System.currentTimeMillis()+300000;
        Runnable waiting=new Runnable() {public void run() {
            if(closed || generation!=codexApplyGeneration || !dialog.isShowing())return;
            if(!terminal.isRunning() || System.currentTimeMillis()>until) {dialog.dismiss();activity.showToast("Не удалось дождаться свободного Codex",true);return;}
            if(codexLoading) {codexHandler.postDelayed(this,500);return;}
            loadCodexStatus(profile,()-> {
                if(closed || generation!=codexApplyGeneration || !dialog.isShowing())return;
                CodexStatus status=codexStatuses.get(profile.id);
                if(status==null || !status.error.isEmpty()) {dialog.dismiss();activity.showToast(status==null?"Статус Codex недоступен":status.error,true);return;}
                if(!status.state.equals("idle")) {dialog.setMessage("Ожидаем завершения ответа Codex…");codexHandler.postDelayed(this,3000);return;}
                dialog.setMessage("Применяем через штатное меню Codex…");
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
            if(!terminal.isRunning()) {dialog.dismiss();activity.showToast("Сессия завершена",false);return;}
            selection.step(adapter,System.currentTimeMillis());
            if(!selection.finished) {codexHandler.postDelayed(this,200);return;}
            dialog.dismiss();
            if(selection.confirmed) {CodexStatus status=codexStatuses.get(profile.id);if(status!=null) {status.model=model.optString("model");status.effort=effort;codexConfirmedChoices.put(profile.codexSocketId,new String[]{status.thread,status.model,effort,"Model changed to "+status.model+" "+effort});renderCodexStatus();}activity.showToast("Codex подтвердил: "+model.optString("displayName",model.optString("model"))+" · "+effortName(effort),true);}
            else activity.showToast(selection.error,true);
        }};tick.run();
    }
    private void finishWorkspaceSession(TerminalSession terminal) {
        WorkspaceStore.Session profile = store.findByHandle(terminal.mHandle);
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
        AlertDialog.Builder picker = WorkspaceDialogs.builder(activity).setTitle("Файл SSH-ключа");
        if (keys.isEmpty()) picker.setMessage("Ключей в ~/.ssh нет. Укажите путь к своему файлу в поле.");
        else { String[] names = new String[keys.size()]; for (int i = 0; i < names.length; i++) names[i] = keys.get(i).getName();
            picker.setItems(names, (d, which) -> identity.setText(keys.get(which).getAbsolutePath())); }
        picker.setNegativeButton("Закрыть", null).show();
    }
    private void requestSshPassword(WorkspaceStore.Session session, boolean add) {
        LinearLayout form = column(); EditText password = labeled(form, "Пароль SSH", "", false);
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD); password.setSaveEnabled(false);
        AlertDialog dialog = WorkspaceDialogs.builder(activity).setTitle(session.sshUser + "@" + session.sshHost)
            .setView(formScroll(form)).setPositiveButton("Подключиться", null).setNegativeButton("Отмена", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (password.length() == 0) { password.setError("Введите пароль"); return; }
            char[] secret = new char[password.length()]; TextUtils.getChars(password.getText(), 0, secret.length, secret, 0);
            char[] old = pendingSshPasswords.put(session.id, secret); if (old != null) java.util.Arrays.fill(old, '\0');
            password.setText(""); dialog.dismiss(); launchNew(session, add);
        })); dialog.setOnDismissListener(d -> password.setText("")); dialog.show();
    }
    private void commandMenu(WorkspaceStore.Command c) {
        WorkspaceDialogs.builder(activity).setCustomTitle(contextHeading(c.name, c.command, c.category, c.favorite)).setItems(new String[]{c.favorite ? "Убрать из избранного" : "В избранное", "Редактировать", "Удалить"}, (d, which) -> {
            if (which == 0) { c.favorite = !c.favorite; store.save(); renderContent(); }
            else if (which == 1) editCommand(c);
            else confirm("Удалить команду «" + c.name + "»?", () -> { store.commands.remove(c); store.save(); renderContent(); });
        }).setNegativeButton("Закрыть", null).show();
    }
    private void sessionMenu(WorkspaceStore.Session s) { sessionMenu(s, false); }
    private void sessionMenu(WorkspaceStore.Session s, boolean terminalTab) {
        List<String> actions = new ArrayList<>();
        if (terminalTab && !s.saved) actions.add("Сохранить сессию");
        if (terminalTab) Collections.addAll(actions, "Перезапустить", "Настройки", "Закрыть сессию");
        else Collections.addAll(actions, "Закрыть сессию", "Настройки", "Удалить сессию");
        WorkspaceDialogs.builder(activity).setTitle(s.name).setItems(actions.toArray(new String[0]), (d, which) -> {
            String action = actions.get(which);
            if (action.equals("Сохранить сессию")) {
                if (!store.isWritable()) { activity.showToast("Библиотека недоступна для записи", true); return; }
                s.saved = true; s.updated = System.currentTimeMillis(); store.save();
                activity.showToast("Сессия сохранена", false); renderContent(); return;
            }
            if (action.equals("Настройки")) editSession(s);
            else if (action.equals("Перезапустить")) confirm("Остановить текущий процесс и повторно выполнить стартовую команду?", () -> {
                TerminalSession live = liveFor(s);
                if (live != null && live.isRunning()) {
                    if (live.getPid() <= 0) { activity.showToast("Откройте терминал и дождитесь запуска", false); return; }
                    pendingRestarts.put(live.mHandle, s); store.save(); finishWorkspaceSession(live);
                } else {
                    store.save(); launch(s);
                }
            });
            else if (action.equals("Закрыть сессию")) closeSession(s);
            else confirm("Удалить настройки и историю сессии «" + s.name + "»? Активный терминал сначала нужно остановить.", () -> {
                TerminalSession live = liveFor(s);
                if (live != null && live.isRunning()) { activity.showToast("Сначала остановите сессию", true); return; }
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
        AlertDialog.Builder picker = WorkspaceDialogs.builder(activity).setTitle("Сохранённые сессии");
        if (saved.isEmpty()) picker.setMessage("Сохранённых сессий нет. Удерживайте вкладку терминала и выберите «Сохранить сессию».");
        else {
            String[] names = new String[saved.size()];
            for (int i = 0; i < names.length; i++) names[i] = saved.get(i).name;
            picker.setItems(names, (d, which) -> launch(saved.get(which)));
        }
        picker.setNegativeButton("Закрыть", null).show();
    }
    private void closeSession(WorkspaceStore.Session profile) {
        TerminalSession target = liveFor(profile);
        if (target == null) { activity.showToast("Сессия уже закрыта", false); return; }
        if (!store.isWritable()) { activity.showToast("Библиотека недоступна для записи", true); return; }
        AlertDialog.Builder dialog = WorkspaceDialogs.builder(activity).setTitle("Закрыть сессию «" + profile.name + "»?");
        if (profile.saved) {
            dialog.setMessage("Процесс будет остановлен, вкладка терминала закрыта. Сохранённые настройки и история останутся.")
                .setPositiveButton("Закрыть сессию", (d, which) -> finishClose(profile, target, true));
        } else {
            dialog.setMessage("Сохранить настройки и историю команд Bash перед закрытием? История Codex остаётся в самом Codex.")
                .setPositiveButton("Сохранить и закрыть", (d, which) -> finishClose(profile, target, true))
                .setNegativeButton("Закрыть без сохранения", (d, which) -> finishClose(profile, target, false));
        }
        dialog.show();
    }
    private void finishClose(WorkspaceStore.Session profile, TerminalSession target, boolean save) {
        if (liveFor(profile) != target) return;
        if (target.isRunning() && target.getPid() <= 0) {
            activity.showToast("Дождитесь запуска сессии", false); return;
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
        profile.handle = "";
        if (!profile.saved) { store.sessions.remove(profile); eraseSessionFiles(profile); }
        store.save();
    }
    private void eraseSessionFiles(WorkspaceStore.Session profile) {
        char[] secret = pendingSshPasswords.remove(profile.id); if (secret != null) java.util.Arrays.fill(secret, '\0');
        File dir = SessionShell.directory(activity.getFilesDir(), profile.id);
        reader.execute(() -> {
            File[] files = dir.listFiles(); boolean removed = true;
            if (files != null) for (File f : files) if (!f.delete()) removed = false;
            if (dir.exists() && !dir.delete()) removed = false;
            if (!removed) activity.runOnUiThread(() -> activity.showToast("Не удалось полностью удалить историю сессии", true));
        });
    }
    private void confirm(String message, Runnable action) {
        WorkspaceDialogs.builder(activity).setMessage(message).setPositiveButton("Продолжить", (d, w) -> action.run()).setNegativeButton("Отмена", null).show();
    }
    private void sendInput() {
        TerminalSession current = activity.getCurrentSession();
        if (current == null || !current.isRunning() || current.getPid() <= 0) { activity.showToast("Нет работающего терминала", false); return; }
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
        TextView commandTab = button("Сохранённые команды", true, () -> {});
        TextView notesTab = button("Заметки", false, () -> {});
        commandTab.setMaxLines(2); notesTab.setMaxLines(2);
        commandTab.setPadding(dp(6), dp(8), dp(6), dp(8));
        LinearLayout.LayoutParams tabParams = new LinearLayout.LayoutParams(0, -2, 1.5f); tabParams.setMarginEnd(dp(8));
        tabs.addView(commandTab, tabParams); tabs.addView(notesTab, new LinearLayout.LayoutParams(0, -2, 1)); heading.addView(tabs);
        TextView hint = text("Нажми на команду, чтобы вставить", 12, MUTED); hint.setPadding(0, dp(8), 0, dp(12)); heading.addView(hint);
        EditText search = field("Найти команду", "", false); search.setContentDescription("Поиск в меню звёздочки");
        search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH | android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        search.setMinHeight(dp(48)); heading.addView(search, new LinearLayout.LayoutParams(-1, -2));
        TextView empty = text(commands.isEmpty() ? "Сохраните команду в библиотеке или нажмите «Сохранить ввод»." : "Ничего не найдено", 12, MUTED);
        empty.setPadding(0, dp(12), 0, 0); empty.setVisibility(commands.isEmpty() ? View.VISIBLE : View.GONE); heading.addView(empty);
        AlertDialog.Builder builder = WorkspaceDialogs.builder(activity).setCustomTitle(heading)
            .setAdapter(adapter, (dialog, index) -> {
                Object item = visible.get(index);
                if (item instanceof WorkspaceStore.Note) {
                    WorkspaceStore.Command value = new WorkspaceStore.Command(); value.command = ((WorkspaceStore.Note) item).text;
                    insertIntoCurrent(value);
                } else insertIntoCurrent((WorkspaceStore.Command) item);
            })
            .setNegativeButton("Закрыть", null);
        String input = terminalInput.getText().toString();
        if (!input.trim().isEmpty() && store.isWritable()) builder.setNeutralButton("Сохранить ввод", (dialog, which) -> editCommand(null, input));
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
            empty.setText(libraryEmpty ? (noteTab[0] ? "Сохраните заметку в библиотеке." : "Сохраните команду в библиотеке или нажмите «Сохранить ввод».") : "Ничего не найдено");
            empty.setVisibility(visible.isEmpty() ? View.VISIBLE : View.GONE); adapter.notifyDataSetChanged();
            hint.setText(noteTab[0] ? "Нажми на заметку, чтобы вставить" : "Нажми на команду, чтобы вставить");
            search.setHint(noteTab[0] ? "Найти заметку" : "Найти команду");
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
    private void terminalMenu() {
        TerminalSession live = activity.getCurrentSession(); WorkspaceStore.Session profile = live == null ? null : store.findByHandle(live.mHandle);
        WorkspaceDialogs.builder(activity).setTitle(live == null ? "Терминал" : live.mSessionName).setItems(profile != null && profile.environment.equals("ssh") ? new String[]{"Клавиатура", "Дополнительные клавиши", "Интерактивный ввод: " + (interactiveInput ? "включён" : "выключен")} : new String[]{"Клавиатура", "Дополнительные клавиши", "Интерактивный ввод: " + (interactiveInput ? "включён" : "выключен"), "Отменить команды запуска"}, (d, which) -> {
            if (which == 0) toggleKeyboard();
            else if (which == 1) { activity.toggleTerminalToolbar(); toolbarVisibility = activity.getTerminalToolbarViewPager().getVisibility(); }
            else if (which == 2) setInteractiveInput(!interactiveInput);
            else if (which == 3 && profile != null) {
                try { SessionShell.cancel(activity.getFilesDir(), profile.id); activity.showToast("Следующие команды отменены. Текущий процесс продолжает работу.", true); }
                catch (IOException e) { activity.showToast("Не удалось отменить команды", true); }
            }
        }).show();
    }
    public void toggleKeyboard() {
        InputMethodManager keyboard = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        View target = interactiveInput ? activity.getTerminalView() : terminalInput;
        if (target.hasFocus() && com.termux.shared.view.KeyboardUtils.isSoftKeyboardVisible(activity)) {
            keyboard.hideSoftInputFromWindow(target.getWindowToken(), 0);
        } else {
            target.requestFocus(); keyboard.showSoftInput(target, InputMethodManager.SHOW_IMPLICIT);
        }
    }
    private void setInteractiveInput(boolean enabled) {
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
                labels[i] = (target.mSessionName == null ? "Терминал " + (i + 1) : target.mSessionName)
                    + " · " + compact(target.getCwd());
            }
            WorkspaceDialogs.builder(activity).setTitle("В какой терминал вставить?").setItems(labels, (dialog, index) -> {
                TerminalSession target = targets.get(index);
                boolean present = false;
                for (TermuxSession session : live()) if (session.getTerminalSession() == target) present = true;
                if (!present || !target.isRunning()) { activity.showToast("Этот терминал уже завершён", false); return; }
                activity.getTermuxTerminalSessionClient().setCurrentSession(target); insertIntoCurrent(command);
            }).setNegativeButton("Отмена", null).show();
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
        TerminalSession existing = liveFor(s);
        if (existing != null && existing.isRunning()) {
            activity.getTermuxTerminalSessionClient().setCurrentSession(existing); show(Screen.TERMINAL, true); return;
        }
        if (existing != null) { store.save(); }
        launchNew(s, false);
    }
    private void launchNew(WorkspaceStore.Session s, boolean add) {
        if (s.environment.equals("ssh") && s.sshAuth.equals("password") && s.sshEncryptedPassword.isEmpty() && !pendingSshPasswords.containsKey(s.id)) {
            requestSshPassword(s, add); return;
        }
        if (activity.getTermuxService() == null) { activity.showToast("Терминальный сервис ещё запускается", false); return; }
        if (!store.isWritable()) { activity.showToast("Библиотека недоступна для записи", true); return; }
        activity.prepareWorkspaceBootstrap(() -> {
            if (closed || activity.isFinishing()) return;
            TerminalSession previous = liveFor(s);
            TerminalSession live = create(s, add, previous != null && !previous.isRunning() ? previous : null);
            if (live != null) { activity.getTermuxTerminalSessionClient().setCurrentSession(live); show(Screen.TERMINAL, true); }
        });
    }
    /** Used by the original new-session action after bootstrap setup. */
    public TermuxSession createDefault(String name, String directory) {
        WorkspaceStore.Session profile = new WorkspaceStore.Session(); profile.name = name == null || name.trim().isEmpty() ? "Bash" : name;
        profile.directory = directory == null ? TermuxConstants.TERMUX_HOME_DIR_PATH : directory;
        TerminalSession session = create(profile, true);
        if (session == null) return null;
        return activity.getTermuxService().getTermuxSession(activity.getTermuxService().getIndexOfSession(session));
    }
    private TerminalSession create(WorkspaceStore.Session profile, boolean add) {
        return create(profile, add, null);
    }
    private TerminalSession create(WorkspaceStore.Session profile, boolean add, TerminalSession replaced) {
        TermuxService service = activity.getTermuxService();
        if (service == null) return null;
        boolean replacing = replaced != null && !replaced.isRunning() && service.getIndexOfSession(replaced) >= 0;
        if (service.getTermuxSessionsSize() >= 8 && !replacing) { activity.showToast("Максимум 8 терминалов. Закройте завершённую сессию.", true); return null; }
        String directory = (profile.environment.equals("ubuntu") || profile.environment.equals("ssh") || (profile.environment.equals("codex") && profile.codexEnvironment.equals("ubuntu"))) ? TermuxConstants.TERMUX_HOME_DIR_PATH : resolve(profile.directory);
        if (!new File(directory).isDirectory() || !new File(directory).canExecute()) {
            activity.showToast("Каталог недоступен: " + directory, true); return null;
        }
        SshPasswordBroker broker = null;
        try {
            String askpass = null, socket = null;
            if (profile.environment.equals("ssh") && profile.sshAuth.equals("password")) {
                char[] secret = pendingSshPasswords.remove(profile.id);
                if (secret == null && !profile.sshEncryptedPassword.isEmpty()) secret = SshPasswordVault.decrypt(profile);
                if (secret == null) { activity.showToast("Введите пароль в настройках сессии", true); return null; }
                try {
                    askpass = SessionShell.prepareAskpass(activity.getFilesDir(), profile, activity.getApplicationInfo().sourceDir).getAbsolutePath();
                    broker = new SshPasswordBroker(activity, secret); socket = broker.address;
                } finally { java.util.Arrays.fill(secret, '\0'); }
            }
            if (profile.environment.equals("codex")) { profile.codexSocketId = java.util.UUID.randomUUID().toString(); CodexSessionShell.prepareBridge(activity); }
            if (profile.environment.equals("codex") && (profile.codexChat.equals("choose") || profile.codexChat.equals("last"))) { activity.showToast("Выберите конкретный чат или введите его ID в настройках сессии", true); return null; }
            File rc = SessionShell.prepare(activity.getFilesDir(), profile, askpass, socket);
            codexStatuses.remove(profile.id);
            // service notifications are synchronous; register the new profile before they fire.
            if (add) store.sessions.add(profile);
            creating = true;
            TermuxSession created;
            try { created = service.createTermuxSession(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/bash",
                new String[]{"--rcfile", rc.getAbsolutePath(), "-i"}, null, directory, false, profile.name);
            } finally { creating = false; }
            if (created == null) { if (broker != null) broker.close(); if (add) store.sessions.remove(profile); activity.showToast("Не удалось создать терминал", true); return null; }
            TerminalSession session = created.getTerminalSession();
            // The synchronous callback may have adopted it; discard that temporary record.
            WorkspaceStore.Session adopted = store.findByHandle(session.mHandle);
            if (adopted != null && adopted != profile) store.sessions.remove(adopted);
            profile.handle = session.mHandle; profile.updated = System.currentTimeMillis();
            // Create first: removing the last old session would otherwise stop the service.
            if (replacing) service.removeTermuxSession(replaced);
            store.save(); refresh();
            if (profile.environment.equals("codex")) codexHandler.postDelayed(() -> { if (!closed) loadCodexStatus(profile, null); }, 1500);
            return session;
        } catch (java.security.GeneralSecurityException e) {
            if (broker != null) broker.close(); activity.showToast("Сохранённый пароль недоступен. Введите его заново в настройках сессии.", true); return null;
        } catch (IOException | RuntimeException e) {
            if (broker != null) broker.close(); activity.showToast("Не удалось подготовить сессию", true); return null;
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
                if (!profile.saved && !profile.handle.isEmpty() && liveFor(profile) == null) {
                    store.sessions.remove(profile); eraseSessionFiles(profile); changed = true;
                }
            }
        }
        if (changed) store.save();
        renderTabs(); renderHeader(); renderContent();
    }
    public boolean onFinished(TerminalSession session) {
        WorkspaceStore.Session profile = store.findByHandle(session.mHandle);
        if (profile != null) { store.save(); }
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
        if (screen == Screen.HOME) return false;
        show(Screen.HOME, true); return true;
    }
    public void close() {
        terminalDrafts.clear();
        for (char[] secret : pendingSshPasswords.values()) java.util.Arrays.fill(secret, '\0'); pendingSshPasswords.clear();
        closed = true; codexApplyGeneration++; if(codexApplyDialog!=null)codexApplyDialog.dismiss(); codexHandler.removeCallbacksAndMessages(null); codexReader.shutdown(); reader.shutdown(); store.close();
    }
}
