import 'package:parse_server_sdk_flutter/parse_server_sdk_flutter.dart';

class ParseService {
  // 与parse-server-config.json中的配置保持一致
  static const String keyApplicationId = 'universe_share_app_id';
  static const String keyClientKey =
      'universe_share_master_key'; // 在前端使用masterKey作为clientKey
  static const String keyParseServerUrl = 'http://10.0.2.2:1337/parse';

  // 初始化Parse服务
  static Future<void> initialize() async {
    await Parse().initialize(
      keyApplicationId,
      keyParseServerUrl,
      clientKey: keyClientKey,
      autoSendSessionId: true,
      debug: true, // 生产环境请设置为false
      liveQueryUrl: keyParseServerUrl, // 如果需要LiveQuery功能
      parseUserConstructor: (
        username,
        password,
        emailAddress, {
        client,
        debug,
        sessionToken,
      }) {
        return ParseUser(username, password, emailAddress);
      },
    );

    // 测试连接
    final response = await Parse().healthCheck();
    if (response.success) {
      print('✅ Parse Server连接成功!');
    } else {
      print('❌ Parse Server连接失败: ${response.error?.message}');
    }
  }

  // 用户注册
  static Future<ParseResponse> signUp(
    String username,
    String email,
    String password, {
    Map<String, dynamic>? userCustomFields,
  }) async {
    final user = ParseUser.createUser(username, password, email);

    // 添加自定义字段
    if (userCustomFields != null) {
      userCustomFields.forEach((key, value) {
        user.set(key, value);
      });
    }

    return await user.signUp();
  }

  // 用户登录
  static Future<ParseResponse> login(String username, String password) async {
    final user = ParseUser(username, password, null);
    return await user.login();
  }

  // 检查用户是否已登录
  static Future<bool> isUserLoggedIn() async {
    ParseUser? currentUser = await ParseUser.currentUser() as ParseUser?;
    return currentUser != null && await currentUser.sessionToken != null;
  }

  // 获取当前用户
  static Future<ParseUser?> getCurrentUser() async {
    return await ParseUser.currentUser() as ParseUser?;
  }

  // 调用云函数获取用户资料
  static Future<Map<String, dynamic>> getUserProfile() async {
    final user = await getCurrentUser();
    if (user == null) {
      throw Exception('用户未登录');
    }

    final response = await ParseCloudFunction('getUserProfile').execute();
    if (response.success && response.result != null) {
      return response.result;
    } else {
      throw Exception(response.error?.message ?? '获取用户资料失败');
    }
  }

  // 用户登出
  static Future<ParseResponse> logout() async {
    final user = await ParseUser.currentUser() as ParseUser;
    return await user.logout();
  }
}
