package eu.macphail.energy.device;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.sql.Timestamp;

public interface DeviceDAO {

    @SqlQuery("SELECT charging FROM device WHERE device_id = :device_id")
    Integer selectChargingFromDeviceID(@Bind("device_id") String deviceID);

    @SqlUpdate("INSERT INTO device (device_id, received_timestamp, region_id, charging) " +
                "VALUES (:device_id, :received_timestamp, :region_id, :charging) " +
                "ON CONFLICT (device_id) DO UPDATE " +
                "SET received_timestamp = excluded.received_timestamp" +
                ", charging = excluded.charging")
    void upsertDevice(@Bind("device_id") String deviceID,
                      @Bind("received_timestamp") Timestamp receivedTimestamp,
                      @Bind("region_id") String regionID,
                      @Bind("charging") int charging);

}
