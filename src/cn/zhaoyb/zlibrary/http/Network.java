package cn.zhaoyb.zlibrary.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.cookie.DateUtils;

import cn.zhaoyb.zlibrary.core.ICache;
import cn.zhaoyb.zlibrary.core.IHttpStack;
import cn.zhaoyb.zlibrary.core.Request;
import cn.zhaoyb.zlibrary.utils.ZLoger;

/**
 * 网络请求执行器，将传入的Request使用HttpStack客户端发起网络请求，并返回一个NetworkRespond结果
 */
public class Network {
    protected static final boolean DEBUG = HttpConfig.DEBUG;
    protected final IHttpStack mHttpStack;

    public Network(IHttpStack httpStack) {
        mHttpStack = httpStack;
    }

    /**
     * 实际执行一个请求的方法
     * 
     * @param request  一个请求任务
     * @return 一个不会为null的响应
     * @throws HttpException
     */
    public NetworkResponse performRequest(Request<?> request)
            throws HttpException {
        while (true) {
            HttpResponse httpResponse = null;
            byte[] responseContents = null;
            Map<String, String> responseHeaders = new HashMap<String, String>();
            try {
                // 标记Http响应头在Cache中的tag
                Map<String, String> headers = new HashMap<String, String>();
                addCacheHeaders(headers, request.getCacheEntry());
                httpResponse = mHttpStack.performRequest(request, headers);

                StatusLine statusLine = httpResponse.getStatusLine();
                int statusCode = statusLine.getStatusCode();
                responseHeaders = convertHeaders(httpResponse.getAllHeaders());
                if (statusCode == HttpStatus.SC_NOT_MODIFIED) { // 304
                    return new NetworkResponse(HttpStatus.SC_NOT_MODIFIED,
                            request.getCacheEntry() == null ? null : request
                                    .getCacheEntry().data,
                            responseHeaders, true);
                }

                if (httpResponse.getEntity() != null) {
                    if (request instanceof FileRequest) {
                        responseContents = ((FileRequest) request)
                                .handleResponse(httpResponse);
                    } else {
                        responseContents = entityToBytes(httpResponse
                                .getEntity());
                    }
                } else {
                    responseContents = new byte[0];
                }

                if (statusCode < 200 || statusCode > 299) {
                    throw new IOException();
                }
                return new NetworkResponse(statusCode, responseContents,
                        responseHeaders, false);
            } catch (SocketTimeoutException e) {
                throw new HttpException(new SocketTimeoutException(
                        "socket timeout"));
            } catch (ConnectTimeoutException e) {
                throw new HttpException(new SocketTimeoutException(
                        "socket timeout"));
            } catch (MalformedURLException e) {
                throw new RuntimeException("Bad URL " + request.getUrl(), e);
            } catch (IOException e) {
                int statusCode = 0;
                NetworkResponse networkResponse = null;
                if (httpResponse != null) {
                    statusCode = httpResponse.getStatusLine().getStatusCode();
                } else {
                    throw new HttpException("NoConnection error", e);
                }
                ZLoger.debug("Unexpected response code %d for %s", statusCode,
                        request.getUrl());
                if (responseContents != null) {
                    networkResponse = new NetworkResponse(statusCode,
                            responseContents, responseHeaders, false);
                    if (statusCode == HttpStatus.SC_UNAUTHORIZED
                            || statusCode == HttpStatus.SC_FORBIDDEN) {
                        throw new HttpException("auth error");
                    } else {
                        throw new HttpException(
                                "server error, Only throw ServerError for 5xx status codes.",
                                networkResponse);
                    }
                } else {
                    throw new HttpException(networkResponse);
                }
            }
        }
    }

    /**
     * 标记Respondeader响应头在Cache中的tag
     * 
     * @param headers
     * @param entry
     */
    private void addCacheHeaders(Map<String, String> headers, ICache.Entry entry) {
        if (entry == null) {
            return;
        }
        if (entry.etag != null) {
            headers.put("If-None-Match", entry.etag);
        }
        if (entry.serverDate > 0) {
            Date refTime = new Date(entry.serverDate);
            headers.put("If-Modified-Since", DateUtils.formatDate(refTime));
        }
    }

    /**
     * 把HttpEntry转换为byte[]
     * 
     * @param entity
     * @return
     * @throws IOException
     * @throws HttpException
     */
    private byte[] entityToBytes(HttpEntity entity) throws IOException,
            HttpException {
        PoolingByteArrayOutputStream bytes = new PoolingByteArrayOutputStream(
                ByteArrayPool.get(), (int) entity.getContentLength());
        byte[] buffer = null;
        try {
            InputStream in = entity.getContent();
            if (in == null) {
                throw new HttpException("server error");
            }
            buffer = ByteArrayPool.get().getBuf(1024);
            int count;
            while ((count = in.read(buffer)) != -1) {
                bytes.write(buffer, 0, count);
            }
            return bytes.toByteArray();
        } finally {
            try {
                entity.consumeContent();
            } catch (IOException e) {
                ZLoger.debug("Error occured when calling consumingContent");
            }
            ByteArrayPool.get().returnBuf(buffer);
            bytes.close();
        }
    }

    /**
     * 转换RespondHeader为Map类型
     */
    private static Map<String, String> convertHeaders(Header[] headers) {
        Map<String, String> result = new HashMap<String, String>();
        for (int i = 0; i < headers.length; i++) {
            result.put(headers[i].getName(), headers[i].getValue());
        }
        return result;
    }
}
