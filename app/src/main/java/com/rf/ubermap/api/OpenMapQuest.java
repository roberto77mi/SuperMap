package com.rf.ubermap.api;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.annotations.SerializedName;

import java.util.List;

import retrofit.Callback;
import retrofit.http.GET;
import retrofit.http.Headers;
import retrofit.http.Query;

/**
 * Created by roberto on 7/9/14.
 */
public interface OpenMapQuest {

    public final static String ENDPOINT = "http://open.mapquestapi.com";

    @Headers({
            "User-Agent: RF-UberMap"
    })
    @GET("/nominatim/v1/search?format=json&accept-language=en&addressdetails=true")
    void reverseGeocode(@Query("q") String query, Callback<List<MapQuestPlace>> cb);

//    class MapQuestResponse {
//        public List<MapQuestPlace> places;
//    }

    class MapQuestPlace {
        String place_id;
        String licence;
        String osm_type; // way, node
        String osm_id;
        float lat;
        float lon;
        public String display_name;

        @SerializedName("class")
        String placeClass; // natural, place, highway, amenity, shop

        String type;  // coastline, hamlet, primary, restaurant, maritime, clothes
        float importance; // ?

        MapQuestAddress address;


        public String getCountry(){
            if (address==null) return null;

            return address.country;
        }

        public String getDisplayCity(){
            if (address==null || address.country==null) return "unknown";
            if (address.city!=null && address.city.equals(address.county)){
                return address.city+", "+address.country;
            } else if (address.city!=null){
                return address.city +", "+address.county +", "+address.country;
            } else {
                return address.country;
            }
        }

        public String getDisplaySuburb(){
            if (address==null || address.country==null) return "unknown";

            String city = address.city!=null ? address.city : address.county!=null ? address.county : address.country;

            if (address.road!=null) {
                return address.road +", "+city;
            } else if (address.neighbourhood!=null) {
                return address.neighbourhood +", "+city;
            } else if (address.suburb!=null) {
                return address.suburb +", "+city;
            } else if (address.hamlet!=null) {
                return address.hamlet +", "+city;
            } else return city;
        }

        public String getDisplayName(int zoomLevel){
            if (address==null || address.country==null) return "unknown";

           // TODO

            return address.country;
        }

    }

    class MapQuestAddress {
        public String coastline;
        public String house_number;
        public String road;
        public String hamlet; // "village"
        public String neighbourhood;
        public String suburb;
        public String city;
        public String postcode;
        public String county;
        public String state;
        public String country;
        public String country_code;
        public String continent;

    }

    class Util {
       public static String toQuery(LatLng latLng) {

           if (latLng==null) return "";
           return latLng.latitude+","+latLng.longitude;

        }
    }
}
