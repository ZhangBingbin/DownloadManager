package com.zbb.downloadlibrary.util;

import android.annotation.TargetApi;
import android.os.Build;
import android.text.TextUtils;
import com.zbb.downloadlibrary.bean.DownloadInfo;
import com.zbb.downloadlibrary.bean.ParserDownloadInfo;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class M3u8ParserUtil {

    private String pathQuery = "";
    private Map<String, String> urlPrefixMap = new HashMap<>();
    private ConcurrentHashMap<String, ParserDownloadInfo> m3u8VariantMap = new ConcurrentHashMap<>();

    private OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();

    private final static Pattern pattern = Pattern.compile("\\d+X\\d+|\\d+P");
    private Headers.Builder headersBuilder = new Headers.Builder()
            .add("connection", "keep-alive")
            .add("keep-alive", "max=20,timeout=120")
            .add("User-Agent", "Mozilla/5.0 (Linux; Android 8.0.0; Pixel 2 XL Build/OPD1.170816.004) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683.75 Mobile Safari/537.36");


    private byte[] sendGetRequest(Headers headers, String url) {
        Request request = new Request.Builder().url(url).headers(headers).build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                return response.body().bytes();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean sendHeadRequest(Headers headers, String url) {
        Request request = new Request.Builder().url(url).headers(headers).build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            return response.isSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private String sendGETRequest(Headers headers, String url) {
        byte[] bytes = sendGetRequest(headers, url);
        return bytes != null ? new String(bytes) : null;
    }

    private void generalWayDerivedUrlPrefix(String prefixFlag, String[] defaultPath, String variantUri) {
        Headers headers = headersBuilder.build();
        for (String prefix : defaultPath) {
            if (sendHeadRequest(headers, prefix + variantUri)) {
                urlPrefixMap.put(prefixFlag, prefix);
            }
        }
    }

    /**
     * 推导 ( 子M3U8 / TS ) 链接中的前缀部分
     */
    private synchronized void derivedVariantUrlPrefix(String prefixFlag, String m3u8Url, String variantUri) {
        variantUri = variantUri.contains("?") ? variantUri : variantUri + pathQuery;
        if (urlPrefixMap.get(prefixFlag) == null) {
            int diff = variantUri.startsWith("/") ? 0 : 1, startWith = m3u8Url.indexOf("/", 8) + diff;
            String domain = m3u8Url.substring(0, startWith),
                    maxSubstring = maxSubstring(m3u8Url, variantUri),
                    path = m3u8Url.substring(startWith, m3u8Url.indexOf(".m3u8")),
                    generalPath = domain + path.substring(0, path.lastIndexOf("/") + diff),
                    defaultPath[] = (maxSubstring.length() > 2 && variantUri.startsWith(maxSubstring)) ?
                            new String[]{(m3u8Url.replace(maxSubstring, "") + variantUri), generalPath, domain} :
                            ((variantUri.length() > path.length() / 2) ? new String[]{domain, generalPath} : new String[]{generalPath, domain});

            Headers headers = headersBuilder.add("Referer", domain).build();

            this.generalWayDerivedUrlPrefix(prefixFlag, defaultPath, variantUri);

            if (urlPrefixMap.get("prefixFlag") == null) {
                String[] pathVar = path.split("/");
                for (String param : pathVar) {
                    if (!TextUtils.isEmpty(param)) {
                        domain = domain + "/" + param + (diff == 0 ? "" : "/");
                        if (sendHeadRequest(headers, domain + variantUri)) {
                            urlPrefixMap.put(prefixFlag, domain);
                        }
                    }
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    public ConcurrentHashMap<String, ParserDownloadInfo> recursiveParserM3u8File(String m3u8Url, String resolution) {
        try {
            pathQuery = m3u8Url.contains("?") ? m3u8Url.substring(m3u8Url.indexOf('?')) : "";
            String content = this.sendGETRequest(headersBuilder.build(), m3u8Url);
            if (!TextUtils.isEmpty(content) && content.startsWith("#EXTM3U")) {
                if (content.contains("RESOLUTION")) {
                    Arrays.asList(content.split("#")).parallelStream().forEach(variant -> {
                        Matcher matcher = pattern.matcher(variant.toUpperCase());
                        String resolutionTemp = matcher.find() ? matcher.group(0) : (variant.substring(variant.lastIndexOf("/") + 1).replaceAll("[\\n\\r]", ""));
                        int size = variant.length(), index = variant.indexOf("\n");
                        if (index > -1 && index + 1 < size) {
                            variant = variant.substring(index + 1).replaceAll("[\\n\\r]", "");
                            if (variant.startsWith("http")) {
                                m3u8VariantMap.put(resolutionTemp, new ParserDownloadInfo(variant));
                                this.recursiveParserM3u8File(variant, resolutionTemp);
                            } else {
                                derivedVariantUrlPrefix("M3U8Prefix", m3u8Url, variant);
                                if (urlPrefixMap.get("M3U8Prefix") != null) {
                                    variant = variant.contains("?") ? variant : variant + pathQuery;
                                    variant = urlPrefixMap.get("M3U8Prefix") + variant;
                                    this.m3u8VariantMap.put(resolutionTemp, new ParserDownloadInfo(variant));
                                    this.recursiveParserM3u8File(variant, resolutionTemp);
                                }
                            }
                        }
                    });
                } else {
                    if (TextUtils.isEmpty(content)) {
                        m3u8VariantMap.remove(resolution);
                    } else {
                        if (resolution.equals("Default")) {
                            Matcher matcher = pattern.matcher(m3u8Url.toUpperCase());
                            resolution = matcher.find() ? matcher.group(0) : (m3u8Url.substring(m3u8Url.lastIndexOf("/") + 1).replaceAll("[\\n\\r]", ""));
                            m3u8VariantMap.put(resolution, new ParserDownloadInfo(m3u8Url));
                        }
                        if (m3u8VariantMap.get(resolution) != null) {
                            Objects.requireNonNull(m3u8VariantMap.get(resolution)).content = content;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return m3u8VariantMap;
    }


    public void parallelParserM3u8Content(DownloadInfo downloadInfo) {
        if (!TextUtils.isEmpty(downloadInfo.getM3u8Content())) {
            String[] variants = downloadInfo.getM3u8Content().split("#");
            LinkedBlockingQueue<String> shardLinkQueue = new LinkedBlockingQueue<>();
            for (String variant : variants) {
                int size = variant.length(), index = variant.indexOf("\n");
                if (variant.startsWith("EXT-X-KEY:")) {
                    String variantUP = variant.toUpperCase();
                    int start = variantUP.indexOf("URI=\"") + "URI=\"".length();
                    String uri = variant.substring(start, variantUP.indexOf("\"", start));
                    if (uri.startsWith("http")) {
                        downloadInfo.setKeyUrl(uri);
                    } else {
                        derivedVariantUrlPrefix("KEYPrefix", downloadInfo.getDownloadUrl(), uri);
                        if (Util.INSTANCE.isEmpty(urlPrefixMap.get("KEYPrefix"))) break;
                        downloadInfo.setKeyUrl(urlPrefixMap.get("KEYPrefix") + uri);
                    }
                    if (!Util.INSTANCE.isEmpty(downloadInfo.getKeyUrl())) {
                        downloadInfo.setM3u8Content(downloadInfo.getM3u8Content().replace(uri, downloadInfo.getKeyUrl()));
                    }
                } else {
                    if (index > -1 && index + 1 < size) {
                        variant = variant.substring(index + 1).replaceAll("[\\n\\r]", "");
                        if (variant.startsWith("http")) {
                            shardLinkQueue.add(variant);
                        } else {
                            derivedVariantUrlPrefix("TSPrefix", downloadInfo.getDownloadUrl(), variant);
                            if (Util.INSTANCE.isEmpty(urlPrefixMap.get("TSPrefix"))) break;
                            variant = variant.contains("?") ? variant : variant + pathQuery;
                            shardLinkQueue.add(urlPrefixMap.get("TSPrefix") + variant);
                        }
                        if (!Util.INSTANCE.isEmpty(urlPrefixMap.get("TSPrefix"))) {
                            downloadInfo.setM3u8Content(downloadInfo.getM3u8Content().replace(variant, urlPrefixMap.get("TSPrefix") + variant));
                        }
                    }
                }
            }
            downloadInfo.setTsList(shardLinkQueue);
        }
    }

    private String maxSubstring(String s1, String s2) {
        String max = (s1.length() > s2.length()) ? s1 : s2;
        String min = max.equals(s1) ? s2 : s1;
        for (int i = 0; i < min.length(); i++) {
            for (int m = 0, n = min.length() - i; n != min.length() + 1; m++, n++) {
                String sub = min.substring(m, n);
                if (max.contains(sub)) {
                    return sub;
                }
            }
        }
        return "";
    }
}
