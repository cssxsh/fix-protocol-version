package xyz.cssxsh.mirai.tool

import io.ktor.client.engine.*
import java.net.*

public object KtorProxyUtil {
    public fun getProxy(useSystemProxy: Boolean): ProxyConfig? =
        getProxyFromJvmArgs() ?: getProxyFromEnv() ?: if (useSystemProxy) getSystemProxy() else null

    private fun getProxyFromJvmArgs(): ProxyConfig? {
        val proxyHost = System.getProperty("http.proxyHost").orEmpty()
        val proxyPort = System.getProperty("http.proxyPort").orEmpty().toIntOrNull()
        if (proxyHost.isEmpty() || proxyPort == null) return null

        return ProxyBuilder.http("http://$proxyHost:$proxyPort")
    }

    private fun getProxyFromEnv(): ProxyConfig? {
        val env = System.getenv("HTTP_PROXY").orEmpty()
        return if (env.isNotEmpty()) ProxyBuilder.http(env) else null
    }

    private fun getSystemProxy(): ProxyConfig? {
        System.setProperty("java.net.useSystemProxies", "true")
        val proxyList = ProxySelector.getDefault().select(URI.create("http://google.com"))
        return proxyList.firstOrNull()?.let { ProxyConfig(it.type(), it.address()) }
    }
}