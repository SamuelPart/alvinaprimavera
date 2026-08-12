package com.flippy.walkietalkie.data.model

enum class VoiceFilter(
    val id: String,
    val displayName: String,
    val description: String,
    val iconName: String
) {
    RADIO("radio", "Radio Walkie", "Banda 300Hz-3.4kHz con distorsión analógica", "Radio"),
    CLEAN("normal", "Voz Limpia", "Alta fidelidad sin efectos", "Mic"),
    ROBOT("robot", "Cyborg Robot", "Modulación en anillo cibernética", "Bot"),
    DEEP("deep", "Voz Grave", "Realce de graves y tono bajo", "Shield"),
    HELIUM("baby", "Helio / Agudo", "Tono agudo y dinámico", "Sparkles"),
    MEGAPHONE("megaphone", "Megáfono", "Altavoz táctico con eco de campo", "Megaphone"),
    ALIEN("alien", "Alien Espacial", "Oscilador FM extraterrestre", "Orbit")
}
