package com.example.animepahedl;

import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.io.FileWriter;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;

/** Device-test watchdog and scoped foreground delegation for aggressive OEM UID freezers. */
public class ThreadTraceListener extends RunListener {
    private Timer timer;
    private void foregroundDelegate(String action) {
        android.content.Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String command = "cmd activity set-foreground-service-delegate --user " + (context.getApplicationInfo().uid / 100000) + " " + context.getPackageName() + " " + action;
        try (android.os.ParcelFileDescriptor descriptor = InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command);
             java.io.InputStream input = new android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor)) {
            while (input.read() >= 0) { }
        } catch (java.io.IOException error) { throw new IllegalStateException("Could not configure device test lifetime", error); }
    }
    @Override public void testRunStarted(Description description) { foregroundDelegate("start"); }
    @Override public void testRunFinished(org.junit.runner.Result result) { foregroundDelegate("stop"); }

    @Override public void testStarted(final Description description) {
        String type = description.getClassName();
        boolean uiTest = type.endsWith("LibraryNavigationTest") || type.endsWith("DownloadSelectionTest") || type.endsWith("SearchScreenTest");
        if (uiTest) {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                for (androidx.test.runner.lifecycle.Stage stage : androidx.test.runner.lifecycle.Stage.values()) {
                    if (stage == androidx.test.runner.lifecycle.Stage.DESTROYED) continue;
                    for (android.app.Activity activity : new java.util.ArrayList<>(androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(stage))) {
                        if (activity instanceof MainActivity) activity.finish();
                    }
                }
            });
        } else {
            android.content.Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            context.startActivity(new android.content.Intent(context, MainActivity.class).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP));
        }
        timer = new Timer(true);
        timer.schedule(new TimerTask() {
            @Override public void run() {
                File target = new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir(), "device-test-stacks.txt");
                try (FileWriter out = new FileWriter(target)) {
                    out.write(description.toString() + "\n");
                    for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
                        out.write("\n" + entry.getKey().getName() + " " + entry.getKey().getState() + "\n");
                        for (StackTraceElement frame : entry.getValue()) out.write(frame.toString() + "\n");
                    }
                } catch (Exception ignored) { }
            }
        }, 8000, 8000);
    }
    @Override public void testFinished(Description description) { if (timer != null) timer.cancel(); }
}
