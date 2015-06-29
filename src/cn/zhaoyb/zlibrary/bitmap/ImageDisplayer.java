package cn.zhaoyb.zlibrary.bitmap;

import java.util.HashMap;
import java.util.LinkedList;

import cn.zhaoyb.zlibrary.ZHttp;

import cn.zhaoyb.zlibrary.core.BitmapCallBack;
import cn.zhaoyb.zlibrary.core.IImageCache;
import cn.zhaoyb.zlibrary.core.Request;

import cn.zhaoyb.zlibrary.http.HttpConfig;
import cn.zhaoyb.zlibrary.http.HttpException;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;

/**
 * 
 * 图片显示器
 * 
 * @author zhaoyb (http://www.zhaoyb.cn)
 *
 */
public class ImageDisplayer {

	/** 使用ZHttp的线程池执行队列去加载图片 */
	private final ZHttp zHttp;

	/** 内存缓存器*/
	private final IImageCache mMemoryCache;
	/** 为了防止网速很快的时候速度过快而造成先显示加载中图片，然后瞬间显示网络图片的闪烁问题*/
	private final int mResponseDelayMs = 100;

	private Runnable mRunnable;
	/** 获取主线程操作句柄*/
	private final Handler mHandler = new Handler(Looper.getMainLooper());

	/** 正在请求的事件 */
	private final HashMap<String, ImageRequestEven> mRequestsMap = new HashMap<String, ImageRequestEven>();
	/** 已经请求完成，待处理的事件*/
	private final HashMap<String, ImageRequestEven> mResponsesMap = new HashMap<String, ImageRequestEven>();

    /**
     * 创建一个图片显示器
     * 
     * @param bitmapConfig
     */
    public ImageDisplayer(BitmapConfig bitmapConfig) {
    	// 网络加载实例
		zHttp = new ZHttp(new HttpConfig(bitmapConfig.mCache,
				bitmapConfig.cacheTime));
		// 图片内存缓存容器
        mMemoryCache = bitmapConfig.mMemoryCache;
    }

    /**
     * 判断指定图片是否已经被缓存
     * 
     * @param requestUrl 图片地址
     * @return
     */
    public boolean isCached(String requestUrl) {
        throwIfNotOnMainThread();
        return mMemoryCache.getBitmap(requestUrl) != null;
    }

    /**
     * 加载一张图片
     * 
     * @param requestUrl 图片地址
     * @param maxWidth 图片最大宽度(如果网络图片大于这个宽度则缩放至这个大小)
     * @param maxHeight 图片最大高度
     * @param callback
     * @return
     */
    public ImageBale get(String requestUrl, int maxWidth, int maxHeight,
            BitmapCallBack callback) {
        throwIfNotOnMainThread();
        callback.onPreStart();

        // 先从缓存中获取指定地址的数据
        Bitmap cachedBitmap = mMemoryCache.getBitmap(requestUrl);
        if (cachedBitmap != null) {
            ImageBale container = new ImageBale(cachedBitmap, requestUrl, null);
            callback.onSuccess(cachedBitmap);
            callback.onFinish();
            return container;
        } else {
            // 开始加载网络图片的标志
            callback.onFromHttp();
        }

        
        ImageBale imageBale = new ImageBale(null, requestUrl, callback);
        ImageRequestEven request = mRequestsMap.get(requestUrl);
        if (request != null) {
            request.addImageBale(imageBale);
            return imageBale;
        }

        // 创建一个网络请求,从网络获取数据,并添加到请求对列中
        Request<Bitmap> newRequest = makeImageRequest(requestUrl, maxWidth,
                maxHeight);
        newRequest.setConfig(zHttp.getConfig());
        zHttp.doRequest(newRequest);
        mRequestsMap.put(requestUrl,new ImageRequestEven(newRequest, imageBale));
        return imageBale;
    }

    /**
     * 创建一个网络请求
     */
    protected Request<Bitmap> makeImageRequest(final String requestUrl,
            int maxWidth, int maxHeight) {
        return new ImageRequest(requestUrl, maxWidth, maxHeight,
                new BitmapCallBack() {
                    @Override
                    public void onSuccess(Bitmap t) {
                        super.onSuccess(t);
                        onGetImageSuccess(requestUrl, t);
                    }

                    @Override
                    public void onFailure(int errorNo, String strMsg) {
                        super.onFailure(errorNo, strMsg);
                        onGetImageError(requestUrl, new HttpException(strMsg));
                    }
                });
    }

    /**
     * 从网络获取bitmap成功时调用
     * 
     * @param url 缓存key
     * @param bitmap 获取到的bitmap
     */
    protected void onGetImageSuccess(String url, Bitmap bitmap) {
        mMemoryCache.putBitmap(url, bitmap);
        // 从正在请求的列表中移除这个已完成的请求
        ImageRequestEven request = mRequestsMap.remove(url);

        if (request != null) {
            request.mBitmapRespond = bitmap;
            batchResponse(url, request);
        }
    }

    /**
     * 从网络获取bitmap失败时调用
     * 
     * @param url 缓存key
     * @param error 失败原因
     */
    protected void onGetImageError(String url, HttpException error) {
        // 从正在请求的列表中移除这个已完成的请求
        ImageRequestEven request = mRequestsMap.remove(url);
        if (request != null) {
            request.setError(error);
            batchResponse(url, request);
        }
    }

    /**************************************************************************/

    /**
     * 对一个图片的封装，包含了这张图片所需要携带的信息
     */
    public class ImageBale {
        private Bitmap mBitmap;
        private final String mRequestUrl;
        private final BitmapCallBack mCallback;

        public ImageBale(Bitmap bitmap, String requestUrl,
                BitmapCallBack callback) {
            mBitmap = bitmap;
            mRequestUrl = requestUrl;
            mCallback = callback;
        }

        public void cancelRequest() {
            if (mCallback == null) {
                return;
            }

            ImageRequestEven request = mRequestsMap.get(mRequestUrl);
            if (request != null) {
                boolean canceled = request.removeBale(this);
                if (canceled) {
                    mRequestsMap.remove(mRequestUrl);
                }
            } else {
                request = mResponsesMap.get(mRequestUrl);
                if (request != null) {
                    request.removeBale(this);
                    if (request.mImageBales.size() == 0) {
                        mResponsesMap.remove(mRequestUrl);
                    }
                }
            }
        }

        public Bitmap getBitmap() {
            return mBitmap;
        }

        public String getRequestUrl() {
            return mRequestUrl;
        }
    }

    /**
     * 图片从网络请求并获取到相应的事件
     */
    private class ImageRequestEven {
        private final Request<?> mRequest;
        private Bitmap mBitmapRespond;
        private HttpException mError;
        private final LinkedList<ImageBale> mImageBales = new LinkedList<ImageBale>();

        public ImageRequestEven(Request<?> request, ImageBale imageBale) {
            mRequest = request;
            mImageBales.add(imageBale);
        }

        public void setError(HttpException error) {
            mError = error;
        }

        public HttpException getError() {
            return mError;
        }

        public void addImageBale(ImageBale imageBale) {
            mImageBales.add(imageBale);
        }

        public boolean removeBale(ImageBale imageBale) {
            mImageBales.remove(imageBale);
            if (mImageBales.size() == 0) {
                mRequest.cancel();
                return true;
            }
            return false;
        }
    }

    /**************************************************************************/

    /**
     * 分发这次ImageRequest事件的结果
     */
    private void batchResponse(String url, final ImageRequestEven request) {
        mResponsesMap.put(url, request);
        if (mRunnable == null) {
            mRunnable = new Runnable() {
                @Override
                public void run() {
                    for (ImageRequestEven even : mResponsesMap.values()) {
                        for (ImageBale imageBale : even.mImageBales) {
                            if (imageBale.mCallback == null) {
                                continue;
                            }
                            if (even.getError() == null) {
                                imageBale.mBitmap = even.mBitmapRespond;
                                imageBale.mCallback
                                        .onSuccess(imageBale.mBitmap);
                            } else {
                                imageBale.mCallback.onFailure(-1, even.getError().getMessage());
                            }
                            imageBale.mCallback.onFinish();
                        }
                    }
                    mResponsesMap.clear();
                    mRunnable = null;
                }

            };
            mHandler.postDelayed(mRunnable, mResponseDelayMs);
        }
    }

    /**
     * 验证 当前进程的looper对象是不是主线程的Looper对象
     * 
     */
    private void throwIfNotOnMainThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalStateException(
                    "ImageLoader must be invoked from the main thread.");
        }
    }

    /**
     * 取消一个加载请求
     * 
     * @param url
     */
    public void cancle(String url) {
        zHttp.cancel(url);
    }
}
