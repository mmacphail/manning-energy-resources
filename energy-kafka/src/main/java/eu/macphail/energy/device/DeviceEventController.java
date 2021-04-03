package eu.macphail.energy.device;

import eu.macphail.energy.RawEnergyEvent;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import java.time.Instant;

@Path("/events")
public class DeviceEventController {

    private final EnergyEventStreamingService energyEventStreamingService;

    public DeviceEventController(EnergyEventStreamingService energyEventStreamingService) {
        this.energyEventStreamingService = energyEventStreamingService;
    }

    @POST
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void registerDeviceEvent(@PathParam("id") String id, String payload) {
        RawEnergyEvent rawEnergyEvent = new RawEnergyEvent(Instant.now().toEpochMilli(), id, payload);
        energyEventStreamingService.sendRawEnergyEvent(rawEnergyEvent);
    }

}
