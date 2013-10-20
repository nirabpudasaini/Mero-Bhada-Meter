package com.nirab.merobhadameter;

import android.text.format.Time;
import android.util.Log;

public class Fare {

	Double distance_road, rateperkm, waitingcharge, maxrate, flagdown;
	String vehicle_type;

	public Fare(Double distance, String type) {
		distance_road = distance;
		vehicle_type = type;
		// 01 is for Normal Taix
		if (vehicle_type.equals("01")) {
			Time now = new Time();
			now.setToNow();
			Log.i("Rate per KM", String.valueOf(now.hour));
			if (now.hour > 06 && now.hour < 20) {
				rateperkm = 37.0;
				flagdown = 14.0;
				waitingcharge = 7.40;
			} else if (now.hour < 06 || now.hour > 20) {
				rateperkm = 55.5;
				flagdown = 21.0;
				waitingcharge = 11.10;
			}
			
			Log.i("Rate per KM", String.valueOf(rateperkm));
			Log.i("Flagdown Rate", String.valueOf(flagdown));
			Log.i("Waiting Charge", String.valueOf(waitingcharge));

		}
		// 02 is for tourist Taxi
		else if (vehicle_type.equals("02")) {

		}

	}

}
