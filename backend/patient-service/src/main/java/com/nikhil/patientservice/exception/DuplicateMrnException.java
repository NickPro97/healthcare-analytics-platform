package com.nikhil.patientservice.exception;

public class DuplicateMrnException extends RuntimeException {
    public DuplicateMrnException(String mrn) {
        super("A patient with MRN " + mrn + " already exists");
    }
}
