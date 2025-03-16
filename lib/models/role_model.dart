import 'package:flutter/foundation.dart';
import 'package:parse_server_sdk_flutter/parse_server_sdk_flutter.dart';
import '../services/parse_service.dart';

enum UserRole {
  consumer, // 消费者
  producer, // 生产者
  both, // 两种角色都有
}

class RoleModel extends ChangeNotifier {
  UserRole _currentRole = UserRole.both;
  bool _isLoading = false;
  bool _isOfflineMode = false;

  UserRole get currentRole => _currentRole;
  bool get isLoading => _isLoading;
  bool get isOfflineMode => _isOfflineMode;
  bool get isConsumer => _currentRole == UserRole.consumer;
  bool get isProducer => _currentRole == UserRole.producer;

  // 设置加载状态
  void setLoading(bool loading) {
    _isLoading = loading;
    notifyListeners();
  }

  // 设置用户角色
  void setUserRole(UserRole role) {
    _currentRole = role;

    // 如果不是离线模式，则保存角色到服务器
    if (!_isOfflineMode) {
      _saveRoleToUser(role);
    }

    notifyListeners();
  }

  // 从服务器获取用户角色
  Future<void> fetchUserRole() async {
    try {
      setLoading(true);

      final userRole = await ParseService.getUserRole();

      if (userRole == null) {
        // 如果没有获取到角色，默认为both
        _currentRole = UserRole.both;
      } else {
        // 根据字符串设置角色
        switch (userRole) {
          case 'consumer':
            _currentRole = UserRole.consumer;
            break;
          case 'producer':
            _currentRole = UserRole.producer;
            break;
          default:
            _currentRole = UserRole.both;
        }
      }

      _isOfflineMode = false;
    } catch (e) {
      print('获取用户角色失败: $e');
      // 在出错时设置离线模式并使用默认角色
      _isOfflineMode = true;
      _currentRole = UserRole.both;
    } finally {
      setLoading(false);
    }
  }

  // 保存角色到用户数据
  Future<void> _saveRoleToUser(UserRole role) async {
    if (_isOfflineMode) return; // 离线模式下不保存

    try {
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

      // 尝试保存用户角色
      final success = await ParseService.setUserRoleDirect(roleString);
      if (!success) {
        print('保存角色失败');
      }
    } catch (e) {
      print('保存角色异常: $e');
      // 设置为离线模式，但不改变当前选择的角色
      _isOfflineMode = true;
    }
  }
}
