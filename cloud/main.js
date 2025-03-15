/**
 * 泰康共享平台 Parse Server 云函数
 */

// 用户注册后触发的函数
Parse.Cloud.afterSave(Parse.User, async (request) => {
  const user = request.object;
  
  // 检查是否为新创建的用户
  if (user.isNew()) {
    console.log(`新用户注册: ${user.get('username')}`);
    
    // 可以在这里执行一些新用户注册后的操作
    // 例如: 创建默认数据、发送欢迎邮件等
  }
});

// 自定义云函数 - 获取用户个人资料
Parse.Cloud.define('getUserProfile', async (request) => {
  if (!request.user) {
    throw new Parse.Error(Parse.Error.INVALID_SESSION_TOKEN, '用户未登录');
  }
  
  const userId = request.user.id;
  const userQuery = new Parse.Query(Parse.User);
  
  try {
    const user = await userQuery.get(userId, { useMasterKey: true });
    
    // 返回用户信息（排除敏感字段）
    return {
      username: user.get('username'),
      email: user.get('email'),
      phone: user.get('phone'),
      createdAt: user.get('createdAt')
    };
  } catch (error) {
    throw new Parse.Error(Parse.Error.OBJECT_NOT_FOUND, '未找到用户');
  }
});

// 手机号验证函数（示例）
Parse.Cloud.define('verifyPhoneNumber', async (request) => {
  const { phoneNumber } = request.params;
  
  if (!phoneNumber || !/^1[3-9]\d{9}$/.test(phoneNumber)) {
    throw new Parse.Error(400, '无效的手机号码');
  }
  
  // 这里通常会接入短信验证服务
  // 本示例仅返回成功，实际应用中应该对接SMS服务
  
  return {
    success: true,
    message: '手机号验证通过'
  };
});

console.log('泰康共享平台云函数已加载'); 