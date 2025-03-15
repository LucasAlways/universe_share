import 'package:parse_server_sdk_flutter/parse_server_sdk_flutter.dart';
import 'package:flutter/foundation.dart';

class UserModel extends ChangeNotifier {
  ParseUser? _currentUser;
  bool _isLoading = false;
  String _errorMessage = '';

  ParseUser? get currentUser => _currentUser;
  bool get isLoading => _isLoading;
  bool get isLoggedIn => _currentUser != null;
  String get errorMessage => _errorMessage;

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
    setLoading(true);
    setErrorMessage('');

    try {
      final user = ParseUser(username, password, null);
      final response = await user.login();

      if (response.success) {
        _currentUser = response.result;
        setLoading(false);
        notifyListeners();
        return true;
      } else {
        setErrorMessage(response.error?.message ?? '登录失败');
        setLoading(false);
        return false;
      }
    } catch (e) {
      setErrorMessage(e.toString());
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
        } else {
          _currentUser = null;
        }
      } else {
        _currentUser = null;
      }
    } catch (e) {
      _currentUser = null;
    }

    setLoading(false);
    notifyListeners();
  }
}
