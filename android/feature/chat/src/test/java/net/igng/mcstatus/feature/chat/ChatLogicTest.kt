package net.igng.mcstatus.feature.chat

import java.time.ZoneOffset
import net.igng.mcstatus.core.data.ChatAttachmentDto
import net.igng.mcstatus.core.data.ChatGroupSourceDto
import net.igng.mcstatus.core.data.ChatLastMessageDto
import net.igng.mcstatus.core.data.ChatMessageDto
import net.igng.mcstatus.core.data.ChatSenderBadgeDto
import net.igng.mcstatus.core.data.ChatSenderProfileDto
import net.igng.mcstatus.core.data.ChatServerSourceDto
import net.igng.mcstatus.core.data.ChatSourcesResponse
import net.igng.mcstatus.core.data.ChatStructurePartDto
import net.igng.mcstatus.core.model.SavedAccount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatLogicTest {

    @Test
    fun unifiedSessionsSortByLatestAcrossAllPlatformsAndFilterByTabs() {
        val sources = ChatSourcesResponse(
            servers = listOf(
                ChatServerSourceDto(
                    serverId = "srv_3",
                    numericServerId = 3,
                    name = "生存服 S6",
                    lastMessageAt = "2026-11-22T12:10:00Z",
                    lastPreview = "§a主城修好了",
                    lastMessage = ChatLastMessageDto(sender = "Steve", content = "§a主城修好了", createdAt = "2026-11-22T12:10:00Z"),
                ),
            ),
            qqGroups = listOf(
                ChatGroupSourceDto(
                    id = "885647215",
                    name = "IGNG 1群",
                    platform = "qq",
                    lastMessageAt = "2026-11-22T12:00:00Z",
                    lastMessage = ChatLastMessageDto(sender = "Alice", content = "晚上打龙", createdAt = "2026-11-22T12:00:00Z"),
                ),
                ChatGroupSourceDto(
                    id = "700000000000101",
                    name = "IGNG 微信群",
                    platform = "wechat",
                    lastMessageAt = "2026-11-22T12:20:00Z",
                    lastMessage = ChatLastMessageDto(sender = "云萤", content = "收到啦", createdAt = "2026-11-22T12:20:00Z"),
                ),
            ),
            privateChats = listOf(
                ChatGroupSourceDto(
                    id = "-10001",
                    name = "Alice",
                    platform = "qq",
                    isPrivate = true,
                    lastMessageAt = "2026-11-22T11:30:00Z",
                ),
            ),
            authenticated = true,
        )

        val unified = buildUnifiedSessions(sources)
        assertEquals(4, unified.size)
        // Newest first: WeChat (12:20) -> MC (12:10) -> QQ Group (12:00) -> QQ Private (11:30)
        assertEquals(ChatSessionKind.WECHAT_GROUP, unified[0].kind)
        assertEquals(ChatSessionKind.MC_SERVER, unified[1].kind)
        assertEquals("Steve: 主城修好了", unified[1].formattedPreview)
        assertEquals(ChatSessionKind.QQ_GROUP, unified[2].kind)
        assertEquals(ChatSessionKind.QQ_PRIVATE, unified[3].kind)
        // Synthetic WeChat IDs are never exposed as displayNumber
        assertNull(unified[0].displayNumber)
        assertEquals("885647215", unified[2].displayNumber)

        assertEquals(4, filterSessions(unified, ChatSessionTab.ALL, "").size)
        assertEquals(2, filterSessions(unified, ChatSessionTab.QQ, "").size)
        assertEquals(1, filterSessions(unified, ChatSessionTab.WECHAT, "").size)
        assertEquals(1, filterSessions(unified, ChatSessionTab.MC, "").size)
        assertEquals(1, filterSessions(unified, ChatSessionTab.PRIVATE, "").size)
    }

    @Test
    fun timelineGroupsConsecutiveMessagesWithinFiveMinutesAndEmitsPatNotice() {
        val account = SavedAccount(userId = 1024, username = "alice", nickname = "Alice", token = "tok")
        val profiles = mapOf(
            "qq:10001" to ChatSenderProfileDto(
                bound = true,
                igngId = 1024,
                igngNickname = "Alice",
                badges = listOf(ChatSenderBadgeDto("pro", "云萤 Pro", "pro")),
            ),
        )
        val messages = listOf(
            ChatMessageDto(
                id = "qq-1",
                source = "qq",
                platform = "qq",
                groupId = "885647215",
                senderId = "10001",
                senderName = "Alice",
                content = "第一条",
                createdAt = "2026-11-22T12:00:00Z",
            ),
            ChatMessageDto(
                id = "qq-2",
                source = "qq",
                platform = "qq",
                groupId = "885647215",
                senderId = "10001",
                senderName = "Alice",
                content = "第二条",
                createdAt = "2026-11-22T12:02:00Z",
            ),
            ChatMessageDto(
                id = "qq-3",
                source = "qq",
                platform = "wechat",
                groupId = "700000000000101",
                senderId = "700000000000202",
                senderName = "Bob",
                content = "Bob 拍了拍 Alice",
                messageStructure = listOf(
                    ChatStructurePartDto(type = "card", cardType = "pat", title = "Bob 拍了拍 Alice"),
                ),
                createdAt = "2026-11-22T12:03:00Z",
            ),
            ChatMessageDto(
                id = "qq-4",
                source = "qq",
                platform = "qq",
                groupId = "885647215",
                senderId = "10001",
                senderName = "Alice",
                content = "十分钟后",
                createdAt = "2026-11-22T12:15:00Z",
            ),
        )

        val timeline = buildChatTimeline(messages, profiles, account, emptySet(), ZoneOffset.UTC)
        assertTrue(timeline[0] is ChatTimelineItem.DayHeader)
        val firstGroup = timeline[1] as ChatTimelineItem.MessageGroup
        assertEquals(2, firstGroup.messages.size)
        assertTrue(firstGroup.sender.isMine)
        assertTrue(timeline[2] is ChatTimelineItem.SystemNotice)
        assertEquals("Bob 拍了拍 Alice", (timeline[2] as ChatTimelineItem.SystemNotice).text)
        assertTrue(timeline[3] is ChatTimelineItem.TimeDivider)
        val secondGroup = timeline[4] as ChatTimelineItem.MessageGroup
        assertEquals(1, secondGroup.messages.size)
    }

    @Test
    fun extractMessageDisplayStripsPlaceholdersAndKeepsWeChatFallback() {
        val qqWithImage = ChatMessageDto(
            id = "qq-10",
            source = "qq",
            platform = "qq",
            content = "[图片] 看看这个 [文件: guide.pdf]",
            attachments = listOf(
                ChatAttachmentDto(index = 0, kind = "image", url = "/api/v1/chat/media/qq-10/0"),
                ChatAttachmentDto(index = 1, kind = "file", name = "guide.pdf", url = "/api/v1/chat/media/qq-10/1"),
            ),
        )
        val parsedQq = extractMessageDisplay(qqWithImage)
        assertEquals("看看这个", parsedQq.text)
        assertEquals(1, parsedQq.images.size)
        assertEquals(1, parsedQq.files.size)
        assertEquals(0, parsedQq.missingImages)

        val wechatNoMedia = ChatMessageDto(
            id = "qq-11",
            source = "qq",
            platform = "wechat",
            content = "[图片] 微信原图暂未落盘",
            attachments = emptyList(),
        )
        val parsedWx = extractMessageDisplay(wechatNoMedia)
        assertEquals("[图片] 微信原图暂未落盘", parsedWx.text)
        assertEquals(0, parsedWx.missingImages)
    }

    @Test
    fun anonymousMaskedQqNeverQueriesQlogoAndSafeUrlRejectsJavascript() {
        val maskedMsg = ChatMessageDto(
            id = "qq-20",
            source = "qq",
            platform = "qq",
            senderId = "12***89",
            senderName = "QQ 12***89",
            content = "hi",
        )
        val sender = resolveChatSender(maskedMsg, emptyMap(), activeAccount = null)
        assertNull(sender.avatarUrl)
        assertFalse(sender.isMine)

        assertNull(ChatPlatformRules.safeExternalUrl("javascript:alert(1)"))
        assertEquals("https://www.igng.net", ChatPlatformRules.safeExternalUrl("https://www.igng.net"))
        assertEquals("qq-90001", ChatPlatformRules.parseLocateMessageId("#90001", "qq"))
        assertEquals("42", ChatPlatformRules.parseLocateMessageId("#42", "server"))
    }

    @Test
    fun stripsMd5StickersEnglishPlaceholdersAndHumanizesHashedNames() {
        // WeChat image with md5 token and attachment strips raw English md5 text cleanly
        val wxWithImg = ChatMessageDto(
            id = "qq-30",
            source = "qq",
            platform = "wechat",
            content = "[图片 md5=61af10c689121897d1226702df40e766] 附言",
            attachments = listOf(
                ChatAttachmentDto(index = 0, kind = "image", url = "/api/v1/chat/media/qq-30/0"),
            ),
        )
        val parsedWxWithImg = extractMessageDisplay(wxWithImg)
        assertEquals("附言", parsedWxWithImg.text)
        assertEquals(1, parsedWxWithImg.images.size)
        assertEquals(0, parsedWxWithImg.missingImages)

        // WeChat image without extracted media humanizes md5 token into clean Chinese `[图片]`
        val wxMissingImg = ChatMessageDto(
            id = "qq-31",
            source = "qq",
            platform = "wechat",
            content = "[图片 md5=61af10c689121897d1226702df40e766]",
            attachments = emptyList(),
        )
        val parsedWxMissing = extractMessageDisplay(wxMissingImg)
        assertEquals("[图片]", parsedWxMissing.text)
        assertFalse(parsedWxMissing.text.contains("md5"))

        // English segment fallbacks and sticker/video/audio placeholders
        val engMedia = ChatMessageDto(
            id = "qq-32",
            source = "qq",
            platform = "qq",
            content = "[贴纸: 开心] [image] [video] [record] 说明",
            attachments = listOf(
                ChatAttachmentDto(index = 0, kind = "sticker", url = "/api/v1/chat/media/qq-32/0"),
                ChatAttachmentDto(index = 1, kind = "file", mimeType = "image/webp", url = "/api/v1/chat/media/qq-32/1"),
                ChatAttachmentDto(index = 2, kind = "video", url = "/api/v1/chat/media/qq-32/2"),
                ChatAttachmentDto(index = 3, kind = "voice", url = "/api/v1/chat/media/qq-32/3"),
            ),
        )
        val parsedEng = extractMessageDisplay(engMedia)
        assertEquals("说明", parsedEng.text)
        assertEquals(2, parsedEng.images.size)
        assertEquals(1, parsedEng.videos.size)
        assertEquals(1, parsedEng.audios.size)
        assertEquals(0, parsedEng.files.size)

        // Machine-generated hash filenames become friendly Chinese titles while real filenames stay
        assertEquals(
            "聊天图片",
            friendlyMediaDisplayName(
                "-2105710147_C56BF959425841EA94464B9E3A3A7660_348b7b5badc340d7a72d78ed81908f6a.webp",
                "聊天图片",
            ),
        )
        assertEquals("聊天图片", friendlyMediaDisplayName("face_277.webp", "聊天图片"))
        assertEquals("服务器必装材质.zip", friendlyMediaDisplayName("服务器必装材质.zip", "聊天文件"))
    }

    @Test
    fun resolvesMcFirstPartyAvatarProxyAndUpgradesHttpQlogo() {
        val mcMsg = ChatMessageDto(
            id = "mc-1",
            source = "server",
            platform = "mc",
            senderName = "Tianning",
            content = "hi",
        )
        val mcSender = resolveChatSender(mcMsg, emptyMap(), activeAccount = null)
        assertEquals("/api/v1/mc/avatars/Tianning?size=64", mcSender.avatarUrl)
        assertTrue(mcSender.fallbackAvatarUrls.any { it.contains("minotar.net") })
        assertTrue(mcSender.fallbackAvatarUrls.any { it.contains("mc-heads.net") })

        val wxMsg = ChatMessageDto(
            id = "qq-40",
            source = "qq",
            platform = "wechat",
            senderId = "700000000000202",
            senderName = "Bob",
            content = "hello",
        )
        val wxProfiles = mapOf(
            "wechat:700000000000202" to ChatSenderProfileDto(
                avatarUrl = "http://wx.qlogo.cn/mmhead/ver_1/abc/0",
            ),
        )
        val wxSender = resolveChatSender(wxMsg, wxProfiles, activeAccount = null)
        assertEquals("https://wx.qlogo.cn/mmhead/ver_1/abc/0", wxSender.avatarUrl)

        val sources = ChatSourcesResponse(
            qqGroups = listOf(
                ChatGroupSourceDto(id = "885647215", name = "IGNGmc1群", platform = "qq"),
            ),
        )
        val sessions = buildUnifiedSessions(sources)
        assertEquals("https://p.qlogo.cn/gh/885647215/885647215/100", sessions.first().avatarUrl)
    }
}
