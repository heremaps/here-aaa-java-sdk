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

    ErrorResponse errorResponse;

    @Test
    public void test_6arg_constructor() {
        errorResponse = new ErrorResponse(error,
                errorDescription,
                errorId,
                httpStatus,
                errorCode,
                message);

        verifyFirst6Properties();

        String actualTitle = errorResponse.getTitle();
        assertTrue("expected title " + null + ", actual " + actualTitle,
                null == actualTitle);

        Integer actualStatus = errorResponse.getStatus();
        assertTrue("expected status " + null + ", actual " + actualStatus,
                null == (actualStatus));

        String actualCode = errorResponse.getCode();
        assertTrue("expected code " + null + ", actual " + actualCode,
                null == (actualCode));

        String actualCause = errorResponse.getCause();
        assertTrue("expected cause " + null + ", actual " + actualCause,
                null == (actualCause));

        String actualAction = errorResponse.getAction();
        assertTrue("expected action " + null + ", actual " + actualAction,
                null == (actualAction));

        String actualCorrelationId = errorResponse.getCorrelationId();
        assertTrue("expected correlationId " + null + ", actual " + actualCorrelationId,
                null == (actualCorrelationId));

    }

    private void verifyFirst6Properties() {
        String actualError = errorResponse.getError();
        assertTrue("expected error " + error + ", actual " + actualError,
                error.equals(actualError));

        String actualErrorDescription = errorResponse.getErrorDescription();
        assertTrue("expected errorDescription " + errorDescription + ", actual " + actualErrorDescription,
                errorDescription.equals(actualErrorDescription));

        String actualErrorId = errorResponse.getErrorId();
        assertTrue("expected errorId " + errorId + ", actual " + actualErrorId,
                errorId.equals(actualErrorId));

        Integer actualHttpStatus = errorResponse.getHttpStatus();
        assertTrue("expected httpStatus " + httpStatus + ", actual " + actualHttpStatus,
                httpStatus.equals(actualHttpStatus));

        Integer actualErrorCode = errorResponse.getErrorCode();
        assertTrue("expected errorCode " + errorCode + ", actual " + actualErrorCode,
                errorCode.equals(actualErrorCode));

        String actualMessage = errorResponse.getMessage();
        assertTrue("expected message " + message + ", actual " + actualMessage,
                message.equals(actualMessage));

    }

    @Test
    public void test_allArgs_constructor() {
        errorResponse = new ErrorResponse(error,
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

        verifyFirst6Properties();

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
