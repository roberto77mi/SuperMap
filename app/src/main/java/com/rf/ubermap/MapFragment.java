package com.rf.ubermap;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;

import android.app.Fragment;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

/*
 * Also play with:
 * 
 * -> city name
 * http://open.mapquestapi.com/nominatim/v1/search?q=50.4,30.4&format=json
 * 
 * --> weather
 * http://forecast.weather.gov/MapClick.php?lat=40.78158&lon=-73.96648&FcstType=json
 * 
 * --> foursquare api
 */

public class MapFragment extends Fragment implements 
GooglePlayServicesClient.ConnectionCallbacks,
GooglePlayServicesClient.OnConnectionFailedListener,
LocationListener {

	Location mCurrentLocation;


	TextView mLocationText;

	// Define an object that holds accuracy and frequency parameters
	LocationRequest mLocationRequest;


	LocationClient mLocationClient;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		mLocationRequest = LocationRequest.create();
		mLocationRequest.setInterval(10000);

		mLocationRequest.setPriority(
				LocationRequest.PRIORITY_HIGH_ACCURACY);
		mLocationRequest.setFastestInterval(2000);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_map,
				container, false);

		mLocationText = (TextView) rootView.findViewById(R.id.map_location_text);

		
        mLocationClient = new LocationClient(getActivity(), this, this);

		int resultCode =
				GooglePlayServicesUtil.
				isGooglePlayServicesAvailable(getActivity());
		// If Google Play services is available
		if (ConnectionResult.SUCCESS == resultCode) {
			Toast.makeText(getActivity(), "ok!", Toast.LENGTH_SHORT).show();
		} else {
            Toast.makeText(getActivity(), "Google Play Services not available", Toast.LENGTH_SHORT).show();
        }

		return rootView;
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

}
