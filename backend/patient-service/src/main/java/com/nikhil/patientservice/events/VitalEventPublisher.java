package com.nikhil.patientservice.events;

public interface VitalEventPublisher {
    void publish(VitalSignRecordedEvent event);
}
