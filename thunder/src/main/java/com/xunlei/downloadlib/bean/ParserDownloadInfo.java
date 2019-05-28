package com.xunlei.downloadlib.bean;

import java.io.Serializable;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * ================================================
 * 框架要求框架中的每个 {@link } 都需要实现此类,以满足规范
 *
 * @function 作用
 * Created by Joe_ZBB on 2019-05-17 10:22
 * ================================================
 */
public class ParserDownloadInfo implements Serializable {

    //视频名称
    public String name;
    //视频下载地址
    public String downloadUrl;
    //视频长度
    public long size;
    //时长
    public double duration;
    //当前状态
    public int status;
    //网页地址
    public String webUrl;
    //清晰度
    public String sharpness;
    //是否高速下载
    public boolean fastDownload;
    //第一个ts大小
    public long firstShardSize;
    //m3u8加密Key地址
    public String keyUrl;
    //完整的m3u8信息
    public String content;
    //ts 集合
    public LinkedBlockingQueue<String> shardLinkQueue;

    public ParserDownloadInfo(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public ParserDownloadInfo() {
    }

}
