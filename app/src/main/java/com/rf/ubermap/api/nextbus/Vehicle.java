package com.rf.ubermap.api.nextbus;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

@Root
public class Vehicle implements ClusterItem {

    @Attribute
    public long id;

    @Attribute
    public String routeTag;

    @Attribute(required = false)
    public String dirTag;

    @Attribute
    public float lat;

    @Attribute
    public float lon;

    @Attribute
    public float secsSinceReport;

    @Attribute
    public boolean predictable;

    @Attribute
    public float heading;

    @Attribute
    public float speedKmHr;

    @Attribute(required = false)
    public long leadingVehicleId;

    @Override
    public LatLng getPosition() {
        return new LatLng(lat,lon);
    }

    @Override
    public String toString() {
        return id+" ["+lat+","+lon+"]";
    }
}
