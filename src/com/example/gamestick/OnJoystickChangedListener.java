package com.example.gamestick;

import android.graphics.Point;
/**
 * An event fired when the values of the GameStick change.
 * @author Cédric Andreolli - Intel Corporation
 *
 */
public interface OnJoystickChangedListener {
	/**
	 *Return the position of the stick in percentage. The default position is 50/50.
	 *The x value represents the horizontal axis, the y value the vertical axis.
	 * @param newPosition The new position of the stick
	 */
	public void readValues(Point newPosition);
}
