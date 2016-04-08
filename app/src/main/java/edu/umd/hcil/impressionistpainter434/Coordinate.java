package edu.umd.hcil.impressionistpainter434;

import android.graphics.Paint;
import android.graphics.Path;

/**
 * Created by Christine Schroeder on 3/28/2016.
 */
public class Coordinate {
    float xCoord;
    float yCoord;
    Paint paint;

    public Coordinate(float x_in, float y_in, Paint paint_in ){
        xCoord = x_in;
        yCoord = y_in;
        paint = paint_in;
    }
}
