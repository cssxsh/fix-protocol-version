package xyz.cssxsh.mirai.tool

import net.mamoe.mirai.internal.spi.*
import net.mamoe.mirai.spi.*
import kotlin.test.*

internal class KFCFactoryTest {
    @Test
    @Suppress("INVISIBLE_MEMBER")
    fun service() {
        assertIs<KFCFactory>(EncryptService.Companion.factory)
    }
}