/**
 * ##library.name##
 * ##library.sentence##
 * ##library.url##
 *
 * Copyright ##copyright## ##author##
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General
 * Public License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307  USA
 * 
 * @author      ##author##
 * @modified    ##date##
 * @version     ##library.prettyVersion## (##library.version##)
 * originally inspired by Tobias Toft blog post
 * http://www.tobiastoft.com/posts/an-intro-to-pen-plotters
 */

package sjunnesson.HPGL_plotter;

import java.awt.Point;

import processing.core.*;
import processing.serial.*;

public class Plotter {
	boolean DEBUG = false;
	Serial port;
	boolean PLOTTING_ENABLED = true;
	boolean PLOTTER_INITIALIZED = false;

	public double DEFAULT_FONT_HEIGHT = 0.2;
	public float DEFAULT_LABEL_DIRECTION = 0; // right

	double characterWidth = DEFAULT_FONT_HEIGHT;
	double characterHeight = DEFAULT_FONT_HEIGHT;
	float labelAngleHorizontal = 1;
	float labelAngleVertical = 0;
	// Plotter dimensions
	public int xMin = 0;
	public int yMin = 0;
	public int xMax = 9000;
	public int yMax = 6402;

	int paperA4 = 0;
	int paperA3 = 1;
	int paperA = 2;
	int paperB = 3;

	static char END_OF_TEXT = 0x03;
	static char ESC_CHAR = 0x1B;

	public boolean bufferFull = false;

	PApplet parent;
	private double bezierDetailLevel = 10;

	public Plotter(String portName, PApplet _t, int paperType) {
		parent = _t;
		if (PLOTTING_ENABLED) {
			port = new Serial(parent, portName, 9600);
			port.clear();
			parent.println("Plotting to port: " + portName);
			// Initialize plotter
			write("IN;SP1;");
			PLOTTER_INITIALIZED = true;
			parent.println("Plotter Initialized");
			setDimensions(paperType);

			// make sure that we set all the plotter variables to their
			// default values
			setFontHeight(DEFAULT_FONT_HEIGHT);
			setLabelDirection(DEFAULT_LABEL_DIRECTION);

		} else {
			parent.println("Plotting DISABLED");
		}
	}

	// COMMUNICATION

	public void write(String hpgl) {

		if (DEBUG) {
			parent.println(hpgl);
		}

		if (PLOTTING_ENABLED) {
			// if the plotter has reported that the buffer is full wait for it
			// to clear
			while (bufferFull == true) {
				parent.println("Waiting for buffer to clear");
				parent.delay(100);
			}

			// send the hpgl string to the plotter
			port.write(hpgl);
			parent.delay(20); // seems to need some time to react on full
			// buffer
		}

	}

	//
	// public int getAvailableBuffer() {
	// int freeBuffer = 0;
	// // send the command to retrieve the buffer size from the plotter
	// // get the current buffer size
	// port.write(ESC_CHAR + ".B");
	//
	// long timeoutStart = System.currentTimeMillis();
	// int responseWaitTime = 1000; // how long time we will wait for the
	// // plotter to respond
	// parent.println("Waiting on plotter response");
	// while (port.available() < 1) {
	// if ((System.currentTimeMillis() - timeoutStart) > responseWaitTime) {
	// // if the plotter has not responded in the alloted time
	// // we
	// // break out and throws a error.
	// parent.println("Plotter did not respond.");
	// return 0;
	// }
	// }
	//
	// byte[] inBuffer = new byte[7];
	// while (port.available() > 0) {
	// inBuffer = port.readBytesUntil(0x0D);
	//
	// if (inBuffer != null) {
	//
	// String bufferAsString = new String(inBuffer);
	// String digits = bufferAsString.replaceAll("[^0-9]", "");
	//
	// freeBuffer = Integer.parseInt(digits);
	// parent.println("Free buffer:" + freeBuffer);
	//
	// } else {
	// parent.println("Null returned from Plotter");
	// }
	// }
	//
	// return freeBuffer;
	// }

	// DIMENSIONS

	public void setCustomDimension(int _xMin, int _yMin, int _xMax, int _yMax) {
		xMin = _xMin;
		yMin = _yMin;
		xMax = _xMax;
		yMax = _yMax;
		writeDimensions();
	}

	public void setDimensions(int paperType) {
		switch (paperType) {
		case 0: // a4
			xMin = 430;
			yMin = 200;
			xMax = 10430;
			yMax = 7400;
			break;
		case 1: // a3
			xMin = 380;
			yMin = 430;
			xMax = 15580;
			yMax = 10430;
			break;
		case 2: // size A
			xMin = 80;
			yMin = 320;
			xMax = 10080;
			yMax = 7520;
			break;
		case 3: // size B
			xMin = 620;
			yMin = 80;
			xMax = 15820;
			yMax = 10080;
			break;
		default: // a4
			parent.println("Unknow paper type, setting default size A4");
			xMin = 430;
			yMin = 200;
			xMax = 10430;
			yMax = 7400;
			break;
		}
		writeDimensions();
	}

	void writeDimensions() {
		write("IW" + xMin + "," + yMin + "," + xMax + "," + yMax + ";");
	}

	// LABELS

	public void writeLabel(String _label, int xPos, int yPos) {
		if (PLOTTING_ENABLED) {
			write("PU" + xPos + "," + yPos + ";"); // Position pen
			write("LB" + _label + END_OF_TEXT); // Draw label
		}
	}

	// set in cm
	public void setFontHeight(double h) {
		characterWidth = h;
		characterHeight = h;
		write("SI" + characterWidth + "," + characterHeight + ";");
	}

	public double getFontHeight() {
		return characterWidth;
	}

	public void setLabelDirection(float angle) {
		String c = parent.nf(parent.cos(parent.radians(angle)), 1, 4); // the
																		// precision
																		// in
																		// our
																		// conversion
																		// is 4
																		// decimals
		String s = parent.nf(parent.sin(parent.radians(angle)), 1, 4);
		write("DR" + c + "," + s + ";");
	}

	public void writeLabel(String _label, float xPos, float yPos) {
		writeLabel(_label, (int) xPos, (int) yPos);
	}

	// MOVEMENT

	public void moveTo(int xPos, int yPos) {
		if (PLOTTING_ENABLED) {
			write("PU" + xPos + "," + yPos + ";"); // Go to specified position
		}
	}

	public void drawTo(int xPos, int yPos) {
		if (PLOTTING_ENABLED) {
			write("PD" + xPos + "," + yPos + ";"); // Go to specified position
		}
	}

	// SHAPES

	public void line(float x1, float y1, float x2, float y2) {
		// _this.line(x1 / 10, y1 / 10, x2 / 10, y2 / 10);
		write("PU" + (int) x1 + "," + (int) y1 + ";");
		write("PD" + (int) x2 + "," + (int) y2 + ";");
	}

	public void circle(int x, int y, int r) {
		circle(x, y, r, 0.5);
	}

	public void circle(int x, int y, int r, double precision) {
		write("PU" + (int) x + "," + (int) y + ";");
		write("CI" + r + "," + precision + ";");
	}

	public void bezier(int x1, int y1, int x2, int y2, int x3, int y3, int x4,
			int y4) {

		double increment = 1.0 / bezierDetailLevel;
		moveTo(x1, y1);

		for (double t = 0.00; t < 1.01; t = t + increment) {
			Point p = getPointOnBezier(x1, y1, x2, y2, x3, y3, x4, y4, t);
			drawTo((int) p.getX(), (int) p.getY());
		}
	}

	public void bezierDetail(double _detail) {
		bezierDetailLevel = _detail;
	}

	private Point getPointOnBezier(int point1x, int point1y, int point2x,
			int point2y, int point3x, int point3y, int point4x, int point4y,
			double t) {

		double xValue = Math.pow((1 - t), 3) * point1x + 3
				* Math.pow((1 - t), 2) * t * point2x + 3 * (1 - t)
				* Math.pow(t, 2) * point3x + Math.pow(t, 3) * point4x;
		double yValue = Math.pow((1 - t), 3) * point1y + 3
				* Math.pow((1 - t), 2) * t * point2y + 3 * (1 - t)
				* Math.pow(t, 2) * point3y + Math.pow(t, 3) * point4y;

		return new Point((int) xValue, (int) yValue);
	}

}
