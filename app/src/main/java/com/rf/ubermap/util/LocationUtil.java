package com.rf.ubermap.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

/**
 * Created by roberto on 7/8/14.
 */
public class LocationUtil {

    public static double distanceInMeters(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371; //kilometers
        double dLat = Math.toRadians(lat2-lat1);
        double dLng = Math.toRadians(lng2-lng1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng/2) * Math.sin(dLng/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double dist = (earthRadius * c);

        return dist;
    }


    public static void saveCameraPosition(String key, CameraPosition cam, Context ctx) {
        if(key==null || cam == null) {
            return;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        SharedPreferences.Editor edit = prefs.edit();

        // TODO fix possible precision loss
        edit.putFloat(key + ".lat", (float) cam.target.latitude);
        edit.putFloat(key + ".lon", (float) cam.target.longitude);
        edit.putFloat(key + ".zoom", cam.zoom);
        // more? tilt and stuff?

        edit.commit();
    }

    public static CameraPosition loadCameraPosition(String key, Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        float lat = prefs.getFloat(key+".lat", 0f);
        float lon = prefs.getFloat(key+".lon", 0f);
        float zoom = prefs.getFloat(key+".zoom", 0f);

        return CameraPosition.fromLatLngZoom(new LatLng(lat,lon), zoom);

    }

}
