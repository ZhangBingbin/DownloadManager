package com.xunlei.downloadlib;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import com.xunlei.downloadlib.parameter.*;

import java.util.concurrent.atomic.AtomicInteger;

public class XLTaskHelper {

    public static void init(Context context) {

        XLDownloadManager instance = XLDownloadManager.getInstance();
        InitParam initParam = new InitParam();
        initParam.mAppKey = "xzNjAwOQ^^yb==aa214316d5e0a63a5b58db24557fa2^e";
        initParam.mAppVersion = "5.21.2.4170";
        initParam.mStatSavePath = context.getFilesDir().getPath();
        initParam.mStatCfgSavePath = context.getFilesDir().getPath();
        initParam.mPermissionLevel = 2;
        instance.init(context, initParam);
        instance.setOSVersion(Build.VERSION.INCREMENTAL);
        instance.setSpeedLimit(-1, -1);
        XLDownloadManager.getInstance().setUserId("");
    }

    private AtomicInteger seq = new AtomicInteger(0);

    private XLTaskHelper() {
    }

    private static volatile XLTaskHelper instance = null;

    public static XLTaskHelper instance() {
        if (instance == null) {
            synchronized (XLTaskHelper.class) {
                if (instance == null) {
                    instance = new XLTaskHelper();
                }
            }

        }
        return instance;
    }


    /**
     * 添加迅雷链接任务 支持thunder:// ftp:// ed2k:// http:// https:// 协议
     *
     * @param url      url
     * @param savePath 下载文件保存路径
     * @param fileName 下载文件名 可以通过 getFileName(url) 获取到,为空默认为getFileName(url)的值
     */
    public synchronized long addThunderTask(String url, String savePath, @Nullable String fileName) {
        if (url.startsWith("thunder://"))
            url = XLDownloadManager.getInstance().parserThunderUrl(url);

        final GetTaskId getTaskId = new GetTaskId();
        if (TextUtils.isEmpty(fileName)) {
            GetFileName getFileName = new GetFileName();
            XLDownloadManager.getInstance().getFileNameFromUrl(url, getFileName);
            fileName = getFileName.getFileName();
        }
        if (url.startsWith("ftp://") || url.startsWith("http://") || url.startsWith("https://")) {
            P2spTaskParam taskParam = new P2spTaskParam();
            taskParam.setCreateMode(1);
            taskParam.setFilePath(savePath);
            taskParam.setFileName(fileName);
            taskParam.setUrl(url);
            taskParam.setSeqId(seq.incrementAndGet());
            taskParam.setCookie("");
            taskParam.setRefUrl("");
            taskParam.setUser("");
            taskParam.setPass("");
            XLDownloadManager.getInstance().createP2spTask(taskParam, getTaskId);
        } else if (url.startsWith("ed2k://")) {
            EmuleTaskParam taskParam = new EmuleTaskParam();
            taskParam.setFilePath(savePath);
            taskParam.setFileName(fileName);
            taskParam.setUrl(url);
            taskParam.setSeqId(seq.incrementAndGet());
            taskParam.setCreateMode(1);
            XLDownloadManager.getInstance().createEmuleTask(taskParam, getTaskId);
        }

        XLDownloadManager.getInstance().setDownloadTaskOrigin(getTaskId.getTaskId(), "out_app/out_app_paste");
        XLDownloadManager.getInstance().setOriginUserAgent(getTaskId.getTaskId(), "AndroidDownloadManager/4.4.4 (Linux; U; Android 4.4.4; Build/KTU84Q)");
        XLDownloadManager.getInstance().startTask(getTaskId.getTaskId(), false);
        XLDownloadManager.getInstance().setTaskLxState(getTaskId.getTaskId(), 0, 1);
        XLDownloadManager.getInstance().startDcdn(getTaskId.getTaskId(), 0, "", "", "");
        return getTaskId.getTaskId();
    }

    /**
     * 通过链接获取文件名
     */
    public synchronized String getFileName(String url) {
        if (url.startsWith("thunder://"))
            url = XLDownloadManager.getInstance().parserThunderUrl(url);
        GetFileName getFileName = new GetFileName();
        XLDownloadManager.getInstance().getFileNameFromUrl(url, getFileName);
        return getFileName.getFileName();
    }

    /**
     * 添加磁力链任务
     *
     * @param url 磁力链接 magnet:? 开头
     */
    public synchronized long addMagnetTask(final String url, final String savePath, String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            final GetFileName getFileName = new GetFileName();
            XLDownloadManager.getInstance().getFileNameFromUrl(url, getFileName);
            fileName = getFileName.getFileName();
        }
        MagnetTaskParam magnetTaskParam = new MagnetTaskParam();
        magnetTaskParam.setFileName(fileName);
        magnetTaskParam.setFilePath(savePath);
        magnetTaskParam.setUrl(url);
        final GetTaskId getTaskId = new GetTaskId();
        XLDownloadManager.getInstance().createBtMagnetTask(magnetTaskParam, getTaskId);

        XLDownloadManager.getInstance().setTaskLxState(getTaskId.getTaskId(), 0, 1);
        XLDownloadManager.getInstance().startDcdn(getTaskId.getTaskId(), 0, "", "", "");
        XLDownloadManager.getInstance().startTask(getTaskId.getTaskId(), false);
        return getTaskId.getTaskId();
    }

    /**
     * 获取种子详情
     */
    public synchronized TorrentInfo getTorrentInfo(String torrentPath) {
        TorrentInfo torrentInfo = new TorrentInfo();
        XLDownloadManager.getInstance().getTorrentInfo(torrentPath, torrentInfo);
        return torrentInfo;
    }

    /**
     * 添加种子下载任务,如果是磁力链需要先通过addMagentTask将种子下载下来
     *
     * @param torrentPath 种子地址
     * @param savePath    保存路径
     */
    public synchronized long addTorrentTask(String torrentPath, String savePath, int[] indexs) {
        TorrentInfo torrentInfo = new TorrentInfo();
        XLDownloadManager.getInstance().getTorrentInfo(torrentPath, torrentInfo);
        TorrentFileInfo[] fileInfos = torrentInfo.mSubFileInfo;
        BtTaskParam taskParam = new BtTaskParam();
        taskParam.setCreateMode(1);
        taskParam.setFilePath(savePath);
        taskParam.setMaxConcurrent(3);
        taskParam.setSeqId(seq.incrementAndGet());
        taskParam.setTorrentPath(torrentPath);
        GetTaskId getTaskId = new GetTaskId();
        XLDownloadManager.getInstance().createBtTask(taskParam, getTaskId);
        if (fileInfos.length > 1 && indexs != null && indexs.length > 0) {
            BtIndexSet btIndexSet = new BtIndexSet(indexs.length);
            int i = 0;
            for (int index : indexs) {
                btIndexSet.mIndexSet[i++] = index;
            }
            XLDownloadManager.getInstance().selectBtSubTask(getTaskId.getTaskId(), btIndexSet);
        }
        XLDownloadManager.getInstance().setTaskLxState(getTaskId.getTaskId(), 0, 1);
//        XLDownloadManager.getInstance().startDcdn(getTaskId.getTaskId(), currentFileInfo.mRealIndex, "", "", "");
        XLDownloadManager.getInstance().startTask(getTaskId.getTaskId(), false);
//        XLDownloadManager.getInstance().setBtPriorSubTask(getTaskId.getTaskId(),currentFileInfo.mRealIndex);
//        XLTaskLocalUrl localUrl = new XLTaskLocalUrl();
//        XLDownloadManager.getInstance().getLocalUrl(savePath+"/" +(TextUtils.isEmpty(currentFileInfo.mSubPath) ? "" : currentFileInfo.mSubPath+"/")+ currentFileInfo.mFileName,localUrl);
//        currentFileInfo.playUrl = localUrl.mStrUrl;
//        currentFileInfo.hash = torrentInfo.mInfoHash;
//        return currentFileInfo;
        return getTaskId.getTaskId();
    }

    /**
     * 获取某个文件的本地proxy url,如果是音视频文件可以实现变下边播
     */
    public synchronized String getLoclUrl(String filePath) {
        XLTaskLocalUrl localUrl = new XLTaskLocalUrl();
        XLDownloadManager.getInstance().getLocalUrl(filePath, localUrl);
        return localUrl.mStrUrl;
    }

    /**
     * 删除一个任务，会把文件也删掉
     */
    public synchronized void deleteTask(long taskId, final String savePath) {
        removeTask(taskId);
        new Handler(Daemon.looper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    new LinuxFileCommand(Runtime.getRuntime()).deleteDirectory(savePath);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 停止任务
     */
    public synchronized void stopTask(long taskId) {
        XLDownloadManager.getInstance().stopTask(taskId);
    }

    /**
     * 删除任务 文件保留
     */
    public synchronized void removeTask(long taskId) {
        XLDownloadManager.getInstance().stopTask(taskId);
        XLDownloadManager.getInstance().releaseTask(taskId);
    }

    /**
     * 开始任务
     */
    public synchronized void startTask(long taskId) {
        XLDownloadManager.getInstance().startTask(taskId, true);
    }

    /**
     * 获取任务详情， 包含当前状态，下载进度，下载速度，文件大小
     * mDownloadSize:已下载大小  mDownloadSpeed:下载速度 mFileSize:文件总大小 mTaskStatus:当前状态，0连接中1下载中 2下载完成 3失败 mAdditionalResDCDNSpeed DCDN加速 速度
     */
    public synchronized XLTaskInfo getTaskInfo(long taskId) {
        XLTaskInfo taskInfo = new XLTaskInfo();
        XLDownloadManager.getInstance().getTaskInfo(taskId, 1, taskInfo);
        return taskInfo;
    }

    /**
     * 获取种子文件子任务的详情
     */
    public synchronized BtSubTaskDetail getBtSubTaskInfo(long taskId, int fileIndex) {
        BtSubTaskDetail subTaskDetail = new BtSubTaskDetail();
        XLDownloadManager.getInstance().getBtSubTaskInfo(taskId, fileIndex, subTaskDetail);
        return subTaskDetail;
    }

    /**
     * 开启dcdn加速
     */
    public synchronized void startDcdn(long taskId, int btFileIndex) {
        XLDownloadManager.getInstance().startDcdn(taskId, btFileIndex, "", "", "");
    }

    /**
     * 停止dcdn加速
     */
    public synchronized void stopDcdn(long taskId, int btFileIndex) {
        XLDownloadManager.getInstance().stopDcdn(taskId, btFileIndex);
    }
}
