package com.nirab.merobhadameter;

import android.os.Bundle;

import com.actionbarsherlock.app.SherlockPreferenceActivity;

public class MyPreferencesActivity extends SherlockPreferenceActivity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      addPreferencesFromResource(R.xml.mapsettings);
  }
} 