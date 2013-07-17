package com.example.gamestick;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
/**
 * Represents the Java side of the UI element.
 * @author Cédric Andreolli - Intel Corporation
 *
 */
@SuppressLint("DrawAllocation")
public class GameStick extends View{
	/**
	 * The background image.
	 */
	private Bitmap backgroundBMP = null;
	/**
	 * Used to save the canvas height.
	 */
	private int canvasHeight;
	/**
	 * Used to save the canvas width.
	 */
	private int canvasWidth;
	/**
	 * A handler that is fired when the joystick moves.
	 */
	private OnJoystickChangedListener joystickChangedListener;
	/**
	 * The new position of the stick after the player released it.
	 */
	private Point newPosition = null;
	/**
	 * The previous position of the stick.
	 */
	private Point previousPosition = null;
	/**
	 * The reference point recorded when the player touch the stick.
	 * This point is used to compute the distance
	 */
	private PointF reference = null;
	/**
	 * Used if you want to reset the x axis when the player releases the stick.
	 */
	private boolean resetX;
	/**
	 * Used if you want to reset the y axis when the player releases the stick.
	 */
	private boolean resetY;
	/**
	 * Indicates that just the Y axis can be used. It changes the background image.
	 */
	private boolean singleAxis = false;
	/**
	 * The size of the stick.
	 */
	private int stick_size;
	/**
	 * The stick bitmap.
	 */
	private Bitmap stickBMP = null;

	/**
	 * Default constructor.
	 * @param context The Android context.
	 * @param attrs The attribute set.
	 * @throws Exception
	 */
	public GameStick(Context context, AttributeSet attrs) throws Exception {
		super(context, attrs);
		//We initiate a new position.
		this.newPosition = new Point();
		//Retrieve parameters from XML file.
		TypedArray a = context.getTheme().obtainStyledAttributes(
				attrs,
				R.styleable.GameStick,
				0, 0);
		/**
		 * Set the parameters from XML
		 */
		this.resetX = a.getBoolean(R.styleable.GameStick_resetX, false);
		this.resetY = a.getBoolean(R.styleable.GameStick_resetY, false);
		this.singleAxis = a.getBoolean(R.styleable.GameStick_singleAxis, false);
		/**
		 * The OnTouchListener is used to detect user actions.
		 */
		this.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					GameStick.this.reference = new PointF(event.getX(), event.getY());
					GameStick.this.previousPosition = new Point(GameStick.this.newPosition);
					break;
				case MotionEvent.ACTION_UP:
					Log.i("appmessages", "resetX:"+GameStick.this.resetX+" resetY:"+GameStick.this.resetY);
					GameStick.this.reference = null;
					if(GameStick.this.resetX)
						GameStick.this.newPosition.x = (GameStick.this.canvasWidth - GameStick.this.stick_size) / 2;
					if(GameStick.this.resetY)
						GameStick.this.newPosition.y = (GameStick.this.canvasHeight - GameStick.this.stick_size) / 2;
					GameStick.this.fireEvent();
					GameStick.this.invalidate();
					break;
				case MotionEvent.ACTION_MOVE:
					if(GameStick.this.reference != null){
						GameStick.this.moveStick(event);
					}
					break;
				default:
					break;
				}

				return true;
			}
		});
	}
	/**
	 * Blit the stick image at the good position.
	 * @param canvas
	 */
	private void drawCanvas(Canvas canvas) {
		canvas.drawBitmap(this.backgroundBMP, 0, 0, null);
		Log.i("appmessages", "drawing for real : "+this.newPosition.x+"-"+this.newPosition.y);
		canvas.drawBitmap(this.stickBMP, this.newPosition.x, this.newPosition.y, null);
	}

	/**
	 * Fire the JoystickChanged event when the joystick values change.
	 */
	private void fireEvent() {
		Point eventToFire = new Point((int)(this.newPosition.x*100.0 / (this.canvasWidth - this.stick_size)),
				100-(int)(this.newPosition.y*100.0 / (this.canvasHeight - this.stick_size)));
		if(this.joystickChangedListener != null){
			Log.i("appmessages", "values : "+eventToFire);
			this.joystickChangedListener.readValues(eventToFire);
		}
	}

	public boolean isResetY() {
		return this.resetY;
	}

	/**
	 * Computes the new position of the stick.
	 * @param event The event.
	 */
	private void moveStick(MotionEvent event) {
		//compute the distance
		int distanceX =  (int) (event.getX() - this.reference.x);
		int distanceY =  (int) (event.getY() - this.reference.y);

		int maxDistanceX = this.getMeasuredWidth() - this.stick_size;
		int maxDistanceY = this.getMeasuredHeight()- this.stick_size;

		int newPositionX = this.previousPosition.x + distanceX;
		int newPositionY = this.previousPosition.y + distanceY;

		newPositionX = newPositionX > maxDistanceX ? maxDistanceX : newPositionX;
		newPositionX = newPositionX < 0 ? 0 : newPositionX;
		newPositionY = newPositionY > maxDistanceY ? maxDistanceY : newPositionY;
		newPositionY = newPositionY < 0 ? 0 : newPositionY;

		this.newPosition.x = newPositionX;
		this.newPosition.y = newPositionY;

		this.fireEvent();

		this.invalidate();
	}


	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		//If the stick has not be set
		if(this.stickBMP == null || this.backgroundBMP == null){
			/**
			 * Retrieve the width and the height and compute the stick size.
			 * If the BMP are not set, we need to retrieve them.
			 */
			this.canvasWidth = this.getMeasuredWidth();
			this.canvasHeight = this.getMeasuredHeight();
			this.stick_size = this.canvasWidth / 3;
			this.stickBMP = BitmapFactory.decodeResource(this.getResources(), R.drawable.stick);
			if(this.singleAxis)
				this.backgroundBMP = BitmapFactory.decodeResource(this.getResources(), R.drawable.base2);
			else
				this.backgroundBMP = BitmapFactory.decodeResource(this.getResources(), R.drawable.base);

			this.stickBMP = Bitmap.createScaledBitmap(this.stickBMP,
					this.stick_size, this.stick_size, false);
			this.backgroundBMP = Bitmap.createScaledBitmap(this.backgroundBMP,
					this.canvasWidth, this.canvasHeight, false);

			//Compute the stick position at startup
			int posX = (this.canvasWidth - this.stick_size) / 2;
			int posY = (this.canvasHeight - this.stick_size) / 2;
			this.newPosition.x = posX;
			this.newPosition.y = posY;
			this.previousPosition = new Point(this.newPosition);

			this.drawCanvas(canvas);
			return;
		}
		//Move the stick at the right position
		this.drawCanvas(canvas);
	}

	void resetComponent(){
		this.newPosition.x = (this.canvasWidth - this.stick_size) / 2;
		this.newPosition.y = (this.canvasHeight - this.stick_size) / 2;
		GameStick.this.invalidate();
	}

	public void setOnJoystickChangeListener(OnJoystickChangedListener listener){
		this.joystickChangedListener = listener;
	}

	public void setResetY(boolean resetY) {
		this.resetY = resetY;
	}
}
