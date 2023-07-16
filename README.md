# 临时性协议修复插件

[![Downloads](https://img.shields.io/github/downloads/cssxsh/fix-protocol-version/total)](https://github.com/cssxsh/fix-protocol-version/releases)

~~此插件仅用于修复 `code=235` 问题~~  
~~使用前请清理掉 `device.json`, 不然仍有可能触发 `code=235`~~  
此插件目前可以用于解决 `ANDROID_PHONE`, `ANDROID_PAD`, `IPAD`, `MACOS` 的 `code=235` 问题  
~~如果遇到 `code=45`，请切换到 `MACOS` 协议~~  
`MACOS` 协议目前也会触发 `code=45`  

~~再次强调，此插件仅用于修复 `code=235` 问题~~  
~~他对 `code=45` 并没有明显效果~~

目前对于 `code=45` 的处理：

在 `1.7.0+` 中加入了 TLV544Provider, 但实际效果有限。

在 `1.9.0+` 中加入了 KFCFactory, 以对接[第三方签名服务](https://mirai.mamoe.net/topic/2373)  

请确保第三方签名服务**可用**！！！  
请确保第三方签名服务**可用**！！！  
请确保第三方签名服务**可用**！！！  

## 第三方签名服务

目前支持的第三方签名服务有  
* [fuqiuluo/unidbg-fetch-qsign](https://github.com/fuqiuluo/unidbg-fetch-qsign)
* [kiliokuara/magic-signer-guide](https://github.com/kiliokuara/magic-signer-guide)

**请确认第三方签名服务 支持的协议版本 和 登录的协议版本 匹配**

下面是配置文件示例，你可以根据实际情况调整  
```json
{
    "8.9.63": {
        "base_url": "http://127.0.0.1:8080",
        "type": "fuqiuluo/unidbg-fetch-qsign",
        "key": "114514"
    },
    "8.9.58": {
        "base_url": "http://127.0.0.1:8888",
        "type": "kiliokuara/magic-signer-guide",
        "server_identity_key": "vivo50",
        "authorization_key": "kfc"
    }
}
```

## JVM 参数

| property                                                | default         |               desc               | 
|:--------------------------------------------------------|:----------------|:--------------------------------:|
| `xyz.cssxsh.mirai.tool.KFCFactory.config`               | KFCFactory.json |   KFCFactory config file path    |
| `xyz.cssxsh.mirai.tool.ViVo50.Session.timeout`          | 60000           |   Session except timeout (ms)    |
| `xyz.cssxsh.mirai.tool.UnidbgFetchQsign.token.interval` | 2400000         | RequestToken interval, 0 is stop |

以上参数在 `1.9.5` 中加入

## Mirai Console 使用方法

下载 `mirai2.jar` 放到 `plugins` ，重启 `Mirai Console` 即可  
~~出现 `Mirai版本低于预期，将升级协议版本` 就表示工作正常，因为这只是临时性修复，之后的Mirai版本会另外再修复~~  
出现 `协议版本检查更新...` 就表示插件开始工作

### 命令

> since 1.6.0

*   `protocol sync <type>` 在线同步协议  
    例如 `protocol sync ANDROID_PAD`

*   `protocol info` 显示当前协议信息

> since 1.8.0

*   `protocol load` 加载本地协议文件  
    例如 `protocol load ANDROID_PHONE`

> since 1.9.6

*   `protocol fetch <type> <version>` 在线获取协议 
    例如 `protocol fetch ANDROID_PAD 8.9.63`

## Mirai Core 使用方法

> since 1.1.0

下载 `mirai2.jar`, 然后作为 lib 引用  
在 `1.9.0+` 中加入了 [async-http-client](https://search.maven.org/artifact/org.asynchttpclient/async-http-client/2.12.3/jar) 作为依赖，请自行补全

然后在代码中调用 `FixProtocolVersion` 的静态方法  
java示例:  
```java
import xyz.cssxsh.mirai.tool.FixProtocolVersion;
import net.mamoe.mirai.utils.BotConfiguration.MiraiProtocol;

import java.util.Map;

public class Example {
    // 升级协议版本
    public static void update() {
        FixProtocolVersion.update();
    }
    // 同步协议版本
    public static void sync() {
        FixProtocolVersion.sync(BotConfiguration.MiraiProtocol.ANDROID_PAD);
    }
    // 获取指定协议版本
    public static void fetch() {
        FixProtocolVersion.fetch(BotConfiguration.MiraiProtocol.ANDROID_PAD, "latest");
        FixProtocolVersion.fetch(BotConfiguration.MiraiProtocol.ANDROID_PAD, "8.9.63");
    }
    // 加载协议版本
    public static void load() {
        FixProtocolVersion.load(BotConfiguration.MiraiProtocol.ANDROID_PAD);
    }
    // 获取协议版本信息 你可以用这个来检查update是否正常工作
    public static Map<BotConfiguration.MiraiProtocol, String> info() {
        return FixProtocolVersion.info();
    }
}
```

## 相关项目

* https://github.com/RomiChan/protocol-versions 协议信息同步来源
* https://github.com/LaoLittle/t544_enc 内置 T544 编码器
* https://github.com/fuqiuluo/unidbg-fetch-qsign
* https://github.com/kiliokuara/magic-signer-guide Docker 镜像, 解决各种 QQ 机器人框架的 sso sign 和 tlv 加密问题。