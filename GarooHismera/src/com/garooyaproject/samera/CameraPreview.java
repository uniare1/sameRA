package com.garooyaproject.samera;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

public class CameraPreview extends SurfaceView implements Callback {
	private SurfaceHolder mSurfaceHolder;
	private Camera mCamera;
	
	//TODO: match the preview ratio to the picture ratio
	
	public CameraPreview(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}

	public CameraPreview(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	public CameraPreview(Context context) {
		super(context);

	}

	public CameraPreview(Context context, Camera camera) {
		super(context);
		mCamera = camera;
		mSurfaceHolder = getHolder();
		mSurfaceHolder.addCallback(this);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		
		if(null == mSurfaceHolder.getSurface()) {
			return;
		}
		
		mCamera.stopPreview();
		
		try {
			Camera.Parameters params = mCamera.getParameters();
			
			float imgRatio = (float) width/ (float)height;			
			Rect previewSize = new Rect(0, 0, 0, 1);
			List<Size> sizes =params.getSupportedPreviewSizes();
			for(Size size : sizes) {
				if(Math.abs(imgRatio - ((float)size.width / (float)size.height)) < Math.abs(imgRatio - ((float)previewSize.width() / (float)previewSize.height()))) {
					previewSize.right = size.width;
					previewSize.bottom = size.height;
				}
			}
			
			params.setPreviewSize(previewSize.width() , previewSize.height());
			mCamera.setParameters(params);
		
			mCamera.setPreviewDisplay(mSurfaceHolder);
						
			mCamera.startPreview();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		
		try {
			if(mCamera == null) {
				mCamera = Camera.open();
			}
			mCamera.setPreviewDisplay(holder);
			mCamera.startPreview();
		} catch (IOException e) {
			mCamera.release();
			mCamera = null;
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		mCamera.stopPreview();
		mCamera.release();
		mCamera = null;
	}

	@Override
	protected boolean fitSystemWindows(Rect insets) {
		// TODO Auto-generated method stub
		return super.fitSystemWindows(insets);
	}
	
}
