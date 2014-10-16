package com.garooyaproject.samera;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

import android.os.Environment;

// The rule of naming : SAMERA_OriginName_timestamp.extention
// timestamp : 15 digits
public class FileManager {
	private static final String TAG = "FileManager";
	public static final String PREFIX = "SAMERA";
	public static final String DELIMITER = "_";
	private static final long DIGITS = 1000000000000000L;
	private static final String DATE_PATTERN = "yyyyMMdd-HHmmss.SSS";
	private static final String EXTENTION = "jpg";
	private static final String DEFAULT_NAME = "IMG";
	private final static String ROOT_DIR = "sameRA";
	
	private File mFile;
	private File mChildFile;
	
	public FileManager(String path) {
		if(path == null) {
			mFile = null;
		} else {
			mFile = new File(path);
		}
	}
	
	public File getFile() {
		return mFile;
	}
	
	public void setFile(File file) {
		mFile = file;
	}
	
	public File getChildFile() {
		return mChildFile;
	}
	
	public File createChild() {
		if(mFile == null) {
			mChildFile = getOutputMediaFile();
		} else {
			String path = mFile.getAbsolutePath().substring(0, mFile.getAbsolutePath().lastIndexOf(File.separator) + 1) + createFileName();
			mChildFile = new File(path);
		}	
		
		return mChildFile;
	}
	
	private String createFileName() {
		String timeStamp = new SimpleDateFormat(DATE_PATTERN, Locale.getDefault()).format(new Date());
		String fileName = mFile.getName();
		StringBuilder targetName = new StringBuilder(PREFIX).append(DELIMITER);
		if(fileName.startsWith(PREFIX + DELIMITER)) {
			// child file
			// update time stamp
			targetName.append(fileName.substring(PREFIX.length() + DELIMITER.length(), fileName.lastIndexOf(DELIMITER) + 1));
		} else {
			// parent file
			// create file with PREFIX and timestamp
			targetName.append(fileName.subSequence(0, fileName.lastIndexOf("."))).append(DELIMITER);
		}
		
		targetName.append(timeStamp).append(".").append(EXTENTION);
		
		return targetName.toString();
	}
	
	
	private static File getOutputMediaFile() {
		File mediaStorageDir =  new File(
				Environment.getExternalStoragePublicDirectory(
						Environment.DIRECTORY_PICTURES), ROOT_DIR);
		if(!mediaStorageDir.exists()) {
			if(!mediaStorageDir.mkdirs()) {
				return null;
			}
		}
		
		String name = new BigInteger(32, new Random()).toString(32).toUpperCase(Locale.US);
		
		
		String timeStamp = new SimpleDateFormat(DATE_PATTERN, Locale.getDefault()).format(new Date());
		File mediaFile = new File(mediaStorageDir, PREFIX + DELIMITER + name + DELIMITER + timeStamp + ".jpg");
		
		try {
			mediaFile.createNewFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return mediaFile;
	}
}
