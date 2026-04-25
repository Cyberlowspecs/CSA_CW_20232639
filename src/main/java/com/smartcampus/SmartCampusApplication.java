package com.smartcampus;

import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.ApplicationPath;

@ApplicationPath("/api/v1")
public class SmartCampusApplication extends ResourceConfig {
    public SmartCampusApplication() {
        // Scan for resources, providers, and filters in the given packages
        packages("com.smartcampus.resource", "com.smartcampus.exception", "com.smartcampus.filter");
    }
}
