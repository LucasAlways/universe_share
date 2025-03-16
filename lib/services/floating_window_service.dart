import 'package:flutter/services.dart';

class FloatingWindowService {
  // 定义方法通道
  static const MethodChannel _channel = MethodChannel(
    'com.universe_share/floating_window',
  );

  // 启动悬浮窗服务
  static Future<bool> startFloatingWindow() async {
    try {
      final bool result = await _channel.invokeMethod('startFloatingWindow');
      return result;
    } on PlatformException catch (e) {
      print("启动悬浮窗失败：${e.message}");
      return false;
    }
  }

  // 停止悬浮窗服务
  static Future<bool> stopFloatingWindow() async {
    try {
      final bool result = await _channel.invokeMethod('stopFloatingWindow');
      return result;
    } on PlatformException catch (e) {
      print("停止悬浮窗失败：${e.message}");
      return false;
    }
  }

  // 检查悬浮窗权限
  static Future<bool> checkFloatingWindowPermission() async {
    try {
      final bool result = await _channel.invokeMethod(
        'checkFloatingWindowPermission',
      );
      return result;
    } on PlatformException catch (e) {
      print("检查悬浮窗权限失败：${e.message}");
      return false;
    }
  }

  // 请求悬浮窗权限
  static Future<bool> requestFloatingWindowPermission() async {
    try {
      final bool result = await _channel.invokeMethod(
        'requestFloatingWindowPermission',
      );
      return result;
    } on PlatformException catch (e) {
      print("请求悬浮窗权限失败：${e.message}");
      return false;
    }
  }
}
