package com.smartcampus.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

@Path("/")
public class DiscoveryResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDiscoveryInfo() {
        Map<String, Object> discoveryMap = new HashMap<>();
        discoveryMap.put("version", "1.0");
        discoveryMap.put("adminContact", "admin@smartcampus.edu");
        discoveryMap.put("description", "Smart Campus Sensor & Room Management API");
        
        Map<String, String> links = new HashMap<>();
        links.put("rooms", "/api/v1/rooms");
        links.put("sensors", "/api/v1/sensors");
        discoveryMap.put("_links", links);
        
        return Response.ok(discoveryMap).build();
    }
}
