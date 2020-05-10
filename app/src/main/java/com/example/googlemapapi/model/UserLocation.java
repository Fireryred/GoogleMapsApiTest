package com.example.googlemapapi.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;

public class UserLocation {
    private GeoPoint geoPoint;
    private Timestamp timestamp;
    private User user;

    public UserLocation(GeoPoint geoPoint, Timestamp timestamp, User user) {
        this.geoPoint = geoPoint;
        this.timestamp = timestamp;
        this.user = user;
    }

    public UserLocation() {
    }

    public GeoPoint getGeoPoint() {
        return geoPoint;
    }

    public void setGeoPoint(GeoPoint geoPoint) {
        this.geoPoint = geoPoint;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
