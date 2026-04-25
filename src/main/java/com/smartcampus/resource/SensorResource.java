package com.smartcampus.resource;

import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    @POST
    public Response createSensor(Sensor sensor) {
        if (sensor == null || sensor.getId() == null || sensor.getRoomId() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Sensor ID and Room ID are required\"}")
                    .build();
        }
        
        // Verify room exists
        Room room = DataStore.getRooms().get(sensor.getRoomId());
        if (room == null) {
            throw new LinkedResourceNotFoundException("Specified room does not exist");
        }
        
        DataStore.getSensors().put(sensor.getId(), sensor);
        
        // Assign sensor to room
        if (!room.getSensorIds().contains(sensor.getId())) {
            room.getSensorIds().add(sensor.getId());
        }
        
        return Response.status(Response.Status.CREATED).entity(sensor).build();
    }

    @GET
    public Response getSensors(@QueryParam("type") String type) {
        List<Sensor> sensorsList = new ArrayList<>(DataStore.getSensors().values());
        
        if (type != null && !type.trim().isEmpty()) {
            sensorsList = sensorsList.stream()
                    .filter(s -> type.equalsIgnoreCase(s.getType()))
                    .collect(Collectors.toList());
        }
        
        return Response.ok(sensorsList).build();
    }

    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingsResource(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }
}
