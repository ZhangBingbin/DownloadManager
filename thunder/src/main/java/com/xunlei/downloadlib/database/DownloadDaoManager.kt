package com.xunlei.downloadlib.database

import android.content.ContentValues
import com.xunlei.downloadlib.bean.DownloadInfo
import com.xunlei.downloadlib.status.DownloadStatus
import com.xunlei.downloadlib.status.VideoType
import org.litepal.LitePal

/**
 * ================================================
 * 框架要求框架中的每个 {@link } 都需要实现此类,以满足规范
 *
 * @function 作用
 * Created by Joe_ZBB on 2019-05-27 13:29
 * ================================================
 */
class DownloadDaoManager {
    companion object {
        val INSTANCE: DownloadDaoManager by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            DownloadDaoManager()
        }
    }

    /**
     * 获取下载中列表数据（除去种子以为的文件）
     */
    fun loadDownloadList(): MutableList<DownloadInfo> {
        return LitePal.where("status != ?", DownloadStatus.COMPLETE.value.toString()).find(DownloadInfo::class.java)
    }

    /**
     * 获取所有正在下载的实体集合
     */
    fun loadAllDownloadingList():MutableList<DownloadInfo>{
        return LitePal.where("status != ?",DownloadStatus.COMPLETE.value.toString()).find(DownloadInfo::class.java)
    }

    /**
     * 下载列表中的种子视频
     */
    fun loadTorrentDownloadList():MutableList<DownloadInfo>{
        return LitePal.where("status != ? and videoType = ?", DownloadStatus.COMPLETE.value.toString(),VideoType.TYPE_OTHER.value).find(DownloadInfo::class.java)
    }

    /**
     * 获取种子类型的视频（正在下载中的）
     */
    fun loadTorrentDownloadingList():MutableList<DownloadInfo>{
        return LitePal.where("status = ? and videoType = ?", DownloadStatus.DOWNLOADING.value.toString(),VideoType.TYPE_OTHER.value).find(DownloadInfo::class.java)
    }

    /**
     * 获取下载完成列表数据
     */
    fun loadDownloadedList(): MutableList<DownloadInfo> {
        return LitePal.where("status = ?", DownloadStatus.COMPLETE.value.toString()).find(DownloadInfo::class.java)
    }

    fun findTaskUrl(url: String): MutableList<DownloadInfo> {
        return LitePal.where("downloadUrl = ?", url).find(DownloadInfo::class.java)
    }

    /**
     * 更新数据
     */
    fun updateTask(downloadInfo: DownloadInfo){
        downloadInfo.saveOrUpdate()
    }
}