/*
 * Copyright (c) 2016-2018. China Mobile Communications Corporation. All rights reserved.
 */

package com.chinamobile.iot.onenet.edp.sample.utility;

import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.chinamobile.iot.onenet.edp.sample.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class FileUtil {

    public static File[] listFiles(String path) {
        File parent = new File(path);
        return listFiles(parent);
    }

    public static File[] listFiles(File parent) {
        File[] files = parent.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return !filename.startsWith(".");
            }
        });
        if (files != null) {
            sort(files);
        }
        return files;
    }

    public static void sort(List<File> files) {
        Collections.sort(files, new Comparator<File>() {
            @Override
            public int compare(File lhs, File rhs) {
                if (lhs.isDirectory() && rhs.isFile()) {
                    return -1;
                } else if (lhs.isFile() && rhs.isDirectory()) {
                    return 1;
                } else {
                    return lhs.getName().compareToIgnoreCase(rhs.getName());
                }
            }
        });
    }

    public static void sort(File[] files) {
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File lhs, File rhs) {
                if (lhs.isDirectory() && rhs.isFile()) {
                    return -1;
                } else if (lhs.isFile() && rhs.isDirectory()) {
                    return 1;
                } else {
                    return lhs.getName().compareToIgnoreCase(rhs.getName());
                }
            }
        });
    }

    public static void deleteFile(String path) {
        if (path != null && path.trim().length() > 0) {
            File file = new File(path);
            deleteFile(file);
        }
    }

    public static void deleteFile(String dir, String name) {
        String path = dir + File.separator + name;
        deleteFile(path);
    }

    public static void deleteFile(File file) {
        if (file != null && file.exists()) {
            try {
                file.deleteOnExit();
            } finally {
                file.delete();
            }
        }
    }

    public static long getFileSize(String path) {
        if (path != null && path.trim().length() > 0) {
            File file = new File(path);
            return getFileSize(file);
        }
        return 0;
    }

    public static long getFileSize(File file) {
        if (file != null && file.exists()) {
            return file.length();
        }
        return 0;
    }

    /**
     * 格式化文件的大小
     */
    public static String formatFileSize(long size) {
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString;
        String wrongSize = "0B";
        if (size <= 0) {
            return wrongSize;
        }
        if (size < 1024) {
            fileSizeString = df.format((double) size) + "B";
        } else if (size < 1048576) {
            fileSizeString = df.format((double) size / 1024) + "KB";
        } else if (size < 1073741824) {
            fileSizeString = df.format((double) size / 1048576) + "MB";
        } else {
            fileSizeString = df.format((double) size / 1073741824) + "GB";
        }
        return fileSizeString;
    }

    public static String getSdcard() {
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    public static String getExSdcard() {
        String sdcard_path = null;
        String sd_default = getSdcard();
        if (sd_default.endsWith("/")) {
            sd_default = sd_default.substring(0, sd_default.length() - 1);
        }
        // 得到路径
        try {
            Runtime runtime = Runtime.getRuntime();
            Process proc = runtime.exec("mount");
            InputStream is = proc.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            String line;
            BufferedReader br = new BufferedReader(isr);
            while ((line = br.readLine()) != null) {
                if (line.contains("secure"))
                    continue;
                if (line.contains("asec"))
                    continue;
                if (line.contains("fat") && line.contains("/mnt/")) {
                    String columns[] = line.split(" ");
                    if (columns != null && columns.length > 1) {
                        if (sd_default.trim().equals(columns[1].trim())) {
                            continue;
                        }
                        sdcard_path = columns[1];
                    }
                } else if (line.contains("fuse") && line.contains("/mnt/")) {
                    String columns[] = line.split(" ");
                    if (columns != null && columns.length > 1) {
                        if (sd_default.trim().equals(columns[1].trim())) {
                            continue;
                        }
                        sdcard_path = columns[1];
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sdcard_path;
    }

    public static String getSuffix(String fileName) {
        if (!TextUtils.isEmpty(fileName)) {
            int index = fileName.lastIndexOf(".");
            if (index > 0) {
                return fileName.substring(index + 1);
            }
        }
        return null;
    }

    public static final String[] IMAGE_SUFFIXS = {"jpg", "png", "bmp", "jpeg", "gif", "psd", "tiff"};
    public static final String[] AUDIO_SUFFIXS = {"mp3", "wav", "wma", "off", "ape", "acc"};
    public static final String[] VIDEO_SUFFIXS = {"mpeg", "mpg", "dat", "avi", "mov", "asf", "wmv",
            "navi", "3gp", "ra", "rm", "mkv", "flv", "f4v", "rmvb", "webm"};
    public static final String[] COMPRESS_SUFFIXS = {"rar", "zip", "tar", "gz", "bz2", "cab", "z", "7z", "jar", "iso"};
    public static final String[] DOC_SUFFIX = {"doc", "docx"};
    public static final String[] XLS_SUFFIX = {"xls", "xlsx"};
    public static final String[] PPT_SUFFIX = {"ppt", "pptx"};
    public static final String[] TXT_SUFFIX = {"txt", "log", "html", "java", "h", "c", "cpp", "css", "js", "py", "xml", "htm"};
    public static final String[] APK_SUFFIX = {"apk"};

    public static final HashMap<String, Integer> DRAWABLE_MAP = new HashMap<>();
    public static Integer getDrawableId(File file) {
        if (file != null) {
            String suffix = getSuffix(file.getName());
            if (suffix != null) {
                return DRAWABLE_MAP.get(suffix);
            }
        }
        return null;
    }

    static {
        for (String suffix : IMAGE_SUFFIXS) {
            DRAWABLE_MAP.put(suffix, R.string.icon_font_pic);
        }
        for (String suffix : AUDIO_SUFFIXS) {
            DRAWABLE_MAP.put(suffix, R.string.icon_font_audio);
        }
        for (String suffix : VIDEO_SUFFIXS) {
            DRAWABLE_MAP.put(suffix, R.string.icon_font_video);
        }
        for (String suffix : COMPRESS_SUFFIXS) {
            DRAWABLE_MAP.put(suffix, R.string.icon_font_rar);
        }
        for (String suffix : DOC_SUFFIX) {
            DRAWABLE_MAP.put(suffix, R.string.icon_font_doc);
        }
        for (String suffix : XLS_SUFFIX) {
            DRAWABLE_MAP.put(suffix, R.string.icon_font_xls);
        }
        for (String suffix : PPT_SUFFIX) {
            DRAWABLE_MAP.put(suffix, R.string.icon_font_ppt);
        }
        for (String suffix : TXT_SUFFIX) {
            DRAWABLE_MAP.put(suffix, R.string.icon_font_txt);
        }
        for (String suffix : APK_SUFFIX) {
            DRAWABLE_MAP.put(suffix, R.string.icon_font_apk);
        }
    }

}
