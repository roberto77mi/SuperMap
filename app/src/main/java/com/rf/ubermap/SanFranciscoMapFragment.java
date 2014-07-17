package com.rf.ubermap;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Property;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.VisibleRegion;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.ui.IconGenerator;
import com.rf.ubermap.api.nextbus.NextBusApi;
import com.rf.ubermap.api.nextbus.Vehicle;
import com.rf.ubermap.api.nextbus.VehiclesResponse;
import com.rf.ubermap.cluster.VehicleClusterRenderer;
import com.rf.ubermap.util.LatLngInterpolator;
import com.rf.ubermap.util.LocationUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.converter.SimpleXmlConverter;

/**
 * - DONE start the map on SF view
 *
 * - DONE show all the muni vehicles
 * - update the vehicles every X seconds (10?)
 *
 * FROM https://github.com/googlemaps/android-maps-utils
 * - IconGenerator: use bubble icon factory to show vehicles labels
 * - Marker clustering  https://developers.google.com/maps/documentation/android/utility/marker-clustering?hl=pt-PT
 * - Heat maps
 *
 * - show Neighborhoods / cluster by Neighborhood when zoomed out.
 *   http://www.zillow.com/howto/api/neighborhood-boundaries.htm  |  http://en.wikipedia.org/wiki/Shapefile
 *
 * - try to animate
 *   TO IMPROVE
 *   - do not pause between calls (I should just set the anim time longer than the interval between calls)
 *   - start animations in a specific area as you move the camera close enough (currently waits to the next refresh)
 *   - predict a better future position: currently linear with speed BUT, fast vehicles will slow down and slow vehicles will go faster
 *
 * - calculate some fun stats about density, direction, neighborhood, distances, etc
 *
 * - use the Geocoding Api to the the neighbourhood of each vehicle
 *
 * - create fun stuff about density and average direction for SF and each neighbourhood
 *
 * - record my position, tell a story; activity recognition api, mark how you are moving
 *
 *
 *
 * - APIs (Google) Places API, Directions API
 *
 *
 * -------->
 *
 */
public class SanFranciscoMapFragment extends Fragment implements Handler.Callback {

    NextBusApi nextBusApi;
    MapView mMapView;
    private GoogleMap mMap;
    ClusterManager<Vehicle> mClusterManager;
    private HashMap<Long, Marker> mVehiclesMap;
    private HashMap<Marker, Vehicle> mMarkerVehicleMap;
    private List<Vehicle> mVehicles;
    SharedPreferences.OnSharedPreferenceChangeListener mPrefListener;

    private MenuItem mRefreshMenu;

    final static LatLng SAN_FRANCISCO = new LatLng(37.7545458, -122.4408335);
    final static CameraPosition mCameraSanFrancisco = CameraPosition.fromLatLngZoom(SAN_FRANCISCO, 9f);
    final static float SAN_FRANCISCO_DEFAULT_ZOOM = 11.5f;
    final static float SAN_FRANCISCO_ZOOM_ANIMATION = 13.5f;
    final static float SF_CLUSTER_ZOOM = 12;
    final static int WHAT_REFRESH = 1;
    final static LatLngInterpolator LINEAR = new LatLngInterpolator.Linear();

    HashMap<String, LatLng> SF_LANDMARKS = new HashMap<String, LatLng>();
    HashSet<Marker> mLandmarksMarkers = new HashSet<Marker>();
    HashMap<Marker, ObjectAnimator> mAnimations = new HashMap<Marker, ObjectAnimator>();

    private boolean mLoading = false;
    private boolean mShowClusters = false;

    TextView mHeaderText;

    Handler mMainThreadHandler;
    long mRefreshInterval = REFRESH_SLOW; // millis
    static final long REFRESH_FAST = 10000;
    static final long REFRESH_SLOW = 30000;


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

        mPrefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {

            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                Log.d("UberMap","Pref Changed ["+key+"]");
                if ("pref_sf_landmarks".equals(key)) {
                    showLandmarks(sharedPreferences.getBoolean(key,false));
                } else if ("pref_sf_cluster".equals(key)){
                    //showVehiclesOrClusters();
                    setUpMap();
                }
            }

        };

        PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(mPrefListener);

        nextBusApi = restAdapter.create(NextBusApi.class);

        mVehiclesMap = new HashMap<Long, Marker>();
        mMarkerVehicleMap = new HashMap<Marker, Vehicle>();

        SF_LANDMARKS.put("Civic Center", new LatLng(37.7778533, -122.4178577));
        SF_LANDMARKS.put("Pier 39", new LatLng(37.808673, -122.409821));
        SF_LANDMARKS.put("Twin Peaks", new LatLng(37.7532511, -122.4512832));
        SF_LANDMARKS.put("Ferry Building", new LatLng(37.795274, -122.393155));


        mMainThreadHandler = new Handler(getActivity().getMainLooper(), this);
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case WHAT_REFRESH:
                loadVehicles();
            break;
        }

        //msg.recycle();
        return true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_map,
                container, false);
        mMapView = (MapView) rootView.findViewById(R.id.mapView);
        mHeaderText = (TextView) rootView.findViewById(R.id.map_location_text);

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
        cancelCurrentAnimations();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mMainThreadHandler!=null) mMainThreadHandler.removeMessages(WHAT_REFRESH);
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

        /*
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                startAnimate(marker);
                return false;
            }
        });
        */

        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                Log.d("UberMap","Camera Changed = "+cameraPosition);

                // if the zoom threashold has passed, start animating what is now visible if it's not moving yet
                if (mMap.getCameraPosition().zoom > SAN_FRANCISCO_ZOOM_ANIMATION) {
                    possiblyStartAnimate();
                }


            }
        });

        if (mClusterManager==null) {
            mClusterManager = new ClusterManager<Vehicle>(getActivity(), mMap);
            mClusterManager.setRenderer(new VehicleClusterRenderer(getActivity(), mMap, mClusterManager));
        }

        if (PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("pref_sf_landmarks",false)) {
            showLandmarks(true);
        }

        loadVehicles();

    }

    private synchronized void possiblyStartAnimate() {
        final VisibleRegion region = mMap.getProjection().getVisibleRegion(); // UI Thread

        for (Marker marker : mMarkerVehicleMap.keySet()){

            if (!mAnimations.containsKey(marker) && region.latLngBounds.contains(marker.getPosition())){
                Log.i("UberMap","Start animate ["+marker.getTitle()+"]. New in this region.");

                startAnimate(marker); // it should catch up in position...

            }

        }
    }

        private void possiblyAnimateAll(final long inMillis) {
        if (mMap.getCameraPosition().zoom > SAN_FRANCISCO_ZOOM_ANIMATION) {

            setRefreshInterval(REFRESH_SLOW);

            mMainThreadHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    final VisibleRegion region = mMap.getProjection().getVisibleRegion(); // UI Thread

                    int count = 0;
                    for (Marker marker : mMarkerVehicleMap.keySet()){

                        if (region.latLngBounds.contains(marker.getPosition())){
                            Log.i("UberMap","["+marker.getTitle()+"] is in this region.");
                            Animator currentAnim = mAnimations.get(marker);
                            if (currentAnim!=null) currentAnim.cancel();

                            startAnimate(marker);
                            count++;
                        }

                    }
                    Log.i("UberMap","Total vehicles here : "+count );
                }
            }, inMillis);

        } else {
            // far away, let the animation finish and reload stuff in 60 seconds
            setRefreshInterval(REFRESH_SLOW);

        }
    }

    private void cancelCurrentAnimations() {
        for (ObjectAnimator anim : new ArrayList<ObjectAnimator>(mAnimations.values())) {
            // prevents ConcurrentModificationException by iterating on a copy
            anim.cancel();
        }
        mAnimations.clear();
    }

    private void setRefreshInterval(long intervalMillis) {
        if (intervalMillis!=mRefreshInterval) {
            mRefreshInterval = intervalMillis;

            // then, reschedule
            mMainThreadHandler.removeMessages(WHAT_REFRESH);
            mMainThreadHandler.sendEmptyMessageDelayed(WHAT_REFRESH, mRefreshInterval);
        }

    }

    private void showVehiclesOrClusters(){

        boolean useClustersConfig = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("pref_sf_cluster",false);

        if (useClustersConfig) {
            mMap.setOnCameraChangeListener(mClusterManager);
            mMap.setOnMarkerClickListener(mClusterManager);

            if (mVehicles != null && mMap != null) showClusters(mVehicles);
        } else {
            showVehicles(mVehicles);
        }
/*
        if (mVehicles!=null && mMap!=null) {
            if (mMap.getCameraPosition().zoom < SF_CLUSTER_ZOOM) {
                showClusters(mVehicles); // called all the times so they can be recomputed
            } else {
                if (mShowClusters) showVehicles(mVehicles);
            }
        }
*/
    }



    private void showLandmarks(boolean visible) {

        if (mLandmarksMarkers.isEmpty()) {
            createLandmarksMarkers();
        }

        for (Marker m:mLandmarksMarkers) {
            m.setVisible(visible);
        }

    }

    private void createLandmarksMarkers() {
        IconGenerator tc = new IconGenerator(getActivity());

        for (String name : SF_LANDMARKS.keySet()) {
            Bitmap bmp = tc.makeIcon(name);
            mLandmarksMarkers.add(
                    mMap.addMarker(new MarkerOptions().position(SF_LANDMARKS.get(name)).icon(BitmapDescriptorFactory.fromBitmap(bmp)))
            );
        }
    }


    private synchronized void loadVehicles() {
        if (mLoading) return;

        mLoading = true;
        mHeaderText.setText(R.string.sf_loading);

        if (mRefreshMenu!=null) mRefreshMenu.setEnabled(false);

        nextBusApi.getVehiclesPositions("sf-muni", new Callback<VehiclesResponse>() {
            @Override
            public void success(VehiclesResponse vehiclesResponse, Response response) {
                if (!isAdded()) return;

                Log.d("UberMap", vehiclesResponse.getList().size() + " vehicles: " + vehiclesResponse);
                mVehicles = vehiclesResponse.getList();

                mHeaderText.setText(mVehicles.size()+" "+getString(R.string.sf_vehicles));

                showVehiclesOrClusters();

                mLoading = false;

                /*
                mMainThreadHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        loadVehicles();
                    }
                }, mRefreshInterval);
                */
                mMainThreadHandler.sendEmptyMessageDelayed(WHAT_REFRESH, mRefreshInterval);

            }

            @Override
            public void failure(RetrofitError retrofitError) {
                Log.d("UberMap", "Error Bus " + retrofitError);
                if (!isAdded()) return;

                mHeaderText.setText(R.string.sf_api_fail);

                mLoading = false;
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




    private void showVehicles(Collection<Vehicle> vehicles){

        mShowClusters = false;
        hideClusters();

        // cancelCurrentAnimations();

        for (Vehicle vehicle : vehicles) {
            if (vehicle==null || vehicle.lat==0 || vehicle.routeTag==null) {
                Log.w("UberMap","skipping vehicle");
                continue;
            }

            // if (vehicle.speedKmHr<30) continue;// for testing, limit to fast vehicles

            boolean alphabetic = !Character.isDigit(vehicle.routeTag.charAt(0));
            boolean stopped = vehicle.speedKmHr == 0;

            Marker vehicleMarker = mVehiclesMap.get(vehicle.id);

            if (vehicleMarker==null) {

                // create new Marker
                vehicleMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(vehicle.lat, vehicle.lon))
                                .draggable(false).title(vehicle.routeTag)
                                .flat(true)
                                .rotation(vehicle.heading - 90)
                                .snippet(vehicle.routeTag + ", " + vehicle.speedKmHr + "km/h")
                                .alpha(vehicle.secsSinceReport > 120 ? 0.5f : vehicle.secsSinceReport > 60 ? 0.8f : 1)
                                        //.icon(BitmapDescriptorFactory.fromBitmap(bmp))

                                .icon(
                                        // stopped ? (
                                        //         alphabetic ? BitmapDescriptorFactory.fromResource(R.drawable.orange_dot) :
                                        //                 BitmapDescriptorFactory.fromResource(R.drawable.blue_dot)
                                        // ) :
                                        (
                                                alphabetic ? BitmapDescriptorFactory.fromResource(R.drawable.orange_tringle) :
                                                        BitmapDescriptorFactory.fromResource(R.drawable.blue_triangle)
                                        )
                                )
                );

                mVehiclesMap.put(vehicle.id, vehicleMarker);
                mMarkerVehicleMap.put(vehicleMarker, vehicle);

                Log.d("UberMap","Creating new Vehicle '"+vehicle.routeTag+"' speed: "+vehicle.speedKmHr+" km/h");
            } else {
                vehicleMarker.setVisible(true);

                vehicleMarker.setAlpha(vehicle.secsSinceReport > 120 ? 0.5f : vehicle.secsSinceReport > mRefreshInterval ? 0.8f : 1);

                if (vehicle.secsSinceReport<mRefreshInterval/1000) {
                    Log.d("UberMap","Moving Vehicle '"+vehicle.routeTag+"'. Last Reported Position "+vehicle.secsSinceReport+" seconds ago.");

                    ObjectAnimator oAnin = mAnimations.remove(vehicleMarker);
                    if (oAnin != null) {
                        oAnin.cancel();
                    }

                    // move vehicles
                  //  vehicleMarker.setPosition(new LatLng(vehicle.lat, vehicle.lon));
                    vehicleMarker.setRotation(vehicle.heading - 90); // TODO animate this
                    vehicleMarker.setSnippet(vehicle.routeTag + ", " + vehicle.speedKmHr + "km/h");

                    animateMarkerToICS(vehicleMarker, new LatLng(vehicle.lat, vehicle.lon), 500, true);
                } else {

                    // the vehicle is moving by the animation, so I don't want to put it back where it was
                    Log.d("UberMap","No update for Vehicle '"+vehicle.routeTag+"'. Old ["+vehicleMarker.getPosition()+"] new ["+vehicle.lat+","+vehicle.lon+"] Last Reported Position "+vehicle.secsSinceReport+" seconds ago.");


                }

                mMarkerVehicleMap.put(vehicleMarker, vehicle); // update data, like speed


            }

            //startAnimate(vehicleMarker);
        }

        possiblyAnimateAll(500); // waits 500 for the animation to correct the position to finish

        if (mRefreshMenu!=null) mRefreshMenu.setEnabled(true);
    }

    private boolean mClusterHasItems = false;
    private void hideClusters(){
        mClusterManager.clearItems();
        mClusterManager.cluster();

        mClusterHasItems = false;
    }

    private void showClusters(Collection<Vehicle> vehicles) {
        Log.i("UberMap", "Showing clusters");

        if (!mShowClusters) {
            for (Marker m : mMarkerVehicleMap.keySet()) m.setVisible(false);
        }

        mShowClusters = true;

        //mMap.setOnMarkerClickListener(mClusterManager);

        if (!mClusterHasItems) {
            for (Vehicle v : vehicles) {
                mClusterManager.addItem(v);
            }
            mClusterHasItems = true;
        }

        mClusterManager.cluster(); // runs the AsyncTask
    }


    // needs to be called by the UI Thread
    private void startAnimate(final Marker marker) {
       Vehicle vehicle = mMarkerVehicleMap.get(marker);
       if (vehicle!=null && vehicle.speedKmHr>0) {

           final LatLng startPosition = marker.getPosition();

           float predictedAvgSpeed = vehicle.speedKmHr - ((vehicle.speedKmHr-15) / 4);

           final LatLng finalPosition = LocationUtil.move(
                   startPosition,
                   vehicle.heading, predictedAvgSpeed*10/36,
                   mRefreshInterval-1000); // move of 1 less second, be conservative slow

           animateMarkerToICS(marker, finalPosition, mRefreshInterval+1000, false); // animate for an extra second

       } else {
           Log.w("UberMap", "Dunno this vehicle, dude. Marker Title: "+marker.getTitle());
       }

    }


    // needs to be called by the UI Thread
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    void animateMarkerToICS(final Marker marker, final LatLng finalPosition, final long time, final boolean smooth) {

       // ObjectAnimator exists = mAnimations.get(marker);
       // exists.cancel();

        TypeEvaluator<LatLng> typeEvaluator = new TypeEvaluator<LatLng>() {
            @Override
            public LatLng evaluate(float fraction, LatLng startValue, LatLng endValue) {
                return LINEAR.interpolate(fraction, startValue, endValue);
            }
        };
        Property<Marker, LatLng> property = Property.of(Marker.class, LatLng.class, "position");
        final ObjectAnimator animator = ObjectAnimator.ofObject(marker, property, typeEvaluator, finalPosition);

        animator.setInterpolator(smooth? new AccelerateDecelerateInterpolator() : new LinearInterpolator());

        animator.setDuration(time);
        // animator.setStartDelay(500);

        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mAnimations.remove(marker);
            }

            @Override
            public void onAnimationCancel(Animator animation) {}

            @Override
            public void onAnimationRepeat(Animator animation) {}
        });
        mAnimations.put(marker, animator);

        animator.start();

        Log.d("UberMap","Start on "+marker.getTitle());
    }

    // final static long ANIMATION_MILLIS = 20000;
    final static long ANIMATION_SPEED_MULTIPLIER = 1;// 20; // for testing, otherwise use 1
}