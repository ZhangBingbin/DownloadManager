package com.xunlei.downloadmanager

import android.Manifest
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentPagerAdapter
import android.support.v7.app.AppCompatActivity
import com.yanzhenjie.permission.AndPermission
import com.xunlei.downloadmanager.fragment.AddDownloadFragment
import com.xunlei.downloadmanager.fragment.DownloadedFragment
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var mAddFragment: AddDownloadFragment
    private lateinit var mDownloadedFragment: DownloadedFragment

    private var mFragmentList = mutableListOf<Fragment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mAddFragment = AddDownloadFragment()
        mDownloadedFragment = DownloadedFragment()

        mFragmentList.add(mAddFragment)
        mFragmentList.add(mDownloadedFragment)

        rd_add.isChecked = true
        index_pager.currentItem = 0
        index_pager.adapter = IndexPagerAdapter()


        AndPermission.with(this)
            .runtime()
            .permission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            .onGranted {

            }
            .onDenied {

            }
            .start()

        rg_index.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.rd_add -> {
                    rd_add.isChecked = true
                    index_pager.currentItem = 0
                }
                R.id.rd_downloaded -> {
                    rd_downloaded.isChecked = true
                    index_pager.currentItem = 1
                }
            }
        }
    }

    inner class IndexPagerAdapter : FragmentPagerAdapter(supportFragmentManager) {
        override fun getItem(position: Int): Fragment {
            return mFragmentList[position]
        }

        override fun getCount(): Int {
            return mFragmentList.size
        }

    }
}
