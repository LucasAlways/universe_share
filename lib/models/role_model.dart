import 'package:flutter/foundation.dart';
import 'package:parse_server_sdk_flutter/parse_server_sdk_flutter.dart';
import '../services/parse_service.dart';

enum UserRole {
  consumer, // 消费者
  producer, // 生产者
  both, // 两种角色都有
}

class RoleModel extends ChangeNotifier {
  UserRole _currentRole = UserRole.consumer; // 默认为消费者
  bool _isLoading = false;

  UserRole get currentRole => _currentRole;
  bool get isLoading => _isLoading;
  bool get isConsumer =>
      _currentRole == UserRole.consumer || _currentRole == UserRole.both;
  bool get isProducer =>
      _currentRole == UserRole.producer || _currentRole == UserRole.both;

  // 设置加载状态
  void setLoading(bool loading) {
    _isLoading = loading;
    notifyListeners();
  }

  // 设置用户角色
  void setUserRole(UserRole role) {
    _currentRole = role;
    notifyListeners();
    _saveRoleToUser(role); // 保存角色到用户数据
  }

  // 从服务器获取用户角色
  Future<void> fetchUserRole() async {
    setLoading(true);

    try {
      final roleString = await ParseService.getUserRole();
      if (roleString != null) {
        switch (roleString) {
          case 'consumer':
            _currentRole = UserRole.consumer;
            break;
          case 'producer':
            _currentRole = UserRole.producer;
            break;
          case 'both':
            _currentRole = UserRole.both;
            break;
          default:
            _currentRole = UserRole.consumer; // 默认为消费者
        }
      } else {
        // 用户没有设置角色，设置默认角色
        _currentRole = UserRole.consumer;
        _saveRoleToUser(_currentRole);
      }
    } catch (e) {
      print('获取用户角色失败: $e');
    } finally {
      setLoading(false);
    }
  }

  // 保存角色到用户数据
  Future<void> _saveRoleToUser(UserRole role) async {
    String roleString;
    switch (role) {
      case UserRole.consumer:
        roleString = 'consumer';
        break;
      case UserRole.producer:
        roleString = 'producer';
        break;
      case UserRole.both:
        roleString = 'both';
        break;
    }

    try {
      // 使用ParseService方法
      final success = await ParseService.setUserRoleDirect(roleString);
      if (!success) {
        print('保存用户角色失败');
      }
    } catch (e) {
      print('保存用户角色失败: $e');
    }
  }
}
