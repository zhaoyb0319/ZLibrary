package cn.zhaoyb.zlibrary.bitmap;

import java.io.ByteArrayOutputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

/**
 * 
 * Bitmap操作助手，包含了图片大小压缩、缩放、旋转
 * 
 * @author zhaoyb (http://www.zhaoyb.cn)
 *
 */
public class BitmapHelper {

    /**
     * 图片压缩处理（使用Options的方法）
     * 
     * 首先你要将Options的inJustDecodeBounds属性设置为true，BitmapFactory.decode一次图片 。
     * 然后将Options连同期望的宽度和高度一起传递到到本方法中。
     * 之后再使用本方法的返回值做参数调用BitmapFactory.decode创建图片。
     * 
     * BitmapFactory创建bitmap会尝试为已经构建的bitmap分配内存,
     * 这时就会很容易导致OOM出现。为此每一种创建方法都提供了一个可选的Options参数,
     * 将这个参数的inJustDecodeBounds属性设置为true就可以让解析方法禁止为bitmap分配内存,
     * 返回值也不再是一个Bitmap对象， 而是null。虽然Bitmap是null了，但是Options的outWidth、
     * outHeight和outMimeType属性都会被赋值。
     * 
     * @param reqWidth 目标宽度,这里的宽高只是阀值，实际显示的图片将小于等于这个值
     * @param reqHeight 目标高度,这里的宽高只是阀值，实际显示的图片将小于等于这个值
     */
    public static BitmapFactory.Options calculateInSampleSize(
            final BitmapFactory.Options options, final int reqWidth,
            final int reqHeight) {
        // 源图片的高度和宽度
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            // 计算出实际宽高和目标宽高的比率
            final int heightRatio = Math.round((float) height
                    / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            // 选择宽和高中最小的比率作为inSampleSize的值，这样可以保证最终图片的宽和高
            // 一定都会大于等于目标的宽和高。
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        // 设置压缩比例
        options.inSampleSize = inSampleSize;
        options.inJustDecodeBounds = false;
        return options;
    }

    /**
     * 图片压缩方法：（使用compress的方法）
     * 
     * bitmap实际并没有被回收，如果你不需要，请手动置空 
     * 如果bitmap本身的大小小于maxSize，则不作处理
     * 
     * @param bitmap 要压缩的图片
     * @param maxSize 压缩后的大小，单位kb
     */
    public static Bitmap imageZoom(Bitmap bitmap, double maxSize) {
        // 将bitmap放至数组中，意在获得bitmap的大小（与实际读取的原文件要大）
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // 格式、质量、输出流
        bitmap.compress(Bitmap.CompressFormat.PNG, 70, baos);
        byte[] b = baos.toByteArray();
        // 将字节换成KB
        double mid = b.length / 1024;
        // 获取bitmap大小 是允许最大大小的多少倍
        double i = maxSize <= 0 ? 0 : mid / maxSize;
        // 判断bitmap占用空间是否大于允许最大空间 如果大于则压缩 小于则不压缩
        if (i > 1) {
            // 缩放图片 此处用到平方根 将宽带和高度压缩掉对应的平方根倍
            // （保持宽高不变，缩放后也达到了最大占用空间的大小）
            bitmap = scaleWithWH(bitmap, bitmap.getWidth() / Math.sqrt(i),
                    bitmap.getHeight() / Math.sqrt(i));
        }
        return bitmap;
    }

    /***
     * 图片的缩放方法,如果参数宽高为0,则不处理
     * 
     * src实际并没有被回收，如果你不需要，请手动置空
     * 
     * @param src 源图片资源
     * @param w 缩放后宽度
     * @param h 缩放后高度
     */
    public static Bitmap scaleWithWH(Bitmap src, double w, double h) {
        if (w == 0 || h == 0 || src == null) {
            return src;
        } else {
            // 记录src的宽高
            int width = src.getWidth();
            int height = src.getHeight();
            // 创建一个matrix容器
            Matrix matrix = new Matrix();
            // 计算缩放比例
            float scaleWidth = (float) (w / width);
            float scaleHeight = (float) (h / height);
            // 开始缩放
            matrix.postScale(scaleWidth, scaleHeight);
            // 创建缩放后的图片
            return Bitmap.createBitmap(src, 0, 0, width, height, matrix, true);
        }
    }

    /**
     * 图片的缩放方法
     * 
     * src实际并没有被回收，如果你不需要，请手动置空
     * 
     * @param src 源图片资源
     * @param scaleMatrix 缩放规则
     */
    public static Bitmap scaleWithMatrix(Bitmap src, Matrix scaleMatrix) {
        return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(),
                scaleMatrix, true);
    }

    /**
     * 图片的缩放方法
     * 
     * src实际并没有被回收，如果你不需要，请手动置空
     * 
     * @param src 源图片资源
     * @param scaleX 横向缩放比例
     * @param scaleY 纵向缩放比例
     */
    public static Bitmap scaleWithXY(Bitmap src, float scaleX, float scaleY) {
        Matrix matrix = new Matrix();
        matrix.postScale(scaleX, scaleY);
        return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(),
                matrix, true);
    }

    /**
     * 图片的缩放方法
     * 
     * src实际并没有被回收，如果你不需要，请手动置空
     * 
     * @param src 源图片资源
     * @param scaleXY 缩放比例
     */
    public static Bitmap scaleWithXY(Bitmap src, float scaleXY) {
        return scaleWithXY(src, scaleXY, scaleXY);
    }

    /**
     * 旋转图片
     * 
     * bitmap实际并没有被回收，如果你不需要，请手动置空
     * 
     * @param angle 旋转角度
     * @param bitmap 要旋转的图片
     * @return 旋转后的图片
     */
    public static Bitmap rotate(int angle, Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                bitmap.getHeight(), matrix, true);
    }

    /**
     * 回收一个未被回收的Bitmap
     * 
     * @param bitmap
     */
    public static void doRecycledIfNot(Bitmap bitmap) {
        if (bitmap.isRecycled()) return;
        bitmap.recycle();
    }

    /**
     * 寻找图片的最优尺寸
     * @param actualWidth
     * @param actualHeight
     * @param desiredWidth
     * @param desiredHeight
     * @return
     */
    public static int findBestSampleSize(int actualWidth, int actualHeight,
            int desiredWidth, int desiredHeight) {
        double wr = (double) actualWidth / desiredWidth;
        double hr = (double) actualHeight / desiredHeight;
        double ratio = Math.min(wr, hr);
        float n = 1.0f;
        while ((n * 2) <= ratio) {
            n *= 2;
        }
        return (int) n;
    }

    /**
     * 框架会自动将大于设定值的bitmap转换成设定值，所以需要这个方法来判断应该显示默认大小或者是设定值大小。
     * 本方法会根据maxPrimary与actualPrimary比较来判断，如果无法判断则会根据辅助值判断，辅助值一般是主要值对应的。
     * 比如宽为主值则高为辅值
     * 
     * @param maxPrimary 需要判断的值，用作主要判断
     * @param maxSecondary 需要判断的值，用作辅助判断
     * @param actualPrimary 真实宽度
     * @param actualSecondary 真实高度
     * @return 获取图片需要显示的大小
     */
    public static int getResizedDimension(int maxPrimary, int maxSecondary,
            int actualPrimary, int actualSecondary) {
        if (maxPrimary == 0 && maxSecondary == 0) {
            return actualPrimary;
        }
        if (maxPrimary == 0) {
            double ratio = (double) maxSecondary / (double) actualSecondary;
            return (int) (actualPrimary * ratio);
        }

        if (maxSecondary == 0) {
            return maxPrimary;
        }

        double ratio = (double) actualSecondary / (double) actualPrimary;
        int resized = maxPrimary;
        if (resized * ratio > maxSecondary) {
            resized = (int) (maxSecondary / ratio);
        }
        return resized;
    }
}
