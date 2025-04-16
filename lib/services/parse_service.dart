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

  // QEMU模拟器可能需要使用10.0.3.2
  static const String keyServerUrlQemu = 'http://10.0.3.2:1337/parse';

  // 2. iOS模拟器 - 使用localhost连接到主机
  static const String keyServerUrlIosSimulator = 'http://localhost:1337/parse';

  // 3. 真机测试 - 使用电脑的实际IP地址
  static const String keyServerUrlPhysicalDevice =
      'http://192.168.1.11:1337/parse';

  // 新增尝试本机IP地址，有时这个更可靠
  static const String keyServerUrlLocalIP = 'http://127.0.0.1:1337/parse';

  // 修改服务器地址尝试顺序，确保包含所有可能地址
  static List<String> get _serverUrls => [
    keyServerUrlEmulator, // 10.0.2.2
    keyServerUrlAvd, // localhost
    keyServerUrlQemu, // 10.0.3.2
    keyServerUrlIosSimulator, // 也是localhost
    keyServerUrlLocalIP, // 127.0.0.1
    keyServerUrlPhysicalDevice, // 实际IP，最后尝试
  ];

  // 用于记录Parse服务器是否可用
  static bool _isServerAvailable = false;
  static bool get isServerAvailable => _isServerAvailable;

  // 记录成功连接的服务器地址
  static String? _successfulServerUrl;
  static String? get successfulServerUrl => _successfulServerUrl;

  // 手动设置服务器状态（主要用于测试或强制离线模式）
  static void setServerAvailability(bool available) {
    _isServerAvailable = available;
    print('手动设置服务器状态: ${available ? '可用' : '不可用'}');
  }

  // 初始化Parse服务
  static Future<void> initialize() async {
    try {
      print('开始初始化Parse服务...');

      // 设置初始状态为不可用
      _isServerAvailable = false;

      // 尝试不同的服务器地址
      for (var serverUrl in _serverUrls) {
        print('尝试连接服务器地址: $serverUrl');
        if (await _tryInitializeWithUrl(serverUrl)) {
          print('成功连接到服务器: $serverUrl');
          _successfulServerUrl = serverUrl;
          _isServerAvailable = true;
          return;
        }
        // 失败后继续尝试下一个地址
        print('连接失败，尝试下一个地址...');
      }

      print('所有地址均连接失败，将以离线模式运行');
      _isServerAvailable = false;
    } catch (e, stackTrace) {
      print('❌❌❌ Parse初始化异常: $e');
      print('异常类型: ${e.runtimeType}');
      print('堆栈跟踪: $stackTrace');
      print('应用将继续在离线模式下运行');
      _isServerAvailable = false;
    }
  }

  // 尝试使用指定URL初始化
  static Future<bool> _tryInitializeWithUrl(String serverUrl) async {
    try {
      // 添加尝试次数限制和延迟
      int retryCount = 0;
      const int maxRetries = 1; // 减少重试次数，加快离线模式判断
      bool initialized = false;

      print('开始尝试连接: $serverUrl');

      while (retryCount < maxRetries && !initialized) {
        try {
          print('初始化尝试 #${retryCount + 1} 连接到 $serverUrl');

          await Parse().initialize(
            keyApplicationId,
            serverUrl,
            clientKey: keyClientKey,
            autoSendSessionId: true,
            debug: true,
            liveQueryUrl: serverUrl,
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
          print('Parse初始化成功！');
        } catch (initError) {
          retryCount++;
          print('初始化尝试 $retryCount 失败: $initError');
          print('异常类型: ${initError.runtimeType}');
          print('异常详情: ${initError.toString()}');
          if (retryCount < maxRetries) {
            print('等待1秒后重试...'); // 减少等待时间
            await Future.delayed(const Duration(seconds: 1));
          }
        }
      }

      if (!initialized) {
        print('达到最大重试次数，尝试下一个地址');
        return false;
      }

      print('Parse初始化完成，正在测试连接...');

      // 测试连接
      try {
        final response = await Parse().healthCheck().timeout(
          const Duration(seconds: 3), // 减少超时时间以加快判断
          onTimeout: () {
            print('健康检查超时！');
            return ParseResponse()
              ..success = false
              ..error = ParseError(code: -1, message: "连接超时");
          },
        );

        if (response.success) {
          print('✅ Parse Server连接成功!');
          _isServerAvailable = true;
          return true;
        } else {
          print('❌ Parse Server连接失败: ${response.error?.message}');
          print('错误详情: ${response.error?.toString()}');
          print('错误码: ${response.error?.code}');
          print('状态码: ${response.statusCode}');
          return false;
        }
      } catch (connectionError) {
        print('❌ 健康检查异常: $connectionError');
        print('异常类型: ${connectionError.runtimeType}');
        print('堆栈跟踪:');
        print(StackTrace.current);
        return false;
      }
    } catch (e) {
      print('尝试连接地址 $serverUrl 失败: $e');
      print('异常类型: ${e.runtimeType}');
      print('堆栈跟踪:');
      print(StackTrace.current);
      return false;
    }
  }

  // 用户注册
  static Future<ParseResponse> signUp(
    String username,
    String email,
    String password, {
    Map<String, dynamic>? userCustomFields,
  }) async {
    try {
      if (!_isServerAvailable) {
        return ParseResponse()
          ..success = false
          ..error = ParseError(code: -1, message: "服务器不可用，请稍后再试");
      }

      final user = ParseUser.createUser(username, password, email);

      // 添加自定义字段
      if (userCustomFields != null) {
        userCustomFields.forEach((key, value) {
          user.set(key, value);
        });
      }

      return await user.signUp();
    } catch (e) {
      print('注册异常: $e');
      return ParseResponse()
        ..success = false
        ..error = ParseError(code: -1, message: "注册失败: $e");
    }
  }

  // 用户登录
  static Future<ParseResponse> login(String username, String password) async {
    try {
      if (!_isServerAvailable) {
        // 如果服务器不可用，则尝试本地模拟登录成功
        // 注意：这仅用于演示目的，实际应用中应考虑安全性
        if (username == 'demo' && password == 'password') {
          final response = ParseResponse()..success = true;
          return response;
        }
        return ParseResponse()
          ..success = false
          ..error = ParseError(code: -1, message: "服务器不可用，请稍后再试");
      }

      final user = ParseUser(username, password, null);
      return await user.login();
    } catch (e) {
      print('登录异常: $e');
      return ParseResponse()
        ..success = false
        ..error = ParseError(code: -1, message: "登录失败: $e");
    }
  }

  // 检查用户是否已登录
  static Future<bool> isUserLoggedIn() async {
    try {
      if (!_isServerAvailable) {
        // 离线模式下默认未登录
        return false;
      }

      ParseUser? currentUser = await ParseUser.currentUser() as ParseUser?;
      return currentUser != null && await currentUser.sessionToken != null;
    } catch (e) {
      print('检查登录状态异常: $e');
      return false;
    }
  }

  // 获取当前用户
  static Future<ParseUser?> getCurrentUser() async {
    try {
      if (!_isServerAvailable) {
        return null;
      }

      return await ParseUser.currentUser() as ParseUser?;
    } catch (e) {
      print('获取当前用户异常: $e');
      return null;
    }
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
    try {
      if (!_isServerAvailable) {
        return ParseResponse()..success = true; // 离线模式下模拟登出成功
      }

      final user = await ParseUser.currentUser() as ParseUser;
      return await user.logout();
    } catch (e) {
      print('登出异常: $e');
      return ParseResponse()..success = true; // 即使出错也视为登出成功，以确保用户可以退出
    }
  }

  // 获取用户角色
  static Future<String?> getUserRole() async {
    try {
      if (!_isServerAvailable) {
        return 'both'; // 离线模式下默认角色
      }

      final user = await getCurrentUser();
      if (user == null) {
        return null;
      }
      return user.get<String>('userRole');
    } catch (e) {
      print('获取用户角色异常: $e');
      return 'both';
    }
  }

  // 设置用户角色 - 通过云函数
  static Future<bool> setUserRole(String role) async {
    try {
      if (!_isServerAvailable) {
        return true; // 离线模式下模拟设置成功
      }

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
      if (!_isServerAvailable) {
        return true; // 离线模式下模拟设置成功
      }

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
