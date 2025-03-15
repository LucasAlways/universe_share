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
        title: const Text(
          '泰康', // 更新为泰康
          style: TextStyle(fontWeight: FontWeight.bold, fontSize: 24),
        ),
        centerTitle: true,
        actions: [
          // 显示模式切换按钮，只在第一个标签页时显示
          if (_selectedIndex == 0)
            Consumer<RoleModel>(
              builder: (context, roleModel, child) {
                // 如果当前不是选择模式界面，则显示模式切换按钮
                if (roleModel.currentRole != UserRole.both) {
                  return IconButton(
                    icon: const Icon(Icons.swap_horiz),
                    tooltip: '切换模式',
                    onPressed: () {
                      // 设置为both角色，显示选择界面
                      roleModel.setUserRole(UserRole.both);
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(
                          content: Text('已切换到模式选择'),
                          duration: Duration(seconds: 2),
                        ),
                      );
                    },
                  );
                }
                return const SizedBox.shrink();
              },
            ),
          IconButton(
            icon: const Icon(Icons.logout),
            onPressed: _logout,
            tooltip: '退出登录',
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
}
