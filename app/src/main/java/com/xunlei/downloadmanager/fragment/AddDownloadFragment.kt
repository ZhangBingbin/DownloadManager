package com.xunlei.downloadmanager.fragment

import android.view.View
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
    override fun attachLayoutRes(): Int {
        return R.layout.fragment_add_download
    }

    override fun initView(view: View) {
    }

    override fun lazyLoad() {
        btn_start_download.setOnClickListener {
            val url = ed_input_url.text.toString().trim()
            DownloadUtil.INSTANCE.startDownload(url)
        }

        btn_check_downloading.setOnClickListener {
            DownloadingActivity.actionDownloading(activity!!)
        }
    }

}