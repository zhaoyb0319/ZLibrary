package cn.zhaoyb.zlibrary.http;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;

import cn.zhaoyb.zlibrary.core.Request;
import cn.zhaoyb.zlibrary.core.Response;
import cn.zhaoyb.zlibrary.core.ZCallBack;
import cn.zhaoyb.zlibrary.utils.ZLoger;

import android.text.TextUtils;
/**
 * 
 * 文件下载请求
 *
 */
public class FileRequest extends Request<byte[]> {

    private final File mStoreFile;
    private final File mTemporaryFile; // 临时文件

    public FileRequest(String storeFilePath, String url, ZCallBack callback) {
        super(HttpMethod.GET, url, callback);
        mStoreFile = new File(storeFilePath);
        File folder = mStoreFile.getParentFile();
        if (folder != null) {
            folder.mkdirs();
        }
        if (!mStoreFile.exists()) {
            try {
                mStoreFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mTemporaryFile = new File(storeFilePath + ".tmp");
        setShouldCache(false);
    }

    public File getStoreFile() {
        return mStoreFile;
    }

    public File getTemporaryFile() {
        return mTemporaryFile;
    }

    @Override
    public Response<byte[]> parseNetworkResponse(NetworkResponse response) {
        String errorMessage = null;
        if (!isCanceled()) {
            if (mTemporaryFile.canRead() && mTemporaryFile.length() > 0) {
                if (mTemporaryFile.renameTo(mStoreFile)) {
                    return Response.success(response.data, response.headers,
                            HttpHeaderParser.parseCacheHeaders(mConfig,
                                    response));
                } else {
                    errorMessage = "Can't rename the download temporary file!";
                }
            } else {
                errorMessage = "Download temporary file was invalid!";
            }
        }
        if (errorMessage == null) {
            errorMessage = "Request was Canceled!";
        }
        return Response.error(new HttpException(errorMessage));
    }

    @Override
    public Map<String, String> getHeaders() {
        Map<String, String> header = new HashMap<String, String>();
        header.put("Range", "bytes=" + mTemporaryFile.length() + "-");
        header.put("Accept-Encoding", "identity");
        return header;
    }

	public byte[] handleResponse(HttpResponse response) throws IOException,
			HttpException {
        HttpEntity entity = response.getEntity();
        long fileSize = entity.getContentLength();
        if (fileSize <= 0) {
            ZLoger.debug("Response doesn't present Content-Length!");
        }

        long downloadedSize = mTemporaryFile.length();
        boolean isSupportRange = HttpUtils.isSupportRange(response);
        if (isSupportRange) {
            fileSize += downloadedSize;

            String realRangeValue = HttpUtils.getHeader(response,
                    "Content-Range");
            if (!TextUtils.isEmpty(realRangeValue)) {
                String assumeRangeValue = "bytes " + downloadedSize + "-"
                        + (fileSize - 1);
                if (TextUtils.indexOf(realRangeValue, assumeRangeValue) == -1) {
                    throw new IllegalStateException(
                            "The Content-Range Header is invalid Assume["
                                    + assumeRangeValue + "] vs Real["
                                    + realRangeValue + "], "
                                    + "please remove the temporary file ["
                                    + mTemporaryFile + "].");
                }
            }
        }

        if (fileSize > 0 && mStoreFile.length() == fileSize) {
            mStoreFile.renameTo(mTemporaryFile);
            mRequestQueue.getDelivery().postDownloadProgress(this,
                    fileSize, fileSize);
            return null;
        }

        RandomAccessFile tmpFileRaf = new RandomAccessFile(mTemporaryFile, "rw");
        if (isSupportRange) {
            tmpFileRaf.seek(downloadedSize);
        } else {
            tmpFileRaf.setLength(0);
            downloadedSize = 0;
        }

        try {
            InputStream in = entity.getContent();
            if (HttpUtils.isGzipContent(response)
                    && !(in instanceof GZIPInputStream)) {
                in = new GZIPInputStream(in);
            }
            byte[] buffer = new byte[6 * 1024]; // 6K buffer
            int offset;

            while ((offset = in.read(buffer)) != -1) {
                tmpFileRaf.write(buffer, 0, offset);

                downloadedSize += offset;
                mRequestQueue.getDelivery().postDownloadProgress(this,
                        fileSize, downloadedSize);

                if (isCanceled()) {
                    break;
                }
            }
        } finally {
            try {
                if (entity != null)
                    entity.consumeContent();
            } catch (Exception e) {
                ZLoger.debug("Error occured when calling consumingContent");
            }
            tmpFileRaf.close();
        }
        return null;
    }

    @Override
    public Priority getPriority() {
        return Priority.LOW;
    }

    @Override
    public void deliverResponse(Map<String, String> headers, byte[] response) {
        if (mCallback != null) {
            mCallback.onSuccess(headers, response);
        }
    }
}
