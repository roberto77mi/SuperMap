package com.rf.ubermap;

import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Property;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.rf.ubermap.api.nextbus.NextBusApi;
import com.rf.ubermap.api.nextbus.Vehicle;
import com.rf.ubermap.api.nextbus.VehiclesResponse;
import com.rf.ubermap.util.LatLngInterpolator;
import com.rf.ubermap.util.LocationUtil;

import java.util.HashMap;
import java.util.List;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.converter.SimpleXmlConverter;

/**
 * - start the map on SF view
 *
 * - show all the muni vehicles
 * - update the vehicles every X seconds (10?)
 *
 * - try to animate
 * - calculate some fun stats about
 *
 * - use the Geocoding Api to the the neighbourhood of each vehicle
 * - create fun stuff about density and average direction for SF and each neighbourhood
 *
 * - record my position, tell a story; activity recognition api, mark how you are moving.
 *
 * See: https://github.com/googlemaps/hellomap-android
 *
 *
 * - APIs (Google) Places API, Directions API
 *
 *
 * --------> use bubble icon factory to show vehicles labels
 *
 */
public class SanFranciscoMapFragment extends Fragment {

    NextBusApi nextBusApi;
    MapView mMapView;
    private GoogleMap mMap;
    private HashMap<Long, Marker> mVehiclesMap;
    private HashMap<Marker, Vehicle> mMarkerVehicleMap;

    private MenuItem mRefreshMenu;

    final static LatLng SAN_FRANCISCO = new LatLng(37.7545458, -122.4408335);
    final static CameraPosition mCameraSanFrancisco = CameraPosition.fromLatLngZoom(SAN_FRANCISCO, 9f);
    final static float SAN_FRANCISCO_DEFAULT_ZOOM = 11.5f;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        // move this code in a SanFranciscoMapFragment
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(NextBusApi.ENDPOINT)
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setConverter(new SimpleXmlConverter())
                .build();


        nextBusApi = restAdapter.create(NextBusApi.class);

        mVehiclesMap = new HashMap<Long, Marker>();
        mMarkerVehicleMap = new HashMap<Marker, Vehicle>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_map,
                container, false);
        mMapView = (MapView) rootView.findViewById(R.id.mapView);


        setUpMapIfNeeded();

        // new WelcomeMessageDialogFragment().show(getFragmentManager(),"welcome");

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.sfmap, menu);

        mRefreshMenu = menu.findItem(R.id.action_refresh);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {

            loadVehicles();

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = mMapView.getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
        if (mMap != null) {
            CameraPosition camPos = mMap.getCameraPosition();
            Log.d("UberMap", "pos=" + camPos);

        } else {
            Log.d("UberMap", "NULL GoogleMap");
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mMapView.onCreate(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
        setUpMapIfNeeded();
    }
    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
    }
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    private void setUpMap() {
        MapsInitializer.initialize(getActivity().getApplicationContext());
        mMap.setMyLocationEnabled(false);
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(mCameraSanFrancisco));

        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                mMap.animateCamera(CameraUpdateFactory.zoomTo(SAN_FRANCISCO_DEFAULT_ZOOM));
            }
        });

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                startAnimate(marker);
                return false;
            }
        });

        loadVehicles();
    }



    private void loadVehicles() {
        if (mRefreshMenu!=null) mRefreshMenu.setEnabled(false);

        nextBusApi.getVehiclesPositions("sf-muni", new Callback<VehiclesResponse>() {
            @Override
            public void success(VehiclesResponse vehiclesResponse, Response response) {

                Log.d("UberMap", vehiclesResponse.getList().size() + " vehicles: " + vehiclesResponse);

                showVehicles(vehiclesResponse.getList());

            }

            @Override
            public void failure(RetrofitError retrofitError) {
                Log.d("UberMap", "Error Bus " + retrofitError);
            }
        });

    }


    public static class WelcomeMessageDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.dialog_welcome)
                    .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // FIRE ZE MISSILES!
                        }
                    })
                    .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                        }
                    });
            // Create the AlertDialog object and return it
            return builder.create();
        }
    }




    private void showVehicles(List<Vehicle> vehicles){

        for (Vehicle vehicle : vehicles) {
            if (vehicle==null || vehicle.lat==0 || vehicle.routeTag==null) {
                Log.w("UberMap","skipping vehicle");
                continue;
            }

            // if (vehicle.speedKmHr<30) continue; for testing, limit to fast vehicles

            boolean alphabetic = !Character.isDigit(vehicle.routeTag.charAt(0));
            boolean stopped = vehicle.speedKmHr == 0;

            Marker vehicleMarker = mVehiclesMap.get(vehicle.id);

            // mMap.getCameraPosition().

            if (vehicleMarker==null) {
                // create new Marker
                vehicleMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(vehicle.lat, vehicle.lon))
                        .draggable(false).title(vehicle.routeTag)
                        .flat(true)
                        .rotation(vehicle.heading - 90)
                        .alpha(vehicle.secsSinceReport > 120 ? 0.5f : vehicle.secsSinceReport > 60 ? 0.8f : 1)
                        .icon(
                                stopped ? (
                                        alphabetic ? BitmapDescriptorFactory.fromResource(R.drawable.orange_dot) :
                                                BitmapDescriptorFactory.fromResource(R.drawable.blue_dot)
                                ) :
                                        (
                                                alphabetic ? BitmapDescriptorFactory.fromResource(R.drawable.orange_tringle) :
                                                        BitmapDescriptorFactory.fromResource(R.drawable.blue_triangle)
                                        )
                        ));

                mVehiclesMap.put(vehicle.id, vehicleMarker);
                mMarkerVehicleMap.put(vehicleMarker, vehicle);

                Log.d("UberMap","Creating new Vehicle '"+vehicle.routeTag+"' speed: "+vehicle.speedKmHr+" km/h");
            } else {

                // move vehicles
                vehicleMarker.setPosition(new LatLng(vehicle.lat, vehicle.lon));
                mMarkerVehicleMap.put(vehicleMarker, vehicle); // update data, like speed

                Log.d("UberMap","Moving Vehicle '"+vehicle.routeTag+"'");
            }
        }

        if (mRefreshMenu!=null) mRefreshMenu.setEnabled(true);
    }


    private void startAnimate(Marker marker) {
       Vehicle vehicle = mMarkerVehicleMap.get(marker);
       if (vehicle!=null) {

           Toast.makeText(getActivity(), "animating "+vehicle.routeTag+" speed:"+vehicle.speedKmHr, Toast.LENGTH_SHORT).show();
           //

           LatLng startPosition = marker.getPosition();

           LatLng finalPosition = LocationUtil.move(startPosition, vehicle.heading, vehicle.speedKmHr*10/36, ANIMATION_MILLIS);

           //startPosition

           animateMarkerToICS(marker, finalPosition, new LatLngInterpolator.Linear());

       } else {
           Log.w("UberMap", "Dunno this vehicle, dude.");
       }

    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    static void animateMarkerToICS(Marker marker, LatLng finalPosition, final LatLngInterpolator latLngInterpolator) {
        TypeEvaluator<LatLng> typeEvaluator = new TypeEvaluator<LatLng>() {
            @Override
            public LatLng evaluate(float fraction, LatLng startValue, LatLng endValue) {
                return latLngInterpolator.interpolate(fraction, startValue, endValue);
            }
        };
        Property<Marker, LatLng> property = Property.of(Marker.class, LatLng.class, "position");
        ObjectAnimator animator = ObjectAnimator.ofObject(marker, property, typeEvaluator, finalPosition);

        animator.setInterpolator(new LinearInterpolator());

        animator.setDuration(ANIMATION_MILLIS);
        animator.start();
    }

    final static long ANIMATION_MILLIS = 10000;

}