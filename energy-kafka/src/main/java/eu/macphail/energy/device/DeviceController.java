package eu.macphail.energy.device;

import eu.macphail.energy.RawEnergyEvent;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.Instant;

@Path("/")
public class DeviceController {

    private final EnergyEventStreamingService energyEventStreamingService;
    private final DeviceDAO deviceDAO;

    public DeviceController(EnergyEventStreamingService energyEventStreamingService, DeviceDAO deviceDAO) {
        this.energyEventStreamingService = energyEventStreamingService;
        this.deviceDAO = deviceDAO;
    }

    @GET
    @Path("/devices/{id}")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response getDeviceCharging(@PathParam("id") String id) {
        Integer charging = this.deviceDAO.selectChargingFromDeviceID(id);
        if(charging != null) {
            return Response
                    .status(Response.Status.OK)
                    .entity(charging.toString())
                    .build();
        } else {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .build();
        }
    }

    @POST
    @Path("/events/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void registerDeviceEvent(@PathParam("id") String id, String payload) {
        RawEnergyEvent rawEnergyEvent = new RawEnergyEvent(Instant.now().toEpochMilli(), id, payload);
        energyEventStreamingService.sendRawEnergyEvent(rawEnergyEvent);
    }

}
