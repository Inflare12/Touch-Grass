package com.example.ads

import android.content.Context

class RewardedAdManager(private val context: Context) : AdManager {

    private var isLoaded: Boolean = true

    override fun isAdLoaded(): Boolean = isLoaded

    override fun showRewardedAd(
        onRewardEarned: (rewardAmount: Int) -> Unit,
        onAdClosed: () -> Unit
    ) {
        // Architecture allows plugging in Google Mobile Ads SDK / AppLovin / Unity Ads
        // Configurable without exposing hardcoded production secrets.
        onRewardEarned(1)
        onAdClosed()
    }
}
