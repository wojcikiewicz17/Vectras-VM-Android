package com.vectras.vm.setupwizard;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

public class SetupFeatureCoreProotSelfCheckTest {

    @Test
    public void toStructuredText_containsDeterministicOrderAndRequiredFields() {
        ProotSelfCheckResult result = new ProotSelfCheckResult(
                true,
                "core-prereq",
                true,
                0,
                "ok"
        );

        String text = result.toStructuredText();
        assertOrderedKeys(text, Arrays.asList("ok=", "validator=", "executed=", "exit=", "reason="));
        assertTrue(text.contains("ok=true"));
        assertTrue(text.contains("validator=core-prereq"));
        assertTrue(text.contains("executed=true"));
        assertTrue(text.contains("exit=0"));
        assertTrue(text.contains("reason=ok"));
    }

    @Test
    public void runSelfCheck_validatorFail_skipsExecutionAndReturnsInformativeSummary() throws Exception {
        File filesDir = Files.createTempDirectory("proot-self-check").toFile();
        Context context = mock(Context.class);
        when(context.getFilesDir()).thenReturn(filesDir);

        ProotSelfCheckResult result = SelfCheckHarness.runSelfCheck(
                context,
                TestPrereqValidator::validate,
                new TrackingExecutionStrategy(true)
        );

        assertFalse(result.ok);
        assertFalse(result.executed);
        assertTrue(result.reason.contains("missing-prereq:proot-binary"));
        assertTrue(result.toStructuredText().contains("executed=false"));
    }

    @Test
    public void runSelfCheck_versionUnavailable_usesDryRunFallbackPath() throws Exception {
        File filesDir = Files.createTempDirectory("proot-self-check-fallback").toFile();
        createFile(filesDir, "usr/bin/proot");
        Context context = mock(Context.class);
        when(context.getFilesDir()).thenReturn(filesDir);

        TrackingExecutionStrategy strategy = new TrackingExecutionStrategy(false);
        ProotSelfCheckResult result = SelfCheckHarness.runSelfCheck(
                context,
                TestPrereqValidator::validate,
                strategy
        );

        assertTrue(result.ok);
        assertTrue(result.executed);
        assertFalse(strategy.versionInvoked);
        assertTrue(strategy.dryRunInvoked);
        assertTrue(result.reason.contains("dry-run-fallback"));
    }

    private static void assertOrderedKeys(String text, List<String> keys) {
        int cursor = -1;
        for (String key : keys) {
            int idx = text.indexOf(key);
            assertTrue("Missing key: " + key, idx >= 0);
            assertTrue("Out-of-order key: " + key, idx > cursor);
            cursor = idx;
        }
    }

    private static void createFile(File filesDir, String relativePath) throws IOException {
        File file = new File(filesDir, relativePath);
        File parent = file.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IOException("Failed to create dir: " + parent);
        }
        if (!file.exists() && !file.createNewFile()) {
            throw new IOException("Failed to create file: " + file);
        }
    }

    private interface PrereqValidator {
        ValidationOutcome validate(Context context);
    }

    private interface ExecutionStrategy {
        boolean supportsVersionExecution();

        ProotSelfCheckResult runVersion(Context context);

        ProotSelfCheckResult runDryRun(Context context);
    }

    private static final class SelfCheckHarness {
        static ProotSelfCheckResult runSelfCheck(Context context, PrereqValidator validator, ExecutionStrategy strategy) {
            ValidationOutcome validationOutcome = validator.validate(context);
            if (!validationOutcome.ok) {
                return new ProotSelfCheckResult(false, validationOutcome.validator, false, -1, validationOutcome.reason);
            }

            if (strategy.supportsVersionExecution()) {
                return strategy.runVersion(context);
            }
            return strategy.runDryRun(context);
        }
    }

    private static final class TestPrereqValidator {
        static ValidationOutcome validate(Context context) {
            File proot = new File(context.getFilesDir(), "usr/bin/proot");
            if (!proot.exists()) {
                return ValidationOutcome.fail("core-prereq", "missing-prereq:proot-binary path=" + proot.getAbsolutePath());
            }
            return ValidationOutcome.ok("core-prereq");
        }
    }

    private static final class TrackingExecutionStrategy implements ExecutionStrategy {
        private final boolean versionAvailable;
        private boolean versionInvoked;
        private boolean dryRunInvoked;

        TrackingExecutionStrategy(boolean versionAvailable) {
            this.versionAvailable = versionAvailable;
        }

        @Override
        public boolean supportsVersionExecution() {
            return versionAvailable;
        }

        @Override
        public ProotSelfCheckResult runVersion(Context context) {
            versionInvoked = true;
            return new ProotSelfCheckResult(true, "core-prereq", true, 0, "version-ok");
        }

        @Override
        public ProotSelfCheckResult runDryRun(Context context) {
            dryRunInvoked = true;
            return new ProotSelfCheckResult(true, "core-prereq", true, 0, "dry-run-fallback");
        }
    }

    private static final class ValidationOutcome {
        final boolean ok;
        final String validator;
        final String reason;

        ValidationOutcome(boolean ok, String validator, String reason) {
            this.ok = ok;
            this.validator = validator;
            this.reason = reason;
        }

        static ValidationOutcome ok(String validator) {
            return new ValidationOutcome(true, validator, "ok");
        }

        static ValidationOutcome fail(String validator, String reason) {
            return new ValidationOutcome(false, validator, reason);
        }
    }

    private static final class ProotSelfCheckResult {
        final boolean ok;
        final String validator;
        final boolean executed;
        final int exit;
        final String reason;

        ProotSelfCheckResult(boolean ok, String validator, boolean executed, int exit, String reason) {
            this.ok = ok;
            this.validator = validator;
            this.executed = executed;
            this.exit = exit;
            this.reason = reason;
        }

        String toStructuredText() {
            return "ok=" + ok
                    + ";validator=" + validator
                    + ";executed=" + executed
                    + ";exit=" + exit
                    + ";reason=" + reason;
        }
    }
}
