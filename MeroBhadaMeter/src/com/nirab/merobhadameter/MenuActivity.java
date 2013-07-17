package com.nirab.merobhadameter;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MenuActivity extends Activity {

	Button tourist_taxi, normal_taxi;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.selection);
		tourist_taxi = (Button) findViewById(R.id.btn_tourist);
		normal_taxi = (Button) findViewById(R.id.btn_normal);

		normal_taxi.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent i = new Intent(MenuActivity.this, MapActivity.class);
				Bundle bundle = new Bundle();
				bundle.putString("taxi_type", "normal");
				bundle.putDouble("rateperkm", 32.00);
				bundle.putDouble("waiting", 6.40);
				i.putExtras(bundle);
				startActivity(i);

			}
		});

		tourist_taxi.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent i = new Intent(MenuActivity.this, MapActivity.class);
				Bundle bundle = new Bundle();
				bundle.putString("taxi_type", "tourist");
				bundle.putDouble("rateperkm", 438.00);
				bundle.putDouble("waiting", 63.00);
				bundle.putDouble("maxrate", 1026.00);
				i.putExtras(bundle);
				startActivity(i);

			}
		});

	}

}
