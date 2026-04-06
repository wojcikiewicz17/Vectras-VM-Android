package com.vectras.vm;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;


import androidx.appcompat.app.AppCompatActivity;

import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.databinding.ActivityQemuParamsEditorBinding;
import com.vectras.vm.rafaelia.RafaeliaQemuProfile;
import com.vectras.vm.utils.DeviceUtils;
import com.vectras.vm.utils.UIUtils;

import java.util.ArrayList;
import java.util.List;

public class QemuParamsEditorActivity extends AppCompatActivity {

    ActivityQemuParamsEditorBinding binding;
    public static String result = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UIUtils.edgeToEdge(this);
        binding = ActivityQemuParamsEditorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UIUtils.setOnApplyWindowInsetsListener(findViewById(R.id.main));
        binding.appbar.post(() -> binding.appbar.setExpanded(false, false));
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        if (getIntent().hasExtra("content")) {
            result = getIntent().getStringExtra("content");
            binding.edittext1.setText(result);
        }

        setupPresetDropdown();
        setupEditorTools();
        updateParamsAnalysis(binding.edittext1.getText().toString());

        binding.done.setOnClickListener(v -> {
            result = binding.edittext1.getText().toString();
            finish();
        });

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            binding.edittext1.requestFocus();
            binding.edittext1.setSelection(binding.edittext1.getText().length());
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(binding.edittext1, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 200);
    }


    private void setupEditorTools() {
        binding.btnNormalizeArgs.setOnClickListener(v -> {
            String normalized = normalizeArgs(binding.edittext1.getText().toString());
            binding.edittext1.setText(normalized);
            binding.edittext1.setSelection(normalized.length());
        });

        binding.btnAssistBase.setOnClickListener(v -> applyAssistantSnippet("-cpu max -m 4096 -smp cpus=4"));
        binding.btnAssistIo.setOnClickListener(v -> applyAssistantSnippet("-object iothread,id=ioth0 -device virtio-scsi-pci,id=scsi0,iothread=ioth0 -drive if=virtio,cache=writeback,aio=threads"));
        binding.btnAssistQmp.setOnClickListener(v -> applyAssistantSnippet("-qmp unix:/data/data/com.vectras.vm/cache/vm/qmpsocket,server,nowait -d trace"));

        binding.edittext1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateParamsAnalysis(s == null ? "" : s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    private void updateParamsAnalysis(String args) {
        String safe = args == null ? "" : args.trim();
        if (safe.isEmpty()) {
            binding.tvParamAnalysis.setText(getString(R.string.qemu_params_analysis_empty));
            return;
        }

        String[] tokens = safe.split("\\s+");
        int tokenCount = 0;
        int switches = 0;
        for (String t : tokens) {
            if (t == null || t.isEmpty()) continue;
            tokenCount++;
            if (t.startsWith("-")) switches++;
        }

        boolean hasQmp = hasFlag(tokens, "-qmp") || safe.contains("qmpsocket");
        boolean hasAccel = hasFlag(tokens, "-accel") || containsAccelToken(tokens);
        boolean hasCpu = hasFlag(tokens, "-cpu");
        boolean hasMem = hasFlag(tokens, "-m");
        boolean hasSmp = hasFlag(tokens, "-smp");
        boolean hasDrive = hasFlag(tokens, "-drive") || hasFlag(tokens, "-hda") || hasFlag(tokens, "-hdb") || hasFlag(tokens, "-cdrom");

        String analysis = getString(R.string.qemu_params_analysis_template,
                tokenCount,
                switches,
                safe.length(),
                hasCpu ? "✓" : "—",
                hasMem ? "✓" : "—",
                hasSmp ? "✓" : "—",
                hasDrive ? "✓" : "—",
                hasQmp ? "✓" : "—",
                hasAccel ? "✓" : "—");
        binding.tvParamAnalysis.setText(analysis);
    }

    private boolean containsAccelToken(String[] tokens) {
        if (tokens == null) return false;
        for (String token : tokens) {
            if (token == null) continue;
            String lower = token.toLowerCase();
            if ("kvm".equals(lower)
                    || lower.startsWith("kvm,")
                    || "tcg".equals(lower)
                    || lower.startsWith("tcg,")
                    || "hvf".equals(lower)
                    || lower.startsWith("hvf,")
                    || "whpx".equals(lower)
                    || lower.startsWith("whpx,")) {
                return true;
            }
        }
        return false;
    }

    private boolean hasFlag(String[] tokens, String flag) {
        if (tokens == null || flag == null || flag.isEmpty()) return false;

        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            if (token == null || token.isEmpty()) continue;

            if (token.equals(flag)) {
                return true;
            }

            if (token.startsWith(flag + "=")) {
                return true;
            }

            if (flag.length() == 2 && token.startsWith(flag) && token.length() > flag.length()) {
                char next = token.charAt(flag.length());
                if (Character.isDigit(next)) {
                    return true;
                }
            }
        }
        return false;
    }


    private void applyAssistantSnippet(String snippet) {
        String current = normalizeArgs(binding.edittext1.getText().toString());
        String addon = normalizeArgs(snippet);
        if (addon.isEmpty()) return;

        String updated;
        if (current.isEmpty()) {
            updated = addon;
        } else if (current.contains(addon)) {
            updated = current;
        } else {
            updated = current + " " + addon;
        }

        binding.edittext1.setText(updated);
        binding.edittext1.setSelection(updated.length());
    }

    private String normalizeArgs(String args) {
        if (args == null) return "";
        String trimmed = args.trim();
        if (trimmed.isEmpty()) return "";
        return trimmed.replaceAll("\\s+", " ");
    }

    private void setupPresetDropdown() {
        List<PresetOption> options = new ArrayList<>();
        boolean is64bit = DeviceUtils.is64bit();
        String arch = MainSettingsManager.getArch(this);
        options.add(new PresetOption(
                getString(R.string.qemu_preset_balanced),
                RafaeliaQemuProfile.presetParams(is64bit, arch, RafaeliaQemuProfile.PresetKind.BALANCED)
        ));
        options.add(new PresetOption(
                getString(R.string.qemu_preset_performance),
                RafaeliaQemuProfile.presetParams(is64bit, arch, RafaeliaQemuProfile.PresetKind.PERFORMANCE)
        ));
        options.add(new PresetOption(
                getString(R.string.qemu_preset_compatibility),
                RafaeliaQemuProfile.presetParams(is64bit, arch, RafaeliaQemuProfile.PresetKind.COMPATIBILITY)
        ));

        String[] labels = new String[options.size()];
        for (int i = 0; i < options.size(); i++) {
            labels[i] = options.get(i).label;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels);
        binding.qemuPresetInput.setAdapter(adapter);
        binding.qemuPresetInput.setOnItemClickListener((parent, view, position, id) -> {
            PresetOption preset = options.get(position);
            binding.edittext1.setText(preset.params);
            binding.edittext1.setSelection(binding.edittext1.getText().length());
        });
    }

    private static class PresetOption {
        private final String label;
        private final String params;

        private PresetOption(String label, String params) {
            this.label = label;
            this.params = params;
        }
    }
}
