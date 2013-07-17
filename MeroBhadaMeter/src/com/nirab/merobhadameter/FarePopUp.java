package com.nirab.merobhadameter;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class FarePopUp extends Activity {

	String fv = "0.00", rv = "0.00", dv = "0.00";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fare_popup);
		Bundle bundle = getIntent().getExtras();
		dv = bundle.getString("distance_value");
		rv = bundle.getString("rate_value");
		fv = bundle.getString("fare_value");
		
		 TextView fare_value = (TextView) findViewById(R.id.fare_value);
		 TextView rate_value = (TextView) findViewById(R.id.rate_value);
		 TextView distance_value = (TextView)findViewById(R.id.distance_value);
		 
		 distance_value.setText(dv);
		 rate_value.setText(rv);
		 fare_value.setText(fv);
		
		 Button ok_btn = (Button) findViewById(R.id.btn_farepopoup_ok);
		 
		 ok_btn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				finish();
			}
		});
	}
	

}
