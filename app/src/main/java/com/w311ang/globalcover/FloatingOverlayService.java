package com.w311ang.globalcover;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.ImageView;

public class FloatingOverlayService extends Service {

    private WindowManager windowManager;
    private ImageView overlayImageView;
    private WindowManager.LayoutParams params;
    private final IBinder binder = new LocalBinder();

    // 定义一个内部 Binder 类，供外部组件绑定后调用 Service 的方法
    public class LocalBinder extends Binder {
        public FloatingOverlayService getService() {
            return FloatingOverlayService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // 创建 ImageView，并设置默认图片与透明度
        overlayImageView = new ImageView(this);
        // 注意：确保在 drawable 目录下有 default_image 图片资源，也可以自行更换为其他默认图片
        //overlayImageView.setImageResource(R.drawable.default_image);
        overlayImageView.setAlpha(0.5f);  // 默认50%透明度

        // 根据 Android 版本选择悬浮窗类型
        int layoutFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }

        // 设置 LayoutParams，不接收触摸与焦点事件
        params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP;

        // 添加悬浮窗到 WindowManager
        windowManager.addView(overlayImageView, params);
    }

    /**
     * 更新显示图片
     * @param bitmap 新的图片 Bitmap
     */
    public void updateImage(Bitmap bitmap) {
        if (overlayImageView != null && bitmap != null) {
            overlayImageView.setImageBitmap(bitmap);
        }
    }

    /**
     * 更新图片透明度，alpha 范围为 [0,1]
     * @param alpha 透明度值
     */
    public void updateAlpha(float alpha) {
        if (overlayImageView != null) {
            overlayImageView.setAlpha(alpha);
        }
    }

    /**
     * 设置图片是否拉伸以覆盖整个屏幕
     * @param stretch true 为非等比例拉伸（直接拉伸到屏幕大小），false 为等比例拉伸（保持图片比例且完整显示）
     */
    public void setImageStretch(boolean stretch) {
        if (overlayImageView != null) {
            if (stretch) {
                // 拉伸到整个屏幕，可能会改变比例
                overlayImageView.setScaleType(ImageView.ScaleType.FIT_XY);
            } else {
                // 默认行为：FIT_CENTER，保持图片比例且完整显示
                overlayImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Service 销毁时移除悬浮窗
        if (overlayImageView != null) {
            windowManager.removeView(overlayImageView);
        }
    }
}
