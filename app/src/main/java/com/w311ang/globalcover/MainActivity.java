package com.w311ang.globalcover;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.provider.Settings;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.widget.Button;
import android.widget.SeekBar;
import android.view.View;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_IMAGE = 1001;
    private FloatingOverlayService floatingService;
    private boolean isBound = false;
    private SeekBar alphaSeekBar;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            FloatingOverlayService.LocalBinder binder = (FloatingOverlayService.LocalBinder) service;
            floatingService = binder.getService();
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 设置 Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // 检查悬浮窗权限（Android 6.0+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                           Uri.parse("package:" + getPackageName()));
                startActivity(intent);
                return;
            }
        }

        // 启动并绑定 FloatingOverlayService
        Intent serviceIntent = new Intent(this, FloatingOverlayService.class);
        startService(serviceIntent);
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);

        // 文件选择按钮，调用系统文件选择器选取图片
        Button selectImageButton = findViewById(R.id.btn_select_image);
        selectImageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openImageChooser();
                }
        });

        // SeekBar 用于调节图片透明度（0~100，转换为[0,1]）
        alphaSeekBar = findViewById(R.id.seekbar_alpha);
        alphaSeekBar.setProgress(50); // 默认50%透明度
        alphaSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    float alpha = progress / 100f;
                    if (isBound && floatingService != null) {
                        floatingService.updateAlpha(alpha);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {  }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {  }
            });
    }

    // 调用系统文件选择器
    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        // 可选：使用Intent.createChooser来显示选择器标题
        Intent chooser = Intent.createChooser(intent, "请选择图片");
        startActivityForResult(chooser, REQUEST_CODE_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                // 获取选中的图片 Bitmap
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                if (isBound && floatingService != null) {
                    floatingService.updateImage(bitmap);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(serviceConnection);
        }
    }
}
