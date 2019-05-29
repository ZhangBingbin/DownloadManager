package com.xunlei.downloadmanager.fragment

import android.view.View
import com.blankj.utilcode.util.ToastUtils
import com.xunlei.downloadlib.status.DownloadStatus
import com.xunlei.downloadlib.util.DownloadUtil
import com.xunlei.downloadmanager.DownloadingActivity
import com.xunlei.downloadmanager.R
import kotlinx.android.synthetic.main.fragment_add_download.*

/**
 * ================================================
 * 框架要求框架中的每个 {@link } 都需要实现此类,以满足规范
 *
 * @function 作用
 * Created by Joe_ZBB on 2019-05-27 16:21
 * ================================================
 */
class AddDownloadFragment : BaseFragment() {

    private val mp4Url =
        "https://upos-hz-mirrorcosu.acgvideo.com/upgcxcode/02/97/93489702/93489702-1-6.mp4?e=ig8euxZM2rNcNbuBhwdVtWuBhwdVNEVEuCIv29hEn0l5QK==&deadline=1559097703&gen=playurl&nbs=1&oi=2102833519&os=cosu&platform=html5&trid=b4aa7dfd642e43cf87f7bd3e11b1edde&uipk=5&upsig=530d2f15d469cd69e093ef11bd8e5550&uparams=e,deadline,gen,nbs,oi,os,platform,trid,uipk"
    private val m3u8Url =
        "https://meiju6.qfxmj.com/ppvod/0BA82EE252DD7EF216B86C9035DBD357.m3u8"

    override fun attachLayoutRes(): Int {
        return R.layout.fragment_add_download
    }

    override fun initView(view: View) {
    }

    override fun lazyLoad() {
        btn_start_download.setOnClickListener {
            val url = ed_input_url.text.toString().trim()
            DownloadUtil.INSTANCE.startDownload(url, {
                when (it) {
                    DownloadStatus.COMPLETE.value -> {
                        //下载已完成
                        ToastUtils.showShort("下载已完成")
                    }
                    else -> {
                        //已在下载列表
                        ToastUtils.showShort("已在下载列表")
                    }
                }
            })
        }

        btn_mp4_download.setOnClickListener {
            DownloadUtil.INSTANCE.startDownload(mp4Url,{
                when (it) {
                    DownloadStatus.COMPLETE.value -> {
                        //下载已完成
                        ToastUtils.showShort("下载已完成")
                    }
                    else -> {
                        //已在下载列表
                        ToastUtils.showShort("已在下载列表")
                    }
                }
            })
        }

        btn_m3u8_download.setOnClickListener {
            DownloadUtil.INSTANCE.startDownload(m3u8Url,{
                when (it) {
                    DownloadStatus.COMPLETE.value -> {
                        //下载已完成
                        ToastUtils.showShort("下载已完成")
                    }
                    else -> {
                        //已在下载列表
                        ToastUtils.showShort("已在下载列表")
                    }
                }
            })
        }

        btn_check_downloading.setOnClickListener {
            DownloadingActivity.actionDownloading(activity!!)
        }
    }

}