package com.rf.ubermap.model;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

/**
 * Represent a Vehicle moving on a Map.
 * It has a position, direction and speed and can be animated to predict its position
 * An API can update its position but while it's animating it knows when the API it still not
 * up to date.
 */
public class VehicleMarker {

    Marker marker;

    public VehicleMarker(Marker marker) {
       this.marker = marker;
    }

    // this position does not change while the Marker is animated
    public LatLng lastReportedPosition;

    public void setAlpha(int secondsSince){

    }
}
