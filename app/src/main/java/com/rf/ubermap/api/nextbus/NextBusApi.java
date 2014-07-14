package com.rf.ubermap.api.nextbus;

import com.rf.ubermap.api.nextbus.VehiclesResponse;

import java.util.List;

import retrofit.Callback;
import retrofit.http.GET;
import retrofit.http.Headers;
import retrofit.http.Query;

/**
 * http://webservices.nextbus.com/service/publicXMLFeed?command=vehicleLocations&a=sf-muni
 *
 *

 <body copyright="All data copyright San Francisco Muni 2014.">
 <Error shouldRetry="false">
 last time "t" parameter must be specified in query string
 </Error>
 <vehicle id="8217" routeTag="38BX" dirTag="38BX_OB" lat="37.78054" lon="-122.47658" secsSinceReport="3" predictable="true" heading="264" speedKmHr="27"/>
 <vehicle id="8179" routeTag="43" dirTag="43_OB2" lat="37.80098" lon="-122.43558" secsSinceReport="71" predictable="true" heading="265" speedKmHr="0"/>


 */
public interface NextBusApi {

    public final static String ENDPOINT = " http://webservices.nextbus.com";

    @Headers({
            "User-Agent: RF-UberMap"
    })
    @GET("/service/publicXMLFeed?command=vehicleLocations")
    void getVehiclesPositions(@Query("a") String agency, Callback<VehiclesResponse> responseCallback);


}
