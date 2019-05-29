package com.xunlei.downloadlib.util

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
import com.xunlei.downloadlib.listener.OkHttp3Connection
import com.xunlei.downloadlib.status.DownloadStatus
import com.xunlei.downloadlib.status.VideoType
import io.reactivex.Observable
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
 * @function 作用  下载核心管理类
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
     * 用来储存m3u8下载的监听事件(处理多任务的暂停)
     */
    private var mDownloadContextMap = mutableMapOf<String, FileDownloadListener>()

    /**
     * 开始下载（分为m3u8，和其他文件类型下载(mp4,ftp,torrent等等)）
     * url：下载地址
     * callBack：任务的状态
     * continueDownload:是否继续下载或重新下载
     * name:文件名称
     * fastDownload:是否高速下载
     */
    fun startDownload(
        url: String,
        callBack: (msg: Int) -> Unit,
        continueDownload: Boolean = false,
        name: String? = "",
        fastDownload: Boolean? = false
    ) {
        val tasks = DownloadDaoManager.INSTANCE.findExitTaskUrl(url)
        if (!tasks.isNullOrEmpty() && !continueDownload) {
            //任务存在
            val task = tasks[0]
            when (task.status) {
                DownloadStatus.COMPLETE.value -> {
                    //已完成
                    callBack(DownloadStatus.COMPLETE.value)
                }
                else -> {
                    //正在下载列表中的数据
                    callBack(DownloadStatus.DOWNLOADING.value)
                }
            }
        } else {
            //任务不存在(重新下载或继续下载)
            val downloadInfo = DownloadInfo()
            downloadInfo.downloadUrl = url
            downloadInfo.fastDownload = fastDownload ?: false
            downloadInfo.status = DownloadStatus.WAIT.value
            DownloadDaoManager.INSTANCE.updateTask(downloadInfo)
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
                            DownloadDaoManager.INSTANCE.updateTask(it)
                            startM3u8(it)
                        }
                }
                else -> {
                    val extensionName = "." + FileUtils.getFileExtension(url).substringBefore("?")
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
    }

    /**
     * m3u8下载
     */
    private fun startM3u8(downloadInfo: DownloadInfo) {
        val downloadPath = getDownloadPath()
        val tsDownloadList = mutableListOf<TsDownloadInfo>()
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

    /**
     * 种子下载(ftp,torrent,mp4)
     */
    fun startTorrent(downloadInfo: DownloadInfo) {
        //任务不存在
        val url = downloadInfo.downloadUrl
        val path = getDownloadPath()
        val taskId = if (url.startsWith("magnet:?")) {
            XLTaskHelper.instance().addMagnetTask(url, path, null)
        } else {
            XLTaskHelper.instance().addThunderTask(url, path, null)
        }
        val taskInfo = XLTaskHelper.instance().getTaskInfo(taskId)
        downloadInfo.taskId = taskInfo.mTaskId.toInt()
        downloadInfo.currentSize = taskInfo.mDownloadSize
        downloadInfo.totalSize = taskInfo.mFileSize
        downloadInfo.savePath = path
        downloadInfo.status = taskInfo.mTaskStatus
        downloadInfo.speed = FileTools.convertFileSize(taskInfo.mDownloadSpeed)
        DownloadDaoManager.INSTANCE.updateTask(downloadInfo)
    }

    /**
     * 暂停任务
     */
    fun pause(downloadInfo: DownloadInfo) {
        val taskId = downloadInfo.taskId
        val videoType = downloadInfo.videoType
        DownloadDaoManager.INSTANCE.pauseTask(taskId)
        when (videoType) {
            VideoType.TYPE_OTHER.value -> {
                //暂停mp4,种子,ftp
                XLTaskHelper.instance().removeTask(taskId.toLong())
            }
            else -> {
                //暂停m3u8
                FileDownloader.getImpl().pause(mDownloadContextMap[downloadInfo.fileName])
            }
        }
    }

    /**
     * 暂停所有
     */
    fun pauseAll() {
        FileDownloader.getImpl().pauseAll()
        for (downloadInfo in DownloadDaoManager.INSTANCE.loadTorrentDownloadingList()) {
            if (downloadInfo.taskId != 0) {
                XLTaskHelper.instance().stopTask(downloadInfo.taskId.toLong())
            }
        }
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
    private fun downloadTs(
        tsDownloadList: List<TsDownloadInfo>,
        downloadInfo: DownloadInfo
    ) {
        DownloadDaoManager.INSTANCE.updateTask(downloadInfo)
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
        if (mDownloadContextMap.contains(downloadInfo.fileName)) {
            mDownloadContextMap.remove(downloadInfo.fileName)
        }
        mDownloadContextMap[downloadInfo.fileName] = downloadListener
        queueSet.disableCallbackProgressTimes()
        queueSet.setForceReDownload(true)
        queueSet.setAutoRetryTimes(2)
        queueSet.downloadSequentially(tasks)
        queueSet.start()
    }

    /**
     * m3u8下载的监听事件
     */
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
                } else {
                    //下载中
                    downloadInfo.status = DownloadStatus.DOWNLOADING.value
                    downloadInfo.speed = "${task?.speed}KB/S"
                    if (downloadInfo.totalSize != 0L) {
                        downloadInfo.progress = (downloadInfo.currentSize * 100 / downloadInfo.totalSize).toInt()
                    }
                }
                DownloadDaoManager.INSTANCE.updateTask(downloadInfo)
            }

            override fun error(task: BaseDownloadTask?, e: Throwable?) {
                downloadInfo.currentSize = task?.smallFileSoFarBytes?.toLong() ?: 0L
                downloadInfo.totalSize = task?.smallFileTotalBytes?.toLong() ?: 1L
                downloadInfo.status = DownloadStatus.ERROR.value
                if (downloadInfo.totalSize != 0L) {
                    downloadInfo.progress = (downloadInfo.currentSize * 100 / downloadInfo.totalSize).toInt()
                }
                DownloadDaoManager.INSTANCE.updateTask(downloadInfo)
            }

            override fun paused(task: BaseDownloadTask?, soFarBytes: Int, totalBytes: Int) {
                if (downloadInfo.status != DownloadStatus.PAUSE.value) {
                    downloadInfo.currentSize = currentNum.get()
                    downloadInfo.totalSize = tsDownloadList.size.toLong()
                    downloadInfo.status = DownloadStatus.PAUSE.value
                    if (downloadInfo.totalSize != 0L) {
                        downloadInfo.progress = (downloadInfo.currentSize * 100 / downloadInfo.totalSize).toInt()
                    }
                    DownloadDaoManager.INSTANCE.updateTask(downloadInfo)
                }
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
     * 初始化数据库以及下载框架（必须）
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