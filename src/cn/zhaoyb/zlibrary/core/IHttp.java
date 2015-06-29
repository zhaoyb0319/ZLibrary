package cn.zhaoyb.zlibrary.core;


/**
 * 
 * 一个http接口协议
 * 
 * @author zhaoyb (http://www.zhaoyb.cn)
 *
 */
public interface IHttp {
	
	void finish(Request<?> request);
	IDelivery getDelivery();
	<T> Request<T> add(Request<T> request);
}
