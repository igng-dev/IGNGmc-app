package net.igng.mcstatus.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IgngFormattersTest {
    @Test
    fun formatsNumbersAndMissingValues() {
        assertEquals("—", IgngFormatters.formatNumber(null as Int?))
        assertEquals("12,345", IgngFormatters.formatNumber(12345))
        assertEquals("20.0", IgngFormatters.formatDecimal(20.0, 1))
        assertEquals("—", IgngFormatters.formatDecimal(null))
        assertEquals("2.50 km", IgngFormatters.formatDistanceCm(250_000L))
        assertEquals("1.50 MB", IgngFormatters.formatBytes(1_572_864L))
    }

    @Test
    fun parsesMinecraftColorCodesCleanly() {
        val spans = McTextParser.parse("§a纯净服 §l第四周目§r 在线")
        assertEquals(3, spans.size)
        assertEquals("纯净服 ", spans[0].text)
        assertEquals(0xFF55FF55L, spans[0].colorHex)
        assertEquals("第四周目", spans[1].text)
        assertTrue(spans[1].bold)
        assertEquals(" 在线", spans[2].text)
        assertEquals("纯净服 第四周目 在线", McTextParser.stripFormatting("§a纯净服 §l第四周目§r 在线"))
    }

    @Test
    fun validatesCustomMedalNamesLikePlugin() {
        assertTrue(CustomMedalValidator.validate("红石工匠") is MedalValidationResult.Valid)
        assertTrue(CustomMedalValidator.validate("") is MedalValidationResult.Invalid)
        assertTrue(CustomMedalValidator.validate("§c彩色违规") is MedalValidationResult.Invalid)
        assertTrue(CustomMedalValidator.validate("<red>标签") is MedalValidationResult.Invalid)
        assertTrue(CustomMedalValidator.validate("一二三四五六七八九十一二三四五六七") is MedalValidationResult.Invalid)
    }
}
