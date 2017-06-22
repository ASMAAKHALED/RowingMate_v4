package info.android.rowing.activity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.constraint.solver.widgets.Snapshot;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.PointsGraphSeries;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import info.android.rowing.R;

import static android.R.attr.angle;
import static android.R.attr.data;
import static android.R.attr.value;
import static android.R.attr.x;
/*
import static info.android.rowing.R.id.x1;
import static info.android.rowing.R.id.x2;
import static info.android.rowing.R.id.x3;
import static info.android.rowing.R.id.x4;
*/
/**
 * Created by mahmoud_mashal on 2/2/2017.
 */

public class NewSessionActivity extends AppCompatActivity implements LocationListener, SensorEventListener, IBaseGpsListener {
    //public class NewSessionActivity extends AppCompatActivity implements  SensorEventListener {
    private int seconds = 0;
    private boolean running, wasRunning;
    private LocationManager locationManager;
    private TextView distance, Speed, strokeRate;
    private CLocation oldLocation = null;//////////////////////////////////
    private float[] results;
    float Dist = 0;
    float nCurrentSpeed = 0;

    private SensorManager senSensorManager; //object fi 5sa2s el sensor
    private Sensor senAccelerometer; //object fi el accelerometer
    //private final Handler mHandler = new Handler();
    private final Handler handler = new Handler();
    private Runnable mTimer0;
    private Runnable mTimer1;
    //private Runnable mTimer2;
    private LineGraphSeries<DataPoint> mSeries1;
    //private PointsGraphSeries<DataPoint> mSeries2;
    // private double graph2LastXValue = 5d;
    private int acc_count = 50, smooth_count = 3, l = 0; //j=0;
    private double[] acc_values = new double[acc_count];
    private double[] acc_smooth = new double[smooth_count];
    private double avg_acc = 0;
    private long lastUpdate = 0, lastUpdate2 = 0;
    private double last_x, last_y, last_z;
    //private static final int SHAKE_THRESHOLD = 400;
    private double acc_x = 0, acc_y = 0, acc_z = 0, speed_Mag, boat_dir_x, boat_dir_z;
    private long curTime, diffTime, curTime2;
    //private long curTime_Orientation, diffTime_Orientation, lastUpdate_Orientation;
    //private double speed_x,speed_y,speed_z;
    private double time_sec = 0;
    private double pthreshold = 2, nthreshold = -0.25;
    //private double[] yPoints=new double[20];
    private static int i = 0;
    private static int flag = 0, flag2 = 0, flag3 = 0;
    private static double ymin = -0.25;
    private double ymax = 0, ymaxGraph = 5, yminGraph = -5;
    private int strokeCount = 0, lastStrokeCount = 0;
    private double avgStrRate = 0, strRate;
    private int lastTime = 0;
    private int time200m = 0;
    //private int lastminute=0;
    private double gravity[] = {0, 0, 0};
    private double linear_acceleration[] = {0, 0, 0};
    private double acc_x_t = 0, acc_y_t = 0, acc_z_t = 0;
    ///firebase variables
    private DatabaseReference mDatabase;

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mMessageDatabaseReference;
    private DatabaseReference mMessageDatabaseReference2;

    private ChildEventListener mChildEventListener;
    //private String mUsername;

    private FirebaseAuth mFirebaseAuth;//
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private static final int RC_SIGN_IN = 1;//request code
    //private MessageAdapter mMessageAdapter;
    // private EditText mMessageEditText;
    private String mUsername;
    private DatabaseReference usersRef;
    private DatabaseReference usersRef2;
    private DatabaseReference ref;
    private String time;
    private String name;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_session);
//to keep the screen light on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mFirebaseDatabase=FirebaseDatabase.getInstance(); //getinstance(),Gets the default FirebaseDatabase instance
        mMessageDatabaseReference=mFirebaseDatabase.getReference().child("Message");

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);//////////////
        this.updateSpeed(null);////////////
        if (savedInstanceState != null) {
            seconds = savedInstanceState.getInt("seconds");
            running = savedInstanceState.getBoolean("running");
            wasRunning = savedInstanceState.getBoolean("wasRunning");
        }
        runTimer();

        GraphView graph = (GraphView) findViewById(R.id.acceleration_graph);
        //Viewport viewport = graph.getViewport();
        mSeries1 = new LineGraphSeries<>(generateData());
        graph.addSeries(mSeries1);


        Viewport viewport = graph.getViewport();
        viewport.setScrollable(true);

        viewport.setXAxisBoundsManual(true);
        viewport.setMinX(0);
        viewport.setMaxX(50);
        viewport.setYAxisBoundsManual(true);
        viewport.setMinY(-5);
        viewport.setMaxY(5);

        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE); //3mlna access lel sensor resource w 7atena el service dy fi variable esmo senSensorManager
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER); //7atet el sensor el default fi el object bta3y
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL); //3malt regiter lel accelerometer bta3y fi el maneger w ch8lto b normal delay
        for (int k = 0; k < acc_count; k++) {
            acc_values[k] = 0;
        }


        //firebase/networking

       /////write data



        final FirebaseDatabase database = FirebaseDatabase.getInstance();

        ///send data every second 1000ms////////////////
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {


                name="asmaa "; //double as=7;
                 RowerData mRowerData=new RowerData( name,strRate, nCurrentSpeed,angle,Dist,time) ;

                mMessageDatabaseReference.push().setValue(mRowerData); //---+-+-+-+

//what ever we want to send we can make a child  for it

                submitPost();

            }
        }, 0, 7000); // 7 sec

        //authentication


    } ///end oncreate
    private void submitPost() {

        mMessageDatabaseReference.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                     for (DataSnapshot asmaa : dataSnapshot.getChildren()) {
                         String string1 = String.valueOf(asmaa.getValue());
                         TextView kh1 = (TextView) findViewById(R.id.as1);
                         kh1.setText((string1));


/*
                         String string1 = String.valueOf(asmaa.child("strokeRate").getValue());
                         TextView kh1 = (TextView) findViewById(R.id.as1);
                         kh1.setText(("str.rat "+string1));

                        String string2 = String.valueOf(asmaa.child("name").getValue());
                         TextView kh2 = (TextView) findViewById(R.id.Rname);
                         kh2.setText(( " name "+string2));

                         String string3 = String.valueOf(asmaa.child("angle").getValue());
                         TextView kh3 = (TextView) findViewById(R.id.Rangle);
                         kh3.setText(("angle "+ string3));

                         String string4 = String.valueOf(asmaa.child("speed").getValue());
                         TextView kh4 = (TextView) findViewById(R.id.Rspeed);
                         kh4.setText(("speed "+string4));

                         String string5 = String.valueOf(asmaa.child("distance").getValue());
                         TextView kh5 = (TextView) findViewById(R.id.Rdist);
                         kh5.setText(("dist "+string5));

                         String string6 = String.valueOf(asmaa.child("time").getValue());
                         TextView kh6 = (TextView) findViewById(R.id.Rtime);
                         kh6.setText(("time "+string6));
*/


                     }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }

                });
    }








    @Override
    protected void onPause() {
        super.onPause();
        wasRunning = running;
        running = false;

        senSensorManager.unregisterListener(this);
        handler.removeCallbacks(mTimer0);
        handler.removeCallbacks(mTimer1);
        //handler.removeCallbacks(mTimer2);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (wasRunning) {
            running = true;
        }
    }
    //-----------------------------------------------------------------------------------------------------
    //---------------------------------time----------------------------------------------------------------


    private void runTimer() {
        final TextView timeView = (TextView) findViewById(R.id.time);
        strokeRate = (TextView) findViewById(R.id.stroke_rate);

        mTimer0 = new Runnable() {
            @Override
            public void run() {
                int hours = seconds / 3600;
                int minutes = (seconds % 3600) / 60;
                int secs = seconds % 60;
                 time = String.format("%d:%02d:%02d", hours, minutes, secs);
                timeView.setText(time);
                if (running) {
                    seconds++;
                }
                //avgStrRate = (double)(strokeCount*60) / (double)seconds;
                handler.postDelayed(this, 1000);
            }
        };
        //handler.postDelayed(mTimer0,1000);
        handler.postDelayed(mTimer0, 100);
        mTimer1 = new Runnable() {
            @Override
            public void run() {
                if (running) {
                    GraphView graph = (GraphView) findViewById(R.id.acceleration_graph);
                    Viewport viewport = graph.getViewport();

                    mSeries1.resetData(generateData());//generateData());
                    viewport.setMinX(m - 10);
                    viewport.setMaxX(m);
                    // mSeries2.resetData(generateData());//generateData());
                    time200m++;
                    if (strokeCount > (lastStrokeCount + 4)) {
                        strRate = (strokeCount - lastStrokeCount) * 60 * 5 / (time200m - lastTime);
                        strokeRate.setText(String.valueOf(strRate) + "str/min");
                        lastTime = time200m;
                        lastStrokeCount = strokeCount;
                    }
                }
                //mSeries1.appendData(new DataPoint((double)j, acc_x), true, 200);
                handler.postDelayed(this, 200);
            }
        };
        handler.postDelayed(mTimer1, 100);

    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt("seconds", seconds);
        savedInstanceState.putBoolean("running", running);
        savedInstanceState.putBoolean("wasRunning", wasRunning);
    }

    //-------------------------------------------------------------------------------------------------
    // --------------------------------GPS-------------------------------------------------------------
    public void finish() {
        super.finish();
        System.exit(0);
    }

    private void updateSpeed(CLocation location) {
        // TODO Auto-generated method stub
        double nCurrentLong = 0;
        double nCurrentLat = 0;

        results = new float[10];
        if (location != null) {
            nCurrentSpeed = location.getSpeed();
            nCurrentLong = location.getLongitude();
            nCurrentLat = location.getLatitude();
            if (oldLocation != null) {
                Location.distanceBetween(oldLocation.getLatitude(), oldLocation.getLongitude(), nCurrentLat, nCurrentLong, results);
            }
            oldLocation = location;
        }
        Dist = Dist + results[0];
        String strUnits = "m/sec";
        TextView Speed = (TextView) this.findViewById(R.id.pace);
        Speed.setText(String.valueOf(nCurrentSpeed) + " " + strUnits);
        TextView distance = (TextView) this.findViewById(R.id.distance);
        distance.setText(String.valueOf(Dist) + "m");
    }

    @Override
    public void onLocationChanged(Location location) {
        // TODO Auto-generated method stub
        if (location != null) {
            CLocation myLocation = new CLocation(location);
            if (running) {
                this.updateSpeed(myLocation);
            }
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onGpsStatusChanged(int event) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {

    }


    //--------------------------------------------------------------------------------------------------------
    // --------------------------------STROKE RATE-------------------------------------------------------------
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private int j = 0;
    private double m = 0;

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor mySensor = sensorEvent.sensor;

        GraphView graph = (GraphView) findViewById(R.id.acceleration_graph);
        Viewport viewport = graph.getViewport();
        if (running) {
            if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                mySensor = sensorEvent.sensor;
                if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {

                    final float alpha = (float) 0.8;
                    gravity[0] = alpha * gravity[0] + (1 - alpha) * sensorEvent.values[0];
                    gravity[1] = alpha * gravity[1] + (1 - alpha) * sensorEvent.values[1];
                    gravity[2] = alpha * gravity[2] + (1 - alpha) * sensorEvent.values[2];
                    acc_x = sensorEvent.values[0] - gravity[0];
                    acc_y = sensorEvent.values[1] - gravity[1];
                    acc_z = sensorEvent.values[2] - gravity[2];
                    //////titing/////////
                    double norm_Of_g = Math.sqrt(acc_x * acc_x + acc_y * acc_y + acc_z * acc_z);
                    acc_x_t = acc_x;
                    acc_y_t = acc_y;
                    acc_z_t = acc_z;
                    // Normalize the accelerometer vector

                    acc_x_t = acc_x_t / norm_Of_g;
                    acc_y_t = acc_y_t / norm_Of_g;
                    acc_z_t = acc_z_t / norm_Of_g;
                    // inclination can be calculated as
                    int inclination = (int) Math.round(Math.toDegrees(Math.acos(acc_z_t)));

                        // device is flat
//////measure the angle 
                        int angle = (int) Math.round(Math.toDegrees(Math.atan2(acc_y_t, acc_x_t)));
                        TextView angle1 = (TextView) findViewById(R.id.angle);
                        angle1.setText(String.valueOf(angle));
//*******tilt by acc

  //*****
                    //

                     angle = (int) Math.round(Math.toDegrees(Math.atan2(linear_acceleration[0], linear_acceleration[1])));


                        // device is not flat
                    }
                /*
                    TextView kh1 = (TextView) findViewById(x1);
                    kh1.setText((" x :)  "+acc_x_t ));
                    TextView kh2 = (TextView) findViewById(x2);
                    kh2.setText((" y :)  "+acc_y_t ));
                    TextView kh3 = (TextView) findViewById(x3);
                    kh3.setText((" z :)  "+acc_z_t ));
                TextView kh4= (TextView) findViewById(x4);
                kh4.setText(("angle "+angle ));
*/


                    //mSeries1.appendData(new DataPoint((double)j, acc_x), true, 250);
                    acc_smooth[l] = acc_z;
                    if (l < (smooth_count - 1)) {
                        l++;
                    } else {
                        l = 0;
                    }

                    avg_acc = (acc_smooth[0] + acc_smooth[1] + acc_smooth[2]) / 3;

                    for (int k = 1; k < acc_count; k++) {
                        acc_values[k - 1] = acc_values[k];
                    }
                    acc_values[49] = avg_acc;


                    if (avg_acc > (pthreshold) && flag == 0) {
                        if (avg_acc > ymax) {
                            ymax = avg_acc;
                        }
                        flag2 = 1; //lma da yb2a 1 m3nah en el acc 3det el +ve threshold
                    }

                    if (avg_acc <= (nthreshold) && flag == 0 && flag2 == 1) {
                        if (ymax > ymaxGraph) {
                            ymaxGraph = ymax;
                            viewport.setMaxY((int) 1.5 * ymaxGraph);
                        }
                        flag = 1; //da ma3nah eny lazem ad5ol fi el -ve
                        flag2 = 0; //l2any hdawar 3ala el min
                        flag3 = 0;
                        pthreshold = 0.6 * ymax;
                        if (pthreshold < 1) {
                            pthreshold = 1;
                        }
                        ymax = 0;
                    }

                    if (avg_acc <= nthreshold && flag == 1) {
                        if (avg_acc < ymin) {
                            ymin = avg_acc;
                        }
                        flag2 = 1;
                    }

                    if (avg_acc >= pthreshold && flag == 1 && flag2 == 1) {
                        strokeCount++;
                      /*  distance = (TextView)findViewById(R.id.distance);
                        distance.setText(String.valueOf(strokeCount) );*/
                        TextView strokeCount1 = (TextView) findViewById(R.id.strokeCount);
                        strokeCount1.setText(String.valueOf(strokeCount));


                        flag = 0; //da ma3nah eny lazem ad5ol fi el +ve
                        flag2 = 0; //l2any hdawar 3ala el max
                        //nthreshold = 0.25 * ymin;
                        nthreshold = 0.6 * ymin;
                        if (nthreshold > -1) {
                            nthreshold = -1;
                        }
                        if (ymin < yminGraph) {
                            yminGraph = ymin;
                            viewport.setMinY((int) 1.5 * yminGraph);
                        }
                        ymin = 0;
                        //for (int k = 0; k < acc_count; k++) {
                        //   acc_values[k] = 0;
                        //}
                    }
                }
            }
        }


    double x_axes, y_axes;

    private DataPoint[] generateData() {
        //int count = 50;
        DataPoint[] values = new DataPoint[acc_count];
        for (int i = 0; i < acc_count; i++) {
            x_axes = m;
            y_axes = acc_values[i];
            DataPoint v = new DataPoint(x_axes, y_axes);
            values[i] = v;
            m = m + 0.2;

        }
        return values;
    }


    //--------------------------------------------------------------------------------------------------------
    // ----------------menu-----------------------------------------------------------------------------------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.new_session, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        distance = (TextView) findViewById(R.id.distance);
        Speed = (TextView) findViewById(R.id.pace);
        strokeRate = (TextView) findViewById(R.id.stroke_rate);
        if (id == R.id.start_session) {
            running = true;
            if (wasRunning) {
                running = true;
            }
        }
        if (id == R.id.stop_session) {
            running = false;



            distance.setText(String.valueOf(Dist) + " m ");
            Speed.setText(String.valueOf(nCurrentSpeed) + " m/s");
            strokeRate.setText(String.valueOf(strRate) + "str/min");
        }

        if (id == R.id.reset_session) {
            running = false;
            seconds = 0;
            Dist = 0;
            nCurrentSpeed = 0;
            strokeCount = 0;
            time200m = 0;


            distance.setText(String.valueOf(Dist) + " m ");
            Speed.setText(String.valueOf(nCurrentSpeed) + " m/s");
            strokeRate.setText(String.valueOf(strRate) + "str/min");
        }
        //noinspection SimplifiableIfStatement
        if (id == android.R.id.home) {
            // finish the activity
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }



}