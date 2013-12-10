package com.nirab.merobhadameter;

import android.app.AlertDialog;
import android.content.Context;
import android.text.format.Time;
import android.util.Log;

public class Fare {

	double totalfare, distance_road, rateperkm, waitingcharge, maxrate,
			flagdown;
	String vehicle_type;
	Context context;

	public Fare(Context c, Double distance, String type) {
		context = c;
		distance_road = distance;
		vehicle_type = type;
		totalfare = 0.0;
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

	public double calculate() {
		totalfare = (rateperkm * distance_road) + flagdown;
		return totalfare;
	}

	public int getRupees() {
		int rupees = (int) totalfare;
		return rupees;

	}

	public int getPaisa() {
		int paisa = (int) ((totalfare % 1) * 100);
		return paisa;
	}

	public void show() {

		AlertDialog.Builder dialogBuilder =

		new AlertDialog.Builder(context);
		dialogBuilder.setTitle("Total Fare Value");
		int rupees = (int) totalfare;
		int paisa = (int) ((totalfare % 1) * 100);

		dialogBuilder
				.setMessage(String
						.format("Total distance is %f Kilometers and your total charge is %d rupees and %d paisa",
								distance_road, rupees, paisa));
		dialogBuilder.setPositiveButton("Ok", null);
		dialogBuilder.show(); // display the dialog
	}

}
