package cn.zhaoyb.zlibrary.bitmap;

import cn.zhaoyb.zlibrary.core.IImageCache;
import cn.zhaoyb.zlibrary.core.LruCache;
import cn.zhaoyb.zlibrary.utils.SystemTool;
import android.graphics.Bitmap;

/**
 * 
 * 使用lru算法的Bitmap内存缓存池
 * 
 * @author zhaoyb (http://www.zhaoyb.cn)
 *
 */
public final class BitmapMemoryCache implements IImageCache {

    private LruCache<String, Bitmap> cache;

    public BitmapMemoryCache() {
        int maxMemory = (int) (Runtime.getRuntime().maxMemory());
        init(maxMemory / 8);
    }

    /**
     * @param maxSize 使用内存缓存的内存大小，单位：kb
     */
    public BitmapMemoryCache(int maxSize) {
        init(maxSize);
    }

    /**
     * @param maxSize 使用内存缓存的内存大小，单位：kb
     */
    private void init(int maxSize) {
		cache = new LruCache<String, Bitmap>(maxSize) {
			@Override
			protected int sizeOf(String key, Bitmap value) {
				super.sizeOf(key, value);
				return SystemTool.getSDKVersion() >= 12 ? value.getByteCount()
						: value.getRowBytes() * value.getHeight();
			}
		};
    }

    public void remove(String key) {
        cache.remove(key);
    }

    public void removeAll() {
        cache.removeAll();
    }

    /**
     * @param url 图片的地址
     * @return
     */
    @Override
    public Bitmap getBitmap(String url) {
        return cache.get(url);
    }

    /**
     * @param url 图片的地址
     * @param bitmap 要缓存的bitmap
     */
    @Override
    public void putBitmap(String url, Bitmap bitmap) {
        if (getBitmap(url) != null) return;
        cache.put(url, bitmap);
    }
}
