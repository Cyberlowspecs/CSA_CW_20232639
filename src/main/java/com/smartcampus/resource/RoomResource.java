package com.smartcampus.resource;

import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;
import com.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    @GET
    public Response getAllRooms() {
        List<Room> roomsList = new ArrayList<>(DataStore.getRooms().values());
        return Response.ok(roomsList).build();
    }

    @POST
    public Response createRoom(Room room) {
        if (room == null || room.getId() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Room ID is required\"}")
                    .build();
        }
        DataStore.getRooms().put(room.getId(), room);
        return Response.status(Response.Status.CREATED).entity(room).build();
    }

    @GET
    @Path("/{roomId}")
    public Response getRoom(@PathParam("roomId") String roomId) {
        Room room = DataStore.getRooms().get(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"Room not found\"}")
                    .build();
        }
        return Response.ok(room).build();
    }

    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = DataStore.getRooms().get(roomId);
        if (room == null) {
            // If room doesn't exist, idempotency means we shouldn't throw an error necessarily, 
            // but returning 404 is also standard REST practice.
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"Room not found\"}")
                    .build();
        }
        
        // Safety check: Does it have assigned sensors?
        if (!room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException("Cannot delete room with assigned sensors");
        }

        DataStore.getRooms().remove(roomId);
        return Response.noContent().build(); // 204 No Content is ideal for DELETE
    }
}
