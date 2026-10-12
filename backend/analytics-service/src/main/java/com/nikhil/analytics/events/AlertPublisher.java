package com.nikhil.analytics.events;

public interface AlertPublisher {
    void publish(AlertCreatedEvent event);
}
