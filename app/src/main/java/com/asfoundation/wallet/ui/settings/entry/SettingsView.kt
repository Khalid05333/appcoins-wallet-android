package com.asfoundation.wallet.ui.settings.entry

import android.content.Intent
import com.appcoins.wallet.feature.changecurrency.data.FiatCurrency
import com.appcoins.wallet.feature.changecurrency.data.FiatCurrencyEntity
import com.appcoins.wallet.feature.promocode.data.repository.PromoCode
import io.reactivex.Observable


interface SettingsView {

  fun setRedeemCodePreference(walletAddress: String)

  fun setPromoCodePreference(promoCode: com.appcoins.wallet.feature.promocode.data.repository.PromoCode)

  fun showError()

  fun navigateToIntent(intent: Intent)

  fun authenticationResult(): Observable<Boolean>

  fun toggleFingerprint(enabled: Boolean)

  fun setPermissionPreference()

  fun setSourceCodePreference()

  fun setFingerprintPreference(hasAuthenticationPermission: Boolean)

  fun setTwitterPreference()

  fun setIssueReportPreference()

  fun setFacebookPreference()

  fun setTelegramPreference()

  fun setEmailPreference()

  fun setPrivacyPolicyPreference()

  fun setTermsConditionsPreference()

  fun setCreditsPreference()

  fun setVersionPreference()

  fun setCurrencyPreference(selectedCurrency: FiatCurrency)

  fun setRestorePreference()

  fun setBackupPreference()

  fun setManageWalletPreference()

  fun setAccountPreference()

  fun setManageSubscriptionsPreference()

  fun removeFingerprintPreference()

  fun setDisabledFingerPrintPreference()

  fun switchPreferenceChange(): Observable<Unit>

  fun updateFingerPrintListener(enabled: Boolean)

  fun setWithdrawPreference()

  fun setFaqsPreference()
}