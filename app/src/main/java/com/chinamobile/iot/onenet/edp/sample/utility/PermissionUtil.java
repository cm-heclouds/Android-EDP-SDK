/*
 * Copyright (c) 2016-2018. China Mobile Communications Corporation. All rights reserved.
 */

package com.chinamobile.iot.onenet.edp.sample.utility;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.AppOpsManagerCompat;
import android.support.v4.app.Fragment;

import com.chinamobile.iot.onenet.edp.sample.BuildConfig;

import java.util.ArrayList;

public class PermissionUtil {

    public static boolean hasPermission(@NonNull Activity activity, @NonNull String permission, int requestCode) {
        return hasPermissions(activity, new String[] {permission}, requestCode);
    }

    public static boolean hasPermissions(@NonNull Activity activity, @NonNull String[] permissions, int requestCode) {
        if (!isAllPermissionGranted(activity, permissions)) {
            ActivityCompat.requestPermissions(activity, permissions, requestCode);
            return false;
        } else {
            return true;
        }
    }

    private static boolean isAllPermissionGranted(@NonNull Activity activity, @NonNull String[] permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check that all given permissions have been granted by verifying that each entry in the
     * given array is of the value {@link PackageManager#PERMISSION_GRANTED}.
     *
     * @see Activity#onRequestPermissionsResult(int, String[], int[])
     */
    public static boolean verifyPermissions(int[] grantResults) {
        // At least one result must be checked.
        if(grantResults.length < 1){
            return false;
        }

        // Verify that each required permission has been granted, otherwise return false.
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static boolean checkOpsPermission(Context context, String permission) {
        try {
            AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            String opsName = AppOpsManagerCompat.permissionToOp(permission);
            if (opsName == null) {
                return true;
            }
            int opsMode = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                opsMode = appOpsManager.checkOpNoThrow(opsName, Process.myUid(), context.getPackageName());
            }
            return opsMode != AppOpsManager.MODE_IGNORED && opsMode != AppOpsManager.MODE_ERRORED;
        } catch (Exception ex) {
            return true;
        }
    }

    public static boolean checkOpsPermissions(Context context, String[] permissions) {
        for (String permission : permissions) {
            if (!checkOpsPermission(context, permission)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 跳转至系统设置-应用权限管理界面
     */
    public static void jumpToPermission(Activity activity, int requestCode) {
        try {
            if (BrandUtil.checkBrand(BrandUtil.BRAND.XIAOMI)) {
                jumpToMiuiPermission(activity, requestCode);
            } else if (BrandUtil.checkBrand(BrandUtil.BRAND.HUAWEI)) {
                jumpToHuaweiPermission(activity, requestCode);
            } else if (BrandUtil.checkBrand(BrandUtil.BRAND.MEIZU)) {
                jumpToMeizuPermission(activity, requestCode);
            } else {
                jumpToAppDetailSetting(activity, requestCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                jumpToAppDetailSetting(activity, requestCode);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }

    private static void jumpToMiuiPermission(Activity activity, int requestCode) throws RuntimeException {
        Intent intent = new Intent("miui.intent.action.APP_PERM_EDITOR");
        ComponentName componentName = new ComponentName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity");
        intent.setComponent(componentName);
        intent.putExtra("extra_pkgname", activity.getPackageName());
        activity.startActivityForResult(intent, requestCode);
    }

    private static void jumpToMeizuPermission(Activity activity, int requestCode) throws RuntimeException {
        Intent intent = new Intent("com.meizu.safe.security.SHOW_APPSEC");
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.putExtra("packageName", BuildConfig.APPLICATION_ID);
        activity.startActivityForResult(intent, requestCode);
    }

    private static void jumpToHuaweiPermission(Activity activity, int requestCode) throws RuntimeException {
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ComponentName comp = new ComponentName("com.huawei.systemmanager", "com.huawei.permissionmanager.ui.MainActivity");
        intent.setComponent(comp);
        activity.startActivityForResult(intent, requestCode);
    }

    private static void jumpToAppDetailSetting(Activity activity, int requestCode) throws RuntimeException {
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
            intent.setData(Uri.fromParts("package", activity.getPackageName(), null));
        } else {
            intent.setAction(Intent.ACTION_VIEW);
            intent.setClassName("com.android.settings", "com.android.settings.InstalledAppDetails");
            intent.putExtra("com.android.settings.ApplicationPkgName", activity.getPackageName());
        }
        activity.startActivityForResult(intent, requestCode);
    }

    public static void requestPermission(Activity activity, String permission, int requestCode, Callback callback) {
        int contactsPermissionState = ActivityCompat.checkSelfPermission(activity, permission);
        if (contactsPermissionState != PackageManager.PERMISSION_GRANTED) {
            // 未获取权限
            if (!BrandUtil.checkBrand(BrandUtil.BRAND.XIAOMI) && ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                // 勾选了不再显示
                if (callback != null) {
                    callback.onShowRationaleDialog(permission);
                }
            } else if (!checkOpsPermission(activity, permission)) {
                // 权限管理中关闭了权限
                if (callback != null) {
                    callback.onShowRationaleDialog(permission);
                }
            } else {
                ActivityCompat.requestPermissions(activity, new String[]{permission}, requestCode);
            }
        } else {
            // 获取了权限
            if (callback != null) {
                callback.onPermissionGranted();
            }
        }
    }

    public static void requestPermission(Fragment fragment, String permission, int requestCode, Callback callback) {
        int contactsPermissionState = ActivityCompat.checkSelfPermission(fragment.getActivity(), permission);
        if (contactsPermissionState != PackageManager.PERMISSION_GRANTED) {
            // 未获取权限
            if (!BrandUtil.checkBrand(BrandUtil.BRAND.XIAOMI) && ActivityCompat.shouldShowRequestPermissionRationale(fragment.getActivity(), permission)) {
                // 勾选了不再显示
                if (callback != null) {
                    callback.onShowRationaleDialog(permission);
                }
            } else if (!checkOpsPermission(fragment.getActivity(), permission)) {
                // 权限管理中关闭了权限
                if (callback != null) {
                    callback.onShowRationaleDialog(permission);
                }
            } else {
                fragment.requestPermissions(new String[]{permission}, requestCode);
            }
        } else {
            // 获取了权限
            if (callback != null) {
                callback.onPermissionGranted();
            }
        }
    }

    public static void requestPermissions(Activity activity, String[] permissions, int requestCode, Callback callback) {
        ArrayList<String> permissionList = new ArrayList<>();
        for (String permission : permissions) {
            int contactsPermissionState = ActivityCompat.checkSelfPermission(activity, permission);
            if (contactsPermissionState != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(permission);
            }
        }
        if (permissionList.isEmpty()) {
            // 获取了权限
            if (callback != null) {
                callback.onPermissionGranted();
            }
        } else {
            String[] ungrantedPermissions = permissionList.toArray(new String[] {});
            for (String ungrantedPermission : ungrantedPermissions) {
                // 未获取权限
                if (!BrandUtil.checkBrand(BrandUtil.BRAND.XIAOMI) && ActivityCompat.shouldShowRequestPermissionRationale(activity, ungrantedPermission)) {
                    // 勾选了不再显示
                    if (callback != null) {
                        callback.onShowRationaleDialog(ungrantedPermission);
                        return;
                    }
                } else if (!checkOpsPermission(activity, ungrantedPermission)) {
                    // 权限管理中关闭了权限
                    if (callback != null) {
                        callback.onShowRationaleDialog(ungrantedPermission);
                        return;
                    }
                }
            }
            ActivityCompat.requestPermissions(activity, ungrantedPermissions, requestCode);
        }

    }

    public interface Callback {
        void onPermissionGranted();
        void onShowRationaleDialog(String permission);
    }

}
