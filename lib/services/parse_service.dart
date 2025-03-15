import 'package:parse_server_sdk_flutter/parse_server_sdk_flutter.dart';

class ParseService {
  // 与parse-server-config.json中的配置保持一致
  static const String keyApplicationId = 'universe_share_app_id';
  static const String keyClientKey =
      'universe_share_master_key'; // 在前端使用masterKey作为clientKey

  // 不同环境下使用的Parse服务器地址
  // 1. Android模拟器 - 使用10.0.2.2连接到主机的localhost
  static const String keyServerUrlEmulator = 'http://10.0.2.2:1337/parse';

  // 当使用AVD时有时需要直接使用localhost
  static const String keyServerUrlAvd = 'http://localhost:1337/parse';

  // 2. iOS模拟器 - 使用localhost连接到主机
  static const String keyServerUrlIosSimulator = 'http://localhost:1337/parse';

  // 3. 真机测试 - 使用电脑的实际IP地址
  static const String keyServerUrlPhysicalDevice =
      'http://192.168.1.11:1337/parse';

  // 根据您的测试环境选择以下一个地址作为keyParseServerUrl
  // static const String keyParseServerUrl = keyServerUrlEmulator; // 标准Android模拟器
  // static const String keyParseServerUrl = keyServerUrlAvd; // 使用AVD时尝试此地址

  // 使用IP地址直接连接 - 这是在AVD中最可靠的方式
  static const String keyParseServerUrl = 'http://10.0.2.2:1337/parse';

  // static const String keyParseServerUrl = keyServerUrlIosSimulator; // 如果使用iOS模拟器
  // static const String keyParseServerUrl = keyServerUrlPhysicalDevice; // 如果使用真机测试

  // 初始化Parse服务
  static Future<void> initialize() async {
    try {
      print('开始初始化Parse服务...');
      print('使用服务器地址: $keyParseServerUrl');

      // 添加尝试次数限制和延迟
      int retryCount = 0;
      const int maxRetries = 3;
      bool initialized = false;

      while (retryCount < maxRetries && !initialized) {
        try {
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
          initialized = true;
        } catch (initError) {
          retryCount++;
          print('初始化尝试 $retryCount 失败: $initError');
          if (retryCount < maxRetries) {
            print('等待2秒后重试...');
            await Future.delayed(const Duration(seconds: 2));
          }
        }
      }

      if (!initialized) {
        print('达到最大重试次数，将继续但可能无法连接服务器');
      }

      print('Parse初始化完成，正在测试连接...');

      // 测试连接，但不阻止应用启动
      try {
        final response = await Parse().healthCheck().timeout(
          const Duration(seconds: 5),
          onTimeout: () {
            // 超时时返回一个模拟的错误响应
            return ParseResponse()
              ..success = false
              ..error = ParseError(code: -1, message: "连接超时");
          },
        );
        if (response.success) {
          print('✅ Parse Server连接成功!');
        } else {
          print('❌ Parse Server连接失败: ${response.error?.message}');
          // 尝试显示更多错误信息
          print('错误详情: ${response.error?.toString()}');
          print('状态码: ${response.statusCode}');

          // 即使连接失败也继续运行应用，可能会在没有服务器连接的情况下运行部分功能
          print('应用将继续在离线模式下运行');
        }
      } catch (connectionError) {
        print('❌ 健康检查异常: $connectionError');
        print('应用将继续在离线模式下运行');
      }
    } catch (e, stackTrace) {
      print('❌❌❌ Parse初始化异常: $e');
      print('堆栈跟踪: $stackTrace');
      print('应用将继续在离线模式下运行');
    }

    print('Parse服务初始化流程完成');
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

  // 获取用户角色
  static Future<String?> getUserRole() async {
    final user = await getCurrentUser();
    if (user == null) {
      return null;
    }
    return user.get<String>('userRole');
  }

  // 设置用户角色 - 通过云函数
  static Future<bool> setUserRole(String role) async {
    try {
      final response = await ParseCloudFunction(
        'setUserRole',
      ).execute(parameters: {'role': role});

      if (response.success) {
        return true;
      } else {
        print('设置角色失败: ${response.error?.message}');
        return false;
      }
    } catch (e) {
      print('设置角色异常: $e');
      return false;
    }
  }

  // 设置用户角色 - 直接设置
  static Future<bool> setUserRoleDirect(String role) async {
    try {
      final user = await getCurrentUser();
      if (user == null) {
        return false;
      }

      user.set('userRole', role);
      final response = await user.save();

      return response.success;
    } catch (e) {
      print('设置角色异常: $e');
      return false;
    }
  }
}
