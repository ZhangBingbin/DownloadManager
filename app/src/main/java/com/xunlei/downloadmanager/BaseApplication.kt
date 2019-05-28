package com.xunlei.downloadmanager

import android.app.Application
import com.xunlei.downloadlib.util.DownloadUtil

/**
 * ================================================
 * 框架要求框架中的每个 {@link } 都需要实现此类,以满足规范
 *
 * @function 作用
 * Created by Joe_ZBB on 2019-05-27 14:59
 * ================================================
 */
class BaseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DownloadUtil.INSTANCE.init(this)
    }
}