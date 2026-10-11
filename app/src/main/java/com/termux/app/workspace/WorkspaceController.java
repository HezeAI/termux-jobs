package com.termux.app.workspace;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.text.InputType;
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
import java.util.LinkedHashSet;
import java.util.List;
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
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService reader = Executors.newSingleThreadExecutor();
    private Screen screen = Screen.HOME;
    private String query = "", category = "Все";
    private boolean favorites, savedOnly, closed, snapshotPending, creating;
    private int toolbarVisibility = View.GONE;
    private final Runnable snapshot = () -> { snapshotPending = false; capture(); };
    private float swipeStart;
    private final java.util.Map<String, WorkspaceStore.Session> pendingRestarts = new java.util.HashMap<>();

    public WorkspaceController(TermuxActivity activity) {
        this.activity = activity;
        store = new WorkspaceStore(activity, message -> activity.showToast(message, true));
        header = activity.findViewById(R.id.workspace_header);
        navigation = activity.findViewById(R.id.workspace_navigation);
        tabs = activity.findViewById(R.id.workspace_tabs);
        content = activity.findViewById(R.id.workspace_content);
        inputRow = activity.findViewById(R.id.workspace_input);
        inputRow.setPadding(dp(12), dp(6), dp(12), dp(6));
        terminalInput = field("Введите команду…", "", false);
        terminalInput.setTypeface(Typeface.MONOSPACE); terminalInput.setTextSize(12);
        terminalInput.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEND);
        terminalInput.setOnEditorActionListener((v, action, event) -> {
            if (action == android.view.inputmethod.EditorInfo.IME_ACTION_SEND ||
                (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER && event.getAction() == android.view.KeyEvent.ACTION_DOWN)) {
                sendInput(); return true;
            }
            return false;
        });
        inputRow.addView(terminalInput, new LinearLayout.LayoutParams(0, -1, 1));
        TextView commands = button("★", false, this::chooseInputCommand);
        commands.setTextColor(ACCENT); commands.setTextSize(22);
        commands.setContentDescription("Вставить сохранённую команду");
        LinearLayout.LayoutParams commandsParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        commandsParams.setMarginStart(dp(8)); inputRow.addView(commands, commandsParams);
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
        LinearLayout r = row(); r.setPadding(0, dp(22), 0, dp(10));
        TextView t = text(title, 17, TEXT); t.setTypeface(null, Typeface.BOLD);
        r.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
        if (action != null) { TextView a = button(action, false, click); a.setTextColor(ACCENT); r.addView(a); }
        parent.addView(r);
    }
    private void show(Screen target, boolean animate) {
        if (screen == Screen.TERMINAL && target != Screen.TERMINAL) {
            toolbarVisibility = activity.getTerminalToolbarViewPager().getVisibility();
            InputMethodManager keyboard = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            keyboard.hideSoftInputFromWindow(activity.getTerminalView().getWindowToken(), 0);
            capture();
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
        header.removeAllViews(); header.setPadding(dp(16), dp(8), dp(12), dp(8));
        TerminalSession current = activity.getCurrentSession();
        String title = screen == Screen.HOME ? "Termux" : screen == Screen.SESSIONS ? "Сессии" : screen == Screen.COMMANDS ? "Команды" : current != null && current.mSessionName != null ? current.mSessionName : "Терминал";
        LinearLayout labels = column(); TextView name = text(title, 22, TEXT); name.setTypeface(null, Typeface.BOLD); name.setSingleLine(); name.setEllipsize(TextUtils.TruncateAt.END); labels.addView(name);
        String subtitle = screen == Screen.HOME ? "Ваше рабочее пространство" : screen == Screen.SESSIONS ? "Настройки и история под рукой" : screen == Screen.COMMANDS ? "Сохранить и вставить в терминал." : currentDirectory();
        TextView sub = text(subtitle, 11, MUTED); sub.setSingleLine(); sub.setEllipsize(TextUtils.TruncateAt.MIDDLE); labels.addView(sub);
        header.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        if (screen == Screen.HOME) {
            TextView settings = button("⚙", false, () -> activity.startActivity(new android.content.Intent(activity, SettingsActivity.class)));
            settings.setContentDescription("Настройки приложения"); header.addView(settings);
        } else if (screen == Screen.TERMINAL) {
            TextView menu = button("⋮", false, this::terminalMenu); menu.setContentDescription("Меню терминала"); header.addView(menu);
        } else {
            TextView search = button("⌕", false, this::search); search.setContentDescription("Поиск"); header.addView(search);
            TextView add = button("+", true, () -> { if (screen == Screen.COMMANDS) editCommand(null); else editSession(null); });
            add.setContentDescription(screen == Screen.COMMANDS ? "Новая команда" : "Новая сессия");
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(48), dp(48)); lp.setMarginStart(dp(8)); header.addView(add, lp);
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
        navigation.removeAllViews(); navigation.setPadding(dp(8), dp(6), dp(8), dp(6));
        String[] labels = {"⌂\nГлавная", "▤\nСессии", ">_\nКоманды", "▣\nТерминал"};
        Screen[] targets = Screen.values();
        for (int i = 0; i < targets.length; i++) {
            Screen target = targets[i]; TextView nav = button(labels[i], false, () -> show(target, true));
            nav.setTextSize(11); tintButton(nav, target == screen ? ACCENT : MUTED);
            nav.setBackground(shape(target == screen ? 0xff102522 : Color.BLACK, Color.BLACK, 12));
            navigation.addView(nav, new LinearLayout.LayoutParams(0, dp(56), 1));
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
        for (TermuxSession ts : live()) {
            TerminalSession s = ts.getTerminalSession();
            TextView tab = button(s.mSessionName == null ? "Bash" : s.mSessionName, false, () -> {
                activity.getTermuxTerminalSessionClient().setCurrentSession(s); show(Screen.TERMINAL, false);
            });
            tab.setTextColor(activity.getCurrentSession() == s ? ACCENT : MUTED);
            tab.setBackground(shape(activity.getCurrentSession() == s ? 0xff102522 : PANEL,
                activity.getCurrentSession() == s ? ACCENT : BORDER, 10));
            tab.setOnLongClickListener(v -> { WorkspaceStore.Session p = store.findByHandle(s.mHandle); if (p != null) sessionMenu(p); return true; });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(44)); lp.setMarginEnd(dp(6)); tabs.addView(tab, lp);
        }
        tabs.addView(button("+", false, () -> editSession(null)));
    }
    private void renderContent() {
        if (screen == Screen.TERMINAL) return;
        content.removeAllViews(); ScrollView scroll = new ScrollView(activity); scroll.setFillViewport(true);
        LinearLayout body = column(); body.setPadding(dp(16), 0, dp(16), dp(20)); scroll.addView(body); content.addView(scroll);
        if (screen == Screen.HOME) home(body);
        else if (screen == Screen.COMMANDS) commands(body);
        else sessions(body);
    }
    private void home(LinearLayout body) {
        TextView eyebrow = text("ГОТОВО К РАБОТЕ", 10, ACCENT); eyebrow.setLetterSpacing(.14f); eyebrow.setPadding(0, dp(12), 0, dp(10)); body.addView(eyebrow);
        LinearLayout shortcuts = row();
        shortcuts.addView(button("+\nНовая сессия", false, () -> editSession(null)), new LinearLayout.LayoutParams(0, dp(88), 1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(88), 1); lp.setMarginStart(dp(8));
        shortcuts.addView(button(">_\nКоманды", false, () -> show(Screen.COMMANDS, true)), lp);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, dp(88), 1); lp2.setMarginStart(dp(8));
        shortcuts.addView(button("▤\nСессии", false, () -> show(Screen.SESSIONS, true)), lp2); body.addView(shortcuts);
        section(body, "Недавние сессии", "Все ›", () -> show(Screen.SESSIONS, true));
        List<WorkspaceStore.Session> recent = sortedSessions();
        if (recent.isEmpty()) empty(body, "Начните с новой сессии", "Задайте имя, каталог и стартовую команду.");
        for (int i = 0; i < Math.min(4, recent.size()); i++) sessionCard(body, recent.get(i));
        section(body, "Избранные команды", "Все ›", () -> { favorites = true; show(Screen.COMMANDS, true); });
        int count = 0;
        for (WorkspaceStore.Command c : store.commands) if (c.favorite && count++ < 4) commandCard(body, c);
        if (count == 0) empty(body, "Ваши команды будут здесь", "Сохраните команду и отметьте её звездой.");
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
        chips(body, new String[]{"Все команды", "Избранное"}, favorites ? "Избранное" : "Все команды", value -> { favorites = value.equals("Избранное"); renderContent(); });
        Set<String> categories = new LinkedHashSet<>(); categories.add("Все");
        for (WorkspaceStore.Command c : store.commands) categories.add(c.category);
        if (!categories.contains(category)) category = "Все";
        chips(body, categories.toArray(new String[0]), category, value -> { category = value; renderContent(); });
        searchSummary(body); int count = 0;
        for (WorkspaceStore.Command c : store.commands) {
            if ((!favorites || c.favorite) && (category.equals("Все") || category.equals(c.category))
                && matches(c.name + " " + c.command + " " + c.category)) { commandCard(body, c); count++; }
        }
        if (count == 0) empty(body, "Команд пока нет", "Нажмите +, чтобы сохранить команду, или измените фильтры.");
        TextView hint = text("Выберите команду, чтобы вставить её в поле текущего терминала. Отправьте её самостоятельно.", 12, MUTED); hint.setPadding(0, dp(16), 0, 0); body.addView(hint);
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
            if ((savedOnly || liveFor(s) != null) && matches(s.name + " " + s.directory + " " + s.startup)) { sessionCard(body, s); count++; }
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
        LinearLayout card = card(parent); cardLabels(card, c.name, c.command, c.category + (c.favorite ? "  ★" : ""), false);
        card.setOnClickListener(v -> insertCommand(c));
        TextView run = button("↳", false, () -> insertCommand(c)); run.setContentDescription("Вставить " + c.name); card.addView(run);
        TextView more = button("⋮", false, () -> commandMenu(c)); more.setContentDescription("Действия с командой " + c.name); card.addView(more);
    }
    private void sessionCard(LinearLayout parent, WorkspaceStore.Session s) {
        TerminalSession running = liveFor(s); boolean active = running != null && running.isRunning();
        LinearLayout card = card(parent); cardLabels(card, s.name, compact(s.directory), active ? "● Активна" : running != null ? "Завершена" : "Сохранена", true);
        card.setOnClickListener(v -> launch(s));
        TextView run = button(active ? "›" : "▷", false, () -> launch(s)); run.setContentDescription(active ? "Открыть " + s.name : "Запустить " + s.name); card.addView(run);
        TextView more = button("⋮", false, () -> sessionMenu(s)); more.setContentDescription("Действия с сессией " + s.name); card.addView(more);
    }
    private void search() {
        EditText input = field("Название, команда или каталог", query, false);
        new AlertDialog.Builder(activity).setTitle("Поиск").setView(input)
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
        AlertDialog dialog = new AlertDialog.Builder(activity).setTitle(existing == null ? "Новая команда" : "Редактировать команду")
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
    private void editSession(WorkspaceStore.Session existing) {
        if (!store.isWritable()) { activity.showToast("Библиотека недоступна для записи", true); return; }
        WorkspaceStore.Session draft = existing == null ? new WorkspaceStore.Session() : existing;
        LinearLayout form = column(); EditText name = labeled(form, "Название сессии", existing == null ? "" : draft.name, false);
        EditText directory = labeled(form, "Рабочий каталог", existing == null ? TermuxConstants.TERMUX_HOME_DIR_PATH : draft.directory, false);
        EditText startup = labeled(form, "Стартовая команда (необязательно)", draft.startup, true);
        TextView hint = text("Сохраняются каталог, стартовая команда, история Bash и снимок терминала. Команды повторно запускаются только по нажатию.", 12, MUTED);
        hint.setPadding(0, dp(16), 0, 0); form.addView(hint);
        AlertDialog dialog = new AlertDialog.Builder(activity).setTitle(existing == null ? "Новая сессия" : "Настройки сессии")
            .setView(formScroll(form)).setPositiveButton(existing == null ? "Создать" : "Сохранить", null).setNegativeButton("Отмена", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (name.getText().toString().trim().isEmpty()) { name.setError("Укажите название"); return; }
            if (!validDirectoryInput(directory)) return;
            draft.name = name.getText().toString().trim(); draft.directory = directory.getText().toString().trim(); draft.startup = startup.getText().toString(); draft.updated = System.currentTimeMillis();
            if (existing == null) store.sessions.add(draft);
            TerminalSession live = liveFor(draft); if (live != null) live.mSessionName = draft.name;
            store.save(); dialog.dismiss(); refresh();
            if (existing == null) launch(draft);
            else if (live != null) activity.showToast("Имя обновлено. Каталог и команда применятся при следующем запуске.", true);
        })); dialog.show();
    }
    private void commandMenu(WorkspaceStore.Command c) {
        new AlertDialog.Builder(activity).setTitle(c.name).setItems(new String[]{"Вставить в терминал", "Редактировать", "Дублировать", c.favorite ? "Убрать из избранного" : "В избранное", "Удалить"}, (d, which) -> {
            if (which == 0) insertCommand(c);
            else if (which == 1) editCommand(c);
            else if (which == 2) { store.commands.add(c.copy()); store.save(); renderContent(); }
            else if (which == 3) { c.favorite = !c.favorite; store.save(); renderContent(); }
            else confirm("Удалить команду «" + c.name + "»?", () -> { store.commands.remove(c); store.save(); renderContent(); });
        }).show();
    }
    private void sessionMenu(WorkspaceStore.Session s) {
        new AlertDialog.Builder(activity).setTitle(s.name).setItems(new String[]{"Открыть / восстановить", "Настройки", "История", "Дублировать", "Перезапустить", "Остановить", "Удалить сохранённую", "Закрыть завершённую вкладку"}, (d, which) -> {
            if (which == 0) launch(s);
            else if (which == 1) editSession(s);
            else if (which == 2) history(s);
            else if (which == 3) { store.sessions.add(s.copy()); store.save(); renderContent(); }
            else if (which == 4) confirm("Остановить текущий процесс и повторно выполнить стартовую команду?", () -> {
                TerminalSession live = liveFor(s);
                if (live != null && live.isRunning()) {
                    if (live.getPid() <= 0) { activity.showToast("Откройте терминал и дождитесь запуска", false); return; }
                    captureOne(s, live); pendingRestarts.put(live.mHandle, s); store.save(); live.finishIfRunning();
                } else {
                    store.save(); launch(s);
                }
            });
            else if (which == 5) confirm("Остановить процесс в сессии «" + s.name + "»?", () -> {
                TerminalSession live = liveFor(s);
                if (live != null && live.getPid() > 0) { captureOne(s, live); live.finishIfRunning(); store.save(); }
            });
            else if (which == 7) {
                TerminalSession live = liveFor(s);
                if (live == null) return;
                if (live.isRunning()) { activity.showToast("Сначала остановите сессию", true); return; }
                activity.getTermuxTerminalSessionClient().removeFinishedSession(live);
            }
            else confirm("Удалить настройки и историю сессии «" + s.name + "»? Активный терминал сначала нужно остановить.", () -> {
                TerminalSession live = liveFor(s);
                if (live != null && live.isRunning()) { activity.showToast("Сначала остановите сессию", true); return; }
                if (live != null) activity.getTermuxTerminalSessionClient().removeFinishedSession(live);
                store.sessions.remove(s); store.save();
                File dir = SessionShell.directory(activity.getFilesDir(), s.id);
                reader.execute(() -> { File[] files = dir.listFiles(); if (files != null) for (File f : files) f.delete(); dir.delete(); });
                renderContent();
            });
        }).show();
    }
    private void confirm(String message, Runnable action) {
        new AlertDialog.Builder(activity).setMessage(message).setPositiveButton("Продолжить", (d, w) -> action.run()).setNegativeButton("Отмена", null).show();
    }
    private void sendInput() {
        TerminalSession current = activity.getCurrentSession();
        if (current == null || !current.isRunning() || current.getPid() <= 0) { activity.showToast("Нет работающего терминала", false); return; }
        String command = terminalInput.getText().toString();
        if (command.trim().isEmpty()) return;
        current.write(command + "\r"); terminalInput.setText(""); activity.getTerminalView().requestFocus();
    }
    private void chooseInputCommand() {
        List<WorkspaceStore.Command> commands = new ArrayList<>(store.commands);
        Collections.sort(commands, (a, b) -> Boolean.compare(b.favorite, a.favorite));
        AlertDialog.Builder picker = new AlertDialog.Builder(activity)
            .setTitle("Вставить команду").setNegativeButton("Отмена", null);
        if (commands.isEmpty()) {
            picker.setMessage("Сохраните команду в библиотеке или введите её в поле и нажмите «Сохранить ввод».");
        } else {
            String[] labels = new String[commands.size()];
            for (int i = 0; i < commands.size(); i++) {
                WorkspaceStore.Command command = commands.get(i);
                labels[i] = (command.favorite ? "★ " : "") + command.name + " · " + command.category
                    + "\n" + command.command;
            }
            picker.setItems(labels, (dialog, index) -> {
                insertCommand(commands.get(index));
            });
        }
        String input = terminalInput.getText().toString();
        if (!input.trim().isEmpty() && store.isWritable()) {
            picker.setNeutralButton("Сохранить ввод", (dialog, which) -> editCommand(null, input));
        }
        picker.show();
    }
    private void terminalMenu() {
        TerminalSession live = activity.getCurrentSession(); WorkspaceStore.Session profile = live == null ? null : store.findByHandle(live.mHandle);
        new AlertDialog.Builder(activity).setTitle(live == null ? "Терминал" : live.mSessionName).setItems(new String[]{"Новая сессия", "Настройки сессии", "Сохранить снимок", "История", "Клавиатура", "Дополнительные клавиши"}, (d, which) -> {
            if (which == 0) editSession(null);
            else if (which == 1 && profile != null) editSession(profile);
            else if (which == 2) { capture(); activity.showToast("Снимок передан на сохранение", false); }
            else if (which == 3 && profile != null) history(profile);
            else if (which == 4) { InputMethodManager keyboard = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE); keyboard.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0); }
            else if (which == 5) { activity.toggleTerminalToolbar(); toolbarVisibility = activity.getTerminalToolbarViewPager().getVisibility(); }
        }).show();
    }
    private void history(WorkspaceStore.Session s) {
        TerminalSession live = liveFor(s); if (live != null) { captureOne(s, live); store.save(); }
        reader.execute(() -> {
            String commands;
            try { commands = SessionShell.readHistory(activity.getFilesDir(), s.id); }
            catch (IOException e) { commands = "Не удалось прочитать историю Bash."; }
            final String result = commands;
            handler.post(() -> {
                if (closed || activity.isFinishing()) return;
                LinearLayout body = column(); body.setPadding(dp(16), dp(12), dp(16), dp(16));
                body.addView(text("История команд Bash", 16, ACCENT)); TextView history = text(result.isEmpty() ? "Команды появятся после возвращения к приглашению Bash." : result, 12, TEXT);
                history.setTypeface(Typeface.MONOSPACE); history.setTextIsSelectable(true); body.addView(history);
                TextView label = text("\nПоследний снимок терминала", 16, ACCENT); body.addView(label);
                TextView transcript = text(s.transcript.isEmpty() ? "Пока нет вывода" : s.transcript, 12, MUTED); transcript.setTypeface(Typeface.MONOSPACE); transcript.setTextIsSelectable(true); body.addView(transcript);
                ScrollView scroll = new ScrollView(activity); scroll.addView(body);
                new AlertDialog.Builder(activity).setTitle(s.name).setView(scroll).setPositiveButton("Закрыть", null).show();
            });
        });
    }
    private void insertCommand(WorkspaceStore.Command command) {
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
        if (existing != null) { captureOne(s, existing); store.save(); }
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
        String directory = resolve(profile.directory);
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
        if (changed) store.save();
        renderTabs(); renderHeader(); renderContent();
    }
    public boolean onFinished(TerminalSession session) {
        WorkspaceStore.Session profile = store.findByHandle(session.mHandle);
        if (profile != null) { captureOne(profile, session); store.save(); }
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
    public void onOutput() {
        if (closed || snapshotPending) return;
        snapshotPending = true; handler.postDelayed(snapshot, 3000);
    }
    private void captureOne(WorkspaceStore.Session profile, TerminalSession session) {
        if (session.getEmulator() != null) profile.transcript = WorkspaceStore.tail(session.getEmulator().getScreen().getTranscriptText(), WorkspaceStore.MAX_TRANSCRIPT);
        // Keep the configured directory; changing cd in a running shell does not overwrite profile settings.
    }
    public void capture() {
        if (closed) return;
        boolean changed = false;
        for (WorkspaceStore.Session profile : store.sessions) {
            TerminalSession live = liveFor(profile);
            if (live != null) { captureOne(profile, live); changed = true; }
        }
        if (changed) store.save();
    }
    public boolean onBack() {
        if (screen == Screen.HOME) return false;
        show(Screen.HOME, true); return true;
    }
    public void close() {
        capture(); closed = true; handler.removeCallbacks(snapshot); reader.shutdown(); store.close();
    }
}
