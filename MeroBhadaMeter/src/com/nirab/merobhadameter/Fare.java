package com.nirab.merobhadameter;

import android.app.AlertDialog;
import android.content.Context;
import android.text.format.Time;


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

		flagdown = getFlagdownRate();
		rateperkm = getRatePerKm();
		waitingcharge = getWaitingCharge();

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

	public static double getFlagdownRate() {
		Time now = new Time();
		now.setToNow();
		if (now.hour > 06 && now.hour < 20) {
			return 14.0;
		} else {
			return 21.0;
		}
	}

	public static double getRatePerKm() {
		Time now = new Time();
		now.setToNow();
		if (now.hour > 06 && now.hour < 20) {
			return 37.0;
		} else {
			return 55.5;
		}
	}

	public static double getWaitingCharge() {
		Time now = new Time();
		now.setToNow();
		if (now.hour > 06 && now.hour < 20) {
			return 7.40;
		} else {
			return 11.10;
		}
	}

	public void show() {

		AlertDialog.Builder dialogBuilder =

		new AlertDialog.Builder(context);
		dialogBuilder.setTitle("Fare Summary");
		int rupees = (int) totalfare;
		int paisa = (int) ((totalfare % 1) * 100);

		dialogBuilder
				.setMessage(String
						.format("Total distance: %.02f Kilometers \n Total Fare: %d Rupees and %d Paisa",
								distance_road, rupees, paisa));
		dialogBuilder.setPositiveButton("Ok", null);
		dialogBuilder.show(); // display the dialog
	}

}
