package com.example.navmap;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;


@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 设置ContentView
        View view = View.inflate(this, R.layout.activity_splash, null);
        setContentView(view);

        // 延迟1秒后启动MainActivity
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startMainActivity();
            }
        }, 1000); // 1000毫秒 = 1秒
    }

    private void startMainActivity() {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);//启动首页
        finish();//结束启动页
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out); // 设置淡入淡出动画
    }
}
