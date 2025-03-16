package com.universe_share.floating_window

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import com.example.universe_share.FloatingWindowService

class FloatingWindowPlugin: FlutterPlugin, MethodCallHandler, ActivityAware, PluginRegistry.ActivityResultListener {
  private lateinit var channel : MethodChannel
  private lateinit var context: Context
  private var activity: Activity? = null
  private var pendingResult: Result? = null
  
  private val REQUEST_CODE_OVERLAY_PERMISSION = 1234

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "com.universe_share/floating_window")
    channel.setMethodCallHandler(this)
    context = flutterPluginBinding.applicationContext
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    when (call.method) {
      "checkFloatingWindowPermission" -> {
        val hasPermission = checkOverlayPermission()
        result.success(hasPermission)
      }
      "requestFloatingWindowPermission" -> {
        pendingResult = result
        requestOverlayPermission()
      }
      "startFloatingWindow" -> {
        if (checkOverlayPermission()) {
          // 实际启动悬浮窗服务
          try {
            val intent = Intent(context, FloatingWindowService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
              context.startForegroundService(intent)
            } else {
              context.startService(intent)
            }
            
            // 返回成功
            result.success(true)
            
            // 如果Activity不为空，添加短暂延迟后退出到桌面
            activity?.let { activity ->
              Handler(Looper.getMainLooper()).postDelayed({
                val homeIntent = Intent(Intent.ACTION_MAIN)
                homeIntent.addCategory(Intent.CATEGORY_HOME)
                homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                activity.startActivity(homeIntent)
              }, 200) // 200毫秒延迟
            }
          } catch (e: Exception) {
            result.error("START_ERROR", "启动悬浮窗服务失败: ${e.message}", null)
            e.printStackTrace()
          }
        } else {
          result.error("PERMISSION_DENIED", "没有悬浮窗权限", null)
        }
      }
      else -> {
        result.notImplemented()
      }
    }
  }

  private fun checkOverlayPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      Settings.canDrawOverlays(context)
    } else {
      true // 低于Android 6.0默认拥有权限
    }
  }

  private fun requestOverlayPermission() {
    if (activity == null) {
      pendingResult?.error("ACTIVITY_NULL", "Activity为空", null)
      pendingResult = null
      return
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      val intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}")
      )
      activity?.startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION)
    } else {
      // 低于Android 6.0，无需请求权限
      pendingResult?.success(true)
      pendingResult = null
    }
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    activity = binding.activity
    binding.addActivityResultListener(this)
  }

  override fun onDetachedFromActivityForConfigChanges() {
    activity = null
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    activity = binding.activity
    binding.addActivityResultListener(this)
  }

  override fun onDetachedFromActivity() {
    activity = null
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
    if (requestCode == REQUEST_CODE_OVERLAY_PERMISSION) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val hasPermission = Settings.canDrawOverlays(context)
        pendingResult?.success(hasPermission)
        pendingResult = null
        return true
      }
    }
    return false
  }
} 