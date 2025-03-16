import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/user_model.dart';
import '../models/role_model.dart';
import './login_screen.dart';
import './mode_selection_screen.dart';
import './consumer_screen.dart';
import './producer_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen>
    with SingleTickerProviderStateMixin {
  int _selectedIndex = 0;
  late TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 3, vsync: this);

    // 在初始化时加载用户角色
    WidgetsBinding.instance.addPostFrameCallback((_) {
      Provider.of<RoleModel>(context, listen: false).fetchUserRole();
    });
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  void _onItemTapped(int index) {
    setState(() {
      _selectedIndex = index;
    });

    // 添加动画效果
    _tabController.animateTo(
      index,
      duration: const Duration(milliseconds: 300),
      curve: Curves.easeInOut,
    );
  }

  Future<void> _logout() async {
    final userModel = Provider.of<UserModel>(context, listen: false);
    final success = await userModel.logout();

    if (success && mounted) {
      Navigator.of(context).pushReplacement(
        MaterialPageRoute(builder: (context) => const LoginScreen()),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: const Color(0xFF1976D2), // 使用标准蓝色
        title: const Text(
          '泰康',
          style: TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: 22,
            color: Colors.white, // 将字体颜色改为白色以保持一致性
          ),
        ),
        centerTitle: true,
        leading:
            _selectedIndex == 0
                ? Consumer<RoleModel>(
                  builder: (context, roleModel, child) {
                    // 如果当前不是选择模式界面，则显示模式切换按钮
                    if (roleModel.currentRole != UserRole.both) {
                      return Container(
                        padding: const EdgeInsets.only(left: 4.0),
                        alignment: Alignment.center,
                        child: TextButton.icon(
                          icon: const Icon(
                            Icons.swap_horiz,
                            color: Colors.white,
                            size: 20,
                          ),
                          label: const Text(
                            '切换',
                            style: TextStyle(color: Colors.white, fontSize: 14),
                          ),
                          onPressed: () {
                            // 显示确认对话框
                            _showModeChangeConfirmation(context, roleModel);
                          },
                          style: TextButton.styleFrom(
                            minimumSize: Size.zero,
                            padding: const EdgeInsets.symmetric(
                              horizontal: 6,
                              vertical: 4,
                            ),
                            tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                          ),
                        ),
                      );
                    }
                    return const SizedBox.shrink();
                  },
                )
                : null,
        actions: [
          Container(
            padding: const EdgeInsets.only(right: 4.0),
            alignment: Alignment.center,
            child: TextButton.icon(
              icon: const Icon(Icons.logout, color: Colors.white, size: 20),
              label: const Text(
                '退出',
                style: TextStyle(color: Colors.white, fontSize: 14),
              ),
              onPressed: _logout,
              style: TextButton.styleFrom(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(8),
                ),
              ),
            ),
          ),
        ],
      ),
      body: _buildBody(),
      bottomNavigationBar: BottomNavigationBar(
        items: const <BottomNavigationBarItem>[
          BottomNavigationBarItem(
            icon: Icon(Icons.qr_code_scanner),
            activeIcon: Icon(Icons.qr_code_scanner_rounded),
            label: '操作',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.article_outlined),
            activeIcon: Icon(Icons.article),
            label: '订单详情',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.settings_outlined),
            activeIcon: Icon(Icons.settings),
            label: '设置',
          ),
        ],
        currentIndex: _selectedIndex,
        selectedItemColor: Colors.blue,
        unselectedItemColor: Colors.grey,
        selectedFontSize: 14,
        unselectedFontSize: 12,
        type: BottomNavigationBarType.fixed,
        onTap: _onItemTapped,
      ),
    );
  }

  Widget _buildBody() {
    if (_selectedIndex == 0) {
      // 操作页面，根据角色显示不同内容
      return Consumer<RoleModel>(
        builder: (context, roleModel, child) {
          if (roleModel.isLoading) {
            return const Center(child: CircularProgressIndicator());
          }

          // 默认显示模式选择界面
          if (roleModel.currentRole == UserRole.both) {
            return const ModeSelectionScreen();
          } else if (roleModel.isConsumer) {
            return const ConsumerScreen();
          } else if (roleModel.isProducer) {
            return const ProducerScreen();
          } else {
            return const ModeSelectionScreen();
          }
        },
      );
    } else if (_selectedIndex == 1) {
      // 订单详情页面
      return const Center(
        child: Text('订单详情页面', style: TextStyle(fontSize: 24)),
      );
    } else if (_selectedIndex == 2) {
      // 设置页面
      return const Center(child: Text('设置页面', style: TextStyle(fontSize: 24)));
    } else {
      return const Center(child: Text('未知页面', style: TextStyle(fontSize: 24)));
    }
  }

  // 显示模式切换确认对话框
  void _showModeChangeConfirmation(BuildContext context, RoleModel roleModel) {
    String currentMode = roleModel.isConsumer ? '消费者模式' : '生产者模式';
    showDialog(
      context: context,
      builder:
          (context) => AlertDialog(
            title: const Text('确认切换模式'),
            content: Text('您确定要离开$currentMode，切换至其他模式吗？'),
            actions: [
              TextButton(
                onPressed: () {
                  Navigator.of(context).pop(); // 关闭对话框
                },
                child: const Text('取消'),
              ),
              TextButton(
                onPressed: () {
                  Navigator.of(context).pop(); // 关闭对话框
                  // 设置为both角色，显示选择界面
                  roleModel.setUserRole(UserRole.both);
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(
                      content: Text('已切换到模式选择'),
                      duration: Duration(seconds: 2),
                    ),
                  );
                },
                style: TextButton.styleFrom(foregroundColor: Colors.blue),
                child: const Text('确定'),
              ),
            ],
          ),
    );
  }
}
