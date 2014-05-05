package ee.cyber.sdsb.proxy.testsuite.testcases;

import ee.cyber.sdsb.proxy.testsuite.Message;
import ee.cyber.sdsb.proxy.testsuite.MessageTestCase;

import static ee.cyber.sdsb.common.ErrorCodes.*;

/**
 * Client sends normal request. Service responds with SOAP with invalid body.
 * Result: SP responds with ServiceFailed error
 */
public class MalformedBodyResponse extends MessageTestCase {
    public MalformedBodyResponse() {
        requestFileName = "getstate.query";
        responseFileName = "malformed-body2.query";
    }

    @Override
    protected void validateFaultResponse(Message receivedResponse) {
        assertErrorCode(SERVER_SERVERPROXY_X, X_SERVICE_FAILED_X,
                X_INVALID_BODY);
    }
}
