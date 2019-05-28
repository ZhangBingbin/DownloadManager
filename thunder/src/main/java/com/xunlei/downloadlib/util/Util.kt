package com.xunlei.downloadlib.util

import java.text.SimpleDateFormat
import java.util.*

/**
 * ================================================
 * 框架要求框架中的每个 {@link } 都需要实现此类,以满足规范
 *
 * @function 作用
 * Created by Joe_ZBB on 2019-05-27 11:37
 * ================================================
 */
object Util {

    /**
     * 获取当前时间戳
     */
    fun getCurrentTime(): String {
        val sDateFormat = SimpleDateFormat("yyyy_MM_ddHHmmssSSS", Locale.CHINA)
        return sDateFormat.format(Date())
    }

    fun isEmpty(str: String?): Boolean {
        if (str == null || str == "" || str == "null") {
            return true
        }
        return false
    }
}
