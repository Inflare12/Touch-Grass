package com.example.utils

import kotlin.random.Random

object FunnyQuotes {

    private val blockedQuotes = listOf(
        "Your phone has decided you've had enough.",
        "Bro. Go outside.",
        "Your screen time is becoming a lifestyle.",
        "You have entered terminal internet dweller mode.",
        "Photosynthesis awaits, modern creature.",
        "The algorithms will survive 5 minutes without you.",
        "Do you remember what oxygen feels like? Go find out.",
        "Emergency protocol engaged: Physical existence required.",
        "Grass is real. The feed is temporary.",
        "Disconnect to reconnect with actual chlorophyll."
    )

    private val cheatQuotes = listOf(
        "Nice try. That's a screen, I can tell.",
        "Artificial grass detected. The nature committee is disappointed.",
        "Tree detected. That is bark, not grass.",
        "Indoor carpet alert: That's polypropylene, not nature.",
        "Static image detected. Trees actually move in the breeze.",
        "You're close... but you have to actually touch it.",
        "Hovering isn't touching. Commit to the soil.",
        "Nice screenshot of grass. Very modern. Now find real grass."
    )

    private val successQuotes = listOf(
        "Nature has accepted you.",
        "Grass touched. Civilization restored.",
        "You may return to the internet.",
        "Achievement unlocked: Outside.",
        "Sensory receptors recalibrated with genuine earth.",
        "Clean air logged. The trees nod in approval.",
        "You survived the wilderness. Return victorious."
    )

    private val onboardingTips = listOf(
        "Screen time getting out of hand?",
        "Real chlorophyll cures algorithm poisoning.",
        "Proof of grass touch resets your digital freedom."
    )

    fun getRandomBlockedQuote(): String = blockedQuotes[Random.nextInt(blockedQuotes.size)]

    fun getRandomCheatQuote(): String = cheatQuotes[Random.nextInt(cheatQuotes.size)]

    fun getRandomSuccessQuote(): String = successQuotes[Random.nextInt(successQuotes.size)]

    fun getRandomOnboardingTip(): String = onboardingTips[Random.nextInt(onboardingTips.size)]
}
