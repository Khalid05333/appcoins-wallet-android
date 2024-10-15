package com.asfoundation.wallet.topup.adyen

import android.net.Uri
import com.adyen.checkout.components.model.payments.response.Action
import com.appcoins.wallet.billing.adyen.PaymentInfoModel
import com.asfoundation.wallet.billing.adyen.AdyenCardWrapper
import com.asfoundation.wallet.billing.adyen.AdyenComponentResponseModel
import io.reactivex.Observable
import java.math.BigDecimal

interface AdyenTopUpView {

  fun showValues(value: String, currency: String)

  fun showLoading()

  fun hideLoading()

  fun showNetworkError()

  fun showInvalidCardError()

  fun showSecurityValidationError()

  fun showOutdatedCardError()

  fun showAlreadyProcessedError()

  fun showPaymentError()

  fun updateTopUpButton(valid: Boolean)

  fun cancelPayment()

  fun setFinishingPurchase(newState: Boolean)

  fun finishCardConfiguration(paymentInfoModel: PaymentInfoModel, forget: Boolean)

  fun setupRedirectComponent()

  fun submitUriResult(uri: Uri)

  fun getPaymentDetails(): Observable<AdyenComponentResponseModel>

  fun showSpecificError(stringRes: Int)

  fun showVerificationError()

  fun showCvvError()

  fun topUpButtonClicked(): Observable<Any>

  fun retrievePaymentData(): Observable<AdyenCardWrapper>

  fun hideKeyboard()

  fun getTryAgainClicks(): Observable<Any>

  fun getSupportClicks(): Observable<Any>

  fun getVerificationClicks(): Observable<Any>

  fun handleCreditCardNeedCVC(needCVC: Boolean)

  fun lockRotation()

  fun retryClick(): Observable<Any>

  fun hideErrorViews()

  fun showRetryAnimation()

  fun setupUi()

  fun showBonus(bonus: BigDecimal, currency: String)

  fun showVerification(paymentType: String)

  fun handle3DSAction(action: Action)

  fun onAdyen3DSError(): Observable<String>

  fun setup3DSComponent()

  fun shouldStoreCard(): Boolean

  fun restartFragment()

  fun hasStoredCardBuy(): Boolean

}
