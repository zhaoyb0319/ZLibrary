package cn.zhaoyb.zlibrary.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import cn.zhaoyb.zlibrary.core.Request;
import cn.zhaoyb.zlibrary.core.Response;
import cn.zhaoyb.zlibrary.core.ZCallBack;
import cn.zhaoyb.zlibrary.utils.ZLoger;

/**
 * 
 * Form表单形式的Http请求
 * 
 */
public class FormRequest extends Request<byte[]> {

    private final HttpParams mParams;

    public FormRequest(String url, ZCallBack callback) {
        this(HttpMethod.GET, url, null, callback);
    }

    public FormRequest(int httpMethod, String url, HttpParams params,
    		ZCallBack callback) {
        super(httpMethod, url, callback);
        if (params == null) {
            params = new HttpParams();
        }
        this.mParams = params;
    }

    @Override
    public String getBodyContentType() {
        if (mParams.getContentType() != null) {
            return mParams.getContentType().getValue();
        } else {
            return super.getBodyContentType();
        }
    }

    @Override
    public Map<String, String> getHeaders() {
        return mParams.getHeaders();
    }

    @Override
    public byte[] getBody() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            mParams.writeTo(bos);
        } catch (IOException e) {
            ZLoger.debug("IOException writing to ByteArrayOutputStream");
        }
        return bos.toByteArray();
    }

    @Override
    public Response<byte[]> parseNetworkResponse(NetworkResponse response) {
        return Response.success(response.data, response.headers,
                HttpHeaderParser.parseCacheHeaders(mConfig, response));
    }

    @Override
    public void deliverResponse(Map<String, String> headers, byte[] response) {
        if (mCallback != null) {
            mCallback.onSuccess(headers, response);
        }
    }
}
