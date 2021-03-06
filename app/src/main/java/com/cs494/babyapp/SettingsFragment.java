package com.cs494.babyapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
public class SettingsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // add the preferences onto the settings fragment
        getFragmentManager().beginTransaction().replace(R.id.preferences_frameLayout, new Settings()).commit();

        return view;
    }





}
