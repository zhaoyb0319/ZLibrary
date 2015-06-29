package cn.zhaoyb.zlibrary.http;

import java.util.concurrent.Executor;

import cn.zhaoyb.zlibrary.core.IDelivery;
import cn.zhaoyb.zlibrary.core.Request;
import cn.zhaoyb.zlibrary.core.Response;

import android.os.Handler;

/**
 * Http响应的分发器，这里用于把异步线程中的响应分发到UI线程中执行
 */
public class DeliveryExecutor implements IDelivery {

    private final Executor mResponsePoster;

    public DeliveryExecutor(final Handler handler) {
        mResponsePoster = new Executor() {
            @Override
            public void execute(Runnable command) {
                handler.post(command);
            }
        };
    }

    public DeliveryExecutor(Executor executor) {
        mResponsePoster = executor;
    }

    @Override
    public void postResponse(Request<?> request, Response<?> response) {
        postResponse(request, response, null);
    }

    /**
     * 当有中介响应的时候，会被调用，首先返回中介响应，并执行runnable(实际就是再去请求网络)
     * 所谓中介响应：当本地有一个未过期缓存的时候会优先返回一个缓存，但如果这个缓存又是需要刷新的时候，会再次去请求网络，
     * 那么之前返回的那个有效但需要刷新的就是中介响应
     */
    @Override
    public void postResponse(Request<?> request, Response<?> response,
            Runnable runnable) {
        request.markDelivered();
        mResponsePoster.execute(new ResponseDeliveryRunnable(request, response,
                runnable));
    }

    @Override
    public void postError(Request<?> request, HttpException error) {
        Response<?> response = Response.error(error);
        mResponsePoster.execute(new ResponseDeliveryRunnable(request, response,
                null));
    }

    /**
     * 一个Runnable，将网络请求响应分发到UI线程中
     */
    @SuppressWarnings("rawtypes")
    private class ResponseDeliveryRunnable implements Runnable {
        private final Request mRequest;
        private final Response mResponse;
        private final Runnable mRunnable;

        public ResponseDeliveryRunnable(Request request, Response response,
                Runnable runnable) {
            mRequest = request;
            mResponse = response;
            mRunnable = runnable;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            if (mRequest.isCanceled()) {
                mRequest.finish("request已经取消，在分发时finish");
                return;
            }

            if (mResponse.isSuccess()) {
                mRequest.deliverResponse(mResponse.headers, mResponse.result);
            } else {
                mRequest.deliverError(mResponse.error);
            }
            mRequest.requestFinish();
            mRequest.finish("done");
            if (mRunnable != null) { // 执行参数runnable
                mRunnable.run();
            }
        }
    }

    @Override
    public void postDownloadProgress(Request<?> request, long fileSize,
            long downloadedSize) {
        request.getCallback().onLoading(fileSize, downloadedSize);
    }

    @Override
    public void postCancel(Request<?> request) {}
}
