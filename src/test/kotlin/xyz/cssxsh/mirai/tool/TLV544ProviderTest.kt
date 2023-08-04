package xyz.cssxsh.mirai.tool

import org.junit.jupiter.api.*

internal class TLV544ProviderTest {

    @Test
    fun sign() {
        intArrayOf(170, 57, 120, 244, 31, 217, 111, 249, 145, 74, 102, 158, 24, 100, 116, 199)
            .joinToString(" ") { it.toString(16).uppercase() }
            .let { println(it) }
        TLV544Provider.native()
        TLV544Provider.sign(payload = byteArrayOf())
            .joinToString(" ") { it.toString(16).uppercase() }
            .let { println(it) }
    }
}