# 临时性协议修复插件

[![Downloads](https://img.shields.io/github/downloads/cssxsh/fix-protocol-version/total)](https://github.com/cssxsh/fix-protocol-version/releases)

欢迎投食 `https://github.com/cssxsh#sponsor`

## Mirai Console 使用方法

下载 `mirai2.jar` 放到 `plugins` ，重启 `Mirai` 即可
出现 `Mirai版本低于预期，将升级协议版本` 就表示工作正常

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
