package com.asfoundation.wallet.topup

import android.net.Uri
import android.os.Bundle
import com.asfoundation.wallet.billing.adyen.PaymentType

interface TopUpActivityView {
  fun showTopUpScreen()

  fun navigateToAdyenPayment(paymentType: PaymentType, data: AdyenTopUpData,
                             transactionType: String,
                             gamificationLevel: Int)

  fun navigateToLocalPayment(paymentId: String, topUpData: TopUpData, transactionType: String,
                             gamificationLevel: Int)

  fun finish(data: Bundle)

  fun navigateBack()

  fun close(navigateToTransactions: Boolean)

  fun acceptResult(uri: Uri)

  fun showToolbar()

  fun lockOrientation()

  fun unlockRotation()

  fun cancelPayment()

  fun setFinishingPurchase()
}
