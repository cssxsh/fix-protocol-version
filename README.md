# 临时性协议修复插件

[![Downloads](https://img.shields.io/github/downloads/cssxsh/fix-protocol-version/total)](https://github.com/cssxsh/fix-protocol-version/releases)

此插件仅用于修复 `code=235` 问题  
使用前请清理掉 `device.json`, 不然仍有可能触发 `code=235`  
此插件目前可以用于解决 `ANDROID_PHONE`, `ANDROID_PAD`, `IPAD`, `MACOS` 的 `code=235` 问题  
如果遇到 `code=45`，请切换到 `MACOS` 协议

## Mirai Console 使用方法

下载 `mirai2.jar` 放到 `plugins` ，重启 `Mirai` 即可  
出现 `Mirai版本低于预期，将升级协议版本` 就表示工作正常，因为这只是临时性修复，之后的Mirai版本会另外再修复

## Mirai Core 使用方法

`since 1.1.0`

下载 `mirai2.jar`, 然后作为 lib 引用  

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
    // 获取协议版本信息 你可以用这个来检查update是否正常工作
    public static Map<MiraiProtocol, String> info() {
        return FixProtocolVersion.info();
    }
}
```
