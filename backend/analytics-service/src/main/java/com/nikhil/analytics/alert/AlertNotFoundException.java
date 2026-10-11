package com.nikhil.analytics.alert;

public class AlertNotFoundException extends RuntimeException {
    public AlertNotFoundException(Long id) {
        super("Alert not found with id: " + id);
    }
}
