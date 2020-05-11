package com.example.googlemapapi;

import android.app.ActivityManager;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.googlemapapi.model.User;
import com.example.googlemapapi.model.UserLocation;
import com.example.googlemapapi.services.LocationService;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.PendingResult;
import com.google.maps.model.DirectionsResult;

public class Map extends AppCompatActivity implements OnMapReadyCallback {
    private static final String MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey";
    private MapView mMapView;
    private static final String TAG = "Map";
    private FirebaseAuth fAuth;
    private Button logoutBtn;
    private FusedLocationProviderClient mFusedLocationClient;
    private FirebaseFirestore fStore;
    private UserLocation riderLocation;
    private User user;
    private UserLocation userLocation;
    private Timestamp timestamp;
    private Handler handler = new Handler();
    private Runnable runnable;
    private static final int LOCATION_UPDATE_INTERVAL = 3000;
    private GeoApiContext geoApiContext = null;
    private GoogleMap googleMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        timestamp = Timestamp.now();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fStore = FirebaseFirestore.getInstance();
        fAuth = FirebaseAuth.getInstance();
        logoutBtn = findViewById(R.id.logoutbtn);
        logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fAuth.signOut();
                startActivity(new Intent(Map.this, Login.class));
            }
        });
        initGoogleMap(savedInstanceState);
        getUserDetails();
    }
    private void startUserLocationsRunnable(final String riderId){
        handler.postDelayed(runnable = new Runnable() {
            @Override
            public void run() {
                retrieveUserLocation(riderId);
                handler.postDelayed(runnable, LOCATION_UPDATE_INTERVAL);
            }
        }, LOCATION_UPDATE_INTERVAL);
    }
    private void stopLocationUpdates(){
        handler.removeCallbacks(runnable);
    }
    private void retrieveUserLocation(String riderId) {
        DocumentReference locRef = fStore.collection("User Locations").document(riderId);
        locRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    riderLocation = task.getResult().toObject(UserLocation.class);
                    LatLng riderLatLng = new LatLng(riderLocation.getGeoPoint().getLatitude(), riderLocation.getGeoPoint().getLongitude());
                    googleMap.clear();
                    Marker riderMarker = googleMap.addMarker(new MarkerOptions().position(riderLatLng).title("Rider"));
                    getUserLocation(riderMarker);
                    Log.d(TAG, "onComplete: " + (googleMap == null));
                    Log.d(TAG, "onComplete: riderLocation" + riderLocation.getGeoPoint().toString());
                }
            }
        });
    }

    private void getUserLocation(Marker riderMarker) {
        DocumentReference locRef = fStore.collection("User Locations").document(fAuth.getCurrentUser().getUid());
        locRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                userLocation = task.getResult().toObject(UserLocation.class);
                LatLng userLatLng = new LatLng(userLocation.getGeoPoint().getLatitude(), userLocation.getGeoPoint().getLongitude());
                Marker userMarker = googleMap.addMarker(new MarkerOptions().position(userLatLng).title("user"));
                calculateDirections(riderMarker, userMarker);
            }
        });
    }

    private void startLocationService(){
        if (!isLocationServiceRunning()){
            Intent serviceIntent = new Intent(this, LocationService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                Log.d(TAG, "startLocationService: Henlo?");
                Map.this.startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        }
    }
    private boolean isLocationServiceRunning(){
        boolean isRunning = false;
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)){
            if ("com.example.googlemapapi.services.LocationService".equals(service.service.getClassName())){
                Log.d(TAG, "isLocationServiceRunning: location service is running");
                isRunning = true;
            }
        }
        Log.d(TAG, "isLocationServiceRunning: henlo");
        return isRunning;
    }
    public void getKnownLastLocation(){
        mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                if(task.isSuccessful()){
                    Location location = task.getResult();
                    GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                    userLocation = new UserLocation(geoPoint, timestamp, user);
                    saveLastLocation();
                    startLocationService();
                }
            }
        });
    }
    public void saveLastLocation(){
        DocumentReference locRef = fStore.collection("User Locations").document(fAuth.getCurrentUser().getUid());
        locRef.set(userLocation).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                }
            }
        });
    }
    public void getUserDetails(){
        DocumentReference userRef = fStore.collection("users").document(fAuth.getCurrentUser().getUid());
        Log.d(TAG, "getUserDetails: " + fAuth.getCurrentUser().getUid());
        userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                user = new User();
                user = task.getResult().toObject(User.class);
                getKnownLastLocation();
            }
        });
    }
//    private void addMarkers() {
//        DocumentReference userLocRef = fStore.collection("User Locations").document(fAuth.getCurrentUser().getUid());
//        userLocRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
//            @Override
//            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
//                if (task.isSuccessful()){
//                    Log.d(TAG, "onComplete: " + (googleMap == null));
//                    userLocation = task.getResult().toObject(UserLocation.class);
//                    Log.d(TAG, "onComplete: " + userLocation.getGeoPoint().toString());
//                    LatLng userLatLng = new LatLng(userLocation.getGeoPoint().getLatitude(), userLocation.getGeoPoint().getLongitude());
//                    Marker userMarker = googleMap.addMarker(new MarkerOptions().position(userLatLng).title("User"));
//                    googleMap.setMyLocationEnabled(true);
//                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15));
//                    getRiderLocation(userMarker);
//                }
//            }
//        });
//    }
//
//    private void getRiderLocation(Marker userMarker) {
//        DocumentReference riderLocRef = fStore.collection("User Locations").document("qW5Q6U11vNdaRqtNxFwxScCoi343");
//        riderLocRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
//            @Override
//            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
//                if (task.isSuccessful()){
//                    riderLocation = task.getResult().toObject(UserLocation.class);
//                    LatLng riderLatLng = new LatLng(riderLocation.getGeoPoint().getLatitude(), riderLocation.getGeoPoint().getLongitude());
//                    Marker riderMarker = googleMap.addMarker(new MarkerOptions().position(riderLatLng).title("Rider"));
//                    calculateDirections(riderMarker, userMarker);
//                }
//            }
//        });
//    }

    private void initGoogleMap(Bundle savedInstanceState){
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY);
        }
        mMapView = (MapView) findViewById(R.id.mapView1);
        mMapView.onCreate(mapViewBundle);

        mMapView.getMapAsync(this);
        if (geoApiContext == null){
            geoApiContext = new GeoApiContext.Builder().apiKey(getString(R.string.map_key)).build();
        }
    }
    private void calculateDirections(Marker riderMarker, Marker userMarker){
        Log.d(TAG, "calculateDirections: Henlo " + (geoApiContext == null));
        com.google.maps.model.LatLng destination = new com.google.maps.model.LatLng(riderMarker.getPosition().latitude, riderMarker.getPosition().longitude);
        DirectionsApiRequest directions = new DirectionsApiRequest(geoApiContext);
        directions.alternatives(true);
        directions.origin(new com.google.maps.model.LatLng(userMarker.getPosition().latitude, userMarker.getPosition().longitude));
        directions.destination(destination).setCallback(new PendingResult.Callback<DirectionsResult>() {
            @Override
            public void onResult(DirectionsResult result) {
                Log.d(TAG, "onResult: routes " + result.routes[0].toString());
                Log.d(TAG, "onResult: duration " + result.routes[0].legs[0].duration);
                Log.d(TAG, "onResult: distance " + result.routes[0].legs[0].distance);
                Log.d(TAG, "onResult: geoCodedWayPoint " + result.geocodedWaypoints[0].toString());
            }

            @Override
            public void onFailure(Throwable e) {
                e.printStackTrace();
                Log.d(TAG, "onFailure: Failed to get Directions");
            }
        });
    }
    private void addPolyLinesToMap(final DirectionsResult result){

    }
    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
        startUserLocationsRunnable("EQ6iJezZdBbUbpuXU3XtIKVwdxD3");

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        googleMap.setMyLocationEnabled(true);
        DocumentReference locRef = fStore.collection("User Locations").document(fAuth.getCurrentUser().getUid());
        locRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                userLocation = task.getResult().toObject(UserLocation.class);
                LatLng location = new LatLng(userLocation.getGeoPoint().getLatitude(), userLocation.getGeoPoint().getLongitude());
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
            }
        });
//        addMarkers();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mMapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mMapView.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
        stopLocationUpdates();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }
}
