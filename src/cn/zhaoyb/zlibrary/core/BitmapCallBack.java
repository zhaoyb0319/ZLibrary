package cn.zhaoyb.zlibrary.core;

import android.graphics.Bitmap;

/**
 * 
 * Bitmap加载的回调方法
 * 
 * @author zhaoyb (http://www.zhaoyb.cn)
 *
 */
public abstract class BitmapCallBack extends ZCallBack {
	
	/** 载入成功 */
	public void onSuccess(final Bitmap bitmap) {}
	/** 从网络加载图片 */
	public void onFromHttp() {}
}
