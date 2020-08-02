package com.iniwap.whereisboat;

import java.io.File;
import java.util.ArrayList;

import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpPost;
import com.koushikdutta.async.http.AsyncHttpResponse;
import com.koushikdutta.async.http.body.MultipartFormDataBody;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class UploadService extends Service {
	
	private int mReuploadCount = 0;
	
	private static ArrayList<String> mImagePath = new ArrayList<String>();
	private static int mCount = 0;
	private static int mCurrentUpload = 0 ;
	private static String mDes = "";
	private static String mLongitude = "";
	private static String mLatitude = ""; 

	private static Handler reuploadHandler;
	private static Runnable reuploadRunnable;
		
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
    
	@Override
	public void onCreate() {
		reuploadHandler = new Handler();
		reuploadRunnable = new Runnable() {
		    @Override
		    public void run() {
		        // TODO Auto-generated method stub
		    	Toast.makeText(MainActivity.getContext(), "尝试第 " + mReuploadCount + " 次重传", Toast.LENGTH_LONG).show();
		        upload();
		    }
		};
	}
    
	@Override
	public void onDestroy() {

	}
    
	@Override
	public void onStart(Intent intent, int startId) {

		if (intent != null) {
			
			//暂时不允许已经存在上传仍旧选择上传的情况
			if(mCurrentUpload != 0){
				Toast.makeText(UploadImageActivity.getContext(), "已经有照片在上传中，请稍后再试！", Toast.LENGTH_LONG).show();
				return;
			}
			
			mCount = Integer.parseInt(intent.getStringExtra("count"));
			
			for(int i = 0;i < mCount;i++){
				mImagePath.add(intent.getStringExtra(""+i));
			}
					
			mDes = intent.getStringExtra("des");
			mLatitude = intent.getStringExtra("latitude");
			mLongitude = intent.getStringExtra("longitude");
			
			if(mCount != 0)
			{
				mCurrentUpload = 1;
			}
			
			upload();
		}
		else{
			Toast.makeText(UploadImageActivity.getContext(), "内部错误！", Toast.LENGTH_LONG).show();
		}
	}
	public void restParam(){
		mImagePath.clear();
		mCount = 0;
		mCurrentUpload = 0 ;
		mDes = "";
		mLongitude = "";
		mLatitude = "";
		mReuploadCount = 0;
		reuploadHandler = null;
		reuploadRunnable = null;
	}
	public void upload(){
		//here we upload image
		//upload image
		AsyncHttpPost post = new AsyncHttpPost(AppConstant.UPLOAD_URL);
		MultipartFormDataBody body = new MultipartFormDataBody();
		
		body.addStringPart("logid", AppConstant.LOGID);
		body.addStringPart("lognote", mDes);
		body.addStringPart("loglong", mLongitude);
		body.addStringPart("loglati", mLatitude);
		body.addStringPart("logtime", MainActivity.getTimeStamp());
		
		if(mCount != 0)
		{
			for(int i = 0;i<mCount;i++)
			{
				//body.addFilePart("file"+i, new File(mImagePath.get(mCurrentUpload - 1)));
				body.addFilePart("file"+i, new File(mImagePath.get(i)));
			}
		}
				
		post.setBody(body);
		
		AsyncHttpClient.getDefaultInstance().executeString(post, new AsyncHttpClient.StringCallback() {
		    @Override
		    public void onCompleted(Exception e, AsyncHttpResponse source, String result) {
		        if (e != null) {

		        	if(mReuploadCount <= AppConstant.MAX_REUPLOAD_NUM)
		        	{
		        		reuploadHandler.postDelayed(reuploadRunnable, (int) (60*1000*Math.pow(2, ++mReuploadCount)));
			    		
			    		showToast("照片上传失败,"+(int)(Math.pow(2, mReuploadCount)) + " 分钟后重传");
		        	}
		        	else
		        	{
			        	restParam();
			        	
			        	stopSelf();
			        	
		        		showToast("照片上传失败，请检查网络或稍后再上传");
		        	}
		        }
		        else
		        {			    	

		        	if(mReuploadCount != 0){
		        		reuploadHandler.removeCallbacks(reuploadRunnable);
		        		mReuploadCount = 0;
		        	}
		        	
		        	//success
		        	showToast("照片上传完毕！");
		        	restParam();
		        	stopSelf();
		        	
		        	/*
			        if(mCurrentUpload >= mCount){
			        				        	
			        	showToast("照片上传完毕！");
			        	restParam();
			        	
			        	stopSelf();
			        }
			        else
			        {
			        	showToast("成功第 " + mCurrentUpload + "张照片 :)");
			        	
				        mCurrentUpload++;
				        
				        upload();
			        }
			        */
		        }
		    }
		});
		
	}
	public final static Handler uiHandler = new Handler(){       
        public void handleMessage(Message msg){
        	
    		//Log.e("UPLOADIMAGE", msg.getData().getString("info"));
    		Toast.makeText(MainActivity.getContext(), msg.getData().getString("info"), Toast.LENGTH_LONG).show();
       }
	};
	public void showToast(String info){
		
    	Message msg = new Message();  
        msg.what = 0;
    	Bundle data = new Bundle();
        data.putString("info", info);
        msg.setData(data);
        
        uiHandler.sendMessage(msg);
     
	}
}
