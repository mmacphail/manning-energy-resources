package eu.macphail.energy.device.boundary;
import eu.macphail.energy.device.control.DeviceDAO;
import eu.macphail.energy.device.control.DeviceEventKafkaProducer;
import eu.macphail.energy.device.entity.RawDeviceEvent;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Path("/")
public class DeviceController {

    private final DeviceEventKafkaProducer deviceEventKafkaProducer;
    private final DeviceDAO deviceDAO;
    private final long bigMessageSize;

    public DeviceController(long bigMessageSize, DeviceEventKafkaProducer deviceEventKafkaProducer, DeviceDAO deviceDAO) {
        this.bigMessageSize = bigMessageSize;
        this.deviceEventKafkaProducer = deviceEventKafkaProducer;
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
        RawDeviceEvent event = new RawDeviceEvent(Instant.now().toEpochMilli(), id, payload);
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
        if(payloadBytes.length < bigMessageSize) {
            deviceEventKafkaProducer.sendRawEnergyEvent(event);
        } else {
            deviceEventKafkaProducer.sendSlowRawEnergyEvent(event);
        }
    }

}
