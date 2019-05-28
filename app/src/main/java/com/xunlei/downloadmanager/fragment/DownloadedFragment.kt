package com.xunlei.downloadmanager.fragment

import android.support.v7.widget.LinearLayoutManager
import android.view.View
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.xunlei.downloadlib.bean.DownloadInfo
import com.xunlei.downloadlib.database.DownloadDaoManager
import com.xunlei.downloadlib.event.DownloadEvent
import com.xunlei.downloadlib.status.DownloadStatus
import com.xunlei.downloadmanager.R
import kotlinx.android.synthetic.main.fragment_downloaded.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * ================================================
 * 框架要求框架中的每个 {@link } 都需要实现此类,以满足规范
 *
 * @function 作用
 * Created by Joe_ZBB on 2019-05-27 16:12
 * ================================================
 */
class DownloadedFragment : BaseFragment() {

    private lateinit var mDownloadedAdapter:DownloadedAdapter

    override fun attachLayoutRes(): Int {
        return R.layout.fragment_downloaded
    }

    override fun useEventBus(): Boolean {
        return true
    }

    override fun initView(view: View) {
    }

    override fun lazyLoad() {
        val downloadedList = DownloadDaoManager.INSTANCE.loadDownloadedList()
        mDownloadedAdapter = DownloadedAdapter()
        downloaded_recyclerView.layoutManager=LinearLayoutManager(activity)
        mDownloadedAdapter.setNewData(downloadedList)
        mDownloadedAdapter.bindToRecyclerView(downloaded_recyclerView)
        downloaded_recyclerView.adapter=mDownloadedAdapter
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun updateDownloadedData(downloadEvent: DownloadEvent) {
        if (downloadEvent.status == DownloadStatus.COMPLETE.value) {
            //更新下载完成
            mDownloadedAdapter.setNewData(DownloadDaoManager.INSTANCE.loadDownloadedList())
        }
    }

    inner class DownloadedAdapter:BaseQuickAdapter<DownloadInfo,BaseViewHolder>(R.layout.item_downloaded){
        override fun convert(helper: BaseViewHolder, item: DownloadInfo) {
            helper.setText(R.id.tv_downloaded_name,item.videoName)
            helper.setText(R.id.tv_downloaded_size,"${item.totalSize}")
        }

    }
}