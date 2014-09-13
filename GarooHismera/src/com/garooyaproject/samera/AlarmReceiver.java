package com.garooyaproject.samera;

import java.util.Date;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.provider.MediaStore;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {

	private Context mContext;
	
	@Override
	public void onReceive(Context context, Intent intent) {
	    // TODO Auto-generated method stub
		mContext = context;
	}

	
	private boolean existPhoto(Location location, double radius) {
		
		String where = "latitude < " + String.valueOf(location.getLatitude() + radius) + 
				" AND latitude > " + String.valueOf(location.getLatitude() - radius)
				+ " AND longitude < " + String.valueOf(location.getLongitude() + radius) + 
				" AND longitude > " + String.valueOf(location.getLongitude() - radius)
				+ " AND datetaken is not null";
		
		String orderBy = MediaStore.Images.Media.DATE_TAKEN + " DESC";
		
		
		
		Cursor cursor = MediaStore.Images.Media.query(mContext.getContentResolver(), 
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
		
		return true;
	}
}
