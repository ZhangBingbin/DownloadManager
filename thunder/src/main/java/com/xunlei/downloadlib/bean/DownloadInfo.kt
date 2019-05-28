package com.xunlei.downloadlib.bean

import com.xunlei.downloadlib.status.DownloadStatus
import com.xunlei.downloadlib.status.VideoType
import org.litepal.crud.LitePalSupport
import java.io.Serializable
import java.util.concurrent.LinkedBlockingQueue

/**
 * ================================================
 * 框架要求框架中的每个 {@link } 都需要实现此类,以满足规范
 *
 * @function 作用
 * Created by Joe_ZBB on 2019-05-27 10:56
 * ================================================
 */
class DownloadInfo : LitePalSupport(), Serializable {
    //任务ID
    var taskId = -1
    //文件名称
    var fileName: String = ""
    //视频名称
    var videoName: String = ""
    //下载地址
    var downloadUrl: String = ""
    //网站地址
    var webUrl: String? = ""
    //当前下载大小
    var currentSize = 0L
    //总大小
    var totalSize = 1L
    //存储路径
    var savePath: String? = ""
    //下载状态
    var status = DownloadStatus.WAIT.value
    //下载进度
    var progress: Int = 0
    //下载速度
    var speed: String = "0KB"
    //视频类型
    var videoType:String?=""
    //m3u8加密key url
    var keyUrl: String? = ""
    //视频时长
    var duration: String? = ""
    //视频大小
    var size = 0L
    //完整的m3u8信息
    var m3u8Content: String? = ""
    //ts集合
    var tsList = LinkedBlockingQueue<String>()
    //是否高速下载
    var fastDownload: Boolean = false

    override fun toString(): String {
        return "DownloadInfo(taskId=$taskId, fileName='$fileName', videoName='$videoName', downloadUrl='$downloadUrl', webUrl=$webUrl, currentSize=$currentSize, totalSize=$totalSize, savePath=$savePath, status=$status, progress=$progress, speed='$speed', videoType='$videoType', keyUrl=$keyUrl, duration=$duration, size=$size, m3u8Content=$m3u8Content, tsList=$tsList, fastDownload=$fastDownload)"
    }


}