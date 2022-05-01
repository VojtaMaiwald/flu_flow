package com.mai0042.flu_flow;

import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import androidx.annotation.NonNull;
import android.widget.Toast;

import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.android.FlutterView;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugins.GeneratedPluginRegistrant;

public class MainActivity extends FlutterActivity {
    private static final String PLATFORM_CHANNEL = "com.mai0042.flu_flow/channel";

    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
        GeneratedPluginRegistrant.registerWith(flutterEngine);
        //super.configureFlutterEngine(flutterEngine);
        new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), PLATFORM_CHANNEL).setMethodCallHandler(
                (call, result) -> {
                    if (call.method.equals("getFaceRecognition")) {
                        String emoji = getFaceRecognition();

                        if (emoji != null && !emoji.isEmpty()) {
                            result.success(emoji);
                        }
                        else {
                            result.error("ERROR", "Emoji unavailable", null);
                        }
                    }
                    else {
                        result.notImplemented();
                    }

                    Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                    startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
                }
        );
    }
    
    private String getFaceRecognition() {
        Toast.makeText(this, "Emoji sent", Toast.LENGTH_LONG).show();
        return ":D";
    }
}
