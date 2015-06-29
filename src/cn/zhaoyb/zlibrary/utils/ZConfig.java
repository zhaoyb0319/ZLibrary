package cn.zhaoyb.zlibrary.utils;

/**
 * 
 * 配置应用相关信息
 * 
 * @author zhaoyb (http://www.zhaoyb.cn)
 *
 */
public final class ZConfig {

	/** 当前框架版本号*/
    public static final double VERSION = 1.0;

    /** 错误处理广播 */
    public static final String RECEIVER_ERROR = ZConfig.class.getName() + "cn.zhaoyb.zlibrary.error";
    /** 无网络警告广播 */
    public static final String RECEIVER_NOT_NET_WARN = ZConfig.class.getName() + "cn.zhaoyb.zlibrary.notnet";
    /** preference键值对 */
    public static final String SETTING_FILE = "zlibrary_preference";
    public static final String ONLY_WIFI = "only_wifi";
}
