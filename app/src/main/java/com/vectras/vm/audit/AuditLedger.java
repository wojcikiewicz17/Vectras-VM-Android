package com.vectras.vm.audit;

import android.content.Context;

import com.vectras.vm.AppConfig;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.json.JSONObject;

public class AuditLedger {
    private static final long MAX_BYTES = 512 * 1024L;
    private static final String LEDGER_NAME = "audit-ledger.jsonl";
    private static final String LEDGER_ROTATED_NAME = "audit-ledger.prev.jsonl";

    public static synchronized void record(Context context, AuditEvent event) {
        try {
            File baseDir = context != null ? context.getFilesDir() : new File(AppConfig.internalDataDirPath);
            File ledger = new File(baseDir, LEDGER_NAME);
            rotateIfNeeded(ledger, new File(baseDir, LEDGER_ROTATED_NAME));
            try (FileWriter writer = new FileWriter(ledger, true)) {
                writer.write(event.toJsonLine());
                writer.write('\n');
            }
        } catch (IOException ignored) {
            // must never block main flow
        }
    }

    public static synchronized long readLastMonoTimestampForVm(Context context, String vmId) {
        try {
            File baseDir = context != null ? context.getFilesDir() : new File(AppConfig.internalDataDirPath);
            long fromCurrent = readLastMonoFromFile(new File(baseDir, LEDGER_NAME), vmId);
            if (fromCurrent > 0L) {
                return fromCurrent;
            }
            return readLastMonoFromFile(new File(baseDir, LEDGER_ROTATED_NAME), vmId);
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private static long readLastMonoFromFile(File file, String vmId) {
        if (file == null || !file.exists() || vmId == null || vmId.trim().isEmpty()) {
            return 0L;
        }
        long lastMono = 0L;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                JSONObject obj = new JSONObject(line);
                if (vmId.equals(obj.optString("vm_id", ""))) {
                    long mono = obj.optLong("ts_mono", 0L);
                    if (mono > lastMono) {
                        lastMono = mono;
                    }
                }
            }
        } catch (Exception ignored) {
            return 0L;
        }
        return lastMono;
    }

    private static void rotateIfNeeded(File current, File rotated) {
        if (!current.exists() || current.length() < MAX_BYTES) {
            return;
        }
        if (rotated.exists()) {
            //noinspection ResultOfMethodCallIgnored
            rotated.delete();
        }
        //noinspection ResultOfMethodCallIgnored
        current.renameTo(rotated);
    }
}
