package cn.zhaoyb.zlibrary.http;

/**
 * 整个框架异常的基类
 * 
 */
@SuppressWarnings("serial")
public class HttpException extends Exception {

    public final NetworkResponse networkResponse;

    public HttpException() {
        networkResponse = null;
    }

    public HttpException(NetworkResponse response) {
        networkResponse = response;
    }

    public HttpException(String exceptionMessage) {
        super(exceptionMessage);
        networkResponse = null;
    }

    public HttpException(String exceptionMessage, NetworkResponse response) {
        super(exceptionMessage);
        networkResponse = response;
    }

    public HttpException(String exceptionMessage, Throwable reason) {
        super(exceptionMessage, reason);
        networkResponse = null;
    }

    public HttpException(Throwable cause) {
        super(cause);
        networkResponse = null;
    }
}
