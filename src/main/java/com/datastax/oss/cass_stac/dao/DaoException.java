package com.datastax.oss.cass_stac.dao;

public class DaoException extends Exception {

    // Default constructor
    public DaoException() {
        super();
    }

    // Constructor that accepts a message
    public DaoException(String message) {
        super(message);
    }

    // Constructor that accepts a message and a cause
    public DaoException(String message, Throwable cause) {
        super(message, cause);
    }

    // Constructor that accepts a cause
    public DaoException(Throwable cause) {
        super(cause);
    }
}
