package ee.cyber.sdsb.common.request;

import java.util.UUID;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import ee.cyber.sdsb.common.identifier.ClientId;
import ee.cyber.sdsb.common.identifier.SecurityServerId;
import ee.cyber.sdsb.common.identifier.ServiceId;
import ee.cyber.sdsb.common.message.SoapBuilder;
import ee.cyber.sdsb.common.message.SoapMessageImpl;

import static ee.cyber.sdsb.common.request.ManagementRequests.*;

public class ManagementRequestBuilder {

    private static final Logger LOG =
            LoggerFactory.getLogger(ManagementRequestBuilder.class);

    private static final ObjectFactory FACTORY = new ObjectFactory();

    private static final JAXBContext jaxbCtx = initJaxbContext();

    private final String userId;
    private final ClientId sender;
    private final ClientId receiver;

    public ManagementRequestBuilder(String userId, ClientId sender,
            ClientId receiver) {
        this.userId = userId;
        this.sender = sender;
        this.receiver = receiver;
    }

    // -- Public API methods --------------------------------------------------

    public SoapMessageImpl buildAuthCertRegRequest(
            SecurityServerId securityServer, String address, byte[] authCert)
                    throws Exception {
        LOG.debug("buildAuthCertRegRequest(server: {}, address: {})",
                new Object[] { securityServer, address });

        AuthCertRegRequestType request =
                FACTORY.createAuthCertRegRequestType();
        request.setServer(securityServer);
        request.setAddress(address);
        request.setAuthCert(authCert);

        return buildMessage(element(AUTH_CERT_REG,
                AuthCertRegRequestType.class, request));
    }

    public SoapMessageImpl buildAuthCertDeletionRequest(
            SecurityServerId securityServer, byte[] authCert)
                    throws Exception {
        LOG.debug("buildAuthCertDeletionRequest(server: {})", securityServer);

        AuthCertDeletionRequestType request =
                FACTORY.createAuthCertDeletionRequestType();
        request.setServer(securityServer);
        request.setAuthCert(authCert);

        return buildMessage(element(AUTH_CERT_DELETION,
                AuthCertDeletionRequestType.class, request));
    }

    public SoapMessageImpl buildClientRegRequest(
            SecurityServerId securityServer, ClientId client)
                    throws Exception {
        LOG.debug("buildClientRegRequest(server: {}, client: {})",
                securityServer, client);

        ClientRequestType request = FACTORY.createClientRequestType();
        request.setServer(securityServer);
        request.setClient(client);

        return buildMessage(element(CLIENT_REG,
                ClientRequestType.class, request));
    }

    public SoapMessageImpl buildClientDeletionRequest(
            SecurityServerId securityServer, ClientId client)
                    throws Exception {
        LOG.debug("buildClientDeletionRequest(server: {}, client: {})",
                securityServer, client);

        ClientRequestType request = FACTORY.createClientRequestType();
        request.setServer(securityServer);
        request.setClient(client);

        return buildMessage(element(CLIENT_DELETION,
                ClientRequestType.class, request));
    }

    // -- Private helper methods ----------------------------------------------

    SoapMessageImpl buildMessage(final JAXBElement<?> bodyJaxbElement)
            throws Exception {
        String serviceCode = bodyJaxbElement.getName().getLocalPart();
        ServiceId service = ServiceId.create(receiver, serviceCode);
        return SoapBuilder.build(false /* D/L wrapped */, sender, service,
                    userId, generateQueryId(),
                new SoapBuilder.BodyBuilderCallback() {
                    /**
                     * Using a callback for setting SOAP body enables us to
                     * marshal the content straight into the body element.
                     */
                    @Override
                    public void build(Node soapBodyNode) throws Exception {
                        getMarshaller().marshal(bodyJaxbElement, soapBodyNode);
                    }
                });
    }

    private static String generateQueryId() {
        return UUID.randomUUID().toString();
    }

    private static Marshaller getMarshaller() throws Exception {
        return jaxbCtx.createMarshaller();
    }

    private static <T> JAXBElement<T> element(String name, Class<T> clazz,
            T value) {
        return new JAXBElement<T>(new QName(SoapMessageImpl.NS_SDSB, name),
                clazz, null, value);
    }

    private static JAXBContext initJaxbContext() {
        try {
            return JAXBContext.newInstance(ObjectFactory.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
