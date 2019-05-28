package com.xunlei.downloadlib.bean

import org.litepal.crud.LitePalSupport
import java.io.Serializable

/**
 * ================================================
 * 框架要求框架中的每个 {@link } 都需要实现此类,以满足规范
 *
 * @function 作用
 * Created by Joe_ZBB on 2019/3/21 2:48 PM
 * ================================================
 */
class TsDownloadInfo(var path: String?="", var url: String?="", var num: Int?=0,var name:String?="") : LitePalSupport(), Serializable