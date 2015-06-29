package cn.zhaoyb.zlibrary.http;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import cn.zhaoyb.zlibrary.core.IHttpStack;
import cn.zhaoyb.zlibrary.core.Request;
import cn.zhaoyb.zlibrary.core.Request.HttpMethod;

/**
 * HttpClient请求端实现
 * 
 */
public class HttpClientStack implements IHttpStack {
    protected final HttpClient mClient;

    private final static String HEADER_CONTENT_TYPE = "Content-Type";

    public HttpClientStack(HttpClient client) {
        mClient = client;
    }

    private static void addHeaders(HttpUriRequest httpRequest,
            Map<String, String> headers) {
        for (String key : headers.keySet()) {
            httpRequest.setHeader(key, headers.get(key));
        }
    }

    @SuppressWarnings("unused")
    private static List<NameValuePair> getPostParameterPairs(
            Map<String, String> postParams) {
        List<NameValuePair> result = new ArrayList<NameValuePair>(
                postParams.size());
        for (String key : postParams.keySet()) {
            result.add(new BasicNameValuePair(key, postParams.get(key)));
        }
        return result;
    }

    @Override
    public HttpResponse performRequest(Request<?> request,
            Map<String, String> additionalHeaders) throws IOException {
        HttpUriRequest httpRequest = createHttpRequest(request,
                additionalHeaders);
        addHeaders(httpRequest, additionalHeaders);
        addHeaders(httpRequest, request.getHeaders());
        onPrepareRequest(httpRequest);
        HttpParams httpParams = httpRequest.getParams();
        int timeoutMs = request.getTimeoutMs();
        HttpConnectionParams.setConnectionTimeout(httpParams, 5000);
        HttpConnectionParams.setSoTimeout(httpParams, timeoutMs);
        return mClient.execute(httpRequest);
    }

    /* protected */static HttpUriRequest createHttpRequest(Request<?> request,
            Map<String, String> additionalHeaders) {
        switch (request.getMethod()) {
        case HttpMethod.GET:
            return new HttpGet(request.getUrl());
        case HttpMethod.DELETE:
            return new HttpDelete(request.getUrl());
        case HttpMethod.POST: {
            HttpPost postRequest = new HttpPost(request.getUrl());
            postRequest.addHeader(HEADER_CONTENT_TYPE,
                    request.getBodyContentType());
            setEntityIfNonEmptyBody(postRequest, request);
            return postRequest;
        }
        case HttpMethod.PUT: {
            HttpPut putRequest = new HttpPut(request.getUrl());
            putRequest.addHeader(HEADER_CONTENT_TYPE,
                    request.getBodyContentType());
            setEntityIfNonEmptyBody(putRequest, request);
            return putRequest;
        }
        case HttpMethod.HEAD:
            return new HttpHead(request.getUrl());
        case HttpMethod.OPTIONS:
            return new HttpOptions(request.getUrl());
        case HttpMethod.TRACE:
            return new HttpTrace(request.getUrl());
        case HttpMethod.PATCH: {
            HttpPatch patchRequest = new HttpPatch(request.getUrl());
            patchRequest.addHeader(HEADER_CONTENT_TYPE,
                    request.getBodyContentType());
            setEntityIfNonEmptyBody(patchRequest, request);
            return patchRequest;
        }
        default:
            throw new IllegalStateException("Unknown request method.");
        }
    }

    private static void setEntityIfNonEmptyBody(
            HttpEntityEnclosingRequestBase httpRequest, Request<?> request) {
        byte[] body = request.getBody();
        if (body != null) {
            HttpEntity entity = new ByteArrayEntity(body);
            httpRequest.setEntity(entity);
        }
    }

    protected void onPrepareRequest(HttpUriRequest request) throws IOException {}

    public static final class HttpPatch extends HttpEntityEnclosingRequestBase {
        public final static String METHOD_NAME = "PATCH";
        public HttpPatch() {
            super();
        }

        public HttpPatch(final URI uri) {
            super();
            setURI(uri);
        }

        public HttpPatch(final String uri) {
            super();
            setURI(URI.create(uri));
        }

        @Override
        public String getMethod() {
            return METHOD_NAME;
        }
    }
}
