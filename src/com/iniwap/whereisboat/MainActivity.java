package com.iniwap.whereisboat;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import cn.zjditu.map.support.TLayer;
import cn.zjditu.map.support.TLayerFactory;
import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.MapView;
import com.esri.android.map.TiledLayer;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.android.runtime.ArcGISRuntime;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.PictureMarkerSymbol;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.iniwap.bestlocation.BestLocationProvider;
import com.iniwap.bestlocation.BestLocationListener;
import com.iniwap.bestlocation.BestLocationProvider.LocationType;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpGet;
import com.koushikdutta.async.http.AsyncHttpPost;
import com.koushikdutta.async.http.AsyncHttpResponse;
import com.koushikdutta.async.http.body.MultipartFormDataBody;
import com.koushikdutta.async.http.body.UrlEncodedFormBody;


public class MainActivity extends Activity{

	private static Context mContext;
	private static Resources mResManager;;
	
	private final static int VECTOR = 0;
	private final static int SATELLITE = 1;

	private int currentType = VECTOR;
	private boolean isOpen = true;
	///////////////////////////////////////////////////////////
	
	private String mAccount;
	private String mPwd;
	private boolean m_bAdmin = false;
	
	private ListView lv = null;
	private ListView offlv = null;

	//Account
	ArrayList<Account> mUserList = new ArrayList<Account>();
	
	private List<Item> list = null;
	private List<Item> offlist = null;

	private ListViewAdapter adapter = null;
	private ListViewAdapter offadapter = null;

	///////////////////////////////////////////////////////////

	private MapView mapView;
	private Point center = new Point(120.15067, 30.275483);
	
	private int     mCorrectCnt = 0;
	private boolean mFirstStart = true;
	private boolean mNeedCorrect = true;
	private Point mCurrentPos = new Point(0,0);
	private Point mPrevPos = new Point(0,0);
	private int   mPrevId = -1;
	private int   mCurrentId = -1;
	private long  mPrevGPSTime = 0;
	
	
	TiledLayer vect = TLayerFactory.Tdt_Vect_base();
	TiledLayer vectPoi = TLayerFactory.Tdt_Vect_poi();
	TiledLayer img = TLayerFactory.Tdt_Img_base();
	TiledLayer imgPoi = TLayerFactory.Tdt_Img_poi();

	GraphicsLayer graphicsLayer = new GraphicsLayer();
	
	private static Handler uploadHandler;
	private static Runnable uploadRunnable;
	
	private static Handler downloadHandler;
	private static Runnable downloadRunnable;
	
	//////for gps//////
	private BestLocationProvider mBestLocationProvider = null;
	private BestLocationListener mBestLocationListener = null;
	
	private static ProgressDialog progressDialog;
	
	///
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		mResManager = getResources();
		
		setContentView(R.layout.main);
		
		
		progressDialog = new ProgressDialog(MainActivity.this);
		progressDialog.setCancelable(false);
		progressDialog.setIndeterminate(true);
		progressDialog.setMessage("加载中...");
		progressDialog.show();
		
		
		
		//init///
		initUI();
		
		initMap();
		
		initUploadLocation();
		
		//管理员用户开启管理界面
		if(m_bAdmin){
			
			initDownloadUserList();
			
			initHttpData();
			
			initListView();
		}
		
		registerBoradcastReceiver();
		
		initLocation();
		mBestLocationProvider.startLocationUpdatesWithListener(mBestLocationListener);
		
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, 
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); 
		
	//	initLocation();
	//	mBestLocationProvider.startLocationUpdatesWithListener(mBestLocationListener);
		
		//
		//mapView.centerAt(center, true);
		findViewById(R.id.center_map).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				mapView.centerAt(mCurrentPos, true);
			}
		});
		
		/*
		findViewById(R.id.line).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				double[] points = {120.13996, 30.27819, 120.15365, 30.28060, 120.16185, 30.27467, 120.15399, 30.26815};
				Polyline line = new Polyline();
				for (int i = 0, len = points.length / 2; i < len; i++) {
					double x = points[i * 2], y = points[i * 2 + 1];
					Log.d("map", "point:" + x + "," + y);
					if (i == 0) {
						line.startPath(x, y);
					} else {
						line.lineTo(x, y);
					}
				}
				Graphic lineGraphic = new Graphic(line, new SimpleLineSymbol(Color.BLUE, 5, SimpleLineSymbol.STYLE.SOLID));
				graphicsLayer.addGraphic(lineGraphic);
			}
		});
		*/
	}
	//////////////////////init//////////////////////
	private void initUI(){
		
		TextView nameView = (TextView)findViewById(R.id.current_user);
		
		mAccount = getIntent().getStringExtra("username");
		mPwd = getIntent().getStringExtra("pwd");
		
		nameView.setText(mAccount);
		
		///if admin//
		String type = getIntent().getStringExtra("type");
		if(type.equals("1"))
		{
			//findViewById(R.id.clear).setVisibility(View.INVISIBLE);//also not support
			m_bAdmin = true;
			
			
			///////////listview button//////
			findViewById(R.id.expand_close).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					//
					if(!isOpen)
					{
											
						RelativeLayout listview= (RelativeLayout)findViewById(R.id.listview);
						
						Animation translateAnimation = AnimationUtils.loadAnimation(mContext, R.anim.animation_open_menu);
						translateAnimation.setFillAfter(true);
						listview.startAnimation(translateAnimation);
						translateAnimation.setAnimationListener(new TranslateAnimationListener());
					}
					else
					{
						
						RelativeLayout listview= (RelativeLayout)findViewById(R.id.listview);
						
						Animation translateAnimation = AnimationUtils.loadAnimation(mContext, R.anim.animation_close_menu); 
						listview.startAnimation(translateAnimation);
						translateAnimation.setFillAfter(true);
						translateAnimation.setAnimationListener(new TranslateAnimationListener());
					}
				}
			});
		}
		else
		{
			m_bAdmin = false;
			
			//not admin hide 
			//findViewById(R.id.clear).setVisibility(View.INVISIBLE);
			//findViewById(R.id.expand_close).setVisibility(View.INVISIBLE);
			findViewById(R.id.listview).setVisibility(View.INVISIBLE);
		//	findViewById(R.id.time_sel).setVisibility(View.INVISIBLE);
			//findViewById(R.id.location_sel).setVisibility(View.INVISIBLE);
			findViewById(R.id.expand_close).setVisibility(View.INVISIBLE);
		}
		
		////////fix
		findViewById(R.id.upload).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				//上传图片
				
		        Intent intent = new Intent();
		        intent.putExtra("longitude",String.valueOf(mCurrentPos.getX()));
		        intent.putExtra("latitude",String.valueOf(mCurrentPos.getY()));
		        
		        intent.setClass(MainActivity.this, UploadImageActivity.class);
		        MainActivity.this.startActivity(intent);
		        //MainActivity.this.finish();
			}
		});
		
		findViewById(R.id.infosettingbtn).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
		        Intent intent = new Intent();
		        intent.setClass(MainActivity.this, InfoSettingActivity.class);
		        MainActivity.this.startActivity(intent);
		        //MainActivity.this.finish();
			}
		});
		
		//
		//TextView cpyView = (TextView)findViewById(R.id.cptag);
		//cpyView.setTypeface(null, Typeface.BOLD_ITALIC);
		/*
		findViewById(R.id.clear).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				//清除痕迹
			}
		});	
		*/
		/*
		//time select
		findViewById(R.id.time_sel).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				//
				Toast.makeText(mContext, "选择时间", Toast.LENGTH_LONG).show();
			}
		});
		//locatin select
		findViewById(R.id.location_sel).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				//
				Toast.makeText(mContext, "选择地点", Toast.LENGTH_LONG).show();
			}
		});
		*/
	}
	
	private void initMap(){
		
		mCurrentPos = new Point(0, 0);
		
		ArcGISRuntime.setClientId("OMxL2qaDfBsFG6uK");
		mapView = (MapView) findViewById(R.id.map);
		mapView.addLayer(vect);
		mapView.addLayer(vectPoi);
		mapView.addLayer(img);
		mapView.addLayer(imgPoi);
		mapView.addLayer(graphicsLayer);
		mapView.setMaxResolution(TLayer.RESOLUTIONS[7]);
		
		
		mapView.setOnStatusChangedListener(new OnStatusChangedListener() {
			@Override
			public void onStatusChanged(Object o, STATUS status) {
				if (status == STATUS.INITIALIZED && o == mapView) {
					mapView.setResolution(TLayer.RESOLUTIONS[15]);
					//mapView.centerAt(center, true);

					setMapType(currentType);
				}
			}
		});
	}
	
	
	private void initDownloadUserList(){
		downloadHandler = new Handler();
		downloadRunnable = new Runnable() {
		    @Override
		    public void run() {
		        // TODO Auto-generated method stub
		    	downloadUserList();
		    }
		};
	}
	private void initUploadLocation(){
		uploadHandler = new Handler();
		uploadRunnable = new Runnable() {
		    @Override
		    public void run() {
		        // TODO Auto-generated method stub
				double distance = GeometryEngine.geodesicDistance(mCurrentPos, mPrevPos,mapView.getSpatialReference(), null);
				
				//
				//uploadLocation();
				
				//here may be no must
				if(distance < AppConstant.MIN_POST_DISTANCE){
					//no post//no draw
				}else{
					//UPLOAD_LOCATION_URL
					uploadLocation();
				}
		    }
		};
	}
	private void initHttpData(){
		
		//
		downloadHandler.postDelayed(downloadRunnable, AppConstant.REFRESH_USERLIST_INTERVAL);
		//
		downloadUserList();
		
	}
	private void downloadUserList(){
		AsyncHttpGet get = new AsyncHttpGet(AppConstant.GET_USER_LIST_URL);
		
		//here we make first refresh
		AsyncHttpClient.getDefaultInstance().executeJSONObject(get, new AsyncHttpClient.JSONObjectCallback() {
		    // Callback is invoked with any exceptions/errors, and the result, if available.
		    @Override
		    public void onCompleted(Exception e, AsyncHttpResponse response, JSONObject result) {
		        if (e != null) {
		            e.printStackTrace();
		           // return;
		        }
		        
		        //first clear 
		        mUserList.clear();
		        Account acc1 = new Account("人员-1","",1);
		        Account acc2 = new Account("人员-2","",1);
		        Account acc3 = new Account("人员-3","",1);
		        Account acc4 = new Account("人员-4","",1);
		        
		        Account acc5 = new Account("人员-5","",1);
		        Account acc6 = new Account("人员-6","",1);
		        Account acc7 = new Account("人员-7","",1);
		        Account acc8 = new Account("人员-8","",1);
		        Account acc9 = new Account("人员-9","",1);
		        
		        acc1.is_online = true;
		        acc2.is_online = true;
		        acc5.is_online = true;
		        acc6.is_online = true;
		        acc8.is_online = true;

		        
		        acc3.is_online = false;
		        acc4.is_online = false;
		        acc7.is_online = false;
		        acc9.is_online = false;

		        mUserList.add(acc1);
		        mUserList.add(acc2);
		        mUserList.add(acc3);
		        mUserList.add(acc4);
		        mUserList.add(acc5);
		        mUserList.add(acc6);
		        mUserList.add(acc7);
		        mUserList.add(acc8);
		        mUserList.add(acc9);
		        
		        //here we need move online user to first
		        sortUserList();//here server should give sorted array
		        //here we parse json
		        //parseJson(result);
		        
		        refreshUserListUI();
		    }
		});
	}
	private void sortUserList(){
		for(int i = 0;i<mUserList.size();i++){
			
		}
	}
	private void refreshUserListUI(){
		
		list.clear();
		offlist.clear();
		
		Message msg = new Message();  
        msg.what = REFRESH_LIST_VIEW_MSG;
        uiHandler.sendMessage(msg);
	}
	private void uploadLocation(){
		//login
		AsyncHttpPost post = new AsyncHttpPost(AppConstant.UPLOAD_LOCATION_URL);
		
		ArrayList<NameValuePair> pairs = new ArrayList<NameValuePair>();
        pairs.add(new BasicNameValuePair("logid", AppConstant.LOGID));
        pairs.add(new BasicNameValuePair("loglong", String.valueOf(mCurrentPos.getX())));
        pairs.add(new BasicNameValuePair("loglati", String.valueOf(mCurrentPos.getY())));
        pairs.add(new BasicNameValuePair("logtime",getTimeStamp()));
        
        
        UrlEncodedFormBody writer = new UrlEncodedFormBody(pairs);
		post.setBody(writer);
		
		//here we make first refresh
		AsyncHttpClient.getDefaultInstance().executeString(post, new AsyncHttpClient.StringCallback() {
		    // Callback is invoked with any exceptions/errors, and the result, if available.
		    @Override
		    public void onCompleted(Exception e, AsyncHttpResponse response, String result) {

		    	if (e != null) {
			    	//Message msg1 = new Message();  
			        //msg1.what = LOGIN_FAIL;
			        //uiHandler.sendMessage(msg1);
		        	return;
		        }
		    	
		    	//upload ok
		        List<Map<String, String>> list = LoginActivity.getData(result);
		        if(!list.isEmpty()){
		        	String isError = list.get(0).get("err");
		        	if(isError != null && isError.length() != 0){
		        		
		        		//logout
		        		Intent intent = new Intent();
				        intent.setClass(MainActivity.this, LoginActivity.class);
				        MainActivity.this.startActivity(intent);
				        MainActivity.this.finish();
				        
				        //
						AsyncHttpPost post = new AsyncHttpPost(AppConstant.LOGOUT_URL);
						
						ArrayList<NameValuePair> pairs = new ArrayList<NameValuePair>();
				        pairs.add(new BasicNameValuePair("unum", getIMEI()));

				        
				        UrlEncodedFormBody writer = new UrlEncodedFormBody(pairs);
						post.setBody(writer);
						
						//here we make first refresh
						AsyncHttpClient.getDefaultInstance().executeString(post, new AsyncHttpClient.StringCallback() {
						    // Callback is invoked with any exceptions/errors, and the result, if available.
						    @Override
						    public void onCompleted(Exception e, AsyncHttpResponse response, String result) {

						    	if (e != null) {
							    	//Message msg1 = new Message();  
							        //msg1.what = LOGIN_FAIL;
							        //uiHandler.sendMessage(msg1);
						        	return;
						        }
						    					    	
						    	//Log.e("DEBUG", result);
						    }
						});
		        	}
		        }
		    //	Log.e("DEBUG", result);
		    }
		});
	}
	
	public String parseJson(JSONObject result){
		
		//mUserList add to
		
       // String jsonString = "{\"FLAG\":\"flag\",\"MESSAGE\":\"SUCCESS\",\"name\":[{\"name\":\"jack\"},{\"name\":\"lucy\"}]}";//json字符串  
        try {  
            //JSONObject result = new JSONObject(jsonString);//转换为JSONObject  
            int num = result.length();  
            JSONArray nameList = result.getJSONArray("name");//获取JSONArray  
            int length = nameList.length();  
            String aa = "";  
            for(int i = 0; i < length; i++){//遍历JSONArray  
                JSONObject oj = nameList.getJSONObject(i);  
                aa = aa + oj.getString("name")+"|";  
                  
            }  
            Iterator<?> it = result.keys();  
            String aa2 = "";  
            String bb2 = null;  
            while(it.hasNext()){//遍历JSONObject  
                bb2 = (String) it.next().toString();  
                aa2 = aa2 + result.getString(bb2);  
                  
            }  
            return aa;  
        } catch (JSONException e) {  
            throw new RuntimeException(e);  
        }  
    }
   
	////
	private void initLocation(){
		
		if(mBestLocationListener == null){
			mBestLocationListener = new BestLocationListener() {
				
				@Override
				public void onStatusChanged(String provider, int status, Bundle extras) {
					//Log.i(TAG, "onStatusChanged PROVIDER:" + provider + " STATUS:" + String.valueOf(status));
				}
				
				@Override
				public void onProviderEnabled(String provider) {
					//Log.i(TAG, "onProviderEnabled PROVIDER:" + provider);
				}
				
				@Override
				public void onProviderDisabled(String provider) {
					//Log.i(TAG, "onProviderDisabled PROVIDER:" + provider);
				}
				
				@Override
				public void onLocationUpdateTimeoutExceeded(LocationType type) {
					//Log.w(TAG, "onLocationUpdateTimeoutExceeded PROVIDER:" + type);
				}
				
				@Override
				public void onLocationUpdate(Location location, LocationType type,
						boolean isFresh) {
					//Log.i(TAG, "onLocationUpdate TYPE:" + type + " Location:" + mBestLocationProvider.locationToString(location));
					//Log.d(TAG,new Date().toLocaleString() + "\nLOCATION UPDATE: isFresh:" + String.valueOf(isFresh) + "\n" + mBestLocationProvider.locationToString(location) );
					
					
					Point newPos = new Point(location.getLongitude(),location.getLatitude());

					//0xff20b2aa
					//Graphic pointGraphic = new Graphic(newPos, new SimpleMarkerSymbol(0xff0000aa, 10, SimpleMarkerSymbol.STYLE.CIRCLE));
					
					/*
					Drawable boatImage = mResManager.getDrawable(R.drawable.point_1);
					boatImage.setBounds(new Rect(0,10,10,0));
					boatImage.setColorFilter(0xFFFF0000, Mode.MULTIPLY);
					Graphic pointGraphic = new Graphic(newPos, new PictureMarkerSymbol(mContext,boatImage));
					*/
					//graphicsLayer.addGraphic(pointGraphic);
					
					
					//graphicsLayer.removeGraphic((int) pointGraphic.getId());
					
					//correct the pos
					if(mFirstStart)
					{
						if(mCurrentPos.getX() != 0 )
						{
							double dis = GeometryEngine.geodesicDistance(mCurrentPos, newPos,mapView.getSpatialReference(), null);
							if((dis <= AppConstant.MIN_POST_DISTANCE && mCorrectCnt > 5)
									||mCorrectCnt > 10)//<=20 CORRECT
							{
								mFirstStart = false;
								mCorrectCnt = 0;
								
							}
							mCorrectCnt++;
						}
						
						mCurrentPos = newPos;
						mPrevGPSTime = location.getTime();
						
						return;
					}
					
					//correct
					double dis1 = GeometryEngine.geodesicDistance(mCurrentPos, newPos,mapView.getSpatialReference(), null);

					if(mNeedCorrect)
					{						
						if(!location.hasSpeed()){
							if(dis1 > AppConstant.MIN_POST_DISTANCE/2){
								mCurrentPos = newPos;
								return;
							}
						}
						
						long time = (location.getTime() - mPrevGPSTime)/1000;
						int speed = (int) (dis1/time);
						if(speed > AppConstant.MAX_SPEED){
							//ignore this pos
							mCurrentPos = newPos;
							return;
						}
						else
						{
							mNeedCorrect = false;
							//first //we make it start point
							Drawable boatImage = mResManager.getDrawable(R.drawable.pos_start);
							
							boatImage.setBounds(new Rect(0,10,10,0));
							boatImage.setColorFilter(0xFFFF0000, Mode.MULTIPLY);
							Graphic pointGraphic = new Graphic(newPos, new PictureMarkerSymbol(mContext,boatImage));
							
							graphicsLayer.addGraphic(pointGraphic);
							
							mapView.centerAt(newPos, true);
							
							progressDialog.hide();//
						}
					}
					
					
					//
					/*
					if(location.hasSpeed()){
						if(location.getSpeed() > AppConstant.MAX_SPEED){
							return;
						}
					}
					*/
					
					if(!location.hasSpeed()){
						if(dis1 > AppConstant.MIN_POST_DISTANCE){
							return;
						}
					}
					
					if(location.hasSpeed()){
						if(location.getSpeed() > AppConstant.MAX_SPEED*2){
							return;
						}
					}
					
					EditText n =  (EditText)findViewById(R.id.NLocation);
					EditText e =  (EditText)findViewById(R.id.ELocation);
					e.setText("E:" +String.format("%.4f",location.getLongitude()));
					n.setText("N:" + String.format("%.4f",location.getLatitude()));
					
					
					mPrevPos = mCurrentPos;
					mCurrentPos = newPos;
					
					//CENTER MAP
					mapView.centerAt(mCurrentPos, true);

					
					//Log.e("map", "pp:" + mPrevPos.getX() + "," + mPrevPos.getX() + "->cp:"+mCurrentPos.getX() + "," + mCurrentPos.getY());

					
					if(mPrevPos.getX() != 0 && (mCurrentPos.getX() != mPrevPos.getX() && mCurrentPos.getY() != mPrevPos.getY())){
						double[] points = {mPrevPos.getX(),mPrevPos.getY(),mCurrentPos.getX(),mCurrentPos.getY()};
						Polyline line = new Polyline();
						for (int i = 0, len = points.length / 2; i < len; i++) {
							double x = points[i * 2], y = points[i * 2 + 1];
							if (i == 0) {
								line.startPath(x, y);
							} else {
								line.lineTo(x, y);
							}
						}
						Graphic lineGraphic = new Graphic(line, new SimpleLineSymbol(0xdd4444aa, 6, SimpleLineSymbol.STYLE.SOLID));
						graphicsLayer.addGraphic(lineGraphic);
						
						///add two point to smooth line
						Graphic startPoint = new Graphic(mPrevPos, new SimpleMarkerSymbol(0xff4444aa, 6, SimpleMarkerSymbol.STYLE.CIRCLE));
						Graphic endPoint = new Graphic(mCurrentPos, new SimpleMarkerSymbol(0xff4444aa, 6, SimpleMarkerSymbol.STYLE.CIRCLE));
						graphicsLayer.addGraphic(startPoint);
						graphicsLayer.addGraphic(endPoint);
						
						//////////////////
						//add current location mark
						
						mPrevId = mCurrentId;
						
						//first //we make it start point
						Drawable boatImage = mResManager.getDrawable(R.drawable.ic_current_location);
						
						//
						/*
						Matrix mMatrix = new Matrix();
						float degree = 0;
						mMatrix.reset();
						if(mCurrentPos.getX() -  mPrevPos.getX() == 0){
							if(mCurrentPos.getY() -  mPrevPos.getY() < 0){
								degree = 270;
							}
							else if(mCurrentPos.getY() -  mPrevPos.getY() > 0){
								degree = 90;
							}
						}
						else{
							degree = (float) Math.atan((mCurrentPos.getY() -  mPrevPos.getY())/(mCurrentPos.getX() -  mPrevPos.getX()));
						}
						
						mMatrix.setRotate(degree,
								boatImage.getBounds().width()/2,
								boatImage.getBounds().height()/2);
						
						BitmapDrawable bd = (BitmapDrawable) boatImage;
						Bitmap mBitmapRotate = Bitmap.createBitmap(bd.getBitmap(), 0, 0, bd.getBounds().width(), bd.getBounds().height(), mMatrix, true);
						Drawable drawable = new BitmapDrawable(mBitmapRotate);
						
						*/
						
						Graphic pointGraphic = new Graphic(newPos, new PictureMarkerSymbol(mContext,boatImage));
						
						mCurrentId = graphicsLayer.addGraphic(pointGraphic);
						if(mPrevId != -1){
							graphicsLayer.removeGraphic(mPrevId);
						}
					}					
					
					if(mPrevPos.getX() == 0){
						uploadHandler.postDelayed(uploadRunnable, AppConstant.POST_LOCATION_TIME_INTERVAL);
					}else{
						
						uploadHandler.postDelayed(uploadRunnable, AppConstant.POST_LOCATION_TIME_INTERVAL);
					}
				}
			};
			
			if(mBestLocationProvider == null){
				mBestLocationProvider = new BestLocationProvider(this, true, true, 10000, 1000, 5000, 0);
			}
		}
	}
	private void setDrawableColor(String color,Drawable drawable){
		int iColor = Color.parseColor(color);

        int red = (iColor & 0xFF0000) / 0xFFFF;
        int green = (iColor & 0xFF00) / 0xFF;
        int blue = iColor & 0xFF;

        float[] matrix = { 0, 0, 0, 0, red
                         , 0, 0, 0, 0, green
                         , 0, 0, 0, 0, blue
                         , 0, 0, 0, 1, 0 };

        ColorFilter colorFilter = new ColorMatrixColorFilter(matrix);
        drawable.setColorFilter(colorFilter);
	}
	private void setMapType(int type) {
		vect.setVisible(type == VECTOR);
		vectPoi.setVisible(vect.isVisible());

		img.setVisible(type == SATELLITE);
		imgPoi.setVisible(img.isVisible());
		currentType = type;
	}
	
	public static Context getContext(){
		return mContext;
	}
	
	@Override
	protected void onResume() {		
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		
		super.onPause();
	}
	@Override
	protected void onDestroy() {
		
		mBestLocationProvider.stopLocationUpdates();
		
		uploadHandler.removeCallbacks(uploadRunnable);
		//downloadHandler.removeCallbacks(downloadRunnable);
		
		super.onDestroy();
	}
	
////////////////////////animation////////////
	private class TranslateAnimationListener implements AnimationListener{

        public void onAnimationEnd(Animation animation) {
            // TODO Auto-generated method stub
        	
        	Button btn = (Button)findViewById(R.id.expand_close);
        	if(isOpen)
        	{
        		//listview
        		btn.setText("展开");
        	}
        	else
        	{
        		btn.setText("隐藏");
        	}
        	
        	isOpen = !isOpen;
        }

        public void onAnimationRepeat(Animation animation) {
            // TODO Auto-generated method stub
            
        }

        public void onAnimationStart(Animation animation) {
            // TODO Auto-generated method stub
            
        }
        
    }
	
	//////////////list view///////////////////

	private void initView() {
		
		//online
		lv = (ListView) this.findViewById(R.id.lv);
		lv.setVerticalScrollBarEnabled(false);

		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				Item item = list.get(arg2);
				item.b = !item.b;// 
				initAdapter();
			}
		});
		
		//offline
		offlv = (ListView) this.findViewById(R.id.offlinelv);
		offlv.setVerticalScrollBarEnabled(false);

		offlv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				Item item = offlist.get(arg2);
				item.b = !item.b;// 
				initAdapter();
			}
		});
	}

	private void init() {
		//first clear
		if(list == null){
			list = new ArrayList<Item>();
			offlist = new ArrayList<Item>();
		}else{
			list.clear();
			offlist.clear();
		}
		
		for (int i = 0;i < mUserList.size();i++) {
			if(mUserList.get(i).is_online){
				list.add(new Item(mUserList.get(i).name, false,mUserList.get(i).is_online));
			}else{
				offlist.add(new Item(mUserList.get(i).name, false,mUserList.get(i).is_online));
			}
		}
		initAdapter();
	}

	public void initAdapter() {
		if (adapter == null) {
			adapter = new ListViewAdapter(list);
			offadapter = new ListViewAdapter(offlist);
			lv.setAdapter(adapter);
			offlv.setAdapter(offadapter);

		} else {
			adapter.notifyDataSetChanged();
			offadapter.notifyDataSetChanged();
		}
	}

	class ListViewAdapter extends BaseAdapter {
		
		private List<Item> dlist = null;
		public ListViewAdapter(List<Item> dlist){
			this.dlist = dlist;
		}
		
		@Override
		public int getCount() {
			return dlist.size();
		}

		@Override
		public Item getItem(int arg0) {
			return dlist.get(arg0);
		}

		@Override
		public long getItemId(int arg0) {
			return arg0;
		}

		@Override
		public View getView(int position, View view, ViewGroup arg2) {
			ViewHolder holder = null;
			Item item = getItem(position);

			if (view == null) {
				holder = new ViewHolder();
				
				if(item.isOnline){
					view = View.inflate(MainActivity.this, R.layout.listviewitem, null);
					holder.tv = (TextView) view.findViewById(R.id.item_tv);
					holder.cb = (CheckBox) view.findViewById(R.id.item_cb);
				}else{
					view = View.inflate(MainActivity.this, R.layout.offlinelistviewitem, null);
					holder.tv = (TextView) view.findViewById(R.id.off_item_tv);
					holder.cb = (CheckBox) view.findViewById(R.id.off_item_cb);
				}
				
				view.setTag(holder);
			} else {
				holder = (ViewHolder) view.getTag();
			}
			
			holder.tv.setText(item.name);
			holder.cb.setChecked(item.b);
			return view;
		}
	}

	class Item {
		public String name;
		public boolean b = false;
		public boolean isOnline = true;

		public Item(String name, boolean b,boolean isOnline) {
			this.name = name;
			this.b = b;
			this.isOnline = isOnline;
		}
	}

	public class ViewHolder {
		public TextView tv = null;
		public CheckBox cb = null;
	}
	
	public void initListView(){
		initView();
		init();
	}
	//////////////////////////////exit////////////////
	@Override  
    public boolean onKeyDown(int keyCode, KeyEvent event)  
    {  
        if (keyCode == KeyEvent.KEYCODE_BACK )  
        {  
            // 创建退出对话框  
            AlertDialog isExit = new AlertDialog.Builder(this).create();  
            // 设置对话框标题  
            isExit.setTitle("温馨提示");  
            // 设置对话框消息  
            isExit.setMessage("确定要退出吗");  
            // 添加选择按钮并注册监听  
            isExit.setButton("确定", listener);  
            isExit.setButton2("取消", listener);  
            // 显示对话框  
            isExit.show();  
  
        }  
          
        return false;     
    }
	/**监听对话框里面的button点击事件*/  
    DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener()  
    {  
        public void onClick(DialogInterface dialog, int which)  
        {  
            switch (which)  
            {  
            case AlertDialog.BUTTON_POSITIVE:// "确认"按钮退出程序  
            	
            	Intent intent = new Intent(mContext,UploadService.class);
            	stopService(intent);
            	
                finish();  
                break;  
            case AlertDialog.BUTTON_NEGATIVE:// "取消"第二个按钮取消对话框  
                break;  
            default:  
                break;  
            }  
        }  
    };
    ///////UI thread/////
    public final static int REFRESH_LIST_VIEW_MSG = 1;
	@SuppressLint("HandlerLeak")
	public  Handler uiHandler = new Handler(){       
        public void handleMessage(Message msg){
        	
        	switch(msg.what){
        	case REFRESH_LIST_VIEW_MSG:
        		{
        			initListView();
        		}
        		break;
        	}
       }
	};
	
	public static String getTimeStamp(){
		//2015-4-21 19:35:45		
	    Date date = new Date(System.currentTimeMillis());  
	    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.getDefault());
	    
	    return dateFormat.format(date);  	    
	}
	
	public String getIMEI(){
		TelephonyManager telephonyManager = (TelephonyManager) (this)
				.getSystemService(Context.TELEPHONY_SERVICE);
		
		String imei = telephonyManager.getDeviceId();
		if(imei == null || imei.length() == 0)
		{
			imei = UUID.randomUUID().toString();
		}
		return imei;
	}
	
	////finish
	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver(){
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(action.equals("finish")){
				unregisterReceiver(mBroadcastReceiver);
				finish();
			}
		}
		
	};

	
	public void registerBoradcastReceiver(){
		IntentFilter myIntentFilter = new IntentFilter();
		myIntentFilter.addAction("finish");
		//注册广播      
		registerReceiver(mBroadcastReceiver, myIntentFilter);
	}

}
