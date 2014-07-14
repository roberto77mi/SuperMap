package com.rf.ubermap.api.nextbus;

import com.rf.ubermap.api.nextbus.Vehicle;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Text;
import org.simpleframework.xml.Transient;

import java.util.List;

/**
 * Created by roberto on 7/10/14.
 */
@Root(name = "body")
public class VehiclesResponse {

    @Element(name="Error")
    Error error;

    @Attribute
    String copyright;

    @ElementList(entry="vehicle", inline=true)
    List<Vehicle> list;

    @Element
    LastTime lastTime;


    public List<Vehicle> getList(){
        return list;
    }

    @Override
    public String toString() {
        if (list==null) return "null";
        return list.toString();
    }
}

@Root
class Error {

    @Text
    String body;

    @Attribute
    boolean shouldRetry;
}

@Root
class LastTime {
    @Attribute
    long time;
}
