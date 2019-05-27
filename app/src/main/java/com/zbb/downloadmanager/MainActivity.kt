package com.zbb.downloadmanager

import android.Manifest
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.yanzhenjie.permission.AndPermission
import com.zbb.downloadlibrary.util.DownloadUtil
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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

        btn_start_download.setOnClickListener {
            val url = ed_input_url.text.toString().trim()
            DownloadUtil.INSTANCE.startDownload(url)
        }
    }
}
