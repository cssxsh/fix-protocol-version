# 临时性协议修复插件

[![Downloads](https://img.shields.io/github/downloads/cssxsh/fix-protocol-version/total)](https://github.com/cssxsh/fix-protocol-version/releases)

目前对于 `code=45` 的处理：

在 `1.7.0+` 中加入了 [TLV544Provider](src/main/kotlin/xyz/cssxsh/mirai/tool/TLV544Provider.kt), 但实际效果有限

在 `1.9.0+` 中加入了 [KFCFactory](src/main/kotlin/xyz/cssxsh/mirai/tool/KFCFactory.kt), 以对接[第三方签名服务](https://mirai.mamoe.net/topic/2373)

本插件不内置签名服务，你需要修改配置，根据版本指定**第三方签名服务**

## 第三方签名服务

> KFCFactory  
> ↓  
> ANDROID_PHONE / ANDROID_PAD  
> ↓  
> KFCFactory.json  
> ↓  
> UnidbgFetchQsign(fuqiuluo/unidbg-fetch-qsign) / ViVo50(kiliokuara/magic-signer-guide)  

KFCFactory 会根据登录协议版本从配置文件(KFCFactory.json)获取签名服务的配置信息

请确保第三方签名服务**可用**！！！  
请确保第三方签名服务**可用**！！！  
请确保第三方签名服务**可用**！！！

目前支持的第三方签名服务有 (你需要准备至少一个可用的服务)  
* [fuqiuluo/unidbg-fetch-qsign](https://github.com/fuqiuluo/unidbg-fetch-qsign)
* [kiliokuara/magic-signer-guide](https://github.com/kiliokuara/magic-signer-guide)

**请确认第三方签名服务 支持的协议版本 和 登录的协议版本 匹配**

下面是配置文件 KFCFactory.json 示例，你可以根据实际情况调整  
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

修改配置文件 KFCFactory.json 无需重启 Mirai

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

import java.io.FileNotFoundException;
import java.util.Map;

public class Example {
    // 获取指定协议版本
    public static void fetch() {
        // 获取最新版本协议
        FixProtocolVersion.fetch(BotConfiguration.MiraiProtocol.ANDROID_PAD, "latest");
        // 获取 8.9.63 版本协议
        FixProtocolVersion.fetch(BotConfiguration.MiraiProtocol.ANDROID_PHONE, "8.9.63");
    }

    // 从本地文件加载协议版本
    public static void load() {
        try {
            FixProtocolVersion.load(BotConfiguration.MiraiProtocol.ANDROID_PAD);
        } catch (FileNotFoundException ignored) {
            FixProtocolVersion.fetch(BotConfiguration.MiraiProtocol.ANDROID_PAD, "8.9.63");
        }
    }

    // 获取协议版本信息
    public static Map<BotConfiguration.MiraiProtocol, String> info() {
        return FixProtocolVersion.info();
    }
}
```

> since 1.9.0

关于 [KFCFactory](src/main/kotlin/xyz/cssxsh/mirai/tool/KFCFactory.kt), 正常来说  
它会根据 [SPI](https://en.wikipedia.org/wiki/Service_provider_interface) 机制被自动加载  
如果项目结构特殊，也可用使用 `KFCFactory.install()` 手动注册

## 相关项目

* https://github.com/RomiChan/protocol-versions 协议信息同步来源
* https://github.com/LaoLittle/t544_enc 内置 T544 编码器
* https://github.com/fuqiuluo/unidbg-fetch-qsign
* https://github.com/kiliokuara/magic-signer-guide Docker 镜像, 解决各种 QQ 机器人框架的 sso sign 和 tlv 加密问题。