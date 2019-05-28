package com.xunlei.downloadlib.event

import com.xunlei.downloadlib.status.DownloadStatus

/**
 * ================================================
 * 框架要求框架中的每个 {@link } 都需要实现此类,以满足规范
 *
 * @function 作用
 * Created by Joe_ZBB on 2019-05-27 11:52
 * ================================================
 */
class DownloadEvent {
    var taskName: String = ""
    var currentSize: Long = 0L
    var totalSize: Long = 1L
    var status = DownloadStatus.WAIT.value
    var speed: String = "0KB/S"
    var fastDownload:Boolean=false
    //是否更新
    var update:Boolean=false
}