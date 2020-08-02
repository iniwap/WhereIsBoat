package com.iniwap.whereisboat;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Media;
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

public class UploadImageActivity extends Activity {
	
	private static Context mContext;

	private HSVLayout hsvLayout = null;  
    private IntentFilter intentFilter = null;  
    private BroadcastReceiver receiver = null;  
    
    private String mCurrenSelectPath = "";
    private String mImagePaths[];
    private int m_iCurrentSelectIndex;
    private int m_iUploadImageCount = 0;
    
    /////////photo 
    private static final int SELECT_PHOTO = 0;
    private static final int TAKE_PHOTO = 1;
    private static final int CROP_PHOTO = 2;
    /*拍照的照片存储位置*/  
    private static String PHOTO_DIR; 
    
    ////map///
    private MapView mapView;
	TiledLayer vect = TLayerFactory.Tdt_Vect_base();
	TiledLayer vectPoi = TLayerFactory.Tdt_Vect_poi();
	
	GraphicsLayer graphicsLayer = new GraphicsLayer();

	public String mLongitude; 
	public String mLatitude; 

	
    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.upload);//here to be fix
		
		mContext = this;
		
		mLongitude = getIntent().getStringExtra("longitude");
		mLatitude = getIntent().getStringExtra("latitude");
				
		TextView longTextView = (TextView)findViewById(R.id.ELocation);
		TextView latTextView = (TextView)findViewById(R.id.NLocation);
		longTextView.setText("E: " + String.format("%.4f",Double.valueOf(mLongitude)));
		latTextView.setText("N: " + String.format("%.4f",Double.valueOf(mLatitude)));
		
		//addMapView(longitude,latitude);
		
		hsvLayout = (HSVLayout) findViewById(R.id.hsvLayout);  
        
        mImagePaths = new String[AppConstant.MAX_UPLOAD_PIC_NUM];
        
        hsvLayout.initHSVView();  
        
        //cancel
		findViewById(R.id.cancelbtn).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
		        UploadImageActivity.this.finish();
			}
		});
		
		findViewById(R.id.backbtn).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
		        UploadImageActivity.this.finish();
			}
		});
        
        ////////upload //////////
		findViewById(R.id.postbtn).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				//上传图片
				if(m_iUploadImageCount == 0){
					Toast.makeText(mContext, "您未选择照片", Toast.LENGTH_LONG).show();
					return;	
				}
		        //Intent intent = new Intent("com.iniwap.whereisboat.UploadService");
		        Intent intent = new Intent(mContext,UploadService.class);
		        intent.putExtra("count", ""+m_iUploadImageCount);
		        int mark = 0;
		        for(int i = 0;i < AppConstant.MAX_UPLOAD_PIC_NUM;i++){
		        	if(mImagePaths[i] != null && mImagePaths[i].length() != 0){
		        		intent.putExtra(""+mark,mImagePaths[i]);
		        		mark++;
		        	}
		        		
		        }
		        EditText text = (EditText)findViewById(R.id.edittext);
		        intent.putExtra("des",text.getText().toString());
		        intent.putExtra("longitude",mLongitude);
		        intent.putExtra("latitude",mLatitude);

		        //
		        
		        startService(intent); // startService  
		        
		        Toast.makeText(mContext, "照片后台上传中...", Toast.LENGTH_LONG).show();
		        
		        UploadImageActivity.this.finish();
			}
		});
	}
    /*
	private void addMapView(String longitude,String latitude){
		
		ArcGISRuntime.setClientId("OMxL2qaDfBsFG6uK");
		mapView = (MapView) findViewById(R.id.map_location);
		mapView.addLayer(vect);
		mapView.addLayer(vectPoi);
		mapView.addLayer(graphicsLayer);
		mapView.setMaxResolution(TLayer.RESOLUTIONS[7]);
		
		final Point location = new Point(Double.valueOf(longitude),Double.valueOf(latitude));
		
		mapView.setOnStatusChangedListener(new OnStatusChangedListener() {
			@Override
			public void onStatusChanged(Object o, STATUS status) {
				if (status == STATUS.INITIALIZED && o == mapView) {
					mapView.setResolution(TLayer.RESOLUTIONS[15]);
					
					mapView.centerAt(location, true);
					
					vect.setVisible(true);
					vectPoi.setVisible(true);
					
					//
					Drawable boatImage = getResources().getDrawable(R.drawable.pos_start);
					
					boatImage.setBounds(new Rect(0,10,10,0));
					boatImage.setColorFilter(0xFFFF0000, Mode.MULTIPLY);
					Graphic pointGraphic = new Graphic(location, new PictureMarkerSymbol(mContext,boatImage));
					
					graphicsLayer.addGraphic(pointGraphic);
				}
			}
		});
	}
	*/
	
    /** 
     * 创建一个消息过滤器 
     *  
     * @return 
     */ 
    
    private IntentFilter getIntentFilter() {  
        if (intentFilter == null) {  
            intentFilter = new IntentFilter();  
            intentFilter.addAction(HSVLayout.UPDATE_IMAGE_ACTION);  
        }  
        return intentFilter;  
    }  
    
    class UpdateImageReceiver extends BroadcastReceiver{  
  
        @Override  
        public void onReceive(Context context, Intent intent) {  
            // TODO Auto-generated method stub  
            if(intent.getAction().equals(HSVLayout.UPDATE_IMAGE_ACTION)){  
                int index = intent.getIntExtra("index", Integer.MAX_VALUE);  
                
                m_iCurrentSelectIndex = index;
                

                doPickPhotoAction();
                
            }
        }          
    } 
	
    public void onSelectedPhoto(){

    	m_iUploadImageCount++;//if clear //--
    	Bitmap photo = BitmapFactory.decodeFile(mCurrenSelectPath);
    	//mSelectedPhoto = photo;
    	Drawable drawable = new BitmapDrawable(photo);

    	mImagePaths[m_iCurrentSelectIndex] = mCurrenSelectPath;
    	
    	hsvLayout.setButtonImage(m_iCurrentSelectIndex, drawable);
    }
    ////////照片操作////////
    private void doPickPhotoAction() {  
         	
        String status = Environment.getExternalStorageState();  
        if(status.equals(Environment.MEDIA_MOUNTED)){//判断是否有SD卡  
        	PHOTO_DIR = Environment.getExternalStorageDirectory() + "/DCIM/水面保洁巡查系统/";
        }  
        else{  
            Toast.makeText(mContext, "没有SD卡", Toast.LENGTH_LONG).show();
            return;
        }  
        
        // Wrap our context to inflate list items using correct theme  
        final Context dialogContext = new ContextThemeWrapper(mContext,  
                android.R.style.Theme_Light);  
        String[] choices;
        
        if(mImagePaths[m_iCurrentSelectIndex] != null && mImagePaths[m_iCurrentSelectIndex].length() != 0)
        {
        	//show clear image
        	choices = new String[3];  
	        choices[0] = getString(R.string.takephoto);  //拍照  
	        choices[1] = getString(R.string.pickphoto);  //从相册中选择
	        choices[2] = getString(R.string.deleteimg);  //从相册中选择
        }
        else
        {
	        choices = new String[2];  
	        choices[0] = getString(R.string.takephoto);  //拍照  
	        choices[1] = getString(R.string.pickphoto);  //从相册中选择  
        }
        final ListAdapter adapter = new ArrayAdapter<String>(dialogContext,  
                android.R.layout.simple_list_item_1, choices);  
      
        final AlertDialog.Builder builder = new AlertDialog.Builder(dialogContext);  
        builder.setTitle(R.string.selectphototitle);  
        builder.setSingleChoiceItems(adapter, -1,  
                new DialogInterface.OnClickListener() {  
                    public void onClick(DialogInterface dialog, int which) {  
                        dialog.dismiss();  
                        switch (which) {  
                        case 0:
                        	{  
                        		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        		
                        		File dir = new File(PHOTO_DIR);
                        		dir.mkdirs();
                        		
                        		mCurrenSelectPath = PHOTO_DIR + getPhotoFileName();
                				File file = new File(mCurrenSelectPath);
                				Uri mOutPutFileUri = Uri.fromFile(file);
                				intent.putExtra(MediaStore.EXTRA_OUTPUT, mOutPutFileUri);
                				((Activity) mContext).startActivityForResult(intent, TAKE_PHOTO);
                        	}
                            break;
                        case 1:  
                            {
                			    Intent picture = new Intent(Intent.ACTION_PICK,Media.EXTERNAL_CONTENT_URI);
                			    ((Activity) mContext).startActivityForResult(picture, SELECT_PHOTO);
                            }
                            break; 
                        case 2:
                        	{
                        		m_iUploadImageCount--;
                        		mImagePaths[m_iCurrentSelectIndex] = null;
                        		hsvLayout.deleteButtonImage(m_iCurrentSelectIndex);
                        	}
                        	break;
                        }  
                    }  
                });

        builder.create().show();  
    }
    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		//super.onActivityResult(requestCode, resultCode, data);
		if(resultCode != RESULT_OK)
		{
			return;
		}
		switch (requestCode)
		{
		case SELECT_PHOTO:
		{
			Uri uri = data.getData();  
			if(uri != null)
			{
	            Cursor cursor = this.getContentResolver().query(uri, null, null, null, null);  
	            cursor.moveToFirst();  
	            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
	            //最后根据索引值获取图片路径
	            final String photoPath = cursor.getString(column_index);
	            mCurrenSelectPath = PHOTO_DIR + getPhotoFileName();
	            cropImageUri(photoPath,mCurrenSelectPath,CROP_PHOTO);
			}
		}
			break;
		case TAKE_PHOTO:
		{
         	MediaScannerConnection.scanFile(mContext,
                    new String[]{mCurrenSelectPath}, new String[]{ "image/*"},
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                        	//
                        }
            });
         	
			final String photoPath = mCurrenSelectPath;
			 mCurrenSelectPath = PHOTO_DIR + getPhotoFileName();
             cropImageUri(photoPath,mCurrenSelectPath,CROP_PHOTO);
		}
		
		break;
		case CROP_PHOTO:
			//here we use path
			onSelectedPhoto();
			break;
		}
	}
	/**
	 * 调用裁剪
	 * @param resourcepath
	 * @param resultPath
	 * @param outputX
	 * @param outputY
	 * @param requestCode
	 */
	private void cropImageUri(String resourcepath, String resultPath, int requestCode)
	{
		//Intent.ACTION_GET_CONTENT
		//com.android.camera.action.CROP
		Intent intent = new Intent("com.android.camera.action.CROP");
		intent.setDataAndType(Uri.fromFile(new File(resourcepath)), "image/*");
		intent.putExtra("crop", "true");
		intent.putExtra("aspectX", 0);
		intent.putExtra("aspectY", 0);
		intent.putExtra("outputX", AppConstant.MAX_UPLOAD_IMG_SIZE);
		intent.putExtra("outputY", AppConstant.MAX_UPLOAD_IMG_SIZE);
		intent.putExtra("scale", true);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(resultPath)));
		intent.putExtra("return-data", false);
		intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
		intent.putExtra("noFaceDetection", true); // no face detection
		startActivityForResult(intent, requestCode);
	}
	  
	/** 
	* 用当前时间给取得的图片命名
	*/  
	private String getPhotoFileName() {  
	    Date date = new Date(System.currentTimeMillis());  
	    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss",Locale.CHINA);;//new SimpleDateFormat("'IMG'_yyyy-MM-dd HH:mm:ss",Locale.CHINA);
	    
	    return dateFormat.format(date) + ".jpg";  
	}
	
	public static Context getContext(){
		return mContext;
	}
	
    @Override  
    protected void onPause() {  
        // TODO Auto-generated method stub  
        super.onPause();  
        unregisterReceiver(receiver);  
    }  
  
    @Override  
    protected void onResume() {  
        // TODO Auto-generated method stub  
        super.onResume();  
        if (receiver == null) {  
            receiver = new UpdateImageReceiver();  
        }  
        registerReceiver(receiver, getIntentFilter());  
    }  
    public boolean onKeyDown(int keyCode, KeyEvent event)  
    {  
        if (keyCode == KeyEvent.KEYCODE_BACK )  
        {  
	        //Intent intent = new Intent();

	       // intent.setClass(UploadImageActivity.this, MainActivity.class);
	        //UploadImageActivity.this.startActivity(intent);
	        UploadImageActivity.this.finish();
        }  
          
        return false;     
    }
}
