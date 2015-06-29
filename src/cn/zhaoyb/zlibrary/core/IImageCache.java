package cn.zhaoyb.zlibrary.core;

import android.graphics.Bitmap;

/**
 * 
 * 一个图片缓存接口协议
 * 
 * @author zhaoyb (http://www.zhaoyb.cn)
 *
 */
public interface IImageCache {
	Bitmap getBitmap(String url);
    void putBitmap(String url, Bitmap bitmap);
}
