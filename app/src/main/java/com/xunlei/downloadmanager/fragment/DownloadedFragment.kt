package com.xunlei.downloadmanager.fragment

import android.support.v7.widget.LinearLayoutManager
import android.view.View
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.xunlei.downloadlib.bean.DownloadInfo
import com.xunlei.downloadlib.database.DownloadDaoManager
import com.xunlei.downloadmanager.R
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.fragment_downloaded.*
import java.util.concurrent.TimeUnit

/**
 * ================================================
 * 框架要求框架中的每个 {@link } 都需要实现此类,以满足规范
 *
 * @function 作用
 * Created by Joe_ZBB on 2019-05-27 16:12
 * ================================================
 */
class DownloadedFragment : BaseFragment() {

    private lateinit var mDownloadedAdapter: DownloadedAdapter
    private var mDisposable:Disposable?=null

    override fun attachLayoutRes(): Int {
        return R.layout.fragment_downloaded
    }


    override fun initView(view: View) {
    }

    override fun lazyLoad() {
        val downloadedList = DownloadDaoManager.INSTANCE.loadAllDownloadedList()
        mDownloadedAdapter = DownloadedAdapter()
        downloaded_recyclerView.layoutManager = LinearLayoutManager(activity)
        mDownloadedAdapter.setNewData(downloadedList)
        mDownloadedAdapter.bindToRecyclerView(downloaded_recyclerView)
        downloaded_recyclerView.adapter = mDownloadedAdapter
    }

    override fun initData() {
        super.initData()
        mDisposable = Flowable.interval(0, 1000, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                val list = DownloadDaoManager.INSTANCE.loadAllDownloadedList()
                mDownloadedAdapter.replaceData(list)
            }
    }

    inner class DownloadedAdapter : BaseQuickAdapter<DownloadInfo, BaseViewHolder>(R.layout.item_downloaded) {
        override fun convert(helper: BaseViewHolder, item: DownloadInfo) {
            helper.setText(R.id.tv_downloaded_name, item.videoName)
            helper.setText(R.id.tv_downloaded_size, "${item.totalSize}")
        }
    }
}