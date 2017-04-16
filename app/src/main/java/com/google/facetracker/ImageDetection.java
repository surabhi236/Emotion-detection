package com.google.facetracker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.affectiva.android.affdex.sdk.Frame;
import com.affectiva.android.affdex.sdk.detector.Detector;
import com.affectiva.android.affdex.sdk.detector.Face;
import com.affectiva.android.affdex.sdk.detector.PhotoDetector;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * Created by surabhi on 4/15/17.
 */
    /**
     * A sample app showing how to use ImageDetector.
     *
     * This app is not a production release and is known to have bugs. Specifically, the UI thread is blocked while the image is being processed,
     * and the app will crash if the user tries loading a very large image.
     *
     * For some images, facial tracking dots will not appear in the correct location.
     *
     * Also, the UI element that displays metrics is not aesthetic.
     *
     */
    public class ImageDetection extends Activity implements Detector.ImageListener {

        public static final String LOG_TAG = "Affectiva";
        public static final int PICK_IMAGE = 100;

        ImageView imageView;
        Button resultButton;

        Face face;
        PhotoDetector detector;
        Bitmap bitmap = null;
        Frame.BitmapFrame frame;
//        PieChart pieChart ;
//        ArrayList<Entry> entries ;
//        ArrayList<String> PieEntryLabels ;
//        PieDataSet pieDataSet ;
//        PieData pieData ;




        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_image_detection);
            resultButton = (Button) findViewById(R.id.showResultButton);
            initUI();

            Log.e(LOG_TAG, "onCreate");

            Intent gallery =
                    new Intent(Intent.ACTION_PICK,
                            android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
            startActivityForResult(gallery, PICK_IMAGE);

        }

        @Override
        protected void onResume() {
            super.onResume();
            Log.e(LOG_TAG, "onResume");

        }


        void startDetector() {
            if (!detector.isRunning()) {
                detector.start();
            }
        }

        @Override
        protected void onDestroy() {
            super.onDestroy();
            Log.e(LOG_TAG, "onDestroy");
        }

        @Override
        protected void onPause() {
            super.onPause();
            Log.e(LOG_TAG, "onPause");

        }

        void stopDetector() {
            if (detector.isRunning()) {
                detector.stop();
            }
        }

        void initUI() {


            imageView = (ImageView) findViewById(R.id.image_view);
        }

        void setAndProcessBitmap(Frame.ROTATE rotation, boolean isExpectingFaceDetection) {
            if (bitmap == null) {
                return;
            }

            switch (rotation) {
                case BY_90_CCW:
                    bitmap = Frame.rotateImage(bitmap,-90);
                    break;
                case BY_90_CW:
                    bitmap = Frame.rotateImage(bitmap,90);
                    break;
                case BY_180:
                    bitmap = Frame.rotateImage(bitmap,180);
                    break;
                default:
                    //keep bitmap as it is
            }

            frame = new Frame.BitmapFrame(bitmap, Frame.COLOR_FORMAT.UNKNOWN_TYPE);

            detector = new PhotoDetector(this,1, Detector.FaceDetectorMode.LARGE_FACES );
            detector.setDetectAllEmotions(true);
            detector.setDetectAllExpressions(true);
            detector.setDetectAllAppearances(true);
            detector.setImageListener(this);

            startDetector();
            detector.process(frame);
            stopDetector();

        }

        @SuppressWarnings("SuspiciousNameCombination")
        Bitmap drawCanvas(int width, int height, PointF[] points, Frame frame, Paint circlePaint) {
            if (width <= 0 || height <= 0) {
                return null;
            }

            Bitmap blackBitmap = Bitmap.createBitmap(width,height, Bitmap.Config.ARGB_8888);
            blackBitmap.eraseColor(Color.BLACK);
            Canvas c = new Canvas(blackBitmap);

            Frame.ROTATE frameRot = frame.getTargetRotation();
            Bitmap bitmap;

            int frameWidth = frame.getWidth();
            int frameHeight = frame.getHeight();
            int canvasWidth = c.getWidth();
            int canvasHeight = c.getHeight();
            int scaledWidth;
            int scaledHeight;
            int topOffset = 0;
            int leftOffset= 0;
            float radius = (float)canvasWidth/100f;

            if (frame instanceof Frame.BitmapFrame) {
                bitmap = ((Frame.BitmapFrame)frame).getBitmap();
            } else { //frame is ByteArrayFrame
                byte[] pixels = ((Frame.ByteArrayFrame)frame).getByteArray();
                ByteBuffer buffer = ByteBuffer.wrap(pixels);
                bitmap = Bitmap.createBitmap(frameWidth, frameHeight, Bitmap.Config.ARGB_8888);
                bitmap.copyPixelsFromBuffer(buffer);
            }

            if (frameRot == Frame.ROTATE.BY_90_CCW || frameRot == Frame.ROTATE.BY_90_CW) {
                int temp = frameWidth;
                frameWidth = frameHeight;
                frameHeight = temp;
            }

            float frameAspectRatio = (float)frameWidth/(float)frameHeight;
            float canvasAspectRatio = (float) canvasWidth/(float) canvasHeight;
            if (frameAspectRatio > canvasAspectRatio) { //width should be the same
                scaledWidth = canvasWidth;
                scaledHeight = (int)((float)canvasWidth / frameAspectRatio);
                topOffset = (canvasHeight - scaledHeight)/2;
            } else { //height should be the same
                scaledHeight = canvasHeight;
                scaledWidth = (int) ((float)canvasHeight*frameAspectRatio);
                leftOffset = (canvasWidth - scaledWidth)/2;
            }

            float scaling = (float)scaledWidth/(float)frame.getOriginalBitmapFrame().getWidth();

            Matrix matrix = new Matrix();
            matrix.postRotate((float)frameRot.toDouble());
            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap,0,0,frameWidth,frameHeight,matrix,false);
            c.drawBitmap(rotatedBitmap,null,new Rect(leftOffset,topOffset,leftOffset+scaledWidth,topOffset+scaledHeight),null);


            if (points != null) {
                //Save our own reference to the list of points, in case the previous reference is overwritten by the main thread.

                for (PointF point : points) {

                    //transform from the camera coordinates to our screen coordinates
                    //The camera preview is displayed as a mirror, so X pts have to be mirrored back.
                    float x = (point.x * scaling) + leftOffset;
                    float y = (point.y * scaling) + topOffset;

                    c.drawCircle(x, y, radius, circlePaint);
                }
            }

            return blackBitmap;
        }

        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            Log.e(LOG_TAG, "onActivityForResult");
            if (resultCode == RESULT_OK && requestCode == PICK_IMAGE) {

                Uri imageUri = data.getData();
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(),imageUri);
                    imageView.setImageBitmap(bitmap);

                } catch (IOException e) {
                    Toast.makeText(this,"Unable to open image.",Toast.LENGTH_LONG).show();
                }

                setAndProcessBitmap(Frame.ROTATE.NO_ROTATION, true);

            } else {
                Toast.makeText(this,"No image selected.",Toast.LENGTH_LONG).show();
            }
        }

        public void rotate_left(View view) {
            setAndProcessBitmap(Frame.ROTATE.BY_90_CCW, true);
        }

        public void rotate_right(View view) {
            setAndProcessBitmap(Frame.ROTATE.BY_90_CW,true);
        }

        @Override
        public void onImageResults(List<Face> faces, Frame image, float timestamp) {

            PointF[] points = null;

            if (faces != null && faces.size() > 0) {
                face = faces.get(0);

                points = face.getFacePoints();
            }


            Paint circlePaint = new Paint();
            circlePaint.setColor(Color.RED);
            circlePaint.setAlpha(75);

            Bitmap imageBitmap = drawCanvas(imageView.getWidth(),imageView.getHeight(),points,image,circlePaint);
            if (imageBitmap != null)
                imageView.setImageBitmap(imageBitmap);


        }

        public void sendResults(View v) {
            Intent resultIntent = new Intent(getApplicationContext(), ShowResultActivity.class);
            float results[] = new float[7];
            results[0] = face.emotions.getJoy();
            results[1] = face.emotions.getSadness();
            results[2] = face.emotions.getDisgust();
            results[3] = face.emotions.getContempt();
            results[4] = face.emotions.getFear();
            results[5] = face.emotions.getSurprise();


            resultIntent.putExtra("results",results);
            startActivity(resultIntent);
        }


        public void select_new_image(View view) {
            Intent galleryIntent =
                    new Intent(Intent.ACTION_PICK,
                            android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
            startActivityForResult(galleryIntent, PICK_IMAGE);
        }

        public Bitmap getBitmapFromAsset(Context context, String filePath) throws IOException {
            AssetManager assetManager = context.getAssets();

            InputStream istr;
            Bitmap bitmap;
            istr = assetManager.open(filePath);
            bitmap = BitmapFactory.decodeStream(istr);

            return bitmap;
        }

        public Bitmap getBitmapFromUri(Uri uri) throws FileNotFoundException {
            InputStream istr;
            Bitmap bitmap;
            istr = getContentResolver().openInputStream(uri);
            bitmap = BitmapFactory.decodeStream(istr);

            return bitmap;
        }




    }
