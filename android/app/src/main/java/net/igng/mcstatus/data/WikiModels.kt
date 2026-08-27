package net.igng.mcstatus.data

import kotlinx.serialization.Serializable

@Serializable
data class WikiCatalogResponse(
    val schemaVersion: Int = 1,
    val generatedAt: String? = null,
    val knowledgeBase: WikiKnowledgeBaseSummary = WikiKnowledgeBaseSummary(),
    val nodes: List<WikiCatalogNode> = emptyList(),
)

@Serializable
data class WikiKnowledgeBaseSummary(
    val id: Int = 0,
    val slug: String = "",
    val name: String = "",
    val description: String? = null,
    val rootPage: WikiPageSummary? = null,
)

@Serializable
data class WikiCatalogNode(
    val id: Int = 0,
    val parentId: Int? = null,
    val nodeType: String = "PAGE",
    val slug: String = "",
    val title: String = "",
    val sortOrder: Int = 100,
    val updatedAt: String? = null,
    val path: String = "",
    val page: WikiPageSummary? = null,
    val children: List<WikiCatalogNode> = emptyList(),
)

@Serializable
data class WikiPageSummary(
    val id: Int = 0,
    val slug: String = "",
    val title: String = "",
    val format: String = "markdown",
    val currentVersion: Int = 1,
    val updatedAt: String? = null,
    val path: String = "",
)

@Serializable
data class WikiPageResponse(
    val schemaVersion: Int = 1,
    val knowledgeBase: WikiKnowledgeBaseRef = WikiKnowledgeBaseRef(),
    val page: WikiPage = WikiPage(),
)

@Serializable
data class WikiKnowledgeBaseRef(
    val id: Int = 0,
    val slug: String = "",
    val name: String = "",
)

@Serializable
data class WikiPage(
    val id: Int = 0,
    val slug: String = "",
    val title: String = "",
    val content: String = "",
    val format: String = "markdown",
    val currentVersion: Int = 1,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val path: String = "",
)
