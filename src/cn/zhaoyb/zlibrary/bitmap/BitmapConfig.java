package cn.zhaoyb.zlibrary.bitmap;

import cn.zhaoyb.zlibrary.core.ICache;
import cn.zhaoyb.zlibrary.core.IImageCache;

import cn.zhaoyb.zlibrary.http.DiskCache;

import cn.zhaoyb.zlibrary.utils.ZLoger;
import cn.zhaoyb.zlibrary.utils.FileUtils;

/**
 * 
 * Bitmap配置器
 * 1,图片存储路径
 * 2,磁盘和内存缓存控制
 * 3,缓存时间控制
 * 
 * @author zhaoyb (http://www.zhaoyb.cn)
 *
 */
public class BitmapConfig {

	/** 是否开启调试*/
	public boolean isDEBUG = ZLoger.IS_DEBUG;
    /** 图片存储路径*/
	private static final String IMAGE_CACHE_PATH = "ZLibrary/image";
    /** 磁盘缓存器 **/
    public ICache mCache;
    /** 内存缓存器*/
    public IImageCache mMemoryCache;
    /** 图片缓存时间,单位为分钟(1年)*/
    public int cacheTime = 525600;

    public BitmapConfig() {
        if (mCache != null) return;
        // 使用默认磁盘大小10M存储图片
        mCache = new DiskCache(FileUtils.getSaveFolder(IMAGE_CACHE_PATH));
        if (mMemoryCache != null) return;
        mMemoryCache = new BitmapMemoryCache();
    }
}
