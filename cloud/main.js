/**
 * 泰康共享平台 Parse Server 云函数
 */

// 用户注册后触发的函数
Parse.Cloud.afterSave(Parse.User, async (request) => {
  const user = request.object;
  
  // 检查是否为新创建的用户
  if (user.isNew()) {
    console.log(`新用户注册: ${user.get('username')}`);
    
    // 为新用户设置默认角色 - 消费者
    if (!user.has('userRole')) {
      user.set('userRole', 'consumer');
      try {
        await user.save(null, { useMasterKey: true });
        console.log(`已为用户 ${user.get('username')} 设置默认角色: consumer`);
      } catch (error) {
        console.error('设置默认角色失败:', error);
      }
    }
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
      userRole: user.get('userRole') || 'consumer', // 默认为消费者
      createdAt: user.get('createdAt')
    };
  } catch (error) {
    throw new Parse.Error(Parse.Error.OBJECT_NOT_FOUND, '未找到用户');
  }
});

// 设置用户角色
Parse.Cloud.define('setUserRole', async (request) => {
  if (!request.user) {
    throw new Parse.Error(Parse.Error.INVALID_SESSION_TOKEN, '用户未登录');
  }
  
  const { role } = request.params;
  
  // 验证角色有效性
  if (!['consumer', 'producer', 'both'].includes(role)) {
    throw new Parse.Error(Parse.Error.INVALID_PARAMS, '无效的角色类型');
  }
  
  try {
    const user = request.user;
    user.set('userRole', role);
    await user.save(null, { useMasterKey: true });
    
    return {
      success: true,
      message: '角色设置成功',
      role: role
    };
  } catch (error) {
    throw new Parse.Error(Parse.Error.INTERNAL_SERVER_ERROR, '设置角色失败: ' + error.message);
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