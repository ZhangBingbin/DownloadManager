package com.xunlei.downloadlib.util

import android.annotation.SuppressLint
import android.app.Application
import android.content.Intent
import android.os.Environment
import android.text.TextUtils
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.Utils
import com.liulishuo.filedownloader.*
import com.xunlei.downloadlib.XLTaskHelper
import com.xunlei.downloadlib.bean.DownloadInfo
import com.xunlei.downloadlib.bean.TsDownloadInfo
import com.xunlei.downloadlib.database.DownloadDaoManager
import com.xunlei.downloadlib.extend.service.DownService
import com.xunlei.downloadlib.extend.utils.FileTools
import com.xunlei.downloadlib.listener.BaseObserveListener
import com.xunlei.downloadlib.listener.OkHttp3Connection
import com.xunlei.downloadlib.status.DownloadStatus
import com.xunlei.downloadlib.status.VideoType
import io.reactivex.Observable
import io.reactivex.ObservableOnSubscribe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.litepal.LitePal
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.concurrent.atomic.AtomicLong

/**
 * ================================================
 * 框架要求框架中的每个 {@link } 都需要实现此类,以满足规范
 *
 * @function 作用
 * Created by Joe_ZBB on 2019-05-27 10:56
 * ================================================
 */
class DownloadUtil {
    /**
     * 单列
     */
    companion object {
        val INSTANCE: DownloadUtil by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            DownloadUtil()
        }
    }

    /**
     * 开始下载
     */
    fun startDownload(
        url: String,
        name: String? = "",
        size: Long? = 1L,
        fastDownload: Boolean? = false
    ) {
        val downloadInfo = DownloadInfo()
        downloadInfo.downloadUrl = url
        downloadInfo.fastDownload = fastDownload ?: false
        downloadInfo.size = size ?: 1L
        downloadInfo.status = DownloadStatus.WAIT.value
        when {
            url.contains(".m3u8") -> {
                Observable.create<DownloadInfo> {
                    it.onNext(M3u8Parser.parserM3u8(downloadInfo))
                    it.onComplete()
                }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        it.videoType = VideoType.TYPE_M3U8.value
                        val fileName = "${Util.getCurrentTime()}.m3u8"
                        it.fileName = fileName
                        if (!TextUtils.isEmpty(name)) {
                            it.videoName = "$name.m3u8"
                        } else {
                            it.videoName = fileName
                        }
                        it.savePath = getDownloadPath() + fileName
                        it.save()
                        startM3u8(it)
                    }
            }
            url.contains(".mp4") -> {
                downloadInfo.videoType = VideoType.TYPE_MP4.value
                val fileName = "${Util.getCurrentTime()}.mp4"
                downloadInfo.fileName = fileName
                if (!TextUtils.isEmpty(name)) {
                    downloadInfo.videoName = "$name.mp4"
                } else {
                    downloadInfo.videoName = fileName
                }
                downloadInfo.savePath = getDownloadPath() + fileName
                downloadInfo.save()
                startMp4(downloadInfo)
            }
            else -> {
                val extensionName = "." + FileUtils.getFileExtension(url)
                val fileName = Util.getCurrentTime() + extensionName
                downloadInfo.fileName = fileName
                if (!TextUtils.isEmpty(name)) {
                    downloadInfo.videoName = name + extensionName
                } else {
                    downloadInfo.videoName = fileName
                }
                downloadInfo.videoType = VideoType.TYPE_OTHER.value
                downloadInfo.savePath = getDownloadPath() + fileName
                startTorrent(downloadInfo)
            }
        }
    }

    /**
     * mp4下载
     */
    private fun startMp4(downloadInfo: DownloadInfo) {
        val taskId = FileDownloader.getImpl()
            .create(downloadInfo.downloadUrl)
            .addHeader("User-Agent", Constants.ANDROID_USER_AGENT)
            .setPath(downloadInfo.savePath)
            .setListener(object : FileDownloadSampleListener() {
                override fun pending(task: BaseDownloadTask?, soFarBytes: Int, totalBytes: Int) {
                    super.pending(task, soFarBytes, totalBytes)
                    //准备中
                    sendMp4DownloadState(
                        downloadInfo,
                        DownloadStatus.WAIT.value,
                        soFarBytes,
                        totalBytes,
                        task?.speed
                    )
                }

                override fun started(task: BaseDownloadTask?) {
                    super.started(task)
                    LogUtils.eTag("ZBB", "下载开始")
                    //开始中
                    sendMp4DownloadState(
                        downloadInfo,
                        DownloadStatus.WAIT.value,
                        task?.smallFileSoFarBytes,
                        task?.smallFileTotalBytes,
                        task?.speed
                    )
                }

                override fun error(task: BaseDownloadTask?, e: Throwable?) {
                    super.error(task, e)
                    //下载错误
                    sendMp4DownloadState(
                        downloadInfo,
                        DownloadStatus.ERROR.value,
                        task?.smallFileSoFarBytes,
                        task?.smallFileTotalBytes,
                        task?.speed
                    )
                }

                override fun connected(
                    task: BaseDownloadTask?,
                    etag: String?,
                    isContinue: Boolean,
                    soFarBytes: Int,
                    totalBytes: Int
                ) {
                    super.connected(task, etag, isContinue, soFarBytes, totalBytes)
                    //已连接
                    sendMp4DownloadState(
                        downloadInfo,
                        DownloadStatus.DOWNLOADING.value,
                        soFarBytes,
                        totalBytes,
                        task?.speed
                    )
                }

                override fun progress(task: BaseDownloadTask?, soFarBytes: Int, totalBytes: Int) {
                    super.progress(task, soFarBytes, totalBytes)
                    LogUtils.eTag("ZBB", "下载中=soFarBytes=$soFarBytes totalBytes=$totalBytes")
                    //下载中
                    sendMp4DownloadState(
                        downloadInfo,
                        DownloadStatus.DOWNLOADING.value,
                        soFarBytes,
                        totalBytes,
                        task?.speed
                    )
                }

                override fun paused(task: BaseDownloadTask?, soFarBytes: Int, totalBytes: Int) {
                    super.paused(task, soFarBytes, totalBytes)
                    //已暂停
                    sendMp4DownloadState(
                        downloadInfo,
                        DownloadStatus.PAUSE.value,
                        soFarBytes,
                        totalBytes,
                        task?.speed
                    )
                }

                override fun completed(task: BaseDownloadTask?) {
                    super.completed(task)
                    LogUtils.eTag("ZBB", "下载成功")
                    //下载完成
                    sendMp4DownloadState(
                        downloadInfo,
                        DownloadStatus.COMPLETE.value,
                        0,
                        task?.smallFileTotalBytes,
                        task?.speed
                    )
                }
            })
            .setForceReDownload(true)
            .setAutoRetryTimes(2)
            .setSyncCallback(true)
            .setCallbackProgressTimes(1000)
            .start()
        downloadInfo.taskId = taskId
        downloadInfo.save()
    }

    @SuppressLint("CheckResult")
    private fun sendMp4DownloadState(
        downloadInfo: DownloadInfo,
        status: Int,
        currentSize: Int?,
        totalSize: Int?,
        speed: Int? = 0
    ) {

        downloadInfo.currentSize = currentSize?.toLong() ?: 0L
        downloadInfo.totalSize = totalSize?.toLong() ?: 1L
        downloadInfo.speed = String.format("%sKB/S", speed)
        downloadInfo.fastDownload = downloadInfo.fastDownload
        downloadInfo.status = status
        if (status == DownloadStatus.COMPLETE.value) {
            //更新数据库
            downloadInfo.status = DownloadStatus.COMPLETE.value
            downloadInfo.totalSize = totalSize?.toLong() ?: 1L
            DownloadDaoManager.INSTANCE.updateTask(downloadInfo)
        } else {
            DownloadDaoManager.INSTANCE.updateTask(downloadInfo)
        }
    }

    /**
     * m3u8下载
     */
    private fun startM3u8(downloadInfo: DownloadInfo) {
        val downloadPath = getDownloadPath()
        val tsDownloadList = mutableListOf<TsDownloadInfo>()
        Observable.create(ObservableOnSubscribe<Void> {
            M3u8ParserUtil().parallelParserM3u8Content(downloadInfo)
            it.onComplete()
        })
            .subscribeOn(Schedulers.io())
            .unsubscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : BaseObserveListener<Void>() {
                override fun onComplete() {
                    super.onComplete()
                    if (!downloadInfo.tsList.isNullOrEmpty()) {
                        val keyUrl = downloadInfo.keyUrl
                        val m3u8Content = downloadInfo.m3u8Content
                        val pathName = "$downloadPath/${downloadInfo.fileName.replace(".m3u8", "")}"
                        if (!Util.isEmpty(keyUrl)) {
                            val key = keyUrl!!.substringAfterLast("URI=\"").substringBefore("\"")
                            downloadKey(key, pathName)
                            m3u8Content?.replace(key, "${pathName}key.key")
                        }
                        val m3u8List = downloadInfo.tsList.toList()
                        if (m3u8List.isNotEmpty()) {
                            var num = 0
                            for (line in m3u8List) {
                                if (!TextUtils.isEmpty(line)) {
                                    val tsDownloadInfo = TsDownloadInfo()
                                    tsDownloadInfo.num = num
                                    tsDownloadInfo.url = line
                                    tsDownloadInfo.name = "$num.ts"
                                    tsDownloadInfo.path = pathName + "/" + tsDownloadInfo.name
                                    tsDownloadList.add(tsDownloadInfo)
                                    ++num
                                }
                            }
                            if (tsDownloadList.isNotEmpty()) {
                                downloadTs(tsDownloadList, downloadInfo)
                            } else {
                                updateM3u8DownloadState(DownloadStatus.ERROR.value, downloadInfo)
                            }
                        } else {
                            updateM3u8DownloadState(DownloadStatus.ERROR.value, downloadInfo)
                        }
                    } else {
                        updateM3u8DownloadState(DownloadStatus.ERROR.value, downloadInfo)
                    }
                }
            })
    }

    /**
     * 种子下载
     */
    fun startTorrent(downloadInfo: DownloadInfo) {
        val url = downloadInfo.downloadUrl
        val tasks = DownloadDaoManager.INSTANCE.findTaskUrl(url)
//        if (!tasks.isNullOrEmpty()) {
//            //任务存在
//            val task = tasks[0]
//            when (task.status) {
//                DownloadStatus.COMPLETE.value -> {
//                    //已完成
//                }
//                else -> {
//                    //下载中
//                }
//            }
//        } else {
        //任务不存在
        val path = getDownloadPath()
        val taskId = if (url.startsWith("magnet:?")) {
            XLTaskHelper.instance().addMagnetTask(url, path, null)
        } else {
            XLTaskHelper.instance().addThunderTask(url, path, null)
        }
        LogUtils.eTag("ZBB", "得到的任务ID=$taskId")
        val taskInfo = XLTaskHelper.instance().getTaskInfo(taskId)
        downloadInfo.taskId = taskInfo.mTaskId.toInt()
        downloadInfo.currentSize = taskInfo.mDownloadSize
        downloadInfo.totalSize = taskInfo.mFileSize
        downloadInfo.savePath = path
        downloadInfo.status = taskInfo.mTaskStatus
        downloadInfo.speed = FileTools.convertFileSize(taskInfo.mDownloadSpeed)
        DownloadDaoManager.INSTANCE.updateTask(downloadInfo)
//        }
    }

    /**
     * 暂停
     */
    fun pause(taskId: Int) {
        FileDownloader.getImpl().pause(taskId)
    }

    /**
     * 暂停所有
     */
    fun pauseAll() {
        FileDownloader.getImpl().pauseAll()
    }

    /**
     * 设置默认下载目录
     */
    fun setDownloadPath(path: String) {
        SpKit.saveDownloadPath(path)
    }

    /**
     * 下载加密Key
     */
    private fun downloadKey(key: String, downloadPath: String) {
        startDownloadFile(key, "$downloadPath/key.key")
    }

    /**
     * 下载TS
     */
    private fun downloadTs(tsDownloadList: List<TsDownloadInfo>, downloadInfo: DownloadInfo) {
        downloadInfo.updateAll("fileName = ?", downloadInfo.fileName)
        val currentNum = AtomicLong(0)
        val downloadListener = createDownloadListener(downloadInfo, tsDownloadList, currentNum)
        val queueSet = FileDownloadQueueSet(downloadListener)
        val tasks = mutableListOf<BaseDownloadTask>()
        for (tsDownloadInfo in tsDownloadList) {
            val downloader = FileDownloader.getImpl()
            downloader.setMaxNetworkThreadCount(30)
            tasks.add(
                downloader
                    .create(tsDownloadInfo.url)
                    .setPath(tsDownloadInfo.path)
            )
        }
        queueSet.disableCallbackProgressTimes()
        queueSet.setForceReDownload(true)
        queueSet.setAutoRetryTimes(3)
        queueSet.downloadSequentially(tasks)
        queueSet.start()
    }

    private fun createDownloadListener(
        downloadInfo: DownloadInfo,
        tsDownloadList: List<TsDownloadInfo>,
        currentNum: AtomicLong
    ): FileDownloadListener {
        return object : FileDownloadSampleListener() {

            override fun completed(task: BaseDownloadTask?) {
                if (task != null && !task.url.isNullOrEmpty()) {
                    downloadInfo.m3u8Content =
                        downloadInfo.m3u8Content?.replace(
                            task.url,
                            "file://${tsDownloadList[currentNum.get().toInt()].path}"
                        )
                    currentNum.getAndIncrement()
                }

                downloadInfo.currentSize = currentNum.get()
                downloadInfo.totalSize = tsDownloadList.size.toLong()
                if (currentNum.get() == tsDownloadList.size.toLong()) {
                    LogUtils.eTag("ZBB", "m3u8下载成功")
                    val bw = BufferedWriter(FileWriter(downloadInfo.savePath))
                    bw.write(downloadInfo.m3u8Content)
                    bw.flush()
                    bw.close()
                    //更新数据库
                    downloadInfo.status = DownloadStatus.COMPLETE.value
                    downloadInfo.updateAll("fileName = ?", downloadInfo.fileName)
                    downloadInfo.status = DownloadStatus.COMPLETE.value
                } else {
                    //下载中
                    downloadInfo.status = DownloadStatus.DOWNLOADING.value
                    downloadInfo.speed = "${task?.speed}KB/S"
                }
                DownloadDaoManager.INSTANCE.updateTask(downloadInfo)
            }

            override fun error(task: BaseDownloadTask?, e: Throwable?) {
                downloadInfo.currentSize = task?.smallFileSoFarBytes?.toLong() ?: 0L
                downloadInfo.totalSize = task?.smallFileTotalBytes?.toLong() ?: 1L
                downloadInfo.status = DownloadStatus.ERROR.value
                DownloadDaoManager.INSTANCE.updateTask(downloadInfo)
            }

            override fun paused(task: BaseDownloadTask?, soFarBytes: Int, totalBytes: Int) {
                downloadInfo.currentSize = currentNum.get()
                downloadInfo.totalSize = tsDownloadList.size.toLong()
                downloadInfo.status = DownloadStatus.PAUSE.value
                DownloadDaoManager.INSTANCE.updateTask(downloadInfo)
            }
        }
    }

    /**
     * 更新M3u8下载状态
     */
    private fun updateM3u8DownloadState(
        status: Int,
        downloadInfo: DownloadInfo
    ) {
        downloadInfo.status = status
        downloadInfo.speed = "0KB/S"
        DownloadDaoManager.INSTANCE.updateTask(downloadInfo)
    }

    /**
     * 下载不同的文件
     */
    fun startDownloadFile(url: String, path: String) {
        FileDownloader.getImpl()
            .create(url)
            .setPath(path)
            .setWifiRequired(true)
            .setListener(object : FileDownloadSampleListener() {
                override fun completed(task: BaseDownloadTask?) {
                    super.completed(task)
                }

                override fun error(task: BaseDownloadTask?, e: Throwable?) {
                    super.error(task, e)
                }
            })
            .setForceReDownload(true)
            .start()
    }

    /**
     * 获取下载路径
     */
    fun getDownloadPath(): String {
        var path = SpKit.getDownloadPath()
        if (TextUtils.isEmpty(path)) {
            path = "${Environment.getExternalStorageDirectory().absolutePath}/LDVideo"
        }
        val file = File(path)
        if (!file.exists()) {
            file.mkdirs()
        }
        return path
    }


    /**
     * 初始化数据库以及下载框架
     */
    fun init(application: Application) {
        Utils.init(application)
        LitePal.initialize(application)
        FileDownloader.setupOnApplicationOnCreate(application)
            .connectionCreator(OkHttp3Connection.Creator())
            .commit()
        XLTaskHelper.init(application)
        val intent = Intent(application, DownService::class.java)
        application.startService(intent)
    }

}