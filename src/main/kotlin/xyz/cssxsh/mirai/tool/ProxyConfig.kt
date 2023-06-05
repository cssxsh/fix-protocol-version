package xyz.cssxsh.mirai.tool

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value

/**
 * @projectName: Seiko
 * @package: com.kagg886.seiko.dic.mirai_console
 * @className: SeikoPluginConfig
 * @author: kagg886
 * @description: 插件的配置文件存储处
 * @date: 2023/1/27 18:22
 * @version: 1.0
 */
public object ProxyConfig : AutoSavePluginConfig("proxy") {
    public val urlPattern: String by value("https://ghproxy.com/%s") //代理网站设置，%s为占位符
}