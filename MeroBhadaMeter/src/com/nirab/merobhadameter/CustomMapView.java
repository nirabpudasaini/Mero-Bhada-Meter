package com.nirab.merobhadameter;

import org.mapsforge.map.android.view.MapView;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;


public class CustomMapView extends MapView {

	
	private GestureDetector gd;
	
	public CustomMapView(Context context){
		super(context);
	}

	public CustomMapView(Context context, AttributeSet attributeSet) {
		super(context, attributeSet);
		// TODO Auto-generated constructor stub
		gd = new GestureDetector(context, sogl);
		
	}

	@Override
	public boolean onTouchEvent(MotionEvent motionEvent) {
		// TODO Auto-generated method stub
		if (gd.onTouchEvent(motionEvent))
	        return true;
	    return super.onTouchEvent(motionEvent);

		
	}
	
	
	GestureDetector.SimpleOnGestureListener sogl =
            new GestureDetector.SimpleOnGestureListener() {

  
    public void onLongPress(MotionEvent event) {
        
    }
};




}
