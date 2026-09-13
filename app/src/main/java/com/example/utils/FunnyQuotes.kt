package com.example.utils

import kotlin.random.Random

object FunnyQuotes {
    private val blockedQuotes = listOf(
        "Bro, the grass is literally outside. 😭",
        "POV: your screen time just exposed you.",
        "Touch grass. The Wi-Fi can survive without you.",
        "Respectfully, log off. 💀",
        "Bro is fighting for his life in the feed.",
        "Your phone said: enough, bestie.",
        "The algorithm is not your therapist. Go outside.",
        "Chronically online detected. Nature DLC required.",
        "You have beef with daylight or what?",
        "Go collect some sunlight XP.",
        "The grass has been waiting. Don't be shy.",
        "Main character? More like main screen-time offender.",
        "Bro touched every app except grass.",
        "Your daily limit said 'nah bro'.",
        "Digital goblin mode has been temporarily suspended.",
        "The feed isn't going anywhere. Your attention span might.",
        "No cap: you need vitamin G. G for grass.",
        "This is your sign to go outside fr.",
        "Imagine losing to a plant. Skill issue. 🌱",
        "Unplug. Hydrate. Touch grass. We believe in you."
    )

    private val cheatQuotes = listOf(
        "Nice try 💀 That's a screen, not outside.",
        "Bro really tried to catfish me with grass.",
        "Artificial grass? Nah, we're not accepting the Temu version.",
        "That's a tree, bestie. The mission says GRASS.",
        "Screenshot grass is crazy work.",
        "The grass needs to be real. No CGI arc today.",
        "Pointing at grass isn't touching grass, bro 😭",
        "Hovering detected. Commit to the touch.",
        "Nice angle. Still not grass contact.",
        "Indoor carpet is not beating the allegations.",
        "Bro brought fake grass to a real-grass challenge.",
        "The camera is live. Your grass needs to be too.",
        "Nice attempt. The chlorophyll committee rejected it.",
        "You cannot outsmart the grass. 🌱"
    )

    private val successQuotes = listOf(
        "W. You touched grass. 🗿🌱",
        "Nature arc completed. Go cook.",
        "Grass touched. Touchscreen privileges restored.",
        "Common outside W.",
        "You beat the algorithm. Massive W.",
        "Bro has returned to civilization.",
        "Vitamin G acquired. GG.",
        "Achievement unlocked: Not chronically online.",
        "The grass accepted your application.",
        "Outside buff activated. 📈",
        "Touch grass quest: COMPLETE.",
        "Internet privileges restored. Don't fumble it.",
        "Nature said you're valid. W.",
        "Go forth, grass warrior."
    )

    private val onboardingTips = listOf(
        "Set a limit. When you hit it, Touch Grass becomes your final boss.",
        "Real grass only. Screenshots are getting absolutely ratio'd.",
        "Your camera stays on-device during verification.",
        "Touch grass or watch a rewarded ad for a short break.",
        "No app hopping. One lock covers the whole phone."
    )

    fun getRandomBlockedQuote(): String = blockedQuotes[Random.nextInt(blockedQuotes.size)]
    fun getRandomCheatQuote(): String = cheatQuotes[Random.nextInt(cheatQuotes.size)]
    fun getRandomSuccessQuote(): String = successQuotes[Random.nextInt(successQuotes.size)]
    fun getRandomOnboardingTip(): String = onboardingTips[Random.nextInt(onboardingTips.size)]
}
