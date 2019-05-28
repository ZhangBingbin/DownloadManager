package com.xunlei.downloadlib.util

import com.blankj.utilcode.util.SPUtils

/**
 * ================================================
 * 框架要求框架中的每个 {@link } 都需要实现此类,以满足规范
 *
 * @function 作用
 * Created by Joe_ZBB on 2019-05-27 11:40
 * ================================================
 */
object SpKit {

    /**
     * 保存下载路径
     */
    fun saveDownloadPath(path: String) {
        SPUtils.getInstance().put(Constants.STORE_PATH, path)
    }

    /**
     * 获取下载路径
     */
    fun getDownloadPath(): String {
        return SPUtils.getInstance().getString(Constants.STORE_PATH)
    }

}