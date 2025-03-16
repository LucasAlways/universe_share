import 'package:flutter/material.dart';
import '../services/floating_window_service.dart';
import 'dart:async';

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
                    onTap: () => _navigateToCreateQrCode(context),
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

  void _navigateToCreateQrCode(BuildContext context) {
    Navigator.of(
      context,
    ).push(MaterialPageRoute(builder: (context) => const CreateQrCodeScreen()));
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

class CreateQrCodeScreen extends StatefulWidget {
  const CreateQrCodeScreen({super.key});

  @override
  State<CreateQrCodeScreen> createState() => _CreateQrCodeScreenState();
}

class _CreateQrCodeScreenState extends State<CreateQrCodeScreen> {
  final TextEditingController _titleController = TextEditingController();
  final TextEditingController _descriptionController = TextEditingController();
  DateTime _expiryDate = DateTime.now().add(const Duration(days: 30));
  TimeOfDay _expiryTime = TimeOfDay.now();
  bool _isLoading = false;
  bool _qrCodeGenerated = false;
  String? _qrData;
  bool _hasPermission = false;

  // 添加倒计时相关变量
  Timer? _countdownTimer;
  int _remainingSeconds = 60; // 一分钟倒计时
  bool _isExpired = false;

  @override
  void initState() {
    super.initState();
    _checkFloatingWindowPermission();
  }

  @override
  void dispose() {
    _titleController.dispose();
    _descriptionController.dispose();
    _countdownTimer?.cancel();
    super.dispose();
  }

  Future<void> _checkFloatingWindowPermission() async {
    bool hasPermission =
        await FloatingWindowService.checkFloatingWindowPermission();
    setState(() {
      _hasPermission = hasPermission;
    });
  }

  Future<void> _requestPermission() async {
    setState(() {
      _isLoading = true;
    });

    bool result = await FloatingWindowService.requestFloatingWindowPermission();

    setState(() {
      _hasPermission = result;
      _isLoading = false;
    });
  }

  void _generateQrCode() async {
    setState(() {
      _isLoading = true;
    });

    // 模拟网络请求延迟
    await Future.delayed(const Duration(seconds: 1));

    // 生成二维码数据
    setState(() {
      _qrCodeGenerated = true;
      _qrData =
          '${_titleController.text.isEmpty ? "无标题二维码" : _titleController.text}_${DateTime.now().millisecondsSinceEpoch}';
      _isLoading = false;

      // 开始倒计时
      _startCountdown();
    });
  }

  // 添加倒计时方法
  void _startCountdown() {
    _countdownTimer = Timer.periodic(const Duration(seconds: 1), (timer) {
      setState(() {
        if (_remainingSeconds > 0) {
          _remainingSeconds--;
        } else {
          _isExpired = true;
          _countdownTimer?.cancel();
        }
      });
    });
  }

  // 获取格式化的倒计时显示
  String get _formattedCountdown {
    if (_isExpired) return "已过期";

    final minutes = _remainingSeconds ~/ 60;
    final seconds = _remainingSeconds % 60;
    return '${minutes.toString().padLeft(2, '0')}:${seconds.toString().padLeft(2, '0')}';
  }

  Future<void> _startFloatingWindow() async {
    try {
      setState(() {
        _isLoading = true;
      });

      // 首先检查权限
      if (!_hasPermission) {
        // 先请求权限
        bool result =
            await FloatingWindowService.requestFloatingWindowPermission();
        if (!result) {
          if (mounted) {
            ScaffoldMessenger.of(
              context,
            ).showSnackBar(const SnackBar(content: Text('需要悬浮窗权限才能继续')));
          }
          setState(() {
            _isLoading = false;
          });
          return;
        }
        setState(() {
          _hasPermission = result;
        });
      }

      // 再次确认权限已经获取
      bool hasPermission =
          await FloatingWindowService.checkFloatingWindowPermission();
      if (!hasPermission) {
        if (mounted) {
          ScaffoldMessenger.of(
            context,
          ).showSnackBar(const SnackBar(content: Text('未获得悬浮窗权限')));
        }
        setState(() {
          _isLoading = false;
          _hasPermission = false;
        });
        return;
      }

      // 尝试启动悬浮窗
      bool result = await FloatingWindowService.startFloatingWindow();
      if (result) {
        // 成功启动悬浮窗，由原生端处理退出到桌面的操作
        // 这里不需要执行任何操作，原生端会自动跳转到桌面
        print('悬浮窗启动成功，等待原生端跳转到桌面');
      } else {
        if (mounted) {
          ScaffoldMessenger.of(
            context,
          ).showSnackBar(const SnackBar(content: Text('启动悬浮窗失败，请确保已授予悬浮窗权限')));
        }
      }
    } catch (e) {
      print('启动悬浮窗出错: $e');
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('发生错误：$e')));
      }
    } finally {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text(
          '创建二维码',
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
            colors: [Colors.green.withOpacity(0.05), Colors.white],
          ),
        ),
        child: _qrCodeGenerated ? _buildQrCodeResult() : _buildQrCodeForm(),
      ),
    );
  }

  Widget _buildQrCodeForm() {
    return SingleChildScrollView(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(12),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withOpacity(0.05),
                    blurRadius: 10,
                    offset: const Offset(0, 3),
                  ),
                ],
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    '二维码信息',
                    style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 16),
                  TextField(
                    controller: _titleController,
                    decoration: const InputDecoration(
                      labelText: '二维码标题',
                      hintText: '例如：咖啡店八折优惠',
                      border: OutlineInputBorder(),
                    ),
                  ),
                  const SizedBox(height: 16),
                  TextField(
                    controller: _descriptionController,
                    maxLines: 3,
                    decoration: const InputDecoration(
                      labelText: '二维码描述',
                      hintText: '详细描述此二维码的用途',
                      border: OutlineInputBorder(),
                    ),
                  ),
                  const SizedBox(height: 16),
                  Row(
                    children: [
                      const Text('有效期至：'),
                      const SizedBox(width: 8),
                      Expanded(
                        child: TextButton.icon(
                          icon: const Icon(Icons.calendar_today),
                          label: Text(
                            '${_expiryDate.year}/${_expiryDate.month}/${_expiryDate.day} ${_expiryTime.format(context)}',
                          ),
                          onPressed: () async {
                            // 选择日期
                            final DateTime? pickedDate = await showDatePicker(
                              context: context,
                              initialDate: _expiryDate,
                              firstDate: DateTime.now(),
                              lastDate: DateTime.now().add(
                                const Duration(days: 365),
                              ),
                            );

                            if (pickedDate != null) {
                              // 选择时间
                              final TimeOfDay? pickedTime =
                                  await showTimePicker(
                                    context: context,
                                    initialTime: _expiryTime,
                                  );

                              if (pickedTime != null && mounted) {
                                setState(() {
                                  _expiryDate = pickedDate;
                                  _expiryTime = pickedTime;
                                });
                              }
                            }
                          },
                          style: TextButton.styleFrom(
                            foregroundColor: Colors.green,
                          ),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
            const SizedBox(height: 32),
            SizedBox(
              width: double.infinity,
              height: 50,
              child: ElevatedButton(
                onPressed: _isLoading ? null : _generateQrCode,
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFF1976D2),
                  foregroundColor: Colors.white,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12),
                  ),
                ),
                child:
                    _isLoading
                        ? const CircularProgressIndicator(color: Colors.white)
                        : const Text('开始', style: TextStyle(fontSize: 18)),
              ),
            ),
            const SizedBox(height: 16),
            Center(
              child: Text(
                '标题和描述非必填',
                style: TextStyle(color: Colors.grey[600], fontSize: 12),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildQrCodeResult() {
    return SingleChildScrollView(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            Container(
              padding: const EdgeInsets.all(24),
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
                  const Icon(Icons.check_circle, color: Colors.green, size: 60),
                  const SizedBox(height: 16),
                  const Text(
                    '二维码创建成功',
                    style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
                  ),
                  // 添加倒计时显示
                  Container(
                    margin: const EdgeInsets.only(top: 12, bottom: 12),
                    padding: const EdgeInsets.symmetric(
                      horizontal: 20,
                      vertical: 8,
                    ),
                    decoration: BoxDecoration(
                      color:
                          _isExpired
                              ? Colors.red.withOpacity(0.1)
                              : const Color(0xFF1976D2).withOpacity(0.1),
                      borderRadius: BorderRadius.circular(20),
                      border: Border.all(
                        color:
                            _isExpired
                                ? Colors.red
                                : const Color(0xFF1976D2).withOpacity(0.3),
                      ),
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(
                          _isExpired ? Icons.timer_off : Icons.timer,
                          color:
                              _isExpired ? Colors.red : const Color(0xFF1976D2),
                        ),
                        const SizedBox(width: 8),
                        Text(
                          _formattedCountdown,
                          style: TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.bold,
                            color:
                                _isExpired
                                    ? Colors.red
                                    : const Color(0xFF1976D2),
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 24),
                  Container(
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: Colors.grey[50],
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(color: Colors.grey.withOpacity(0.3)),
                    ),
                    child: Image.network(
                      'https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${Uri.encodeComponent(_qrData!)}',
                      width: 200,
                      height: 200,
                      errorBuilder:
                          (context, error, stackTrace) => const Center(
                            child: Icon(
                              Icons.error,
                              size: 100,
                              color: Colors.red,
                            ),
                          ),
                      loadingBuilder: (context, child, loadingProgress) {
                        if (loadingProgress == null) return child;
                        return const SizedBox(
                          width: 200,
                          height: 200,
                          child: Center(child: CircularProgressIndicator()),
                        );
                      },
                    ),
                  ),
                  const SizedBox(height: 24),
                  Container(
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: Colors.grey[50],
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          '标题: ${_titleController.text}',
                          style: const TextStyle(fontWeight: FontWeight.bold),
                        ),
                        const SizedBox(height: 8),
                        Text('描述: ${_descriptionController.text}'),
                        const SizedBox(height: 8),
                        Text(
                          '有效期至: ${_expiryDate.year}/${_expiryDate.month}/${_expiryDate.day} ${_expiryTime.format(context)}',
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 24),
            Row(
              children: [
                Expanded(
                  child: ElevatedButton.icon(
                    onPressed: _isLoading ? null : _startFloatingWindow,
                    icon: const Icon(Icons.phone_android, color: Colors.white),
                    label:
                        _isLoading
                            ? const SizedBox(
                              width: 20,
                              height: 20,
                              child: CircularProgressIndicator(
                                color: Colors.white,
                                strokeWidth: 2,
                              ),
                            )
                            : const Text('启动悬浮窗'),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: const Color(0xFF1976D2),
                      foregroundColor: Colors.white,
                      padding: const EdgeInsets.symmetric(vertical: 12),
                    ),
                  ),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: ElevatedButton.icon(
                    onPressed: () {
                      // 重置状态，创建新二维码
                      setState(() {
                        _qrCodeGenerated = false;
                        _titleController.clear();
                        _descriptionController.clear();
                        _expiryDate = DateTime.now().add(
                          const Duration(days: 30),
                        );
                      });
                    },
                    icon: const Icon(Icons.add, color: Colors.white),
                    label: const Text('创建新二维码'),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: const Color.fromARGB(255, 28, 149, 28),
                      foregroundColor: Colors.white,
                      padding: const EdgeInsets.symmetric(vertical: 12),
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            if (!_hasPermission)
              ElevatedButton.icon(
                onPressed: _requestPermission,
                icon: const Icon(Icons.security, color: Colors.white),
                label: const Text('授予悬浮窗权限'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color.fromARGB(255, 235, 137, 18),
                  foregroundColor: Colors.white,
                ),
              ),
            const SizedBox(height: 16),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: Colors.blue.withOpacity(0.1),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Row(
                children: [
                  Icon(Icons.info_outline, color: Colors.blue.shade700),
                  const SizedBox(width: 12),
                  const Expanded(
                    child: Text(
                      '点击"启动悬浮窗"将允许您在后台继续提供二维码功能',
                      style: TextStyle(color: Colors.black87),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
