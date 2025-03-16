import 'package:flutter/material.dart';
import 'dart:async';

class ConsumerScreen extends StatelessWidget {
  const ConsumerScreen({super.key});

  @override
  Widget build(BuildContext context) {
    // 模拟数据 - 在实际应用中应当从服务器获取
    final qrCodeProviders = [
      {
        'name': '泰康有限',
        'description': '海淀44F-提供食品和日用品的二维码',
        'qrCodes': 5,
        'icon': Icons.shopping_cart,
        'color': Colors.blue,
      },
      {
        'name': '中国银行',
        'description': '朝阳8F-各类服装折扣二维码',
        'qrCodes': 3,
        'icon': Icons.shopping_bag,
        'color': Colors.green,
      },
      {
        'name': 'Alibaba',
        'description': '朝阳1F-餐厅优惠和菜单二维码',
        'qrCodes': 7,
        'icon': Icons.restaurant,
        'color': Colors.orange,
      },
      {
        'name': '腾讯技术',
        'description': '深圳11F-电子产品促销二维码',
        'qrCodes': 2,
        'icon': Icons.devices,
        'color': Colors.purple,
      },
      {
        'name': '瑞幸咖啡',
        'description': '朝阳1F-咖啡及甜点优惠二维码',
        'qrCodes': 4,
        'icon': Icons.coffee,
        'color': Colors.brown,
      },
    ];

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
                    color: Colors.blue.withOpacity(0.1),
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: const Icon(Icons.person, color: Colors.blue),
                ),
                const SizedBox(width: 12),
                const Text(
                  '消费者模式',
                  style: TextStyle(
                    fontSize: 20,
                    fontWeight: FontWeight.bold,
                    color: Colors.black87,
                  ),
                ),
              ],
            ),
          ),
          // 小标题和描述
          Padding(
            padding: const EdgeInsets.all(16.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  '可用的二维码提供者',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 4),
                Text(
                  '点击查看提供者的二维码列表',
                  style: TextStyle(color: Colors.grey[600], fontSize: 14),
                ),
              ],
            ),
          ),
          // 列表
          Expanded(
            child: ListView.builder(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              itemCount: qrCodeProviders.length,
              itemBuilder: (context, index) {
                final provider = qrCodeProviders[index];
                return _buildProviderCard(
                  context,
                  name: provider['name'] as String,
                  description: provider['description'] as String,
                  qrCodes: provider['qrCodes'] as int,
                  icon: provider['icon'] as IconData,
                  color: provider['color'] as Color,
                );
              },
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
                    '点击提供者查看可用的二维码',
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

  Widget _buildProviderCard(
    BuildContext context, {
    required String name,
    required String description,
    required int qrCodes,
    required IconData icon,
    required Color color,
  }) {
    return Card(
      margin: const EdgeInsets.only(bottom: 16),
      elevation: 3,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: InkWell(
        onTap: () => _navigateToQrCodeDetail(context, name),
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
                child: Icon(icon, color: color, size: 32),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      name,
                      style: const TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      description,
                      style: TextStyle(color: Colors.grey[600], fontSize: 14),
                    ),
                    const SizedBox(height: 8),
                    Row(
                      children: [
                        Icon(Icons.qr_code, size: 16, color: Colors.grey[600]),
                        const SizedBox(width: 4),
                        Text(
                          '$qrCodes 个可用二维码',
                          style: TextStyle(
                            fontSize: 12,
                            color: Colors.grey[600],
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              Icon(Icons.arrow_forward_ios, size: 16, color: Colors.grey[500]),
            ],
          ),
        ),
      ),
    );
  }

  void _navigateToQrCodeDetail(BuildContext context, String providerName) {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (context) => QrCodeDetailScreen(providerName: providerName),
      ),
    );
  }
}

class QrCodeDetailScreen extends StatelessWidget {
  final String providerName;

  const QrCodeDetailScreen({super.key, required this.providerName});

  @override
  Widget build(BuildContext context) {
    // 模拟数据 - 在实际应用中应从服务器获取
    final qrCodes = List.generate(
      5,
      (index) => {
        'id': 'qr_$index',
        'name': '$providerName - 优惠码 ${index + 1}',
        'description': '使用此二维码可${index % 2 == 0 ? '享受折扣' : '兑换礼品'}',
        'validUntil': DateTime.now().add(Duration(days: 30 + index * 5)),
      },
    );

    return Scaffold(
      appBar: AppBar(
        title: Text(
          '$providerName 的二维码',
          style: const TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: 20,
            color: Colors.white,
          ),
        ),
        centerTitle: true,
        backgroundColor: const Color(0xFF1976D2),
      ),
      body: Container(
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
            Padding(
              padding: const EdgeInsets.all(16.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '$providerName 提供的二维码',
                    style: const TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    '点击查看详情并使用二维码',
                    style: TextStyle(color: Colors.grey[600], fontSize: 14),
                  ),
                ],
              ),
            ),
            Expanded(
              child: ListView.builder(
                padding: const EdgeInsets.symmetric(horizontal: 16),
                itemCount: qrCodes.length,
                itemBuilder: (context, index) {
                  final qrCode = qrCodes[index];
                  return _buildQrCodeCard(
                    context,
                    name: qrCode['name'] as String,
                    description: qrCode['description'] as String,
                    validUntil: qrCode['validUntil'] as DateTime,
                  );
                },
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildQrCodeCard(
    BuildContext context, {
    required String name,
    required String description,
    required DateTime validUntil,
  }) {
    return Card(
      margin: const EdgeInsets.only(bottom: 16),
      elevation: 3,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: InkWell(
        onTap: () => _showQrCodeDialog(context, name),
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(
                      color: Colors.blue.withOpacity(0.1),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: const Icon(Icons.qr_code, color: Color(0xFF1976D2)),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Text(
                      name,
                      style: const TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              Text(
                description,
                style: TextStyle(color: Colors.grey[600], fontSize: 14),
              ),
              const SizedBox(height: 8),
              Row(
                children: [
                  Icon(Icons.event, size: 16, color: Colors.grey[600]),
                  const SizedBox(width: 4),
                  Text(
                    '有效期至: ${validUntil.year}/${validUntil.month}/${validUntil.day}',
                    style: TextStyle(
                      fontSize: 12,
                      color: Colors.grey[600],
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              Align(
                alignment: Alignment.centerRight,
                child: ElevatedButton.icon(
                  onPressed: () => _showQrCodeDialog(context, name),
                  icon: const Icon(Icons.remove_red_eye),
                  label: const Text('查看二维码'),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFF1976D2),
                    foregroundColor: Colors.white,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(8),
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  void _showQrCodeDialog(BuildContext context, String qrName) {
    // 首先显示确认弹窗
    showDialog(
      context: context,
      builder:
          (context) => AlertDialog(
            title: const Text('确认使用'),
            content: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Icon(Icons.help_outline, color: Colors.amber, size: 60),
                const SizedBox(height: 16),
                Text('你确定使用 "$qrName" 这个二维码吗？'),
              ],
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.of(context).pop(),
                child: const Text('取消'),
              ),
              ElevatedButton(
                onPressed: () {
                  Navigator.of(context).pop();
                  _navigateToQrCodeProvideScreen(context, qrName);
                },
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFF1976D2),
                ),
                child: const Text('是'),
              ),
            ],
          ),
    );
  }

  // 跳转到二维码提供界面
  void _navigateToQrCodeProvideScreen(BuildContext context, String qrName) {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (context) => QrCodeProvideScreen(qrCodeName: qrName),
      ),
    );
  }
}

// 新增二维码提供界面
class QrCodeProvideScreen extends StatefulWidget {
  final String qrCodeName;

  const QrCodeProvideScreen({super.key, required this.qrCodeName});

  @override
  State<QrCodeProvideScreen> createState() => _QrCodeProvideScreenState();
}

class _QrCodeProvideScreenState extends State<QrCodeProvideScreen> {
  late int _seconds = 0;
  late Timer _timer;
  String get _formattedTime {
    final minutes = _seconds ~/ 60;
    final remainingSeconds = _seconds % 60;
    return '${minutes.toString().padLeft(2, '0')}:${remainingSeconds.toString().padLeft(2, '0')}';
  }

  @override
  void initState() {
    super.initState();
    _startTimer();
  }

  @override
  void dispose() {
    _timer.cancel();
    super.dispose();
  }

  void _startTimer() {
    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      setState(() {
        _seconds++;
      });
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text(
          '二维码使用',
          style: TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: 20,
            color: Colors.white,
          ),
        ),
        centerTitle: true,
        backgroundColor: const Color(0xFF1976D2),
      ),
      body: Container(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [Colors.blue.withOpacity(0.05), Colors.white],
          ),
        ),
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              // 计时器显示
              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 20,
                  vertical: 10,
                ),
                decoration: BoxDecoration(
                  color: const Color(0xFF1976D2).withOpacity(0.1),
                  borderRadius: BorderRadius.circular(20),
                  border: Border.all(
                    color: const Color(0xFF1976D2).withOpacity(0.3),
                  ),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Icon(Icons.timer, color: Color(0xFF1976D2)),
                    const SizedBox(width: 8),
                    Text(
                      _formattedTime,
                      style: const TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                        color: Color(0xFF1976D2),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 20),
              const Icon(
                Icons.qr_code_scanner,
                size: 70,
                color: Color(0xFF1976D2),
              ),
              const SizedBox(height: 24),
              Text(
                widget.qrCodeName,
                style: const TextStyle(
                  fontSize: 24,
                  fontWeight: FontWeight.bold,
                ),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 12),
              const Text(
                '请向服务提供方出示此二维码',
                style: TextStyle(fontSize: 16, color: Colors.grey),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 40),
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(12),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withOpacity(0.1),
                      blurRadius: 15,
                      offset: const Offset(0, 5),
                    ),
                  ],
                ),
                child: Image.network(
                  'https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=${Uri.encodeComponent(widget.qrCodeName)}',
                  width: 250,
                  height: 250,
                  errorBuilder:
                      (context, error, stackTrace) => const Center(
                        child: Icon(Icons.error, size: 100, color: Colors.red),
                      ),
                  loadingBuilder: (context, child, loadingProgress) {
                    if (loadingProgress == null) return child;
                    return const SizedBox(
                      width: 250,
                      height: 250,
                      child: Center(child: CircularProgressIndicator()),
                    );
                  },
                ),
              ),
              const Spacer(),
              SizedBox(
                width: double.infinity,
                height: 56,
                child: ElevatedButton.icon(
                  onPressed: () => _navigateToPaymentScreen(context),
                  icon: const Icon(Icons.check_circle),
                  label: const Text('使用完毕', style: TextStyle(fontSize: 18)),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFF1976D2),
                    foregroundColor: Colors.white,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12),
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  void _navigateToPaymentScreen(BuildContext context) {
    Navigator.of(
      context,
    ).push(MaterialPageRoute(builder: (context) => const PaymentScreen()));
  }
}

// 支付界面
class PaymentScreen extends StatefulWidget {
  const PaymentScreen({super.key});

  @override
  State<PaymentScreen> createState() => _PaymentScreenState();
}

class _PaymentScreenState extends State<PaymentScreen> {
  // 模拟数据
  double _balance = 100.0;
  double _amount = 35.50;
  bool _isProcessing = false;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text(
          '支付',
          style: TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: 20,
            color: Colors.white,
          ),
        ),
        centerTitle: true,
        backgroundColor: const Color(0xFF1976D2),
      ),
      body: Container(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [Colors.blue.withOpacity(0.05), Colors.white],
          ),
        ),
        child: Padding(
          padding: const EdgeInsets.all(20.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              const SizedBox(height: 20),
              Container(
                padding: const EdgeInsets.all(20),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(16),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withOpacity(0.1),
                      blurRadius: 15,
                      offset: const Offset(0, 5),
                    ),
                  ],
                ),
                child: Column(
                  children: [
                    const Text(
                      '支付详情',
                      style: TextStyle(
                        fontSize: 20,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 24),
                    _buildInfoRow('当前余额', '¥${_balance.toStringAsFixed(2)}'),
                    const SizedBox(height: 12),
                    _buildInfoRow('消费金额', '¥${_amount.toStringAsFixed(2)}'),
                    const SizedBox(height: 12),
                    const Divider(),
                    const SizedBox(height: 12),
                    _buildInfoRow(
                      '支付后余额',
                      '¥${(_balance - _amount).toStringAsFixed(2)}',
                      isBold: true,
                    ),
                  ],
                ),
              ),
              const Spacer(),
              SizedBox(
                width: double.infinity,
                height: 56,
                child: ElevatedButton.icon(
                  onPressed: _isProcessing ? null : _processPayment,
                  icon:
                      _isProcessing
                          ? const SizedBox(
                            width: 20,
                            height: 20,
                            child: CircularProgressIndicator(
                              color: Colors.white,
                            ),
                          )
                          : const Icon(Icons.payment),
                  label: Text(
                    _isProcessing ? '处理中...' : '确认支付',
                    style: const TextStyle(fontSize: 18),
                  ),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFF1976D2),
                    foregroundColor: Colors.white,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12),
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 20),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildInfoRow(String label, String value, {bool isBold = false}) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(label, style: TextStyle(fontSize: 16, color: Colors.grey[600])),
        Text(
          value,
          style: TextStyle(
            fontSize: 16,
            fontWeight: isBold ? FontWeight.bold : FontWeight.normal,
            color: isBold ? const Color(0xFF1976D2) : Colors.black,
          ),
        ),
      ],
    );
  }

  Future<void> _processPayment() async {
    setState(() {
      _isProcessing = true;
    });

    // 模拟支付处理
    await Future.delayed(const Duration(seconds: 2));

    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('支付成功！'), backgroundColor: Colors.green),
      );

      setState(() {
        _isProcessing = false;
        // 更新余额
        _balance -= _amount;
      });

      // 延迟一会后返回主页
      Future.delayed(const Duration(seconds: 1), () {
        if (mounted) {
          // 返回到首页
          Navigator.of(context).popUntil((route) => route.isFirst);
        }
      });
    }
  }
}
