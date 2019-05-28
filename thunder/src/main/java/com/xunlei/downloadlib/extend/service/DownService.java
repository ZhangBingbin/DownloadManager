package com.xunlei.downloadlib.extend.service;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.annotation.Nullable;
import com.blankj.utilcode.util.LogUtils;
import com.xunlei.downloadlib.extend.thread.DownLoadingTask;

public class DownService extends Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtils.eTag("ZBB","onStartCommand");
        new DownLoadingTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        return super.onStartCommand(intent, flags, startId);
    }
}
