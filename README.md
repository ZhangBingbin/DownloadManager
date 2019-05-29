# 一,下载框架
1,支持mp4,ftp,torrent,文件下载,m3u8的下载
2,内置数据库,数据库使用的是LitePal 数据库模型为DownloadInfo,数据库管理类为DownloadDaoManager 对数据库进行操作

# 二,使用
1,初始化框架
在Application中
class BaseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DownloadUtil.INSTANCE.init(this)
    }
}

2,开始下载
DownloadUtil.INSTANCE
        .startDownload(url,{
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
            
3,暂停任务
DownloadUtil.INSTANCE.pause(downloadInfo)   

4,继续下载
 DownloadUtil.INSTANCE.startDownload(downloadInfo.downloadUrl,{},true)         