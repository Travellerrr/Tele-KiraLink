# Telegram-Onebot-Adapter

![](https://img.shields.io/badge/OneBot-11-black?logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAHAAAABwCAMAAADxPgR5AAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAAxQTFRF////29vbr6+vAAAAk1hCcwAAAAR0Uk5T////AEAqqfQAAAKcSURBVHja7NrbctswDATQXfD//zlpO7FlmwAWIOnOtNaTM5JwDMa8E+PNFz7g3waJ24fviyDPgfhz8fHP39cBcBL9KoJbQUxjA2iYqHL3FAnvzhL4GtVNUcoSZe6eSHizBcK5LL7dBr2AUZlev1ARRHCljzRALIEog6H3U6bCIyqIZdAT0eBuJYaGiJaHSjmkYIZd+qSGWAQnIaz2OArVnX6vrItQvbhZJtVGB5qX9wKqCMkb9W7aexfCO/rwQRBzsDIsYx4AOz0nhAtWu7bqkEQBO0Pr+Ftjt5fFCUEbm0Sbgdu8WSgJ5NgH2iu46R/o1UcBXJsFusWF/QUaz3RwJMEgngfaGGdSxJkE/Yg4lOBryBiMwvAhZrVMUUvwqU7F05b5WLaUIN4M4hRocQQRnEedgsn7TZB3UCpRrIJwQfqvGwsg18EnI2uSVNC8t+0QmMXogvbPg/xk+Mnw/6kW/rraUlvqgmFreAA09xW5t0AFlHrQZ3CsgvZm0FbHNKyBmheBKIF2cCA8A600aHPmFtRB1XvMsJAiza7LpPog0UJwccKdzw8rdf8MyN2ePYF896LC5hTzdZqxb6VNXInaupARLDNBWgI8spq4T0Qb5H4vWfPmHo8OyB1ito+AysNNz0oglj1U955sjUN9d41LnrX2D/u7eRwxyOaOpfyevCWbTgDEoilsOnu7zsKhjRCsnD/QzhdkYLBLXjiK4f3UWmcx2M7PO21CKVTH84638NTplt6JIQH0ZwCNuiWAfvuLhdrcOYPVO9eW3A67l7hZtgaY9GZo9AFc6cryjoeFBIWeU+npnk/nLE0OxCHL1eQsc1IciehjpJv5mqCsjeopaH6r15/MrxNnVhu7tmcslay2gO2Z1QfcfX0JMACG41/u0RrI9QAAAABJRU5ErkJggg==)

基于 [OneBot](https://github.com/botuniverse/onebot/blob/main/README.md) 的 Telegram机器人Onebot v11 Java实现端

## 底层
- [Java Telegram Bot API](https://github.com/pengrad/java-telegram-bot-api): Telegram Bot API的Java实现

## 兼容性
完全兼容Onebot-v11协议,可与Onebot-v11协议的框架相连接,实现大部分功能

使用SpringBoot框架,可直接打包为jar文件在`Jdk17`环境下运行

提供Onebot-v11正向Websocket方式连接该项目。

Telegram适配器支持以下连接方式:

- [x] 纯http轮询 getmsg获取信息


支持连接 [Mirai-Overflow](https://github.com/MrXiaoM/Overflow)

其他项目暂未测试

可以与支持onebotV11适配器的项目相连接使用

## 配置指南

该项目目前仅支持[数组格式](https://github.com/botuniverse/onebot-11/blob/master/message/array.md)消息转发/接收,请确保你的框架支持该格式

后续会逐渐适配其他格式,若有问题请移步issue提出

支持proxy代理(HTTP(未测试)/SOCKS 账密),若有需要请在config.yml中配置

下方的需配置 均为config.yml的配置项,配置项右侧有注释解释和格式例子

```yaml
command:
  commandMap:
    start: 开始
    help: 帮助
    [Telegram发送内容]: [转发至Onebot内容]
  prefix: [指令前缀 默认为"/"
onebot:
  ip: 0.0.0.0
  path: [Onebot ws连接路径]
  port: [Onebot ws连接端口]
  useArray: [是否启用Array消息 true/false]
spring:
  database:
    dataType: [数据库类型, H2/SQLITE/MYSQL]
    mysqlPassword: [数据库密码, 若使用H2/SQLITE可不填]
    mysqlUrl: [数据库连接url, 若使用H2/SQLITE可不填]
    mysqlUser: [数据库用户名, 若使用H2/SQLITE可不填]
  jackson:
    dateformat: yyyy-MM-dd HH:mm:ss
    timezone: Asia/Shanghai
telegram:
  bot:
    proxy:
      host: [代理IP地址，不建议纯域名]
      port: [代理端口]
      secret: [代理密码]
      type: [HTTP/SOCKS/DIRECT]
      username: [代理账号]
    token: [你的bot token]
    username: [你的bot 用户名，随意设置]
```

### 功能

- [ ] HTTP API
- [ ] 反向 HTTP POST
- [x] 正向 WebSocket
- [ ] 反向 WebSocket
- [ ] 连接多个ws地址
- [x] 网页控制台
- [x] 后台操作收发消息
- [x] 代理支持
- [x] Telegram聊天信息区分用户和群组


### 实现

> [!TIP]
> 下列表格中的✅表示已实现,❌表示未实现,✅❓表示已实现但未测试

<details>
<summary>已实现 API</summary>

#### 符合 OneBot 标准的 API

| API                      |      功能       |  实现情况  |
|--------------------------|:-------------:|:------:|
| /send_private_msg        |   [发送私聊消息]    |   ✅    |
| /send_group_msg          |    [发送群消息]    |   ✅    |
| /send_msg                |    [发送消息]     |   ✅    |
| /delete_msg              |    [撤回信息]     |   ✅    |
| /set_group_kick          |    [群组踢人]     |   ✅❓   |
| /set_group_ban           |   [群组单人禁言]    |   ❌    |
| /set_group_whole_ban     |   [群组全员禁言]    |   ❌    |
| /set_group_admin         |   [群组设置管理员]   |   ✅❓   |
| /set_group_card          | [设置群名片（群备注）]  |   ✅❓   |
| /set_group_name          |    [设置群名]     |   ✅❓   |
| /set_group_leave         |    [退出群组]     |   ✅❓   |
| /set_group_special_title |  [设置群组专属头衔]   |   ✅❓   |
| /set_friend_add_request  |   [处理加好友请求]   |   ❌    |
| /set_group_add_request   |  [处理加群请求/邀请]  |   ❌    |
| /get_login_info          |   [获取登录号信息]   |   ✅    |
| /get_stranger_info       |   [获取陌生人信息]   |   ❌    |
| /get_friend_list         |   [获取好友列表]    |   ✅    |
| /get_group_info          |    [获取群信息]    |   ✅    |
| /get_group_list          |    [获取群列表]    |   ✅    |
| /get_group_member_info   |   [获取群成员信息]   |   ✅    |
| /get_group_member_list   |   [获取群成员列表]   |   ✅    |
| /get_group_honor_info    |   [获取群荣誉信息]   |   ❌    |
| /can_send_image          | [检查是否可以发送图片]  |   ❌    |
| /can_send_record         | [检查是否可以发送语音]  |   ❌    |
| /get_version_info        |   [获取版本信息]    |   ✅    |
| /set_restart             |    [重启协议端]    |   ❌    |
| /.handle_quick_operation |  [对事件执行快速操作]  |   ❌    |
| /get_image               |   [获取图片信息]    |   ❌    |
| /get_msg                 |    [获取消息]     |   ❌    |
| /get_status              |    [获取状态]     |   ✅    |


</details>

<details>
<summary>已实现 Event</summary>

#### 符合 OneBot 标准的事件

| 事件类型 |    事件描述     | 实现情况 |
|------|:-----------:|:----:|
| 消息事件 |   [私聊信息]    |  ✅   |
| 消息事件 |    [群消息]    |  ✅   |
| 通知事件 |   [群文件上传]   |  ❌   |
| 通知事件 |  [群管理员变动]   |  ❌   |
| 通知事件 |   [群成员减少]   |  ❌   |
| 通知事件 |   [群成员增加]   |  ❌   |
| 通知事件 |    [群禁言]    |  ❌   |
| 通知事件 |   [好友添加]    |  ❌   |
| 通知事件 |   [群消息撤回]   |  ❌   |
| 通知事件 |  [好友消息撤回]   |  ❌   |
| 请求事件 |   [加好友请求]   |  ❌   |
| 请求事件 |  [加群请求/邀请]  |  ❌   |


</details>

<details>
<summary>已实现 Message</summary>

#### 符合 OneBot 标准的消息

| 消息类型               | 收情况 | 发情况 |
|--------------------|:---:|:---:|
| 纯文本                |  ✅  |  ✅  |
| 图片                 |  ❌  |  ✅  |
| 图文混合(图片描述)         |  ❌  |  ✅  |
| 语音                 |  ❌  |  ❌  |
| 短视频                |  ❌  |  ❌  |
| at某人               |  ✅  |  ✅  |
| 回复                 |  ❌  |  ✅  |
| QQ表情(替代为`Sticker`) |  ❌  |  ❌  | 

</details>
