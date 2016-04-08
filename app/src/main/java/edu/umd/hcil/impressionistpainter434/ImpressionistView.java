package edu.umd.hcil.impressionistpainter434;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.effect.Effect;
import android.media.effect.EffectContext;
import android.media.effect.EffectFactory;
import android.os.Environment;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.support.v4.view.VelocityTrackerCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.reflect.Array;
import java.nio.IntBuffer;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by jon on 3/20/2016.
 */
public class ImpressionistView extends View {

    private ImageView _imageView;

    private Canvas _offScreenCanvas = null;
    private Bitmap _offScreenBitmap = null;
    Bitmap sketched = null;
    Bitmap greyscale = null;
    Bitmap blurred = null;
    Bitmap img_bitmap = null;
    private Paint _paint = new Paint();

    private int _alpha = 150;
    private Paint _paintBorder = new Paint();
    private BrushType _brushType = BrushType.Circle;

    int image_top, image_left;
    float image_scaleX, image_scaleY;
    int image_heightActual, image_widthActual;
    VelocityTracker velocity;

    Path curr_path;

    float xPos = 0;
    float yPos = 0;


    public ImpressionistView(Context context) {
        super(context);
        init(null, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    /**
     * Because we have more than one constructor (i.e., overloaded constructors), we use
     * a separate initialization method
     * @param attrs
     * @param defStyle
     */
    private void init(AttributeSet attrs, int defStyle){

        // Set setDrawingCacheEnabled to true to support generating a bitmap copy of the view (for saving)
        // See: http://developer.android.com/reference/android/view/View.html#setDrawingCacheEnabled(boolean)
        //      http://developer.android.com/reference/android/view/View.html#getDrawingCache()
        this.setDrawingCacheEnabled(true);
        this.buildDrawingCache();

        _paint.setColor(Color.WHITE);
        _paint.setAlpha(_alpha);
        _paint.setAntiAlias(true);
        //_paint.setStyle(Paint.Style.STROKE);
        _paint.setStyle(Paint.Style.FILL);
        _paint.setStrokeWidth(4);

        _paintBorder.setColor(Color.BLACK);
        _paintBorder.setStrokeWidth(3);
        _paintBorder.setStyle(Paint.Style.STROKE);
        _paintBorder.setAlpha(50);

        curr_path = new Path();
        velocity = VelocityTracker.obtain();

    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh){

        Bitmap bitmap = getDrawingCache();
        Log.v("onSizeChanged", MessageFormat.format("bitmap={0}, w={1}, h={2}, oldw={3}, oldh={4}", bitmap, w, h, oldw, oldh));
        if(bitmap != null) {
            _offScreenBitmap = getDrawingCache().copy(Bitmap.Config.ARGB_8888, true);
            _offScreenCanvas = new Canvas(_offScreenBitmap);
        }
    }

    /**
     * Sets the ImageView, which hosts the image that we will paint in this view
     * @param imageView
     */
    public void setImageView(ImageView imageView){

        _imageView = imageView;



    }

    /**
     * Sets the brush type. Feel free to make your own and completely change my BrushType enum
     * @param brushType
     */
    public void setBrushType(BrushType brushType){
        _brushType = brushType;
    }

    /**
     * Clears the painting
     */
    public void clearPainting(){
        if(_offScreenCanvas != null) {
            curr_path = new Path();
            Paint paint = new Paint();
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.FILL);
            _offScreenCanvas.drawRect(0, 0, this.getWidth(), this.getHeight(), paint);
            invalidate();
        }
    }

    public Bitmap getBitmap(){
        return _offScreenBitmap;
    }
    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Draw the border. Helpful to see the size of the bitmap in the ImageView
        Rect r = getBitmapPositionInsideImageView(_imageView);


        if(_offScreenBitmap != null) {

            canvas.drawBitmap(_offScreenBitmap,0,0, null);
        }

        if(_brushType.name() == "Sketch"){
            _paint.setAlpha(125);
            _paint.setStrokeWidth(3);
            canvas.drawPath(curr_path, _paint);
        }

        canvas.drawRect(r, _paintBorder);

    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent){
        if(_imageView.getDrawable()!=null) {
            Paint p = new Paint();
            Rect r = getBitmapPositionInsideImageView(_imageView);
            int historySize = motionEvent.getHistorySize();
            //http://stackoverflow.com/questions/35250485/how-to-translate-scale-ontouchevent-coordinates-onto-bitmap-canvas-in-android-in

            float x = motionEvent.getX();
            float y = motionEvent.getY();

            if (img_bitmap == null) {
                img_bitmap = ((BitmapDrawable) _imageView.getDrawable()).getBitmap();
                //Bitmap bitmap = ((BitmapDrawable)_imageView.getDrawingCache;
            }

            if ((int) y < img_bitmap.getHeight() && y >= 0 && (int) x < img_bitmap.getWidth() && x >= 0) {
            //SKETCH
                if (_brushType.name() == "Sketch") {

                    //Bitmap greyed_bitmap = toGrayscale(bitmap);
                    if (greyscale == null || blurred == null || sketched == null) {
                        greyscale = toGrayscale(img_bitmap);
                        blurred = addGradient(greyscale);
                        sketched = ColorDodgeBlend(greyscale, blurred);
                    }

                    //http://stackoverflow.com/questions/32935843/apply-colorfilter-to-bitmap-using-brush-strokes
                    BitmapShader fillBMPshader = new BitmapShader(sketched, BitmapShader.TileMode.REPEAT, BitmapShader.TileMode.REPEAT);

                    _paint.setStyle(Paint.Style.STROKE);
                    _paint.setStrokeJoin(Paint.Join.ROUND);
                    _paint.setStrokeCap(Paint.Cap.ROUND);
                    _paint.setShader(fillBMPshader);

                    switch (motionEvent.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            curr_path.moveTo(x, y);
                            break;

                        case MotionEvent.ACTION_MOVE:
                            curr_path.lineTo(x, y);
                            break;

                        case MotionEvent.ACTION_UP:
                        /*for (int i = 0; i < historySize; i++) {
                            float historicalX = motionEvent.getHistoricalX(i);
                            float historicalY = motionEvent.getHistoricalY(i);

                            curr_path.lineTo(historicalX, historicalY);
                        }
                        curr_path.lineTo(x, y);*/
                            break;

                    }

//CIRCLE
                } else if (_brushType.name() == "Circle") {
                    Bitmap grad = addGradient(img_bitmap);
                    int color = grad.getPixel((int) x, (int) y);

                    _paint.setStyle(Paint.Style.FILL);
                    _paint.setShader(null);
                    switch (motionEvent.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            velocity.addMovement(motionEvent);
                        case MotionEvent.ACTION_MOVE:

                            velocity.addMovement(motionEvent);
                            velocity.computeCurrentVelocity(1000);
//http://developer.android.com/intl/zh-tw/training/gestures/movement.html
                            float x_velocity = Math.abs(VelocityTrackerCompat.getXVelocity(velocity,
                                    motionEvent.getPointerId(motionEvent.getActionIndex())));
                            float y_velocity = Math.abs(VelocityTrackerCompat.getYVelocity(velocity,
                                    motionEvent.getPointerId(motionEvent.getActionIndex())));
                            Log.i("X velocity", x_velocity + " ");
                            Log.i("Y velocity", y_velocity + " ");
                            float stroke_width = adjust_brushstroke(x_velocity, y_velocity);
                            p.setStrokeWidth(stroke_width);
                            for (int i = 0; i < historySize; i++) {
                                float touchX = motionEvent.getHistoricalX(i);
                                float touchY = motionEvent.getHistoricalY(i);

                                _offScreenCanvas.drawCircle(touchX, touchY, stroke_width, _paint);

                            }
                            _paint.setColor(color);
                            _paint.setAlpha(75);

                            _offScreenCanvas.drawCircle(x, y, stroke_width, _paint);
                            invalidate();

                        case MotionEvent.ACTION_UP:
                            break;
                    }
//SQUARE
                } else {

                    //int color = blurred_bitmap.getPixel((int) x, (int) y);
                    int color = img_bitmap.getPixel((int) x, (int) y);
                    _paint.setStyle(Paint.Style.STROKE);
                    _paint.setStrokeJoin(Paint.Join.ROUND);
                    _paint.setStrokeCap(Paint.Cap.ROUND);
                    _paint.setAlpha(100);
                    _paint.setShader(null);
                    switch (motionEvent.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            velocity.addMovement(motionEvent);
                        case MotionEvent.ACTION_MOVE:

                            velocity.addMovement(motionEvent);
                            velocity.computeCurrentVelocity(1000);
//http://developer.android.com/intl/zh-tw/training/gestures/movement.html
                            float x_velocity = Math.abs(VelocityTrackerCompat.getXVelocity(velocity,
                                    motionEvent.getPointerId(motionEvent.getActionIndex())));
                            float y_velocity = Math.abs(VelocityTrackerCompat.getYVelocity(velocity,
                                    motionEvent.getPointerId(motionEvent.getActionIndex())));
                            //Log.i("X velocity",x_velocity+" ");
                            //Log.i("Y velocity",y_velocity+" ");
                            float stroke_width = adjust_brushstroke(x_velocity, y_velocity);

                            for (int i = 0; i < historySize; i++) {
                                float touchX = motionEvent.getHistoricalX(i);
                                float touchY = motionEvent.getHistoricalY(i);
                                _offScreenCanvas.drawRect(touchX - stroke_width, touchY - stroke_width, touchX + stroke_width, touchY + stroke_width, _paint);
                            }
                            _paint.setColor(color);
                            _offScreenCanvas.drawRect(x - stroke_width, y - stroke_width, x + stroke_width, y + stroke_width, _paint);

                            //coordinates_arr.add(new Coordinate(x, y, p));
                        case MotionEvent.ACTION_UP:
                            break;
                    }
                    //p.setColor(color);
                    //coordinates_arr.add(new Coordinate(x, y, p));
                }
                Log.i("new point", x + " " + y);
            }


            invalidate();
        }
        return true;
    }



    private int colordodge(int in1, int in2) {
        float image = (float)in2;
        float mask = (float)in1;
        return ((int) ((image == 255) ? image:Math.min(255, (((long)mask << 8 ) / (255 - image)))));
    }

    public Bitmap ColorDodgeBlend(Bitmap source, Bitmap layer) {
        Bitmap base = source.copy(Bitmap.Config.ARGB_8888, true);
        Bitmap blend = layer.copy(Bitmap.Config.ARGB_8888, false);

        IntBuffer buffBase = IntBuffer.allocate(base.getWidth() * base.getHeight());
        base.copyPixelsToBuffer(buffBase);
        buffBase.rewind();

        IntBuffer buffBlend = IntBuffer.allocate(blend.getWidth() * blend.getHeight());
        blend.copyPixelsToBuffer(buffBlend);
        buffBlend.rewind();

        IntBuffer buffOut = IntBuffer.allocate(base.getWidth() * base.getHeight());
        buffOut.rewind();

        while (buffOut.position() < buffOut.limit()) {

            int filterInt = buffBlend.get();
            int srcInt = buffBase.get();

            int redValueFilter = Color.red(filterInt);
            int greenValueFilter = Color.green(filterInt);
            int blueValueFilter = Color.blue(filterInt);

            int redValueSrc = Color.red(srcInt);
            int greenValueSrc = Color.green(srcInt);
            int blueValueSrc = Color.blue(srcInt);

            int redValueFinal = colordodge(redValueFilter, redValueSrc);
            int greenValueFinal = colordodge(greenValueFilter, greenValueSrc);
            int blueValueFinal = colordodge(blueValueFilter, blueValueSrc);


            int pixel = Color.argb(255, redValueFinal, greenValueFinal, blueValueFinal);


            buffOut.put(pixel);
        }

        buffOut.rewind();

        base.copyPixelsFromBuffer(buffOut);
        blend.recycle();

        return base;
    }


    public float adjust_brushstroke(float x_vel, float y_vel){
        if(x_vel > 2000 || y_vel > 2000){
            return 70;
        }else if(x_vel > 700 || y_vel > 700){
            return 20;
        }else{
            return 5;
        }
    }
    //http://stackoverflow.com/questions/23053933/how-to-apply-the-grayscale-effect-to-the-bitmap-image
    public Bitmap toGrayscale(Bitmap bmp){
        Bitmap bmpOriginal = bmp.copy(bmp.getConfig(), true);
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();

        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);

        float[] colorMatrix_Negative = {
                -1.0f, 0, 0, 0, 255, //red
                0, -1.0f, 0, 0, 255, //green
                0, 0, -1.0f, 0, 255, //blue
                0, 0, 0, 1.0f, 0 //alpha
                 };
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.set(colorMatrix_Negative);

        //ColorFilter colorFilter_Negative = new ColorMatrixColorFilter(colorMatrix_Negative);

        ColorMatrix colorMatrixConcat = new ColorMatrix();
        colorMatrixConcat.setConcat(cm, colorMatrix);

        ColorMatrixColorFilter filter1 = new ColorMatrixColorFilter(colorMatrixConcat);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);


        return bmpGrayscale;
    }

    public Bitmap addGradient(Bitmap src) {
        int w = src.getWidth();
        int h = src.getHeight();
        Bitmap overlay = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(overlay);

        canvas.drawBitmap(src, 0, 0, null);

        Paint paint = new Paint();
        LinearGradient shader = new LinearGradient(0,  h - 22, 0, h, 0xFFFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP);
        paint.setShader(shader);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        canvas.drawRect(0, 22, w, h, paint);

        return overlay;
    }


    /**
     * This method is useful to determine the bitmap position within the Image View. It's not needed for anything else
     * Modified from:
     *  - http://stackoverflow.com/a/15538856
     *  - http://stackoverflow.com/a/26930938
     * @param imageView
     * @return
     */
    private Rect getBitmapPositionInsideImageView(ImageView imageView){
        Rect rect = new Rect();

        if (imageView == null || imageView.getDrawable() == null) {
            return rect;
        }

        // Get image dimensions
        // Get image matrix values and place them in an array
        float[] f = new float[9];
        imageView.getImageMatrix().getValues(f);

        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        image_scaleX = f[Matrix.MSCALE_X];
        image_scaleY = f[Matrix.MSCALE_Y];

        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        final Drawable d = imageView.getDrawable();
        final int origW = d.getIntrinsicWidth();
        final int origH = d.getIntrinsicHeight();

        // Calculate the actual dimensions
        image_widthActual = Math.round(origW * image_scaleX);
        image_heightActual = Math.round(origH * image_scaleY);

        // Get image position
        // We assume that the image is centered into ImageView
        int imgViewW = imageView.getWidth();
        int imgViewH = imageView.getHeight();

        image_top = (int) (imgViewH - image_heightActual)/2;
        image_left = (int) (imgViewW - image_widthActual)/2;

        rect.set(image_left, image_top, image_left + image_widthActual, image_top + image_heightActual);

        return rect;
    }
}