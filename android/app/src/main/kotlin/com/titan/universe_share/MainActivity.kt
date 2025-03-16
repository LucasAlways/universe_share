package com.titan.universe_share

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.annotation.NonNull
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import com.universe_share.floating_window.FloatingWindowPlugin
import com.example.universe_share.FloatingWindowService

class MainActivity : FlutterActivity() {
    private val CHANNEL = "com.universe_share/floating_window"
    private val REQUEST_CODE_OVERLAY_PERMISSION = 1234
    private var pendingResult: MethodChannel.Result? = null
    private lateinit var floatingWindowPlugin: FloatingWindowPlugin

    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        
        // 注册我们的浮动窗口插件
        floatingWindowPlugin = FloatingWindowPlugin()
        flutterEngine.plugins.add(floatingWindowPlugin)

        // 设置方法通道
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
            .setMethodCallHandler { call, result ->
                Log.d("MainActivity", "Method call received: ${call.method}")
                when (call.method) {
                    "startFloatingWindow" -> startFloatingWindow(result)
                    "stopFloatingWindow" -> stopFloatingWindow(result)
                    "checkFloatingWindowPermission" -> result.success(checkOverlayPermission())
                    "requestFloatingWindowPermission" -> {
                        pendingResult = result
                        requestOverlayPermission()
                    }
                    else -> result.notImplemented()
                }
            }
    }

    private fun startFloatingWindow(result: MethodChannel.Result) {
        if (!checkOverlayPermission()) {
            result.error("PERMISSION_DENIED", "没有悬浮窗权限", null)
            return
        }

        try {
            val intent = Intent(this, FloatingWindowService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            
            // 先返回结果表示成功启动
            result.success(true)
            
            // 添加短暂延迟确保服务已启动
            Handler(Looper.getMainLooper()).postDelayed({
                // 退出到桌面
                val homeIntent = Intent(Intent.ACTION_MAIN)
                homeIntent.addCategory(Intent.CATEGORY_HOME)
                homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(homeIntent)
            }, 200) // 200毫秒延迟
            
        } catch (e: Exception) {
            result.error("START_ERROR", "启动悬浮窗服务失败: ${e.message}", null)
            e.printStackTrace()
        }
    }

    private fun stopFloatingWindow(result: MethodChannel.Result) {
        try {
            val intent = Intent(this, FloatingWindowService::class.java)
            stopService(intent)
            result.success(true)
        } catch (e: Exception) {
            result.error("STOP_ERROR", "停止悬浮窗服务失败: ${e.message}", null)
            e.printStackTrace()
        }
    }

    private fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION)
        } else {
            // 对于 Android 6.0 以下的设备，默认已授权
            pendingResult?.success(true)
            pendingResult = null
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_OVERLAY_PERMISSION) {
            // 检查权限是否已授予
            val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(this)
            } else {
                true
            }

            pendingResult?.success(hasPermission)
            pendingResult = null

            // 如果没有获得权限，显示提示
            if (!hasPermission) {
                Toast.makeText(this, "需要悬浮窗权限才能正常工作", Toast.LENGTH_LONG).show()
            }
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}
