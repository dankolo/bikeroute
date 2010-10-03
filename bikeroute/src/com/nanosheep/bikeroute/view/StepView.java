package com.nanosheep.bikeroute.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class StepView extends LinearLayout 
{ 
	private Paint paint;
    
	public StepView(final Context context, final AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public StepView(final Context context) {
		super(context);
		init();
	}

	private void init() {
		paint = new Paint();
		paint.setARGB(200, 75, 75, 75);
		paint.setAntiAlias(true);

	}
	
	public void setInnerPaint(final Paint paint) {
		this.paint = paint;
	}


    @Override
    protected void dispatchDraw(final Canvas canvas) {
    	
    	final RectF drawRect = new RectF();
    	drawRect.set(0,0, getMeasuredWidth(), getMeasuredHeight());
    	
    	canvas.drawRoundRect(drawRect, 5, 5, paint);
		
		super.dispatchDraw(canvas);
    }
}