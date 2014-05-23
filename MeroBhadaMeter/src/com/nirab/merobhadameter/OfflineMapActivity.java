package com.nirab.merobhadameter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.android.AndroidPreferences;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.layer.MyLocationOverlay;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.LayerManager;
import org.mapsforge.map.layer.Layers;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.layer.overlay.Polyline;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.model.common.PreferencesFacade;
import org.mapsforge.map.reader.MapDatabase;
import org.mapsforge.map.reader.header.FileOpenResult;
import org.mapsforge.map.reader.header.MapFileInfo;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
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
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.util.PointList;

/**
 * A simple application which demonstrates how to use a MapView.
 */
public class OfflineMapActivity extends SherlockActivity implements
		OnSharedPreferenceChangeListener {
	protected static final int DIALOG_ENTER_COORDINATES = 2923878;
	protected ArrayList<LayerManager> layerManagers = new ArrayList<LayerManager>();
	protected ArrayList<MapViewPosition> mapViewPositions = new ArrayList<MapViewPosition>();
	protected ArrayList<MapView> mapViews = new ArrayList<MapView>();
	protected PreferencesFacade preferencesFacade;
	protected SharedPreferences sharedPreferences;

	protected Marker marker_start, marker_destination, marker_set_start,
			marker_set_destination;
	protected Polyline polyline_track;
	protected List<LatLong> latLongs_track = new ArrayList<LatLong>();
	protected List<LatLong> tmpLatLongs_track = new ArrayList<LatLong>();

	protected TileCache tileCache;

	TextView faredisplay;

	protected LatLong gpsStartPoint, gpsEndPoint;
	protected LatLong tmpClickedPoint = new LatLong(0, 0);
	protected LatLong departurePoint, destinationPoint;
	private MyLocationOverlay myLocationOverlay;
	private LocationManager locationManager;
	private Location previousLocation;
	private boolean gpsFix;

	private long distanceTraveled;
	private long startTime;
	private static final double MILLISECONDS_PER_HOUR = 1000 * 60 * 60;
	protected Boolean tracking, offline_mode;

	@Override
	public void onSharedPreferenceChanged(SharedPreferences preferences,
			String key) {
		if (SamplesApplication.SETTING_SCALE.equals(key)) {
			destroyTileCaches();
			for (MapView mapView : mapViews) {
				mapView.getModel().displayModel.setUserScaleFactor(DisplayModel
						.getDefaultUserScaleFactor());
			}
			Log.d(SamplesApplication.TAG, "Tilesize now "
					+ mapViews.get(0).getModel().displayModel.getTileSize());
			createTileCaches();
			redrawLayers();
		}
	}

	protected void addOverlayLayers(Layers layers) {
		marker_start = Utils.createMarker(this, R.drawable.marker_departure,
				gpsStartPoint);
		marker_destination = Utils.createMarker(this,
				R.drawable.marker_destination, gpsEndPoint);
		marker_set_start = Utils.createMarker(this,
				R.drawable.marker_departure, departurePoint);
		marker_set_destination = Utils.createMarker(this,
				R.drawable.marker_destination, destinationPoint);

		layers.add(marker_start);
		layers.add(marker_destination);
		layers.add(marker_set_start);
		layers.add(marker_set_destination);

		polyline_track = new Polyline(Utils.createPaint(
				AndroidGraphicFactory.INSTANCE.createColor(Color.BLUE), 8,
				Style.STROKE), AndroidGraphicFactory.INSTANCE);
		latLongs_track = polyline_track.getLatLongs();
		if (tmpLatLongs_track != null) {
			for (int i = 0; i < tmpLatLongs_track.size(); i++) {

				latLongs_track.add(tmpLatLongs_track.get(i));

			}
		}
		Log.i("LatLongs in Overlay", String.valueOf(latLongs_track));

		layers.add(polyline_track);

	}

	protected void createControls() {
		// time to create control elements
	}

	protected void createLayerManagers() {
		for (MapView mapView : mapViews) {
			this.layerManagers.add(mapView.getLayerManager());
		}
	}

	protected void createLayers() {
		TileRendererLayer tileRendererLayer = new TileRendererLayer(
				this.tileCache,
				this.mapViewPositions.get(0),
				false,
				org.mapsforge.map.android.graphics.AndroidGraphicFactory.INSTANCE) {
			@Override
			public boolean onLongPress(LatLong tapLatLong, Point thisXY,
					Point tapXY) {
				OfflineMapActivity.this.onLongPress(tapLatLong);
				return true;
			}
		};
		tileRendererLayer.setMapFile(this.getMapFile());
		tileRendererLayer.setXmlRenderTheme(this.getRenderTheme());
		this.layerManagers.get(0).getLayers().add(tileRendererLayer);

		// a marker to show at the position
		Drawable drawable = getResources().getDrawable(R.drawable.taxi_icon1);
		Bitmap bitmap = AndroidGraphicFactory.convertToBitmap(drawable);

		// create the overlay and tell it to follow the location
		this.myLocationOverlay = new MyLocationOverlay(this,
				this.mapViewPositions.get(0), bitmap);
		this.myLocationOverlay.setSnapToLocationEnabled(false);
		this.layerManagers.get(0).getLayers().add(this.myLocationOverlay);

		addOverlayLayers(layerManagers.get(0).getLayers());
	}

	protected void createMapViewPositions() {
		for (MapView mapView : mapViews) {
			this.mapViewPositions
					.add(initializePosition(mapView.getModel().mapViewPosition));
		}
	}

	protected void createMapViews() {
		MapView mapView = getMapView();
		mapView.getModel().init(this.preferencesFacade);
		mapView.setClickable(true);
		mapView.getMapScaleBar().setVisible(true);
		mapView.setBuiltInZoomControls(hasZoomControls());
		mapView.getMapZoomControls().setZoomLevelMin((byte) 10);
		mapView.getMapZoomControls().setZoomLevelMax((byte) 20);
		registerForContextMenu(mapView);
		this.mapViews.add(mapView);
	}

	protected void createSharedPreferences() {
		SharedPreferences sp = this.getSharedPreferences(getPersistableId(),
				MODE_PRIVATE);
		this.preferencesFacade = new AndroidPreferences(sp);
	}

	protected void createTileCaches() {
		this.tileCache = AndroidUtil.createTileCache(this, getPersistableId(),
				this.mapViews.get(0).getModel().displayModel.getTileSize(),
				this.getScreenRatio(),
				this.mapViews.get(0).getModel().frameBufferModel
						.getOverdrawFactor());
	}

	protected void destroyLayers() {
		for (LayerManager layerManager : this.layerManagers) {
			for (Layer layer : layerManager.getLayers()) {
				layerManager.getLayers().remove(layer);
				layer.onDestroy();
				Log.i("DESTROY", "Destroyed: " + layer.toString());
			}
		}
	}

	protected void destroyMapViewPositions() {
		for (MapViewPosition mapViewPosition : mapViewPositions) {
			mapViewPosition.destroy();
		}
	}

	protected void destroyMapViews() {
		for (MapView mapView : mapViews) {
			mapView.destroy();
		}
	}

	protected void destroyTileCaches() {
		this.tileCache.destroy();
	}

	protected MapPosition getInitialPosition() {
		MapDatabase mapDatabase = new MapDatabase();
		final FileOpenResult result = mapDatabase.openFile(getMapFile());
		if (result.isSuccess()) {
			final MapFileInfo mapFileInfo = mapDatabase.getMapFileInfo();
			if (mapFileInfo != null && mapFileInfo.startPosition != null) {
				return new MapPosition(mapFileInfo.startPosition,
						(byte) mapFileInfo.startZoomLevel);
			} else {
				return new MapPosition(new LatLong(27.517037, 85.38886),
						(byte) 12);
			}
		}
		throw new IllegalArgumentException("Invalid Map File "
				+ getMapFileName());
	}

	/**
	 * @return a map file
	 */
	protected File getMapFile() {
		File file = new File(Environment.getExternalStorageDirectory()
				+ "/merobhadameter/maps/kathmandu-gh/", this.getMapFileName());
		Log.i(SamplesApplication.TAG, "Map file is " + file.getAbsolutePath());
		return file;
	}

	/**
	 * @return the map file name to be used
	 */
	protected String getMapFileName() {
		return "kathmandu.map";
	}

	/**
	 * @return the layout to be used
	 */
	protected int getLayoutId() {
		return R.layout.offlinemap;
	}

	protected MapView getMapView() {
		setContentView(getLayoutId());
		return (MapView) findViewById(R.id.offlinemapview);
	}

	/**
	 * @return the id that is used to save this mapview
	 */
	protected String getPersistableId() {
		return this.getClass().getSimpleName();
	}

	/**
	 * @return the rendertheme for this viewer
	 */
	protected XmlRenderTheme getRenderTheme() {
		return InternalRenderTheme.OSMARENDER;
	}

	/**
	 * @return the screen ratio that the mapview takes up (for cache
	 *         calculation)
	 */
	protected float getScreenRatio() {
		return 1.0f;
	}

	protected boolean hasZoomControls() {
		return true;
	}

	/**
	 * initializes the map view position.
	 * 
	 * @param mvp
	 *            the map view position to be set
	 * @return the mapviewposition set
	 */
	protected MapViewPosition initializePosition(MapViewPosition mvp) {
		LatLong center = mvp.getCenter();

		if (center.equals(new LatLong(0, 0))) {
			mvp.setMapPosition(this.getInitialPosition());
		}
		mvp.setZoomLevelMax((byte) 24);
		mvp.setZoomLevelMin((byte) 7);
		return mvp;
	}

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		this.sharedPreferences.registerOnSharedPreferenceChangeListener(this);
		AndroidGraphicFactory.createInstance(getApplication());

		offline_mode = sharedPreferences.getBoolean(
				"offline_chkbox_preference", false);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		File filecheck = new File(Environment.getExternalStorageDirectory()
				.getPath() + "/merobhadameter/maps/kathmandu-gh/kathmandu.map");

		if (!filecheck.exists()) {
			Toast.makeText(
					this,
					"No Offline Map File Present. Please Download it by Clicking Download in the Menu",
					Toast.LENGTH_LONG).show();
			editor.putBoolean("offline_chkbox_preference", false);
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD) {
				editor.apply();
			} else {
				editor.commit();
			}

			Intent Map = new Intent(this, MapActivity.class);
			startActivity(Map);
			finish();
		}

		createSharedPreferences();
		createMapViews();
		createMapViewPositions();
		createLayerManagers();
		createTileCaches();
		createControls();

		tracking = false;

		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		locationManager.addGpsStatusListener(gpsStatusListener);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				1000, 20, locationListener);
		faredisplay = (TextView) findViewById(R.id.faredisplay_offline);
		Typeface face = Typeface.createFromAsset(getAssets(),
				"fonts/led-real-regular.ttf");
		faredisplay.setTypeface(face, 1);

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		destroyTileCaches();
		destroyMapViewPositions();
		destroyMapViews();
		this.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
		org.mapsforge.map.android.graphics.AndroidResourceBitmap
				.clearResourceBitmaps();
	}

	@Override
	protected void onPause() {
		myLocationOverlay.disableMyLocation();
		super.onPause();
		for (MapView mapView : mapViews) {
			mapView.getModel().save(this.preferencesFacade);
		}
		this.preferencesFacade.save();
	}

	@Override
	protected void onStart() {
		super.onStart();
		offline_mode = sharedPreferences.getBoolean(
				"offline_chkbox_preference", false);
		if (!offline_mode) {
			Intent Map = new Intent(this, MapActivity.class);
			startActivity(Map);
		}
		createLayers();
	}

	@Override
	public void onResume() {
		super.onResume();
		this.myLocationOverlay.enableMyLocation(true);
	}

	@Override
	protected void onStop() {
		super.onStop();
		destroyLayers();
	}

	protected void redrawLayers() {
		for (LayerManager layerManager : this.layerManagers) {
			layerManager.redrawLayers();
		}
	}

	/**
	 * sets the content view if it has not been set already.
	 */
	protected void setContentView() {
		setContentView(this.mapViews.get(0));
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

				// Things to be done when user hits "Stop Tracking"

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
				// Things that are done when user hits "Start Tracking"
				if (!locationManager
						.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
					buildAlertMessageNoGps();
					return true;
				}

				item.setTitle("Stop Tracking");
				tracking = true;
				faredisplay.setText("Fare Amount: RS "
						+ String.valueOf(Fare.getFlagdownRate()));
				startTime = System.currentTimeMillis(); // get current time
				latLongs_track.clear();
				distanceTraveled = 0;
				previousLocation = null;
				resetMarkers();

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

	protected void resetMarkers() {
		destroyLayers();
		gpsStartPoint = null;
		gpsEndPoint = null;
		departurePoint = null;
		destinationPoint = null;
		createLayers();
		redrawLayers();
	}

	// Alert dialog to propmt user to enable gps
	private void buildAlertMessageNoGps() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(
				"Your GPS seems to be disabled, Please enable it to start tracking")
				.setCancelable(false)
				.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog,
							final int id) {
						startActivity(new Intent(
								android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
					}
				})
				.setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog,
							final int id) {
						dialog.cancel();
					}
				});
		final AlertDialog alert = builder.create();
		alert.show();
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

	// update location on map
	public void updateLocation(Location location) {
		if (location != null && gpsFix) // location not null; have GPS fix
		{

			// add the given Location to the route
			if (tmpLatLongs_track != null) {
				latLongs_track = tmpLatLongs_track;
			}

			// if there is a previous location
			if (previousLocation != null) {

				distanceTraveled += location.distanceTo(previousLocation);
				Fare realtime_fare = new Fare(this, distanceTraveled / 1000.0,
						"01");
				realtime_fare.calculate();
				int rupees, paisa;
				rupees = realtime_fare.getRupees();
				paisa = realtime_fare.getPaisa();
				faredisplay.setText(String.format("Fare Amount: Rs %d . %d",
						rupees, paisa));

				gpsEndPoint = locationToLatLong(location);
				latLongs_track.add(gpsEndPoint);
				marker_destination.setLatLong(gpsEndPoint);

				this.mapViews.get(0).getModel().mapViewPosition
						.animateTo(gpsEndPoint);

			} else {

				gpsStartPoint = locationToLatLong(location);
				latLongs_track.add(gpsStartPoint);
				marker_start.setLatLong(gpsStartPoint);
			}

		}

		Log.i("LATLONGS", String.valueOf(latLongs_track));
		previousLocation = location;
		destroyLayers();
		createLayers();

		if (latLongs_track != null) {
			tmpLatLongs_track = latLongs_track;
		}
	}

	public static LatLong locationToLatLong(Location location) {
		return new LatLong(location.getLatitude(), location.getLongitude());
	}

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

	protected void onLongPress(LatLong position) {
		if (tracking) {
			Toast.makeText(this, "Tracking in Progress", Toast.LENGTH_LONG)
					.show();
			return;
		}

		openContextMenu(this.mapViews.get(0));
		tmpClickedPoint = position;

	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.map_menu, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_departure:
			departurePoint = tmpClickedPoint;
			marker_set_start.setLatLong(tmpClickedPoint);
			getRoad();
			redrawLayers();
			return true;
		case R.id.menu_destination:
			destinationPoint = tmpClickedPoint;
			marker_set_destination.setLatLong(tmpClickedPoint);
			getRoad();
			redrawLayers();
			return true;

		case R.id.menu_viapoint:
			Toast.makeText(this, "This feature is in Progress for Offline",
					Toast.LENGTH_LONG).show();
			return true;
		case R.id.menu_reset_markers:
			resetMarkers();
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	void getRoad() {
		if (departurePoint == null || destinationPoint == null) {
			if (gpsStartPoint != null || gpsEndPoint != null) {
				resetMarkers();
			}
			return;
		}

		latLongs_track.clear();
		GraphHopper gh = new GraphHopper().forMobile();
		gh.setCHShortcuts(true, true);
		gh.load(Environment.getExternalStorageDirectory()
				+ "/merobhadameter/maps/kathmandu-gh/");

		GHRequest request = new GHRequest(departurePoint.latitude,
				departurePoint.longitude, destinationPoint.latitude,
				destinationPoint.longitude);
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
		Log.i("Latlong after gh calculation", String.valueOf(latLongs_track));
		Fare f = new Fare(this, response.getDistance() / 1000, "01");
		f.calculate();
		f.show();
		int rupees, paisa;
		rupees = f.getRupees();
		paisa = f.getPaisa();
		faredisplay.setText(String.format("Fare Amount: Rs %d . %d", rupees,
				paisa));

	}

}
