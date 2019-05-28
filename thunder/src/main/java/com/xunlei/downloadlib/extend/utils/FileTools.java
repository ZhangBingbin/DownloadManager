package com.xunlei.downloadlib.extend.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.os.Environment;
import android.text.TextUtils;

import java.io.*;

public class FileTools {
    public static final int FLAG_SHORTER = 1 << 0;
    public static final long KB_IN_BYTES = 1024;
    public static final long MB_IN_BYTES = KB_IN_BYTES * 1024;
    public static final long GB_IN_BYTES = MB_IN_BYTES * 1024;
    public static final long TB_IN_BYTES = GB_IN_BYTES * 1024;
    public static final long PB_IN_BYTES = TB_IN_BYTES * 1024;
    public static final String byteShort = "B";
    public static final String kilobyteShort = "KB";
    public static final String megabyteShort = "MB";
    public static final String gigabyteShort = "GB";
    public static final String terabyteShort = "TB";
    public static final String petabyteShort = "PB";
    private static final String TAG = "FileTools";

    public static String getFileSuffix(String name) {
        return name.substring(name.lastIndexOf(".") + 1).toUpperCase();
    }

    /**
     * 获取SD卡路径
     *
     * @return
     */
    public static String getSDCardPath() {
        return Environment.getExternalStorageDirectory().getAbsolutePath()
                + File.separator;
    }

    /**
     * 获取系统存储路径
     *
     * @return
     */
    public static String getRootDirectoryPath() {
        return Environment.getRootDirectory().getAbsolutePath();
    }

    /**
     * 格式化文件大小为可读字符串
     *
     * @param fileSize   long形式的文件大小
     * @param formatSort true表示只保留一位小数点
     * @return
     */
    public static String getReadableFileSize(long fileSize, boolean formatSort) {
        if (formatSort) {
            return formatShortFileSize(fileSize);
        } else {
            return formatFileSize(fileSize);
        }
    }

    /**
     * 获取文件大小
     *
     * @param file
     * @return
     * @throws Exception
     */
    public static long getFileSize(File file) {
        if (!file.exists()) {
            return 0;
        }
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            return fis.available();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                closeQuietly(fis);
            }
        }

        return 0;

    }

    /**
     * 获取文件最新修改时间
     *
     * @param filePath
     * @return
     */
    public static long getFileModifyTime(String filePath) {
        if (null == filePath) {
            return 0;
        }
        File file = new File(filePath);
        if (file == null || !file.exists()) {
            return 0;
        }
        return file.lastModified();
    }

    /**
     * 判断输入文件是否存在
     *
     * @param path
     * @return
     */
    public static boolean exists(String path) {
        if (TextUtils.isEmpty(path) || TextUtils.isEmpty(path)) {
            return false;
        }

        return new File(path).exists();
    }

    /**
     * 复制文件,只能复制非目录文件，需要复制目录请使用{@link #copyDir(String, String)}
     *
     * @param srcFile 需要复制的文件
     * @param dstFile 复制之后的文件
     * @return 复制成功返回true，否则返回false
     * @throws IOException
     */
    public static boolean copyFile(String srcFile, String dstFile) throws IOException {

        if (TextUtils.isEmpty(srcFile) || TextUtils.isEmpty(dstFile)) {
            return false;
        }

        File destFile = new File(dstFile);
        File sFile = new File(srcFile);

        if (!sFile.exists()) {
            return false;
        }

        if (destFile.exists()) {
            destFile.delete();
        }
        FileInputStream inputStream = null;
        FileOutputStream out = null;
        try {
            inputStream = new FileInputStream(sFile);
            out = new FileOutputStream(destFile);
        } catch (FileNotFoundException e) {

            return false;
        }
        try {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) >= 0) {
                out.write(buffer, 0, bytesRead);
            }
        } finally {
            out.flush();
            try {
                out.getFD().sync();
            } catch (IOException e) {

            }
            out.close();
        }
        return true;
    }

    /**
     * 复制目录
     *
     * @param srcDir 源目录
     * @param dstDir 目标目录
     * @return
     * @throws IOException
     */
    public static boolean copyDir(String srcDir, String dstDir) throws IOException {
        if (TextUtils.isEmpty(srcDir) || TextUtils.isEmpty(dstDir)) {
            return false;
        }

        File srcDirFile = new File(srcDir);
        if (!srcDirFile.exists()) {
            return false;
        }
        if (!srcDirFile.isDirectory()) {
            return false;
        }
        File dstDirFile = new File(dstDir);
        if (!dstDirFile.exists()) {
            dstDirFile.mkdirs();
        }
        File[] listFiles = srcDirFile.listFiles();

        if (listFiles == null || listFiles.length == 0) {
            return false;
        }
        for (File child : listFiles) {
            if (child.isFile()) {
                copyFile(child.getAbsolutePath(), dstDirFile.getAbsolutePath() +
                        File.separator + child.getName());
            } else {
                copyDir(child.getAbsolutePath(), dstDirFile.getAbsolutePath() +
                        File.separator + child.getName());
            }
        }
        return true;
    }

    /**
     * 文件剪切
     *
     * @param srcFile
     * @param dstFile dstFile必须是最后剪切之后的完整文件路径名
     * @return
     */
    public static boolean moveFile(String srcFile, String dstFile) {
        if (TextUtils.isEmpty(srcFile) || TextUtils.isEmpty(dstFile)) {
            return false;
        }
        File src = new File(srcFile);
        File dst = new File(dstFile);
        if (!src.exists()) {
            return false;
        }
        int srcStorageIndex = src.getAbsolutePath().indexOf("/", 1);
        int dstStorageIndex = dst.getAbsolutePath().indexOf("/", 1);
        String srcStorage = srcFile.substring(0, srcStorageIndex);
        String dstStorage = dstFile.substring(0, dstStorageIndex);
        //如果是同磁盘剪切文件，直接使用rename即可，提高剪切速度，不同磁盘间使用
        //先拷贝后删除的操作
        if (!srcStorage.equals(dstStorage)) {
            return src.renameTo(new File(dstFile));
        } else {
            try {
                boolean isDir = src.isDirectory();
                boolean success = isDir ? copyDir(srcFile, dstFile) : copyFile(srcFile, dstFile);
                if (success) {
                    if (isDir) {
                        deleteDir(srcFile);
                    } else {
                        deleteFile(srcFile);
                    }
                }
                return success;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 重命名文件
     *
     * @param oldFile
     * @param newName
     * @return
     */
    public static boolean renameFile(File oldFile, String newName) {
        if (TextUtils.isEmpty(newName)) {
            return false;
        }
        if (oldFile.exists()) {
            return oldFile.renameTo(new File(oldFile.getParent(), newName));
        }
        return false;
    }

    /**
     * 删除目标文件，该文件不能是目录，如果是目录请使用{@link #deleteDir(String)}
     *
     * @param srcFile
     * @return
     */
    public static boolean deleteFile(String srcFile) {
        if (TextUtils.isEmpty(srcFile) || TextUtils.isEmpty(srcFile)) {
            return false;
        }
        File file = new File(srcFile);
        if (file.exists() && !file.isDirectory()) {
            return file.delete();
        }
        return false;
    }

    /**
     * 删除指定路径的目录，包括该目录的子目录以及子文件
     *
     * @param dirFile
     * @return
     */
    public static boolean deleteDir(String dirFile) {
        if (TextUtils.isEmpty(dirFile) || TextUtils.isEmpty(dirFile)) {
            return false;
        }
        File oldFile = new File(dirFile);
        File file = new File(oldFile.getAbsolutePath() + System.currentTimeMillis());
        oldFile.renameTo(file);
        if (file == null || !file.exists()) {
            return false;
        }
        if (file.isDirectory()) {
            File[] list = file.listFiles();
            for (int i = 0; i < list.length; i++) {
                if (list[i].isDirectory()) {
                    deleteDir(list[i].getAbsolutePath());
                } else {
                    list[i].delete();
                }
            }
        }

        file.delete();
        return true;
    }

    /**
     * 根据指定文件名创建文件
     *
     * @param file
     * @return
     */
    public static boolean makeNewFile(String file) {
        if (TextUtils.isEmpty(file)) {
            return false;
        }
        File newFile = new File(file);
        boolean success = false;
        if (newFile.exists()) {
            return true;
        } else {
            try {
                success = newFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                success = false;
            }
        }
        return success;
    }

    public static boolean mkdirs(String path) {
        if (!exists(path)) {
            if (!(new File(path).mkdirs())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 在指定目录中创建一些子目录
     *
     * @param parentPath 自定父目录
     * @param dirNames   需要创建的子目录数组
     * @return
     */
    public static boolean mkdirsInParent(String parentPath, String... dirNames) {

        if (!exists(parentPath)) {
            if (!(new File(parentPath).mkdirs())) {
                return false;
            }
        }
        File childDirFile = null;
        boolean success = false;

        for (String child : dirNames) {
            childDirFile = new File(parentPath, child);
            success = childDirFile.mkdirs();
            if (!success) {
            }
        }

        return success;
    }


    /**
     * 批量创建文件目录，需要指定目录完整路径：
     * 例如：
     * <code>sdcard/Android/TestDirName</code>
     *
     * @param dirNames 需要创建的目录路径集合
     * @return
     */
    public static boolean mkdirs(String... dirNames) {
        File childDirFile = null;
        boolean success = false;
        for (String dirPath : dirNames) {
            childDirFile = new File(dirPath);
            success = childDirFile.mkdirs();
            if (!success) {
            }
        }
        return success;
    }

    /**
     * 判断sdcard是否已经挂载
     *
     * @param context
     * @return
     */
    public static boolean hasSdcard(Context context) {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED;
    }

    /**
     * 检查目标文件是否有读取权限
     *
     * @param targetFile
     * @return
     */
    public static boolean checkFileReadPermission(String targetFile) {
        if (TextUtils.isEmpty(targetFile)) {
            return false;
        }
        File file = new File(targetFile);
        if (!file.exists()) {
            return false;
        }
        return file.canRead();
    }

    /**
     * 检查目标文件是否有写权限
     *
     * @param targetFile
     * @return
     */
    public static boolean checkFileWritePermission(String targetFile) {
        if (TextUtils.isEmpty(targetFile)) {
            return false;
        }
        File file = new File(targetFile);
        if (!file.exists()) {
            return false;
        }
        return file.canWrite();
    }


    /**
     * 关闭可关闭的数据
     *
     * @param closeable
     */
    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                // Ignore.
            }
        }
    }


    static String formatFileSize(long sizeBytes) {
        return formatBytes(sizeBytes, 0);
    }

    static String formatShortFileSize(long sizeBytes) {
        return formatBytes(sizeBytes, FLAG_SHORTER);
    }

    static String formatBytes(long sizeBytes, int flags) {
        final boolean isNegative = (sizeBytes < 0);
        float result = isNegative ? -sizeBytes : sizeBytes;
        String suffix = byteShort;
        long mult = 1;
        if (result > 900) {
            suffix = kilobyteShort;
            mult = KB_IN_BYTES;
            result = result / 1024;
        }
        if (result > 900) {
            suffix = megabyteShort;
            mult = MB_IN_BYTES;
            result = result / 1024;
        }
        if (result > 900) {
            suffix = gigabyteShort;
            mult = GB_IN_BYTES;
            result = result / 1024;
        }
        if (result > 900) {
            suffix = terabyteShort;
            mult = TB_IN_BYTES;
            result = result / 1024;
        }
        if (result > 900) {
            suffix = petabyteShort;
            mult = PB_IN_BYTES;
            result = result / 1024;
        }
        // Note we calculate the rounded long by ourselves, but still let String.format()
        // compute the rounded value. String.format("%f", 0.1) might not return "0.1" due to
        // floating point errors.
        final int roundFactor;
        final String roundFormat;
        if (mult == 1 || result >= 100) {
            roundFactor = 1;
            roundFormat = "%.0f";
        } else if (result < 1) {
            roundFactor = 100;
            roundFormat = "%.2f";
        } else if (result < 10) {
            if ((flags & FLAG_SHORTER) != 0) {
                roundFactor = 10;
                roundFormat = "%.1f";
            } else {
                roundFactor = 100;
                roundFormat = "%.2f";
            }
        } else { // 10 <= result < 100
            if ((flags & FLAG_SHORTER) != 0) {
                roundFactor = 1;
                roundFormat = "%.0f";
            } else {
                roundFactor = 100;
                roundFormat = "%.2f";
            }
        }

        if (isNegative) {
            result = -result;
        }
        final String roundedString = String.format(roundFormat, result);

        return roundedString + suffix;
    }

    public static String convertFileSize(long size) {
        long kb = 1024;
        long mb = kb * 1024;
        long gb = mb * 1024;

        if (size >= gb) {
            return String.format("%.1f GB", (float) size / gb);
        } else if (size >= mb) {
            float f = (float) size / mb;
            return String.format(f > 100 ? "%.0f M" : "%.1f M", f);
        } else if (size >= kb) {
            float f = (float) size / kb;
            return String.format(f > 100 ? "%.0f K" : "%.1f K", f);
        } else
            return String.format("%d B", size);
    }

    public static String getFileNameWithoutSuffix(File file) {
        if (file.isDirectory()) {
            return file.getName();
        }
        String file_name = file.getName();
        return file_name.substring(0, file_name.lastIndexOf("."));
    }

    public static String getFileNameWithoutSuffix(String path) {
        return getFileNameWithoutSuffix(new File(path));
    }

    public static Bitmap getVideoThumbnail(String videoPath, int width, int height, int kind) {
        Bitmap bitmap = null;
        // 获取视频的缩略图
        bitmap = ThumbnailUtils.createVideoThumbnail(videoPath, kind);
        if (bitmap != null) {
            bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height,
                    ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        }
        return bitmap;
    }

    //根据路径得到视频缩略图
    public static Bitmap getVideoThumbnail(String videoPath) {
        if (!new File(videoPath).exists()) return null;
        MediaMetadataRetriever media = new MediaMetadataRetriever();
        media.setDataSource(videoPath);
        Bitmap bitmap = media.getFrameAtTime();
        return bitmap;
    }

    //获取视频总时长
    public static int getVideoDuration(String path) {
        MediaMetadataRetriever media = new MediaMetadataRetriever();
        media.setDataSource(path);
        String duration = media.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION); //
        return Integer.parseInt(duration);
    }

    public static String getFileName(String path) {
        return path.substring(path.lastIndexOf("/") + 1);
    }

    public static boolean isVideoFile(String fileName) {
        if (null == fileName) return false;
        int pos = fileName.lastIndexOf('.');
        String extensionName = (pos >= 0) ? fileName.substring(pos + 1) : null;
        return (null != extensionName && VideoFileHelper.isSupportedVideoFileExtension(extensionName));
    }

//    public static String saveBitmap(Bitmap bm, String picName) {
//
//
//        try {
//            File file = new File(Const.VIDEO_PIC_PATH);
//            if (!file.exists()) file.mkdirs();
//            File f = new File(Const.VIDEO_PIC_PATH, picName);
//            if (!f.exists()) {
//                f.createNewFile();
//            }
//            FileOutputStream out = new FileOutputStream(f);
//            bm.compress(Bitmap.CompressFormat.PNG, 90, out);
//            out.flush();
//            out.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//            return null;
//        }
//        return Const.VIDEO_PIC_PATH + File.separator + picName;
//    }

//    public static int getFileIcon(String fileName) {
//        if (null == fileName) {
//            return R.drawable.ic_unknow;
//        }
//        String suffix = fileName.substring(fileName.lastIndexOf(".") + 1).toUpperCase();
//        if (fileName.indexOf(".") < 0) {
//            return R.drawable.ic_floder;
//        } else if (isVideoFile(fileName)) {
//            return R.drawable.ic_video;
//        } else if ("DOC".equals(suffix) || "DOCX".equals(suffix)) {
//            return R.drawable.ic_doc;
//        } else if ("XLS".equals(suffix) || "XLSX".equals(suffix)) {
//            return R.drawable.ic_excel;
//        } else if ("PPT".equals(suffix) || "PPTX".equals(suffix)) {
//            return R.drawable.ic_ppt;
//        } else if (".RAR.ZIP.7Z.ISO".indexOf(suffix.toUpperCase()) > -1) {
//            return R.drawable.ic_rar;
//        } else if (".MP3.WMA.FLAC.AAC.MMF.AMR.M4A.M4R.OGG.MP2.WAV.WV".indexOf(suffix.toUpperCase()) > -1) {
//            return R.drawable.ic_music;
//        } else if (".BMP.JPEG.GIF.PSD.PNG.TIFF.TGA.EPS,JPG".indexOf(suffix.toUpperCase()) > -1) {
//            return R.drawable.ic_pic;
//        } else if (".HTML.PHP.JSP.MHT.HTM".indexOf(suffix.toUpperCase()) > -1) {
//            return R.drawable.ic_web;
//        } else if ("EXE".equals(suffix)) {
//            return R.drawable.ic_exe;
//        } else if ("TXT".equals(suffix)) {
//            return R.drawable.ic_txt;
//        } else if ("EXE".equals(suffix)) {
//            return R.drawable.ic_exe;
//        } else if ("APK".equals(suffix)) {
//            return R.drawable.ic_apk;
//        } else if ("TORRENT".equals(suffix)) {
//            return R.drawable.ic_bticon;
//        } else if ("URL".equals(suffix)) {
//            return R.drawable.ic_urlicon;
//        }
//        return R.drawable.ic_unknow;
//    }
}
