package cn.zhaoyb.zlibrary.utils;


import android.util.Log;

/**
 * 
 * 应用程序的Log管理
 * 
 * @author zhaoyb (http://www.zhaoyb.cn)
 *
 */
public final class ZLoger {
	
	/** log输出的tag*/
	private static final String DEBUG_TIP = "ZLoger";
	/** 是否处理调试模式*/
    public static boolean IS_DEBUG = true;
    /** 是否显示activity的状态*/
    public static boolean SHOW_ACTIVITY_STATE = true;

    public static final void openDebutLog(boolean enable) {
        IS_DEBUG = enable;
    }

    public static final void openActivityState(boolean enable) {
        SHOW_ACTIVITY_STATE = enable;
    }

    public static final void debug(String msg) {
        if (!IS_DEBUG) return;
        Log.d(DEBUG_TIP, msg);
    }

    public static final void log(String packName, String state) {
        debugLog(packName, state);
    }

    public static final void debug(String msg, Throwable tr) {
        if (!IS_DEBUG) return;
        Log.d(DEBUG_TIP, msg, tr);
    }

    public static final void state(String packName, String state) {
        if (!SHOW_ACTIVITY_STATE) return;
        Log.d("activity_state", packName + state);
        
    }

    public static final void debugLog(String packName, String state) {
        if (!IS_DEBUG) return;
        Log.d(DEBUG_TIP, packName + state);
    }

    public static final void exception(Exception e) {
        if (!IS_DEBUG) return;
        e.printStackTrace();
    }

    public static final void debug(String msg, Object... format) {
        debug(String.format(msg, format));
    }
}
