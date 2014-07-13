package com.garooyaproject.hismera;

import java.io.IOException;
import java.util.List;

import android.content.Context;
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
		// TODO Auto-generated constructor stub
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
			
			List<Size> sizes =params.getSupportedPreviewSizes();
			
			params.setPreviewSize(sizes.get(0).width, sizes.get(0).height);
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
			mCamera.setPreviewDisplay(holder);
//			mCamera.startPreview();
		} catch (IOException e) {
			mCamera.release();
			mCamera = null;
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		mCamera.release();
//		mCamera.stopPreview();
		mCamera = null;
	}

}
