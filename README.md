ZLibrary简介
===================================
ZLibrary，是一个android的框架。同时封装了android中的Bitmap与Http操作的框架，使其更加简单易用；<br/>
http是基于vollery而来，通过封装达到简化Android应用级开发，最终实现快速而又安全的开发APP。<br/>
Bitmap借鉴了ImageLoader的思想，使用LRU算法处理缓存。

使用方法
===================================
  1，可在自身项目中 Add-library<br/>
  2，将项目导出为jar文件直接复制到自身项目的libs中<br/>
  3，可去ZLibraryExample中查看相应的使用方法<br/>
  4，框架API文档：[http://www.zhaoyb.cn/zlibrary](http://www.zhaoyb.cn/zlibrary) 注:需要在AndroidManifest.xml 中声明如下权限<br/>
### 
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    
项目结构
===================================
包为cn.zhaoyb.zlibrary <br/>
### 
    /bitmap 为图片相关操作 <br/>
    /http   网络相关操作 <br/>
    /util   工具类-> file,preferences,配置,屏幕相关等 <br/>
    /core   提取了接口和抽象类等相关操作 <br/>

    ZBitmap 为封装好的图片加载类,具体使用参考 ZLibraryExample的BitmapActivity <br/>
    ZHttp   为封装好的http加载类,具体使用参考 ZLibraryExample的HttpActivity <br/>

关于
===================================
  QQ群：47086070(开发者群)<br/>
  个人站点：[http://www.zhaoyb.cn](http://www.zhaoyb.cn)





    
