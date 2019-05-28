package com.xunlei.downloadlib.extend.thread;

import android.os.AsyncTask;
import android.os.SystemClock;
import com.blankj.utilcode.util.LogUtils;
import com.xunlei.downloadlib.bean.DownloadInfo;
import com.xunlei.downloadlib.extend.TorrentUpdate;
import com.xunlei.downloadlib.extend.utils.DownUtil;

import java.util.List;

public class DownLoadingTask extends AsyncTask<Void, List<DownloadInfo>, List<DownloadInfo>> {

    public DownLoadingTask() {
    }

    @Override
    protected List<DownloadInfo> doInBackground(Void... objects) {

        LogUtils.eTag("ZBB","doInBackground");

        while (DownUtil.getInstance().isIsLoopDown()) {
            LogUtils.eTag("ZBB","isIsLoopDown");
            TorrentUpdate.Companion.getINSTANCE().updateItem();
            SystemClock.sleep(1000);
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(List<DownloadInfo>... values) {
        super.onProgressUpdate(values);
    }

    @Override
    protected void onPostExecute(List<DownloadInfo> downloadTaskEntities) {
        super.onPostExecute(downloadTaskEntities);
    }
}
