package com.iniwap.whereisboat;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

public class HSVLayout extends LinearLayout {
    
	protected static final String  UPDATE_IMAGE_ACTION = "UPDATE_IMAGE_ACTION";
	private Context mContext;
	private Button[] mButtons;
	
	public HSVLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
	}
    
	public void initHSVView() {
				
		mButtons = new Button[AppConstant.MAX_UPLOAD_PIC_NUM];

		for (int i = 0; i < AppConstant.MAX_UPLOAD_PIC_NUM; i++) {
			
			View view = LayoutInflater.from(mContext).inflate(R.layout.horizontalscrollview,null);
			Button button  = (Button)view.findViewById(R.id.addtext);
			
			mButtons[i] = button;
			button.setTag(i);
			
			view.setPadding(10, 0, 10, 0);

			button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					//Toast.makeText(mContext, "选择了"+v.getTag().toString(), Toast.LENGTH_LONG).show();
					
					Intent intent = new Intent();
					intent.setAction(UPDATE_IMAGE_ACTION);
					intent.putExtra("index", Integer.parseInt(v.getTag().toString()));
					mContext.sendBroadcast(intent);
					
				}
			});
			this.setOrientation(HORIZONTAL);
			this.addView(view, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		}
	}
	public void setButtonImage(int index,Drawable drawable){

		mButtons[index].setTextColor(0x00aaaaaa);
		mButtons[index].setBackgroundColor(0xffffffff);
		mButtons[index].setBackgroundDrawable(drawable);
	}
	public void deleteButtonImage(int index){

		mButtons[index].setTextColor(0xffaaaaaa);
		mButtons[index].setBackgroundColor(0xffffffff);
		mButtons[index].setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.background_edittext));
	}
}
