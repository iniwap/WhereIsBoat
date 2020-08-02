package com.iniwap.whereisboat;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

import com.iniwap.whereisboat.Account;

public class DataBaseManage {

	public static final int VERSION = 3;
	public static final String DATABASE = "mdb";
	
	public static final String KEY_NAME = "name";
	public static final String KEY_PASSWORD = "password";
	public static final String KEY_TYPE = "type";
	public static final String KEY_ID = "id";
	
	//for location
	//name-account
	public static final String KEY_TIME = "timestamp";
	public static final String KEY_LONGITUDE = "longitude";
	public static final String KEY_LATITUDE = "latitude";
	public static final String KEY_LOACTION_NAME = "locationname";
	
	public static final String TABLE_ACCOUNT = "account";
	public static final String TABLE_LOCATION = "location";
	
	public static final String SQL_TABLE_CREATE = "CREATE TABLE "
			+ TABLE_ACCOUNT + "(" + KEY_NAME + " TEXT PRIMARY KEY,"
			+ KEY_PASSWORD + " TEXT," + KEY_TYPE + " INTEGER)";
	
	public static final String SQL_TABLE_LOCATION = "CREATE TABLE " + TABLE_LOCATION
			+ "(" + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + KEY_NAME + "TEXT,"+KEY_LONGITUDE + " DOUBLE,"
			+ KEY_LATITUDE + " DOUBLE,"+ KEY_LOACTION_NAME + " TEXT,"+ KEY_TIME
			+ " TIMESTAMP NOT NULL DEFAULT (datetime('now','localtime')))";
	
	private Context context;
	private DataBaseHelper helper;
	private SQLiteDatabase database;

	public DataBaseManage(Context context) {
		this.context = context;
	}

	public void open() {
		if (helper == null)
			helper = new DataBaseHelper(context, DATABASE, null, VERSION);
		database = helper.getWritableDatabase();
	}

	public void close() {
		helper.close();
		database.close();
	}
	
	/**
	 * @param numid
	 */
	public void insertLocation(double longitude,double latitude,String account,String location){
		ContentValues content=new ContentValues();
		content.put(KEY_LONGITUDE, longitude);
		content.put(KEY_LATITUDE, latitude);
		content.put(KEY_LOACTION_NAME, location);
		content.put(KEY_NAME, account);

		database.insert(TABLE_LOCATION, KEY_NAME, content);

	}
	
	/**
	 * @return
	 */
	public double[] getLocation(String account){
		Cursor cs=database.query(TABLE_LOCATION, new String[]{KEY_NAME}, null, null, null, null, null);
		if(cs.getCount()==0){
			cs.close();
			return null;
		}
		double[] value=new double[cs.getCount()];
		int i=0;
		cs.moveToFirst();
		do{
			value[i]=cs.getDouble(0);
			i++;
		}while(cs.moveToNext());
		cs.close();
		return value;
	}

	public void Insert(String name, String password, int type) {
		ContentValues cv = new ContentValues();
		cv.put(KEY_NAME, name);
		cv.put(KEY_PASSWORD, password);
		cv.put(KEY_TYPE, type);
		
		ContentValues content=new ContentValues();
		content.put(KEY_NAME, name);
		Cursor cs=database.query(TABLE_ACCOUNT, new String[]{KEY_NAME}, KEY_NAME+"='"+name+"'", null, null, null, null);
		
		if(cs.getCount()==0){
			database.insert(TABLE_ACCOUNT, KEY_NAME, cv);		
		}
		
		cs.close();
	}

	
	public Account[] fetchAllAccountRecord(){
		Cursor cs=database.query(TABLE_ACCOUNT,new String[] {KEY_NAME,
				KEY_PASSWORD,KEY_TYPE}, null, null, null, null, null);
		if(cs.getCount()==0){
			cs.close();
			return null;
		}
		cs.moveToFirst();
		int len=cs.getCount();
		Account[] result=new Account[len];
		int i=0;
		do{
			result[i]=new Account(cs.getString(0), cs.getString(1), cs.getInt(2));
			i++;
		}while(cs.moveToNext());
		cs.close();
		return result;
	}

	public Account fetchAccount(String name){
		Cursor cs=database.query(true, TABLE_ACCOUNT, new String[] { KEY_NAME,
				KEY_PASSWORD, KEY_TYPE}, KEY_NAME + "='" + name + "'", null,
				null, null, null, null);
		cs.moveToFirst();
		if(cs.getCount()==0){
			cs.close();
			return null;
		}
		Account result=new Account(cs.getString(0), cs.getString(1), cs.getInt(2));
		cs.close();
		return result;
	}

	public void delete(String name) {
		try {
			database.delete(TABLE_ACCOUNT, KEY_NAME + "='" + name + "'", null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

	public class DataBaseHelper extends SQLiteOpenHelper {

		public DataBaseHelper(Context context, String name,
				CursorFactory factory, int version) {
			super(context, name, factory, version);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(SQL_TABLE_CREATE);
			db.execSQL(SQL_TABLE_LOCATION);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	
		}

	}
}
