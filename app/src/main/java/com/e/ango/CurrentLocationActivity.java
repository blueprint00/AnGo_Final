package com.e.ango;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.e.ango.API.AirWeatherTask;
import com.e.ango.API.PlayTask;
import com.e.ango.API.Play.PlayObject;
import com.e.ango.ListView.ListAdapter;
import com.e.ango.ListView.ListData;
import com.e.ango.Recommend.RecommendTask;
import com.e.ango.SurveyConnection.LoadImage;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class CurrentLocationActivity extends AppCompatActivity
        implements OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback {

    private GoogleMap mMap;
    //    private Marker currentMarker = null;
    private Marker playMarker = null; /////////////////////////////////////////////////

    private static final String TAG = "googlemap_example";
    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int UPDATE_INTERVAL_MS = 1000;  // 1???
    private static final int FASTEST_UPDATE_INTERVAL_MS = 500; // 0.5???

    // onRequestPermissionsResult?????? ????????? ???????????? ActivityCompat.requestPermissions??? ????????? ????????? ????????? ???????????? ?????? ???????????????.
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    boolean needRequest = false;


    // ?????? ???????????? ?????? ????????? ???????????? ???????????????.
    String[] REQUIRED_PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};  // ?????? ?????????

    LatLng currentPosition;

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest locationRequest;
    private Location location;

    private View mLayout;  // Snackbar ???????????? ???????????? View??? ???????????????.
    // (????????? Toast????????? Context??? ??????????????????.)


    //////////////////////////////////////////////////////////////////????????? ??????
    //?????? ?????? ???
    public static TextView textView_Humidity, textView_Weather, textView_Temperature, textView_Air;
    public static ImageView imageView_weatherIcon; // ?????? ????????? ?????? ???
//    private int drawableIcon = R.drawable.untitled3; // ?????? ????????? ???

    public String currentLatitude, currentLongitude; // ?????? ?????? ??????, ??????

    private String cityName; // ??????????????? ???????????? ?????? ?????? ??????
    private String weather_type; // ?????? ??????

    private ArrayList<PlayObject> originalPlayObjects; // ?????? ?????? ?????????
    private ArrayList<PlayObject> userPreferencePlayObjects; // ???????????? ????????? ?????? ?????????

    //?????? ????????? ????????? ????????? ???
    public static ListView printList = null;

    private boolean flag_coordinate = false; // ?????? ?????? ?????? true
    private int weatherTypeInt;
    private Bitmap bitmap;

    ArrayList<ListData> arrListData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_current_location);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mLayout = findViewById(R.id.layout_main);

        locationRequest = new LocationRequest()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        // .setInterval(UPDATE_INTERVAL_MS) // ???????????? ?????? ???????????? ???????????? ????????? ??????x
        // .setFastestInterval(FASTEST_UPDATE_INTERVAL_MS);


        LocationSettingsRequest.Builder builder =
                new LocationSettingsRequest.Builder();

        builder.addLocationRequest(locationRequest);


        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        ////////////////////////////////////////////////////????????? ??????
        // ??????, ??????, ??????, ???????????? ???????????? ????????????
        textView_Humidity = (TextView) findViewById(R.id.textView_humidity);
        textView_Temperature = (TextView) findViewById(R.id.textView_temperature);
        textView_Weather = (TextView) findViewById(R.id.textView_weather);
        textView_Air = (TextView) findViewById(R.id.textView_air);

        //?????? ????????? ?????? ????????? ????????????
        imageView_weatherIcon = (ImageView) findViewById(R.id.imageView_WeatherIcon);

    }


    @Override
    public void onMapReady(final GoogleMap googleMap) {

        Log.d(TAG, "onMapReady :");

        mMap = googleMap;

        //????????? ????????? ??????
        // 1. ?????? ???????????? ????????? ????????? ???????????????.
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION);


        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED) {

            // 2. ?????? ???????????? ????????? ?????????
            // ( ??????????????? 6.0 ?????? ????????? ????????? ???????????? ???????????? ????????? ?????? ????????? ?????? ???????????????.)


            startLocationUpdates(); // 3. ?????? ???????????? ??????


        } else {  //2. ????????? ????????? ????????? ?????? ????????? ????????? ????????? ???????????????. 2?????? ??????(3-1, 4-1)??? ????????????.

            // 3-1. ???????????? ????????? ????????? ??? ?????? ?????? ????????????
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])) {

                // 3-2. ????????? ???????????? ?????? ?????????????????? ???????????? ????????? ????????? ???????????? ????????? ????????????.
                Snackbar.make(mLayout, "??? ?????? ??????????????? ?????? ?????? ????????? ???????????????.",
                        Snackbar.LENGTH_INDEFINITE).setAction("??????", new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {

                        // 3-3. ??????????????? ????????? ????????? ?????????. ?????? ????????? onRequestPermissionResult?????? ???????????????.
                        ActivityCompat.requestPermissions(CurrentLocationActivity.this, REQUIRED_PERMISSIONS,
                                PERMISSIONS_REQUEST_CODE);
                    }
                }).show();


            } else {
                // 4-1. ???????????? ????????? ????????? ??? ?????? ?????? ???????????? ????????? ????????? ?????? ?????????.
                // ?????? ????????? onRequestPermissionResult?????? ???????????????.
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);
            }

        }

        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

            @Override
            public void onMapClick(LatLng latLng) {

                Log.d(TAG, "onMapClick :");
            }
        });
    }


    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            Log.d(TAG , "locationCallback");
            super.onLocationResult(locationResult);

            List<Location> locationList = locationResult.getLocations();

            if (locationList.size() > 0) {
                location = locationList.get(locationList.size() - 1);
                //location = locationList.get(0);

                currentPosition
                        = new LatLng(location.getLatitude(), location.getLongitude());

                String markerSnippet = "??????:" + String.valueOf(location.getLatitude())
                        + " ??????:" + String.valueOf(location.getLongitude());

                Log.d(TAG, "onLocationResult : " + markerSnippet);

                //?????? ??????
                setCurrentLocation(location);

                ////////////////////////////////////????????? ?????? 1
                //???????????? ??????, ??????
                currentLatitude = String.valueOf(location.getLatitude());
                currentLongitude = String.valueOf(location.getLongitude());


                System.out.println("CURRENT : " + currentLongitude);
                if(currentLatitude != null && currentLongitude != null) flag_coordinate = true;
                if(flag_coordinate) setPlayLocation(currentLatitude, currentLongitude);
                System.out.println("FLAG_COORDINATES FIRST : " + flag_coordinate);
                //userPreferencePlayForMarker = new UserPreferencePlayForMarker(currentLatitude, currentLongitude);
            }
        }
    };


    private void startLocationUpdates() {

        if (!checkLocationServicesStatus()) {
            System.out.println("startLocationUpdates");

            Log.d(TAG, "startLocationUpdates : call showDialogForLocationServiceSetting");
            showDialogForLocationServiceSetting();
        } else {

            int hasFineLocationPermission = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION);
            int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION);


            if (hasFineLocationPermission != PackageManager.PERMISSION_GRANTED ||
                    hasCoarseLocationPermission != PackageManager.PERMISSION_GRANTED) {

                Log.d(TAG, "startLocationUpdates : ????????? ???????????? ??????");
                return;
            }

            Log.d(TAG, "startLocationUpdates : call mFusedLocationClient.requestLocationUpdates");

//            mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());

            if (checkPermission())
                mMap.setMyLocationEnabled(true);
            mMap.animateCamera(CameraUpdateFactory.zoomTo(15));

        }

    }

    @Override
    protected void onStart() {

        Log.d(TAG ,"onStart");
        super.onStart();

        Log.d(TAG, "onStart");

        if(mMap == null) {
            if (checkPermission()) {

                Log.d(TAG, "onStart : call mFusedLocationClient.requestLocationUpdates");
                mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);

                if (mMap != null)
                    mMap.setMyLocationEnabled(true);

            }
        }
        System.out.println("1");
    }


    @Override
    protected void onStop() {
        Log.d(TAG ,"onStop");

        super.onStop();

        if (mFusedLocationClient != null) {
            Log.d(TAG, "onStop : call stopLocationUpdates");
            mFusedLocationClient.removeLocationUpdates(locationCallback);
        }

    }


    public void setPlayLocation(String currentLatitude, String currentLongitude) {
        System.out.println("GETCURRENTADDRESS 4");
        Log.d(TAG ,"getCurrentAddress 2");

        ////////////////////////////////////????????? ?????? 2
        //flag_coordinate == true == ?????? ??????
        System.out.println("FLAG_COORDINATES  : " + flag_coordinate);
        if (flag_coordinate) { //????????? ???????????? ??? ?????? ?????? ??????
            System.out.println("FLAG_COORDINATES2  : " + flag_coordinate);


            userPreferencePlayObjects = getUserPreferencePlayObjects();

            //?????? ???????????? ???????????? ?????? ?????????
            if (userPreferencePlayObjects != null) {
                for (int i = 0; i < userPreferencePlayObjects.size(); i++) {
                    setPlayLocationMarker(userPreferencePlayObjects.get(i));
                }
            }

            //????????? ?????? ?????????
            printListData(userPreferencePlayObjects);

            printList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    Toast.makeText(CurrentLocationActivity.this, arrListData.get(position).getCategoryId(), Toast.LENGTH_LONG).show();
                    System.out.println("cur :" + arrListData.get(position).getCategoryId());
                    System.out.println("cur :" + arrListData.get(position).getContentId());

                    Intent intent = new Intent(getApplicationContext(), DetailInfoActivity.class);
                    intent.putExtra("content_id", arrListData.get(position).getContentId());
                    intent.putExtra("category_id", arrListData.get(position).getCategoryId());
                    intent.putExtra("xCoordinate", String.valueOf(userPreferencePlayObjects.get(position).getMapy()));
                    intent.putExtra("yCoordinate", String.valueOf(userPreferencePlayObjects.get(position).getMapx()));

                    //????????? ???????????? ?????? ????????? ???????????? ???????????? ?????? ?????????.
                    // ??????????????? ???????????? ?????? ????????? ?????? ????????? ?????? ????????? ????????? ????????? ????????? ????????? ?????????.
                    Bitmap intentBitmap = arrListData.get(position).getBitmap();

                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    float scale = (float) (256 / (float) intentBitmap.getWidth());
                    int image_w = (int) (intentBitmap.getWidth() * scale);
                    int image_h = (int) (intentBitmap.getHeight() * scale);

                    Bitmap resize = Bitmap.createScaledBitmap(intentBitmap, image_w, image_h, true);
                    resize.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    byte[] byteArray = stream.toByteArray();
                    intent.putExtra("image", byteArray);

                    startActivity(intent);
                }
            });
        }
    }


    private ArrayList<PlayObject> getUserPreferencePlayObjects(){
        try {
            originalPlayObjects = new PlayTask(currentLatitude, currentLongitude).execute().get();

            //???????????? ?????? ??? ????????? ??????(**???) ????????????
            //??????????????? **??? ...
            cityName = originalPlayObjects.get(0).getAddr1().split(" ")[1];
            System.out.println("cITYNAME : " + cityName);

            //?????? ????????? ??????????????? ??????, ????????? ?????????????
            if (originalPlayObjects != null && cityName != null) {
                weather_type = new AirWeatherTask(currentLatitude, currentLongitude, cityName).execute().get();
                if (weather_type != null) setWeatherIcon(weather_type); // ?????? ????????? ??????

                //?????? ?????????
                userPreferencePlayObjects = new RecommendTask(weather_type, originalPlayObjects).execute().get();
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return userPreferencePlayObjects;
    }

    private void setWeatherIcon(String weather_type) {

        System.out.println("WEATHERTYPE : " + weather_type);
        //weather_type??? ?????? ?????? ??????
        if (weather_type != null) {
            //?????? ????????? ???????????? ??????
            weatherTypeInt = setDrawableIcon(weather_type);
            Drawable drawable = getResources().getDrawable(weatherTypeInt);
            imageView_weatherIcon.setImageDrawable(drawable);
        }
    }


    private void printListData(ArrayList<PlayObject> userPreferencePlayObjects) {
        int size = userPreferencePlayObjects.size();

        arrListData = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            ListData listData = new ListData();
            try {
                if (userPreferencePlayObjects.get(i).getFirstimage2() == null) {
                    bitmap = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.no_image);
                } else {
                    bitmap = new LoadImage().execute(userPreferencePlayObjects.get(i).getFirstimage2()).get();
                }
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (userPreferencePlayObjects.size() == 0) {
                listData.setTitle("?????? ???????????? ????????????.");
            } else {
                listData.setTitle(userPreferencePlayObjects.get(i).getTitle());
                listData.setSummary(userPreferencePlayObjects.get(i).getCategoryName());
                listData.setCategoryId(userPreferencePlayObjects.get(i).getPreferCategoryId());
                listData.setBitmap(bitmap);
                listData.setContentId(userPreferencePlayObjects.get(i).getContentid());

                if (userPreferencePlayObjects.get(i).getAddr2() == null)
                    listData.setAddress(userPreferencePlayObjects.get(i).getAddr1());
                else
                    listData.setAddress(userPreferencePlayObjects.get(i).getAddr1() + " " + userPreferencePlayObjects.get(i).getAddr2());
                //?????? ?????? ???????????? ????????????
            }
            arrListData.add(listData);
        }

        //????????? ?????? ?????????
        printList = (ListView) findViewById(R.id.listView);
        ListAdapter listAdapter = new ListAdapter(arrListData);
        printList.setAdapter(listAdapter);

        System.out.println("flag_userPreferenceObjects : " );

    }


    public void setCurrentLocation(Location location) {

        System.out.println("///////////////////////////////////////////////1");
        Log.d(TAG ,"setCurrentLocation");

        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(currentLatLng);
        mMap.moveCamera(cameraUpdate);

    }



    ////////////////////////////////////????????? ?????? 4
    public void setPlayLocationMarker(PlayObject userPreferencePlayObjects){


        System.out.println("///////////////////////////////////////////////2");
        Log.d(TAG ,"setPlayLocationMarker");

        LatLng playMarkerLatLng = new LatLng(userPreferencePlayObjects.getMapy(), userPreferencePlayObjects.getMapx());

        System.out.println("GETMAPX : " + userPreferencePlayObjects.getMapx());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(playMarkerLatLng);
        markerOptions.title(userPreferencePlayObjects.getTitle());
        markerOptions.snippet(String.valueOf(userPreferencePlayObjects.getDist()) + "m");
        markerOptions.draggable(true);

        playMarker = mMap.addMarker(markerOptions);
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));

    }

    public boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    //??????????????? ????????? ????????? ????????? ?????? ????????????
    private boolean checkPermission() {

        int hasFineLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION);


        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED) {
            return true;
        }

        return false;

    }


    /*
     * ActivityCompat.requestPermissions??? ????????? ????????? ????????? ????????? ???????????? ??????????????????.
     */
    @Override
    public void onRequestPermissionsResult(int permsRequestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grandResults) {

        if (permsRequestCode == PERMISSIONS_REQUEST_CODE && grandResults.length == REQUIRED_PERMISSIONS.length) {

            // ?????? ????????? PERMISSIONS_REQUEST_CODE ??????, ????????? ????????? ???????????? ??????????????????

            boolean check_result = true;


            // ?????? ???????????? ??????????????? ???????????????.

            for (int result : grandResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check_result = false;
                    break;
                }
            }


            if (check_result) {

                // ???????????? ??????????????? ?????? ??????????????? ???????????????.
                startLocationUpdates();
            } else {
                // ????????? ???????????? ????????? ?????? ????????? ??? ?????? ????????? ??????????????? ?????? ???????????????.2 ?????? ????????? ????????????.

                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])
                        || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[1])) {


                    // ???????????? ????????? ????????? ???????????? ?????? ?????? ???????????? ????????? ???????????? ?????? ????????? ??? ????????????.
                    Snackbar.make(mLayout, "???????????? ?????????????????????. ?????? ?????? ???????????? ???????????? ??????????????????. ",
                            Snackbar.LENGTH_INDEFINITE).setAction("??????", new View.OnClickListener() {

                        @Override
                        public void onClick(View view) {

                            finish();
                        }
                    }).show();

                } else {


                    // "?????? ?????? ??????"??? ???????????? ???????????? ????????? ????????? ???????????? ??????(??? ??????)?????? ???????????? ???????????? ?????? ????????? ??? ????????????.
                    Snackbar.make(mLayout, "???????????? ?????????????????????. ??????(??? ??????)?????? ???????????? ???????????? ?????????. ",
                            Snackbar.LENGTH_INDEFINITE).setAction("??????", new View.OnClickListener() {

                        @Override
                        public void onClick(View view) {

                            finish();
                        }
                    }).show();
                }
            }

        }
    }


    //??????????????? GPS ???????????? ?????? ????????????
    private void showDialogForLocationServiceSetting() {

        AlertDialog.Builder builder = new AlertDialog.Builder(CurrentLocationActivity.this);
        builder.setTitle("?????? ????????? ????????????");
        builder.setMessage("?????? ???????????? ???????????? ?????? ???????????? ???????????????.\n"
                + "?????? ????????? ???????????????????");
        builder.setCancelable(true);
        builder.setPositiveButton("??????", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent
                        = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("??????", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {

            case GPS_ENABLE_REQUEST_CODE:

                //???????????? GPS ?????? ???????????? ??????
                if (checkLocationServicesStatus()) {
                    if (checkLocationServicesStatus()) {

                        Log.d(TAG, "onActivityResult : GPS ????????? ?????????");

                        needRequest = true;

                        return;
                    }
                }

                break;
        }
    }

    public int setDrawableIcon(String weather_type) {
        int weatherInt = 0;
        if(weather_type != null) {
            switch (weather_type) {
                case "type_0":
                    weatherInt = R.drawable.medical_mask;
                    break;
                case "type_1":
                    weatherInt = R.drawable.snowflake;
                case "type_2":
                case "type_3":
                case "type_4":
                    weatherInt = R.drawable.rain;
                    //return R.drawable.rain;
                    break;
                case "type_5":
                case "type_6":
                case "type_7":
                case "type_8":
                case "type_9":
                case "type_10":
                    weatherInt = R.drawable.cloud;
                    break;
                case "type_11":
                case "type_12":
                case "type_13":
                case "type_14":
                case "type_15":
                case "type_16":
                    weatherInt = R.drawable.sun;
                    break;
            }
            System.out.println("MAIN WEATHERINT : " + weatherInt);

        }
        return weatherInt;
    }
}