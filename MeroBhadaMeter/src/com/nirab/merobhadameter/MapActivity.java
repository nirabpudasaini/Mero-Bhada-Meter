package com.nirab.merobhadameter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.bonuspack.location.GeocoderNominatim;
import org.osmdroid.bonuspack.overlays.ExtendedOverlayItem;
import org.osmdroid.bonuspack.overlays.ItemizedOverlayWithBubble;
import org.osmdroid.bonuspack.overlays.MapEventsOverlay;
import org.osmdroid.bonuspack.overlays.MapEventsReceiver;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.bonuspack.routing.RoadNode;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.PathOverlay;
import org.osmdroid.views.overlay.SimpleLocationOverlay;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;

public class MapActivity extends SherlockActivity implements MapEventsReceiver, AsyncTaskCompleteListener {

	MapView mv;
	MapController mc;
	Button mapb;
	protected GeoPoint gpsStartPoint, gpsDestinationPoint, startPoint,
			destinationPoint, kathmandu;
	private LocationManager locationManager;
	protected ArrayList<GeoPoint> viaPoints;
	protected ItemizedOverlayWithBubble<ExtendedOverlayItem> itineraryMarkers;
	protected static int START_INDEX = -2, DEST_INDEX = -1;
	protected ExtendedOverlayItem markerStart, markerDestination;
	SimpleLocationOverlay myLocationOverlay;

	protected Road mRoad;
	protected ItemizedOverlayWithBubble<ExtendedOverlayItem> roadNodeMarkers;
	protected PathOverlay roadOverlay;

	private PowerManager.WakeLock wakeLock; // used to prevent device sleep
	private boolean gpsFix; // whether we have a GPS fix for accurate data
	private Location previousLocation;
	private long distanceTraveled; // total distance the user traveled
	private long startTime; // time (in milliseconds) when tracking starts
	private static final double MILLISECONDS_PER_HOUR = 1000 * 60 * 60;

	SharedPreferences preferences;
	boolean tracking, offline_mode;

	TextView faredisplay;

	Double road_distance;
	String vehicle_type;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		preferences = PreferenceManager.getDefaultSharedPreferences(this);

		setContentView(R.layout.map);
		mv = (MapView) findViewById(R.id.mapview);
		mv.setTileSource(TileSourceFactory.MAPNIK);

		mv.setClickable(true);
		mv.setBuiltInZoomControls(true);
		mv.setMultiTouchControls(true);

		mc = (MapController) mv.getController();
		kathmandu = new GeoPoint(27.7167, 85.3667);
		distanceTraveled = 0;
		faredisplay = (TextView) findViewById(R.id.faredisplay);
		Typeface face = Typeface.createFromAsset(getAssets(),
				"fonts/led-real-regular.ttf");
		faredisplay.setTypeface(face, 1);

		// To use MapEventsReceiver methods, we add a MapEventsOverlay:
		MapEventsOverlay overlay = new MapEventsOverlay(this, this);
		mv.getOverlays().add(overlay);

		MapEventsOverlay gpsOverlay = new MapEventsOverlay(this, this);
		mv.getOverlays().add(gpsOverlay);

		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		locationManager.addGpsStatusListener(gpsStatusListener);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				1000, 20, locationListener);

		if (savedInstanceState == null) {
			Location l = locationManager
					.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			if (l != null) {
				startPoint = new GeoPoint(l.getLatitude(), l.getLongitude());

			} else {
				// we put a hard-coded start
				startPoint = kathmandu;
			}
			destinationPoint = null;
			viaPoints = new ArrayList<GeoPoint>();
			mc.setZoom(14);
			mc.setCenter(startPoint);
		} else {
			startPoint = savedInstanceState.getParcelable("start");
			destinationPoint = savedInstanceState.getParcelable("destination");
			viaPoints = savedInstanceState.getParcelableArrayList("viapoints");
			mc.setZoom(savedInstanceState.getInt("zoom_level"));
			mc.setCenter((GeoPoint) savedInstanceState
					.getParcelable("map_center"));
		}

		myLocationOverlay = new SimpleLocationOverlay(this,
				new DefaultResourceProxyImpl(this));
		mv.getOverlays().add(myLocationOverlay);
		myLocationOverlay.setLocation(startPoint);

		// Itinerary markers:
		final ArrayList<ExtendedOverlayItem> waypointsItems = new ArrayList<ExtendedOverlayItem>();
		itineraryMarkers = new ItemizedOverlayWithBubble<ExtendedOverlayItem>(
				this, waypointsItems, mv, new ViaPointInfoWindow(
						R.layout.itinerary_bubble, mv));
		mv.getOverlays().add(itineraryMarkers);
		updateUIWithItineraryMarkers();

		// Route and Directions
		final ArrayList<ExtendedOverlayItem> roadItems = new ArrayList<ExtendedOverlayItem>();
		roadNodeMarkers = new ItemizedOverlayWithBubble<ExtendedOverlayItem>(
				this, roadItems, mv);
		mv.getOverlays().add(roadNodeMarkers);

		if (savedInstanceState != null) {
			mRoad = savedInstanceState.getParcelable("road");
			updateUIWithRoad(mRoad);
		}

		// TODO try add the search gunctionality using the action bar
		// // on click handler for the destination search button
		// Button searchButton = (Button) findViewById(R.id.buttonSearch);
		// searchButton.setOnClickListener(new View.OnClickListener() {
		// public void onClick(View view) {
		// handleSearchLocationButton();
		// }
		// });

		// register for the long press that brings the context menu, registered
		// in the textview because mapview will also catch map drag events
		mapb = (Button) findViewById(R.id.mapview_btn);
		mapb.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {

			}
		});

		registerForContextMenu(mapb);

	}

	// TODO this is for geocoding address from the search button
	// /**
	// * Geocoding of the destination address
	// */
	// public void handleSearchLocationButton() {
	// EditText destinationEdit = (EditText) findViewById(R.id.editDestination);
	// // Hide the soft keyboard:
	// InputMethodManager imm = (InputMethodManager)
	// getSystemService(Context.INPUT_METHOD_SERVICE);
	// imm.hideSoftInputFromWindow(destinationEdit.getWindowToken(), 0);
	//
	// String destinationAddress = destinationEdit.getText().toString();
	// GeocoderNominatim geocoder = new GeocoderNominatim(this);
	// geocoder.setOptions(true); // ask for enclosing polygon (if any)
	// try {
	// List<Address> foundAdresses = geocoder.getFromLocationName(
	// destinationAddress, 1);
	// if (foundAdresses.size() == 0) { // if no address found, display an
	// // error
	// Toast.makeText(this, "Address not found.", Toast.LENGTH_SHORT)
	// .show();
	// } else {
	// Address address = foundAdresses.get(0); // get first address
	// destinationPoint = new GeoPoint(address.getLatitude(),
	// address.getLongitude());
	// markerDestination = putMarkerItem(markerDestination,
	// destinationPoint, DEST_INDEX, R.string.destination,
	// R.drawable.marker_destination, -1);
	// getRoadAsync();
	// mc.setCenter(destinationPoint);
	// // // get and display enclosing polygon:
	// // Bundle extras = address.getExtras();
	// // if (extras != null && extras.containsKey("polygonpoints")) {
	// // ArrayList<GeoPoint> polygon = extras
	// // .getParcelableArrayList("polygonpoints");
	// // // Log.d("DEBUG", "polygon:"+polygon.size());
	// // updateUIWithPolygon(polygon);
	// // } else {
	// // updateUIWithPolygon(null);
	// // }
	// }
	// } catch (Exception e) {
	// Toast.makeText(this, "Error preforming search", Toast.LENGTH_SHORT)
	// .show();
	// }
	// }

	/**
	 * callback to store activity status before a restart (orientation change
	 * for instance)
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putParcelable("start", startPoint);
		outState.putParcelable("destination", destinationPoint);
		outState.putParcelableArrayList("viapoints", viaPoints);
		outState.putParcelable("road", mRoad);
		outState.putInt("zoom_level", mv.getZoomLevel());
		GeoPoint c = (GeoPoint) mv.getMapCenter();
		outState.putParcelable("map_center", c);
	}

	@Override
	protected void onStart() {
		super.onStart();
		// TODO
		offline_mode = preferences.getBoolean("offline_chkbox_preference",
				false);
		if (offline_mode) {
			Intent offlineMap = new Intent(this, OfflineMapActivity.class);
			startActivity(offlineMap);
		}
		// get the app's power manager
		PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		// get a wakelock preventing the device from sleeping
		wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
				"No sleep");
		wakeLock.acquire(); // acquire the wake lock
	}

	@Override
	protected void onStop() {

		super.onStop();
		wakeLock.release(); // release the wakelock

	}

	// ----------- Context Menu when clicking on the map
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
			startPoint = new GeoPoint((GeoPoint) tempClickedGeoPoint);
			markerStart = putMarkerItem(markerStart, startPoint, START_INDEX,
					R.string.departure, R.drawable.marker_departure, -1);
			getRoadAsync();
			return true;
		case R.id.menu_destination:
			destinationPoint = new GeoPoint((GeoPoint) tempClickedGeoPoint);
			markerDestination = putMarkerItem(markerDestination,
					destinationPoint, DEST_INDEX, R.string.destination,
					R.drawable.marker_destination, -1);
			getRoadAsync();
			return true;
		case R.id.menu_viapoint:
			GeoPoint viaPoint = new GeoPoint((GeoPoint) tempClickedGeoPoint);
			addViaPoint(viaPoint);
			getRoadAsync();
			return true;

		case R.id.menu_reset_markers:
			removeMarkers();
			return true;

		default:
			return super.onContextItemSelected(item);
		}
	}

	/**
	 * Reverse Geocoding
	 */
	public String getAddress(GeoPoint p) {
		GeocoderNominatim geocoder = new GeocoderNominatim(this);
		String theAddress;
		try {
			double dLatitude = p.getLatitudeE6() * 1E-6;
			double dLongitude = p.getLongitudeE6() * 1E-6;
			List<Address> addresses = geocoder.getFromLocation(dLatitude,
					dLongitude, 1);
			StringBuilder sb = new StringBuilder();
			if (addresses.size() > 0) {
				Address address = addresses.get(0);
				int n = address.getMaxAddressLineIndex();
				for (int i = 0; i <= n; i++) {
					if (i != 0)
						sb.append(", ");
					sb.append(address.getAddressLine(i));
				}
				theAddress = new String(sb.toString());
			} else {
				theAddress = null;
			}
		} catch (IOException e) {
			theAddress = null;
		}
		if (theAddress != null) {
			return theAddress;
		} else {
			return "";
		}
	}

	public void removePoint(int index) {
		if (index == START_INDEX)
			startPoint = null;
		else if (index == DEST_INDEX)
			destinationPoint = null;
		else
			viaPoints.remove(index);
		getRoadAsync();
		updateUIWithItineraryMarkers();
	}

	public void updateUIWithItineraryMarkers() {
		itineraryMarkers.removeAllItems();
		// Start marker:
		if (startPoint != null) {
			markerStart = putMarkerItem(null, startPoint, START_INDEX,
					R.string.departure, R.drawable.marker_departure, -1);
		}
		// Via-points markers if any:
		for (int index = 0; index < viaPoints.size(); index++) {
			putMarkerItem(null, viaPoints.get(index), index, R.string.viapoint,
					R.drawable.marker_via, -1);
		}
		// Destination marker if any:
		if (destinationPoint != null) {
			markerDestination = putMarkerItem(null, destinationPoint,
					DEST_INDEX, R.string.destination,
					R.drawable.marker_destination, -1);
		}
	}

	public void removeMarkers() {
		startPoint = null;
		destinationPoint = null;
		viaPoints.clear();
		updateUIWithItineraryMarkers();

	}

	// Async task to reverse-geocode the marker position in a separate thread:
	private class GeocodingTask extends AsyncTask<Object, Void, String> {
		ExtendedOverlayItem marker;

		protected String doInBackground(Object... params) {
			marker = (ExtendedOverlayItem) params[0];
			return getAddress(marker.getPoint());
		}

		protected void onPostExecute(String result) {
			marker.setDescription(result);
			// itineraryMarkers.showBubbleOnItem(???, map); //open bubble on the
			// item
		}
	}

	/**
	 * add (or replace) an item in markerOverlays. p position.
	 */
	public ExtendedOverlayItem putMarkerItem(ExtendedOverlayItem item,
			GeoPoint p, int index, int titleResId, int markerResId,
			int iconResId) {
		if (item != null) {
			itineraryMarkers.removeItem(item);
		}
		Drawable marker = getResources().getDrawable(markerResId);
		String title = getResources().getString(titleResId);
		ExtendedOverlayItem overlayItem = new ExtendedOverlayItem(title, "", p,
				this);
		overlayItem.setMarkerHotspot(OverlayItem.HotspotPlace.BOTTOM_CENTER);
		overlayItem.setMarker(marker);
		if (iconResId != -1)
			overlayItem.setImage(getResources().getDrawable(iconResId));
		overlayItem.setRelatedObject(index);
		itineraryMarkers.addItem(overlayItem);
		mv.invalidate();
		// Start geocoding task to update the description of the marker with its
		// address:
		new GeocodingTask().execute(overlayItem);
		return overlayItem;
	}

	public void addViaPoint(GeoPoint p) {
		viaPoints.add(p);
		putMarkerItem(null, p, viaPoints.size() - 1, R.string.viapoint,
				R.drawable.marker_via, -1);
	}

	/**
	 * Async task to get the road in a separate thread.
	 */
	private class UpdateRoadTask extends AsyncTask<Object, Void, Road> {
		protected Road doInBackground(Object... params) {
			@SuppressWarnings("unchecked")
			ArrayList<GeoPoint> waypoints = (ArrayList<GeoPoint>) params[0];
			RoadManager roadManager = null;
			roadManager = new OSRMRoadManager();
			return roadManager.getRoad(waypoints);
		}

		protected void onPostExecute(Road result) {
			mRoad = result;
			updateUIWithRoad(result);
			calculateFare(result);

		}
	}

	public void getRoadAsync() {
		mRoad = null;
		if (startPoint == null || destinationPoint == null) {
			updateUIWithRoad(mRoad);
			return;
		}
		ArrayList<GeoPoint> waypoints = new ArrayList<GeoPoint>(2);
		waypoints.add(startPoint);
		// add intermediate via points:
		for (GeoPoint p : viaPoints) {
			waypoints.add(p);
		}
		waypoints.add(destinationPoint);
		new UpdateRoadTask().execute(waypoints);
	}

	private void calculateFare(Road road) {

		if (road == null)
			return;

		if (road.mStatus == Road.STATUS_DEFAULT) {

			// Show error if route could not be fetched
			Toast.makeText(this, "Error getting the distance",
					Toast.LENGTH_SHORT).show();
			return;

		}

		road_distance = road.mLength;
		vehicle_type = preferences.getString("vehicle_list_preference", "01");
		Fare fare = new Fare(this, road_distance, "01");
		fare.calculate();
		fare.show();

	}

	private void putRoadNodes(Road road) {
		roadNodeMarkers.removeAllItems();
		Drawable marker = getResources().getDrawable(R.drawable.marker_node);
		int n = road.mNodes.size();
		TypedArray iconIds = getResources().obtainTypedArray(
				R.array.direction_icons);
		for (int i = 0; i < n; i++) {
			RoadNode node = road.mNodes.get(i);
			String instructions = (node.mInstructions == null ? ""
					: node.mInstructions);
			ExtendedOverlayItem nodeMarker = new ExtendedOverlayItem("Step "
					+ (i + 1), instructions, node.mLocation, this);
			nodeMarker.setSubDescription(road.getLengthDurationText(
					node.mLength, node.mDuration));
			nodeMarker.setMarkerHotspot(OverlayItem.HotspotPlace.CENTER);
			nodeMarker.setMarker(marker);
			int iconId = iconIds.getResourceId(node.mManeuverType,
					R.drawable.ic_empty);
			if (iconId != R.drawable.ic_empty) {
				Drawable icon = getResources().getDrawable(iconId);
				nodeMarker.setImage(icon);
			}
			roadNodeMarkers.addItem(nodeMarker);
		}
	}

	void updateUIWithRoad(Road road) {
		roadNodeMarkers.removeAllItems();
		List<Overlay> mapOverlays = mv.getOverlays();
		if (roadOverlay != null) {
			mapOverlays.remove(roadOverlay);
		}
		if (road == null)
			return;
		if (road.mStatus == Road.STATUS_DEFAULT)
			Toast.makeText(mv.getContext(),
					"We have a problem to get the route", Toast.LENGTH_SHORT)
					.show();
		roadOverlay = RoadManager.buildRoadOverlay(road, mv.getContext());
		Overlay removedOverlay = mapOverlays.set(1, roadOverlay);
		// we set the road overlay at the "bottom", just above the
		// MapEventsOverlay,
		// to avoid covering the other overlays.
		mapOverlays.add(removedOverlay);
		putRoadNodes(road);
		mv.invalidate();
		// Set route info in the text view:
		// ((TextView) findViewById(R.id.routeInfo)).setText(road
		// .getLengthDurationText(-1));
	}

	// for the touched point
	GeoPoint tempClickedGeoPoint;

	@Override
	public boolean longPressHelper(IGeoPoint p) {
		tempClickedGeoPoint = new GeoPoint((GeoPoint) p);
		Button mapb = (Button) findViewById(R.id.mapview_btn);
		openContextMenu(mapb); // menu is hooked on the Invisible button
		return true;
	}

	@Override
	public boolean singleTapUpHelper(IGeoPoint arg0) {

		return false;
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
				item.setTitle("Start Tracking");
				mapb.setEnabled(true);

			} else {

				// check if gps is enabled
				if (!locationManager
						.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
					buildAlertMessageNoGps();
					return true;
				}
				item.setTitle("Stop Tracking");
				faredisplay.setText("Fare Amount: RS "
						+ String.valueOf(Fare.getFlagdownRate()));
				tracking = true;
				mapb.setEnabled(false);
				startTime = System.currentTimeMillis(); // get current time
				mv.invalidate(); // clear the route
				distanceTraveled = 0;
				previousLocation = null; // starting a new route

			}

			return true;

		case R.id.action_preference:

			Intent i = new Intent(MapActivity.this, MyPreferencesActivity.class);
			startActivity(i);

			return true;

		case R.id.action_download:

			if (!isInternetAvailable()) {
				Toast.makeText(
						getBaseContext(),
						"Internet not available, Make sure you are connected to internet",
						Toast.LENGTH_LONG).show();
				return true;
			}

			if (!isWifiOn()) {
				if (isMobileDataOn()) {
					buildAlertMessageUsingDataNetwork();
					return true;
				}
			}

			final DownloadOfflineData downloadTask = new DownloadOfflineData(
					MapActivity.this);
			downloadTask
					.execute("https://dl.dropboxusercontent.com/u/95497883/kathmandu-gh.zip");

		}

		return super.onOptionsItemSelected(item);
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

	// Alert dialog to propmt user to enable gps
	private void buildAlertMessageUsingDataNetwork() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(
				"You are using data network, Are you sure you want to continue download")
				.setCancelable(false)
				.setPositiveButton("Yes",
						new DialogInterface.OnClickListener() {
							public void onClick(final DialogInterface dialog,
									final int id) {
								final DownloadOfflineData downloadTask = new DownloadOfflineData(
										MapActivity.this);
								downloadTask
										.execute("https://dl.dropboxusercontent.com/u/95497883/kathmandu-gh.zip");

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

	// update location on map
	public void updateLocation(Location location) {
		if (location != null && gpsFix) {
			markerStart = putMarkerItem(markerStart, gpsStartPoint,
					START_INDEX, R.string.departure,
					R.drawable.marker_departure, -1);
			markerDestination = putMarkerItem(markerDestination, new GeoPoint(
					location), DEST_INDEX, R.string.destination,
					R.drawable.marker_destination, -1);
			getRoadAsync();

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
			}

			Double latitude = location.getLatitude() * 1E6;
			Double longitude = location.getLongitude() * 1E6;
			// create GeoPoint representing the given Locations
			GeoPoint point = new GeoPoint(latitude.intValue(),
					longitude.intValue());
			// move the map to the current location
			mc.animateTo(point);

		} // end if

		previousLocation = location;
	}

	// responds to events from the LocationManager
	private final LocationListener locationListener = new LocationListener() {
		// when the location is changed
		@Override
		public void onLocationChanged(Location location) {

			gpsFix = true; // if getting Locations, then we have a GPS fix
			myLocationOverlay.setLocation(new GeoPoint(location));// current
																	// Position
			if (tracking) // if we're currently tracking
				updateLocation(location); // update the location
		} // end onLocationChanged

		public void onProviderDisabled(String provider) {
		} // end onProviderDisabled

		public void onProviderEnabled(String provider) {
		} // end onProviderEnabled

		public void onStatusChanged(String provider, int status, Bundle extras) {
		} // end onStatusChanged
	}; // end locationListener

	// determine whether we have GPS fix
	GpsStatus.Listener gpsStatusListener = new GpsStatus.Listener() {
		public void onGpsStatusChanged(int event) {
			if (event == GpsStatus.GPS_EVENT_FIRST_FIX) {
				gpsFix = true;
				Location gps = locationManager
						.getLastKnownLocation(LocationManager.GPS_PROVIDER);
				gpsStartPoint = new GeoPoint(gps.getLatitude(),
						gps.getLongitude());
				if (tracking) {
					Toast results = Toast.makeText(MapActivity.this,
							"Gpx Fix Available, Tracking Started",
							Toast.LENGTH_SHORT);
					// center the Toast in the screen
					results.setGravity(Gravity.CENTER,
							results.getXOffset() / 2, results.getYOffset() / 2);
					results.show(); // display the results
				}
			} // end if
		} // end method on GpsStatusChanged
	}; // end anonymous inner class

	public boolean isInternetAvailable() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnected()) {
			return true;
		}
		return false;
	}

	public boolean isWifiOn() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if (netInfo != null && netInfo.isConnected()) {
			return true;
		}
		return false;
	}

	public boolean isMobileDataOn() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm
				.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
		if (netInfo != null && netInfo.isConnected()) {
			return true;
		}
		return false;
	}

	@Override
	public void onTaskComplete(String result) {
		Toast.makeText(this, result, Toast.LENGTH_LONG).show();
		
	}

}
