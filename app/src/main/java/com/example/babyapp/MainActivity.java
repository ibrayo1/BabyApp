package com.example.babyapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    // this is the standard uuid for the hc-05 module
    // 00:14:03:06:81:42 is the mac address of the hc-05 module
    // H-C-2010-06-01 is the name of the hc-05 module
    private HomeFragment homeFragment;
    private HistoryFragment historyFragment;
    private SettingsFragment settingsFragment;
    private SharedPreferences spf;
    private FragmentManager fm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // get the spf manager
        spf = PreferenceManager.getDefaultSharedPreferences(this);

        // declare the new fragments
        homeFragment = new HomeFragment();
        historyFragment = new HistoryFragment();
        settingsFragment = new SettingsFragment();

        // create the fragment manager
        fm = getSupportFragmentManager();

        // select the home fragment as the default
        //bottomNav.setSelectedItemId(R.id.nav_home);
        fm.beginTransaction().add(R.id.fragment_container, settingsFragment).hide(settingsFragment).commit();
        fm.beginTransaction().add(R.id.fragment_container, historyFragment).hide(historyFragment).commit();
        fm.beginTransaction().add(R.id.fragment_container, homeFragment).commit();

        // create the bottom navigation menu
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnNavigationItemSelectedListener(navListener);

    }

    @Override
    public void onResume(){ // set the temperature scale
        homeFragment.setTempScale(spf.getString("temperature", "Celsius"));
        super.onResume();
    }

    // override back pressed button so that the app minimizes instead of closing
    @Override
    public void onBackPressed() {
        this.moveTaskToBack(true);
    }

    // set the on item click listener for the bottom navigation menu
    private BottomNavigationView.OnNavigationItemSelectedListener navListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

                    switch (menuItem.getItemId()){
                        case R.id.nav_home:
                            fm.beginTransaction().hide(historyFragment).commit();
                            fm.beginTransaction().hide(settingsFragment).commit();
                            fm.beginTransaction().show(homeFragment).commit(); homeFragment.setTempScale(spf.getString("temperature", "Celsius"));
                            break;
                        case R.id.nav_history:
                            fm.beginTransaction().hide(homeFragment).commit();
                            fm.beginTransaction().hide(settingsFragment).commit();
                            fm.beginTransaction().show(historyFragment).commit();
                            break;
                        case R.id.nav_settings:
                            fm.beginTransaction().hide(homeFragment).commit();
                            fm.beginTransaction().hide(historyFragment).commit();
                            fm.beginTransaction().show(settingsFragment).commit();
                            break;
                    }

                    return true;
                }
            };

}
