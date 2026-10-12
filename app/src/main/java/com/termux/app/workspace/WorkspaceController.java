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
    private final LinearLayout header, navigation, tabs, inputRow;
    private final EditText terminalInput;
    private final FrameLayout content;
    private final ExecutorService reader = Executors.newSingleThreadExecutor();
    private Screen screen = Screen.HOME;
    private String query = "", journalQuery = "", commandCategory = "", noteCategory = "";
    private boolean noteFavorites, favorites, savedOnly, closed, creating;
    private int toolbarVisibility = View.GONE;
    private boolean interactiveInput;
    private float swipeStart;
    private final Set<String> pendingCloses = new java.util.HashSet<>();
    private final java.util.Map<String, WorkspaceStore.Session> pendingRestarts = new java.util.HashMap<>();

    public WorkspaceController(TermuxActivity activity) {
        this.activity = activity;
        store = new WorkspaceStore(activity, message -> activity.showToast(message, true));
        interactiveInput = activity.getSharedPreferences("workspace-input", Context.MODE_PRIVATE)
            .getBoolean("interactive-input", false);
        activity.getTerminalView().setSoftKeyboardInputEnabled(interactiveInput);
        header = activity.findViewById(R.id.workspace_header);
        navigation = activity.findViewById(R.id.workspace_navigation);
        tabs = activity.findViewById(R.id.workspace_tabs);
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
        renderHeader(); renderNavigation(); renderTabs(); renderContent();
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
    private void renderTabs() {
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
        ScrollView journal = new ScrollView(activity); journal.setFillViewport(true);
        journal.setBackground(shape(PANEL, BORDER, 16)); journal.setClipToOutline(true);
        journal.setContentDescription("Журнал последних 50 команд");
        journal.setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(event.getActionMasked() != MotionEvent.ACTION_UP
                && event.getActionMasked() != MotionEvent.ACTION_CANCEL);
            return false;
        });
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
        ScrollView saved = new ScrollView(activity); saved.setFillViewport(true);
        saved.setBackground(shape(PANEL, BORDER, 16)); saved.setClipToOutline(true);
        saved.setContentDescription("Список сохранённых сессий");
        saved.setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(event.getActionMasked() != MotionEvent.ACTION_UP
                && event.getActionMasked() != MotionEvent.ACTION_CANCEL);
            return false;
        });
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
        ScrollView panel = new ScrollView(activity); panel.setFillViewport(true);
        panel.setBackground(shape(PANEL, BORDER, 16)); panel.setClipToOutline(true); panel.setContentDescription(description);
        panel.setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(event.getActionMasked() != MotionEvent.ACTION_UP
                && event.getActionMasked() != MotionEvent.ACTION_CANCEL); return false;
        });
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
        String[] environments = {"legacy", "termux", "ubuntu"};
        String[] environmentNames = {"Как раньше", "Termux · ждать Bash", "Ubuntu · ждать Bash"};
        TextView environment = button("", false, () -> {});
        environment.setContentDescription("Среда запуска");
        Runnable updateEnvironment = () -> {
            int index = java.util.Arrays.asList(environments).indexOf(chosenEnvironment[0]);
            environment.setText("Среда запуска: " + environmentNames[Math.max(0, index)]);
        };
        updateEnvironment.run(); form.addView(environment);
        environment.setOnClickListener(v -> WorkspaceDialogs.builder(activity).setTitle("Среда запуска")
            .setSingleChoiceItems(environmentNames, java.util.Arrays.asList(environments).indexOf(chosenEnvironment[0]), (d, which) -> {
                chosenEnvironment[0] = environments[which];
                if (which == 2 && directory.getText().toString().equals(TermuxConstants.TERMUX_HOME_DIR_PATH)) directory.setText("/root");
                else if (which != 2 && directory.getText().toString().equals("/root")) directory.setText(TermuxConstants.TERMUX_HOME_DIR_PATH);
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
        TextView hint = text("Termux / Ubuntu: команды запускаются после готовности Bash в выбранной среде. Вход в Ubuntu выполняется автоматически — команду proot-distro login вводить не нужно. До трёх шагов, при ошибке остановка. После запуска Codex следующие шаги ждут его выхода.", 12, MUTED);
        hint.setPadding(0, dp(16), 0, 0); form.addView(hint);
        AlertDialog dialog = WorkspaceDialogs.builder(activity).setTitle(existing == null ? "Новая сессия" : "Настройки сессии")
            .setView(formScroll(form)).setPositiveButton(existing == null && launchAfterSave ? "Создать и запустить" : "Сохранить", null).setNegativeButton("Отмена", null).create();
        if (existing == null && launchAfterSave) dialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Сохранённые", (d, w) -> chooseSavedSession());
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (name.getText().toString().trim().isEmpty()) { name.setError("Укажите название"); return; }
            if (!validDirectoryInput(directory)) return;
            draft.environment = chosenEnvironment[0]; draft.name = name.getText().toString().trim(); draft.directory = directory.getText().toString().trim(); draft.startup = startupFields.get(0).getText().toString();
            draft.startup2 = startupFields.size() > 1 ? startupFields.get(1).getText().toString() : "";
            draft.startup3 = startupFields.size() > 2 ? startupFields.get(2).getText().toString() : ""; draft.updated = System.currentTimeMillis();
            if (existing == null) { draft.saved = !launchAfterSave; store.sessions.add(draft); }
            TerminalSession live = liveFor(draft); if (live != null) live.mSessionName = draft.name;
            store.save(); dialog.dismiss(); refresh();
            if (existing == null && launchAfterSave) launch(draft);
            else if (live != null) activity.showToast("Имя обновлено. Каталог и команда применятся при следующем запуске.", true);
        })); dialog.show();
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
                    pendingRestarts.put(live.mHandle, s); store.save(); live.finishIfRunning();
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
        if (target.isRunning()) { pendingCloses.add(target.mHandle); target.finishIfRunning(); }
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
        AlertDialog picker = builder.create(); picker.setOnShowListener(d -> heading.requestFocus()); picker.show();
    }
    private void terminalMenu() {
        TerminalSession live = activity.getCurrentSession(); WorkspaceStore.Session profile = live == null ? null : store.findByHandle(live.mHandle);
        WorkspaceDialogs.builder(activity).setTitle(live == null ? "Терминал" : live.mSessionName).setItems(new String[]{"Клавиатура", "Дополнительные клавиши", "Интерактивный ввод: " + (interactiveInput ? "включён" : "выключен"), "Отменить команды запуска"}, (d, which) -> {
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
        String directory = profile.environment.equals("ubuntu") ? TermuxConstants.TERMUX_HOME_DIR_PATH : resolve(profile.directory);
        if (!new File(directory).isDirectory() || !new File(directory).canExecute()) {
            activity.showToast("Каталог недоступен: " + directory, true); return null;
        }
        try {
            File rc = SessionShell.prepare(activity.getFilesDir(), profile);
            // service notifications are synchronous; register the new profile before they fire.
            if (add) store.sessions.add(profile);
            creating = true;
            TermuxSession created;
            try { created = service.createTermuxSession(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/bash",
                new String[]{"--rcfile", rc.getAbsolutePath(), "-i"}, null, directory, false, profile.name);
            } finally { creating = false; }
            if (created == null) { if (add) store.sessions.remove(profile); activity.showToast("Не удалось создать терминал", true); return null; }
            TerminalSession session = created.getTerminalSession();
            // The synchronous callback may have adopted it; discard that temporary record.
            WorkspaceStore.Session adopted = store.findByHandle(session.mHandle);
            if (adopted != null && adopted != profile) store.sessions.remove(adopted);
            profile.handle = session.mHandle; profile.updated = System.currentTimeMillis();
            // Create first: removing the last old session would otherwise stop the service.
            if (replacing) service.removeTermuxSession(replaced);
            store.save(); refresh();
            return session;
        } catch (IOException e) { activity.showToast("Не удалось подготовить сессию: " + e.getMessage(), true); return null; }
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
        closed = true; reader.shutdown(); store.close();
    }
}
