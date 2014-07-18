package com.rf.ubermap;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.rf.ubermap.api.OpenMapQuest;
import com.rf.ubermap.util.LocationUtil;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.text.NumberFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

/*
 * Also play with:
 * 
 * -> city name
 * http://open.mapquestapi.com/nominatim/v1/search?q=50.4,30.4&format=json
 * or use Geocoder class that needs a provider, that is avaiable if Google Maps is installed
 * http://karanbalkar.com/2013/11/tutorial-63-implement-reverse-geocoding-in-android/
 *
 *
 * --> weather
 * http://forecast.weather.gov/MapClick.php?lat=40.78158&lon=-73.96648&FcstType=json
 * 
 * --> foursquare api
 *
 * --> Google Places API
 * https://developers.google.com/places/
 *
 * --
 * https://maps.googleapis.com/maps/api/geocode/json?latlng=40.714224,-73.961452
 *
 * -->
 * http://www.fleetmon.com/products/services_data
 *
 * More projects ideas:
 * https://developers.google.com/maps/showcase/#mobile
 *
 * Shazam Explore
 *
 *
 * Muni, SF bus locations, ping every 10 seconds.
 *
 *
 * https://developers.google.com/maps/documentation/android/utility/
 *
 *
 * TODO implement reverse Geocoder in a generic configurable way and with a listener to update the UI.
 * TODO actually, create many useful configurable and reusable location services using a listener interface
 *
 */

public class MapFragment extends Fragment implements 
GooglePlayServicesClient.ConnectionCallbacks,
GooglePlayServicesClient.OnConnectionFailedListener,
LocationListener,
GoogleMap.OnCameraChangeListener {

    private static final String KEY_CAM = "KEY_CAM_POS";

    private MapView mMapView;
    private GoogleMap mMap;// Might be null if Google Play services APK is not available.

	Location mCurrentLocation;

	TextView mLocationText;

	// Define an object that holds accuracy and frequency parameters
	LocationRequest mLocationRequest;


	LocationClient mLocationClient;
    CameraPosition mCameraPosition;

    ImageView mBearingImageView;

    Marker mMarkerBoy;
    Marker mMarkerGirl;
    Marker mMarkerCat;
    Marker mMarkerDog;

    Queue<Marker> mMarkersQueue;
    Queue<MarkerOptions> mMarkerOptionsQueue;


    // api
    RestAdapter mMapQuestAdapter;
    OpenMapQuest mOpenMapQuestApi;

    boolean mGeocoderAvailable = true;
    Geocoder geo;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        //setRetainInstance(true);

        super.onCreate(savedInstanceState);

        mMarkersQueue = new LinkedList<Marker>();
        mMarkerOptionsQueue = new LinkedList<MarkerOptions>();

        mMapQuestAdapter = new RestAdapter.Builder()
                .setEndpoint(OpenMapQuest.ENDPOINT)
                //.setLogLevel(RestAdapter.LogLevel.FULL)
                .build();

        mOpenMapQuestApi = mMapQuestAdapter.create(OpenMapQuest.class);

        geo = new Geocoder(getActivity());
        if (!geo.isPresent()) {
            mGeocoderAvailable = false;
            Log.w("UberMap", "Google Maps Geocoder not available");
        }
    }

    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
        //Log.i("UberMap", "--------------------------------- map CreateView");

		View rootView = inflater.inflate(R.layout.fragment_map,
				container, false);

		mLocationText = (TextView) rootView.findViewById(R.id.map_location_text);
        mBearingImageView = (ImageView) rootView.findViewById(R.id.map_bearing_image);
        mLocationClient = new LocationClient(getActivity(), this, this);

		int resultCode =
				GooglePlayServicesUtil.
				isGooglePlayServicesAvailable(getActivity());
		// If Google Play services is available
		if (ConnectionResult.SUCCESS == resultCode) {
			// Toast.makeText(getActivity(), "ok!", Toast.LENGTH_SHORT).show();
		} else {
            Toast.makeText(getActivity(), "Google Play Services not available", Toast.LENGTH_SHORT).show();
        }


        mBearingImageView.animate().rotation(360).setStartDelay(1000).setDuration(1000).start();


        mMapView = (MapView) rootView.findViewById(R.id.mapView);

        setUpMapIfNeeded();

		return rootView;
	}

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // after onCreateView
        super.onActivityCreated(savedInstanceState);

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(10000);

        mLocationRequest.setPriority(
                LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setFastestInterval(2000);


        mMapView.onCreate(savedInstanceState);

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {


        if (mMap!=null) {
            CameraPosition camPos = mMap.getCameraPosition();
            Log.d("UberMap", "pos=" + camPos);

        } else {
            Log.d("UberMap", "NULL GoogleMap");
        }

        mMapView.onSaveInstanceState(outState);

        super.onSaveInstanceState(outState);
    }


    @Override
	public void onStart() {
		super.onStart();
		mLocationClient.connect();

	}
	@Override
	public void onStop() {
		super.onStop();
		mLocationClient.disconnect();
	}
    
	@Override
	public void onResume() {
		super.onResume();
			//mLocationManager.requestLocationUpdates(10000, 0, intent);
        //setUpMapIfNeeded();

        mMapView.onResume();
        setUpMapIfNeeded();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onDestroy() {

        if (mCameraPosition!=null) {
            LocationUtil.saveCameraPosition(KEY_CAM,mCameraPosition, getActivity());
        }

        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    public void onLocationChanged(Location location) {
		mCurrentLocation = location;
		updateLocationText();
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub

	}
	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub

	}
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		Toast.makeText(getActivity(), "Status Changed "+status, Toast.LENGTH_SHORT).show();
	}
	@Override
	public void onConnected(Bundle arg0) {
		Toast.makeText(getActivity(), "Connected", Toast.LENGTH_SHORT).show();
		
		mCurrentLocation = mLocationClient.getLastLocation();

        //if (mCurrentLocation==null) {
            LatLng MOSCONE = new LatLng(37.78353872135503, -122.40336209535599);

            Location moscone = new Location("");

            moscone.setLatitude(MOSCONE.latitude);
            moscone.setLongitude(MOSCONE.longitude);
            moscone.setAccuracy(1);

            mCurrentLocation = moscone;
        //}

		updateLocationText();

	}
	public void onDisconnected() {
		// Display the connection status
		Toast.makeText(getActivity(), "Disconnected. Please re-connect.",
				Toast.LENGTH_SHORT).show();
	}
	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		Toast.makeText(getActivity(), "Connection Failed.",
				Toast.LENGTH_SHORT).show();
	}
	protected void updateLocationText(){
		if (mCurrentLocation==null) {
			mLocationText.setText(R.string.map_updating);
		} else {

			mLocationText.setText("== "+Double.toString(mCurrentLocation.getLatitude())+","+Double.toString(mCurrentLocation.getLongitude())+" ==");
		}
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
        if (mMap!=null) {
            CameraPosition camPos = mMap.getCameraPosition();
            Log.d("UberMap", "pos=" + camPos);

        } else {
            Log.d("UberMap", "NULL GoogleMap");
        }
    }

    private void setUpMap() {

        Log.i("UberMap"," ------------------------ setUpMap()");

        MapsInitializer.initialize(getActivity().getApplicationContext());

      //  mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
        mMap.setOnCameraChangeListener(this);
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setCompassEnabled(false);
        mMap.getUiSettings().setRotateGesturesEnabled(false);
       //--  mMap.getUiSettings().setTiltGesturesEnabled(false);

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                Toast.makeText(getActivity(),marker.getTitle(),Toast.LENGTH_SHORT).show();
                if (marker.isInfoWindowShown()) {
                    marker.hideInfoWindow();
                } else {
                    marker.showInfoWindow();
                }
                return true;
            }
        });
        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {

                marker.setSnippet("I'm flying!");
                marker.showInfoWindow();
            }

            @Override
            public void onMarkerDrag(Marker marker) {
                if (marker.getSnippet().contains("_")) {
                    marker.setSnippet("I'm flying! -o-");
                } else {
                    marker.setSnippet("I'm flying! _o_");
                }
                marker.showInfoWindow();
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                updateMarker(marker);
            }
        });


        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {

                MarkerOptions mOption = mMarkerOptionsQueue.poll();
                if (mOption!=null) {
                    final Marker marker = mMap.addMarker(mOption.position(latLng).snippet("Hi!"));

                    updateMarker(marker);

                }
            }

        });

        mMarkerOptionsQueue.add(new MarkerOptions().draggable(true).title("Boy").alpha(0.8f).icon(BitmapDescriptorFactory.fromResource(R.drawable.hp_boy)));

        mMarkerOptionsQueue.add(new MarkerOptions().draggable(true).title("Girl").alpha(0.8f).icon(BitmapDescriptorFactory.fromResource(R.drawable.hp_girl)));

        mMarkerOptionsQueue.add(new MarkerOptions().draggable(true).title("Dog").alpha(0.8f).icon(BitmapDescriptorFactory.fromResource(R.drawable.hp_dog)));

        mMarkerOptionsQueue.add(new MarkerOptions().draggable(true).title("Cat").alpha(0.8f).icon(BitmapDescriptorFactory.fromResource(R.drawable.hp_cat)));


        CameraPosition lastCam = LocationUtil.loadCameraPosition(KEY_CAM, getActivity());
        if (lastCam!=null) {
            mCameraPosition = lastCam;
        }
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(lastCam));

    }


    private void updateMarker(final Marker marker){
        marker.setSnippet("...where am I?");
        marker.showInfoWindow();

        reverseGeocode(marker);

    }

    private void reverseGeocode(final Marker marker) {

        if (mGeocoderAvailable && "google_maps".equals(PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("pref_geocoding",null))){

            new GeocoderAsyncTask(marker).execute();

        } else {

            mOpenMapQuestApi.reverseGeocode(OpenMapQuest.Util.toQuery(marker.getPosition()), new Callback<List<OpenMapQuest.MapQuestPlace>>() {
                @Override
                public void success(List<OpenMapQuest.MapQuestPlace> mapQuestResponse, Response response) {

                    if (mapQuestResponse != null && mapQuestResponse.size() > 0) {
                        OpenMapQuest.MapQuestPlace mapQuestPlace = mapQuestResponse.get(0);

                        if (mCameraPosition.zoom < 5) {
                            marker.setSnippet("Hi! I'm in " + mapQuestPlace.getCountry());
                        } else if (mCameraPosition.zoom < 12) {
                            marker.setSnippet("Hi! I'm in " + mapQuestPlace.getDisplayCity());
                        } else {
                            marker.setSnippet("I'm in " + mapQuestPlace.getDisplaySuburb());
                        }

                        marker.showInfoWindow();
                    }
                }

                @Override
                public void failure(RetrofitError retrofitError) {
                    marker.setSnippet("Oh No! " + retrofitError.getLocalizedMessage());
                    marker.showInfoWindow();


                    Log.e("UberMap", retrofitError.getMessage());
                }
            });
        }
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        Log.d("UberMap", "camera=" + cameraPosition);
        Log.d("UberMap", "you=" + mCurrentLocation);

        mCameraPosition = cameraPosition;

        if (mCurrentLocation!=null) {
/*
            double distance = LocationUtil.distanceInMeters(
                    mCurrentLocation.getLatitude(),
                    mCurrentLocation.getLongitude(),
                    cameraPosition.target.latitude,
                    cameraPosition.target.longitude
            ); // meters
*/
            float[] results = new float[3];
            Location.distanceBetween(
                    mCurrentLocation.getLatitude(),
                    mCurrentLocation.getLongitude(),
                    cameraPosition.target.latitude,
                    cameraPosition.target.longitude,
                    results);

            Log.d("UberMap", "distance =" + results[1]+" initial_bearing="+results[1]+" final_bearing="+results[1]);

            float distance = results[0];

            String unit = "";
            if (distance<1000) {
                unit = "m";
            } else {
                distance = distance / 1000;
                unit = "km";
            }
            NumberFormat nf = NumberFormat.getInstance(Locale.US);
            nf.setMaximumFractionDigits(0);
            mLocationText.setText(nf.format(distance)+unit);

            float rotationDegrees = results[1] > 0 ? results[1]+180 : 180 + results[1];

            mBearingImageView.animate().rotation(rotationDegrees).setDuration(150).start();
        }
    }


    class GeocoderAsyncTask extends AsyncTask<LatLng, Void, Address> {
        SoftReference<Marker> refMarker;

        LatLng location;

        public GeocoderAsyncTask(final Marker marker) {
            refMarker = new SoftReference<Marker>(marker);
        }

        @Override
        protected void onPreExecute() {
            location = refMarker.get().getPosition();
        }

        @Override
        protected Address doInBackground(LatLng... params) {

            location = params.length>0 ? params[0] : location;

            try {
                List<Address> addresses = geo.getFromLocation(location.latitude, location.longitude, 1);
                if (addresses!=null && addresses.size()>0) return addresses.get(0);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Address address) {
            if (!isAdded()) return;
            Marker marker = refMarker.get();

            if (marker==null) return;

            if (address!=null) {
                marker.setSnippet("I'm in "+address.getCountryName());
            } else {
                marker.setSnippet(" ? ");
            }

            marker.showInfoWindow();
        }
    }

}
