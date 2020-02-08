package com.cs494.babyapp;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.race604.drawable.wave.WaveDrawable;


public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // get the image view for the splash screen and make a new wave drawable
        ImageView logo = findViewById(R.id.logo);
        WaveDrawable mWaveDrawable = new WaveDrawable(this, R.drawable.logo);
        logo.setImageDrawable(mWaveDrawable);

        // create the animation for the image on the splash screen
        ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
        animator.setDuration(2000);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        mWaveDrawable.setIndeterminateAnimator(animator);
        mWaveDrawable.setIndeterminate(true);

        // create the thread and start it
        final Intent i = new Intent(this, MainActivity.class);
        Thread timer = new Thread(){
            public void run (){
                try{
                    sleep(2000);
                } catch (InterruptedException e){
                    e.printStackTrace();
                } finally {
                    startActivity(i);
                    finish();
                }
            }
        };

        timer.start();


    }

}

