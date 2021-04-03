 kafka-consumer-groups $SERVER --topic tracking.energy.rawevent --group raw-devices --reset-offsets --to-earliest --execute
 kafka-consumer-groups $SERVER --topic tracking.energy.rawevent.slow --group raw-devices --reset-offsets --to-earliest --execute
 kafka-consumer-groups $SERVER --topic tracking.energy.deviceevent --group device-event-to-database --reset-offsets --to-earliest --execute