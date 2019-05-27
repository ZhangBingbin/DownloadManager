package com.zbb.downloadlibrary.util

import android.annotation.SuppressLint
import android.app.Application
import android.os.Environment
import android.text.TextUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.Utils
import com.liulishuo.filedownloader.*
import com.zbb.downloadlibrary.bean.DownloadInfo
import com.zbb.downloadlibrary.bean.ParserDownloadInfo
import com.zbb.downloadlibrary.bean.TsDownloadInfo
import com.zbb.downloadlibrary.event.DownloadEvent
import com.zbb.downloadlibrary.listener.BaseObserveListener
import com.zbb.downloadlibrary.listener.OkHttp3Connection
import com.zbb.downloadlibrary.status.DownloadStatus
import com.zbb.downloadlibrary.status.VideoType
import io.reactivex.Observable
import io.reactivex.ObservableOnSubscribe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
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
    fun startDownload(url: String, name: String? = "", size: Long? = 1L, fastDownload: Boolean? = false,parserInfo: ParserDownloadInfo?=null) {
        val downloadInfo = DownloadInfo()
        downloadInfo.downloadUrl = url
        downloadInfo.fastDownload = fastDownload ?: false
        downloadInfo.size = size ?: 1L
        downloadInfo.status = DownloadStatus.WAIT
        when {
            url.contains(".m3u8") -> {
                downloadInfo.videoType = VideoType.TYPE_M3U8
                val fileName = "${Util.getCurrentTime()}.m3u8"
                downloadInfo.fileName = fileName
                if (!TextUtils.isEmpty(name)) {
                    downloadInfo.videoName = "$name.m3u8"
                } else {
                    downloadInfo.videoName = fileName
                }
                downloadInfo.savePath = getDownloadPath() + fileName
                downloadInfo.save()
                startM3u8(downloadInfo)
            }
            url.contains(".mp4") -> {
                downloadInfo.videoType = VideoType.TYPE_MP4
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
                        DownloadStatus.WAIT,
                        soFarBytes,
                        totalBytes,
                        task?.speed
                    )
                }

                override fun started(task: BaseDownloadTask?) {
                    super.started(task)
                    LogUtils.eTag("ZBB","下载开始")
                    //开始中
                    sendMp4DownloadState(
                        downloadInfo,
                        DownloadStatus.WAIT,
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
                        DownloadStatus.ERROR,
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
                        DownloadStatus.DOWNLOADING,
                        soFarBytes,
                        totalBytes,
                        task?.speed
                    )
                }

                override fun progress(task: BaseDownloadTask?, soFarBytes: Int, totalBytes: Int) {
                    super.progress(task, soFarBytes, totalBytes)
                    //下载中
                    sendMp4DownloadState(
                        downloadInfo,
                        DownloadStatus.DOWNLOADING,
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
                        DownloadStatus.PAUSE,
                        soFarBytes,
                        totalBytes,
                        task?.speed
                    )
                }

                override fun completed(task: BaseDownloadTask?) {
                    super.completed(task)
                    LogUtils.eTag("ZBB","下载成功")
                    //下载完成
                    sendMp4DownloadState(
                        downloadInfo,
                        DownloadStatus.COMPLETE,
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
        status: DownloadStatus,
        currentSize: Int?,
        totalSize: Int?,
        speed: Int? = 0
    ) {
        val downloadEvent = DownloadEvent()
        downloadEvent.taskName = downloadInfo.fileName
        downloadEvent.currentSize = currentSize?.toLong() ?: 0L
        downloadEvent.totalSize = totalSize?.toLong() ?: 1L
        downloadEvent.speed = String.format("%sKB/S", speed)
        downloadEvent.fastDownload = downloadInfo.fastDownload
        downloadEvent.status = status
        if (status == DownloadStatus.COMPLETE) {
            downloadEvent.update = true
            //更新数据库
            Observable.create<Void> {
                downloadInfo.status = DownloadStatus.COMPLETE
                downloadInfo.totalSize = totalSize?.toLong() ?: 1L
                downloadInfo.save()
                it.onComplete()
            }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : BaseObserveListener<Void>() {
                    override fun onComplete() {
                        super.onComplete()
                        EventBus.getDefault().post(downloadEvent)
                    }
                })
        } else {
            downloadEvent.update = false
            EventBus.getDefault().post(downloadEvent)
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
                                updateM3u8DownloadState(DownloadStatus.ERROR, downloadInfo)
                            }
                        } else {
                            updateM3u8DownloadState(DownloadStatus.ERROR, downloadInfo)
                        }
                    } else {
                        updateM3u8DownloadState(DownloadStatus.ERROR, downloadInfo)
                    }
                }
            })
    }

    /**
     * 种子下载
     */
    private fun startTorrent(downloadInfo: DownloadInfo) {

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

                val downloadEvent = DownloadEvent()
                downloadEvent.taskName = downloadInfo.fileName
                downloadEvent.currentSize = currentNum.get()
                downloadEvent.totalSize = tsDownloadList.size.toLong()
                downloadEvent.fastDownload = downloadInfo.fastDownload
                if (currentNum.get() == tsDownloadList.size.toLong()) {

                    LogUtils.eTag("ZBB","m3u8下载成功")

                    val bw = BufferedWriter(FileWriter(downloadInfo.savePath))
                    bw.write(downloadInfo.m3u8Content)
                    bw.flush()
                    bw.close()
                    //更新数据库
                    downloadInfo.status = DownloadStatus.COMPLETE
                    downloadInfo.updateAll("fileName = ?", downloadInfo.fileName)

                    downloadEvent.update = true
                    downloadEvent.status = DownloadStatus.COMPLETE
                    EventBus.getDefault().post(downloadEvent)
                } else {

                    LogUtils.eTag("ZBB","m3u8下载中=${downloadEvent.currentSize}")

                    //下载中
                    downloadEvent.status = DownloadStatus.DOWNLOADING
                    downloadEvent.speed = "${task?.speed}KB/S"
                    downloadEvent.update = false
                    EventBus.getDefault().post(downloadEvent)
                }
            }

            override fun error(task: BaseDownloadTask?, e: Throwable?) {
                val downloadEvent = DownloadEvent()
                downloadEvent.taskName = downloadInfo.fileName
                downloadEvent.currentSize = task?.smallFileSoFarBytes?.toLong() ?: 0L
                downloadEvent.totalSize = task?.smallFileTotalBytes?.toLong() ?: 1L
                downloadEvent.fastDownload = downloadInfo.fastDownload
                downloadEvent.update = false
                downloadEvent.status = DownloadStatus.ERROR
                EventBus.getDefault().post(downloadEvent)
            }

            override fun paused(task: BaseDownloadTask?, soFarBytes: Int, totalBytes: Int) {
                val downloadEvent = DownloadEvent()
                downloadEvent.taskName = downloadInfo.fileName
                downloadEvent.currentSize = currentNum.get()
                downloadEvent.totalSize = tsDownloadList.size.toLong()
                downloadEvent.fastDownload = downloadInfo.fastDownload
                downloadEvent.status = DownloadStatus.PAUSE
                downloadEvent.update = false
                EventBus.getDefault().post(downloadEvent)
            }
        }
    }

    /**
     * 更新M3u8下载状态
     */
    private fun updateM3u8DownloadState(
        status: DownloadStatus,
        downloadInfo: DownloadInfo
    ) {
        val downloadEvent = DownloadEvent()
        downloadEvent.status = status
        downloadEvent.taskName = downloadInfo.fileName
        downloadEvent.speed = "0KB/S"
        downloadEvent.update = false
        downloadEvent.fastDownload = downloadInfo.fastDownload
        downloadEvent.update = status == DownloadStatus.COMPLETE
        EventBus.getDefault().post(downloadEvent)
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
    }

}