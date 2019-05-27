package com.zbb.downloadlibrary.status

/**
 * ================================================
 * 框架要求框架中的每个 {@link } 都需要实现此类,以满足规范
 *
 * @function 作用
 * Created by Joe_ZBB on 2019-05-27 11:11
 * ================================================
 */
enum class DownloadStatus {
    COMPLETE,
    DOWNLOADING,
    WAIT,
    PAUSE,
    ERROR
}