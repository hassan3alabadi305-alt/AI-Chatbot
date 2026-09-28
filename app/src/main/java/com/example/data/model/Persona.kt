package com.example.data.model

data class Persona(
    val id: String,
    val name: String,
    val nameAr: String,
    val systemPrompt: String,
    val iconName: String,
    val description: String,
    val descriptionAr: String,
    val colorHex: Long
) {
    companion object {
        val DEFAULT_PERSONAS = listOf(
            Persona(
                id = "general",
                name = "General Assistant",
                nameAr = "المساعد الذكي العام",
                systemPrompt = "You are a helpful, friendly, and highly capable AI assistant. Answer clearly and concisely. When asked for YouTube videos, Google/Chrome links, or general websites, always provide proper clickable Markdown links (e.g., [رابط على يوتيوب](https://www.youtube.com/results?search_query=...) or [بحث على جوجل](https://www.google.com/search?q=...)).",
                iconName = "SmartToy",
                description = "Balanced assistant for everyday queries and general knowledge",
                descriptionAr = "مساعد متوازن للأسئلة اليومية والمعلومات العامة",
                colorHex = 0xFF6366F1
            ),
            Persona(
                id = "code",
                name = "Code Architect",
                nameAr = "خبير البرمجة",
                systemPrompt = "You are an expert software engineer. Provide clean, production-grade code examples with syntax formatting and step-by-step explanations.",
                iconName = "Code",
                description = "Specialized in Kotlin, Python, web development & algorithms",
                descriptionAr = "متخصص في برمجة كود نظيف وتطوير البرمجيات",
                colorHex = 0xFF10B981
            ),
            Persona(
                id = "creative",
                name = "Creative Writer",
                nameAr = "الكاتب المبدع",
                systemPrompt = "You are a creative author and poet. Craft inspiring stories, engaging posts, and expressive prose.",
                iconName = "AutoAwesome",
                description = "For poetry, storytelling, article drafting and creative ideas",
                descriptionAr = "لكتابة قصص ومقالات وأفكار إبداعية",
                colorHex = 0xFF8B5CF6
            ),
            Persona(
                id = "tutor",
                name = "Bilingual Tutor",
                nameAr = "معلم اللغات",
                systemPrompt = "You are a friendly language teacher fluent in Arabic and English. Help translate, practice conversation, and explain complex grammar simply.",
                iconName = "Translate",
                description = "Master languages, translate text, and practice conversation",
                descriptionAr = "لتعلم اللغات الترجمة وتوضيح القواعد",
                colorHex = 0xFF06B6D4
            ),
            Persona(
                id = "analyst",
                name = "Logical Analyst",
                nameAr = "المحلل المنطقي",
                systemPrompt = "You are a strategic thinker. Break down problem statements logically into step-by-step bullet points, weighing pros and cons.",
                iconName = "Psychology",
                description = "For decision making, deep analysis, and structured research",
                descriptionAr = "للتحليل العميق واتخاذ القرارات الهيكلية",
                colorHex = 0xFFF59E0B
            )
        )

        val DEFAULT_PERSONA = DEFAULT_PERSONAS[0]
    }
}
