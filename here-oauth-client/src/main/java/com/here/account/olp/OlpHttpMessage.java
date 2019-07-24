package com.here.account.olp;

public interface OlpHttpMessage {
    String X_CORRELATION_ID = "X-Correlation-ID";

    /**
     * Get the correlationId (the unique value for tracking a request across services and within a service).
     *
     * @return the correlationId
     */
    String getCorrelationId();

    /**
     * Set the correlationId (the unique value for tracking a request across services and within a service).
     *
     * @param correlationId     the correlationId
     * @return                  the class implementing this method. Allows chaining methods,
     *                          e.g. classInstance.setCorrelationId("abc123").setFoo().setBar()
     */
    OlpHttpMessage setCorrelationId(String correlationId);
}
