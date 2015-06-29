package cn.zhaoyb.zlibrary.bitmap;

import java.util.Map;

import cn.zhaoyb.zlibrary.core.BitmapCallBack;
import cn.zhaoyb.zlibrary.core.Request;
import cn.zhaoyb.zlibrary.core.Response;
import cn.zhaoyb.zlibrary.core.ZCallBack;
import cn.zhaoyb.zlibrary.http.HttpException;
import cn.zhaoyb.zlibrary.http.HttpHeaderParser;
import cn.zhaoyb.zlibrary.http.NetworkResponse;
import cn.zhaoyb.zlibrary.utils.ZLoger;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * 
 * 网络图片加载
 * 
 * @author zhaoyb (http://www.zhaoyb.cn)
 *
 */
public class ImageRequest extends Request<Bitmap> {

	/** 图片的最大宽和高*/
    private final int mMaxWidth;
    private final int mMaxHeight;
    /** 用来保证当前对象只有一个线程在访问 */
    private static final Object sDecodeLock = new Object();

    public ImageRequest(String url, int maxWidth, int maxHeight,
            ZCallBack callback) {
        super(HttpMethod.GET, url, callback);
        mMaxWidth = maxWidth;
        mMaxHeight = maxHeight;
    }

    @Override
    public Priority getPriority() {
        return Priority.LOW;
    }

    @Override
    public Response<Bitmap> parseNetworkResponse(NetworkResponse response) {
        synchronized (sDecodeLock) {
            try {
                return doParse(response);
            } catch (OutOfMemoryError e) {
                ZLoger.debug("Caught OOM for %d byte image, url=%s",
                        response.data.length, getUrl());
                return Response.error(new HttpException(e));
            }
        }
    }

    /**
     * @param response
     * @return
     */
    private Response<Bitmap> doParse(NetworkResponse response) {
        byte[] data = response.data;
        BitmapFactory.Options option = new BitmapFactory.Options();
        Bitmap bitmap = null;
        if (mMaxWidth == 0 && mMaxHeight == 0) {
            bitmap = BitmapFactory
                    .decodeByteArray(data, 0, data.length, option);
        } else {
            option.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, option);
            int actualWidth = option.outWidth;
            int actualHeight = option.outHeight;

            // 计算出图片应该显示的宽高
            int desiredWidth = BitmapHelper.getResizedDimension(mMaxWidth, mMaxHeight,
                    actualWidth, actualHeight);
            int desiredHeight = BitmapHelper.getResizedDimension(mMaxHeight, mMaxWidth,
                    actualHeight, actualWidth);

            option.inJustDecodeBounds = false;
            option.inSampleSize = BitmapHelper.findBestSampleSize(actualWidth, actualHeight,
                    desiredWidth, desiredHeight);
            Bitmap tempBitmap = BitmapFactory.decodeByteArray(data, 0,
                    data.length, option);

            // 做缩放
            if (tempBitmap != null
                    && (tempBitmap.getWidth() > desiredWidth || tempBitmap
                            .getHeight() > desiredHeight)) {
                bitmap = Bitmap.createScaledBitmap(tempBitmap, desiredWidth,
                        desiredHeight, true);
                tempBitmap.recycle();
            } else {
                bitmap = tempBitmap;
            }
        }
        if (bitmap == null) {
            return Response.error(new HttpException(response));
        } else {
            Response<Bitmap> b = Response.success(bitmap, response.headers,
                    HttpHeaderParser.parseCacheHeaders(mConfig, response));
            return b;
        }
    }

    @Override
    public void deliverResponse(Map<String, String> header, Bitmap response) {
        if (mCallback == null) return;
        ((BitmapCallBack)mCallback).onSuccess(response);
    }
}
