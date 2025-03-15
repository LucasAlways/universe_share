import 'package:flutter/material.dart';

class ProducerScreen extends StatelessWidget {
  const ProducerScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [Colors.green.withOpacity(0.05), Colors.white],
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 标题栏
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
                    color: Colors.green.withOpacity(0.1),
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: const Icon(Icons.business, color: Colors.green),
                ),
                const SizedBox(width: 12),
                const Text(
                  '生产者模式',
                  style: TextStyle(
                    fontSize: 20,
                    fontWeight: FontWeight.bold,
                    color: Colors.black87,
                  ),
                ),
              ],
            ),
          ),
          Expanded(
            child: Padding(
              padding: const EdgeInsets.all(16.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const SizedBox(height: 8),
                  Text(
                    '您可以在此创建和管理您的二维码',
                    style: TextStyle(color: Colors.grey[600], fontSize: 14),
                  ),
                  const SizedBox(height: 24),
                  _buildFeatureCard(
                    context,
                    title: '创建二维码',
                    icon: Icons.qr_code,
                    color: Colors.green,
                    description: '创建新的二维码供消费者使用',
                    onTap: () => _showFeatureMessage(context, '创建二维码'),
                  ),
                  const SizedBox(height: 16),
                  _buildFeatureCard(
                    context,
                    title: '管理我的二维码',
                    icon: Icons.list_alt,
                    color: Colors.orange,
                    description: '查看和管理已创建的二维码',
                    onTap: () => _showFeatureMessage(context, '管理二维码'),
                  ),
                  const SizedBox(height: 16),
                  _buildFeatureCard(
                    context,
                    title: '分析使用情况',
                    icon: Icons.analytics,
                    color: Colors.purple,
                    description: '查看二维码的使用统计和分析',
                    onTap: () => _showFeatureMessage(context, '使用分析'),
                  ),
                ],
              ),
            ),
          ),
          // 底部提示
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
                    '二维码创建后可以方便地分享给消费者',
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

  Widget _buildFeatureCard(
    BuildContext context, {
    required String title,
    required IconData icon,
    required Color color,
    required String description,
    required VoidCallback onTap,
  }) {
    return Card(
      elevation: 3,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Row(
            children: [
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: color.withOpacity(0.1),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Icon(icon, color: color, size: 30),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: const TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      description,
                      style: TextStyle(color: Colors.grey[600], fontSize: 12),
                    ),
                  ],
                ),
              ),
              const Icon(Icons.arrow_forward_ios, size: 16),
            ],
          ),
        ),
      ),
    );
  }

  void _showFeatureMessage(BuildContext context, String feature) {
    showDialog(
      context: context,
      builder:
          (context) => AlertDialog(
            title: const Text('提示'),
            content: Text('$feature功能将在后续版本中实现'),
            actions: [
              TextButton(
                onPressed: () {
                  Navigator.of(context).pop();
                },
                child: const Text('确定'),
              ),
            ],
          ),
    );
  }
}
