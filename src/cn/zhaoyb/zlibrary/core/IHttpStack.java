package cn.zhaoyb.zlibrary.core;

import java.io.IOException;
import java.util.Map;

import org.apache.http.HttpResponse;


/**
 * Http请求端接口
 * 
 * @author zhaoyb(http://www.zhaoyb.cn)
 * 
 */
public interface IHttpStack {
    /**
     * 让Http请求端去发起一个Request
     * 
     * @param request 一次实际请求集合
     * @param additionalHeaders Http请求头
     * @return 一个Http响应
     */
    HttpResponse performRequest(Request<?> request,
                                Map<String, String> additionalHeaders) throws IOException;
}
