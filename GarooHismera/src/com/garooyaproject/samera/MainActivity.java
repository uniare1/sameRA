package com.garooyaproject.samera;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity implements OnClickListener
											, OnTouchListener
											, OnLongClickListener
											, LocationListener 
											, AutoFocusCallback{

	
	private final static String TAG = "MainActivity";
	
	private final static String ROOT_DIR = "sameRA";
	private final static int REQ_SELECT_PHOTO = 0;
	
	private Camera mCamera;
	private CameraPreview mPreview;
	private LocationManager mLocationManager;
	private Location mLocation;
//	private LayoutParams mLayoutParams;
//	private Point mScreenSize;
//	private boolean mIsSmallImageView = true;
	
	private ReferenceImage mReferenceImage;
	private List<Size> mPreviewSizes;
	
	private OrientationEventListener mOrientationEventListener;
	private int mOrientation;
	private static boolean mIsRequetingImage = false;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Orientation
        
        mOrientationEventListener = new OrientationEventListener(this) {
			
			@Override
			public void onOrientationChanged(int orientation) {
				Log.d(TAG, "Orentation : " + orientation);
				
				if(orientation >= 315 || orientation < 45) {
//					mOrientation = ExifInterface.ORIENTATION_NORMAL;
					mOrientation = 90; // landscape
				} else if(orientation >=45 && orientation < 135) {
//					mOrientation = ExifInterface.ORIENTATION_ROTATE_90;
					mOrientation = 180;//landscape
				} else if(orientation >= 135 && orientation < 225) {
//					mOrientation = ExifInterface.ORIENTATION_ROTATE_180;
					mOrientation = 270;//landscape
				} else if(orientation >= 225 && orientation < 316) {
//					mOrientation = ExifInterface.ORIENTATION_ROTATE_270;
					mOrientation = 0; //landscape
				}
				
				
			}
		};
        
        
        // GPS
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        String locationProvider = mLocationManager.getBestProvider(criteria, true);
        Log.d(TAG, locationProvider);
        
        mLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if(mLocation == null) {
        	mLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }
        
        mLocationManager.requestLocationUpdates(locationProvider, 
        										1000, 	// 1 sec
        										10, 	// 10 meter
        										this);
        
        
        // Camera
        if(false == checkCameraHardware(this)) {
        	Toast.makeText(this, "Camera Hardware is not supported", Toast.LENGTH_SHORT).show();
        	return;
        }
        
//        initCamera();
        
        Button button = (Button) findViewById(R.id.button1);
        button.setOnClickListener(this);
        
        ToggleButton toggleButton = (ToggleButton) findViewById(R.id.toggleButton1);
        toggleButton.setOnClickListener(this);
        
        Point screenSize = new Point(0, 0);
        getWindowManager().getDefaultDisplay().getSize(screenSize);

        ImageView imageView = (ImageView) findViewById(R.id.imageView1);

        mReferenceImage = new ReferenceImage(imageView, screenSize);        
        mReferenceImage.setOnClickListener(this);        
        mReferenceImage.setOnLongClickListener(this);
        
        
        // TODO: has to change
        // 해당 위치에 있는 기존의 사진
//        Bitmap bitmap = getImage(mLocation, 1);
//        mReferenceImage.setImageBitmap(bitmap);
        
        // TODO: setting alarm
        // booting시 동작, intent받을 곳?
//        Intent intent = new Intent(this, com.garooyaproject.samera.AlarmReceiver.class);
//        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
//        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
//        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP
//        		, System.currentTimeMillis()
//        		, AlarmManager.INTERVAL_HALF_HOUR
//        		, pendingIntent);     

        
//    	selectImage();
        
    }
    
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	mIsRequetingImage = false;
    	if(requestCode == REQ_SELECT_PHOTO) {
    		int width = LayoutParams.MATCH_PARENT, height = LayoutParams.MATCH_PARENT;
    		ImageView imageView = (ImageView) findViewById(R.id.imageView1);
    		
    		if(resultCode == RESULT_OK) {
    			Uri uri = data.getData();
    			
                String[] filePathColumn = {MediaStore.Images.Media.DATA};

                Cursor cursor = getContentResolver().query(uri, filePathColumn, null, null, null);
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String filePath = cursor.getString(columnIndex);
                cursor.close();

                Bitmap bitmap = BitmapFactory.decodeFile(filePath);
    			mReferenceImage.setImageBitmap(bitmap);
        		height = imageView.getDrawable().getIntrinsicHeight();
        		width = imageView.getDrawable().getIntrinsicWidth();
        		
        		// camera preview size 결정
    			float imgRatio = (float) width/ (float)height;			
    			Rect previewRect = new Rect(0, 0, 0, 1);
    			
    			while(mPreviewSizes == null) {
    				try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
    			}
    			
    			for(Size size : mPreviewSizes) {
    				if(Math.abs(imgRatio - ((float)size.width / (float)size.height)) < Math.abs(imgRatio - ((float)previewRect.width() / (float)previewRect.height()))) {
    					previewRect.right = size.width;
    					previewRect.bottom = size.height;
    				}
    			}
    			
    			Point screenSize = new Point(0, 0);
    	        getWindowManager().getDefaultDisplay().getSize(screenSize);
    			
    			width = (int) (screenSize.y * ((float) previewRect.right / (float) previewRect.bottom));
    			height = screenSize.y;
        		
    		} else {
    			mReferenceImage.setImageBitmap(null);
    		}

    		FrameLayout frameLayout = (FrameLayout) findViewById(R.id.camera_preview);
    		LayoutParams lp = frameLayout.getLayoutParams();
    		lp.height = height;
    		lp.width = width;
    		frameLayout.setLayoutParams(lp);
    	}

    }
    
    @Override
    protected void onStart() {
    	super.onStart();
    	Log.d(TAG, "onStart");
    	
    	selectImage();	
    }
    
    @Override
    protected void onStop() {
    	super.onStop();
    	Log.d(TAG, "onStop");
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	Log.d(TAG, "onResume");
    	if(mOrientationEventListener != null) {
    		mOrientationEventListener.enable();
    	}
    	initCamera(CameraInfo.CAMERA_FACING_BACK);
    }
    

    @Override
    protected void onPause() {
    	super.onPause();
    	Log.d(TAG, "onPause");
    	if(mOrientationEventListener != null) {
    		mOrientationEventListener.disable();
    	}
    	releaseCamera();
    }
    
    
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	releaseCamera();
    	mLocationManager.removeUpdates(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    
    private void initCamera(int facing) {
    	

    	int numOfCameras = Camera.getNumberOfCameras();
    	
    	for(int index = 0; index < numOfCameras; index++) {
    		CameraInfo cameraInfo = new CameraInfo();
    		Camera.getCameraInfo(index, cameraInfo);
    		if(cameraInfo.facing == facing) {
    			mCamera = Camera.open(index);
    			break;
    		} 
    	}  	

        // Orientation
//        mOrientationEventListener = new CameraOrientationEventListener(this, mCamera); 
        
        // config params
        Camera.Parameters cameraParams = mCamera.getParameters();
        
        // preview
        mPreviewSizes =cameraParams.getSupportedPreviewSizes();
        
        
        // resolution
        cameraParams.setJpegQuality(100);
        List<Camera.Size> sizes = cameraParams.getSupportedPictureSizes();
        Camera.Size size = sizes.get(0);
        cameraParams.setPictureSize(size.width, size.height);
        
        Log.d("Supported Resolution", size.width + "x" + size.height);
        
        // focus
        List<String> focusMode = cameraParams.getSupportedFocusModes();
        if(focusMode.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
        	cameraParams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }       
        
        mCamera.setParameters(cameraParams);
        
        mPreview = new CameraPreview(this, mCamera);
        mPreview.setFitsSystemWindows(false);
        
        FrameLayout fl = (FrameLayout) findViewById(R.id.camera_preview);
        fl.removeAllViews();
        fl.addView(mPreview);
    }
    
    private void selectImage () {
    	Intent intent = new Intent(Intent.ACTION_PICK);
    	intent.setType("image/*");
    	if(mIsRequetingImage == false && startActivityIfNeeded(intent, 0)) {
    		mIsRequetingImage = true;    		
    	}
    	
    }
    
    private boolean checkCameraHardware(Context context) {    	
    	if(context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
    		return true;
    	} else {
    		return false;
    	}    	
    }
    
    
    private PictureCallback mPictureCallback = new PictureCallback() {
		
		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			
			Log.d("MainActivity", "onPictureTaken");
			
			camera.startPreview();
			SavePictureAsync save = new SavePictureAsync();
			save.execute(data);
			
		}
	};
	
	private static File getOutputMediaFile() {
		File mediaStorageDir =  new File(
				Environment.getExternalStoragePublicDirectory(
						Environment.DIRECTORY_PICTURES), ROOT_DIR);
		if(!mediaStorageDir.exists()) {
			if(!mediaStorageDir.mkdirs()) {
				return null;
			}
		}
		
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
		File mediaFile = new File(mediaStorageDir, timeStamp + ".jpg");
		
		try {
			mediaFile.createNewFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return mediaFile;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onClick(View v) {
		if(R.id.button1 == v.getId()) {
			
			Log.d("MainActivity", "onClick - button");
			
			// config params
	        Camera.Parameters cameraParams = mCamera.getParameters();
	        cameraParams.setGpsAltitude(mLocation.getAltitude());
	        cameraParams.setGpsLatitude(mLocation.getLatitude());
	        cameraParams.setGpsLongitude(mLocation.getLongitude());
	        cameraParams.setGpsTimestamp(mLocation.getTime());	        
//	        cameraParams.setGpsProcessingMethod();
	        //TODO: set orientation information
	        
	        mCamera.setParameters(cameraParams);
	        
	        mCamera.autoFocus(this);
	        
			
		} else if(R.id.imageView1 == v.getId()) {			
			mReferenceImage.update();			
		} else if(R.id.toggleButton1 == v.getId()) {
			
			mCamera.release();
			
			ToggleButton toggleButton = (ToggleButton) findViewById(R.id.toggleButton1);
			if(toggleButton.isChecked()) {
				//current : front facing camera to back facing camera
				toggleButton.setChecked(true);
				initCamera(CameraInfo.CAMERA_FACING_BACK);
			} else {
				//current : back facing camera to front facing camera
				toggleButton.setChecked(false);
				initCamera(CameraInfo.CAMERA_FACING_FRONT);
			}
		}
	}
	
	@Override
	public boolean onLongClick(View v) {
		if(R.id.imageView1 == v.getId()) {			
			selectImage();			
			return true;
		}
		return false;
	}
	
	
	private void releaseCamera() {
		if(null != mCamera) {
			mCamera.release();
			mCamera = null;
		}
		
		if(mPreview != null) {
			mPreview.getHolder().removeCallback(mPreview);
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		Log.d("Location", location.toString());
		mLocation = location;		
	}

	@Override
	public void onProviderDisabled(String provider) {
		Log.d("onProviderDisabled", provider);
	}

	@Override
	public void onProviderEnabled(String provider) {
		Log.d("onProviderEnabled", provider);
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		Log.d("onStatusChanged", provider);
	}
	
	private Bitmap getImage(Location location, double radius) {
		
		// radius 단위?
		// 0도일때???
		String where = "latitude < " + String.valueOf(location.getLatitude() + radius) + 
				" AND latitude > " + String.valueOf(location.getLatitude() - radius)
				+ " AND longitude < " + String.valueOf(location.getLongitude() + radius) + 
				" AND longitude > " + String.valueOf(location.getLongitude() - radius)
				+ " AND datetaken is not null";
		
		String orderBy = MediaStore.Images.Media.DATE_TAKEN + " DESC";
		
		Cursor cursor = MediaStore.Images.Media.query(getContentResolver(), 
				MediaStore.Images.Media.EXTERNAL_CONTENT_URI, 
				new String [] {MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA, MediaStore.Images.Media.DATE_TAKEN}, 
				where, orderBy);
		
		cursor.moveToFirst();
		
//		for(int i = 0; i < cursor.getCount(); i++) {
//			Log.d("getImage", cursor.getLong(0) + ":" + cursor.getString(1) + ":" + new Date(cursor.getLong(2)));
//			if(!cursor.moveToNext()) break;
//		}
		Log.d("getImage", cursor.getLong(0) + ":" + cursor.getString(1) + ":" + new Date(cursor.getLong(2)));
		String path = cursor.getString(1);		
		cursor.close();
		
		// TODO: exif 바탕으로 rotation
		ExifInterface exif = null;
		try {
			exif = new ExifInterface(path);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return BitmapFactory.decodeFile(path);
	}

	@Override
	public void onAutoFocus(boolean success, Camera camera) {
		
		Log.d("MainActivity", "onAutoFocus - success : " + success);
		camera.takePicture(null, null, mPictureCallback);
//		if(success) {
//			camera.takePicture(null, null, mPictureCallback);
//		}
		
	}
	
	private class SavePictureAsync extends AsyncTask<byte[], Integer, Boolean> {

		private ExifInterface mExif;
		
		@Override
		protected Boolean doInBackground(byte[]... params) {
			
			Log.d("MainActivity", "doInBackground");
			
			
			File pictureFile = getOutputMediaFile();
			
			String path = pictureFile.getAbsolutePath();

			OutputStream os;
			try {
				os = new FileOutputStream(pictureFile);
				os.write(params[0]);
				os.flush();
				os.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			mExif = writeEXIF(path, mLocation);

			String dateTime = mExif.getAttribute(ExifInterface.TAG_DATETIME);
			long time = 0;
			try {
				time = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault()).parse(dateTime).getTime();
			} catch (ParseException e) {
				e.printStackTrace();
			}
			
			// Orientation
			ContentValues values = new ContentValues(2);
			values.put(MediaStore.Images.Media.DATA, path);
			values.put(MediaStore.Images.Media.LATITUDE, mLocation.getLatitude());
			values.put(MediaStore.Images.Media.LONGITUDE, mLocation.getLongitude());
			values.put(MediaStore.Images.Media.DATE_TAKEN, time);
			values.put(MediaStore.Images.Media.ORIENTATION, ExifToOrientation(Integer.decode(mExif.getAttribute(ExifInterface.TAG_ORIENTATION))));
			
			getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
			
			return true;
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
//			super.onPostExecute(result);
			Log.d("MainActivity", "onPostExecute");
	        Bitmap bitmap = getImage(mLocation, 1);
	        mReferenceImage.setExif(mExif);
	        mReferenceImage.setFace(!((ToggleButton) findViewById(R.id.toggleButton1)).isChecked());
	        mReferenceImage.setImageBitmap(bitmap);			
		}		
	}


	

	private ExifInterface writeEXIF(String path, Location location) {
		ExifInterface exif = null;
		try {
			exif = new ExifInterface(path);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		// when the photo is taken
		exif.setAttribute(ExifInterface.TAG_DATETIME, 
				DateFormat.format("yyyy:MM:dd HH:mm:ss", System.currentTimeMillis()).toString());
		
		// where the photo is taken
		double latitude = location.getLatitude();
		double longitude = location.getLongitude();
		
		int num1Lat = (int)Math.floor(latitude);
		int num2Lat = (int)Math.floor((latitude - num1Lat) * 60);
		double num3Lat = (latitude - ((double)num1Lat+((double)num2Lat/60))) * 3600000;
		
		int num1Lon = (int)Math.floor(longitude);
		int num2Lon = (int)Math.floor((longitude - num1Lon) * 60);
		double num3Lon = (longitude - ((double)num1Lon+((double)num2Lon/60))) * 3600000;
		
		exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, num1Lat+"/1,"+num2Lat+"/1,"+num3Lat+"/1000");
		exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, num1Lon+"/1,"+num2Lon+"/1,"+num3Lon+"/1000");
		
		if (latitude > 0) {
		    exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N"); 
		} else {
		    exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "S");
		}
		
		if (longitude > 0) {
		    exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "E");    
		} else {
		exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "W");
		}
		
		
		// TODO: 멤버 변수 사용하지 말자.
		// toogle 직접 사용하지 말자.
		ToggleButton toggleButton = (ToggleButton) findViewById(R.id.toggleButton1);
		exif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(OrientationToExif(mOrientation, !toggleButton.isChecked())));
		
		
		
		try {
			exif.saveAttributes();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return exif;
		
	}
	
	
	private int OrientationToExif(int orientation, boolean isFace) {		
		int result;
		
		switch (orientation) {
		case 0:
			result = ExifInterface.ORIENTATION_NORMAL;
			break;
		case 90:
			if(isFace) {
				result = ExifInterface.ORIENTATION_ROTATE_270;
			} else {
				result = ExifInterface.ORIENTATION_ROTATE_90;
			}
			
			break;
		case 180:
			result = ExifInterface.ORIENTATION_ROTATE_180;
			break;
		case 270:
			if(isFace) {
				result = ExifInterface.ORIENTATION_ROTATE_90;
			} else {
				result = ExifInterface.ORIENTATION_ROTATE_270;
			}
			break;
		default:
			result = ExifInterface.ORIENTATION_NORMAL;
			break;
		}
		
		return result;
	}
	
	private int ExifToOrientation(int exifOrientation) {
		
		int degree;
		
		switch (exifOrientation) {
		case ExifInterface.ORIENTATION_NORMAL:
			degree = 0;
			break;
		case ExifInterface.ORIENTATION_ROTATE_90:
			degree = 90;
			break;
		case ExifInterface.ORIENTATION_ROTATE_180:
			degree = 180;
			break;
		case ExifInterface.ORIENTATION_ROTATE_270:
			degree = 270;
			break;
		default:
			degree = 0;
			break;
		}
		
		return degree;
	}
	
}
