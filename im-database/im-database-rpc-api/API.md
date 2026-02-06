# im-database-rpc-api API 文档

## 概述

`im-database-rpc-api` 是即时通讯系统的数据库 RPC API 模块，提供了 13 个 Dubbo 服务接口，用于通过远程过程调用（RPC）方式访问即时通讯系统的核心数据服务。

### 模块信息

- **模块名称**: im-database-rpc-api
- **描述**: Database RPC API for Dubbo services (Web/MVC)
- **依赖模块**:
    - im-database-domain: 数据域模型
    - im-starter-core: 核心组件
    - im-starter-dubbo: Dubbo 服务启动器

### 技术栈

- Apache Dubbo: RPC 框架
- MyBatis Plus: ORM 框架
- Java 8+

---

## 服务接口列表

本模块提供以下 13 个 Dubbo 服务接口：

1. **ImUserDubboService** - 用户基础信息服务
2. **ImUserDataDubboService** - 用户扩展资料服务
3. **ImFriendshipDubboService** - 好友关系服务
4. **ImFriendshipRequestDubboService** - 好友请求服务
5. **ImGroupDubboService** - 群组服务
6. **ImGroupMemberDubboService** - 群成员服务
7. **ImGroupInviteRequestDubboService** - 群邀请请求服务
8. **ImSingleMessageDubboService** - 单聊消息服务
9. **ImGroupMessageDubboService** - 群聊消息服务
10. **ImChatDubboService** - 聊天会话服务
11. **ImUserEmojiPackDubboService** - 用户表情包服务
12. **IMOutboxDubboService** - 消息出站服务
13. **ImAuthTokenDubboService** - 认证令牌服务

---

## 1. ImUserDubboService - 用户基础信息服务

### 接口描述

提供用户基础信息的增删改查操作。

### 接口全限定名

```
com.xy.lucky.api.user.ImUserDubboService
```

### 方法列表

#### 1.1 获取用户列表

```java
List<ImUserPo> queryList()
```

**功能说明**: 获取所有用户列表

**参数**: 无

**返回值**:

- `List<ImUserPo>`: 用户基础信息列表

**异常**: 无

**示例代码**:

```java

@Autowired
private ImUserDubboService imUserDubboService;

public void getAllUsers() {
    List<ImUserPo> users = imUserDubboService.queryList();
    users.forEach(user -> {
        System.out.println("用户ID: " + user.getUserId());
        System.out.println("用户名: " + user.getUserName());
        System.out.println("手机号: " + user.getMobile());
    });
}
```

---

#### 1.2 获取单个用户信息

```java
ImUserPo queryOne(String userId)
```

**功能说明**: 根据用户ID获取用户基础信息

**参数**:

- `userId` (String): 用户ID，必填

**返回值**:

- `ImUserPo`: 用户基础信息对象，不存在时返回 null

**异常**: 无

**示例代码**:

```java
public void getUserInfo(String userId) {
    ImUserPo user = imUserDubboService.queryOne(userId);
    if (user != null) {
        System.out.println("用户信息: " + user);
    } else {
        System.out.println("用户不存在");
    }
}
```

---

#### 1.3 创建用户

```java
Boolean creat(ImUserPo userDataPo)
```

**功能说明**: 创建单个用户基础信息

**参数**:

- `userDataPo` (ImUserPo): 用户基础信息对象
    - `userId` (String): 用户ID，必填
    - `userName` (String): 用户名，必填
    - `password` (String): 密码，必填
    - `mobile` (String): 手机号，可选

**返回值**:

- `Boolean`: true-创建成功，false-创建失败

**异常**: 无

**示例代码**:

```java
public void createUser() {
    ImUserPo user = new ImUserPo();
    user.setUserId("100001");
    user.setUserName("zhangsan");
    user.setPassword("encrypted_password");
    user.setMobile("13800138000");

    Boolean result = imUserDubboService.creat(user);
    if (result) {
        System.out.println("用户创建成功");
    } else {
        System.out.println("用户创建失败");
    }
}
```

---

#### 1.4 批量创建用户

```java
Boolean creatBatch(List<ImUserPo> userDataPoList)
```

**功能说明**: 批量创建用户基础信息

**参数**:

- `userDataPoList` (List<ImUserPo>): 用户基础信息对象列表

**返回值**:

- `Boolean`: true-批量创建成功，false-批量创建失败

**异常**: 无

**示例代码**:

```java
public void batchCreateUsers() {
    List<ImUserPo> users = new ArrayList<>();

    ImUserPo user1 = new ImUserPo();
    user1.setUserId("100001");
    user1.setUserName("user1");
    user1.setPassword("pass1");
    user1.setMobile("13800138001");

    ImUserPo user2 = new ImUserPo();
    user2.setUserId("100002");
    user2.setUserName("user2");
    user2.setPassword("pass2");
    user2.setMobile("13800138002");

    users.add(user1);
    users.add(user2);

    Boolean result = imUserDubboService.creatBatch(users);
    System.out.println("批量创建结果: " + result);
}
```

---

#### 1.5 更新用户信息

```java
Boolean modify(ImUserPo userDataPo)
```

**功能说明**: 更新用户基础信息

**参数**:

- `userDataPo` (ImUserPo): 用户基础信息对象
    - `userId` (String): 用户ID，必填

**返回值**:

- `Boolean`: true-更新成功，false-更新失败

**异常**: 无

**示例代码**:

```java
public void updateUser(String userId) {
    ImUserPo user = new ImUserPo();
    user.setUserId(userId);
    user.setUserName("new_username");
    user.setMobile("13900139000");

    Boolean result = imUserDubboService.modify(user);
    System.out.println("更新结果: " + result);
}
```

---

#### 1.6 删除用户

```java
Boolean removeOne(String userId)
```

**功能说明**: 根据用户ID删除用户基础信息

**参数**:

- `userId` (String): 用户ID，必填

**返回值**:

- `Boolean`: true-删除成功，false-删除失败

**异常**: 无

**示例代码**:

```java
public void deleteUser(String userId) {
    Boolean result = imUserDubboService.removeOne(userId);
    if (result) {
        System.out.println("用户删除成功");
    } else {
        System.out.println("用户删除失败");
    }
}
```

---

#### 1.7 根据手机号获取用户信息

```java
ImUserPo queryOneByMobile(String phoneNumber)
```

**功能说明**: 根据手机号查询用户基础信息

**参数**:

- `phoneNumber` (String): 手机号码，必填

**返回值**:

- `ImUserPo`: 用户基础信息对象，不存在时返回 null

**异常**: 无

**示例代码**:

```java
public void getUserByMobile(String mobile) {
    ImUserPo user = imUserDubboService.queryOneByMobile(mobile);
    if (user != null) {
        System.out.println("找到用户: " + user.getUserName());
    } else {
        System.out.println("未找到该手机号对应的用户");
    }
}
```

---

### 数据模型: ImUserPo

```java
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户基础信息")
@TableName(value = "im_user")
public class ImUserPo extends BasePo {

    /**
     * 用户ID
     */
    @TableId(value = "user_id")
    private String userId;

    /**
     * 用户名
     */
    @TableField(value = "user_name")
    private String userName;

    /**
     * 密码
     */
    @TableField(value = "password")
    private String password;

    /**
     * 手机号
     */
    @TableField(value = "mobile")
    private String mobile;
}
```

**字段说明**:

- `userId`: 用户唯一标识
- `userName`: 用户登录名
- `password`: 用户密码（通常为加密存储）
- `password`: 用户手机号

**继承字段** (来自 BasePo):

- `createTime`: 创建时间
- `updateTime`: 更新时间
- `delFlag`: 删除标记
- `version`: 乐观锁版本号

---

## 2. ImUserDataDubboService - 用户扩展资料服务

### 接口描述

提供用户扩展资料（昵称、头像、个性签名等）的增删改查操作。

### 接口全限定名

```
com.xy.lucky.api.user.ImUserDataDubboService
```

### 方法列表

#### 2.1 获取用户扩展资料

```java
ImUserDataPo queryOne(String userId)
```

**功能说明**: 根据用户ID获取用户扩展资料

**参数**:

- `userId` (String): 用户ID，必填

**返回值**:

- `ImUserDataPo`: 用户扩展资料对象，不存在时返回 null

**异常**: 无

**示例代码**:

```java

@Autowired
private ImUserDataDubboService imUserDataDubboService;

public void getUserProfile(String userId) {
    ImUserDataPo profile = imUserDataDubboService.queryOne(userId);
    if (profile != null) {
        System.out.println("昵称: " + profile.getName());
        System.out.println("头像: " + profile.getAvatar());
        System.out.println("个性签名: " + profile.getSelfSignature());
    }
}
```

---

#### 2.2 创建用户扩展资料

```java
Boolean creat(ImUserDataPo userDataPo)
```

**功能说明**: 创建用户扩展资料

**参数**:

- `userDataPo` (ImUserDataPo): 用户扩展资料对象
    - `userId` (String): 用户ID，必填

**返回值**:

- `Boolean`: true-创建成功，false-创建失败

**异常**: 无

**示例代码**:

```java
public void createUserProfile(String userId) {
    ImUserDataPo profile = new ImUserDataPo();
    profile.setUserId(userId);
    profile.setName("张三");
    profile.setAvatar("https://example.com/avatar.jpg");
    profile.setGender(1);
    profile.setBirthday("1990-01-01");
    profile.setLocation("北京市");
    profile.setSelfSignature("这个人很懒，什么都没留下");
    profile.setFriendAllowType(2);

    Boolean result = imUserDataDubboService.creat(profile);
    System.out.println("创建结果: " + result);
}
```

---

#### 2.3 批量创建用户扩展资料

```java
Boolean creatBatch(List<ImUserDataPo> userDataPoList)
```

**功能说明**: 批量创建用户扩展资料

**参数**:

- `userDataPoList` (List<ImUserDataPo>): 用户扩展资料对象列表

**返回值**:

- `Boolean`: true-批量创建成功，false-批量创建失败

**异常**: 无

**示例代码**:

```java
public void batchCreateUserProfiles() {
    List<ImUserDataPo> profiles = new ArrayList<>();

    ImUserDataPo profile1 = new ImUserDataPo();
    profile1.setUserId("100001");
    profile1.setName("用户1");

    ImUserDataPo profile2 = new ImUserDataPo();
    profile2.setUserId("100002");
    profile2.setName("用户2");

    profiles.add(profile1);
    profiles.add(profile2);

    Boolean result = imUserDataDubboService.creatBatch(profiles);
    System.out.println("批量创建结果: " + result);
}
```

---

#### 2.4 更新用户扩展资料

```java
Boolean modify(ImUserDataPo userDataPo)
```

**功能说明**: 更新用户扩展资料

**参数**:

- `userDataPo` (ImUserDataPo): 用户扩展资料对象
    - `userId` (String): 用户ID，必填

**返回值**:

- `Boolean`: true-更新成功，false-更新失败

**异常**: 无

**示例代码**:

```java
public void updateUserProfile(String userId) {
    ImUserDataPo profile = new ImUserDataPo();
    profile.setUserId(userId);
    profile.setName("新昵称");
    profile.setAvatar("https://example.com/new-avatar.jpg");

    Boolean result = imUserDataDubboService.modify(profile);
    System.out.println("更新结果: " + result);
}
```

---

#### 2.5 关键词搜索用户

```java
List<ImUserDataPo> queryByKeyword(String keyword)
```

**功能说明**: 根据关键词搜索用户（昵称、手机号等）

**参数**:

- `keyword` (String): 搜索关键词，必填

**返回值**:

- `List<ImUserDataPo>`: 匹配的用户扩展资料列表

**异常**: 无

**示例代码**:

```java
public void searchUsers(String keyword) {
    List<ImUserDataPo> users = imUserDataDubboService.queryByKeyword(keyword);
    System.out.println("找到 " + users.size() + " 个用户");
    users.forEach(user -> {
        System.out.println("用户ID: " + user.getUserId() + ", 昵称: " + user.getName());
    });
}
```

---

#### 2.6 批量获取用户扩展资料

```java
List<ImUserDataPo> queryListByIds(List<String> userIdList)
```

**功能说明**: 根据用户ID列表批量获取用户扩展资料

**参数**:

- `userIdList` (List<String>): 用户ID列表，必填

**返回值**:

- `List<ImUserDataPo>`: 用户扩展资料列表

**异常**: 无

**示例代码**:

```java
public void batchGetUsers(List<String> userIds) {
    List<ImUserDataPo> users = imUserDataDubboService.queryListByIds(userIds);
    users.forEach(user -> {
        System.out.println("用户: " + user.getName());
    });
}
```

---

#### 2.7 删除用户扩展资料

```java
Boolean removeOne(String userId)
```

**功能说明**: 根据用户ID删除用户扩展资料

**参数**:

- `userId` (String): 用户ID，必填

**返回值**:

- `Boolean`: true-删除成功，false-删除失败

**异常**: 无

**示例代码**:

```java
public void deleteUserProfile(String userId) {
    Boolean result = imUserDataDubboService.removeOne(userId);
    System.out.println("删除结果: " + result);
}
```

---

### 数据模型: ImUserDataPo

```java

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户扩展资料信息")
@TableName(value = "im_user_data")
public class ImUserDataPo extends BasePo {

    /**
     * 用户ID
     */
    @TableId(value = "user_id")
    private String userId;

    /**
     * 昵称
     */
    @TableField(value = "name")
    private String name;

    /**
     * 头像
     */
    @TableField(value = "avatar")
    private String avatar;

    /**
     * 性别
     */
    @TableField(value = "gender")
    private Integer gender;

    /**
     * 生日
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField(value = "birthday")
    private String birthday;

    /**
     * 地址
     */
    @TableField(value = "location")
    private String location;

    /**
     * 个性签名
     */
    @TableField(value = "self_signature")
    private String selfSignature;

    /**
     * 加好友验证类型（1无需验证，2需要验证）
     */
    @TableField(value = "friend_allow_type")
    private Integer friendAllowType;

    /**
     * 禁用标识（1禁用）
     */
    @TableField(value = "forbidden_flag")
    private Integer forbiddenFlag;

    /**
     * 管理员禁止添加好友：0未禁用，1已禁用
     */
    @TableField(value = "disable_add_friend")
    private Integer disableAddFriend;

    /**
     * 禁言标识（1禁言）
     */
    @TableField(value = "silent_flag")
    private Integer silentFlag;

    /**
     * 用户类型（1普通用户，2客服，3机器人）
     */
    @TableField(value = "user_type")
    private Integer userType;

    /**
     * 扩展字段
     */
    @TableField(value = "extra")
    private String extra;
}
```

**字段说明**:

- `userId`: 用户唯一标识
- `name`: 用户昵称
- `avatar`: 用户头像URL
- `gender`: 性别（0-未知，1-男，2-女）
- `birthday`: 生日，格式 yyyy-MM-dd
- `location`: 所在地
- `selfSignature`: 个性签名
- `friendAllowType`: 加好友验证方式（1-无需验证，2-需要验证）
- `forbiddenFlag`: 账号禁用标识（0-正常，1-禁用）
- `disableAddFriend`: 禁止添加好友标识（0-允许，1-禁止）
- `silentFlag`: 禁言标识（0-正常，1-禁言）
- `userType`: 用户类型（1-普通用户，2-客服，3-机器人）
- `extra`: JSON格式的扩展字段

---

## 3. ImFriendshipDubboService - 好友关系服务

### 接口描述

提供好友关系的增删改查操作，包括黑名单管理。

### 接口全限定名

```
com.xy.lucky.api.friend.ImFriendshipDubboService
```

### 方法列表

#### 3.1 根据时间序列查询好友列表

```java
List<ImFriendshipPo> queryList(String ownerId, Long sequence)
```

**功能说明**: 根据用户ID和序列号查询好友列表（用于增量同步）

**参数**:

- `ownerId` (String): 用户ID，必填
- `sequence` (Long): 序列号，可选（null表示查询全部）

**返回值**:

- `List<ImFriendshipPo>`: 好友关系列表

**异常**: 无

**示例代码**:

```java

@Autowired
private ImFriendshipDubboService imFriendshipDubboService;

public void getFriendList(String userId) {
    // 查询所有好友
    List<ImFriendshipPo> friends = imFriendshipDubboService.queryList(userId, null);
    System.out.println("好友数量: " + friends.size());

    // 增量查询（从某个序列号之后的好友）
    List<ImFriendshipPo> newFriends = imFriendshipDubboService.queryList(userId, 1234567890L);
    System.out.println("新增好友数量: " + newFriends.size());
}
```

---

#### 3.2 查询单个好友关系

```java
ImFriendshipPo queryOne(String ownerId, String toId)
```

**功能说明**: 查询指定用户与某个好友的关系

**参数**:

- `ownerId` (String): 用户ID，必填
- `toId` (String): 好友ID，必填

**返回值**:

- `ImFriendshipPo`: 好友关系对象，不存在时返回 null

**异常**: 无

**示例代码**:

```java
public void getFriendship(String userId, String friendId) {
    ImFriendshipPo friendship = imFriendshipDubboService.queryOne(userId, friendId);
    if (friendship != null) {
        System.out.println("好友备注: " + friendship.getRemark());
        System.out.println("是否在黑名单: " + (friendship.getBlack() == 2));
    } else {
        System.out.println("不是好友关系");
    }
}
```

---

#### 3.3 批量查询好友关系

```java
List<ImFriendshipPo> queryListByIds(String ownerId, List<String> ids)
```

**功能说明**: 批量查询用户与多个好友的关系

**参数**:

- `ownerId` (String): 用户ID，必填
- `ids` (List<String>): 好友ID列表，必填

**返回值**:

- `List<ImFriendshipPo>`: 好友关系列表

**异常**: 无

**示例代码**:

```java
public void batchGetFriendships(String userId, List<String> friendIds) {
    List<ImFriendshipPo> friendships = imFriendshipDubboService.queryListByIds(userId, friendIds);
    friendships.forEach(friendship -> {
        System.out.println("好友ID: " + friendship.getToId());
        System.out.println("备注: " + friendship.getRemark());
    });
}
```

---

#### 3.4 添加好友关系

```java
Boolean creat(ImFriendshipPo friendship)
```

**功能说明**: 创建好友关系

**参数**:

- `friendship` (ImFriendshipPo): 好友关系对象
    - `ownerId` (String): 用户ID，必填
    - `toId` (String): 好友ID，必填
    - 其他字段可选

**返回值**:

- `Boolean`: true-创建成功，false-创建失败

**异常**: 无

**示例代码**:

```java
public void addFriend(String userId, String friendId) {
    ImFriendshipPo friendship = new ImFriendshipPo();
    friendship.setOwnerId(userId);
    friendship.setToId(friendId);
    friendship.setRemark("大学同学");
    friendship.setBlack(1); // 正常状态
    friendship.setSequence(System.currentTimeMillis());
    friendship.setAddSource("search");

    Boolean result = imFriendshipDubboService.creat(friendship);
    if (result) {
        System.out.println("添加好友成功");

        // 双向添加好友
        ImFriendshipPo reverseFriendship = new ImFriendshipPo();
        reverseFriendship.setOwnerId(friendId);
        reverseFriendship.setToId(userId);
        reverseFriendship.setSequence(System.currentTimeMillis());
        imFriendshipDubboService.creat(reverseFriendship);
    }
}
```

---

#### 3.5 更新好友关系

```java
Boolean modify(ImFriendshipPo friendship)
```

**功能说明**: 更新好友关系信息（备注、黑名单状态等）

**参数**:

- `friendship` (ImFriendshipPo): 好友关系对象
    - `ownerId` (String): 用户ID，必填
    - `toId` (String): 好友ID，必填

**返回值**:

- `Boolean`: true-更新成功，false-更新失败

**异常**: 无

**示例代码**:

```java
public void updateFriendRemark(String userId, String friendId, String newRemark) {
    ImFriendshipPo friendship = new ImFriendshipPo();
    friendship.setOwnerId(userId);
    friendship.setToId(friendId);
    friendship.setRemark(newRemark);
    friendship.setBlackSequence(System.currentTimeMillis());

    Boolean result = imFriendshipDubboService.modify(friendship);
    System.out.println("更新结果: " + result);
}

public void addToBlacklist(String userId, String friendId) {
    ImFriendshipPo friendship = new ImFriendshipPo();
    friendship.setOwnerId(userId);
    friendship.setToId(friendId);
    friendship.setBlack(2); // 拉黑状态
    friendship.setBlackSequence(System.currentTimeMillis());

    Boolean result = imFriendshipDubboService.modify(friendship);
    System.out.println("拉黑结果: " + result);
}
```

---

#### 3.6 删除好友关系

```java
Boolean removeOne(String ownerId, String friendId)
```

**功能说明**: 删除好友关系

**参数**:

- `ownerId` (String): 用户ID，必填
- `friendId` (String): 好友ID，必填

**返回值**:

- `Boolean`: true-删除成功，false-删除失败

**异常**: 无

**示例代码**:

```java
public void deleteFriend(String userId, String friendId) {
    Boolean result = imFriendshipDubboService.removeOne(userId, friendId);
    if (result) {
        System.out.println("删除好友成功");

        // 双向删除
        imFriendshipDubboService.removeOne(friendId, userId);
    }
}
```

---

### 数据模型: ImFriendshipPo

```java
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "好友关系信息")
@TableName(value = "im_friendship")
public class ImFriendshipPo extends BasePo {

    /**
     * 用户ID
     */
    @TableId(value = "owner_id")
    private String ownerId;

    /**
     * 好友用户ID
     */
    @TableField(value = "to_id")
    private String toId;

    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;

    /**
     * 黑名单状态（1正常，2拉黑）
     */
    @TableField(value = "black")
    private Integer black;

    /**
     * 序列号
     */
    @TableField(value = "sequence")
    private Long sequence;

    /**
     * 黑名单序列号
     */
    @TableField(value = "black_sequence")
    private Long blackSequence;

    /**
     * 好友来源
     */
    @TableField(value = "add_source")
    private String addSource;

    /**
     * 扩展字段
     */
    @TableField(value = "extra")
    private String extra;
}
```

**字段说明**:

- `ownerId`: 用户ID
- `toId`: 好友用户ID
- `remark`: 好友备注
- `black`: 黑名单状态（1-正常，2-拉黑）
- `sequence`: 好友关系序列号（用于增量同步）
- `blackSequence`: 黑名单操作序列号
- `addSource`: 好友来源（如：search、qrcode、group等）
- `extra`: JSON格式的扩展字段

---

## 4. ImFriendshipRequestDubboService - 好友请求服务

### 接口描述

提供好友请求的增删改查操作，包括请求状态管理。

### 接口全限定名

```
com.xy.lucky.api.friend.ImFriendshipRequestDubboService
```

### 方法列表

#### 4.1 获取好友请求列表

```java
List<ImFriendshipRequestPo> queryList(String userId)
```

**功能说明**: 获取用户收到的好友请求列表

**参数**:

- `userId` (String): 用户ID，必填

**返回值**:

- `List<ImFriendshipRequestPo>`: 好友请求列表

**异常**: 无

**示例代码**:

```java

@Autowired
private ImFriendshipRequestDubboService imFriendshipRequestDubboService;

public void getFriendRequests(String userId) {
    List<ImFriendshipRequestPo> requests = imFriendshipRequestDubboService.queryList(userId);
    System.out.println("待处理的好友请求数量: " + requests.size());

    requests.forEach(request -> {
        System.out.println("请求ID: " + request.getId());
        System.out.println("申请人: " + request.getFromId());
        System.out.println("验证信息: " + request.getMessage());
        System.out.println("状态: " + request.getApproveStatus());
    });
}
```

---

#### 4.2 获取单个好友请求

```java
ImFriendshipRequestPo queryOne(ImFriendshipRequestPo request)
```

**功能说明**: 根据请求对象查询好友请求详情

**参数**:

- `request` (ImFriendshipRequestPo): 好友请求查询条件对象
    - 至少需要设置 `id` 字段

**返回值**:

- `ImFriendshipRequestPo`: 好友请求对象，不存在时返回 null

**异常**: 无

**示例代码**:

```java
public void getFriendRequest(String requestId) {
    ImFriendshipRequestPo query = new ImFriendshipRequestPo();
    query.setId(requestId);

    ImFriendshipRequestPo request = imFriendshipRequestDubboService.queryOne(query);
    if (request != null) {
        System.out.println("请求详情: " + request);
    }
}
```

---

#### 4.3 创建好友请求

```java
Boolean creat(ImFriendshipRequestPo request)
```

**功能说明**: 发送好友请求

**参数**:

- `request` (ImFriendshipRequestPo): 好友请求对象
    - `id` (String): 请求ID，必填
    - `fromId` (String): 申请人ID，必填
    - `toId` (String): 接收人ID，必填

**返回值**:

- `Boolean`: true-创建成功，false-创建失败

**异常**: 无

**示例代码**:

```java
public void sendFriendRequest(String fromUserId, String toUserId) {
    ImFriendshipRequestPo request = new ImFriendshipRequestPo();
    request.setId(UUID.randomUUID().toString());
    request.setFromId(fromUserId);
    request.setToId(toUserId);
    request.setRemark("想加你为好友");
    request.setReadStatus(0); // 未读
    request.setAddSource("search");
    request.setMessage("你好，我是...");
    request.setApproveStatus(0); // 未审批
    request.setSequence(System.currentTimeMillis());

    Boolean result = imFriendshipRequestDubboService.creat(request);
    if (result) {
        System.out.println("好友请求已发送");
    }
}
```

---

#### 4.4 更新好友请求

```java
Boolean modify(ImFriendshipRequestPo request)
```

**功能说明**: 更新好友请求信息

**参数**:

- `request` (ImFriendshipRequestPo): 好友请求对象
    - `id` (String): 请求ID，必填

**返回值**:

- `Boolean`: true-更新成功，false-更新失败

**异常**: 无

**示例代码**:

```java
public void markRequestAsRead(String requestId) {
    ImFriendshipRequestPo request = new ImFriendshipRequestPo();
    request.setId(requestId);
    request.setReadStatus(1); // 已读

    Boolean result = imFriendshipRequestDubboService.modify(request);
    System.out.println("标记已读结果: " + result);
}
```

---

#### 4.5 删除好友请求

```java
Boolean removeOne(String requestId)
```

**功能说明**: 删除好友请求

**参数**:

- `requestId` (String): 请求ID，必填

**返回值**:

- `Boolean`: true-删除成功，false-删除失败

**异常**: 无

**示例代码**:

```java
public void deleteFriendRequest(String requestId) {
    Boolean result = imFriendshipRequestDubboService.removeOne(requestId);
    System.out.println("删除结果: " + result);
}
```

---

#### 4.6 更新好友请求状态

```java
Boolean modifyStatus(String requestId, Integer status)
```

**功能说明**: 更新好友请求的审批状态

**参数**:

- `requestId` (String): 请求ID，必填
- `status` (Integer): 审批状态
    - 0: 未审批
    - 1: 同意
    - 2: 拒绝

**返回值**:

- `Boolean`: true-更新成功，false-更新失败

**异常**: 无

**示例代码**:

```java
public void approveFriendRequest(String requestId, boolean approve) {
    Integer status = approve ? 1 : 2;
    Boolean result = imFriendshipRequestDubboService.modifyStatus(requestId, status);

    if (result) {
        if (approve) {
            System.out.println("已同意好友请求");
            // 创建好友关系
            ImFriendshipRequestPo request = new ImFriendshipRequestPo();
            request.setId(requestId);
            ImFriendshipRequestPo requestDetail = imFriendshipRequestDubboService.queryOne(request);

            if (requestDetail != null) {
                // 创建双向好友关系
                ImFriendshipPo friendship1 = new ImFriendshipPo();
                friendship1.setOwnerId(requestDetail.getToId());
                friendship1.setToId(requestDetail.getFromId());
                imFriendshipDubboService.creat(friendship1);

                ImFriendshipPo friendship2 = new ImFriendshipPo();
                friendship2.setOwnerId(requestDetail.getFromId());
                friendship2.setToId(requestDetail.getToId());
                imFriendshipDubboService.creat(friendship2);
            }
        } else {
            System.out.println("已拒绝好友请求");
        }
    }
}
```

---

### 数据模型: ImFriendshipRequestPo

```java

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "好友请求信息")
@TableName(value = "im_friendship_request")
public class ImFriendshipRequestPo extends BasePo {

    /**
     * 请求ID
     */
    @TableId(value = "id")
    private String id;

    /**
     * 请求发起者
     */
    @TableField(value = "from_id")
    private String fromId;

    /**
     * 请求接收者
     */
    @TableField(value = "to_id")
    private String toId;

    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;

    /**
     * 是否已读（1已读）
     */
    @TableField(value = "read_status")
    private Integer readStatus;

    /**
     * 好友来源
     */
    @TableField(value = "add_source")
    private String addSource;

    /**
     * 好友验证信息
     */
    @TableField(value = "message")
    private String message;

    /**
     * 审批状态（0未审批，1同意，2拒绝）
     */
    @TableField(value = "approve_status")
    private Integer approveStatus;

    /**
     * 序列号
     */
    @TableField(value = "sequence")
    private Long sequence;
}
```

**字段说明**:

- `id`: 请求唯一标识
- `fromId`: 申请人用户ID
- `toId`: 接收人用户ID
- `remark`: 备注信息
- `readStatus`: 已读状态（0-未读，1-已读）
- `addSource`: 好友来源（如：search、qrcode、group等）
- `message`: 验证申请消息
- `approveStatus`: 审批状态（0-未审批，1-同意，2-拒绝）
- `sequence`: 序列号（用于增量同步）

---

## 5. ImGroupDubboService - 群组服务

### 接口描述

提供群组的增删改查操作。

### 接口全限定名

```
com.xy.lucky.api.group.ImGroupDubboService
```

### 方法列表

#### 5.1 获取用户的群列表

```java
List<ImGroupPo> queryList(String userId)
```

**功能说明**: 获取用户加入的所有群组列表

**参数**:

- `userId` (String): 用户ID，必填

**返回值**:

- `List<ImGroupPo>`: 群组信息列表

**异常**: 无

**示例代码**:

```java

@Autowired
private ImGroupDubboService imGroupDubboService;

public void getUserGroups(String userId) {
    List<ImGroupPo> groups = imGroupDubboService.queryList(userId);
    System.out.println("用户加入的群组数量: " + groups.size());

    groups.forEach(group -> {
        System.out.println("群ID: " + group.getGroupId());
        System.out.println("群名称: " + group.getGroupName());
        System.out.println("群成员数: " + group.getMemberCount());
    });
}
```

---

#### 5.2 获取单个群组信息

```java
ImGroupPo queryOne(String groupId)
```

**功能说明**: 根据群组ID获取群组详细信息

**参数**:

- `groupId` (String): 群组ID，必填

**返回值**:

- `ImGroupPo`: 群组信息对象，不存在时返回 null

**异常**: 无

**示例代码**:

```java
public void getGroupInfo(String groupId) {
    ImGroupPo group = imGroupDubboService.queryOne(groupId);
    if (group != null) {
        System.out.println("群名称: " + group.getGroupName());
        System.out.println("群主: " + group.getOwnerId());
        System.out.println("群类型: " + group.getGroupType());
        System.out.println("群简介: " + group.getIntroduction());
        System.out.println("群公告: " + group.getNotification());
    } else {
        System.out.println("群组不存在");
    }
}
```

---

#### 5.3 创建群组

```java
Boolean creat(ImGroupPo groupPo)
```

**功能说明**: 创建群组

**参数**:

- `groupPo` (ImGroupPo): 群组信息对象
    - `groupId` (String): 群组ID，必填
    - `ownerId` (String): 群主用户ID，必填
    - `groupName` (String): 群名称，必填

**返回值**:

- `Boolean`: true-创建成功，false-创建失败

**异常**: 无

**示例代码**:

```java
public void createGroup(String ownerId) {
    ImGroupPo group = new ImGroupPo();
    group.setGroupId("group_" + UUID.randomUUID().toString());
    group.setOwnerId(ownerId);
    group.setGroupType(1); // 私有群
    group.setGroupName("我的好友群");
    group.setMute(0); // 不禁言
    group.setApplyJoinType(1); // 需要审批
    group.setAvatar("https://example.com/group-avatar.jpg");
    group.setMaxMemberCount(500);
    group.setIntroduction("这是一个好友群");
    group.setNotification("欢迎加入群聊");
    group.setStatus(0); // 正常状态
    group.setSequence(System.currentTimeMillis());

    Boolean result = imGroupDubboService.creat(group);
    if (result) {
        System.out.println("群组创建成功，群ID: " + group.getGroupId());

        // 将群主添加到群成员列表
        ImGroupMemberPo ownerMember = new ImGroupMemberPo();
        ownerMember.setGroupMemberId(UUID.randomUUID().toString());
        ownerMember.setGroupId(group.getGroupId());
        ownerMember.setMemberId(ownerId);
        ownerMember.setRole(2); // 群主
        ownerMember.setJoinTime(System.currentTimeMillis());
        imGroupMemberDubboService.creat(ownerMember);
    }
}
```

---

#### 5.4 更新群组信息

```java
Boolean modify(ImGroupPo groupPo)
```

**功能说明**: 更新群组信息

**参数**:

- `groupPo` (ImGroupPo): 群组信息对象
    - `groupId` (String): 群组ID，必填

**返回值**:

- `Boolean`: true-更新成功，false-更新失败

**异常**: 无

**示例代码**:

```java
public void updateGroupInfo(String groupId) {
    ImGroupPo group = new ImGroupPo();
    group.setGroupId(groupId);
    group.setGroupName("新群名称");
    group.setIntroduction("新的群简介");
    group.setNotification("新的群公告");

    Boolean result = imGroupDubboService.modify(group);
    System.out.println("更新结果: " + result);
}
```

---

#### 5.5 批量创建群组

```java
Boolean creatBatch(List<ImGroupPo> list)
```

**功能说明**: 批量创建群组

**参数**:

- `list` (List<ImGroupPo>): 群组信息列表

**返回值**:

- `Boolean`: true-批量创建成功，false-批量创建失败

**异常**: 无

**示例代码**:

```java
public void batchCreateGroups() {
    List<ImGroupPo> groups = new ArrayList<>();

    for (int i = 0; i < 10; i++) {
        ImGroupPo group = new ImGroupPo();
        group.setGroupId("group_" + UUID.randomUUID().toString());
        group.setOwnerId("admin_user_id");
        group.setGroupName("群组 " + i);
        // 设置其他属性...
        groups.add(group);
    }

    Boolean result = imGroupDubboService.creatBatch(groups);
    System.out.println("批量创建结果: " + result);
}
```

---

#### 5.6 删除群组

```java
Boolean removeOne(String groupId)
```

**功能说明**: 删除群组（解散群）

**参数**:

- `groupId` (String): 群组ID，必填

**返回值**:

- `Boolean`: true-删除成功，false-删除失败

**异常**: 无

**示例代码**:

```java
public void dissolveGroup(String groupId) {
    // 先更新群状态为解散
    ImGroupPo group = new ImGroupPo();
    group.setGroupId(groupId);
    group.setStatus(1); // 解散状态
    imGroupDubboService.modify(group);

    // 删除所有群成员
    imGroupMemberDubboService.removeByGroupId(groupId);

    // 删除群组
    Boolean result = imGroupDubboService.removeOne(groupId);
    if (result) {
        System.out.println("群组已解散");
    }
}
```

---

### 数据模型: ImGroupPo

```java
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "群基础信息")
@TableName(value = "im_group")
public class ImGroupPo extends BasePo {

    /**
     * 群组ID
     */
    @TableId(value = "group_id")
    private String groupId;

    /**
     * 群主用户ID
     */
    @TableField(value = "owner_id")
    private String ownerId;

    /**
     * 群类型（1私有群，2公开群）
     */
    @TableField(value = "group_type")
    private Integer groupType;

    /**
     * 群名称
     */
    @TableField(value = "group_name")
    private String groupName;

    /**
     * 是否全员禁言（0不禁言，1禁言）
     */
    @TableField(value = "mute")
    private Integer mute;

    /**
     * 申请加群方式（0禁止申请，1需要审批，2允许自由加入）
     */
    @TableField(value = "apply_join_type")
    private Integer applyJoinType;

    /**
     * 群头像
     */
    @TableField(value = "avatar")
    private String avatar;

    /**
     * 最大成员数
     */
    @TableField(value = "max_member_count")
    private Integer maxMemberCount;

    /**
     * 群简介
     */
    @TableField(value = "introduction")
    private String introduction;

    /**
     * 群公告
     */
    @TableField(value = "notification")
    private String notification;

    /**
     * 群状态（0正常，1解散）
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 消息序列号
     */
    @TableField(value = "sequence")
    private Long sequence;

    /**
     * 成员数
     */
    @TableField(exist = false)
    private Integer memberCount;

    /**
     * 扩展字段
     */
    @TableField(value = "extra")
    private String extra;
}
```

**字段说明**:

- `groupId`: 群组唯一标识
- `ownerId`: 群主用户ID
- `groupType`: 群类型（1-私有群，2-公开群）
- `groupName`: 群名称
- `mute`: 全员禁言标识（0-不禁言，1-禁言）
- `applyJoinType`: 申请加群方式（0-禁止申请，1-需要审批，2-允许自由加入）
- `avatar`: 群头像URL
- `maxMemberCount`: 最大成员数限制
- `introduction`: 群简介
- `notification`: 群公告
- `status`: 群状态（0-正常，1-解散）
- `sequence`: 消息序列号
- `memberCount`: 成员数量（非数据库字段，查询时动态填充）
- `extra`: JSON格式的扩展字段

---

## 6. ImGroupMemberDubboService - 群成员服务

### 接口描述

提供群成员的增删改查操作，包括成员权限管理。

### 接口全限定名

```
com.xy.lucky.api.group.ImGroupMemberDubboService
```

### 方法列表

#### 6.1 获取群成员列表

```java
List<ImGroupMemberPo> queryList(String groupId)
```

**功能说明**: 获取指定群组的所有成员列表

**参数**:

- `groupId` (String): 群组ID，必填

**返回值**:

- `List<ImGroupMemberPo>`: 群成员列表

**异常**: 无

**示例代码**:

```java

@Autowired
private ImGroupMemberDubboService imGroupMemberDubboService;

public void getGroupMembers(String groupId) {
    List<ImGroupMemberPo> members = imGroupMemberDubboService.queryList(groupId);
    System.out.println("群成员数量: " + members.size());

    members.forEach(member -> {
        String role = member.getRole() == 2 ? "群主" :
                member.getRole() == 1 ? "管理员" : "普通成员";
        System.out.println("成员ID: " + member.getMemberId() + ", 角色: " + role);
    });
}
```

---

#### 6.2 获取单个群成员信息

```java
ImGroupMemberPo queryOne(String groupId, String memberId)
```

**功能说明**: 获取指定群组的某个成员信息

**参数**:

- `groupId` (String): 群组ID，必填
- `memberId` (String): 成员ID，必填

**返回值**:

- `ImGroupMemberPo`: 群成员信息对象，不存在时返回 null

**异常**: 无

**示例代码**:

```java
public void getGroupMember(String groupId, String userId) {
    ImGroupMemberPo member = imGroupMemberDubboService.queryOne(groupId, userId);
    if (member != null) {
        System.out.println("群昵称: " + member.getAlias());
        System.out.println("角色: " + member.getRole());
        System.out.println("是否禁言: " + (member.getMute() == 1));
        System.out.println("加入时间: " + member.getJoinTime());
    } else {
        System.out.println("该用户不在群中");
    }
}
```

---

#### 6.3 按角色查询群成员

```java
List<ImGroupMemberPo> queryByRole(String groupId, Integer role)
```

**功能说明**: 查询群组中指定角色的所有成员

**参数**:

- `groupId` (String): 群组ID，必填
- `role` (Integer): 角色类型
    - 0: 普通成员
    - 1: 管理员
    - 2: 群主

**返回值**:

- `List<ImGroupMemberPo>`: 群成员列表

**异常**: 无

**示例代码**:

```java
public void getGroupAdmins(String groupId) {
    // 获取所有管理员和群主
    List<ImGroupMemberPo> admins = imGroupMemberDubboService.queryByRole(groupId, 1);
    List<ImGroupMemberPo> owners = imGroupMemberDubboService.queryByRole(groupId, 2);

    System.out.println("管理员数量: " + admins.size());
    System.out.println("群主数量: " + owners.size());
}
```

---

#### 6.4 移除群成员

```java
Boolean removeOne(String memberId)
```

**功能说明**: 移除指定的群成员（成员ID即群成员表的group_member_id）

**参数**:

- `memberId` (String): 群成员ID，必填

**返回值**:

- `Boolean`: true-移除成功，false-移除失败

**异常**: 无

**示例代码**:

```java
public void removeGroupMember(String groupMemberId) {
    Boolean result = imGroupMemberDubboService.removeOne(groupMemberId);
    if (result) {
        System.out.println("成员已移出群组");
    }
}
```

---

#### 6.5 移除群所有成员

```java
Boolean removeByGroupId(String groupId)
```

**功能说明**: 移除指定群组的所有成员（用于解散群时）

**参数**:

- `groupId` (String): 群组ID，必填

**返回值**:

- `Boolean`: true-移除成功，false-移除失败

**异常**: 无

**示例代码**:

```java
public void removeAllMembers(String groupId) {
    Boolean result = imGroupMemberDubboService.removeByGroupId(groupId);
    if (result) {
        System.out.println("所有成员已移出群组");
    }
}
```

---

#### 6.6 添加群成员

```java
Boolean creat(ImGroupMemberPo groupMember)
```

**功能说明**: 添加成员到群组

**参数**:

- `groupMember` (ImGroupMemberPo): 群成员信息对象
    - `groupMemberId` (String): 群成员ID，必填
    - `groupId` (String): 群组ID，必填
    - `memberId` (String): 成员用户ID，必填

**返回值**:

- `Boolean`: true-添加成功，false-添加失败

**异常**: 无

**示例代码**:

```java
public void addMemberToGroup(String groupId, String userId, Integer role) {
    ImGroupMemberPo member = new ImGroupMemberPo();
    member.setGroupMemberId(UUID.randomUUID().toString());
    member.setGroupId(groupId);
    member.setMemberId(userId);
    member.setRole(role); // 0-普通成员, 1-管理员, 2-群主
    member.setSpeakDate(System.currentTimeMillis());
    member.setMute(0); // 不禁言
    member.setJoinTime(System.currentTimeMillis());
    member.setJoinType("invite");

    Boolean result = imGroupMemberDubboService.creat(member);
    if (result) {
        System.out.println("成员已添加到群组");
    }
}
```

---

#### 6.7 修改群成员信息

```java
Boolean modify(ImGroupMemberPo groupMember)
```

**功能说明**: 修改群成员信息（角色、昵称、禁言状态等）

**参数**:

- `groupMember` (ImGroupMemberPo): 群成员信息对象
    - `groupMemberId` (String): 群成员ID，必填

**返回值**:

- `Boolean`: true-修改成功，false-修改失败

**异常**: 无

**示例代码**:

```java
public void updateMemberRole(String groupMemberId, Integer newRole) {
    ImGroupMemberPo member = new ImGroupMemberPo();
    member.setGroupMemberId(groupMemberId);
    member.setRole(newRole);

    Boolean result = imGroupMemberDubboService.modify(member);
    System.out.println("角色更新结果: " + result);
}

public void muteMember(String groupMemberId, long duration) {
    ImGroupMemberPo member = new ImGroupMemberPo();
    member.setGroupMemberId(groupMemberId);
    member.setMute(1); // 禁言
    member.setMuteStartTime(System.currentTimeMillis());
    member.setMuteEndTime(System.currentTimeMillis() + duration);

    Boolean result = imGroupMemberDubboService.modify(member);
    System.out.println("禁言结果: " + result);
}
```

---

#### 6.8 批量修改群成员信息

```java
Boolean modifyBatch(List<ImGroupMemberPo> groupMemberList)
```

**功能说明**: 批量修改群成员信息

**参数**:

- `groupMemberList` (List<ImGroupMemberPo>): 群成员信息列表

**返回值**:

- `Boolean`: true-批量修改成功，false-批量修改失败

**异常**: 无

**示例代码**:

```java
public void batchMuteMembers(List<String> memberIds) {
    List<ImGroupMemberPo> members = new ArrayList<>();

    memberIds.forEach(memberId -> {
        ImGroupMemberPo member = new ImGroupMemberPo();
        member.setGroupMemberId(memberId);
        member.setMute(1);
        member.setMuteStartTime(System.currentTimeMillis());
        member.setMuteEndTime(System.currentTimeMillis() + 3600000); // 禁言1小时
        members.add(member);
    });

    Boolean result = imGroupMemberDubboService.modifyBatch(members);
    System.out.println("批量禁言结果: " + result);
}
```

---

#### 6.9 批量添加群成员

```java
Boolean creatBatch(List<ImGroupMemberPo> groupMemberList)
```

**功能说明**: 批量添加群成员

**参数**:

- `groupMemberList` (List<ImGroupMemberPo>): 群成员信息列表

**返回值**:

- `Boolean`: true-批量添加成功，false-批量添加失败

**异常**: 无

**示例代码**:

```java
public void batchAddMembers(String groupId, List<String> userIds) {
    List<ImGroupMemberPo> members = new ArrayList<>();

    userIds.forEach(userId -> {
        ImGroupMemberPo member = new ImGroupMemberPo();
        member.setGroupMemberId(UUID.randomUUID().toString());
        member.setGroupId(groupId);
        member.setMemberId(userId);
        member.setRole(0); // 普通成员
        member.setJoinTime(System.currentTimeMillis());
        member.setJoinType("invite");
        members.add(member);
    });

    Boolean result = imGroupMemberDubboService.creatBatch(members);
    if (result) {
        System.out.println("批量添加成员成功，数量: " + userIds.size());
    }
}
```

---

#### 6.10 获取群九宫格头像

```java
List<String> queryNinePeopleAvatar(String groupId)
```

**功能说明**: 随机获取9个群成员头像，用于生成群聊九宫格头像

**参数**:

- `groupId` (String): 群组ID，必填

**返回值**:

- `List<String>`: 用户头像URL列表（最多9个）

**异常**: 无

**示例代码**:

```java
public void getGroupAvatar(String groupId) {
    List<String> avatars = imGroupMemberDubboService.queryNinePeopleAvatar(groupId);
    System.out.println("获取到 " + avatars.size() + " 个头像");
    // 可用于合成九宫格群头像
}
```

---

#### 6.11 统计群成员数量

```java
Long countByGroupId(String groupId)
```

**功能说明**: 统计指定群组的成员数量

**参数**:

- `groupId` (String): 群组ID，必填

**返回值**:

- `Long`: 群成员数量

**异常**: 无

**示例代码**:

```java
public void getGroupMemberCount(String groupId) {
    Long count = imGroupMemberDubboService.countByGroupId(groupId);
    System.out.println("群成员数量: " + count);
}
```

---

### 数据模型: ImGroupMemberPo

```java
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "群成员信息")
@TableName(value = "im_group_member")
public class ImGroupMemberPo extends BasePo {

    /**
     * 群成员ID
     */
    @TableId(value = "group_member_id")
    private String groupMemberId;

    /**
     * 群组ID
     */
    @TableField(value = "group_id")
    private String groupId;

    /**
     * 成员用户ID
     */
    @TableField(value = "member_id")
    private String memberId;

    /**
     * 群成员角色（0普通成员，1管理员，2群主）
     */
    @TableField(value = "role")
    private Integer role;

    /**
     * 最后发言时间
     */
    @TableField(value = "speak_date")
    private Long speakDate;

    /**
     * 是否禁言（0不禁言，1禁言）
     */
    @TableField(value = "mute")
    private Integer mute;

    /**
     * 禁言开始时间（毫秒时间戳）
     */
    @TableField(value = "mute_start_time")
    private Long muteStartTime;

    /**
     * 禁言结束时间（毫秒时间戳，null 表示永久禁言）
     */
    @TableField(value = "mute_end_time")
    private Long muteEndTime;

    /**
     * 群昵称
     */
    @TableField(value = "alias")
    private String alias;

    /**
     * 加入时间
     */
    @TableField(value = "join_time")
    private Long joinTime;

    /**
     * 离开时间
     */
    @TableField(value = "leave_time")
    private Long leaveTime;

    /**
     * 加入类型
     */
    @TableField(value = "join_type")
    private String joinType;

    /**
     * 群备注
     */
    @TableField(value = "remark")
    private String remark;

    /**
     * 扩展字段
     */
    @TableField(value = "extra")
    private String extra;
}
```

**字段说明**:

- `groupMemberId`: 群成员记录唯一标识
- `groupId`: 群组ID
- `memberId`: 成员用户ID
- `role`: 群成员角色（0-普通成员，1-管理员，2-群主）
- `speakDate`: 最后发言时间（毫秒时间戳）
- `mute`: 是否禁言（0-不禁言，1-禁言）
- `muteStartTime`: 禁言开始时间（毫秒时间戳）
- `muteEndTime`: 禁言结束时间（毫秒时间戳，null表示永久禁言）
- `alias`: 群昵称（在群中显示的昵称）
- `joinTime`: 加入时间（毫秒时间戳）
- `leaveTime`: 离开时间（毫秒时间戳）
- `joinType`: 加入类型（如：invite、apply、qrcode等）
- `remark`: 群备注
- `extra`: JSON格式的扩展字段

---

## 7. ImGroupInviteRequestDubboService - 群邀请请求服务

### 接口描述

提供群组邀请请求的增删改查操作。

### 接口全限定名

```
com.xy.lucky.api.group.ImGroupInviteRequestDubboService
```

### 方法列表

#### 7.1 获取群邀请请求列表

```java
List<ImGroupInviteRequestPo> queryList(String userId)
```

**功能说明**: 获取用户的群邀请请求列表

**参数**:

- `userId` (String): 用户ID，必填

**返回值**:

- `List<ImGroupInviteRequestPo>`: 群邀请请求列表

**异常**: 无

**示例代码**:

```java

@Autowired
private ImGroupInviteRequestDubboService imGroupInviteRequestDubboService;

public void getGroupInviteRequests(String userId) {
    List<ImGroupInviteRequestPo> requests = imGroupInviteRequestDubboService.queryList(userId);
    System.out.println("群邀请请求数量: " + requests.size());

    requests.forEach(request -> {
        System.out.println("邀请ID: " + request.getRequestId());
        System.out.println("群ID: " + request.getGroupId());
        System.out.println("邀请人: " + request.getFromId());
        System.out.println("验证状态: " + request.getVerifierStatus());
        System.out.println("审批状态: " + request.getApproveStatus());
    });
}
```

---

#### 7.2 获取单个群邀请请求

```java
ImGroupInviteRequestPo queryOne(ImGroupInviteRequestPo imGroupInviteRequestPo)
```

**功能说明**: 根据请求对象查询群邀请请求详情

**参数**:

- `imGroupInviteRequestPo` (ImGroupInviteRequestPo): 群邀请请求查询条件对象
    - 至少需要设置 `requestId` 字段

**返回值**:

- `ImGroupInviteRequestPo`: 群邀请请求对象，不存在时返回 null

**异常**: 无

**示例代码**:

```java
public void getGroupInviteRequest(String requestId) {
    ImGroupInviteRequestPo query = new ImGroupInviteRequestPo();
    query.setRequestId(requestId);

    ImGroupInviteRequestPo request = imGroupInviteRequestDubboService.queryOne(query);
    if (request != null) {
        System.out.println("邀请详情: " + request);
    }
}
```

---

#### 7.3 创建群邀请请求

```java
Boolean creat(ImGroupInviteRequestPo imGroupInviteRequestPo)
```

**功能说明**: 创建群邀请请求

**参数**:

- `imGroupInviteRequestPo` (ImGroupInviteRequestPo): 群邀请请求对象
    - `requestId` (String): 请求ID，必填
    - `groupId` (String): 群组ID，必填
    - `fromId` (String): 邀请发起者ID，必填
    - `toId` (String): 被邀请者ID，必填

**返回值**:

- `Boolean`: true-创建成功，false-创建失败

**异常**: 无

**示例代码**:

```java
public void inviteUserToGroup(String groupId, String fromUserId, String toUserId) {
    ImGroupInviteRequestPo request = new ImGroupInviteRequestPo();
    request.setRequestId(UUID.randomUUID().toString());
    request.setGroupId(groupId);
    request.setFromId(fromUserId);
    request.setToId(toUserId);
    request.setMessage("邀请你加入群聊");
    request.setVerifierStatus(0); // 待验证
    request.setApproveStatus(0); // 待审批
    request.setAddSource(1); // 成员邀请
    request.setExpireTime(System.currentTimeMillis() + 86400000); // 24小时过期

    Boolean result = imGroupInviteRequestDubboService.creat(request);
    if (result) {
        System.out.println("群邀请已发送");
    }
}
```

---

#### 7.4 修改群邀请请求

```java
Boolean modify(ImGroupInviteRequestPo imGroupInviteRequestPo)
```

**功能说明**: 修改群邀请请求信息

**参数**:

- `imGroupInviteRequestPo` (ImGroupInviteRequestPo): 群邀请请求对象
    - `requestId` (String): 请求ID，必填

**返回值**:

- `Boolean`: true-修改成功，false-修改失败

**异常**: 无

**示例代码**:

```java
public void updateInviteRequest(String requestId) {
    ImGroupInviteRequestPo request = new ImGroupInviteRequestPo();
    request.setRequestId(requestId);
    request.setVerifierId("verifier_user_id");
    request.setVerifierStatus(1); // 管理员已同意

    Boolean result = imGroupInviteRequestDubboService.modify(request);
    System.out.println("更新结果: " + result);
}
```

---

#### 7.5 删除群邀请请求

```java
Boolean removeOne(String requestId)
```

**功能说明**: 删除群邀请请求

**参数**:

- `requestId` (String): 请求ID，必填

**返回值**:

- `Boolean`: true-删除成功，false-删除失败

**异常**: 无

**示例代码**:

```java
public void deleteInviteRequest(String requestId) {
    Boolean result = imGroupInviteRequestDubboService.removeOne(requestId);
    System.out.println("删除结果: " + result);
}
```

---

#### 7.6 批量创建群邀请请求

```java
Boolean creatBatch(List<ImGroupInviteRequestPo> requests)
```

**功能说明**: 批量创建群邀请请求

**参数**:

- `requests` (List<ImGroupInviteRequestPo>): 群邀请请求列表

**返回值**:

- `Boolean`: true-批量创建成功，false-批量创建失败

**异常**: 无

**示例代码**:

```java
public void batchInviteUsers(String groupId, String fromUserId, List<String> toUserIds) {
    List<ImGroupInviteRequestPo> requests = new ArrayList<>();

    toUserIds.forEach(toUserId -> {
        ImGroupInviteRequestPo request = new ImGroupInviteRequestPo();
        request.setRequestId(UUID.randomUUID().toString());
        request.setGroupId(groupId);
        request.setFromId(fromUserId);
        request.setToId(toUserId);
        request.setVerifierStatus(0);
        request.setApproveStatus(0);
        request.setExpireTime(System.currentTimeMillis() + 86400000);
        requests.add(request);
    });

    Boolean result = imGroupInviteRequestDubboService.creatBatch(requests);
    System.out.println("批量邀请结果: " + result);
}
```

---

### 数据模型: ImGroupInviteRequestPo

```java

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "群邀请请求信息")
@TableName(value = "im_group_invite_request")
public class ImGroupInviteRequestPo extends BasePo {

    /**
     * 邀请请求ID
     */
    @TableId(value = "request_id")
    private String requestId;

    /**
     * 群组ID
     */
    @TableField(value = "group_id")
    private String groupId;

    /**
     * 邀请发起者用户ID
     */
    @TableField(value = "from_id")
    private String fromId;

    /**
     * 被邀请者用户ID
     */
    @TableField(value = "to_id")
    private String toId;

    /**
     * 验证者用户ID（群主或管理员）
     */
    @TableField(value = "verifier_id")
    private String verifierId;

    /**
     * 群主或管理员验证 （0:待处理, 1:同意, 2:拒绝）
     */
    @TableField(value = "verifier_status")
    private Integer verifierStatus;

    /**
     * 邀请验证信息
     */
    @TableField(value = "message")
    private String message;

    /**
     * 被邀请人状态（0:待处理, 1:同意, 2:拒绝）
     */
    @TableField(value = "approve_status")
    private Integer approveStatus;

    /**
     * 邀请来源（如二维码、成员邀请等）
     */
    @TableField(value = "add_source")
    private Integer addSource;

    /**
     * 邀请过期时间（Unix时间戳）
     */
    @TableField(value = "expire_time")
    private Long expireTime;
}
```

**字段说明**:

- `requestId`: 邀请请求唯一标识
- `groupId`: 群组ID
- `fromId`: 邀请发起者用户ID
- `toId`: 被邀请者用户ID
- `verifierId`: 验证者用户ID（群主或管理员）
- `verifierStatus`: 群主或管理员验证状态（0-待处理，1-同意，2-拒绝）
- `message`: 邀请验证信息
- `approveStatus`: 被邀请人状态（0-待处理，1-同意，2-拒绝）
- `addSource`: 邀请来源（1-成员邀请，2-二维码，3-链接等）
- `expireTime`: 邀请过期时间（Unix时间戳，毫秒）

---

## 8. ImSingleMessageDubboService - 单聊消息服务

### 接口描述

提供单聊消息的增删改查操作，包括消息已读状态管理。

### 接口全限定名

```
com.xy.lucky.api.message.ImSingleMessageDubboService
```

### 方法列表

#### 8.1 查询单聊消息列表

```java
List<ImSingleMessagePo> queryList(String userId, Long sequence)
```

**功能说明**: 根据用户ID和序列号查询单聊消息列表（用于增量同步）

**参数**:

- `userId` (String): 用户ID，必填
- `sequence` (Long): 消息序列号，可选（null表示查询全部）

**返回值**:

- `List<ImSingleMessagePo>`: 单聊消息列表

**异常**: 无

**示例代码**:

```java

@Autowired
private ImSingleMessageDubboService imSingleMessageDubboService;

public void getSingleMessages(String userId) {
    // 查询所有消息
    List<ImSingleMessagePo> messages = imSingleMessageDubboService.queryList(userId, null);
    System.out.println("消息数量: " + messages.size());

    // 增量查询（从某个序列号之后的消息）
    List<ImSingleMessagePo> newMessages = imSingleMessageDubboService.queryList(userId, 1234567890L);
    System.out.println("新消息数量: " + newMessages.size());
}
```

---

#### 8.2 查询单条单聊消息

```java
ImSingleMessagePo queryOne(String messageId)
```

**功能说明**: 根据消息ID查询单聊消息详情

**参数**:

- `messageId` (String): 消息ID，必填

**返回值**:

- `ImSingleMessagePo`: 单聊消息对象，不存在时返回 null

**异常**: 无

**示例代码**:

```java
public void getSingleMessage(String messageId) {
    ImSingleMessagePo message = imSingleMessageDubboService.queryOne(messageId);
    if (message != null) {
        System.out.println("发送者: " + message.getFromId());
        System.out.println("接收者: " + message.getToId());
        System.out.println("消息内容: " + message.getMessageBody());
        System.out.println("消息类型: " + message.getMessageContentType());
        System.out.println("已读状态: " + message.getReadStatus());
    }
}
```

---

#### 8.3 发送单聊消息

```java
Boolean creat(ImSingleMessagePo singleMessagePo)
```

**功能说明**: 发送单聊消息

**参数**:

- `singleMessagePo` (ImSingleMessagePo): 单聊消息对象
    - `messageId` (String): 消息ID，必填
    - `fromId` (String): 发送者ID，必填
    - `toId` (String): 接收者ID，必填
    - `messageBody` (Object): 消息内容，必填
    - `messageContentType` (Integer): 消息类型，必填

**返回值**:

- `Boolean`: true-发送成功，false-发送失败

**异常**: 无

**示例代码**:

```java
public void sendSingleMessage(String fromUserId, String toUserId) {
    // 构建消息内容
    Map<String, Object> content = new HashMap<>();
    content.put("text", "你好，这是一条消息");

    ImSingleMessagePo message = new ImSingleMessagePo();
    message.setMessageId(UUID.randomUUID().toString());
    message.setFromId(fromUserId);
    message.setToId(toUserId);
    message.setMessageBody(content);
    message.setMessageTime(System.currentTimeMillis());
    message.setMessageContentType(1); // 文本消息
    message.setReadStatus(0); // 未读
    message.setSequence(System.currentTimeMillis());
    message.setMessageRandom(UUID.randomUUID().toString());

    Boolean result = imSingleMessageDubboService.creat(message);
    if (result) {
        System.out.println("消息发送成功，消息ID: " + message.getMessageId());

        // 更新会话
        ImChatPo chat = new ImChatPo();
        chat.setChatId(UUID.randomUUID().toString());
        chat.setOwnerId(fromUserId);
        chat.setToId(toUserId);
        chat.setChatType(0); // 单聊
        chat.setSequence(System.currentTimeMillis());
        imChatDubboService.creatOrModify(chat);
    }
}
```

---

#### 8.4 批量发送单聊消息

```java
Boolean creatBatch(List<ImSingleMessagePo> singleMessagePoList)
```

**功能说明**: 批量发送单聊消息

**参数**:

- `singleMessagePoList` (List<ImSingleMessagePo>): 单聊消息列表

**返回值**:

- `Boolean`: true-批量发送成功，false-批量发送失败

**异常**: 无

**示例代码**:

```java
public void batchSendMessages(String fromUserId, List<String> toUserIds) {
    List<ImSingleMessagePo> messages = new ArrayList<>();

    toUserIds.forEach(toUserId -> {
        Map<String, Object> content = new HashMap<>();
        content.put("text", "群发消息");

        ImSingleMessagePo message = new ImSingleMessagePo();
        message.setMessageId(UUID.randomUUID().toString());
        message.setFromId(fromUserId);
        message.setToId(toUserId);
        message.setMessageBody(content);
        message.setMessageTime(System.currentTimeMillis());
        message.setMessageContentType(1);
        message.setReadStatus(0);
        message.setSequence(System.currentTimeMillis());
        messages.add(message);
    });

    Boolean result = imSingleMessageDubboService.creatBatch(messages);
    System.out.println("批量发送结果: " + result);
}
```

---

#### 8.5 更新单聊消息

```java
Boolean modify(ImSingleMessagePo singleMessagePo)
```

**功能说明**: 更新单聊消息（如已读状态）

**参数**:

- `singleMessagePo` (ImSingleMessagePo): 单聊消息对象
    - `messageId` (String): 消息ID，必填

**返回值**:

- `Boolean`: true-更新成功，false-更新失败

**异常**: 无

**示例代码**:

```java
public void markMessageAsRead(String messageId) {
    ImSingleMessagePo message = new ImSingleMessagePo();
    message.setMessageId(messageId);
    message.setReadStatus(1); // 已读

    Boolean result = imSingleMessageDubboService.modify(message);
    System.out.println("标记已读结果: " + result);
}
```

---

#### 8.6 删除单聊消息

```java
Boolean removeOne(String messageId)
```

**功能说明**: 删除单聊消息

**参数**:

- `messageId` (String): 消息ID，必填

**返回值**:

- `Boolean`: true-删除成功，false-删除失败

**异常**: 无

**示例代码**:

```java
public void deleteSingleMessage(String messageId) {
    Boolean result = imSingleMessageDubboService.removeOne(messageId);
    System.out.println("删除结果: " + result);
}
```

---

#### 8.7 查询单聊最后一条消息

```java
ImSingleMessagePo queryLast(String fromId, String toId)
```

**功能说明**: 查询两个用户之间的最后一条消息

**参数**:

- `fromId` (String): 发送方ID，必填
- `toId` (String): 接收方ID，必填

**返回值**:

- `ImSingleMessagePo`: 单聊消息对象，不存在时返回 null

**异常**: 无

**示例代码**:

```java
public void getLastMessage(String userId1, String userId2) {
    ImSingleMessagePo message = imSingleMessageDubboService.queryLast(userId1, userId2);
    if (message != null) {
        System.out.println("最后一条消息: " + message.getMessageBody());
        System.out.println("发送时间: " + message.getMessageTime());
    }
}
```

---

#### 8.8 查询单聊消息已读状态

```java
Integer queryReadStatus(String fromId, String toId, Integer code)
```

**功能说明**: 查询单聊消息的已读状态

**参数**:

- `fromId` (String): 发送方ID，必填
- `toId` (String): 接收方ID，必填
- `code` (Integer): 状态码（具体含义根据业务定义）

**返回值**:

- `Integer`: 已读状态

**异常**: 无

**示例代码**:

```java
public void checkReadStatus(String fromId, String toId) {
    Integer status = imSingleMessageDubboService.queryReadStatus(fromId, toId, 1);
    System.out.println("已读状态: " + status);
}
```

---

### 数据模型: ImSingleMessagePo

```java
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "单聊消息")
@TableName(value = "im_single_message")
public class ImSingleMessagePo extends BasePo implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /**
     * 消息ID
     */
    @TableId(value = "message_id")
    private String messageId;

    /**
     * 发送者用户ID
     */
    @TableField(value = "from_id")
    private String fromId;

    /**
     * 接收者用户ID
     */
    @TableField(value = "to_id")
    private String toId;

    /**
     * 消息内容
     */
    @TableField(value = "message_body", typeHandler = JacksonTypeHandler.class)
    private Object messageBody;

    /**
     * 发送时间
     */
    @TableField(value = "message_time")
    private Long messageTime;

    /**
     * 消息类型
     */
    @TableField(value = "message_content_type")
    private Integer messageContentType;

    /**
     * 阅读状态（1已读）
     */
    @TableField(value = "read_status")
    private Integer readStatus;

    /**
     * 扩展字段
     */
    @TableField(value = "extra", typeHandler = JacksonTypeHandler.class)
    private Object extra;

    /**
     * 消息序列
     */
    @TableField(value = "sequence")
    private Long sequence;

    /**
     * 随机标识
     */
    @TableField(value = "message_random")
    private String messageRandom;
}
```

**字段说明**:

- `messageId`: 消息唯一标识
- `fromId`: 发送者用户ID
- `toId`: 接收者用户ID
- `messageBody`: 消息内容（JSON格式，存储文本、图片、文件等）
- `messageTime`: 消息发送时间（毫秒时间戳）
- `messageContentType`: 消息类型（1-文本，2-图片，3-语音，4-视频，5-文件等）
- `readStatus`: 阅读状态（0-未读，1-已读）
- `extra`: 扩展字段（JSON格式）
- `sequence`: 消息序列号（用于增量同步）
- `messageRandom`: 消息随机标识（用于消息去重）

---

## 9. ImGroupMessageDubboService - 群聊消息服务

### 接口描述

提供群聊消息的增删改查操作，包括消息已读状态管理。

### 接口全限定名

```
com.xy.lucky.api.message.ImGroupMessageDubboService
```

### 方法列表

#### 9.1 查询群聊消息列表

```java
List<ImGroupMessagePo> queryList(String groupId, Long sequence)
```

**功能说明**: 根据群组ID和序列号查询群聊消息列表（用于增量同步）

**参数**:

- `groupId` (String): 群组ID，必填
- `sequence` (Long): 消息序列号，可选（null表示查询全部）

**返回值**:

- `List<ImGroupMessagePo>`: 群聊消息列表

**异常**: 无

**示例代码**:

```java

@Autowired
private ImGroupMessageDubboService imGroupMessageDubboService;

public void getGroupMessages(String groupId) {
    // 查询所有消息
    List<ImGroupMessagePo> messages = imGroupMessageDubboService.queryList(groupId, null);
    System.out.println("群消息数量: " + messages.size());

    // 增量查询（从某个序列号之后的消息）
    List<ImGroupMessagePo> newMessages = imGroupMessageDubboService.queryList(groupId, 1234567890L);
    System.out.println("新消息数量: " + newMessages.size());
}
```

---

#### 9.2 查询单条群聊消息

```java
ImGroupMessagePo queryOne(String messageId)
```

**功能说明**: 根据消息ID查询群聊消息详情

**参数**:

- `messageId` (String): 消息ID，必填

**返回值**:

- `ImGroupMessagePo`: 群聊消息对象，不存在时返回 null

**异常**: 无

**示例代码**:

```java
public void getGroupMessage(String messageId) {
    ImGroupMessagePo message = imGroupMessageDubboService.queryOne(messageId);
    if (message != null) {
        System.out.println("群组ID: " + message.getGroupId());
        System.out.println("发送者: " + message.getFromId());
        System.out.println("消息内容: " + message.getMessageBody());
        System.out.println("消息类型: " + message.getMessageContentType());
    }
}
```

---

#### 9.3 发送群聊消息

```java
boolean creat(ImGroupMessagePo groupMessagePo)
```

**功能说明**: 发送群聊消息

**参数**:

- `groupMessagePo` (ImGroupMessagePo): 群聊消息对象
    - `messageId` (String): 消息ID，必填
    - `groupId` (String): 群组ID，必填
    - `fromId` (String): 发送者ID，必填
    - `messageBody` (Object): 消息内容，必填
    - `messageContentType` (Integer): 消息类型，必填

**返回值**:

- `boolean`: true-发送成功，false-发送失败

**异常**: 无

**示例代码**:

```java
public void sendGroupMessage(String groupId, String fromUserId) {
    // 构建消息内容
    Map<String, Object> content = new HashMap<>();
    content.put("text", "大家好，这是一条群消息");

    ImGroupMessagePo message = new ImGroupMessagePo();
    message.setMessageId(UUID.randomUUID().toString());
    message.setGroupId(groupId);
    message.setFromId(fromUserId);
    message.setMessageBody(content);
    message.setMessageTime(System.currentTimeMillis());
    message.setMessageContentType(1); // 文本消息
    message.setSequence(System.currentTimeMillis());
    message.setMessageRandom(UUID.randomUUID().toString());

    boolean result = imGroupMessageDubboService.creat(message);
    if (result) {
        System.out.println("群消息发送成功");

        // 为所有群成员创建消息状态记录
        List<ImGroupMemberPo> members = imGroupMemberDubboService.queryList(groupId);
        List<ImGroupMessageStatusPo> statusList = new ArrayList<>();

        members.forEach(member -> {
            if (!member.getMemberId().equals(fromUserId)) { // 不给发送者创建状态
                ImGroupMessageStatusPo status = new ImGroupMessageStatusPo();
                status.setGroupId(groupId);
                status.setMessageId(message.getMessageId());
                status.setToId(member.getMemberId());
                status.setReadStatus(0); // 未读
                statusList.add(status);
            }
        });

        imGroupMessageDubboService.creatBatch(statusList);

        // 更新群会话
        ImChatPo chat = new ImChatPo();
        chat.setChatId(UUID.randomUUID().toString());
        chat.setOwnerId(fromUserId);
        chat.setToId(groupId);
        chat.setChatType(1); // 群聊
        chat.setSequence(System.currentTimeMillis());
        imChatDubboService.creatOrModify(chat);
    }
}
```

---

#### 9.4 批量创建群消息状态

```java
boolean creatBatch(List<ImGroupMessageStatusPo> groupMessagePoList)
```

**功能说明**: 批量创建群消息状态记录（用于记录每个成员的阅读状态）

**参数**:

- `groupMessagePoList` (List<ImGroupMessageStatusPo>): 群消息状态列表

**返回值**:

- `boolean`: true-批量创建成功，false-批量创建失败

**异常**: 无

**示例代码**:

```java
public void createGroupMessageStatus(String groupId, String messageId, List<String> memberIds) {
    List<ImGroupMessageStatusPo> statusList = new ArrayList<>();

    memberIds.forEach(memberId -> {
        ImGroupMessageStatusPo status = new ImGroupMessageStatusPo();
        status.setGroupId(groupId);
        status.setMessageId(messageId);
        status.setToId(memberId);
        status.setReadStatus(0); // 未读
        statusList.add(status);
    });

    boolean result = imGroupMessageDubboService.creatBatch(statusList);
    System.out.println("批量创建消息状态结果: " + result);
}
```

---

#### 9.5 更新群聊消息

```java
boolean modify(ImGroupMessagePo groupMessagePo)
```

**功能说明**: 更新群聊消息

**参数**:

- `groupMessagePo` (ImGroupMessagePo): 群聊消息对象
    - `messageId` (String): 消息ID，必填

**返回值**:

- `boolean`: true-更新成功，false-更新失败

**异常**: 无

**示例代码**:

```java
public void updateGroupMessage(String messageId) {
    ImGroupMessagePo message = new ImGroupMessagePo();
    message.setMessageId(messageId);
    // 更新消息内容或其他字段

    boolean result = imGroupMessageDubboService.modify(message);
    System.out.println("更新结果: " + result);
}
```

---

#### 9.6 删除群聊消息

```java
boolean removeOne(String messageId)
```

**功能说明**: 删除群聊消息

**参数**:

- `messageId` (String): 消息ID，必填

**返回值**:

- `boolean`: true-删除成功，false-删除失败

**异常**: 无

**示例代码**:

```java
public void deleteGroupMessage(String messageId) {
    boolean result = imGroupMessageDubboService.removeOne(messageId);
    System.out.println("删除结果: " + result);
}
```

---

#### 9.7 查询群聊最后一条消息

```java
ImGroupMessagePo queryLast(String groupId, String userId)
```

**功能说明**: 查询用户在群组中的最后一条消息

**参数**:

- `groupId` (String): 群组ID，必填
- `userId` (String): 用户ID，必填

**返回值**:

- `ImGroupMessagePo`: 群聊消息对象，不存在时返回 null

**异常**: 无

**示例代码**:

```java
public void getLastGroupMessage(String groupId, String userId) {
    ImGroupMessagePo message = imGroupMessageDubboService.queryLast(groupId, userId);
    if (message != null) {
        System.out.println("最后一条消息: " + message.getMessageBody());
    }
}
```

---

#### 9.8 查询群聊消息已读状态

```java
Integer queryReadStatus(String groupId, String ownerId, Integer code)
```

**功能说明**: 查询群聊消息的已读状态

**参数**:

- `groupId` (String): 群组ID，必填
- `ownerId` (String): 群主ID，必填
- `code` (Integer): 状态码

**返回值**:

- `Integer`: 已读状态

**异常**: 无

**示例代码**:

```java
public void checkGroupReadStatus(String groupId, String ownerId) {
    Integer status = imGroupMessageDubboService.queryReadStatus(groupId, ownerId, 1);
    System.out.println("已读状态: " + status);
}
```

---

### 数据模型: ImGroupMessagePo

```java
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "群聊消息")
@TableName(value = "im_group_message")
public class ImGroupMessagePo extends BasePo {

    /**
     * 消息ID
     */
    @TableId(value = "message_id")
    private String messageId;

    /**
     * 群组ID
     */
    @TableField(value = "group_id")
    private String groupId;

    /**
     * 发送者用户ID
     */
    @TableField(value = "from_id")
    private String fromId;

    /**
     * 消息内容
     */
    @TableField(value = "message_body", typeHandler = JacksonTypeHandler.class)
    private Object messageBody;

    /**
     * 发送时间
     */
    @TableField(value = "message_time")
    private Long messageTime;

    /**
     * 消息类型
     */
    @TableField(value = "message_content_type")
    private Integer messageContentType;

    /**
     * 扩展字段
     */
    @TableField(value = "extra", typeHandler = JacksonTypeHandler.class)
    private Object extra;

    /**
     * 阅读状态（1已读）
     */
    @TableField(exist = false)
    private Integer readStatus;

    /**
     * 消息序列
     */
    @TableField(value = "sequence")
    private Long sequence;

    /**
     * 随机标识
     */
    @TableField(value = "message_random")
    private String messageRandom;
}
```

**字段说明**:

- `messageId`: 消息唯一标识
- `groupId`: 群组ID
- `fromId`: 发送者用户ID
- `messageBody`: 消息内容（JSON格式）
- `messageTime`: 消息发送时间（毫秒时间戳）
- `messageContentType`: 消息类型（1-文本，2-图片，3-语音，4-视频，5-文件等）
- `extra`: 扩展字段（JSON格式）
- `readStatus`: 阅读状态（非数据库字段，查询时动态填充）
- `sequence`: 消息序列号
- `messageRandom`: 消息随机标识

### 数据模型: ImGroupMessageStatusPo

```java
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "群聊消息阅读状态")
@TableName(value = "im_group_message_status", excludeProperty = {"delFlag"})
public class ImGroupMessageStatusPo extends BasePo {

    /**
     * 群组ID
     */
    @TableId(value = "group_id")
    private String groupId;

    /**
     * 消息ID
     */
    @TableField(value = "message_id")
    private String messageId;

    /**
     * 接收者用户ID
     */
    @TableField(value = "to_id")
    private String toId;

    /**
     * 阅读状态（1已读）
     */
    @TableField(value = "read_status")
    private Integer readStatus;
}
```

**字段说明**:

- `groupId`: 群组ID（联合主键的一部分）
- `messageId`: 消息ID
- `toId`: 接收者用户ID
- `readStatus`: 阅读状态（0-未读，1-已读）

---

## 10. ImChatDubboService - 聊天会话服务

### 接口描述

提供聊天会话的增删改查操作，会话用于管理用户的聊天列表。

### 接口全限定名

```
com.xy.lucky.api.chat.ImChatDubboService
```

### 方法列表

#### 10.1 查询单个会话

```java
ImChatPo queryOne(String ownerId, String toId, Integer chatType)
```

**功能说明**: 查询指定用户的特定会话

**参数**:

- `ownerId` (String): 所属用户ID，必填
- `toId` (String): 会话对象ID（好友ID或群组ID），必填
- `chatType` (Integer): 会话类型
    - 0: 单聊
    - 1: 群聊
    - 2: 机器人
    - 3: 公众号

**返回值**:

- `ImChatPo`: 会话信息对象，不存在时返回 null

**异常**: 无

**示例代码**:

```java

@Autowired
private ImChatDubboService imChatDubboService;

public void getChat(String userId, String friendId) {
    // 查询单聊会话
    ImChatPo chat = imChatDubboService.queryOne(userId, friendId, 0);
    if (chat != null) {
        System.out.println("会话ID: " + chat.getChatId());
        System.out.println("是否置顶: " + (chat.getIsTop() == 1));
        System.out.println("是否免打扰: " + (chat.getIsMute() == 1));
    } else {
        System.out.println("会话不存在");
    }
}

public void getGroupChat(String userId, String groupId) {
    // 查询群聊会话
    ImChatPo chat = imChatDubboService.queryOne(userId, groupId, 1);
    System.out.println("群会话: " + chat);
}
```

---

#### 10.2 查询用户所有会话

```java
List<ImChatPo> queryList(String ownerId, Long sequence)
```

**功能说明**: 查询用户的所有会话列表（支持增量同步）

**参数**:

- `ownerId` (String): 所属用户ID，必填
- `sequence` (Long): 时序，可选（null表示查询全部）

**返回值**:

- `List<ImChatPo>`: 会话列表

**异常**: 无

**示例代码**:

```java
public void getUserChats(String userId) {
    // 查询所有会话
    List<ImChatPo> chats = imChatDubboService.queryList(userId, null);
    System.out.println("会话数量: " + chats.size());

    // 增量查询（从某个序列号之后的会话）
    List<ImChatPo> newChats = imChatDubboService.queryList(userId, 1234567890L);
    System.out.println("新会话数量: " + newChats.size());

    // 按sequence降序排序（最新的在前面）
    chats.sort((a, b) -> b.getSequence().compareTo(a.getSequence()));

    chats.forEach(chat -> {
        String type = chat.getChatType() == 0 ? "单聊" :
                chat.getChatType() == 1 ? "群聊" : "其他";
        System.out.println("会话类型: " + type + ", 对象ID: " + chat.getToId());
    });
}
```

---

#### 10.3 创建会话

```java
Boolean creat(ImChatPo chatPo)
```

**功能说明**: 创建新的聊天会话

**参数**:

- `chatPo` (ImChatPo): 会话信息对象
    - `chatId` (String): 会话ID，必填
    - `ownerId` (String): 所属用户ID，必填
    - `toId` (String): 会话对象ID，必填
    - `chatType` (Integer): 会话类型，必填

**返回值**:

- `Boolean`: true-创建成功，false-创建失败

**异常**: 无

**示例代码**:

```java
public void createChat(String userId, String friendId) {
    ImChatPo chat = new ImChatPo();
    chat.setChatId(UUID.randomUUID().toString());
    chat.setOwnerId(userId);
    chat.setToId(friendId);
    chat.setChatType(0); // 单聊
    chat.setIsMute(0); // 不免打扰
    chat.setIsTop(0); // 不置顶
    chat.setSequence(System.currentTimeMillis());
    chat.setReadSequence(0L);

    Boolean result = imChatDubboService.creat(chat);
    if (result) {
        System.out.println("会话创建成功");
    }
}
```

---

#### 10.4 更新会话

```java
Boolean modify(ImChatPo chatPo)
```

**功能说明**: 更新会话信息

**参数**:

- `chatPo` (ImChatPo): 会话信息对象
    - `chatId` (String): 会话ID，必填

**返回值**:

- `Boolean`: true-更新成功，false-更新失败

**异常**: 无

**示例代码**:

```java
public void updateChat(String chatId) {
    ImChatPo chat = new ImChatPo();
    chat.setChatId(chatId);
    chat.setIsTop(1); // 置顶
    chat.setIsMute(1); // 免打扰

    Boolean result = imChatDubboService.modify(chat);
    System.out.println("更新结果: " + result);
}

public void updateChatSequence(String chatId) {
    ImChatPo chat = new ImChatPo();
    chat.setChatId(chatId);
    chat.setSequence(System.currentTimeMillis()); // 更新序列号

    imChatDubboService.modify(chat);
}
```

---

#### 10.5 创建或更新会话

```java
Boolean creatOrModify(ImChatPo chatPo)
```

**功能说明**: 创建会话，如果已存在则更新（用于接收新消息时更新会话）

**参数**:

- `chatPo` (ImChatPo): 会话信息对象

**返回值**:

- `Boolean`: true-操作成功，false-操作失败

**异常**: 无

**示例代码**:

```java
public void onReceiveMessage(String fromId, String toId, Long messageSequence) {
    // 更新接收者的会话
    ImChatPo chat = new ImChatPo();
    chat.setChatId(UUID.randomUUID().toString());
    chat.setOwnerId(toId);
    chat.setToId(fromId);
    chat.setChatType(0); // 单聊
    chat.setSequence(messageSequence);
    chat.setReadSequence(messageSequence - 1); // 之前的已读序列

    // 使用 creatOrModify 自动处理创建或更新
    Boolean result = imChatDubboService.creatOrModify(chat);
    if (result) {
        System.out.println("会话已更新");
    }
}

public void markChatAsRead(String ownerId, String toId, Long readSequence) {
    ImChatPo chat = new ImChatPo();
    chat.setChatId(UUID.randomUUID().toString());
    chat.setOwnerId(ownerId);
    chat.setToId(toId);
    chat.setChatType(0);
    chat.setReadSequence(readSequence); // 更新已读序列

    imChatDubboService.creatOrModify(chat);
}
```

---

#### 10.6 删除会话

```java
Boolean removeOne(String id)
```

**功能说明**: 删除会话（不会删除聊天记录，只是从会话列表中移除）

**参数**:

- `id` (String): 会话ID，必填

**返回值**:

- `Boolean`: true-删除成功，false-删除失败

**异常**: 无

**示例代码**:

```java
public void deleteChat(String chatId) {
    Boolean result = imChatDubboService.removeOne(chatId);
    if (result) {
        System.out.println("会话已删除");
    }
}
```

---

### 数据模型: ImChatPo

```java
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户聊天会话信息")
@TableName(value = "im_chat")
public class ImChatPo extends BasePo {

    /**
     * 会话ID
     */
    @TableId(value = "chat_id")
    private String chatId;

    /**
     * 会话类型（0 单聊 1群聊 2机器人 3公众号）
     */
    @TableField(value = "chat_type")
    private Integer chatType;

    /**
     * 所属用户ID
     */
    @TableField(value = "owner_id")
    private String ownerId;

    /**
     * 会话对象ID（好友ID或群组ID）
     */
    @TableField(value = "to_id")
    private String toId;

    /**
     * 是否免打扰（1免打扰）
     */
    @TableField(value = "is_mute")
    private Integer isMute;

    /**
     * 是否置顶（1置顶）
     */
    @TableField(value = "is_top")
    private Integer isTop;

    /**
     * 会话序列号
     */
    @TableField(value = "sequence")
    private Long sequence;

    /**
     * 已读序列号
     */
    @TableField(value = "read_sequence")
    private Long readSequence;
}
```

**字段说明**:

- `chatId`: 会话唯一标识
- `chatType`: 会话类型（0-单聊，1-群聊，2-机器人，3-公众号）
- `ownerId`: 所属用户ID
- `toId`: 会话对象ID（单聊时为好友ID，群聊时为群组ID）
- `isMute`: 是否免打扰（0-否，1-是）
- `isTop`: 是否置顶（0-否，1-是）
- `sequence`: 会话序列号（用于排序，最新消息的序列号）
- `readSequence`: 已读序列号（标记用户已读到的位置）

---

## 11. ImUserEmojiPackDubboService - 用户表情包服务

### 接口描述

提供用户表情包的绑定和解绑操作。

### 接口全限定名

```
com.xy.lucky.api.emoji.ImUserEmojiPackDubboService
```

### 方法列表

#### 11.1 获取用户的表情包列表

```java
List<ImUserEmojiPackPo> listByUserId(String userId)
```

**功能说明**: 获取用户绑定的所有表情包

**参数**:

- `userId` (String): 用户ID，必填

**返回值**:

- `List<ImUserEmojiPackPo>`: 用户表情包关联列表

**异常**: 无

**示例代码**:

```java

@Autowired
private ImUserEmojiPackDubboService imUserEmojiPackDubboService;

public void getUserEmojiPacks(String userId) {
    List<ImUserEmojiPackPo> packs = imUserEmojiPackDubboService.listByUserId(userId);
    System.out.println("用户绑定的表情包数量: " + packs.size());

    packs.forEach(pack -> {
        System.out.println("表情包ID: " + pack.getPackId());
    });
}
```

---

#### 11.2 获取用户的表情包ID列表

```java
List<String> listPackIds(String userId)
```

**功能说明**: 获取用户绑定的所有表情包ID

**参数**:

- `userId` (String): 用户ID，必填

**返回值**:

- `List<String>`: 表情包ID列表

**异常**: 无

**示例代码**:

```java
public void getUserEmojiPackIds(String userId) {
    List<String> packIds = imUserEmojiPackDubboService.listPackIds(userId);
    System.out.println("表情包ID列表: " + packIds);
}
```

---

#### 11.3 绑定单个表情包

```java
Boolean bindPack(String userId, String packId)
```

**功能说明**: 为用户绑定单个表情包

**参数**:

- `userId` (String): 用户ID，必填
- `packId` (String): 表情包ID，必填

**返回值**:

- `Boolean`: true-绑定成功，false-绑定失败

**异常**: 无

**示例代码**:

```java
public void bindEmojiPack(String userId, String packId) {
    Boolean result = imUserEmojiPackDubboService.bindPack(userId, packId);
    if (result) {
        System.out.println("表情包绑定成功");
    } else {
        System.out.println("表情包绑定失败或已存在");
    }
}
```

---

#### 11.4 批量绑定表情包

```java
Boolean bindPacks(String userId, List<String> packIds)
```

**功能说明**: 为用户批量绑定表情包

**参数**:

- `userId` (String): 用户ID，必填
- `packIds` (List<String>): 表情包ID列表，必填

**返回值**:

- `Boolean`: true-批量绑定成功，false-批量绑定失败

**异常**: 无

**示例代码**:

```java
public void bindEmojiPacks(String userId) {
    List<String> packIds = Arrays.asList("pack_001", "pack_002", "pack_003");

    Boolean result = imUserEmojiPackDubboService.bindPacks(userId, packIds);
    if (result) {
        System.out.println("批量绑定表情包成功");
    }
}
```

---

#### 11.5 解绑表情包

```java
Boolean unbindPack(String userId, String packId)
```

**功能说明**: 为用户解绑表情包

**参数**:

- `userId` (String): 用户ID，必填
- `packId` (String): 表情包ID，必填

**返回值**:

- `Boolean`: true-解绑成功，false-解绑失败

**异常**: 无

**示例代码**:

```java
public void unbindEmojiPack(String userId, String packId) {
    Boolean result = imUserEmojiPackDubboService.unbindPack(userId, packId);
    if (result) {
        System.out.println("表情包解绑成功");
    }
}
```

---

### 数据模型: ImUserEmojiPackPo

```java
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户-表情包关联信息")
@TableName(value = "im_user_emoji_pack")
public class ImUserEmojiPackPo extends BasePo {

    @TableId(value = "id")
    private String id;

    @TableField(value = "user_id")
    private String userId;

    @TableField(value = "pack_id")
    private String packId;
}
```

**字段说明**:

- `id`: 关联记录唯一标识
- `userId`: 用户ID
- `packId`: 表情包ID

---

## 12. IMOutboxDubboService - 消息出站服务

### 接口描述

提供消息出站的增删改查操作，用于保证消息通过 MQ 可靠发送（Outbox 模式）。

### 接口全限定名

```
com.xy.lucky.api.outbox.IMOutboxDubboService
```

### 方法列表

#### 12.1 获取所有消息

```java
List<IMOutboxPo> queryList()
```

**功能说明**: 获取所有出站消息

**参数**: 无

**返回值**:

- `List<IMOutboxPo>`: 出站消息列表

**异常**: 无

**示例代码**:

```java

@Autowired
private IMOutboxDubboService imOutboxDubboService;

public void getAllOutboxMessages() {
    List<IMOutboxPo> messages = imOutboxDubboService.queryList();
    System.out.println("出站消息数量: " + messages.size());
}
```

---

#### 12.2 获取单个消息

```java
IMOutboxPo queryOne(Long id)
```

**功能说明**: 根据ID获取出站消息详情

**参数**:

- `id` (Long): 消息ID，必填

**返回值**:

- `IMOutboxPo`: 出站消息对象，不存在时返回 null

**异常**: 无

**示例代码**:

```java
public void getOutboxMessage(Long id) {
    IMOutboxPo message = imOutboxDubboService.queryOne(id);
    if (message != null) {
        System.out.println("消息ID: " + message.getMessageId());
        System.out.println("交换机: " + message.getExchange());
        System.out.println("路由键: " + message.getRoutingKey());
        System.out.println("状态: " + message.getStatus());
        System.out.println("尝试次数: " + message.getAttempts());
    }
}
```

---

#### 12.3 保存出站消息

```java
Boolean creat(IMOutboxPo outboxPo)
```

**功能说明**: 保存出站消息（在发送到 MQ 前先保存到 Outbox 表）

**参数**:

- `outboxPo` (IMOutboxPo): 出站消息对象
    - `id` (Long): 消息ID，必填
    - `messageId` (String): 业务消息ID，必填
    - `payload` (Object): 消息负载，必填
    - `exchange` (String): 交换机名称，必填
    - `routingKey` (String): 路由键，必填
    - `status` (String): 状态，必填

**返回值**:

- `Boolean`: true-保存成功，false-保存失败

**异常**: 无

**示例代码**:

```java
public void saveOutboxMessage(String messageId, Object messageContent) {
    IMOutboxPo outbox = new IMOutboxPo();
    outbox.setId(System.currentTimeMillis()); // 使用时间戳作为ID
    outbox.setMessageId(messageId);

    // 构建轻量级的消息负载
    Map<String, Object> payload = new HashMap<>();
    payload.put("messageId", messageId);
    payload.put("type", "single_message");
    outbox.setPayload(payload);

    outbox.setExchange("im.exchange");
    outbox.setRoutingKey("message.single");
    outbox.setStatus("PENDING");
    outbox.setAttempts(0);
    outbox.setCreatedAt(System.currentTimeMillis());
    outbox.setNextTryAt(System.currentTimeMillis());

    Boolean result = imOutboxDubboService.creat(outbox);
    if (result) {
        System.out.println("出站消息已保存");

        // 发送到 MQ
        try {
            // 发送逻辑...
            // 发送成功后更新状态
            imOutboxDubboService.modifyStatus(outbox.getId(), "SENT", 1);
        } catch (Exception e) {
            // 发送失败更新状态
            imOutboxDubboService.modifyToFailed(outbox.getId(), e.getMessage(), 1);
        }
    }
}
```

---

#### 12.4 批量保存出站消息

```java
Boolean creatBatch(List<IMOutboxPo> list)
```

**功能说明**: 批量保存出站消息

**参数**:

- `list` (List<IMOutboxPo>): 出站消息列表

**返回值**:

- `Boolean`: true-批量保存成功，false-批量保存失败

**异常**: 无

**示例代码**:

```java
public void batchSaveOutboxMessages(List<Map<String, Object>> messages) {
    List<IMOutboxPo> outboxList = new ArrayList<>();

    messages.forEach(msg -> {
        IMOutboxPo outbox = new IMOutboxPo();
        outbox.setId(System.currentTimeMillis() + outboxList.size());
        outbox.setMessageId((String) msg.get("messageId"));
        outbox.setPayload(msg.get("payload"));
        outbox.setExchange((String) msg.get("exchange"));
        outbox.setRoutingKey((String) msg.get("routingKey"));
        outbox.setStatus("PENDING");
        outbox.setAttempts(0);
        outbox.setCreatedAt(System.currentTimeMillis());
        outbox.setNextTryAt(System.currentTimeMillis());
        outboxList.add(outbox);
    });

    Boolean result = imOutboxDubboService.creatBatch(outboxList);
    System.out.println("批量保存结果: " + result);
}
```

---

#### 12.5 更新出站消息

```java
Boolean modify(IMOutboxPo outboxPo)
```

**功能说明**: 更新出站消息信息

**参数**:

- `outboxPo` (IMOutboxPo): 出站消息对象
    - `id` (Long): 消息ID，必填

**返回值**:

- `Boolean`: true-更新成功，false-更新失败

**异常**: 无

**示例代码**:

```java
public void updateOutboxMessage(Long id) {
    IMOutboxPo outbox = new IMOutboxPo();
    outbox.setId(id);
    // 更新其他字段...

    Boolean result = imOutboxDubboService.modify(outbox);
    System.out.println("更新结果: " + result);
}
```

---

#### 12.6 保存或更新出站消息

```java
boolean creatOrModify(IMOutboxPo outboxPo)
```

**功能说明**: 保存出站消息，如果已存在则更新

**参数**:

- `outboxPo` (IMOutboxPo): 出站消息对象

**返回值**:

- `boolean`: true-操作成功，false-操作失败

**异常**: 无

**示例代码**:

```java
public void saveOrUpdateOutbox(Long id, String messageId) {
    IMOutboxPo outbox = new IMOutboxPo();
    outbox.setId(id);
    outbox.setMessageId(messageId);
    // 设置其他字段...

    boolean result = imOutboxDubboService.creatOrModify(outbox);
    System.out.println("保存或更新结果: " + result);
}
```

---

#### 12.7 删除出站消息

```java
Boolean removeOne(Long id)
```

**功能说明**: 删除出站消息

**参数**:

- `id` (Long): 消息ID，必填

**返回值**:

- `Boolean`: true-删除成功，false-删除失败

**异常**: 无

**示例代码**:

```java
public void deleteOutboxMessage(Long id) {
    Boolean result = imOutboxDubboService.removeOne(id);
    System.out.println("删除结果: " + result);
}
```

---

#### 12.8 获取待发送的消息

```java
List<IMOutboxPo> queryByStatus(String status, Integer limit)
```

**功能说明**: 根据状态批量获取待发送的消息（用于定时任务重试）

**参数**:

- `status` (String): 消息状态
    - "PENDING": 待发送
    - "FAILED": 失败
- `limit` (Integer): 限制数量

**返回值**:

- `List<IMOutboxPo>`: 待发送的消息列表

**异常**: 无

**示例代码**:

```java

@Scheduled(fixedDelay = 5000) // 每5秒执行一次
public void retryPendingMessages() {
    // 获取待发送的消息（最多100条）
    List<IMOutboxPo> pendingMessages = imOutboxDubboService.queryByStatus("PENDING", 100);

    pendingMessages.forEach(outbox -> {
        if (outbox.getNextTryAt() <= System.currentTimeMillis()) {
            try {
                // 重新发送到 MQ
                sendToMQ(outbox);

                // 更新状态为已发送
                imOutboxDubboService.modifyStatus(outbox.getId(), "SENT", outbox.getAttempts() + 1);
            } catch (Exception e) {
                // 更新状态为失败
                imOutboxDubboService.modifyToFailed(outbox.getId(), e.getMessage(), outbox.getAttempts() + 1);
            }
        }
    });

    // 处理失败的消息
    List<IMOutboxPo> failedMessages = imOutboxDubboService.queryByStatus("FAILED", 50);
    // 重试逻辑...
}
```

---

#### 12.9 更新消息状态

```java
Boolean modifyStatus(Long id, String status, Integer attempts)
```

**功能说明**: 更新出站消息的状态和尝试次数

**参数**:

- `id` (Long): 消息ID，必填
- `status` (String): 新状态
    - "PENDING": 待发送
    - "SENT": 已发送
    - "FAILED": 失败
    - "DLX": 死信
- `attempts` (Integer): 尝试次数

**返回值**:

- `Boolean`: true-更新成功，false-更新失败

**异常**: 无

**示例代码**:

```java
public void markAsSent(Long id, Integer currentAttempts) {
    Boolean result = imOutboxDubboService.modifyStatus(id, "SENT", currentAttempts + 1);
    if (result) {
        System.out.println("消息已标记为发送成功");
    }
}

public void markAsPending(Long id, Integer currentAttempts) {
    // 计算下次重试时间（指数退避）
    long nextTryAt = System.currentTimeMillis() + (long) Math.pow(2, currentAttempts) * 1000;

    IMOutboxPo outbox = new IMOutboxPo();
    outbox.setId(id);
    outbox.setNextTryAt(nextTryAt);
    imOutboxDubboService.modify(outbox);

    imOutboxDubboService.modifyStatus(id, "PENDING", currentAttempts + 1);
}
```

---

#### 12.10 更新消息为发送失败

```java
Boolean modifyToFailed(Long id, String lastError, Integer attempts)
```

**功能说明**: 更新出站消息为失败状态，记录错误信息

**参数**:

- `id` (Long): 消息ID，必填
- `lastError` (String): 最后一次错误信息
- `attempts` (Integer): 尝试次数

**返回值**:

- `Boolean`: true-更新成功，false-更新失败

**异常**: 无

**示例代码**:

```java
public void markAsFailed(Long id, Exception e, Integer currentAttempts) {
    Boolean result = imOutboxDubboService.modifyToFailed(
            id,
            e.getMessage(),
            currentAttempts + 1
    );

    if (result) {
        System.out.println("消息已标记为发送失败");
        // 可以发送告警通知
    }
}
```

---

### 数据模型: IMOutboxPo

```java

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "消息投递 Outbox 实体")
@TableName(value = "im_outbox", excludeProperty = {"createTime", "updateTime", "delFlag", "version"})
public class IMOutboxPo extends BasePo {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.NONE)
    private Long id;

    /**
     * 业务 messageId
     */
    @TableField(value = "message_id")
    private String messageId;

    /**
     * 要发送的 JSON 负载（建议尽量轻量：可仅包含 messageId + 必要路由信息）
     */
    @TableField(value = "payload", typeHandler = JacksonTypeHandler.class)
    private Object payload;

    /**
     * 交换机名称
     */
    @TableField(value = "exchange")
    private String exchange;

    /**
     * 路由键
     */
    @TableField(value = "routing_key")
    private String routingKey;

    /**
     * 累积投递次数
     */
    @TableField(value = "attempts")
    private Integer attempts;

    /**
     * 投递状态：PENDING(待投递) / SENT(已确认) / FAILED(失败，需要人工介入) / DLX(死信)
     */
    @TableField(value = "status")
    private String status;

    /**
     * 最后错误信息
     */
    @TableField(value = "last_error")
    private String lastError;

    /**
     * 下一次重试时间（用以调度延迟重试）
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "next_try_at", fill = FieldFill.INSERT)
    private Long nextTryAt;

    /**
     * 创建时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private Long createdAt;

    /**
     * 更新时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "updated_at", fill = FieldFill.UPDATE)
    private Long updatedAt;
}
```

**字段说明**:

- `id`: 主键ID
- `messageId`: 业务消息ID
- `payload`: 消息负载（JSON格式，建议轻量化）
- `exchange`: RabbitMQ 交换机名称
- `routingKey`: RabbitMQ 路由键
- `attempts`: 累积投递次数
- `status`: 投递状态（PENDING-待投递，SENT-已确认，FAILED-失败，DLX-死信）
- `lastError`: 最后一次错误信息
- `nextTryAt`: 下一次重试时间（毫秒时间戳）
- `createdAt`: 创建时间（毫秒时间戳）
- `updatedAt`: 更新时间（毫秒时间戳）

---

## 13. ImAuthTokenDubboService - 认证令牌服务

### 接口描述

提供认证令牌的持久化操作，用于令牌管理和撤销。

### 接口全限定名

```
com.xy.lucky.api.auth.ImAuthTokenDubboService
```

### 方法列表

#### 13.1 保存令牌元信息

```java
Boolean create(ImAuthTokenPo token)
```

**功能说明**: 保存认证令牌的元信息到数据库

**参数**:

- `token` (ImAuthTokenPo): 认证令牌对象
    - `id` (String): 令牌ID，必填
    - `userId` (String): 用户ID，必填
    - `deviceId` (String): 设备ID，必填
    - `accessTokenHash` (String): 访问令牌哈希，必填
    - `refreshTokenHash` (String): 刷新令牌哈希，必填

**返回值**:

- `Boolean`: true-保存成功，false-保存失败

**异常**: 无

**示例代码**:

```java

@Autowired
private ImAuthTokenDubboService imAuthTokenDubboService;

public void saveToken(String userId, String deviceId, String accessToken, String refreshToken) {
    ImAuthTokenPo token = new ImAuthTokenPo();
    token.setId(UUID.randomUUID().toString());
    token.setUserId(userId);
    token.setDeviceId(deviceId);
    token.setClientIp("192.168.1.100");
    token.setUserAgent("Mozilla/5.0...");

    // 存储令牌的哈希值（不存储明文）
    token.setAccessTokenHash(digest(accessToken));
    token.setRefreshTokenHash(digest(refreshToken));

    token.setTokenVersion(1L);
    token.setTokenFamilyId(UUID.randomUUID().toString());
    token.setSequenceNumber(1);

    long now = System.currentTimeMillis();
    token.setIssuedAt(now);
    token.setAccessExpiresAt(now + 3600000); // 1小时后过期
    token.setAbsoluteExpiresAt(now + 2592000000L); // 30天后绝对过期

    token.setUsed(0); // 未使用
    token.setRevokedAt(null); // 未撤销

    token.setGrantType("password");
    token.setScope("read write");

    Boolean result = imAuthTokenDubboService.create(token);
    if (result) {
        System.out.println("令牌已保存");
    }
}

private String digest(String input) {
    // 哈希函数（如 SHA-256）
    return DigestUtils.sha256Hex(input);
}
```

---

#### 13.2 标记刷新令牌已使用

```java
Boolean markUsedByRefreshHash(String refreshTokenHash)
```

**功能说明**: 标记刷新令牌已被使用（防止重放攻击）

**参数**:

- `refreshTokenHash` (String): 刷新令牌哈希值，必填

**返回值**:

- `Boolean`: true-标记成功，false-标记失败

**异常**: 无

**示例代码**:

```java
public void refreshToken(String oldRefreshToken) {
    String refreshTokenHash = digest(oldRefreshToken);

    // 标记旧刷新令牌已使用
    Boolean marked = imAuthTokenDubboService.markUsedByRefreshHash(refreshTokenHash);
    if (!marked) {
        throw new RuntimeException("刷新令牌无效或已使用");
    }

    // 生成新的访问令牌和刷新令牌
    String newAccessToken = generateAccessToken();
    String newRefreshToken = generateRefreshToken();

    // 保存新令牌...
}
```

---

#### 13.3 撤销访问令牌

```java
Boolean revokeByAccessHash(String accessTokenHash, String reason)
```

**功能说明**: 撤销指定的访问令牌

**参数**:

- `accessTokenHash` (String): 访问令牌哈希值，必填
- `reason` (String): 撤销原因

**返回值**:

- `Boolean`: true-撤销成功，false-撤销失败

**异常**: 无

**示例代码**:

```java
public void logout(String accessToken) {
    String accessTokenHash = digest(accessToken);

    Boolean result = imAuthTokenDubboService.revokeByAccessHash(
            accessTokenHash,
            "用户主动登出"
    );

    if (result) {
        System.out.println("令牌已撤销");
    }
}

public void forceLogoutUser(String userId) {
    // 查询用户的所有令牌并撤销
    // 注意：这里需要额外的查询方法，当前接口只提供了按哈希撤销
    // 可以在实现层添加按用户ID撤销的功能
}
```

---

#### 13.4 撤销刷新令牌

```java
Boolean revokeByRefreshHash(String refreshTokenHash, String reason)
```

**功能说明**: 撤销指定的刷新令牌

**参数**:

- `refreshTokenHash` (String): 刷新令牌哈希值，必填
- `reason` (String): 撤销原因

**返回值**:

- `Boolean`: true-撤销成功，false-撤销失败

**异常**: 无

**示例代码**:

```java
public void revokeRefreshToken(String refreshToken) {
    String refreshTokenHash = digest(refreshToken);

    Boolean result = imAuthTokenDubboService.revokeByRefreshHash(
            refreshTokenHash,
            "令牌泄露"
    );

    if (result) {
        System.out.println("刷新令牌已撤销");

        // 通知用户重新登录
        sendSecurityNotification("检测到异常登录，请重新登录");
    }
}

public void onPasswordChanged(String userId) {
    // 密码修改后，撤销所有刷新令牌
    // 注意：需要实现层的支持来按用户ID查询和撤销
}
```

---

### 数据模型: ImAuthTokenPo

```java
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "认证令牌持久化信息")
@TableName(value = "im_auth_token")
public class ImAuthTokenPo extends BasePo {

    @TableId(value = "id")
    private String id;

    @Schema(description = "用户id")
    @TableField(value = "user_id")
    private String userId;

    @Schema(description = "设备id")
    @TableField(value = "device_id")
    private String deviceId;

    @Schema(description = "客户端ip")
    @TableField(value = "client_ip")
    private String clientIp;

    @Schema(description = "用户代理")
    @TableField(value = "user_agent")
    private String userAgent;

    @Schema(description = "访问令牌")
    @TableField(value = "access_token_hash")
    private String accessTokenHash;

    @Schema(description = "刷新令牌")
    @TableField(value = "refresh_token_hash")
    private String refreshTokenHash;

    @Schema(description = "令牌版本")
    @TableField(value = "token_version")
    private Long tokenVersion;

    @Schema(description = "令牌族")
    @TableField(value = "token_family_id")
    private String tokenFamilyId;

    @Schema(description = "令牌序列号")
    @TableField(value = "sequence_number")
    private Integer sequenceNumber;

    @Schema(description = "令牌颁发时间")
    @TableField(value = "issued_at")
    private Long issuedAt;

    @Schema(description = "令牌访问到期时间")
    @TableField(value = "access_expires_at")
    private Long accessExpiresAt;

    @Schema(description = "令牌绝对到期时间")
    @TableField(value = "absolute_expires_at")
    private Long absoluteExpiresAt;

    @Schema(description = "令牌是否被使用")
    @TableField(value = "used")
    private Integer used;

    @Schema(description = "令牌是否被撤销")
    @TableField(value = "revoked_at")
    private Long revokedAt;

    @Schema(description = "撤销原因")
    @TableField(value = "revoke_reason")
    private String revokeReason;

    @Schema(description = "授权类型")
    @TableField(value = "grant_type")
    private String grantType;

    @Schema(description = "授权范围")
    @TableField(value = "scope")
    private String scope;
}
```

**字段说明**:

- `id`: 令牌记录唯一标识
- `userId`: 用户ID
- `deviceId`: 设备ID
- `clientIp`: 客户端IP地址
- `userAgent`: 用户代理字符串
- `accessTokenHash`: 访问令牌的哈希值（不存储明文）
- `refreshTokenHash`: 刷新令牌的哈希值（不存储明文）
- `tokenVersion`: 令牌版本号
- `tokenFamilyId`: 令牌族ID（同一登录流程的令牌属于同一族）
- `sequenceNumber`: 令牌序列号
- `issuedAt`: 令牌颁发时间（毫秒时间戳）
- `accessExpiresAt`: 访问令牌过期时间（毫秒时间戳）
- `absoluteExpiresAt`: 绝对过期时间（毫秒时间戳，刷新令牌也会过期）
- `used`: 是否已使用（0-未使用，1-已使用）
- `revokedAt`: 撤销时间（毫秒时间戳）
- `revokeReason`: 撤销原因
- `grantType`: 授权类型（如：password、refresh_token、client_credentials等）
- `scope`: 授权范围（如：read write等）

---

## 通用数据模型: BasePo

所有数据模型（PO）都继承自 `BasePo`，包含以下公共字段：

```java
public class BasePo {
    /**
     * 创建时间
     */
    private Long createTime;

    /**
     * 更新时间
     */
    private Long updateTime;

    /**
     * 删除标记（0未删除，1已删除）
     */
    private Integer delFlag;

    /**
     * 乐观锁版本号
     */
    private Integer version;
}
```

**字段说明**:

- `createTime`: 记录创建时间（毫秒时间戳）
- `updateTime`: 记录更新时间（毫秒时间戳）
- `delFlag`: 逻辑删除标记（0-未删除，1-已删除）
- `version`: 乐观锁版本号，用于并发控制

---

## 附录

### A. 消息类型常量

建议定义以下消息类型常量：

```java
public class MessageContentType {
    /** 文本消息 */
    public static final int TEXT = 1;
    /** 图片消息 */
    public static final int IMAGE = 2;
    /** 语音消息 */
    public static final int VOICE = 3;
    /** 视频消息 */
    public static final int VIDEO = 4;
    /** 文件消息 */
    public static final int FILE = 5;
    /** 位置消息 */
    public static final int LOCATION = 6;
    /** 自定义消息 */
    public static final int CUSTOM = 99;
}
```

### B. 会话类型常量

```java
public class ChatType {
    /** 单聊 */
    public static final int SINGLE = 0;
    /** 群聊 */
    public static final int GROUP = 1;
    /** 机器人 */
    public static final int ROBOT = 2;
    /** 公众号 */
    public static final int PUBLIC_ACCOUNT = 3;
}
```

### C. 群成员角色常量

```java
public class GroupMemberRole {
    /** 普通成员 */
    public static final int MEMBER = 0;
    /** 管理员 */
    public static final int ADMIN = 1;
    /** 群主 */
    public static final int OWNER = 2;
}
```

### D. 群组类型常量

```java
public class GroupType {
    /** 私有群 */
    public static final int PRIVATE = 1;
    /** 公开群 */
    public static final int PUBLIC = 2;
}
```

### E. 审批状态常量

```java
public class ApproveStatus {
    /** 未审批 */
    public static final int PENDING = 0;
    /** 同意 */
    public static final int APPROVED = 1;
    /** 拒绝 */
    public static final int REJECTED = 2;
}
```

### F. Outbox 状态常量

```java
public class OutboxStatus {
    /** 待投递 */
    public static final String PENDING = "PENDING";
    /** 已确认 */
    public static final String SENT = "SENT";
    /** 失败 */
    public static final String FAILED = "FAILED";
    /** 死信 */
    public static final String DLX = "DLX";
}
```

---

## 总结

本文档详细介绍了 `im-database-rpc-api` 模块提供的 13 个 Dubbo 服务接口，共 80+ 个方法，涵盖了即时通讯系统的核心功能：

1. **用户管理**: 用户基础信息、用户扩展资料
2. **好友系统**: 好友关系、好友请求
3. **群组系统**: 群组管理、群成员管理、群邀请
4. **消息系统**: 单聊消息、群聊消息、消息状态
5. **会话管理**: 聊天会话
6. **扩展功能**: 表情包管理
7. **基础设施**: 消息出箱（Outbox 模式）、认证令牌

所有接口都支持通过 Dubbo RPC 进行远程调用，返回类型主要为 Boolean（表示操作是否成功）或实体对象（表示查询结果）。

在使用这些接口时，请注意：

- 所有时间字段均使用毫秒时间戳
- 建议使用序列号（sequence）实现增量同步
- 消息内容使用 JSON 格式存储
- 使用 Outbox 模式保证消息可靠性
- 令牌存储哈希值而非明文

更多使用示例和最佳实践，请参考 `HELP.md` 文档。
