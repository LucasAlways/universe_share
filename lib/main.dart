import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:flutter/services.dart';
import 'models/user_model.dart';
import 'models/role_model.dart';
import 'screens/login_screen.dart';
import 'screens/home_screen.dart';
import 'services/parse_service.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // 设置状态栏颜色
  SystemChrome.setSystemUIOverlayStyle(
    const SystemUiOverlayStyle(statusBarColor: Colors.transparent),
  );

  // 捕获全局错误
  FlutterError.onError = (FlutterErrorDetails details) {
    FlutterError.presentError(details);
    print('Flutter错误: ${details.exception}');
  };

  // 初始化Parse服务
  try {
    await ParseService.initialize();
    print('Parse服务初始化成功');
  } catch (e) {
    print('Parse服务初始化失败: $e');
    // 你可以在这里添加更多的错误处理逻辑
  }

  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (context) => UserModel()),
        ChangeNotifierProvider(create: (context) => RoleModel()),
      ],
      child: MaterialApp(
        title: '泰康共享平台',
        debugShowCheckedModeBanner: false,
        theme: ThemeData(
          colorScheme: ColorScheme.fromSeed(seedColor: Colors.blue),
          useMaterial3: true,
          appBarTheme: const AppBarTheme(
            backgroundColor: Colors.blue,
            foregroundColor: Colors.white,
            elevation: 0,
          ),
          elevatedButtonTheme: ElevatedButtonThemeData(
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.blue,
              foregroundColor: Colors.white,
            ),
          ),
          pageTransitionsTheme: const PageTransitionsTheme(
            builders: {
              TargetPlatform.android: ZoomPageTransitionsBuilder(),
              TargetPlatform.iOS: CupertinoPageTransitionsBuilder(),
            },
          ),
        ),
        home: FutureBuilder<bool>(
          future: ParseService.isUserLoggedIn(),
          builder: (context, snapshot) {
            if (snapshot.connectionState == ConnectionState.waiting) {
              return const Scaffold(
                body: Center(child: CircularProgressIndicator()),
              );
            }

            // 处理错误情况
            if (snapshot.hasError) {
              return Scaffold(
                body: Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      const Icon(
                        Icons.error_outline,
                        size: 60,
                        color: Colors.red,
                      ),
                      const SizedBox(height: 16),
                      const Text('连接服务器失败', style: TextStyle(fontSize: 18)),
                      const SizedBox(height: 8),
                      Text(
                        '${snapshot.error}',
                        style: const TextStyle(color: Colors.grey),
                      ),
                      const SizedBox(height: 24),
                      ElevatedButton(
                        onPressed: () {
                          // 重新检查登录状态
                          Navigator.pushReplacement(
                            context,
                            MaterialPageRoute(builder: (_) => const MyApp()),
                          );
                        },
                        child: const Text('重试'),
                      ),
                    ],
                  ),
                ),
              );
            }

            final bool isLoggedIn = snapshot.data ?? false;
            return isLoggedIn ? const HomeScreen() : const LoginScreen();
          },
        ),
      ),
    );
  }
}
