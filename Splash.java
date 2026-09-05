package com.example.proteahealth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class Splash extends AppCompatActivity {

    private static final int SPLASH_TIME = 2500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);

        ImageView logo = findViewById(R.id.logo);
        TextView appName = findViewById(R.id.ProteaHealth);
        TextView tagline = findViewById(R.id.tagline);

        Animation animation = AnimationUtils.loadAnimation(
                this,
                R.anim.splash_animation
        );

        logo.startAnimation(animation);

        new Handler().postDelayed(() -> {

            Animation textAnimation =
                    AnimationUtils.loadAnimation(
                            Splash.this,
                            R.anim.splash_animation
                    );

            ProteaHealth.startAnimation(textAnimation);
            tagline.startAnimation(textAnimation);

        }, 400);

        new Handler().postDelayed(() -> {

            Intent intent = new Intent(
                    Splash.this,
                    Login.class
            );

            startActivity(intent);

            finish();

        }, SPLASH_TIME);
    }
}
