package com.nirab.merobhadameter;

import java.io.File;
import java.util.List;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.map.android.AndroidPreferences;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.layer.MyLocationOverlay;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.LayerManager;
import org.mapsforge.map.layer.Layers;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.layer.overlay.Polyline;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.model.common.PreferencesFacade;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.util.PointList;

public class OfflineMapActivity extends SherlockActivity {

	protected MapView mapView;
	protected PreferencesFacade preferencesFacade;
	protected TileCache tileCache;
	protected MapViewPosition mapViewPosition;
	protected Marker marker_start, marker_destination;
	protected Polyline polyline_track;
	protected List<LatLong> latLongs_track;

	protected LatLong gpsStartPoint = new LatLong(0, 0);
	protected LatLong gpsEndPoint = new LatLong(0, 0);
	private MyLocationOverlay myLocationOverlay;
	private LocationManager locationManager;
	private Location previousLocation;
	private boolean gpsFix;

	private long distanceTraveled;
	private long startTime;
	private static final double MILLISECONDS_PER_HOUR = 1000 * 60 * 60;
	protected Boolean tracking, offline_mode;
	SharedPreferences preferences;
	Button mapb;
	TextView faredisplay;

	protected void addLayers(LayerManager layerManager, TileCache tileCache,
			MapViewPosition mapViewPosition) {
		layerManager.getLayers().add(
				Utils.createTileRendererLayer(tileCache, mapViewPosition,
						getMapFile()));

		Drawable drawable_currentpos = getResources().getDrawable(
				R.drawable.person);
		Bitmap bitmap_currentpos = AndroidGraphicFactory
				.convertToBitmap(drawable_currentpos);

		this.myLocationOverlay = new MyLocationOverlay(this, mapViewPosition,
				bitmap_currentpos);

		layerManager.getLayers().add(this.myLocationOverlay);
		addOverlayLayers(layerManager.getLayers());
	}

	protected void addOverlayLayers(Layers layers) {
		marker_start = Utils.createMarker(this, R.drawable.marker_departure,
				gpsStartPoint);
		marker_destination = Utils.createMarker(this,
				R.drawable.marker_destination, gpsEndPoint);

		layers.add(marker_start);
		layers.add(marker_destination);

		polyline_track = new Polyline(Utils.createPaint(
				AndroidGraphicFactory.INSTANCE.createColor(Color.BLUE), 8,
				Style.STROKE), AndroidGraphicFactory.INSTANCE);
		latLongs_track = polyline_track.getLatLongs();

		layers.add(polyline_track);

	}

	protected TileCache createTileCache() {
		return Utils.createExternalStorageTileCache(this, getPersistableId());
	}

	protected MapPosition getInitialPosition() {
		return new MapPosition(new LatLong(27.707, 85.315), (byte) 16);
	}

	/**
	 * @return the layout to be used
	 */
	protected int getLayoutId() {
		return R.layout.offlinemap;
	}

	/**
	 * @return a map file
	 */
	protected File getMapFile() {
		return new File(Environment.getExternalStorageDirectory()
				+ "/merobhadameter/maps/kathmandu-gh/", this.getMapFileName());
	}

	/**
	 * @return the map file name to be used
	 */
	protected String getMapFileName() {
		return "kathmandu.map";
	}

	/**
	 * @return the mapview to be used
	 */
	protected MapView getMapView() {
		// in this example the mapview is defined in the layout file
		// mapviewer.xml
		setContentView(getLayoutId());
		return (MapView) this.findViewById(R.id.offlinemapview);
	}

	/**
	 * @return the id that is used to save this mapview
	 */
	protected String getPersistableId() {
		return this.getClass().getSimpleName();
	}

	/**
	 * initializes the map view, here from source
	 */
	protected void init() {
		this.mapView = getMapView();

		initializeMapView(this.mapView, this.preferencesFacade);

		this.tileCache = createTileCache();

		mapViewPosition = this
				.initializePosition(this.mapView.getModel().mapViewPosition);

		addLayers(this.mapView.getLayerManager(), this.tileCache,
				mapViewPosition);

	}

	/**
	 * initializes the map view
	 * 
	 * @param mapView
	 *            the map view
	 */
	protected void initializeMapView(MapView mapView,
			PreferencesFacade preferences) {
		mapView.getModel().init(preferences);
		mapView.setClickable(true);
		mapView.setLongClickable(true);
		mapView.getMapScaleBar().setVisible(true);
	}

	/**
	 * initializes the map view position
	 * 
	 * @param mapViewPosition
	 *            the map view position to be set
	 * @return the mapviewposition set
	 */
	protected MapViewPosition initializePosition(MapViewPosition mapViewPosition) {
		LatLong center = mapViewPosition.getCenter();

		if (center.equals(new LatLong(0, 0))) {
			mapViewPosition.setMapPosition(this.getInitialPosition());
		}
		return mapViewPosition;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		offline_mode = preferences.getBoolean("offline_chkbox_preference",
				false);
		SharedPreferences.Editor editor = preferences.edit();
		File filecheck = new File(Environment.getExternalStorageDirectory()
				.getPath() + "/merobhadameter/maps/kathmandu-gh/kathmandu.map");

		if (!filecheck.exists()) {
			Toast.makeText(
					this,
					"No Offline Map File Present. Please Download it by Clicking Download in the Menu",
					Toast.LENGTH_LONG).show();
			editor.putBoolean("offline_chkbox_preference",
					false);
			editor.apply();
			Intent Map = new Intent(this, MapActivity.class);
			startActivity(Map);
			finish();
		}
		
		
//		GraphHopper gh = new GraphHopper().forMobile();
//		gh.setCHShortcuts(true, true);
//		gh.load(Environment.getExternalStorageDirectory()
//				+ "/merobhadameter/maps/kathmandu-gh/");
//
//		GHRequest request = new GHRequest(27.707, 85.315, 27.710, 85.325);
//		request.setAlgorithm("dijkstrabi");
//		GHResponse response = gh.route(request);
//		String gh_value = String.valueOf(response);
//		Log.i("What GraphHopper API Response", gh_value);

		SharedPreferences sharedPreferences = this.getSharedPreferences(
				getPersistableId(), MODE_PRIVATE);
		this.preferencesFacade = new AndroidPreferences(sharedPreferences);
		init();

		tracking = false;

		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		locationManager.addGpsStatusListener(gpsStatusListener);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				1000, 20, locationListener);
		faredisplay = (TextView) findViewById(R.id.faredisplay_offline);
		Typeface face=Typeface.createFromAsset(getAssets(),
                "fonts/led-real-regular.ttf");
		faredisplay.setTypeface(face,1);

	}

	@Override
	protected void onStart() {
		super.onStart();
		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		offline_mode = preferences.getBoolean("offline_chkbox_preference",
				false);
		if (!offline_mode) {
			Intent Map = new Intent(this, MapActivity.class);
			startActivity(Map);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		this.mapView.destroy();
	}

	@Override
	protected void onPause() {
		super.onPause();
		this.mapView.getModel().save(this.preferencesFacade);
		this.preferencesFacade.save();
		// stop receiving location updates
		this.myLocationOverlay.disableMyLocation();
	}

	@Override
	public void onResume() {
		super.onResume();
		// register for location updates
		this.myLocationOverlay.enableMyLocation(true);
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
		case R.id.action_track:
			if (tracking) {

				// compute the total time we were tracking

				long milliseconds = System.currentTimeMillis() - startTime;
				double totalHours = milliseconds / MILLISECONDS_PER_HOUR;

				double distanceKM = distanceTraveled / 1000.0;
				double totalMins = totalHours * 60;

				Fare fare = new Fare(this, distanceKM, "01");
				fare.calculate();
				fare.show();

				tracking = false;
				item.setTitle("Strat Tracking");
				// mapb.setEnabled(true);

			} else {
				item.setTitle("Stop Tracking");
				tracking = true;
				// mapb.setEnabled(false);
				startTime = System.currentTimeMillis(); // get current time
				mapView.invalidate();// clear the route
				distanceTraveled = 0;
				previousLocation = null;

			}

			return true;

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

	public static LatLong locationToLatLong(Location location) {
		return new LatLong(location.getLatitude(), location.getLongitude());
	}

	// update location on map
	public void updateLocation(Location location) {
		if (location != null && gpsFix) // location not null; have GPS fix
		{
			// add the given Location to the route

			marker_start.setLatLong(gpsStartPoint);
			marker_destination.setLatLong(gpsEndPoint);
			latLongs_track.clear();


			GraphHopper gh = new GraphHopper().forMobile();
			gh.setCHShortcuts(true, true);
			gh.load(Environment.getExternalStorageDirectory()
					+ "/merobhadameter/maps/kathmandu-gh/");

			GHRequest request = new GHRequest(gpsStartPoint.latitude,
					gpsStartPoint.longitude, gpsEndPoint.latitude,
					gpsEndPoint.longitude);
			request.setAlgorithm("dijkstrabi");
			GHResponse response = gh.route(request);
			String gh_value = String.valueOf(response);
			Log.i("What GraphHopper API Response", gh_value);

			int points = response.getPoints().getSize();
			PointList tmp = response.getPoints();
			for (int i = 0; i < points; i++) {
				latLongs_track.add(new LatLong(tmp.getLatitude(i), tmp
						.getLongitude(i)));
			}

			// if there is a previous location
			if (previousLocation != null) {

				distanceTraveled += location.distanceTo(previousLocation);
				Fare realtime_fare = new Fare(this, distanceTraveled/1000.0, "01");
				realtime_fare.calculate();
				int rupees, paisa;
				rupees = realtime_fare.getRupees();
				paisa = realtime_fare.getPaisa();
				faredisplay.setText(String.format("Fare Value: Rs %d . %d", rupees, paisa));
				
				
			} else {

				gpsStartPoint = locationToLatLong(location);
			}

			gpsEndPoint = locationToLatLong(location);
			MapPosition center = new MapPosition(gpsEndPoint, (byte) 17);
			mapViewPosition.setMapPosition(center);

		}

		previousLocation = location;
	}

	// responds to events from the LocationManager
	private final LocationListener locationListener = new LocationListener() {

		@Override
		public void onLocationChanged(Location location) {

			gpsFix = true;

			if (tracking)
				updateLocation(location);
		}

		public void onProviderDisabled(String provider) {
		}

		public void onProviderEnabled(String provider) {
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
		}
	};

	GpsStatus.Listener gpsStatusListener = new GpsStatus.Listener() {
		public void onGpsStatusChanged(int event) {
			if (event == GpsStatus.GPS_EVENT_FIRST_FIX) {
				gpsFix = true;

				if (tracking) {

					Toast results = Toast.makeText(OfflineMapActivity.this,
							"Gpx Fix Available, Tracking Started",
							Toast.LENGTH_SHORT);
					results.setGravity(Gravity.CENTER,
							results.getXOffset() / 2, results.getYOffset() / 2);
					results.show();
				}
			}
		}
	};

}
