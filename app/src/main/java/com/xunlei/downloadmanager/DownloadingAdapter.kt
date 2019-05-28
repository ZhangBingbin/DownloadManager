package com.xunlei.downloadmanager

import android.support.v7.widget.AppCompatTextView
import android.widget.ProgressBar
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.xunlei.downloadlib.bean.DownloadInfo
import com.xunlei.downloadlib.event.DownloadEvent
import com.xunlei.downloadlib.status.DownloadStatus

/**
 * ================================================
 * 框架要求框架中的每个 {@link } 都需要实现此类,以满足规范
 *
 * @function 作用
 * Created by Joe_ZBB on 2019-05-27 16:43
 * ================================================
 */
class DownloadingAdapter : BaseQuickAdapter<DownloadInfo, BaseViewHolder>(R.layout.item_downloading) {
    override fun convert(helper: BaseViewHolder, item: DownloadInfo) {
        helper.setText(R.id.tv_name, item.videoName)
        helper.setProgress(R.id.pb_downloading_bar, item.progress)
        val status = when (item.status) {
            DownloadStatus.WAIT.value -> {
                //开始下载
                "等待下载"
            }
            DownloadStatus.PAUSE.value -> {
                //暂停下载
                "已暂停"
            }
            DownloadStatus.DOWNLOADING.value -> {
                //正在下载
                "下载中"
            }
            DownloadStatus.ERROR.value -> {
                //下载错误
                "下载错误"
            }
            DownloadStatus.COMPLETE.value -> {
                //下载完成
                "下载完成"
            }
            else -> {
                "等待下载"
            }
        }
        helper.setText(R.id.tv_downloading_status, status)
        helper.setText(R.id.tv_downloading_speed, item.speed)
    }

    fun updateItemData(downloadInfo: DownloadInfo) {
        if (!data.isNullOrEmpty()) {
            repeat(data.size) {
                val info = data[it]
                if (downloadInfo.fileName == info.fileName) {
                    val view = getViewByPosition(it, R.id.ll_downloading_container)
                    val downloadingPb = view?.findViewById<ProgressBar>(R.id.pb_downloading_bar)
                    val tvStatus = view?.findViewById<AppCompatTextView>(R.id.tv_downloading_status)
                    val tvSpeed = view?.findViewById<AppCompatTextView>(R.id.tv_downloading_speed)
                    val status = when (downloadInfo.status) {
                        DownloadStatus.WAIT.value -> {
                            //开始下载
                            "等待下载"
                        }
                        DownloadStatus.PAUSE.value -> {
                            //暂停下载
                            "已暂停"
                        }
                        DownloadStatus.DOWNLOADING.value -> {
                            //正在下载
                            "下载中"
                        }
                        DownloadStatus.ERROR.value -> {
                            //下载错误
                            "下载错误"
                        }
                        DownloadStatus.COMPLETE.value -> {
                            //下载完成
                            "下载完成"
                        }
                        else -> {
                            "等待下载"
                        }
                    }
                    downloadingPb?.progress = downloadInfo.progress
                    tvStatus?.text = status
                    tvSpeed?.text = downloadInfo.speed
                }
            }
        }
    }
}