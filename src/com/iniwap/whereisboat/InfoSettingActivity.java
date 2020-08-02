package com.iniwap.whereisboat;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Media;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;
import cn.zjditu.map.support.TLayer;
import cn.zjditu.map.support.TLayerFactory;

import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.MapView;
import com.esri.android.map.TiledLayer;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.android.map.event.OnStatusChangedListener.STATUS;
import com.esri.android.runtime.ArcGISRuntime;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polyline;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.PictureMarkerSymbol;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpPost;
import com.koushikdutta.async.http.AsyncHttpResponse;
import com.koushikdutta.async.http.body.UrlEncodedFormBody;

public class InfoSettingActivity extends Activity {
	
	private static Context mContext;

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.infosetting);//here to be fix
		
		mContext = this;
		
		TextView ver = (TextView)findViewById(R.id.version_num);
		ver.setText("当前版本:"+ getVersion());
		
		findViewById(R.id.backbtn).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				InfoSettingActivity.this.finish();
			}
		});
		
		findViewById(R.id.logout).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				//上传图片
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
				  
						Intent mIntent = new Intent("finish");
						sendBroadcast(mIntent);
	
				    	//Log.e("DEBUG", result);
				        Intent intent = new Intent("logout");
				        intent.setClass(InfoSettingActivity.this, LoginActivity.class);
				        InfoSettingActivity.this.startActivity(intent);
				        InfoSettingActivity.this.finish();
				        
				    }
				});
			}
		});
    }
    /**
     * 获取版本号
     * @return 当前应用的版本号
     */
    public String getVersion() {
        try {
            PackageManager manager = this.getPackageManager();
            PackageInfo info = manager.getPackageInfo(this.getPackageName(), 0);
            String version = info.versionName;
            return version;
        } catch (Exception e) {
            e.printStackTrace();
            return "无法获取版本号";
        }
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
	public static Context getContext(){
		return mContext;
	}
	
    @Override  
    protected void onPause() {  
        // TODO Auto-generated method stub  
        super.onPause();  
    }  
  
    @Override  
    protected void onResume() {  
        // TODO Auto-generated method stub  
        super.onResume(); 
    }  
    public boolean onKeyDown(int keyCode, KeyEvent event)  
    {  
        if (keyCode == KeyEvent.KEYCODE_BACK )  
        {  
	        //Intent intent = new Intent();

	       // intent.setClass(UploadImageActivity.this, MainActivity.class);
	        //UploadImageActivity.this.startActivity(intent);
        	InfoSettingActivity.this.finish();
        }  
          
        return false;     
    }
}
