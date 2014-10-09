package com.garooyaproject.samera;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.util.Log;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
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
	private ExifInterface mExifInterface;
	private boolean mIsFace = false;
	
	public ReferenceImage (ImageView imageView, Point size) {
		mImageView = imageView;
		mScreenSize = size;
		
        LayoutParams lp = (LayoutParams) mImageView.getLayoutParams();
        mLayoutParams = copyLayoutParams(lp);
	}
	
	
	public void update() {
	
		LayoutParams lp = copyLayoutParams(mLayoutParams);
		
        Drawable drawable = mImageView.getDrawable();
        
		if(mIsSmallView == true) {
			mIsSmallView = false;				

	        drawable.setAlpha(120);			
			
			lp.addRule(RelativeLayout.CENTER_IN_PARENT);
			lp.width = mScreenSize.x;
			lp.height = mScreenSize.y;
			lp.topMargin = 0;
			lp.bottomMargin = 0;
			lp.rightMargin = 0;
			lp.leftMargin = 0;

		} else {
			mIsSmallView = true;
			drawable.setAlpha(255);
		}
		mImageView.setLayoutParams(lp);
		mImageView.setAdjustViewBounds(true);
		mImageView.setScaleType(ScaleType.CENTER_INSIDE);
	}
	
	private LayoutParams copyLayoutParams(LayoutParams params) {
        LayoutParams lp = new LayoutParams((ViewGroup.LayoutParams)params);
        int [] rules = params.getRules();
        int length = rules.length;
        for(int index = 0; index < length; index++) {
        	lp.addRule(index, rules[index]);
        }
        
		lp.topMargin = params.topMargin;
		lp.bottomMargin = params.bottomMargin;
		lp.leftMargin = params.leftMargin;
		lp.rightMargin = params.rightMargin;        
        
        return lp;
	}
	
	public void setOnClickListener(OnClickListener listener) {
		mImageView.setOnClickListener(listener);
	}
	
	public void setOnLongClickListener(OnLongClickListener listener) {
		mImageView.setOnLongClickListener(listener);
	}
	
	public void setExif(ExifInterface exif) {
		mExifInterface = exif;
	}
	
	public void setFace(boolean isFace) {
		mIsFace = isFace;
	}
	
	public void setImageBitmap(Bitmap bitmap) {
		
		if(bitmap == null) {
			mImageView.setImageBitmap(null);
			return;
		}
		
        float y = (float) mScreenSize.y / (float) bitmap.getHeight();
        float x = (float) mScreenSize.x / (float) bitmap.getWidth();
        float scale = Math.max(y, x);
        
        
        
        Matrix matrix = new Matrix();
        
        float degree = 0;
        if(bitmap.getHeight() > bitmap.getWidth()) {
        	degree = -90;
        }
        
		int exifOrientation = mExifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0);
		degree += ExifToOrientation(exifOrientation);
		if(degree != 0) {
			matrix.setRotate(degree);
		}

        if(mIsFace) {        
        		matrix.setScale(-scale, scale);
        } else {
        	matrix.setScale(scale, scale);
        }
		
		Bitmap image = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
		
		mImageView.setImageBitmap(image);
		mImageView.invalidate();
	}
	
	private int ExifToOrientation(int exifOrientation) {
		
		int degree;
		
		switch (exifOrientation) {
		case ExifInterface.ORIENTATION_NORMAL:
			degree = 0;
			break;
		case ExifInterface.ORIENTATION_ROTATE_90:
			degree = 0;
			break;
		case ExifInterface.ORIENTATION_ROTATE_180:
			degree = 0;
			break;
		case ExifInterface.ORIENTATION_ROTATE_270:
			degree = 180;
			break;
		default:
			degree = 0;
			break;
		}
		
		return degree;
	}
	
}
