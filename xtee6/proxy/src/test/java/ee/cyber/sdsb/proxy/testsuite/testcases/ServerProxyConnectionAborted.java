package ee.cyber.sdsb.proxy.testsuite.testcases;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ee.cyber.sdsb.common.SystemProperties;
import ee.cyber.sdsb.proxy.testsuite.Message;
import ee.cyber.sdsb.proxy.testsuite.MessageTestCase;

import static ee.cyber.sdsb.common.ErrorCodes.SERVER_CLIENTPROXY_X;
import static ee.cyber.sdsb.common.ErrorCodes.X_NETWORK_ERROR;

/**
 * Client sends normal message, SP aborts connection.
 * Result: CP responds with RequestFailed
 */
public class ServerProxyConnectionAborted extends MessageTestCase {
    private static final Logger LOG = LoggerFactory.getLogger(
            ServerProxyConnectionAborted.class);

    private Thread serverThread;

    public ServerProxyConnectionAborted() {
        requestFileName = "getstate.query";
    }

    @Override
    protected void startUp() throws Exception {
        serverThread = new Thread(new AbortingServer());
        serverThread.start();
        Thread.sleep(1000);
    }

    @Override
    protected void closeDown() throws Exception {
        serverThread.interrupt();
        serverThread.join();
    }

    @Override
    public String getProviderAddress(String providerName) {
        // We'll connect to local AbortingServer
        return "127.0.0.3";
    }

    @Override
    protected void validateFaultResponse(Message receivedResponse) {
        assertErrorCode(SERVER_CLIENTPROXY_X, X_NETWORK_ERROR);
    }

    private class AbortingServer implements Runnable {
        @Override
        public void run() {
            try {
                byte[] buffer = new byte[1024];
                int port = SystemProperties.getServerProxyPort();

                LOG.debug("Starting to listen at 127.0.0.3:{}", port);

                ServerSocket srvr = new ServerSocket(port, 1,
                        InetAddress.getByName("127.0.0.3"));
                Socket skt = srvr.accept();

                LOG.debug("Received connection from {}",
                        skt.getRemoteSocketAddress());

                // Read something.
                skt.getInputStream().read(buffer);
                skt.getInputStream().close();
                skt.close();
                srvr.close();
                LOG.debug("Closing the test socket");
            } catch (Exception ex) {
                LOG.debug("Aborting server failed", ex);
            }
        }
    }
}
