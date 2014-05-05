package ee.cyber.sdsb.common.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.EofSensorInputStream;
import org.apache.http.conn.EofSensorWatcher;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ee.cyber.sdsb.common.CodedException;

import static ee.cyber.sdsb.common.ErrorCodes.*;

public abstract class AbstractHttpSender implements Closeable {

    private static final Logger LOG =
            LoggerFactory.getLogger(AbstractHttpSender.class);

    private static final int CHUNKED = -1;

    private final Map<String, String> additionalHeaders = new HashMap<>();

    private String responseContentType;
    private InputStream responseContent;
    private Map<String, String> responseHeaders;

    protected final HttpContext context;

    protected HttpRequestBase request;

    protected int timeout = 30000; // default 30 sec

    public AbstractHttpSender() {
        context = new BasicHttpContext();
    }

    /**
     * Sets the connection timeout in milliseconds.
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * Sets the value of an attribute.
     */
    public void setAttribute(String name, Object value) {
        context.setAttribute(name, value);
    }

    /**
     * Adds an additional header to the request.
     */
    public void addHeader(String name, String value) {
        additionalHeaders.put(name, value);
    }

    /**
     * Returns the response content type.
     */
    public String getResponseContentType() {
        return responseContentType;
    }

    /**
     * Returns the response content input stream.
     */
    public InputStream getResponseContent() {
        return responseContent;
    }

    /**
     * Returns all response headers returned in the response.
     */
    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }

    public void handleResponse(HttpResponse response) throws Exception {
        LOG.debug("handleResponse()");

        checkResponseStatus(response);

        responseHeaders = getResponseHeaders(response);

        HttpEntity responseEntity = getResponseEntity(response);
        responseContentType = getResponseContentType(responseEntity);

        // Wrap the response input stream in order to catch EOF errors.
        responseContent = new EofSensorInputStream(
                responseEntity.getContent(), new ResponseStreamWatcher());
    }

    public abstract void doGet(URI address) throws Exception;

    public abstract void doPost(URI address, InputStream content,
            String contentType) throws Exception;

    @Override
    public void close() {
        if (request != null) {
            request.releaseConnection();
        }
    }

    protected void addAdditionalHeaders() {
        for (Entry<String, String> header : additionalHeaders.entrySet()) {
            request.addHeader(header.getKey(), header.getValue());
        }
    }

    protected RequestConfig getRequestConfig() {
        RequestConfig.Builder rb = RequestConfig.custom();
        rb.setConnectTimeout(timeout);
        rb.setConnectionRequestTimeout(timeout);
        return rb.build();
    }

    protected static InputStreamEntity createInputStreamEntity(
            InputStream content, String contentType) {
        InputStreamEntity entity = new InputStreamEntity(content, CHUNKED);
        entity.setChunked(true); // Just in case
        entity.setContentType(contentType);
        return entity;
    }

    protected static void checkResponseStatus(HttpResponse response) {
        // TODO: people say that according to SOAP spec, it is possible
        // to send faults with HTTP 500 error code.
        int status = response.getStatusLine().getStatusCode();
        if (status != HttpStatus.OK_200) {
            throw new CodedException(X_HTTP_ERROR,
                    "Server responded with error %s: %s", status,
                    response.getStatusLine().getReasonPhrase());
        }
    }

    protected static Map<String, String> getResponseHeaders(
            HttpResponse response) {
        Map<String, String> headers = new HashMap<>();
        for (Header header : response.getAllHeaders()) {
            headers.put(header.getName(), header.getValue());
        }

        return headers;
    }

    protected static HttpEntity getResponseEntity(HttpResponse response) {
        HttpEntity entity = response.getEntity();
        if (entity == null) {
            throw new CodedException(X_HTTP_ERROR,
                    "Could not get content from response");
        }
        return entity;
    }

    protected static String getResponseContentType(HttpEntity entity) {
        Header contentType = entity.getContentType();
        if (contentType == null) {
            throw new CodedException(X_INVALID_CONTENT_TYPE,
                    "Could not get content type from response");
        }
        return contentType.getValue();
    }

    protected class ResponseStreamWatcher implements EofSensorWatcher {
        @Override
        public boolean eofDetected(InputStream wrapped) throws IOException {
            return true;
        }

        @Override
        public boolean streamClosed(InputStream wrapped) throws IOException {
            throw new CodedException(X_IO_ERROR, "Stream was closed");
        }

        @Override
        public boolean streamAbort(InputStream wrapped) throws IOException {
            throw new CodedException(X_IO_ERROR, "Stream was aborted");
        }
    }
}
