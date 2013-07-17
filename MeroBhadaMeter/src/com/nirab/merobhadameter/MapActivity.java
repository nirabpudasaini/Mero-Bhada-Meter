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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MapActivity extends Activity implements MapEventsReceiver,
		LocationListener {

	MapView mv;
	MapController mc;
	protected GeoPoint startPoint, destinationPoint, kathmandu;
	protected ArrayList<GeoPoint> viaPoints;
	protected ItemizedOverlayWithBubble<ExtendedOverlayItem> itineraryMarkers;
	protected static int START_INDEX = -2, DEST_INDEX = -1;
	protected ExtendedOverlayItem markerStart, markerDestination;
	SimpleLocationOverlay myLocationOverlay;

	protected Road mRoad;
	protected ItemizedOverlayWithBubble<ExtendedOverlayItem> roadNodeMarkers;
	protected PathOverlay roadOverlay;

	String fv = "0.00", rv = "0.00", dv = "0.00";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map);
		mv = (MapView) findViewById(R.id.mapview);
		mv.setTileSource(TileSourceFactory.MAPNIK);

		mv.setClickable(true);
		mv.setBuiltInZoomControls(true);
		mv.setMultiTouchControls(true);

		mc = mv.getController();
		kathmandu = new GeoPoint(27.7167, 85.3667);

		// To use MapEventsReceiver methods, we add a MapEventsOverlay:
		MapEventsOverlay overlay = new MapEventsOverlay(this, this);
		mv.getOverlays().add(overlay);

		LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				30 * 1000, 100.0f, this);

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

		// on click handler for the destination search button
		Button searchButton = (Button) findViewById(R.id.buttonSearch);
		searchButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				handleSearchLocationButton();
			}
		});

		// register for the long press that brings the context menu, registered
		// in the textview because mapview will also catch map drag events
		Button mapb = (Button) findViewById(R.id.mapview_btn);
		mapb.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {

			}
		});

		registerForContextMenu(mapb);

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

	/**
	 * Geocoding of the destination address
	 */
	public void handleSearchLocationButton() {
		EditText destinationEdit = (EditText) findViewById(R.id.editDestination);
		// Hide the soft keyboard:
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(destinationEdit.getWindowToken(), 0);

		String destinationAddress = destinationEdit.getText().toString();
		GeocoderNominatim geocoder = new GeocoderNominatim(this);
		geocoder.setOptions(true); // ask for enclosing polygon (if any)
		try {
			List<Address> foundAdresses = geocoder.getFromLocationName(
					destinationAddress, 1);
			if (foundAdresses.size() == 0) { // if no address found, display an
												// error
				Toast.makeText(this, "Address not found.", Toast.LENGTH_SHORT)
						.show();
			} else {
				Address address = foundAdresses.get(0); // get first address
				destinationPoint = new GeoPoint(address.getLatitude(),
						address.getLongitude());
				markerDestination = putMarkerItem(markerDestination,
						destinationPoint, DEST_INDEX, R.string.destination,
						R.drawable.marker_destination, -1);
				getRoadAsync();
				mc.setCenter(destinationPoint);
				// // get and display enclosing polygon:
				// Bundle extras = address.getExtras();
				// if (extras != null && extras.containsKey("polygonpoints")) {
				// ArrayList<GeoPoint> polygon = extras
				// .getParcelableArrayList("polygonpoints");
				// // Log.d("DEBUG", "polygon:"+polygon.size());
				// updateUIWithPolygon(polygon);
				// } else {
				// updateUIWithPolygon(null);
				// }
			}
		} catch (Exception e) {
			Toast.makeText(this, "Error preforming search", Toast.LENGTH_SHORT)
					.show();
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

//		 final Dialog d = new Dialog(this);
//		 d.setContentView(R.layout.fare_popup);
//		
//		 TextView fare_value = (TextView) findViewById(R.id.fare_value);
//		 TextView rate_value = (TextView) findViewById(R.id.rate_value);
//		 TextView distance_value = (TextView)findViewById(R.id.distance_value);

		Bundle bundle = getIntent().getExtras();
		double rateperkm = bundle.getDouble("rateperkm");
		double waitingcharge = bundle.getDouble("waiting");
		double maxrate = bundle.getDouble("maxrate");
		String taxi_type = bundle.getString("taxi_type");

		if (taxi_type.equals("normal")) {
			rv = "Rs " + rateperkm + " per Kilometer and Rs " + waitingcharge
					+ " per 2 min waiting";
		}
		else if (taxi_type.equals("tourist")) {
			rv = "Rs " + rateperkm + " for 5 Km and Rs " + waitingcharge
					+ " for additional km, Rs " + maxrate + "maximum";
		}
		else{
			rv = "Could not get the rate";
		}

		if (road.mStatus == Road.STATUS_DEFAULT) {
			dv = "Could not get distance";
			fv = "Could not calculate fare";
			showFare();
//			 distance_value.setText(dv);
//			 rate_value.setText(rv);
//			 fare_value.setText(fv);
//			 d.show();

			return;

		}

		dv = String.valueOf(road.mLength);
		
		if (taxi_type.equals("normal")) {
			double fare = (road.mLength * rateperkm) + 10;
			fv = String.valueOf(fare);
		}
		
		if (taxi_type.equals("tourist")) {
			if (road.mLength < 5.00) {
				fv = String.valueOf(rateperkm);
			} else {
				double fare = ((road.mLength - 5) * waitingcharge + rateperkm);
				if (fare > maxrate) {
					fare = maxrate;
				}
				fv = String.valueOf(fare);
			}
		}

		Log.i("distance value", dv);
		Log.i("rate value", rv);
		Log.i("fare value", fv);
		showFare();


//		fare_value.setText(fv);
//		distance_value.setText(dv);
//		rate_value.setText(rv);
//		d.show();

	}
	
	public void showFare(){
		Intent popup = new Intent(MapActivity.this, FarePopUp.class);
		Bundle popup_bundle = new Bundle();
		popup_bundle.putString("distance_value", dv );
		popup_bundle.putString("rate_value", rv );
		popup_bundle.putString("fare_value", fv );
		popup.putExtras(popup_bundle);
		startActivity(popup);
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
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onLocationChanged(Location location) {
		// TODO Auto-generated method stub
		myLocationOverlay.setLocation(new GeoPoint(location));

	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub

	}

}
