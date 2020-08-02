package com.iniwap.whereisboat;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import com.iniwap.whereisboat.Account;
import com.iniwap.whereisboat.DataBaseManage;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpPost;
import com.koushikdutta.async.http.AsyncHttpResponse;
import com.koushikdutta.async.http.body.MultipartFormDataBody;
import com.koushikdutta.async.http.body.UrlEncodedFormBody;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


public class LoginActivity extends Activity {
	
	private final static boolean DEFINE_TEST_DEBUG = false;
	
    private static DataBaseManage dbm;
    private Account mAccount[] = null;
    
	private static Context mContext;
	private static ProgressDialog progressDialog;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.login);
		mContext = this;
		
		dbm = new DataBaseManage(mContext);
		mAccount = getLoginAccountRecord();
		//login
		findViewById(R.id.login).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				
				
				EditText usernameinupt = (EditText)findViewById(R.id.usernameinupt);
				EditText pwdinput = (EditText)findViewById(R.id.pwdinput);
				
				//
				final String username = usernameinupt.getText().toString();
				final String password = encryptPwd(pwdinput.getText().toString());
				final String nonMD5Pwd = pwdinput.getText().toString();
				
				///check first
				if(!checkUserName(username))
				{
	        		Toast.makeText(mContext, "请输入正确的用户名", Toast.LENGTH_LONG).show();
	        		return;
				}
				
				if(!checkPassword(pwdinput.getText().toString()))
				{
	        		Toast.makeText(mContext, "请输入正确的密码", Toast.LENGTH_LONG).show();
	        		return;
				}
				
				progressDialog = new ProgressDialog(LoginActivity.this);
				progressDialog.setCancelable(false);
				progressDialog.setIndeterminate(true);
				progressDialog.setMessage("登陆中...");
				progressDialog.show();
				
				//login
				
				AsyncHttpPost post = new AsyncHttpPost(AppConstant.LOGIN_URL);
				
				ArrayList<NameValuePair> pairs = new ArrayList<NameValuePair>();
		        pairs.add(new BasicNameValuePair("uid", username));
		        pairs.add(new BasicNameValuePair("upwd",nonMD5Pwd));
		        
		        if(DEFINE_TEST_DEBUG){
		        	pairs.add(new BasicNameValuePair("unum", "debug"));

		        }else{
		        	pairs.add(new BasicNameValuePair("unum", getIMEI()));

		        }

		        UrlEncodedFormBody writer = new UrlEncodedFormBody(pairs);
				post.setBody(writer);
				
				//here we make first refresh
				AsyncHttpClient.getDefaultInstance().executeString(post, new AsyncHttpClient.StringCallback() {
				    // Callback is invoked with any exceptions/errors, and the result, if available.
				    @Override
				    public void onCompleted(Exception e, AsyncHttpResponse response, String result) {

			        	//progressDialog.hide();
				    	Message msg = new Message();  
				        msg.what = CLOSE_PROGRESS_DIALOG;
				        uiHandler.sendMessage(msg);
				        
				        if (e != null) {
				        
					    	Message msg1 = new Message();  
					        msg1.what = LOGIN_FAIL;
					        msg1.obj = e.getLocalizedMessage();
					        uiHandler.sendMessage(msg1);
				        	return;
				        }
				        
				        if(result == null){
				        	Message msg1 = new Message();  
					        msg1.what = LOGIN_FAIL;
					        msg1.obj = "未收到服务器响应";
					        uiHandler.sendMessage(msg1);
				        	return;
				        }
				        		       
				        List<Map<String, String>> list = getData(result);
				        
				        if(list.isEmpty()){
				        	
				        	Message msg1 = new Message();  
					        msg1.what = LOGIN_FAIL;
					        msg1.obj = "服务器响应数据错误";
					        uiHandler.sendMessage(msg1);
				        	
				        	return;
				        }
				        
				        AppConstant.LOGID = list.get(0).get("LogID");
				        
				        if(AppConstant.LOGID == null || AppConstant.LOGID.length() == 0){
				        	AppConstant.LOGID = "";
				        	
					    	Message msg1 = new Message();  
					        msg1.what = LOGIN_FAIL;
					        msg1.obj = list.get(0).get("err");
					        uiHandler.sendMessage(msg1);
				        	
					        return;
				        }
//				        Log.e("DEBUG", list.get(1).get("UName"));
				        //login sucess
				        dbm.open();
				        dbm.Insert(username, nonMD5Pwd, 1);//type is admin or normal//get from http resp
				        dbm.close();
				        //
				        Intent intent = new Intent();
				        intent.putExtra("username",list.get(1).get("UName")==null?username:list.get(1).get("UName"));
				        intent.putExtra("pwd",password);
				        intent.putExtra("type","0");//if admin
				        intent.setClass(LoginActivity.this, MainActivity.class);
				        LoginActivity.this.startActivity(intent);
				        LoginActivity.this.finish();
				    }
				});
			}
		});
		
		//reset
		findViewById(R.id.reset).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				//here we reset
				//findViewById(R.id.pwdinput).setContentDescription("");
				
				EditText accinput = (EditText)findViewById(R.id.usernameinupt);
				accinput.setText("");
				
				EditText pwdinput = (EditText)findViewById(R.id.pwdinput);
				pwdinput.setText("");
			}
		});
		
		////
		findViewById(R.id.act_btn).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
			}
		});
		
		
		////////here init history account record///////////////
		
		if(mAccount != null && mAccount.length != 0){
			//has record
			EditText usernameinupt = (EditText)findViewById(R.id.usernameinupt);
			EditText pwdinput = (EditText)findViewById(R.id.pwdinput);
			
			usernameinupt.setText(mAccount[0].name);
			pwdinput.setText(mAccount[0].password);			
			
			
	        Intent intent = getIntent();  
	        //获得Intent的Action  
	        String action = intent.getAction();  
	        
			if(action.equals("logout")){
				return;
			}
			
			//AUTO LOGIN
			final String username = mAccount[0].name;
			final String nonMD5Pwd = mAccount[0].password;
			
			progressDialog = new ProgressDialog(LoginActivity.this);
			progressDialog.setCancelable(false);
			progressDialog.setIndeterminate(true);
			progressDialog.setMessage("登陆中...");
			progressDialog.show();
			
			//login
			
			AsyncHttpPost post = new AsyncHttpPost(AppConstant.LOGIN_URL);
			
			ArrayList<NameValuePair> pairs = new ArrayList<NameValuePair>();
	        pairs.add(new BasicNameValuePair("uid", username));
	        pairs.add(new BasicNameValuePair("upwd",nonMD5Pwd));
	        
	        if(DEFINE_TEST_DEBUG){
	        	pairs.add(new BasicNameValuePair("unum", "debug"));

	        }else{
	        	pairs.add(new BasicNameValuePair("unum", getIMEI()));

	        }

	        UrlEncodedFormBody writer = new UrlEncodedFormBody(pairs);
			post.setBody(writer);
			
			//here we make first refresh
			AsyncHttpClient.getDefaultInstance().executeString(post, new AsyncHttpClient.StringCallback() {
			    // Callback is invoked with any exceptions/errors, and the result, if available.
			    @Override
			    public void onCompleted(Exception e, AsyncHttpResponse response, String result) {

		        	//progressDialog.hide();
			    	Message msg = new Message();  
			        msg.what = CLOSE_PROGRESS_DIALOG;
			        uiHandler.sendMessage(msg);
			        
			        if (e != null) {
			        
				    	Message msg1 = new Message();  
				        msg1.what = LOGIN_FAIL;
				        msg1.obj = e.getLocalizedMessage();
				        uiHandler.sendMessage(msg1);
			        	return;
			        }
			        
			        if(result == null){
			        	Message msg1 = new Message();  
				        msg1.what = LOGIN_FAIL;
				        msg1.obj = "未收到服务器响应";
				        uiHandler.sendMessage(msg1);
			        	return;
			        }
			        		       
			        List<Map<String, String>> list = getData(result);
			        
			        if(list.isEmpty()){
			        	
			        	Message msg1 = new Message();  
				        msg1.what = LOGIN_FAIL;
				        msg1.obj = "服务器响应数据错误";
				        uiHandler.sendMessage(msg1);
			        	
			        	return;
			        }
			        
			        AppConstant.LOGID = list.get(0).get("LogID");
			        
			        if(AppConstant.LOGID == null || AppConstant.LOGID.length() == 0){
			        	AppConstant.LOGID = "";
			        	
				    	Message msg1 = new Message();  
				        msg1.what = LOGIN_FAIL;
				        msg1.obj = list.get(0).get("err");
				        uiHandler.sendMessage(msg1);
			        	
				        return;
			        }

			        //
			        Intent intent = new Intent();
			        intent.putExtra("username",list.get(1).get("UName")==null?username:list.get(1).get("UName"));
			        intent.putExtra("pwd",nonMD5Pwd);
			        intent.putExtra("type","0");//if admin
			        intent.setClass(LoginActivity.this, MainActivity.class);
			        LoginActivity.this.startActivity(intent);
			        LoginActivity.this.finish();
			    }
			});
			
		}
	}
	
	public void login(String uname,String pwd,boolean isAuto){
		
	}
	
	public static List<Map<String, String>> getData(String xml) {
        List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        XmlPullParser xrp = null;
        
		try {
	        XmlPullParserFactory factory;
	        
			factory = XmlPullParserFactory.newInstance();
	        factory.setNamespaceAware(true);
	        xrp = factory.newPullParser();
	        
			try {
				InputStream is = new ByteArrayInputStream(xml.getBytes("UTF-8"));
		        xrp.setInput(is, null);

			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	        
		} catch (XmlPullParserException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        
        try {
            // 直到文档的结尾处
            while (xrp.getEventType() != XmlResourceParser.END_DOCUMENT) {
                // 如果遇到了开始标签
                if (xrp.getEventType() == XmlResourceParser.START_TAG) {
                    String tagName = xrp.getName();// 获取标签的名字
                    if (tagName.equals("LogID")) {
                        Map<String, String> map = new HashMap<String, String>();
                        map.put("LogID", xrp.nextText());
                        list.add(map);
                    }else if(tagName.equals("err")){
                    	Map<String, String> map = new HashMap<String, String>();
                        map.put("err", xrp.nextText());
                        list.add(map);
                        break;
                    }else if(tagName.equals("UName")){
                    	Map<String, String> map = new HashMap<String, String>();
                        map.put("UName", xrp.nextText());
                        list.add(map);
                    }
                    
                }
                xrp.next();// 获取解析下一个事件
            }
        } catch (XmlPullParserException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return list;
    }
	public boolean checkPassword(String str){
		boolean nRet = true;
		if(str.length()<2 || str.length() >15){
			nRet = false;
		}
			
		return nRet;
	}
	public boolean checkUserName(String str){
		boolean nRet = true;
		
		if(str.length()<2 || str.length() >15){
			nRet = false;
		}
		
		return nRet;
	}
	
	public final static int CLOSE_PROGRESS_DIALOG = 1;
	public final static int LOGIN_FAIL = 2;
	public final static Handler uiHandler = new Handler(){       
        public void handleMessage(Message msg){
        	
        	switch(msg.what){
        	case CLOSE_PROGRESS_DIALOG:
        		{
        			progressDialog.hide();
        		}
        		break;
        	case LOGIN_FAIL:
        		Toast.makeText(mContext, (String)msg.obj, Toast.LENGTH_LONG).show();
        		break;
        	}
       }
	};
	
	//////////
	public static Account[] getLoginAccountRecord() {
		dbm.open();
		Account[] accounts = dbm.fetchAllAccountRecord();
		dbm.close();
		if (accounts == null){
			return null;
		}
		for (int i = accounts.length - 1; i >= 0; i--) {
			//Log.e("------>", accounts[i].name+"|"+accounts[i].password+"|"+String.valueOf(accounts[i].type));
		}
		return accounts;
	}
/////////////////////////////////////////////////////////////
	@SuppressLint("TrulyRandom")
	public static String encryptPwd(String pwd_str) {
		String pwd;
		pwd = pwd_str;
		byte _gszDefalutAesKey[] = { (byte) 0xF3, 0x62, 0x12, 0x05, 0x13,
									(byte) 0xE3, (byte) 0x89, (byte) 0xFF, 0x23, 0x11, (byte) 0xD7,
									0x36, 0x01, 0x23, 0x10, 0x07, 0x05, (byte) 0xA2, 0x10, 0x00,
									0x7A, (byte) 0xCC, 0x02, 0x3C, 0x39, 0x01, (byte) 0xDA, 0x2E,
									(byte) 0xCB, 0x12, 0x44, (byte) 0x8B };
		byte _gAesIV[] = { 0x15, (byte) 0xFF, 0x01, 0x00, 0x34, (byte) 0xAB,
							0x4C, (byte) 0xD3, 0x55, (byte) 0xFE, (byte) 0xA1, 0x22, 0x08,
							0x4F, 0x13, 0x07 };
		
		SecretKeySpec secretKeySpec = new SecretKeySpec(_gszDefalutAesKey,"AES");
		IvParameterSpec ivParameterSpec = new IvParameterSpec(_gAesIV);
		Cipher cipherencrypt;
		try {
			cipherencrypt = Cipher.getInstance("AES/CFB/PKCS7Padding");
			cipherencrypt.init(Cipher.ENCRYPT_MODE, secretKeySpec,
			ivParameterSpec);
			byte aa1[] = pwd.getBytes();
			byte[] sss = cipherencrypt.doFinal(aa1);
			
			pwd = new String(Base64.encode(sss, Base64.DEFAULT));
			
			pwd = URLEncoder.encode(pwd, "utf-8");
		
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return pwd;
	}
	///////////////////exit/////////////
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

    DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener()  
    {  
        public void onClick(DialogInterface dialog, int which)  
        {  
            switch (which)  
            {  
            case AlertDialog.BUTTON_POSITIVE:// 
                finish();  
                break;  
            case AlertDialog.BUTTON_NEGATIVE://
                break;  
            default:  
                break;  
            }  
        }  
    };
    
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
}
