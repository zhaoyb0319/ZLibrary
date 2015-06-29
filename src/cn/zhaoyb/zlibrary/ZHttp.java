package cn.zhaoyb.zlibrary;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import cn.zhaoyb.zlibrary.core.ICache;
import cn.zhaoyb.zlibrary.core.IDelivery;
import cn.zhaoyb.zlibrary.core.IHttp;
import cn.zhaoyb.zlibrary.core.Request;
import cn.zhaoyb.zlibrary.core.Request.HttpMethod;
import cn.zhaoyb.zlibrary.core.ZCallBack;
import cn.zhaoyb.zlibrary.http.CacheDispatcher;
import cn.zhaoyb.zlibrary.http.DownloadController;
import cn.zhaoyb.zlibrary.http.DownloadTaskQueue;
import cn.zhaoyb.zlibrary.http.FileRequest;
import cn.zhaoyb.zlibrary.http.HttpConfig;
import cn.zhaoyb.zlibrary.http.FormRequest;
import cn.zhaoyb.zlibrary.http.HttpParams;
import cn.zhaoyb.zlibrary.http.JsonRequest;
import cn.zhaoyb.zlibrary.http.NetworkDispatcher;
import cn.zhaoyb.zlibrary.utils.ZLoger;

/**
 * 本类工作流程： 每当发起一次Request，会对这个Request标记一个唯一值。
 * 并加入当前请求的Set中(保证唯一;方便控制)。
 * 同时判断是否启用缓存，若启用则加入缓存队列，否则加入执行队列。
 * 
 * 整个ZHttp工作流程：采用责任链设计模式，由三部分组成，类似设计可以类比Handle...Looper...MessageQueue
 * 
 * 1、ZHttp负责不停向NetworkQueue(或CacheQueue实际还是NetworkQueue)
 * 2、另一边由TaskThread不停从NetworkQueue中取Request并交给Network执行器
 * 3、Network执行器将执行成功的NetworkResponse返回给TaskThead，并通过Request的定制方法封装成Response，最终交给分发器
 * 分发到主线程并调用ZCallback相应的方法
 * 
 * @author zhaoyb (https://www.zhaoyb.cn/)
 */
public class ZHttp implements IHttp {

    // 请求缓冲区
    private final Map<String, Queue<Request<?>>> mWaitingRequests = new HashMap<String, Queue<Request<?>>>();
    // 请求的序列化生成器
    private final AtomicInteger mSequenceGenerator = new AtomicInteger();
    // 当前正在执行请求的线程集合
    private final Set<Request<?>> mCurrentRequests = new HashSet<Request<?>>();
    // 执行缓存任务的队列.
    private final PriorityBlockingQueue<Request<?>> mCacheQueue = new PriorityBlockingQueue<Request<?>>();
    // 需要执行网络请求的工作队列
    private final PriorityBlockingQueue<Request<?>> mNetworkQueue = new PriorityBlockingQueue<Request<?>>();
    // 请求任务执行池
    private final NetworkDispatcher[] mTaskThreads;
    // 缓存队列调度器
    private CacheDispatcher mCacheDispatcher;
    // 配置器
    private HttpConfig mConfig;

    public ZHttp() {
        this(new HttpConfig());
    }

    public ZHttp(HttpConfig config) {
        this.mConfig = config;
        mConfig.mController.setRequestQueue(this);
        mTaskThreads = new NetworkDispatcher[HttpConfig.NETWORK_POOL_SIZE];
        start();
    }

    /**
     * 发起get请求
     * 
     * @param url 地址
     * @param callback 请求中的回调方法
     */
    public Request<byte[]> get(String url, ZCallBack callback) {
        return get(url, new HttpParams(), callback);
    }

    /**
     * 发起get请求
     * 
     * @param url 地址
     * @param params 参数集
     * @param callback 请求中的回调方法
     */
    public Request<byte[]> get(String url, HttpParams params,
    		ZCallBack callback) {
        return get(url, params, true, callback);
    }

    /**
     * 发起get请求
     * 
     * @param url 地址
     * @param params 参数集
     * @param callback 请求中的回调方法
     * @param useCache 是否缓存本条请求
     */
    public Request<byte[]> get(String url, HttpParams params, boolean useCache,
    		ZCallBack callback) {
        if (params != null) {
            url += params.getUrlParams();
        }
        Request<byte[]> request = new FormRequest(HttpMethod.GET, url, params,
                callback);
        request.setShouldCache(useCache);
        doRequest(request);
        return request;
    }

    /**
     * 发起post请求
     * 
     * @param url 地址
     * @param params 参数集
     * @param callback 请求中的回调方法
     */
    public Request<byte[]> post(String url, HttpParams params,
            ZCallBack callback) {
        return post(url, params, true, callback);
    }

    /**
     * 发起post请求
     * 
     * @param url 地址
     * @param params 参数集
     * @param callback 请求中的回调方法
     * @param useCache 是否缓存本条请求
     */
    public Request<byte[]> post(String url, HttpParams params,
            boolean useCache, ZCallBack callback) {
        Request<byte[]> request = new FormRequest(HttpMethod.POST, url, params,
                callback);
        request.setShouldCache(useCache);
        doRequest(request);
        return request;
    }

    /**
     * 使用JSON传参的post请求
     * 
     * @param url 地址
     * @param params 参数集
     * @param callback 请求中的回调方法
     */
    public Request<byte[]> jsonPost(String url, HttpParams params,
    		ZCallBack callback) {
        return jsonPost(url, params, true, callback);
    }

    /**
     * 使用JSON传参的post请求
     * 
     * @param url 地址
     * @param params 参数集
     * @param callback 请求中的回调方法
     * @param useCache 是否缓存本条请求
     */
    public Request<byte[]> jsonPost(String url, HttpParams params,
            boolean useCache, ZCallBack callback) {
        Request<byte[]> request = new JsonRequest(HttpMethod.POST, url, params,
                callback);
        request.setShouldCache(useCache);
        doRequest(request);
        return request;
    }

    /**
     * 使用JSON传参的get请求
     * 
     * @param url 地址
     * @param params 参数集
     * @param callback 请求中的回调方法
     */
    public Request<byte[]> jsonGet(String url, HttpParams params,
    		ZCallBack callback) {
        Request<byte[]> request = new JsonRequest(HttpMethod.GET, url, params,
                callback);
        doRequest(request);
        return request;
    }

    /**
     * 使用JSON传参的get请求
     * 
     * @param url 地址
     * @param params 参数集
     * @param callback 请求中的回调方法
     * @param useCache 是否缓存本条请求
     */
    public Request<byte[]> jsonGet(String url, HttpParams params,
            boolean useCache, ZCallBack callback) {
        Request<byte[]> request = new JsonRequest(HttpMethod.GET, url, params,
                callback);
        request.setShouldCache(useCache);
        doRequest(request);
        return request;
    }

    /**
     * 下载
     * 
     * @param storeFilePath 文件保存路径。注，必须是一个file路径不能是folder
     * @param url 下载地址
     * @param callback 请求中的回调方法
     */
    public DownloadTaskQueue download(String storeFilePath, String url,
            ZCallBack callback) {
        FileRequest request = new FileRequest(storeFilePath, url, callback);
        mConfig.mController.add(request);
        doRequest(request);
        return mConfig.mController;
    }

    /**
     * 返回下载总控制器
     * 
     * @return
     */
    public DownloadController getDownloadController(String storeFilePath,
            String url) {
        return mConfig.mController.get(storeFilePath, url);
    }

    public void cancleAll() {
        mConfig.mController.clearAll();
    }

    /**
     * 执行一个自定义请求
     * 
     * @param request
     */
    public void doRequest(Request<?> request) {
        request.setConfig(mConfig);
        add(request);
    }

    /**
     * 获取内存缓存数据
     * 
     * @param url 哪条url的缓存
     * @return
     */
    public byte[] getCache(String url) {
        ICache cache = mConfig.mCache;
        cache.initialize();
        ICache.Entry entry = cache.get(url);
        if (entry != null) {
            return entry.data;
        } else {
            return new byte[0];
        }
    }

    /**
     * 只有你确定cache是一个String时才可以使用这个方法，否则还是应该使用getCache(String);
     * 
     * @param url
     * @return
     */
    public String getStringCache(String url) {
        return new String(getCache(url));
    }

    /**
     * 移除一个缓存
     * 
     * @param url 哪条url的缓存
     */
    public void removeCache(String url) {
        mConfig.mCache.remove(url);
    }

    /**
     * 清空缓存
     */
    public void cleanCache() {
        mConfig.mCache.clear();
    }

    public HttpConfig getConfig() {
        return mConfig;
    }

    public void setConfig(HttpConfig config) {
        this.mConfig = config;
    }

    /******************************** core method ****************************************/

    /**
     * 启动队列调度
     */
    private void start() {
        stop();// 首先关闭之前的运行，不管是否存在
        mCacheDispatcher = new CacheDispatcher(mCacheQueue, mNetworkQueue,
                mConfig.mCache, mConfig.mDelivery, mConfig);
        mCacheDispatcher.start();
        // 构建线程池
        for (int i = 0; i < mTaskThreads.length; i++) {
            NetworkDispatcher tasker = new NetworkDispatcher(mNetworkQueue,
                    mConfig.mNetwork, mConfig.mCache, mConfig.mDelivery);
            mTaskThreads[i] = tasker;
            tasker.start();
        }
    }

    /**
     * 停止队列调度
     */
    private void stop() {
        if (mCacheDispatcher != null) {
            mCacheDispatcher.quit();
        }
        for (int i = 0; i < mTaskThreads.length; i++) {
            if (mTaskThreads[i] != null) {
                mTaskThreads[i].quit();
            }
        }
    }

    public void cancel(String url) {
        synchronized (mCurrentRequests) {
            for (Request<?> request : mCurrentRequests) {
                if (url.equals(request.getTag())) {
                    request.cancel();
                }
            }
        }
    }

    /**
     * 取消全部请求
     */
    public void cancelAll() {
        synchronized (mCurrentRequests) {
            for (Request<?> request : mCurrentRequests) {
                request.cancel();
            }
        }
    }

    /**
     * 向请求队列加入一个请求
     * 此处工作模式是这样的：ZHttp可以看做是一个队列类，而本方法不断的向这个队列添加request；另一方面，
     * TaskThread不停的从这个队列中取request并执行。类似的设计可以参考Handle...Looper...MessageQueue的关系
     */
    @Override
    public <T> Request<T> add(Request<T> request) {
        if (request.getCallback() != null) {
            request.getCallback().onPreStart();
        }

        // 标记该请求属于该队列，并将它添加到该组当前的请求。
        request.setRequestQueue(this);
        synchronized (mCurrentRequests) {
            mCurrentRequests.add(request);
        }
        // 设置进程优先序列
        request.setSequence(mSequenceGenerator.incrementAndGet());

        // 如果请求不可缓存，跳过缓存队列，直接进入网络。
        if (!request.shouldCache()) {
            mNetworkQueue.add(request);
            return request;
        }

        // 如果已经在mWaitingRequests中有本请求，则替换
        synchronized (mWaitingRequests) {
            String cacheKey = request.getCacheKey();
            if (mWaitingRequests.containsKey(cacheKey)) {
                // There is already a request in flight. Queue up.
                Queue<Request<?>> stagedRequests = mWaitingRequests
                        .get(cacheKey);
                if (stagedRequests == null) {
                    stagedRequests = new LinkedList<Request<?>>();
                }
                stagedRequests.add(request);
                mWaitingRequests.put(cacheKey, stagedRequests);
                if (HttpConfig.DEBUG) {
                	ZLoger.debug(
                            "Request for cacheKey=%s is in flight, putting on hold.",
                            cacheKey);
                }
            } else {
                mWaitingRequests.put(cacheKey, null);
                mCacheQueue.add(request);
            }
            return request;
        }
    }

    /**
     * 将一个请求标记为已完成
     */
    @Override
    public void finish(Request<?> request) {
        synchronized (mCurrentRequests) {
            mCurrentRequests.remove(request);
        }

        if (request.shouldCache()) {
            synchronized (mWaitingRequests) {
                String cacheKey = request.getCacheKey();
                Queue<Request<?>> waitingRequests = mWaitingRequests
                        .remove(cacheKey);
                if (waitingRequests != null) {
                    if (HttpConfig.DEBUG) {
                        ZLoger.debug(
                                "Releasing %d waiting requests for cacheKey=%s.",
                                waitingRequests.size(), cacheKey);
                    }
                    mCacheQueue.addAll(waitingRequests);
                }
            }
        }
    }

    public void destroy() {
        cancelAll();
        stop();
    }

	@Override
	public IDelivery getDelivery() {
		return mConfig.mDelivery;
	}
}
