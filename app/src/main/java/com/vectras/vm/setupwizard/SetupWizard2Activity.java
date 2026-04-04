package com.vectras.vm.setupwizard;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.vectras.vm.main.MainActivity;

/**
 * Minimal bootstrap-compatible setup activity fallback.
 * Keeps external intent/API contract compile-stable while setup flow is rebuilt.
 */
public class SetupWizard2Activity extends AppCompatActivity {
    public static final String ACTION_DEBUG_PROOT_SELF_CHECK = "com.vectras.vm.action.DEBUG_PROOT_SELF_CHECK";
    public static final String EXTRA_DEBUG_PROOT_SELF_CHECK = "debug_proot_self_check";
    public static final int ACTION_SYSTEM_UPDATE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
