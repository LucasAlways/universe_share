import 'package:parse_server_sdk_flutter/parse_server_sdk_flutter.dart';

class ParseService {
  // 初始化Parse服务
  static Future<void> initialize() async {
    const String keyApplicationId = 'universe_share_app_id';
    const String keyClientKey = 'universe_share_client_key';
    const String keyParseServerUrl =
        'http://localhost:1337/parse'; // 使用本地服务器进行测试

    await Parse().initialize(
      keyApplicationId,
      keyParseServerUrl,
      clientKey: keyClientKey,
      autoSendSessionId: true,
      debug: true,
    );
  }

  // 用户注册
  static Future<ParseResponse> signUp(
    String username,
    String email,
    String password,
  ) async {
    final user = ParseUser.createUser(username, password, email);
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

  // 用户登出
  static Future<ParseResponse> logout() async {
    final user = await ParseUser.currentUser() as ParseUser;
    return await user.logout();
  }
}
