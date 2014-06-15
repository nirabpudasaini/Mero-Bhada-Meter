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
	private Double roadLength;

	public OfflineRoute(LatLong start, LatLong stop, List<LatLong> via) {
		startPoint = start;
		stopPoint = stop;
		viaPoints = via;
		roadLength = 0.0;
	}

	public List<LatLong> getRoute() {
		List<LatLong> road = new ArrayList<LatLong>();
		GraphHopper gh = new GraphHopper().forMobile();
		gh.setCHShortcuts(true, true);
		gh.load(Environment.getExternalStorageDirectory()
				+ "/merobhadameter/maps/kathmandu-gh/");

		if (viaPoints.isEmpty()) {

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
			roadLength = roadLength + response.getDistance();

		}

		else {

			for (int i = 0; i < viaPoints.size(); i++) {

				GHRequest request;

				if (i == 0) {
					

					request = new GHRequest(startPoint.latitude,
							startPoint.longitude, viaPoints.get(0).latitude,
							viaPoints.get(0).longitude);
					request.setAlgorithm("dijkstrabi");
					GHResponse response = gh.route(request);
					String gh_value = String.valueOf(response);
					Log.i("What GraphHopper API Response", gh_value);

					int points = response.getPoints().getSize();
					PointList tmp = response.getPoints();
					for (int j = 0; j < points; j++) {
						road.add(new LatLong(tmp.getLatitude(i), tmp
								.getLongitude(i)));
					}
					Log.i("Latlong after gh calculation", String.valueOf(road));
					roadLength = roadLength + response.getDistance();
				}

				if (i > 0 && i <= viaPoints.size() - 1) {

					

					request = new GHRequest(viaPoints.get(i - 1).latitude,
							viaPoints.get(i - 1).longitude,
							viaPoints.get(i).latitude,
							viaPoints.get(i).longitude);

					request.setAlgorithm("dijkstrabi");
					GHResponse response = gh.route(request);
					String gh_value = String.valueOf(response);
					Log.i("What GraphHopper API Response", gh_value);

					int points = response.getPoints().getSize();
					PointList tmp = response.getPoints();
					for (int j = 0; j < points; j++) {
						road.add(new LatLong(tmp.getLatitude(i), tmp
								.getLongitude(i)));
					}
					Log.i("Latlong after gh calculation", String.valueOf(road));
					roadLength = roadLength + response.getDistance();

				}

				if (i == (viaPoints.size() - 1)) {
					

					request = new GHRequest(
							viaPoints.get(viaPoints.size() - 1).latitude,
							viaPoints.get(viaPoints.size() - 1).longitude,
							stopPoint.latitude, stopPoint.longitude);
					request.setAlgorithm("dijkstrabi");
					GHResponse response = gh.route(request);
					String gh_value = String.valueOf(response);
					Log.i("What GraphHopper API Response", gh_value);

					int points = response.getPoints().getSize();
					PointList tmp = response.getPoints();
					for (int j = 0; j < points; j++) {
						road.add(new LatLong(tmp.getLatitude(i), tmp
								.getLongitude(i)));
					}
					Log.i("Latlong after gh calculation", String.valueOf(road));
					roadLength = roadLength + response.getDistance();

				}

			}

		}

		return road;

	}

	public Double getRoadLengthInKm() {
		return roadLength / 1000;
	}

}
