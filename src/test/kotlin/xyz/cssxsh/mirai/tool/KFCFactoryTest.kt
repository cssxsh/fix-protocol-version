package xyz.cssxsh.mirai.tool

import kotlinx.serialization.*
import net.mamoe.mirai.internal.spi.*
import net.mamoe.mirai.spi.*
import kotlin.test.*

internal class KFCFactoryTest {

    @Test
    fun service() {
        @Suppress("INVISIBLE_MEMBER")
        assertIs<KFCFactory>(EncryptService.Companion.factory)
    }
}