package cn.zhaoyb.zlibrary.core;

import cn.zhaoyb.zlibrary.http.HttpException;

/**
 * 分发器，将异步线程中的结果响应到UI线程中
 * 
 * @author zhaoyb(http://www.zhaoyb.cn)
 * 
 */
public interface IDelivery {
    /**
     * 分发响应结果
     * 
     * @param request
     * @param response
     */
    void postResponse(Request<?> request, Response<?> response);

    /**
     * 分发Failure事件
     * 
     * @param request 请求
     * @param error 异常原因
     */
    void postError(Request<?> request, HttpException error);

    /**
     * 当有中介响应的时候，会被调用，首先返回中介响应，并执行runnable(实际就是再去请求网络)
     * 所谓中介响应：当本地有一个未过期缓存的时候会优先返回一个缓存，但如果这个缓存又是需要刷新的时候，会再次去请求网络，
     * 那么之前返回的那个有效但需要刷新的就是中介响应
     */
    void postResponse(Request<?> request, Response<?> response,
                      Runnable runnable);

    /**
     * 分发下载进度事件
     * 
     * @param request
     * @param fileSize
     * @param downloadedSize
     */
    void postDownloadProgress(Request<?> request, long fileSize,
                              long downloadedSize);

    /**
     * 取消下载事件
     * 
     * @param request
     */
    void postCancel(Request<?> request);
}
