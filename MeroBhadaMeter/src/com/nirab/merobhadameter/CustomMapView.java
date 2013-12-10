package com.nirab.merobhadameter;

import org.mapsforge.map.android.view.MapView;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;


public class CustomMapView extends MapView {

	
	private GestureDetectorCompat mDetector;
	
	public CustomMapView(Context context){
		super(context);
	}

	public CustomMapView(Context context, AttributeSet attributeSet) {
		super(context, attributeSet);
		// TODO Auto-generated constructor stub
		
	}

	@Override
	public boolean onTouchEvent(MotionEvent motionEvent) {
		// TODO Auto-generated method stub
		return super.onTouchEvent(motionEvent);
		
		
	}
	
	class MyGestureListener extends GestureDetector.SimpleOnGestureListener {

		@Override
		public void onLongPress(MotionEvent e) {
			// TODO Auto-generated method stub
			super.onLongPress(e);
		}
		
	}




}
