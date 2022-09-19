package com.appcoins.wallet.gamification.repository

import com.appcoins.wallet.gamification.GamificationContext
import com.appcoins.wallet.gamification.repository.entity.LevelsResponse
import com.appcoins.wallet.gamification.repository.entity.PromotionsResponse
import com.appcoins.wallet.gamification.repository.entity.WalletOrigin
import io.reactivex.Completable
import io.reactivex.Single

interface UserStatsLocalData {
  /**
   * @return GamificationStats.INVALID_LEVEL if never showed any level
   */
  fun getLastShownLevel(wallet: String, gamificationContext: GamificationContext): Single<Int>

  fun saveShownLevel(wallet: String, level: Int, gamificationContext: GamificationContext)

  fun setGamificationLevel(gamificationLevel: Int)

  fun getGamificationLevel(): Int

  fun getSeenGenericPromotion(id: String, screen: String): Boolean

  fun setSeenGenericPromotion(id: String, screen: String)

  fun getPromotions(): Single<List<PromotionsResponse>>

  fun deleteAndInsertPromotions(promotions: List<PromotionsResponse>): Completable

  fun deleteLevels(): Completable

  fun getLevels(): Single<LevelsResponse>

  fun insertLevels(levels: LevelsResponse): Completable

  fun insertWalletOrigin(wallet: String, walletOrigin: WalletOrigin): Completable

  fun retrieveWalletOrigin(wallet: String): Single<WalletOrigin>

  fun shouldShowGamificationDisclaimer(): Boolean

  fun setGamificationDisclaimerShown()

  fun setSeenWalletOrigin(wallet: String, walletOrigin: String)

  fun getSeenWalletOrigin(wallet: String): String

  fun isVipCalloutAlreadySeen(): Boolean

  fun setVipCalloutAlreadySeen(isSeen: Boolean)
}
