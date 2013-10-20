package com.nirab.merobhadameter;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
//import android.view.Menu;
import android.content.Intent;
import android.content.SharedPreferences;

public class MainActivity extends Activity {

	SharedPreferences preferences;
	boolean offline_mode;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.splash);
		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		Thread timer = new Thread(){
			public void run(){
				try{
					sleep(2000);
				}
				catch(InterruptedException e){
					e.printStackTrace();
				}
				finally{
					offline_mode = preferences.getBoolean("offline_chkbox_preference", false);
					if (offline_mode){
						Intent openMain = new Intent(MainActivity.this, OfflineMapActivity.class);
						startActivity(openMain);
					}
					else{
					Intent openMain = new Intent(MainActivity.this, MapActivity.class);
					startActivity(openMain);
					}
				}
			}
			
		};
		timer.start();
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		finish();
	}


}
