package com.xunlei.downloadlib.util

import android.text.TextUtils
import com.blankj.utilcode.util.LogUtils
import com.xunlei.downloadlib.bean.DownloadInfo
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.*

/**
 * ================================================
 * 框架要求框架中的每个 {@link } 都需要实现此类,以满足规范
 *
 * @function 作用
 * Created by Joe_ZBB on 2019-05-28 10:00
 * ================================================
 */
object M3u8Parser {

    private val DO_NOT_VERIFY: HostnameVerifier = HostnameVerifier { _, _ -> true }

    fun parserM3u8(downloadInfo: DownloadInfo): DownloadInfo {
        val url = downloadInfo.downloadUrl
        val content = doGetRequest(url)
        downloadInfo.m3u8Content = content
        val basePath = url.substring(0, url.lastIndexOf("/") + 1)
        if (!TextUtils.isEmpty(content)) {
            val lines = content.split("\n")
            for (line in lines) {
                if (!TextUtils.isEmpty(line)) {
                    if (line.startsWith("#EXT-X-KEY")) {
                        //加密key
                        val variantUP = line.toUpperCase()
                        val start = variantUP.indexOf("URI=\"") + "URI=\"".length
                        val keyUrl = line.substring(start, variantUP.indexOf("\"", start))
                        if (keyUrl.startsWith("http")) {
                            downloadInfo.keyUrl = keyUrl
                        } else {
                            downloadInfo.keyUrl = basePath + keyUrl
                        }
                    } else if (!line.startsWith("#EXT")) {
                        //ts
                        val tsLine = if (line.startsWith("http")) {
                            line
                        } else {
                            basePath + line
                        }
                        downloadInfo.tsList.add(tsLine)
                    } else {
                        //其他
                    }
                }
            }
        }
        return downloadInfo
    }

    private fun doGetRequest(url: String): String {
        val buf = StringBuilder()
        var urlObject = URL(url)
        buf.append(urlObject.protocol).append("://").append(urlObject.host)
            .append(if (urlObject.port == -1 || urlObject.port != urlObject.defaultPort) "" else ":" + urlObject.port)
            .append(urlObject.path)
        val query = urlObject.query
        var isQueryExist = false
        if (!(query == null || query.isEmpty())) {
            buf.append("?")
            isQueryExist = true
        }
        if (!(query == null || query.isEmpty())) {
            buf.append(query)
            buf.append("&")
        }
        if (isQueryExist) {
            buf.deleteCharAt(buf.length - 1)
        }
        urlObject = URL(buf.toString())
        val conn: HttpURLConnection?
        if (urlObject.protocol.toUpperCase() == "HTTPS") {
            trustAllHosts()
            val https = urlObject.openConnection() as HttpsURLConnection
            https.hostnameVerifier = DO_NOT_VERIFY
            conn = https
        } else {
            conn = urlObject.openConnection() as HttpURLConnection
        }
        conn.setRequestProperty("User-Agent", Constants.ANDROID_USER_AGENT)
        conn.requestMethod = "GET"
        conn.connectTimeout = 5000
        conn.readTimeout = 10000
        if (conn.responseCode == 200) {
            val resultBuffer = StringBuffer()
            val inputStream = conn.inputStream
            val reader = InputStreamReader(inputStream, "UTF-8")
            val bufferedReader = BufferedReader(reader)
            val lines = bufferedReader.readLines()
            for (line in lines) {
                resultBuffer.append("$line\n")
            }
            return resultBuffer.toString()
        }
        return ""
    }

    private fun trustAllHosts() {
        // Create a trust manager that does not validate certificate chains
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {

            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return arrayOf()
            }

            @Throws(CertificateException::class)
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
            }

            @Throws(CertificateException::class)
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
            }
        })
        val sc = SSLContext.getInstance("TLS")
        sc.init(null, trustAllCerts, java.security.SecureRandom())
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
    }
}