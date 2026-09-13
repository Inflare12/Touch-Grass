package com.example.ads

import android.content.Context

interface AdManager {
    fun isAdLoaded(): Boolean
    fun showRewardedAd(onRewardEarned: (rewardAmount: Int) -> Unit, onAdClosed: () -> Unit)
}
