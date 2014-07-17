package com.garooyaproject.hismera;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

public class ReferenceImage {
	
	private ImageView mImageView;
	private Point mScreenSize;
	private LayoutParams mLayoutParams;
	private boolean mIsSmallView = true;
	
	public ReferenceImage (ImageView imageView, Point size) {
		mImageView = imageView;
		mScreenSize = size;
		
        LayoutParams lp = (LayoutParams) mImageView.getLayoutParams();
        mLayoutParams = new LayoutParams((ViewGroup.LayoutParams)lp);
        int [] rules = lp.getRules();
        int length = rules.length;
        for(int index = 0; index < length; index++) {
        	mLayoutParams.addRule(index, rules[index]);
        }
	}
	
	
	public void update() {
		LayoutParams lp = (LayoutParams) mImageView.getLayoutParams();
        Drawable drawable = mImageView.getDrawable();
        
		if(mIsSmallView == true) {
			mIsSmallView = false;				

	        drawable.setAlpha(120);			
			
			int rules[] = lp.getRules();
			int length = rules.length;
			for(int index = 0; index < length; index++) {
				lp.addRule(index, 0);
			}			
			
//			lp.addRule(RelativeLayout.CENTER_IN_PARENT);
			lp.width = LayoutParams.WRAP_CONTENT;
			lp.height = LayoutParams.WRAP_CONTENT;
			lp.rightMargin = 0;
			lp.topMargin = 0;

		} else {
			mIsSmallView = true;
			drawable.setAlpha(255);		
			lp = new LayoutParams((ViewGroup.LayoutParams)mLayoutParams);
			
			int rules[] = mLayoutParams.getRules();
			int length = rules.length;
			for(int index = 0; index < length; index++) {
				lp.addRule(index, rules[index]);
			}	
		}
		mImageView.setLayoutParams(lp);
		mImageView.setAdjustViewBounds(true);
		mImageView.setScaleType(ScaleType.CENTER_INSIDE);
	}
	
	public void setOnClickListener(OnClickListener listener) {
		mImageView.setOnClickListener(listener);
	}
	
	public void setImageBitmap(Bitmap bitmap) {
		
		if(bitmap == null) {
			mImageView.setImageBitmap(null);
			return;
		}
		
        float y = (float) mScreenSize.y / (float) bitmap.getHeight();
        float x = (float) mScreenSize.x / (float) bitmap.getWidth();
        
        Log.d("ScreenSize", "x=" + mScreenSize.x + ", y=" + mScreenSize.y);
        Log.d("BitmapSize", "x=" + bitmap.getWidth() + ", y=" + bitmap.getHeight());
        Log.d("Radio", "xr = " + x + ", yr = " + y );
        
        
        
        int scaleX;
        int scaleY;
        
        if( y < x) {
        	scaleX = (bitmap.getWidth() * mScreenSize.y) / bitmap.getHeight();
        	scaleY = mScreenSize.y;
        } else {
        	scaleX = mScreenSize.x;
        	scaleY = (bitmap.getHeight() * mScreenSize.x) / bitmap.getWidth();
        }
        
        Log.d("Scale", "scaleX = " + scaleX + ", scaleY = " + scaleY );
        
        Bitmap image = Bitmap.createScaledBitmap(bitmap, scaleX, scaleY, false);
		
		mImageView.setImageBitmap(image);
	}
}
