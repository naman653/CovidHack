package com.hackathon.covid.data;

import androidx.annotation.Nullable;
import androidx.room.Ignore;

import java.util.Objects;

public class Point {
    double latitude;
    double longitude;

    public Point() {
    }

    @Ignore
    public Point(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

//    public String getHash() {
//        return GeoHash.encodeHash(latitude, longitude);
//    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof Point) {
            Point p2 = (Point) obj;
            return Double.compare(latitude, p2.latitude) == 0 && Double.compare(longitude, p2.longitude) == 0;
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return Objects.hash(latitude, longitude);
    }
}
