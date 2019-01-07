package com.here.account.oauth2;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ErrorResponseTest {

    private String error = "error";
    private String errorDescription = "errorDescription";
    private String errorId = "errorId";
    private Integer httpStatus = 400;
    private Integer errorCode = 2;
    private String message = "message";
    private String title = "title";
    private Integer status = 400;
    private String code = "code";
    private String cause = "cause";
    private String action = "action";
    private String correlationId = "correlationId";

    @Test
    public void test_correlationId() {
        ErrorResponse errorResponse = new ErrorResponse(error,
                errorDescription,
                errorId,
                httpStatus,
                errorCode,
                message,
                title,
                status,
                code,
                cause,
                action,
                correlationId);
        String actualTitle = errorResponse.getTitle();
        assertTrue("expected title " + title + ", actual " + actualTitle,
                title.equals(actualTitle));

        Integer actualStatus = errorResponse.getStatus();
        assertTrue("expected status " + status + ", actual " + actualStatus,
            status.equals(actualStatus));

        String actualCode = errorResponse.getCode();
        assertTrue("expected code " + code + ", actual " + actualCode,
            code.equals(actualCode));

        String actualCause = errorResponse.getCause();
        assertTrue("expected cause " + cause + ", actual " + actualCause,
            cause.equals(actualCause));

        String actualAction = errorResponse.getAction();
        assertTrue("expected action " + action + ", actual " + actualAction,
            action.equals(actualAction));

        String actualCorrelationId = errorResponse.getCorrelationId();
        assertTrue("expected correlationId " + correlationId + ", actual " + actualCorrelationId,
                correlationId.equals(actualCorrelationId));
    }
}
