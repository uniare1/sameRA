package com.garooyaproject.hismera;

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
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener
											, LocationListener 
											, AutoFocusCallback{

	private Camera mCamera;
	private CameraPreview mPreview;
	private LocationManager mLocationManager;
	private Location mLocation;
//	private LayoutParams mLayoutParams;
//	private Point mScreenSize;
//	private boolean mIsSmallImageView = true;
	
	private ReferenceImage mReferenceImage;
	
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // GPS
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        String locationProvider = mLocationManager.getBestProvider(criteria, true);
        Log.d("Provider", locationProvider);
        
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
        
        mCamera = Camera.open();
        
        // config params
        Camera.Parameters cameraParams = mCamera.getParameters();
        
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
        mPreview.setFitsSystemWindows(true);
        
        FrameLayout fl = (FrameLayout) findViewById(R.id.camera_preview);

        fl.addView(mPreview);
        
        Button button = (Button) findViewById(R.id.button1);
        button.setOnClickListener(this);
        
        Point screenSize = new Point(960, 720);
        getWindowManager().getDefaultDisplay().getSize(screenSize);

        ImageView imageView = (ImageView) findViewById(R.id.imageView1);

        mReferenceImage = new ReferenceImage(imageView, screenSize);        
        mReferenceImage.setOnClickListener(this);
        
        // TODO: has to change
        // 해당 위치에 있는 기존의 사진
        Bitmap bitmap = getImage(mLocation, 1);
        mReferenceImage.setImageBitmap(bitmap);
        
        // TODO: setting alarm
        // booting시 동작, intent받을 곳?
        Intent intent = new Intent(this, com.garooyaproject.hismera.AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP
        		, System.currentTimeMillis()
        		, AlarmManager.INTERVAL_HALF_HOUR
        		, pendingIntent);     
        
    }

    @Override
    protected void onPause() {
    	super.onPause();
    	releaseCamera();
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	mLocationManager.removeUpdates(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
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
			File pictureFile = getOutputMediaFile();
			
			String path = pictureFile.getAbsolutePath();
			
			OutputStream os;
			try {
				os = new FileOutputStream(pictureFile);
				os.write(data);
				os.flush();
				os.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
//			writeEXIF(path, mLocation);

			ExifInterface exif = null;
			try {
				exif = new ExifInterface(path);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			String dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME);
			long time = 0;
			try {
				time = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault()).parse(dateTime).getTime();
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			ContentValues values = new ContentValues(2);
			values.put(MediaStore.Images.Media.DATA, path);
			values.put(MediaStore.Images.Media.LATITUDE, mLocation.getLatitude());
			values.put(MediaStore.Images.Media.LONGITUDE, mLocation.getLongitude());
			values.put(MediaStore.Images.Media.DATE_TAKEN, time);
		
			getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
			
			mCamera.startPreview();
			
	        Bitmap bitmap = getImage(mLocation, 1);
	        mReferenceImage.setImageBitmap(bitmap);
			
			
		}
	};
	
	private static File getOutputMediaFile() {
		File mediaStorageDir =  new File(
				Environment.getExternalStoragePublicDirectory(
						Environment.DIRECTORY_PICTURES), "GarooHismera");
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
	public void onClick(View v) {
		if(R.id.button1 == v.getId()) {
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
			
		}
		
	}
	
	private void releaseCamera() {
		if(null != mCamera) {
			mCamera.release();
			mCamera = null;
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		Log.d("Location", location.toString());
		mLocation = location;		
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		Log.d("onProviderDisabled", provider);
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		Log.d("onProviderEnabled", provider);
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
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
		
		return BitmapFactory.decodeFile(path);
	}

	@Override
	public void onAutoFocus(boolean success, Camera camera) {
		if(success) {
			camera.takePicture(null, null, mPictureCallback);
		}
		
	}
	

//	private void writeEXIF(String path, Location location) {
//		ExifInterface exif = null;
//		try {
//			exif = new ExifInterface(path);
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//		
//		// when the photo is taken
//		exif.setAttribute(ExifInterface.TAG_DATETIME, 
//				DateFormat.format("yyyy:MM:dd HH:mm:ss", System.currentTimeMillis()).toString());
//		
//		// where the photo is taken
//		double latitude = location.getLatitude();
//		double longitude = location.getLongitude();
//		
//		int num1Lat = (int)Math.floor(latitude);
//		int num2Lat = (int)Math.floor((latitude - num1Lat) * 60);
//		double num3Lat = (latitude - ((double)num1Lat+((double)num2Lat/60))) * 3600000;
//		
//		int num1Lon = (int)Math.floor(longitude);
//		int num2Lon = (int)Math.floor((longitude - num1Lon) * 60);
//		double num3Lon = (longitude - ((double)num1Lon+((double)num2Lon/60))) * 3600000;
//		
//		exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, num1Lat+"/1,"+num2Lat+"/1,"+num3Lat+"/1000");
//		exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, num1Lon+"/1,"+num2Lon+"/1,"+num3Lon+"/1000");
//		
//		if (latitude > 0) {
//		    exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N"); 
//		} else {
//		    exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "S");
//		}
//		
//		if (longitude > 0) {
//		    exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "E");    
//		} else {
//		exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "W");
//		}
//		
//		
//		try {
//			exif.saveAttributes();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//	}
}
