package com.termux.app.workspace;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BooleanSupplier;

/** The app tracks one Ubuntu AI update operation at a time, including with its dialog closed. */
final class AgentCliUpdateUi {
    interface Runner {
        AgentCliUpdate.Result run(Context context, AgentCliUpdate.Agent agent, String expectedVersion);
    }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor();
    private static final ExecutorService RUN_CHECK_WORKER = Executors.newSingleThreadExecutor();
    private static final Map<AgentCliUpdate.Agent, Job> JOBS = new EnumMap<>(AgentCliUpdate.Agent.class);
    private static final Map<AgentCliUpdate.Agent, String> INSTALLED = new EnumMap<>(AgentCliUpdate.Agent.class);
    private static final Map<String, RunCheck> RUN_CHECKS = new HashMap<>();
    private static final Set<AgentCliUpdateUi> BADGE_OBSERVERS = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Set<Runnable> OBSERVERS = new LinkedHashSet<>();
    private static Job active;

    private static final class Job {
        final AgentCliUpdate.Target target;
        final AgentCliUpdate.Result checked;
        AgentCliUpdate.Result result;

        Job(AgentCliUpdate.Target target, AgentCliUpdate.Result checked) {
            this.target = target;
            this.checked = checked;
        }
    }

    private static final class RunCheck {
        final Job job;
        volatile boolean superseded;

        RunCheck(AgentCliUpdate.Target target) {
            job = new Job(target, null);
        }
    }

    private final Activity activity;
    private final Runner runner, runChecker;
    private final Runnable observer = this::render;
    private Runnable badgeChanged;
    private AlertDialog dialog, confirmation;
    private AgentCliUpdate.Target target;
    private BooleanSupplier eligible;

    AgentCliUpdateUi(Activity activity) {
        this(activity, AgentCliUpdate::run, AgentCliUpdate::checkPrepared);
    }

    AgentCliUpdateUi(Activity activity, Runner runner) {
        this(activity, runner, runner);
    }

    private AgentCliUpdateUi(Activity activity, Runner runner, Runner runChecker) {
        this.activity = activity;
        this.runner = runner;
        this.runChecker = runChecker;
    }

    String label() {
        return tr("Обновить CLI", "Update CLI");
    }

    // Invoked on the main thread once the owning terminal's preparation is ready.
    void checkForRun(WorkspaceStore.Session profile, Runnable badgeChanged) {
        AgentCliUpdate.Target captured = AgentCliUpdate.capture(profile);
        if (captured == null || captured.runId == null || captured.runId.isEmpty()) return;
        this.badgeChanged = badgeChanged;
        BADGE_OBSERVERS.add(this);
        RunCheck previous = RUN_CHECKS.get(captured.profileId);
        if (previous != null && previous.job.target.sameRun(captured)) return;
        forgetProfile(captured.profileId);
        RunCheck check = new RunCheck(captured);
        RUN_CHECKS.put(captured.profileId, check);
        Context context = activity.getApplicationContext();
        Runner checker = runChecker;
        RUN_CHECK_WORKER.execute(() -> {
            if (check.superseded) return;
            AgentCliUpdate.Result result = checker.run(context, captured.agent, null);
            MAIN.post(() -> {
                if (check.superseded) return;
                check.job.result = result;
                if (availableCheck(captured) != null) {
                    updateBadges();
                    changed();
                }
            });
        });
    }

    void forgetProfile(String profileId) {
        RunCheck previous = RUN_CHECKS.remove(profileId);
        if (previous != null) previous.superseded = true;
    }

    boolean hasRunUpdate(WorkspaceStore.Session profile) {
        return availableCheck(AgentCliUpdate.capture(profile)) != null;
    }

    private static Job availableCheck(AgentCliUpdate.Target captured) {
        if (captured == null) return null;
        RunCheck check = RUN_CHECKS.get(captured.profileId);
        if (check == null || !check.job.target.sameRun(captured)) return null;
        AgentCliUpdate.Result result = check.job.result;
        if (result == null || result.status != AgentCliUpdate.Status.AVAILABLE
            || !AgentCliUpdate.newerStableVersion(result.latest, result.current)) return null;
        String installed = INSTALLED.get(captured.agent);
        return installed == null || AgentCliUpdate.newerStableVersion(result.latest, installed) ? check.job : null;
    }

    void show(AgentCliUpdate.Target captured, BooleanSupplier stillEligible) {
        close();
        target = captured;
        eligible = stillEligible;
        AlertDialog opened = WorkspaceDialogs.builder(activity)
            .setTitle(label()).setMessage(identity())
            .setPositiveButton(tr("Установить…", "Install…"), null)
            .setNeutralButton(tr("Проверить", "Check"), null)
            .setNegativeButton(tr("Закрыть", "Close"), null).create();
        dialog = opened;
        opened.setOnDismissListener(d -> {
            if (dialog == opened) close();
        });
        opened.show();
        opened.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> start(null));
        opened.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> confirm());
        OBSERVERS.add(observer);
        render();
    }

    void close() {
        OBSERVERS.remove(observer);
        if (confirmation != null) {
            confirmation.dismiss();
            confirmation = null;
        }
        AlertDialog previous = dialog;
        dialog = null;
        target = null;
        eligible = null;
        if (previous != null) previous.dismiss();
    }

    private void confirm() {
        Job checked = dialogJob();
        if (!canInstall(checked) || confirmation != null) return;
        AlertDialog prompt = WorkspaceDialogs.builder(activity)
            .setTitle(tr("Обновить CLI?", "Update CLI?"))
            .setMessage(versions(checked.result) + "\n\n" + tr(
                "Обновление действует на все чаты этого агента в Ubuntu AI. Новая версия будет использована при следующем запуске чата.",
                "The update applies to all chats for this agent in Ubuntu AI. They will use the new version on their next launch."))
            .setNegativeButton(tr("Отмена", "Cancel"), null)
            .setPositiveButton(tr("Установить", "Install"), (d, w) -> {
                if (dialog != null && dialogJob() == checked && canInstall(checked))
                    start(checked.result);
            }).create();
        confirmation = prompt;
        prompt.setOnDismissListener(d -> {
            if (confirmation == prompt) confirmation = null;
        });
        prompt.show();
    }

    private boolean canInstall(Job job) {
        return active == null && job != null && job.checked == null && job.result != null
            && job.result.status == AgentCliUpdate.Status.AVAILABLE
            && target.profileId.equals(job.target.profileId) && eligible.getAsBoolean();
    }

    private void start(AgentCliUpdate.Result checked) {
        if (dialog == null || active != null || !eligible.getAsBoolean()) return;
        Context context = activity.getApplicationContext();
        Job job = new Job(target, checked);
        active = job;
        JOBS.put(target.agent, job);
        changed();
        run(context, job, runner);
    }

    private static void run(Context context, Job job, Runner runner) {
        WORKER.execute(() -> {
            AgentCliUpdate.Result result = runner.run(context, job.target.agent,
                job.checked == null ? null : job.checked.latest);
            MAIN.post(() -> {
                job.result = result;
                active = null;
                if (result.status == AgentCliUpdate.Status.INSTALLED) {
                    INSTALLED.put(job.target.agent, result.latest);
                    updateBadges();
                }
                changed();
            });
        });
    }

    private static void changed() {
        for (Runnable observer : new ArrayList<>(OBSERVERS)) observer.run();
    }

    private static void updateBadges() {
        for (AgentCliUpdateUi ui : new ArrayList<>(BADGE_OBSERVERS)) ui.badgeChanged.run();
    }

    private Job dialogJob() {
        Job explicit = JOBS.get(target.agent);
        Job automatic = availableCheck(target);
        if (automatic != null && (explicit == null
            || explicit != active && !target.sameRun(explicit.target))) return automatic;
        if (explicit != null && (explicit.checked != null || active == explicit
            || target.profileId.equals(explicit.target.profileId))) return explicit;
        return automatic;
    }

    private void render() {
        if (dialog == null || !dialog.isShowing()) return;
        Job job = dialogJob();
        String message = versions(null);
        if (!eligible.getAsBoolean()) {
            message += "\n\n" + tr("Профиль чата изменился или недоступен.", "The chat profile changed or is unavailable.");
        } else if (active != null && active != job) {
            message += "\n\n" + tr("Дождитесь завершения другой операции с CLI.", "Wait for the other CLI operation to finish.");
        } else if (job != null) {
            AgentCliUpdate.Result result = job.result;
            message = versions(result == null ? job.checked : result.error == null ? result : null);
            if (result == null) {
                message += "\n\n" + (job.checked == null ? tr("Проверяем версии…", "Checking versions…")
                    : tr("Устанавливаем обновление…", "Installing the update…"));
            } else {
                switch (result.status) {
                    case AVAILABLE:
                        message += "\n\n" + tr("Доступна новая версия.", "An update is available.");
                        break;
                    case CURRENT:
                        message += "\n\n" + tr("Обновление не требуется.", "The CLI is up to date.");
                        break;
                    case INSTALLED:
                        message += "\n\n" + tr(
                            "Обновление установлено. Новая версия используется при следующем запуске чата. Открытый чат можно перезапустить кнопкой в верхней панели.",
                            "Update installed. The new version takes effect on the next chat launch. Use the restart button in the top bar for an open chat.");
                        break;
                    case FAILED:
                        message += "\n\n" + error(result.error);
                        break;
                }
            }
        }
        dialog.setMessage(message);
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(active == null && eligible.getAsBoolean());
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(canInstall(job));
    }

    private String identity() {
        return target.agent.label + " · " + AgentCliUpdate.environmentLabel();
    }

    private String versions(AgentCliUpdate.Result result) {
        String current = result == null ? "—" : result.current;
        if ("none".equals(current)) current = tr("не найдена", "unavailable");
        return identity() + "\n\n" + tr("Версия для запуска: ", "Launch version: ") + current
            + "\n" + tr("Последняя версия: ", "Latest version: ") + (result == null ? "—" : result.latest);
    }

    private String error(AgentCliUpdate.Error error) {
        switch (error) {
            case UNSUPPORTED_ENVIRONMENT:
            case DEPENDENCIES:
                return tr("Не удалось открыть Ubuntu AI или найти компоненты обновления. Проверьте среду запуска агента.",
                    "Could not open Ubuntu AI or find its update tools. Check the agent’s launch environment.");
            case REGISTRY:
                return tr("Не удалось проверить реестр пакетов. Проверьте соединение и повторите.",
                    "Could not check the package registry. Check your connection and retry.");
            case PACKAGE:
            case VERIFY:
                return tr("Пакет CLI не прошёл проверку. Повторите проверку версии.", "CLI package verification failed. Check the version again.");
            case VERSION_CHANGED:
                return tr("Последняя версия изменилась. Проверьте её ещё раз.", "The latest version changed. Check again.");
            case BUSY:
                return tr("Обновление уже выполняется. Повторите проверку позже.", "An update is already running. Check again later.");
            case TIMEOUT:
                return tr("Истекло время ожидания. Проверьте текущую версию.", "The operation timed out. Check the current version.");
            default:
                return tr("Операция с CLI не завершилась. Повторите проверку версии.", "The CLI operation could not be completed. Check the version again.");
        }
    }

    private String tr(String ru, String en) {
        return AgentPreparation.tr(activity, ru, en);
    }
}
