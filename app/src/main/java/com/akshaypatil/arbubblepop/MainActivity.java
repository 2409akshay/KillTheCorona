package com.akshaypatil.arbubblepop;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.hardware.Sensor.TYPE_GYROSCOPE;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    //region --Variables--
    SensorManager mSensorManager;
    Sensor mGyroscope, mAccelerometer, mMagneticField;
    float[] gyroscopeValues, accValues, geoValues;
    float[] originOrientation;
    int[] imageResourceArray;
    float handRad;
    int screenWidth, screenHeight;
    float widthMultiplicationFactor, heightMultiplicationFactor, maxHorizontalDisplacement, maxVerticalDisplacement;
    ArrayList<Integer> imageViewList;
    int maxObjectNumber, objGenerationTimeOut, scoreNumber, timerDuration, timerResetDuration;
    ColorStateList timerColor;
    AtomicBoolean stopLoop;
    float alpha;
    //endregion

    //region --Handlers and Runnables--
    Thread vibrationThread;
    Handler screenMoveHandler;
    Runnable screenMoveRun = new Runnable() {
        @Override
        public void run() {
            if(stopLoop.get())
            {
                screenMoveHandler.removeCallbacks(screenMoveRun);
                TextView scoreMsg = ((TextView)findViewById(R.id.scoreMsg));
                scoreMsg.setText("Your Score is "+Integer.toString(scoreNumber));
                scoreMsg.setVisibility(View.VISIBLE);
                scoreMsg.animate().alpha(1f).setDuration(4000).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        recreate();
                    }
                });
            }
            else {
                ScreenMovementCalculator();
                ((TextView) findViewById(R.id.txtScore)).setText(Integer.toString(scoreNumber));
                if (timerDuration < 5) {
                    ((TextView) findViewById(R.id.txtTimer)).setTextColor(Color.RED);
                } else {
                    ((TextView) findViewById(R.id.txtTimer)).setTextColor(timerColor);
                }
                ((TextView) findViewById(R.id.txtTimer)).setText(Integer.toString(timerDuration));
                screenMoveHandler.postDelayed(screenMoveRun, 10);
            }
        }
    };

    Handler objectGeneratorHandler;
    Runnable objectGeneratorRunnable = new Runnable() {
        @Override
        public void run() {
            if(stopLoop.get())
            {
                objectGeneratorHandler.removeCallbacks(objectGeneratorRunnable);
            }
            else {
                if (imageViewList.size() < maxObjectNumber) {
                    ObjectGeneration();
                }
                objectGeneratorHandler.postDelayed(objectGeneratorRunnable, objGenerationTimeOut);
            }
        }
    };

    HandlerThread timerHandlerThread;
    Handler timerHandler;
    Runnable timerRun = new Runnable() {
        @Override
        public void run() {
            if(timerDuration > 0) {
                timerDuration -= 1;
                timerHandler.postDelayed(timerRun, 1000);
            }
            else
            {
                stopLoop.set(true);
                timerHandler.removeCallbacks(timerRun);
            }
        }
    };

    @SuppressLint("NewApi")
    private void ObjectGeneration()
    {
        double randomX = Math.random();
        randomX = randomX * maxHorizontalDisplacement;

        double randomY = Math.random();
        randomY = randomY * maxVerticalDisplacement;

        final RelativeLayout rLayout = findViewById(R.id.KTCMainScreen);
        final ImageView imgView = new ImageView(rLayout.getContext());
        imgView.setId(View.generateViewId());
        //imgView.setImageResource();
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(250, 250);
        imgView.setX((float)randomX);
        imgView.setY((float)randomY);
        imgView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        imgView.setLayoutParams(params);
        imgView.setImageResource(imageResourceArray[((int)randomY)%4]);
        rLayout.addView(imgView);
        imgView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (vibrationThread.getState() == Thread.State.NEW)
                {
                    vibrationThread.start();
                }
                else {
                    vibrationThread.run();
                }

                final RelativeLayout rLayout = findViewById(R.id.KTCMainScreen);
                int imageViewID = view.getId();
                rLayout.removeView(view);
                scoreNumber += 10;
                final TextView scoreView = ((TextView)findViewById(R.id.txtScore));
                scoreView.animate().scaleX(1.2f).scaleY(1.2f).setDuration(300).start();

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        scoreView.animate().scaleX(1).scaleY(1).setDuration(300).start();
                    }
                }, 300);
                if(imageViewList.indexOf(imageViewID) > -1) {
                    imageViewList.remove(imageViewList.indexOf(imageViewID));
                }

                if(scoreNumber > 250)
                {
                    timerResetDuration = 5;
                }
                else if(scoreNumber > 200)
                {
                    timerResetDuration = 10;
                }
                else if(scoreNumber > 150)
                {
                    timerResetDuration = 15;
                }
                else if(scoreNumber > 100)
                {
                    timerResetDuration = 20;
                }
                else if(scoreNumber > 50)
                {
                    timerResetDuration = 25;
                }
                else
                {
                    timerResetDuration = 30;
                }
                timerDuration = timerResetDuration;
            }
        });
        imageViewList.add(imgView.getId());
    }

    private void ScreenMovementCalculator()
    {
        for (int imageViewId : imageViewList) {
            ImageView imgView = ((ImageView) findViewById(imageViewId));
            RelativeLayout.LayoutParams relativeLayoutParams = (RelativeLayout.LayoutParams) imgView.getLayoutParams();
            float imgX = imgView.getX();
            float imgY = imgView.getY();

            imgX = imgX + gyroscopeValues[1] * widthMultiplicationFactor;
            imgY = imgY + gyroscopeValues[0] * heightMultiplicationFactor;

            //Log.i("X-Y:"+Integer.toString(imageViewId), String.valueOf(imgX) + " | " + String.valueOf(imgY));
            if (Math.abs(imgX) > maxHorizontalDisplacement) {
                if (imgX < 0) {
                    imgX += maxHorizontalDisplacement;
                } else {
                    imgX -= maxHorizontalDisplacement;
                }
            }

            if (Math.abs(imgY) > maxVerticalDisplacement) {
                if (imgY < 0) {
                    imgY += maxVerticalDisplacement;
                } else {
                    imgY -= maxVerticalDisplacement;
                }
            }
            imgView.setX(imgX);
            imgView.setY(imgY);
        }
    }
    //endregion

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();
        ((TextView)findViewById(R.id.txtTimer)).setBackgroundColor(Color.parseColor("#FFFFFF"));
        ((TextView)findViewById(R.id.txtScore)).setBackgroundColor(Color.parseColor("#FFFFFF"));

        //region --Sensor initialization--
        alpha = 0.8f;
        gyroscopeValues = new float[3];
        //accValues = new float[3];
        //geoValues = new float[3];
        //originOrientation = new float[3];
        //originOrientation[0] = -10000f;
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mGyroscope = mSensorManager.getDefaultSensor(TYPE_GYROSCOPE);
        //mAccelerometer = mSensorManager.getDefaultSensor(TYPE_ACCELEROMETER);
        //mMagneticField = mSensorManager.getDefaultSensor(TYPE_MAGNETIC_FIELD);
        //endregion

        //region --Gather Screen Details--
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        widthMultiplicationFactor = (float)screenWidth/metrics.xdpi * 15f;
        heightMultiplicationFactor = (float)screenHeight/metrics.ydpi * 15f;
        maxHorizontalDisplacement = (float)(2666*Math.PI);
        maxVerticalDisplacement = (float)(2666*Math.PI);
        //maxVerticalDisplacement = (320)*heightMultiplicationFactor;
        //endregion

        //region --Variable initialization--
        imageViewList = new ArrayList<Integer>();
        maxObjectNumber = 20;
        objGenerationTimeOut = 3000;
        scoreNumber = 0;
        timerResetDuration = 30;
        timerDuration = timerResetDuration;
        timerColor = ((TextView)findViewById(R.id.txtTimer)).getTextColors();
        stopLoop = new AtomicBoolean(false);
        imageResourceArray = new int[]{R.drawable.vector1,R.drawable.vector2,R.drawable.vector3,R.drawable.vector4};
        //endregion

        findViewById(R.id.btnStart).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                findViewById(R.id.welcome).setVisibility(View.INVISIBLE);
                InitiateNewGame();
            }
        });
    }
    @Override
    public void recreate() {
        super.recreate();
    }
    void InitiateNewGame()
    {
        findViewById(R.id.KTCMainScreen).setVisibility(View.VISIBLE);
        //region --Handler initialization--
        screenMoveHandler = new Handler();
        screenMoveHandler.postDelayed(screenMoveRun,10);
        objectGeneratorHandler = new Handler();
        objectGeneratorHandler.postDelayed(objectGeneratorRunnable,100);
        HandlerThread timerHandlerThread = new HandlerThread("timerHandlerThread");
        timerHandlerThread.start();
        timerHandler = new Handler(timerHandlerThread.getLooper());
        timerHandler.postDelayed(timerRun, 100);

        vibrationThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(30);
            }
        });
        //endregion
    }

    @Override
    protected void onResume() {
        super.onResume();
        //mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        //mSensorManager.registerListener(this, mMagneticField, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    public void onSensorChanged(SensorEvent sensorEvent) {
        GatherEventData(sensorEvent);
        /*if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accValues[0] = sensorEvent.values[0];
            accValues[1] = sensorEvent.values[1];
            accValues[2] = sensorEvent.values[2];
        }*/

        /*if (sensorEvent.sensor.getType() == TYPE_ACCELEROMETER) {
            accValues[0] = alpha * accValues[0] + (1 - alpha) * sensorEvent.values[0];
            accValues[1] = alpha * accValues[1] + (1 - alpha) * sensorEvent.values[1];
            accValues[2] = alpha * accValues[2] + (1 - alpha) * sensorEvent.values[2];
        }

        if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            geoValues[0] = sensorEvent.values[0];
            geoValues[1] = sensorEvent.values[1];
            geoValues[2] = sensorEvent.values[2];
        }
        float r[] = new float[9];
        float v[] = new float[3];
        float i[] = new float[9];
        boolean success = SensorManager.getRotationMatrix(r, i, accValues, geoValues);

        if (success) {
//            float [] A_D = sensorEvent.values.clone();
//            float [] A_W = new float[3];
//            A_W[0] = r[0] * A_D[0] + r[1] * A_D[1] + r[2] * A_D[2];
//            A_W[1] = r[3] * A_D[0] + r[4] * A_D[1] + r[5] * A_D[2];
//            A_W[2] = r[6] * A_D[0] + r[7] * A_D[1] + r[8] * A_D[2];
//
//            if(originOrientation[0] == -10000f) {
//                originOrientation = A_W.clone();
//            }
//            else {
//                //Log.d("Field", "X :" + (A_W[0] - originOrientation[0]) + "|Y :" + (A_W[1] - originOrientation[1]) + "|Z :" + (A_W[2] - originOrientation[2]));
//                Log.d("Field", "X :" + A_W[0] + "|Y :" + A_W[1] + "|Z :" + A_W[2]);
//            }
            SensorManager.getOrientation(r, v);
            if(originOrientation[0] == -10000) {
                originOrientation[0] = v[0];//azimuth - around z
                originOrientation[1] = v[1];//pitch - around X
                originOrientation[2] = v[2];//roll - around Y
            }
            else {
                Log.i("Orientation", Double.toString(Math.toDegrees(v[1] - originOrientation[1])) + "|" + Double.toString(Math.toDegrees(v[0] - originOrientation[0])));
            }
        }*/

    }

    private void GatherEventData(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == TYPE_GYROSCOPE) {
            gyroscopeValues[0] = sensorEvent.values[0];
            gyroscopeValues[1] = sensorEvent.values[1];
            gyroscopeValues[2] = sensorEvent.values[2];

            //Log.i("GyroScopeValues",String.valueOf(gyroscopeValues[1]) + " | " + String.valueOf(gyroscopeValues[0]));
        }
    }

    private String getStringValue(float[] values) {
        StringBuilder sb = new StringBuilder();
        for (float fl:values) {
            sb.append(String.valueOf(fl)+",");
        }
        return sb.toString();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

}