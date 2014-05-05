package ee.cyber.sdsb.proxy.testsuite.testcases;

import java.security.cert.X509Certificate;

import ee.cyber.sdsb.common.conf.GlobalConf;
import ee.cyber.sdsb.common.identifier.ClientId;
import ee.cyber.sdsb.proxy.conf.ServerConf;
import ee.cyber.sdsb.proxy.testsuite.Message;
import ee.cyber.sdsb.proxy.testsuite.SslMessageTestCase;
import ee.cyber.sdsb.proxy.testsuite.TestGlobalConf;
import ee.cyber.sdsb.proxy.testsuite.TestServerConf;

import static ee.cyber.sdsb.common.ErrorCodes.SERVER_CLIENTPROXY_X;
import static ee.cyber.sdsb.common.ErrorCodes.X_SSL_AUTH_FAILED;

public class SslClientCertVerificationError extends SslMessageTestCase {

    public SslClientCertVerificationError() {
        requestFileName = "getstate.query";
    }

    @Override
    public String getProviderAddress(String providerName) {
        return "127.0.0.5";
    }

    @Override
    protected void startUp() throws Exception {
        ServerConf.reload(new TestServerConf());
        GlobalConf.reload(new TestGlobalConf() {
            @Override
            public boolean authCertMatchesMember(X509Certificate cert,
                    ClientId member) {
                return false;
            }
        });
    }

    @Override
    protected void validateFaultResponse(Message receivedResponse)
            throws Exception {
        assertErrorCodeStartsWith(SERVER_CLIENTPROXY_X, X_SSL_AUTH_FAILED);
    }
}
