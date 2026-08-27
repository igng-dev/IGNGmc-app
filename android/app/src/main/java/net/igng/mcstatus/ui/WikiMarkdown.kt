@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package net.igng.mcstatus.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

sealed interface WikiBlock {
    data class Heading(val level: Int, val text: String) : WikiBlock
    data class Paragraph(val text: String) : WikiBlock
    data class ListBlock(val ordered: Boolean, val items: List<String>) : WikiBlock
    data class Quote(val text: String) : WikiBlock
    data class CodeBlock(val language: String, val code: String) : WikiBlock
    data class Table(val headers: List<String>, val rows: List<List<String>>) : WikiBlock
    data object Divider : WikiBlock
}

fun parseWikiMarkdown(markdown: String): List<WikiBlock> {
    val lines = markdown.replace("\r", "").lines()
    val blocks = mutableListOf<WikiBlock>()
    var index = 0

    while (index < lines.size) {
        val line = lines[index]
        val trimmed = line.trim()
        if (trimmed.isBlank()) {
            index++
            continue
        }

        val fence = fenceFor(trimmed)
        if (fence != null) {
            val language = trimmed.drop(3).trim()
            val codeLines = mutableListOf<String>()
            index++
            while (index < lines.size && !lines[index].trim().startsWith(fence)) {
                codeLines += lines[index]
                index++
            }
            if (index < lines.size) index++
            blocks += WikiBlock.CodeBlock(language, codeLines.joinToString("\n"))
            continue
        }

        headingFor(trimmed)?.let { heading ->
            blocks += heading
            index++
            continue
        }

        if (isDivider(trimmed)) {
            blocks += WikiBlock.Divider
            index++
            continue
        }

        if (line.contains('|') && index + 1 < lines.size && isTableDivider(lines[index + 1])) {
            val headers = splitTableRow(line)
            val rows = mutableListOf<List<String>>()
            index += 2
            while (index < lines.size && lines[index].isNotBlank() && lines[index].contains('|')) {
                rows += splitTableRow(lines[index])
                index++
            }
            if (headers.isNotEmpty()) {
                blocks += WikiBlock.Table(headers, rows)
                continue
            }
            index -= 2
        }

        if (trimmed.startsWith(">")) {
            val quoteLines = mutableListOf<String>()
            while (index < lines.size && lines[index].trim().startsWith(">")) {
                quoteLines += lines[index].trim().removePrefix(">").trimStart()
                index++
            }
            blocks += WikiBlock.Quote(quoteLines.joinToString("\n"))
            continue
        }

        val firstListItem = listItemFor(trimmed)
        if (firstListItem != null) {
            val items = mutableListOf<String>()
            val ordered = firstListItem.first
            while (index < lines.size) {
                val item = listItemFor(lines[index].trim()) ?: break
                if (item.first != ordered) break
                items += item.second
                index++
            }
            blocks += WikiBlock.ListBlock(ordered, items)
            continue
        }

        val paragraphLines = mutableListOf(line)
        index++
        while (index < lines.size && lines[index].isNotBlank() && !startsBlock(lines, index)) {
            paragraphLines += lines[index]
            index++
        }
        blocks += WikiBlock.Paragraph(paragraphLines.joinToString("\n"))
    }

    return blocks
}

@Composable
fun WikiBlockView(
    block: WikiBlock,
    onLinkClick: (String) -> Unit,
    onCopyCode: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (block) {
        is WikiBlock.Heading -> {
            val style = when (block.level) {
                1 -> MaterialTheme.typography.headlineSmall
                2 -> MaterialTheme.typography.titleLarge
                3 -> MaterialTheme.typography.titleMedium
                else -> MaterialTheme.typography.titleSmall
            }
            WikiInlineText(
                text = block.text,
                onLinkClick = onLinkClick,
                style = style.copy(fontWeight = FontWeight.Bold),
                modifier = modifier.padding(top = if (block.level <= 2) 12.dp else 4.dp),
            )
        }

        is WikiBlock.Paragraph -> WikiInlineText(
            text = block.text,
            onLinkClick = onLinkClick,
            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 27.sp),
            modifier = modifier,
        )

        is WikiBlock.ListBlock -> Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            block.items.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = if (block.ordered) "${index + 1}." else "•",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    WikiInlineText(
                        text = item,
                        onLinkClick = onLinkClick,
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        is WikiBlock.Quote -> Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(44.dp)
                    .padding(vertical = 2.dp),
            ) {
                Surface(color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth().height(40.dp)) {}
            }
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.weight(1f),
            ) {
                WikiInlineText(
                    text = block.text,
                    onLinkClick = onLinkClick,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 23.sp),
                    modifier = Modifier.padding(12.dp),
                )
            }
        }

        is WikiBlock.CodeBlock -> CodeBlockView(block, onCopyCode, modifier)
        is WikiBlock.Table -> TableBlockView(block, onLinkClick, modifier)
        WikiBlock.Divider -> HorizontalDivider(modifier = modifier.padding(vertical = 8.dp))
    }
}

@Composable
private fun CodeBlockView(
    block: WikiBlock.CodeBlock,
    onCopyCode: (String) -> Unit,
    modifier: Modifier,
) {
    val scrollState = rememberScrollState()
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2430)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = block.language.ifBlank { "命令" },
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFB9C6DA),
            )
            IconButton(onClick = { onCopyCode(block.code) }) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = "复制代码", tint = Color(0xFFB9C6DA))
            }
        }
        SelectionContainer {
            Text(
                text = block.code,
                modifier = Modifier.horizontalScroll(scrollState).padding(12.dp),
                color = Color(0xFFE7EDF7),
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                lineHeight = 21.sp,
            )
        }
    }
}

@Composable
private fun TableBlockView(
    block: WikiBlock.Table,
    onLinkClick: (String) -> Unit,
    modifier: Modifier,
) {
    val columns = max(block.headers.size, block.rows.maxOfOrNull { it.size } ?: 0)
    if (columns == 0) return
    val scrollState = rememberScrollState()
    Column(modifier = modifier.horizontalScroll(scrollState)) {
        TableRow(block.headers, columns, onLinkClick, isHeader = true)
        block.rows.forEach { row -> TableRow(row, columns, onLinkClick, isHeader = false) }
    }
}

@Composable
private fun TableRow(
    cells: List<String>,
    columns: Int,
    onLinkClick: (String) -> Unit,
    isHeader: Boolean,
) {
    Row {
        repeat(columns) { index ->
            Surface(
                color = if (isHeader) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    MaterialTheme.colorScheme.surface
                },
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.widthIn(min = 128.dp).width(150.dp),
            ) {
                WikiInlineText(
                    text = cells.getOrNull(index).orEmpty(),
                    onLinkClick = onLinkClick,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                        lineHeight = 20.sp,
                    ),
                    modifier = Modifier.padding(10.dp),
                )
            }
        }
    }
}

@Composable
private fun WikiInlineText(
    text: String,
    onLinkClick: (String) -> Unit,
    style: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier,
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val annotated = remember(text, linkColor) {
        buildInlineAnnotatedString(text, linkColor)
    }
    var layoutResult by remember(annotated) { mutableStateOf<TextLayoutResult?>(null) }

    BasicText(
        text = annotated,
        style = style,
        onTextLayout = { layoutResult = it },
        modifier = modifier.pointerInput(annotated, layoutResult) {
            detectTapGestures { position ->
                val offset = layoutResult?.getOffsetForPosition(position) ?: return@detectTapGestures
                annotated.getStringAnnotations("URL", offset, offset)
                    .firstOrNull()
                    ?.item
                    ?.let(onLinkClick)
            }
        },
    )
}

private fun buildInlineAnnotatedString(text: String, linkColor: Color): AnnotatedString =
    androidx.compose.ui.text.buildAnnotatedString {
        var index = 0
        while (index < text.length) {
            when {
                text.startsWith("![", index) -> {
                    val labelEnd = text.indexOf("](", index + 2)
                    val urlEnd = if (labelEnd >= 0) text.indexOf(')', labelEnd + 2) else -1
                    if (labelEnd >= 0 && urlEnd > labelEnd) {
                        append(text.substring(index + 2, labelEnd))
                        index = urlEnd + 1
                    } else {
                        append(text[index])
                        index++
                    }
                }

                text[index] == '[' -> {
                    val labelEnd = text.indexOf("](", index + 1)
                    val urlEnd = if (labelEnd >= 0) text.indexOf(')', labelEnd + 2) else -1
                    if (labelEnd >= 0 && urlEnd > labelEnd) {
                        val label = text.substring(index + 1, labelEnd)
                        val url = text.substring(labelEnd + 2, urlEnd)
                        pushStringAnnotation("URL", url)
                        withStyle(
                            SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                        ) { append(label) }
                        pop()
                        index = urlEnd + 1
                    } else {
                        append(text[index])
                        index++
                    }
                }

                text.startsWith("**", index) -> {
                    val end = text.indexOf("**", index + 2)
                    if (end > index + 2) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(text.substring(index + 2, end)) }
                        index = end + 2
                    } else {
                        append(text[index])
                        index++
                    }
                }

                text.startsWith("~~", index) -> {
                    val end = text.indexOf("~~", index + 2)
                    if (end > index + 2) {
                        withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { append(text.substring(index + 2, end)) }
                        index = end + 2
                    } else {
                        append(text[index])
                        index++
                    }
                }

                text[index] == '`' -> {
                    val end = text.indexOf('`', index + 1)
                    if (end > index + 1) {
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = linkColor.copy(alpha = 0.12f),
                                color = linkColor,
                            ),
                        ) { append(text.substring(index + 1, end)) }
                        index = end + 1
                    } else {
                        append(text[index])
                        index++
                    }
                }

                else -> {
                    append(text[index])
                    index++
                }
            }
        }
    }

private fun headingFor(line: String): WikiBlock.Heading? {
    val match = Regex("^(#{1,6})\\s+(.+?)\\s*#*$").matchEntire(line) ?: return null
    return WikiBlock.Heading(match.groupValues[1].length, match.groupValues[2].trim())
}

private fun fenceFor(line: String): String? = when {
    line.startsWith("```") -> "```"
    line.startsWith("~~~") -> "~~~"
    else -> null
}

private fun listItemFor(line: String): Pair<Boolean, String>? {
    Regex("^[-*+]\\s+(.+)$").matchEntire(line)?.let { return false to it.groupValues[1] }
    Regex("^\\d+[.)]\\s+(.+)$").matchEntire(line)?.let { return true to it.groupValues[1] }
    return null
}

private fun isTableDivider(line: String): Boolean {
    val cells = splitTableRow(line)
    return cells.isNotEmpty() && cells.all { it.trim().matches(Regex("^:?-{3,}:?$")) }
}

private fun splitTableRow(line: String): List<String> = line.trim()
    .removePrefix("|")
    .removeSuffix("|")
    .split('|')
    .map { it.trim() }

private fun isDivider(line: String): Boolean {
    val normalized = line.replace(" ", "")
    return normalized.length >= 3 && normalized.all { it == '-' || it == '*' || it == '_' }
}

private fun startsBlock(lines: List<String>, index: Int): Boolean {
    val line = lines[index].trim()
    return fenceFor(line) != null ||
        headingFor(line) != null ||
        isDivider(line) ||
        line.startsWith(">") ||
        listItemFor(line) != null ||
        (line.contains('|') && index + 1 < lines.size && isTableDivider(lines[index + 1]))
}
