package cn.zhaoyb.zlibrary.http;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;

import cn.zhaoyb.zlibrary.core.IHttpStack;
import cn.zhaoyb.zlibrary.core.Request;
import cn.zhaoyb.zlibrary.core.Request.HttpMethod;

/**
 * HttpUrlConnection方式实现
 * 
 */
public class HttpConnectStack implements IHttpStack {

    private static final String HEADER_CONTENT_TYPE = "Content-Type";

    private final UrlRewriter mUrlRewriter;
    private final SSLSocketFactory mSslSocketFactory;

    public interface UrlRewriter {
        /** 重写用于请求的URL */
        String rewriteUrl(String originalUrl);
    }

    public HttpConnectStack() {
        this(null);
    }

    public HttpConnectStack(UrlRewriter urlRewriter) {
        this(urlRewriter, null);
    }

    public HttpConnectStack(UrlRewriter urlRewriter,
            SSLSocketFactory sslSocketFactory) {
        mUrlRewriter = urlRewriter;
        mSslSocketFactory = sslSocketFactory;
    }

    @Override
    public HttpResponse performRequest(Request<?> request,
            Map<String, String> additionalHeaders) throws IOException {
        String url = request.getUrl();
        HashMap<String, String> map = new HashMap<String, String>();
        map.putAll(request.getHeaders());
        map.putAll(additionalHeaders);

        if (mUrlRewriter != null) {
            String rewritten = mUrlRewriter.rewriteUrl(url);
            if (rewritten == null) {
                throw new IOException("URL blocked by rewriter: " + url);
            }
            url = rewritten;
        }
        URL parsedUrl = new URL(url);
        HttpURLConnection connection = openConnection(parsedUrl, request);
        for (String headerName : map.keySet()) {
            connection.addRequestProperty(headerName, map.get(headerName));
        }
        setConnectionParametersForRequest(connection, request);

        ProtocolVersion protocolVersion = new ProtocolVersion("HTTP", 1, 1);
        int responseCode = connection.getResponseCode();
        if (responseCode == -1) {
            throw new IOException(
                    "Could not retrieve response code from HttpUrlConnection.");
        }
        StatusLine responseStatus = new BasicStatusLine(protocolVersion,
                connection.getResponseCode(), connection.getResponseMessage());

        BasicHttpResponse response = new BasicHttpResponse(responseStatus);
        response.setEntity(entityFromConnection(connection));
        for (Entry<String, List<String>> header : connection.getHeaderFields()
                .entrySet()) {
            if (header.getKey() != null) {
                String value = "";
                for (String v : header.getValue()) {
                    value += (v + "; ");
                }
                Header h = new BasicHeader(header.getKey(), value);
                response.addHeader(h);
            }
        }
        return response;
    }

    /**
     * 从给定的HttpurlConnection创建HttpEntity
     */
    private static HttpEntity entityFromConnection(HttpURLConnection connection) {
        BasicHttpEntity entity = new BasicHttpEntity();
        InputStream inputStream;
        try {
            inputStream = connection.getInputStream();
        } catch (IOException ioe) {
            inputStream = connection.getErrorStream();
        }
        entity.setContent(inputStream);
        entity.setContentLength(connection.getContentLength());
        entity.setContentEncoding(connection.getContentEncoding());
        entity.setContentType(connection.getContentType());
        return entity;
    }

    private HttpURLConnection openConnection(URL url, Request<?> request)
            throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        int timeoutMs = request.getTimeoutMs();
        connection.setConnectTimeout(timeoutMs);
        connection.setReadTimeout(timeoutMs);
        connection.setUseCaches(false);
        connection.setDoInput(true);

        // use caller-provided custom SslSocketFactory, if any, for HTTPS
        if ("https".equals(url.getProtocol()) && mSslSocketFactory != null) {
            ((HttpsURLConnection) connection)
                    .setSSLSocketFactory(mSslSocketFactory);
        }

        return connection;
    }

    /* package */static void setConnectionParametersForRequest(
            HttpURLConnection connection, Request<?> request)
            throws IOException {
        switch (request.getMethod()) {
        case HttpMethod.GET:
            connection.setRequestMethod("GET");
            break;
        case HttpMethod.DELETE:
            connection.setRequestMethod("DELETE");
            break;
        case HttpMethod.POST:
            connection.setRequestMethod("POST");
            addBodyIfExists(connection, request);
            break;
        case HttpMethod.PUT:
            connection.setRequestMethod("PUT");
            addBodyIfExists(connection, request);
            break;
        case HttpMethod.HEAD:
            connection.setRequestMethod("HEAD");
            break;
        case HttpMethod.OPTIONS:
            connection.setRequestMethod("OPTIONS");
            break;
        case HttpMethod.TRACE:
            connection.setRequestMethod("TRACE");
            break;
        case HttpMethod.PATCH:
            connection.setRequestMethod("PATCH");
            addBodyIfExists(connection, request);
            break;
        default:
            throw new IllegalStateException("Unknown method type.");
        }
    }

    /**
     * 如果有body则添加
     */
    private static void addBodyIfExists(HttpURLConnection connection,
            Request<?> request) throws IOException {
        byte[] body = request.getBody();
        if (body != null) {
            connection.setDoOutput(true);
            connection.addRequestProperty(HEADER_CONTENT_TYPE,
                    request.getBodyContentType());
            DataOutputStream out = new DataOutputStream(
                    connection.getOutputStream());
            out.write(body);
            out.close();
        }
    }
}
