import 'package:flutter/material.dart';

class SplashScreen extends StatelessWidget {
  final String message;
  final bool showRetryButton;
  final VoidCallback? onRetry;
  final bool showContinueButton;
  final VoidCallback? onContinue;

  const SplashScreen({
    Key? key,
    required this.message,
    this.showRetryButton = false,
    this.onRetry,
    this.showContinueButton = false,
    this.onContinue,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Container(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [Colors.blue.shade700, Colors.blue.shade900],
          ),
        ),
        child: Center(
          child: Padding(
            padding: const EdgeInsets.all(20.0),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                // 应用Logo
                Icon(Icons.apps_rounded, size: 80, color: Colors.white),
                SizedBox(height: 24),
                // 应用名称
                Text(
                  '泰康共享平台',
                  style: TextStyle(
                    fontSize: 28,
                    fontWeight: FontWeight.bold,
                    color: Colors.white,
                  ),
                ),
                SizedBox(height: 40),
                // 加载动画
                if (!showRetryButton && !showContinueButton)
                  CircularProgressIndicator(
                    valueColor: AlwaysStoppedAnimation<Color>(Colors.white),
                  ),
                SizedBox(height: 24),
                // 消息文本
                Text(
                  message,
                  textAlign: TextAlign.center,
                  style: TextStyle(fontSize: 16, color: Colors.white),
                ),
                SizedBox(height: 30),
                // 重试按钮
                if (showRetryButton)
                  ElevatedButton.icon(
                    icon: Icon(Icons.refresh),
                    label: Text('重试连接'),
                    onPressed: onRetry,
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.white,
                      foregroundColor: Colors.blue.shade900,
                      padding: EdgeInsets.symmetric(
                        horizontal: 24,
                        vertical: 12,
                      ),
                    ),
                  ),
                SizedBox(height: showRetryButton ? 16 : 0),
                // 继续按钮
                if (showContinueButton)
                  ElevatedButton.icon(
                    icon: Icon(Icons.arrow_forward),
                    label: Text('继续使用（离线模式）'),
                    onPressed: onContinue,
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.white.withOpacity(0.8),
                      foregroundColor: Colors.blue.shade900,
                      padding: EdgeInsets.symmetric(
                        horizontal: 24,
                        vertical: 12,
                      ),
                    ),
                  ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
