package com.zbb.downloadlibrary.database

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
}