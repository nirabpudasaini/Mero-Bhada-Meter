package com.nirab.merobhadameter;

import java.io.File;

import org.mapsforge.android.maps.MapController;
import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.MapActivity;
import org.mapsforge.core.GeoPoint;

import com.actionbarsherlock.app.SherlockActivity;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;




public class OfflineMapActivity extends SherlockActivity {
	
	SharedPreferences preferences;
	boolean offline_mode;
	
	MapView mv;
	MapController mc;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		mv = new MapView(this);
		mv.setClickable(true);
		mv.setBuiltInZoomControls(true);
		mv.setMapFile(new File(Environment.getExternalStorageDirectory()
				.getPath() + "/kathmandu.map"));
		setContentView(mv);
		mc = mv.getController();//		mv =//		mv = new MapView(this);
//		mv.setClickable(true);
//		mv.setBuiltInZoomControls(true);
//		mv.setMapFile(new File(Environment.getExternalStorageDirectory()
//				.getPath() + "/kathmandu.map"));
//		setContentView(mv);
//		mc = mv.getController();
//		GeoPoint kathmandu = new GeoPoint(27.7167, 85.3667);
//		mc.setZoom(14);
//		mc.setCenter(kathmandu);//		mv = new MapView(this);
//		mv.setClickable(true);
//		mv.setBuiltInZoomControls(true);
//		mv.setMapFile(new File(Environment.getExternalStorageDirectory()
//				.getPath() + "/kathmandu.map"));
//		setContentView(mv);
//		mc = mv.getController();
//		GeoPoint kathmandu = new GeoPoint(27.7167, 85.3667);
//		mc.setZoom(14);
//		mc.setCenter(kathmandu); new MapView(this);
//		mv.setClickable(true);
//		mv.setBuiltInZoomControls(true);
//		mv.setMapFile(new File(Environment.getExternalStorageDirectory()
//				.getPath() + "/kathmandu.map"));
//		setContentView(mv);
//		mc = mv.getController();
//		GeoPoint kathmandu = new GeoPoint(27.7167, 85.3667);
//		mc.setZoom(14);
//		mc.setCenter(kathmandu);
		GeoPoint kathmandu = new GeoPoint(27.7167, 85.3667);
		mc.setZoom(14);
		mc.setCenter(kathmandu);
	}

	@Override
	protected void onStart() {//		mv = new MapView(this);
//		mv.setClickable(true);
//		mv.setBuiltInZoomControls(true);
//		mv.setMapFile(new File(Environment.getExternalStorageDirectory()
//				.getPath() + "/kathmandu.map"));
//		setContentView(mv);
//		mc = mv.getController();
//		GeoPoint kathmandu = new GeoPoint(27.7167, 85.3667);
//		mc.setZoom(14);
//		mc.setCenter(kathmandu);
		// TODO Auto-generated method stub
		super.onStart();
		offline_mode = preferences.getBoolean("offline_chkbox_preference",
				false);
		if (!offline_mode) {
			Intent onlineMap = new Intent(OfflineMapActivity.this,
					MapActivity.class);
			startActivity(onlineMap);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
		getSupportMenuInflater().inflate(R.menu.map_activity_actions, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(
			com.actionbarsherlock.view.MenuItem item) {
		switch (item.getItemId()) {
		// case R.id.action_track:
		//
		// if (tracking) {
		//
		// // compute the total time we were tracking
		//
		// long milliseconds = System.currentTimeMillis() - startTime;
		//
		// double totalHours = milliseconds / MILLISECONDS_PER_HOUR;
		//
		// // create a dialog displaying the results
		//
		// AlertDialog.Builder dialogBuilder =
		//
		// new AlertDialog.Builder(MapActivity.this);
		// dialogBuilder.setTitle("Fare Value");
		//
		// double distanceKM = distanceTraveled / 1000.0;
		// double totalMins = totalHours * 60;
		//
		// // display distanceTraveled traveled and average speed
		// dialogBuilder.setMessage(String.format(
		// "You travelled %f Kilometers in %f minutes",
		// distanceKM, totalMins));
		// dialogBuilder.setPositiveButton("Ok", null);
		// dialogBuilder.show(); // display the dialog
		// tracking = false;
		// item.setTitle("Strat Tracking");
		// mapb.setEnabled(true);
		//
		// } else {
		// item.setTitle("Stop Tracking");
		// tracking = true;
		// mapb.setEnabled(false);
		// startTime = System.currentTimeMillis(); // get current time
		//
		// mv.invalidate(); // clear the route
		// distanceTraveled = 0;
		// previousLocation = null; // starting a new route
		//
		// }
		//
		// return true;

		case R.id.action_preference:

			Intent i = new Intent(OfflineMapActivity.this,
					MyPreferencesActivity.class);
			startActivity(i);

			return true;

			// case R.id.action_download:
			//
			// final DownloadTask downloadTask = new
			// DownloadTask(MapActivity.this);
			// downloadTask
			// .execute("https://dl.dropboxusercontent.com/u/95497883/kathmandu-2013-8-12.map");
			//
			// mProgressDialog
			// .setOnCancelListener(new DialogInterface.OnCancelListener() {
			// @Override
			// public void onCancel(DialogInterface dialog) {
			// downloadTask.cancel(true);
			// }
			// });

		}

		return super.onOptionsItemSelected(item);
	}

}
