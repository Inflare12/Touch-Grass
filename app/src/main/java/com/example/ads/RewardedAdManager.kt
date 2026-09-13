package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/** Real rewarded-ad implementation. Uses Google's test unit until replaced for release. */
class RewardedAdManager(private val context: Context) : AdManager {
    private var rewardedAd: RewardedAd? = null
    private var loading = false

    init {
        load()
    }

    override fun isAdLoaded(): Boolean = rewardedAd != null

    private fun load() {
        if (loading || rewardedAd != null) return
        loading = true
        RewardedAd.load(
            context,
            TEST_REWARDED_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    loading = false
                    rewardedAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loading = false
                    rewardedAd = null
                    Log.w(TAG, "Rewarded ad failed to load: ${error.message}")
                }
            }
        )
    }

    override fun showRewardedAd(
        onRewardEarned: (rewardAmount: Int) -> Unit,
        onAdClosed: () -> Unit
    ) {
        val activity = context as? Activity
        val ad = rewardedAd
        if (activity == null || ad == null) {
            load()
            onAdClosed()
            return
        }

        rewardedAd = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                onAdClosed()
                load()
            }

            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                Log.w(TAG, "Rewarded ad failed to show: ${adError.message}")
                onAdClosed()
                load()
            }
        }

        ad.show(activity, OnUserEarnedRewardListener { rewardItem ->
            onRewardEarned(rewardItem.amount)
        })
    }

    companion object {
        // Google-provided test rewarded unit. Replace before production release.
        const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
        private const val TAG = "TouchGrassAds"
    }
}
