import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/role_model.dart';

class ModeSelectionScreen extends StatelessWidget {
  const ModeSelectionScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final roleModel = Provider.of<RoleModel>(context);

    return Container(
      decoration: BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [Colors.blue.withOpacity(0.05), Colors.white],
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            padding: const EdgeInsets.all(16.0),
            decoration: BoxDecoration(
              color: Colors.white,
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withOpacity(0.05),
                  blurRadius: 10,
                  offset: const Offset(0, 3),
                ),
              ],
            ),
            child: Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(8),
                  decoration: BoxDecoration(
                    color: Colors.blue.withOpacity(0.1),
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: const Icon(Icons.touch_app, color: Colors.blue),
                ),
                const SizedBox(width: 12),
                const Text(
                  '选择模式',
                  style: TextStyle(
                    fontSize: 20,
                    fontWeight: FontWeight.bold,
                    color: Colors.black87,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 20),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16.0),
            child: Text(
              '请选择您希望使用的操作模式',
              style: TextStyle(fontSize: 14, color: Colors.grey[600]),
            ),
          ),
          Expanded(
            child: Padding(
              padding: const EdgeInsets.all(16.0),
              child: GridView.count(
                crossAxisCount: 2,
                crossAxisSpacing: 16,
                mainAxisSpacing: 16,
                children: [
                  _buildModeCard(
                    context,
                    title: '消费者模式',
                    icon: Icons.smartphone,
                    description: '浏览和使用二维码',
                    isActive: roleModel.isConsumer,
                    color: Colors.blue,
                    onTap: () {
                      // 设置为消费者角色
                      roleModel.setUserRole(UserRole.consumer);
                      _showModeActivatedSnackbar(context, '消费者模式已激活');
                    },
                  ),
                  _buildModeCard(
                    context,
                    title: '生产者模式',
                    icon: Icons.add_box,
                    description: '创建和管理二维码',
                    isActive: roleModel.isProducer,
                    color: Colors.green,
                    onTap: () {
                      // 设置为生产者角色
                      roleModel.setUserRole(UserRole.producer);
                      _showModeActivatedSnackbar(context, '生产者模式已激活');
                    },
                  ),
                ],
              ),
            ),
          ),
          // 底部帮助提示
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: Colors.grey[50],
              border: Border(
                top: BorderSide(color: Colors.grey.withOpacity(0.2)),
              ),
            ),
            child: Row(
              children: [
                Icon(Icons.info_outline, color: Colors.grey[600], size: 20),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    '您可以随时在此页面切换操作模式',
                    style: TextStyle(color: Colors.grey[600], fontSize: 13),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildModeCard(
    BuildContext context, {
    required String title,
    required IconData icon,
    required String description,
    required bool isActive,
    required Color color,
    required VoidCallback onTap,
  }) {
    return GestureDetector(
      onTap: onTap,
      child: Card(
        elevation: 4,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
          side: isActive ? BorderSide(color: color, width: 2) : BorderSide.none,
        ),
        child: Container(
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(16),
            gradient:
                isActive
                    ? LinearGradient(
                      colors: [color.withOpacity(0.1), Colors.white],
                      begin: Alignment.topLeft,
                      end: Alignment.bottomRight,
                    )
                    : null,
          ),
          padding: const EdgeInsets.all(16),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(icon, size: 50, color: color),
              const SizedBox(height: 16),
              Text(
                title,
                style: const TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                description,
                textAlign: TextAlign.center,
                style: TextStyle(color: Colors.grey[600], fontSize: 12),
              ),
              if (isActive)
                Padding(
                  padding: const EdgeInsets.only(top: 8.0),
                  child: Chip(
                    backgroundColor: color.withOpacity(0.2),
                    label: const Text(
                      '已激活',
                      style: TextStyle(
                        fontSize: 12,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    avatar: Icon(Icons.check_circle, color: color, size: 16),
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }

  void _showModeActivatedSnackbar(BuildContext context, String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
        duration: const Duration(seconds: 2),
        action: SnackBarAction(
          label: '确定',
          onPressed: () {
            ScaffoldMessenger.of(context).hideCurrentSnackBar();
          },
        ),
      ),
    );
  }
}
