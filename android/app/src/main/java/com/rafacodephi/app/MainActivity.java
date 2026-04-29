package com.rafacodephi.app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity {
    static {
        System.loadLibrary("rafcoder_jni");
    }

    public native String nativeMessage();
    public native String nativeSectorSummary();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView tv = new TextView(this);
        tv.setText(nativeMessage() + "\n" + nativeSectorSummary());
        setContentView(tv);
    }
}
