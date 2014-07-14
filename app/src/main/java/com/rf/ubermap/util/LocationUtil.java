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

    public static LatLng move(LatLng startPosition, float bearing, float speedMetersSeconds, float timeMillis) {

        double distanceMeters = speedMetersSeconds + timeMillis/1000d;

        return _move(startPosition.latitude,startPosition.longitude,bearing,distanceMeters);

        /*
        double meterStep = 0.0000898d; //0.00000898d;

        double distanceLat = meterStep * distanceMeters * Math.cos(Math.toRadians(bearing));

        double distanceLon = meterStep * distanceMeters * Math.sin(Math.toRadians(bearing));

        LatLng finalPosition = new LatLng(startPosition.latitude + distanceLat, startPosition.longitude + distanceLon);

        return finalPosition;
        */
    }

    /**
     * This method extrapolates the endpoint of a movement with a given length from a given starting point using a given
     * course.
     *
     * from: http://stackoverflow.com/questions/5857523/calculate-latitude-and-longitude-having-meters-distance-from-another-latitude-lo
     *
     * @param startPointLat the latitude of the starting point in degrees, must not be {@link Double#NaN}.
     * @param startPointLon the longitude of the starting point in degrees, must not be {@link Double#NaN}.
     * @param course        the course to be used for extrapolation in degrees, must not be {@link Double#NaN}.
     * @param distance      the distance to be extrapolated in meters, must not be {@link Double#NaN}.
     *
     * @return the extrapolated point.
     */
    public static LatLng _move(final double startPointLat, final double startPointLon, final double course,
                                    final double distance) {
        //
        //lat =asin(sin(lat1)*cos(d)+cos(lat1)*sin(d)*cos(tc))
        //dlon=atan2(sin(tc)*sin(d)*cos(lat1),cos(d)-sin(lat1)*sin(lat))
        //lon=mod( lon1+dlon +pi,2*pi )-pi
        //
        // where:
        // lat1,lon1  -start pointi n radians
        // d          - distance in radians Deg2Rad(nm/60)
        // tc         - course in radians

        final double crs = Math.toRadians(course);
        final double d12 = Math.toRadians(distance / MINUTES_TO_METERS / DEGREE_TO_MINUTES);

        final double lat1 = Math.toRadians(startPointLat);
        final double lon1 = Math.toRadians(startPointLon);

        final double lat = Math.asin(Math.sin(lat1) * Math.cos(d12)
                + Math.cos(lat1) * Math.sin(d12) * Math.cos(crs));
        final double dlon = Math.atan2(Math.sin(crs) * Math.sin(d12) * Math.cos(lat1),
                Math.cos(d12) - Math.sin(lat1) * Math.sin(lat));
        final double lon = (lon1 + dlon + Math.PI) % (2 * Math.PI) - Math.PI;

        return new LatLng(Math.toDegrees(lat), Math.toDegrees(lon));
    }
    private static final double MINUTES_TO_METERS = 1852d;
    private static final double DEGREE_TO_MINUTES = 60d;

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
        float lat = prefs.getFloat(key + ".lat", 0f);
        float lon = prefs.getFloat(key+".lon", 0f);
        float zoom = prefs.getFloat(key+".zoom", 0f);

        return CameraPosition.fromLatLngZoom(new LatLng(lat,lon), zoom);

    }

}
