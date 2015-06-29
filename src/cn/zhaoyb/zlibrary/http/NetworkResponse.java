package cn.zhaoyb.zlibrary.http;

import java.util.Collections;
import java.util.Map;

import org.apache.http.HttpStatus;

/**
 * 从NetWork执行器返回的Http响应，包含了本次响应是成功还是失败，请求头，响应内容，HTTP状态码
 */
public class NetworkResponse {

    public NetworkResponse(int statusCode, byte[] data,
            Map<String, String> headers, boolean notModified) {
        this.statusCode = statusCode;
        this.data = data;
        this.headers = headers;
        this.notModified = notModified;
    }

    public NetworkResponse(byte[] data) {
        this(HttpStatus.SC_OK, data, Collections.<String, String> emptyMap(),
                false);
    }

    public NetworkResponse(byte[] data, Map<String, String> headers) {
        this(HttpStatus.SC_OK, data, headers, false);
    }

    public final int statusCode;
    public final byte[] data;
    public final Map<String, String> headers;
    public final boolean notModified; // 如果服务器返回304(Not Modified)，则为true
}