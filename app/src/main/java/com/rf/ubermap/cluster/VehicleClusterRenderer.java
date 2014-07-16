package com.rf.ubermap.cluster;

import android.content.Context;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.rf.ubermap.R;
import com.rf.ubermap.api.nextbus.Vehicle;

/**
 *
 */
public class VehicleClusterRenderer extends DefaultClusterRenderer<Vehicle> {

    public VehicleClusterRenderer(Context context, GoogleMap map, ClusterManager<Vehicle> clusterManager) {
        super(context, map, clusterManager);
    }


    @Override
    protected void onBeforeClusterItemRendered(Vehicle vehicle, MarkerOptions markerOptions) {
        boolean alphabetic = !Character.isDigit(vehicle.routeTag.charAt(0));
        boolean stopped = vehicle.speedKmHr == 0;

        markerOptions.position(new LatLng(vehicle.lat, vehicle.lon))
                        .draggable(false).title(vehicle.routeTag)
                        .flat(true)
                        .rotation(vehicle.heading - 90)
                        .alpha(vehicle.secsSinceReport > 120 ? 0.5f : vehicle.secsSinceReport > 60 ? 0.8f : 1)
                                //.icon(BitmapDescriptorFactory.fromBitmap(bmp))

                        .icon(
                                stopped ? (
                                        alphabetic ? BitmapDescriptorFactory.fromResource(R.drawable.orange_dot) :
                                                BitmapDescriptorFactory.fromResource(R.drawable.blue_dot)
                                ) :
                                        (
                                                alphabetic ? BitmapDescriptorFactory.fromResource(R.drawable.orange_tringle) :
                                                        BitmapDescriptorFactory.fromResource(R.drawable.blue_triangle)
                                        )
                        );
    }

    @Override
    protected boolean shouldRenderAsCluster(Cluster<Vehicle> cluster) {
        return cluster.getSize()>=20;
    }
}
