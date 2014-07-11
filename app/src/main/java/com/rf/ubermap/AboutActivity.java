package com.rf.ubermap;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.rf.ubermap.api.NextBusApi;
import com.rf.ubermap.api.nextbus.VehiclesResponse;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.converter.SimpleXmlConverter;

public class AboutActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);




        // move this code in a SanFranciscoMapFragment
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(NextBusApi.ENDPOINT)
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setConverter(new SimpleXmlConverter())
                .build();


        NextBusApi service = restAdapter.create(NextBusApi.class);
        service.getVehiclesPositions("sf-muni", new Callback<VehiclesResponse>() {
            @Override
            public void success(VehiclesResponse vehiclesResponse, Response response) {

                Log.d("UberMap", vehiclesResponse.getList().size()+ " vehicles: " + vehiclesResponse);

            }

            @Override
            public void failure(RetrofitError retrofitError) {
                Log.d("UberMap", "Error Bus " + retrofitError);
            }
        });



    }



}
