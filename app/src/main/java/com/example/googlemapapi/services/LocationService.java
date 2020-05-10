package com.example.googlemapapi.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.example.googlemapapi.model.User;
import com.example.googlemapapi.model.UserLocation;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

public class LocationService extends Service {
    private static final String TAG = "LocationService";
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private static final long UPDATE_INTERVAL = 4000;
    private static final long FASTEST_INTERVAL = 2000;
    private User user;
    private UserLocation userLocation;
    private FirebaseAuth fAuth;
    private FirebaseFirestore fStore;
    private Timestamp timestamp;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        user = new User();
        userLocation = new UserLocation();
        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        timestamp = Timestamp.now();
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        if (Build.VERSION.SDK_INT >= 26){
            String CHANNEL_ID = "my_channel_06";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "My_Channel", NotificationManager.IMPORTANCE_DEFAULT);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID).setContentTitle("").setContentText("").build();
            startForeground(1, notification);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: called.");
        getLocation();
        return START_NOT_STICKY;
    }

    private void getLocation() {
        LocationRequest mLocationRequestHighAccuracy = new LocationRequest();
        mLocationRequestHighAccuracy.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequestHighAccuracy.setInterval(UPDATE_INTERVAL);
        mLocationRequestHighAccuracy.setFastestInterval(FASTEST_INTERVAL);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            stopSelf();
            return;
        }
        mFusedLocationProviderClient.requestLocationUpdates(mLocationRequestHighAccuracy, new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null){
                    GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                    userLocation.setGeoPoint(geoPoint);
                    userLocation.setTimestamp(timestamp);
                    getUserDetails();
                    userLocation.setUser(user);
                    saveUserLocation(userLocation);
                }
            }
        }, Looper.myLooper());
    }

    private void saveUserLocation(UserLocation userLocation) {
        DocumentReference locRef = fStore.collection("User Locations").document(fAuth.getCurrentUser().getUid());
        locRef.set(userLocation).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "onComplete: inserted user location to db");
                }
            }
        });
    }

    private void getUserDetails() {
        try {
            DocumentReference userRef = fStore.collection("users").document(fAuth.getCurrentUser().getUid());
            userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()){
                        user = task.getResult().toObject(User.class);
                    }
                }
            });
        } catch (NullPointerException npe){
            stopSelf();
        }
    }
}
