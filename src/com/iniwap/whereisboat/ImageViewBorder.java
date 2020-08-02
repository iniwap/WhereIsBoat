package com.iniwap.whereisboat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.ImageView;

public class ImageViewBorder extends ImageView {

	private String namespace="http://xmz.com";
	private int color;

	public ImageViewBorder(Context context, AttributeSet attrs) {
		super(context, attrs);
		color = Color.parseColor(attrs.getAttributeValue(namespace,
				"BorderColor"));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.ImageView#onDraw(android.graphics.Canvas)
	 */
	@Override
	protected void onDraw(Canvas canvas) {

		super.onDraw(canvas);
		// »­±ß¿ò ÔÝÊ±È¥³ýÐ¡±ß¿ò
		Rect rec = canvas.getClipBounds();
		rec.bottom--;
		rec.right--;
		Paint paint = new Paint();
		paint.setColor(color);
		paint.setStyle(Paint.Style.STROKE);
		canvas.drawRect(rec, paint);
	}
}
