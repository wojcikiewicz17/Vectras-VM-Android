package com.vectras.vm;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

        binding.done.setOnClickListener(v -> {
            result = binding.edittext1.getText().toString();
            finish();
        });

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            binding.edittext1.requestFocus();
            binding.edittext1.setSelection(binding.edittext1.getText().length());
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.showSoftInput(binding.edittext1, InputMethodManager.SHOW_IMPLICIT);
        }, 200);
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
