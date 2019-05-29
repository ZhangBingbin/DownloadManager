package com.xunlei.downloadmanager

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import com.blankj.utilcode.util.LogUtils
import com.xunlei.downloadlib.bean.DownloadInfo
import com.xunlei.downloadlib.database.DownloadDaoManager
import com.xunlei.downloadlib.status.DownloadStatus
import com.xunlei.downloadlib.util.DownloadUtil
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.fragment_downloading.*
import java.util.concurrent.TimeUnit

/**
 * ================================================
 * 框架要求框架中的每个 {@link } 都需要实现此类,以满足规范
 *
 * @function 作用
 * Created by Joe_ZBB on 2019-05-27 19:07
 * ================================================
 */
class DownloadingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_downloading)
        initData()
    }

    private lateinit var mDownloadingAdapter: DownloadingAdapter
    private lateinit var mDownloadingList: MutableList<DownloadInfo>

    private var mDisposable: Disposable? = null


    @SuppressLint("CheckResult")
    private fun initData() {
        mDownloadingList = DownloadDaoManager.INSTANCE.loadDownloadList()
        mDownloadingAdapter = DownloadingAdapter()
        downloading_recyclerView.layoutManager = LinearLayoutManager(this)
        mDownloadingAdapter.setNewData(mDownloadingList)
        mDownloadingAdapter.bindToRecyclerView(downloading_recyclerView)
        downloading_recyclerView.adapter = mDownloadingAdapter

        mDownloadingAdapter.setOnItemClickListener { adapter, _, position ->
            val downloadInfo = adapter.data[position] as DownloadInfo
            when (downloadInfo.status) {
                DownloadStatus.DOWNLOADING.value -> {
                    DownloadUtil.INSTANCE.pause(downloadInfo)
                }
                else -> {
                    LogUtils.eTag("ZBB","恢复下载")
                    DownloadUtil.INSTANCE.startDownload(downloadInfo.downloadUrl,{
                    },true)
                }
            }
        }

        mDisposable = Flowable.interval(0, 1000, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                val list = DownloadDaoManager.INSTANCE.loadAllDownloadingList()
                mDownloadingAdapter.replaceData(list)
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        mDisposable?.dispose()
    }

    companion object {
        fun actionDownloading(activity: Activity) {
            val intent = Intent(activity, DownloadingActivity::class.java)
            activity.startActivity(intent)
        }
    }

}