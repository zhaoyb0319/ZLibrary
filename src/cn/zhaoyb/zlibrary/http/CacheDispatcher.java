package cn.zhaoyb.zlibrary.http;

import java.util.concurrent.BlockingQueue;

import cn.zhaoyb.zlibrary.core.ICache;
import cn.zhaoyb.zlibrary.core.IDelivery;
import cn.zhaoyb.zlibrary.core.Request;
import cn.zhaoyb.zlibrary.core.Response;
import cn.zhaoyb.zlibrary.utils.ZLoger;

import android.os.Process;

/**
 * 缓存调度器
 * 
 * 工作描述： 缓存逻辑同样也采用责任链模式，
 * 由缓存任务队列CacheQueue，缓存调度器CacheDispatcher，缓存器Cache组成
 * 
 * 调度器不停的从CacheQueue中取request，并把这个request尝试从缓存器中获取缓存响应。
 * 如果缓存器有有效且及时的缓存则直接返回缓存;
 * 如果缓存器有有效但待刷新的有效缓存，则交给分发器去分发一次中介相应，并再去添加到工作队列中执行网络请求获取最新的数据;
 * 如果缓存器中没有有效缓存，则把请求添加到mNetworkQueue工作队列中去执行网络请求;
 * 
 */
public class CacheDispatcher extends Thread {

    private final BlockingQueue<Request<?>> mCacheQueue; // 缓存队列
    private final BlockingQueue<Request<?>> mNetworkQueue; // 用于执行网络请求的工作队列
    private final ICache mCache; // 缓存器
    private final IDelivery mDelivery; // 分发器
    private final HttpConfig mConfig; // 配置器

    private volatile boolean mQuit = false;

    /**
     * 创建分发器(必须手动调用star()方法启动分发任务)
     * 
     * @param cacheQueue 缓存队列
     * @param networkQueue 正在执行的队列
     * @param cache 缓存器对象
     * @param delivery 分发器
     */
    public CacheDispatcher(BlockingQueue<Request<?>> cacheQueue,
            BlockingQueue<Request<?>> networkQueue, ICache cache,
            IDelivery delivery, HttpConfig config) {
        mCacheQueue = cacheQueue;
        mNetworkQueue = networkQueue;
        mCache = cache;
        mDelivery = delivery;
        mConfig = config;
    }

    /**
     * 强制退出
     */
    public void quit() {
        mQuit = true;
        interrupt();
    }

    /**
     * 工作在阻塞态
     */
    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        mCache.initialize();

        while (true) {
            try {
                final Request<?> request = mCacheQueue.take();
                if (request.isCanceled()) {
                    request.finish("cache-discard-canceled");
                    continue;
                }

                ICache.Entry entry = mCache.get(request.getCacheKey());
                if (entry == null) { // 如果没有缓存，去网络请求
                    mNetworkQueue.put(request);
                    continue;
                }

                // 如果缓存过期，去网络请求,图片缓存永久有效
                if (entry.isExpired()) {
                    // && !(request instanceof ImageRequest)
                    request.setCacheEntry(entry);
                    mNetworkQueue.put(request);
                    continue;
                }

                // 从缓存返回数据
                Response<?> response = request
                        .parseNetworkResponse(new NetworkResponse(entry.data,
                                entry.responseHeaders));
                ZLoger.debugLog("CacheDispatcher：", "http resopnd from cache");
                if (mConfig.useDelayCache) {
                    sleep(mConfig.delayTime);
                }
                mDelivery.postResponse(request, response);
            } catch (InterruptedException e) {
                if (mQuit) {
                    return;
                } else {
                    continue;
                }
            }
        }
    }
}
