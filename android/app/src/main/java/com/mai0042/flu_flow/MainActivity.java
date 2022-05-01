package com.mai0042.flu_flow;

import android.app.Activity;
import android.content.Intent;
import androidx.annotation.NonNull;

import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugins.GeneratedPluginRegistrant;
import io.flutter.plugin.common.MethodChannel.Result;

public class MainActivity extends FlutterActivity {
    private static final String PLATFORM_CHANNEL = "com.mai0042.flu_flow/channel";
    private static int LAUNCH_CAMERA_ACTIVITY = 1234;
    private Result result;

    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
        GeneratedPluginRegistrant.registerWith(flutterEngine);
        //super.configureFlutterEngine(flutterEngine);
        new MethodChannel(flutterEngine.getDartExecutor(), PLATFORM_CHANNEL).setMethodCallHandler(
                (call, result) -> {

                    if (call.method.equals("getFaceRecognition")) {
                        this.result = result;
                        Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                        startActivityForResult(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP), LAUNCH_CAMERA_ACTIVITY);
                    }
                    else {
                        result.notImplemented();
                    }
                }
        );
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == LAUNCH_CAMERA_ACTIVITY) {
            if (resultCode == Activity.RESULT_OK){
                int emojiIndex = data.getIntExtra("emojiIndex", -1);
                if (emojiIndex != -1) {
                    this.result.success(emojiIndex);
                }
                else {
                    this.result.error("UNAVAILABLE", "Face detection not available.", null);
                }
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                this.result.error("RESULT_CANCELED", "Activity result is not valid", null);
            }
        }
    }
}
