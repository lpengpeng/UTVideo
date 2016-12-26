package cn.utsoft.commons.videoplayer;

import android.app.Application;

//import com.squareup.leakcanary.LeakCanary;

/**
 * Create by 李俊鹏 on 2016/12/20 20:49
 * Function：
 * Desc：
 */

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        //if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            //return;
        //}
        //LeakCanary.install(this);
        //UTVideoType.enableMediaCodec();
        //UTVideoManager.instance().setVideoType(this, UTVideoType.IJKEXOPLAYER);
    }
}
