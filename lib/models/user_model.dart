import 'package:parse_server_sdk_flutter/parse_server_sdk_flutter.dart';
import 'package:flutter/foundation.dart';
import '../services/parse_service.dart';
import 'package:shared_preferences/shared_preferences.dart';

class UserModel extends ChangeNotifier {
  ParseUser? _currentUser;
  bool _isLoading = false;
  String _errorMessage = '';
  bool _isOfflineMode = false;

  ParseUser? get currentUser => _currentUser;
  bool get isLoading => _isLoading;
  bool get isLoggedIn => _currentUser != null;
  String get errorMessage => _errorMessage;
  bool get isOfflineMode => _isOfflineMode;

  // 设置加载状态
  void setLoading(bool loading) {
    _isLoading = loading;
    notifyListeners();
  }

  // 设置错误消息
  void setErrorMessage(String message) {
    _errorMessage = message;
    notifyListeners();
  }

  // 登录
  Future<bool> login(String username, String password) async {
    _isLoading = true;
    _errorMessage = '';
    notifyListeners();

    print('开始登录流程，用户: $username');
    print('服务器状态: ${ParseService.isServerAvailable ? '在线' : '离线'}');

    try {
      // 检查是否为离线登录
      if (!ParseService.isServerAvailable) {
        print('服务器不可用，尝试离线登录');

        // 检查是否为预设的离线用户
        if (username == 'admin1' && password == '123456') {
          print('离线用户验证成功');
          // 创建离线用户
          _currentUser = ParseUser(username, password, '');
          _isOfflineMode = true;
          _isLoading = false;
          notifyListeners();

          // 保存登录状态到本地存储
          await _saveLoginStatus(true, username, password);
          print('离线登录成功：$username');
          return true;
        } else {
          print('离线登录失败：用户名或密码不匹配预设账号');
          _errorMessage = '离线模式下，只能使用预设账号（admin1/123456）登录';
          _isLoading = false;
          notifyListeners();
          return false;
        }
      }

      // 在线登录流程
      print('开始在线登录过程');
      final user = ParseUser(username, password, '');
      var response = await user.login();

      if (response.success) {
        _currentUser = response.result;
        _isOfflineMode = false;
        setLoading(false);
        notifyListeners();
        return true;
      } else {
        setErrorMessage(response.error?.message ?? '登录失败');
        setLoading(false);
        return false;
      }
    } catch (e) {
      // 捕获到异常可能是网络问题，建议使用离线账号
      setErrorMessage('登录失败: ${e.toString()}\n请尝试使用离线账号(admin1/123456)');
      setLoading(false);
      return false;
    }
  }

  // 注册
  Future<bool> signUp(
    String username,
    String email,
    String password, {
    Map<String, dynamic>? userCustomFields,
  }) async {
    setLoading(true);
    setErrorMessage('');

    // 检查服务器是否可用
    if (!ParseService.isServerAvailable) {
      setErrorMessage('服务器连接失败，无法注册新账号。请使用离线账号(admin1/123456)登录');
      setLoading(false);
      return false;
    }

    try {
      final user = ParseUser.createUser(username, password, email);

      // 添加自定义字段
      if (userCustomFields != null) {
        userCustomFields.forEach((key, value) {
          user.set(key, value);
        });
      }

      final response = await user.signUp();

      if (response.success) {
        _currentUser = response.result;
        _isOfflineMode = false;
        setLoading(false);
        notifyListeners();
        return true;
      } else {
        setErrorMessage(response.error?.message ?? '注册失败');
        setLoading(false);
        return false;
      }
    } catch (e) {
      setErrorMessage(e.toString());
      setLoading(false);
      return false;
    }
  }

  // 登出
  Future<bool> logout() async {
    setLoading(true);

    try {
      // 如果是离线用户，直接登出
      if (_isOfflineMode) {
        _currentUser = null;
        _isOfflineMode = false;
        setLoading(false);
        notifyListeners();
        return true;
      }

      // 正常的在线登出流程
      final user = await ParseUser.currentUser() as ParseUser;
      final response = await user.logout();

      if (response.success) {
        _currentUser = null;
        setLoading(false);
        notifyListeners();
        return true;
      } else {
        setErrorMessage(response.error?.message ?? '登出失败');
        setLoading(false);
        return false;
      }
    } catch (e) {
      setErrorMessage(e.toString());
      setLoading(false);
      return false;
    }
  }

  // 检查用户登录状态
  Future<void> checkUserLoggedIn() async {
    setLoading(true);

    try {
      final user = await ParseUser.currentUser() as ParseUser?;
      if (user != null) {
        final String? sessionToken = await user.sessionToken;
        if (sessionToken != null) {
          _currentUser = user;
          _isOfflineMode = user.get('isOfflineUser') == true;
        } else {
          _currentUser = null;
          _isOfflineMode = false;
        }
      } else {
        _currentUser = null;
        _isOfflineMode = false;
      }
    } catch (e) {
      _currentUser = null;
      _isOfflineMode = false;
    }

    setLoading(false);
    notifyListeners();
  }

  // 检查是否已登录
  Future<bool> checkLogin() async {
    print('检查用户登录状态...');
    _isLoading = true;
    notifyListeners();

    try {
      if (!ParseService.isServerAvailable) {
        print('服务器不可用，检查本地存储的离线登录状态');

        // 从本地存储获取上次登录的凭据
        bool wasLoggedIn = await _getLoginStatus();
        if (wasLoggedIn) {
          String? username = await _getUsernameFromStorage();
          String? password = await _getPasswordFromStorage();

          if (username == 'admin1' && password == '123456') {
            print('发现有效的离线用户凭据，恢复离线会话');
            _currentUser = ParseUser(username, password, '');
            _isOfflineMode = true;
            _isLoading = false;
            notifyListeners();
            return true;
          }
        }

        print('没有找到有效的离线用户凭据');
        _isOfflineMode = false;
        _isLoading = false;
        notifyListeners();
        return false;
      }

      // 在线状态检查登录
      print('服务器可用，检查在线登录状态');
      ParseUser? user = await ParseUser.currentUser() as ParseUser?;
      if (user != null) {
        final String? sessionToken = await user.sessionToken;
        if (sessionToken != null) {
          _currentUser = user;
          _isOfflineMode = user.get('isOfflineUser') == true;
        } else {
          _currentUser = null;
          _isOfflineMode = false;
        }
      } else {
        _currentUser = null;
        _isOfflineMode = false;
      }
    } catch (e) {
      _currentUser = null;
      _isOfflineMode = false;
    }

    setLoading(false);
    notifyListeners();
    return _currentUser != null;
  }

  // 保存登录状态到本地存储
  Future<void> _saveLoginStatus(
    bool isLoggedIn,
    String username,
    String password,
  ) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('isLoggedIn', isLoggedIn);
    await prefs.setString('username', username);
    await prefs.setString('password', password);
  }

  // 获取登录状态
  Future<bool> _getLoginStatus() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getBool('isLoggedIn') ?? false;
  }

  // 从本地存储获取用户名
  Future<String?> _getUsernameFromStorage() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString('username');
  }

  // 从本地存储获取密码
  Future<String?> _getPasswordFromStorage() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString('password');
  }
}
