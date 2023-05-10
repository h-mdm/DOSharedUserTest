package com.hmdm.dosharedusertest;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import dalvik.system.PathClassLoader;

public class MainActivity extends AppCompatActivity {

    String deviceOwnerPackageName = "com.hmdm.launcher";
    String adminClassPath = "com.hmdm.launcher.AdminReceiver";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button button = findViewById(R.id.reboot_button);
        button.setOnClickListener(v -> {
            if (!rebootDevice()) {
                Toast.makeText(MainActivity.this, R.string.reboot_failed, Toast.LENGTH_LONG).show();;
            }
        });
    }

    private boolean rebootDevice() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return false;
        }
        try {
            DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

            // We must use the context of Device Owner
            // Since we're using the same shared user, it should be returned without any security issues
            Context deviceOwnerContext = createPackageContext(deviceOwnerPackageName, 0);

            // Android SDK is not supposed to load classes of another application
            // However since we're using the same shared user ID (so it's the same application underhood),
            // we can use a hack to get the required class
            Class adminReceiver = loadAdminReceiver();
            if (adminReceiver == null) {
                return false;
            }
            ComponentName adminComponentName = new ComponentName(deviceOwnerContext, loadAdminReceiver());
            dpm.reboot(adminComponentName);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // A hack to retrieve a class from another package
    Class loadAdminReceiver() {
        String apkName = null;
        try {
            apkName = getPackageManager().getApplicationInfo(deviceOwnerPackageName,0).sourceDir;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        PathClassLoader pathClassLoader = new dalvik.system.PathClassLoader(
                apkName,
                ClassLoader.getSystemClassLoader());

        try {
            Class<?> clazz = Class.forName(adminClassPath, true, pathClassLoader);
            return clazz;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}