import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:flutter/services.dart';
import 'models/user_model.dart';
import 'models/role_model.dart';
import 'screens/login_screen.dart';
import 'screens/home_screen.dart';
import 'screens/splash_screen.dart';
import 'services/parse_service.dart';

void main() {
  runApp(
    MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (context) => UserModel()),
        ChangeNotifierProvider(create: (context) => RoleModel()),
      ],
      child: const MyApp(),
    ),
  );
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  bool _isInitializing = true;
  bool _initializationError = false;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _initializeApp();
  }

  Future<void> _initializeApp() async {
    try {
      print('应用启动 - 开始初始化Parse服务...');
      // 初始化Parse服务
      await ParseService.initialize().timeout(
        const Duration(seconds: 10), // 增加超时时间
        onTimeout: () {
          print('连接Parse服务器超时（10秒），将以离线模式启动应用');
          throw Exception('连接Parse服务器超时，将以离线模式启动应用');
        },
      );

      // 如果成功连接，显示连接信息
      if (ParseService.isServerAvailable) {
        print('成功连接到Parse服务器: ${ParseService.successfulServerUrl}');
      } else {
        print('无法连接到Parse服务器，应用将以离线模式运行');
      }
    } catch (e, stackTrace) {
      setState(() {
        _initializationError = true;
        _errorMessage = '连接服务器失败: ${e.toString()}';
      });
      print('初始化应用失败: $e');
      print('异常类型: ${e.runtimeType}');
      print('堆栈跟踪: $stackTrace');
    } finally {
      setState(() {
        _isInitializing = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: '泰康',
      theme: ThemeData(
        primarySwatch: Colors.blue,
        visualDensity: VisualDensity.adaptivePlatformDensity,
      ),
      home: _buildHomeScreen(),
    );
  }

  Widget _buildHomeScreen() {
    if (_isInitializing) {
      return const SplashScreen(message: '正在启动应用...\n连接Parse服务器中...');
    }

    if (_initializationError) {
      return SplashScreen(
        message: '应用将以离线模式启动\n$_errorMessage',
        showRetryButton: true,
        showContinueButton: true, // 增加继续按钮
        onRetry: () {
          setState(() {
            _isInitializing = true;
            _initializationError = false;
          });
          _initializeApp();
        },
        onContinue: () {
          // 即使出错，也允许用户继续使用应用
          Navigator.of(context).pushReplacement(
            MaterialPageRoute(builder: (context) => const LoginScreen()),
          );
        },
      );
    }

    return FutureBuilder<bool>(
      future: ParseService.isUserLoggedIn(),
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const SplashScreen(message: '正在检查登录状态...');
        }

        if (snapshot.hasError) {
          return SplashScreen(
            message: '检查登录状态失败，将以离线模式启动\n${snapshot.error}',
            showContinueButton: true,
            onContinue: () {
              Navigator.of(context).pushReplacement(
                MaterialPageRoute(builder: (context) => const LoginScreen()),
              );
            },
          );
        }

        final bool isLoggedIn = snapshot.data ?? false;
        if (isLoggedIn) {
          return const HomeScreen();
        } else {
          return const LoginScreen();
        }
      },
    );
  }
}
