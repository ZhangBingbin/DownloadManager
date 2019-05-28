package com.xunlei.downloadlib.extend.utils;


import com.xunlei.downloadlib.bean.DownloadInfo;
import com.xunlei.downloadlib.status.DownloadStatus;

public class DownUtil {
    private static DownUtil downUtil = null;
    private static boolean isLoopDown = true;

    public DownUtil() {

    }

    public static synchronized DownUtil getInstance() {
        if (downUtil == null) {
            downUtil = new DownUtil();
        }
        return downUtil;
    }

    public static boolean isDownSuccess(DownloadInfo task) {
        long fileSize = task.getTotalSize();
        if (task.getStatus() == DownloadStatus.COMPLETE.getValue() ||
                (fileSize > 0 && task.getCurrentSize() > 0 && fileSize <= task.getCurrentSize())) {
            return true;
        }
        return false;
    }

    public boolean isIsLoopDown() {
        return isLoopDown;
    }

    public void setIsLoopDown(boolean isLoopDown) {
        DownUtil.isLoopDown = isLoopDown;
    }
}
