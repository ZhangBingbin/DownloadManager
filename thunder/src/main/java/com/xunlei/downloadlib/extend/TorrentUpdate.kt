package com.xunlei.downloadlib.extend

import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.NetworkUtils
import com.xunlei.downloadlib.XLTaskHelper
import com.xunlei.downloadlib.database.DownloadDaoManager
import com.xunlei.downloadlib.extend.utils.DownUtil
import com.xunlei.downloadlib.extend.utils.FileTools
import com.xunlei.downloadlib.status.DownloadStatus
import com.xunlei.downloadlib.util.DownloadUtil
import org.greenrobot.eventbus.EventBus

/**
 * ================================================
 * 框架要求框架中的每个 {@link } 都需要实现此类,以满足规范
 *
 * @function 作用
 * Created by Joe_ZBB on 2019-05-28 11:49
 * ================================================
 */
class TorrentUpdate {
    companion object {
        val INSTANCE: TorrentUpdate by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            TorrentUpdate()
        }
    }

    fun updateItem() {
        val downloadingTasks = DownloadDaoManager.INSTANCE.loadTorrentDownloadList()
        if (!downloadingTasks.isNullOrEmpty()) {
            if (!NetworkUtils.isConnected()) {
                for (task in downloadingTasks) {
                    if (task.status != DownloadStatus.PAUSE.value) {
                        XLTaskHelper.instance().removeTask(task.taskId.toLong())
                        task.status = DownloadStatus.PAUSE.value
                        DownloadDaoManager.INSTANCE.updateTask(task)
                    }
                }
            } else {
                val downCount = 3
                val downloadingList = DownloadDaoManager.INSTANCE.loadTorrentDownloadingList()
                var wait = if (downloadingList.isNullOrEmpty()) {
                    0
                } else {
                    downloadingList.size - downCount
                }
                for (task in downloadingTasks) {
                    if (wait > 0) {
                        if (task.status != DownloadStatus.WAIT.value && task.status != DownloadStatus.ERROR.value) {
                            wait--
                            XLTaskHelper.instance().removeTask(task.taskId.toLong())
                            task.status = DownloadStatus.WAIT.value
                            DownloadDaoManager.INSTANCE.updateTask(task)
                            continue
                        }
                    }
                    if (task.status != DownloadStatus.PAUSE.value && task.status != DownloadStatus.WAIT.value && task.taskId != 0) {
                        val taskInfo = XLTaskHelper.instance().getTaskInfo(task.taskId.toLong())
                        task.status = taskInfo.mTaskStatus
                        task.currentSize = taskInfo.mDownloadSize
                        task.taskId=taskInfo.mTaskId.toInt()
                        task.totalSize = taskInfo.mFileSize
                        task.speed = FileTools.convertFileSize(taskInfo.mDownloadSpeed)
                        if (taskInfo.mFileSize != 0L) {
                            task.progress = (taskInfo.mDownloadSize * 100 / taskInfo.mFileSize).toInt()
                        }
                        DownloadDaoManager.INSTANCE.updateTask(task)
                        if (DownUtil.isDownSuccess(task)) {
                            XLTaskHelper.instance().removeTask(task.taskId.toLong())
                            task.status = DownloadStatus.COMPLETE.value
                            DownloadDaoManager.INSTANCE.updateTask(task)
                        }
                    } else {
                        if (wait<0&&task.status==DownloadStatus.WAIT.value){
                            DownloadUtil.INSTANCE.startTorrent(task)
                        }
                    }
                }
            }
        }
    }
}