package com.termux.app.workspace;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.os.Looper;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = Application.class)
public class AgentCliUpdateUiTest {
    @Test public void automaticCheckUsesTheRunIdentityAndIgnoresSupersededResults() throws Exception {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        WorkspaceStore.Session profile = new WorkspaceStore.Session();
        profile.environment = "ubuntu";
        profile.codexEnvironment = "ubuntu-ai";
        profile.geminiAgent = true;
        profile.geminiAuth = GeminiCli.ANTIGRAVITY;
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger checks = new AtomicInteger(), badges = new AtomicInteger();
        AgentCliUpdateUi ui = new AgentCliUpdateUi(activity, (context, agent, version) -> {
            assertEquals(AgentCliUpdate.Agent.ANTIGRAVITY, agent);
            if (version != null) {
                return AgentCliUpdate.parse("CURRENT:1.5.14\nLATEST:1.5.14\nRESULT:installed\n", 0, version);
            }
            int check = checks.incrementAndGet();
            if (check == 1) {
                entered.countDown();
                try {
                    if (!release.await(5, TimeUnit.SECONDS)) throw new AssertionError("Probe fixture timed out");
                } catch (InterruptedException error) {
                    throw new AssertionError(error);
                }
            }
            return AgentCliUpdate.parse("CURRENT:1.5.13\nLATEST:1.5.14\nRESULT:available\n", 0, null);
        });
        try {
            ui.checkForRun(profile, () -> fail("Superseded run refreshed the badge"));
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            ui.checkForRun(profile, () -> fail("Repeated refresh restarted the probe"));
            assertFalse(ui.hasRunUpdate(profile));
            profile.geminiSocketId = java.util.UUID.randomUUID().toString();
            ui.checkForRun(profile, badges::incrementAndGet);
            release.countDown();
            waitUntil(() -> badges.get() == 1);
            assertTrue(ui.hasRunUpdate(profile));
            ui.checkForRun(profile, badges::incrementAndGet);
            assertEquals(2, checks.get());
            ui.show(AgentCliUpdate.capture(profile), () -> true);
            assertTrue(latestDialog().getButton(AlertDialog.BUTTON_POSITIVE).isEnabled());
            latestDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            latestDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            waitUntil(() -> badges.get() == 2);
            assertFalse(ui.hasRunUpdate(profile));
            ui.forgetProfile(profile.id);
            assertFalse(ui.hasRunUpdate(profile));
        } finally {
            release.countDown();
            ui.forgetProfile(profile.id);
            ui.close();
            activity.finish();
            idle();
        }
    }

    @Test public void consentIsBoundToTheProfileAndOneInstallSurvivesClosingItsDialog() throws Exception {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        WorkspaceLanguage.select(activity, "en");
        WorkspaceStore.Session profile = new WorkspaceStore.Session();
        profile.environment = "codex";
        profile.codexEnvironment = "ubuntu-ai";
        AgentCliUpdate.Target target = AgentCliUpdate.capture(profile);
        AtomicInteger installs = new AtomicInteger();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AgentCliUpdateUi ui = new AgentCliUpdateUi(activity, (context, agent, version) -> {
            if (version == null) {
                return AgentCliUpdate.parse("CURRENT:0.163.0\nLATEST:0.171.0\nRESULT:available\n", 0, null);
            }
            installs.incrementAndGet();
            entered.countDown();
            try {
                if (release.await(5, TimeUnit.SECONDS) && agent == AgentCliUpdate.Agent.CODEX) {
                    return AgentCliUpdate.parse("CURRENT:0.171.0\nLATEST:0.171.0\nRESULT:installed\n", 0, version);
                }
            } catch (InterruptedException interruption) {
                Thread.currentThread().interrupt();
            }
            return AgentCliUpdate.Result.failed(AgentCliUpdate.Error.TRANSPORT);
        });
        try {
            ui.show(target, () -> target.matches(profile));
            AlertDialog dialog = latestDialog();
            assertFalse(dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled());
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).performClick();
            waitUntilEnabled(dialog, AlertDialog.BUTTON_POSITIVE);
            assertTrue(message(dialog).contains("Launch version: 0.163.0"));
            assertTrue(message(dialog).contains("Latest version: 0.171.0"));

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            latestDialog().getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
            idle();
            assertEquals(0, installs.get());

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            profile.codexEnvironment = "ubuntu";
            latestDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            idle();
            assertEquals(0, installs.get());

            profile.codexEnvironment = "ubuntu-ai";
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            latestDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            idle();
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            ui.close();
            ui.show(target, () -> target.matches(profile));
            AlertDialog reopened = latestDialog();
            assertFalse(reopened.getButton(AlertDialog.BUTTON_NEUTRAL).isEnabled());
            assertFalse(reopened.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled());
            assertTrue(message(reopened).contains("Installing the update"));
            release.countDown();
            waitUntilEnabled(reopened, AlertDialog.BUTTON_NEUTRAL);
            assertTrue(message(reopened).contains("Launch version: 0.171.0"));
            assertTrue(message(reopened).contains("restart button in the top bar"));
            assertEquals(1, installs.get());
        } finally {
            release.countDown();
            ui.close();
            activity.finish();
            idle();
        }
    }

    @Test public void aRegistryFailureCanBeRetriedInRussian() throws Exception {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        WorkspaceLanguage.select(activity, "ru");
        WorkspaceStore.Session profile = new WorkspaceStore.Session();
        profile.environment = "ubuntu";
        profile.codexEnvironment = "ubuntu-ai";
        profile.claudeAgent = true;
        AgentCliUpdate.Target target = AgentCliUpdate.capture(profile);
        AtomicInteger checks = new AtomicInteger();
        AgentCliUpdateUi ui = new AgentCliUpdateUi(activity, (context, agent, version) -> {
            if (checks.incrementAndGet() == 1) return AgentCliUpdate.Result.failed(AgentCliUpdate.Error.REGISTRY);
            return AgentCliUpdate.parse("CURRENT:2.1.289\nLATEST:2.1.290\nRESULT:available\n", 0, null);
        });
        try {
            ui.show(target, () -> target.matches(profile));
            AlertDialog dialog = latestDialog();
            assertEquals("Обновить CLI", Shadows.shadowOf(dialog).getTitle());
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).performClick();
            waitUntilEnabled(dialog, AlertDialog.BUTTON_NEUTRAL);
            assertFalse(dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled());
            assertTrue(message(dialog).contains("Не удалось проверить реестр пакетов"));
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).performClick();
            waitUntilEnabled(dialog, AlertDialog.BUTTON_POSITIVE);
            assertTrue(message(dialog).contains("Последняя версия: 2.1.290"));
            assertEquals(2, checks.get());
        } finally {
            ui.close();
            activity.finish();
            idle();
        }
    }

    private static AlertDialog latestDialog() {
        return ShadowAlertDialog.getLatestAlertDialog();
    }

    private static String message(AlertDialog dialog) {
        return Shadows.shadowOf(dialog).getMessage().toString();
    }

    private static void idle() {
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }

    private static void waitUntilEnabled(AlertDialog dialog, int button) throws InterruptedException {
        waitUntil(() -> dialog.getButton(button).isEnabled());
    }

    private static void waitUntil(BooleanSupplier done) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        do {
            idle();
            if (done.getAsBoolean()) return;
            Thread.sleep(10);
        } while (System.nanoTime() < deadline);
        fail("CLI operation did not update the dialog buttons");
    }
}
