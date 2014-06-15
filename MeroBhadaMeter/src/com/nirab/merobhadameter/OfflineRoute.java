package com.nirab.merobhadameter;

import java.util.ArrayList;
import java.util.List;

import org.mapsforge.core.model.LatLong;

import android.os.Environment;
import android.util.Log;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.util.PointList;

public class OfflineRoute {

	private LatLong startPoint, stopPoint;
	private List<LatLong> viaPoints;

	public OfflineRoute(LatLong start, LatLong stop, List<LatLong> via) {
		startPoint = start;
		stopPoint = stop;
		viaPoints = via;
	}

	public List<LatLong> getRoute() {
		List<LatLong> road = new ArrayList<LatLong>();
		if (viaPoints == null) {
			GraphHopper gh = new GraphHopper().forMobile();
			gh.setCHShortcuts(true, true);
			gh.load(Environment.getExternalStorageDirectory()
					+ "/merobhadameter/maps/kathmandu-gh/");

			GHRequest request = new GHRequest(startPoint.latitude,
					startPoint.longitude, stopPoint.latitude,
					stopPoint.longitude);
			request.setAlgorithm("dijkstrabi");
			GHResponse response = gh.route(request);
			String gh_value = String.valueOf(response);
			Log.i("What GraphHopper API Response", gh_value);

			int points = response.getPoints().getSize();
			PointList tmp = response.getPoints();
			for (int i = 0; i < points; i++) {
				road.add(new LatLong(tmp.getLatitude(i), tmp.getLongitude(i)));
			}
			Log.i("Latlong after gh calculation", String.valueOf(road));

		}
		else {
			
		}
		
		
		
		
		return road;

	}

}
