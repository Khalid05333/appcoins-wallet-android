package com.asfoundation.wallet.onboarding_new_payment.google_pay

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appcoins.wallet.billing.adyen.PaymentModel
import com.appcoins.wallet.core.analytics.analytics.legacy.BillingAnalytics
import com.appcoins.wallet.core.network.microservices.model.GooglePayWebTransaction
import com.appcoins.wallet.core.utils.android_common.RxSchedulers
import com.appcoins.wallet.core.utils.android_common.toSingleEvent
import com.asf.wallet.R
import com.asfoundation.wallet.billing.adyen.PaymentType
import com.asfoundation.wallet.billing.googlepay.GooglePayWebFragment
import com.asfoundation.wallet.billing.googlepay.GooglePayWebViewModel
import com.asfoundation.wallet.billing.googlepay.models.GooglePayConst
import com.asfoundation.wallet.billing.googlepay.models.GooglePayResult
import com.asfoundation.wallet.billing.googlepay.usecases.BuildGooglePayUrlUseCase
import com.asfoundation.wallet.billing.googlepay.usecases.CreateGooglePayWebTransactionUseCase
import com.asfoundation.wallet.billing.googlepay.usecases.GetGooglePayResultUseCase
import com.asfoundation.wallet.billing.googlepay.usecases.GetGooglePayUrlUseCase
import com.asfoundation.wallet.billing.googlepay.usecases.WaitForSuccessUseCase
import com.asfoundation.wallet.entity.TransactionBuilder
import com.asfoundation.wallet.onboarding.use_cases.GetResponseCodeWebSocketUseCase
import com.asfoundation.wallet.onboarding.use_cases.SetResponseCodeWebSocketUseCase
import com.asfoundation.wallet.onboarding_new_payment.OnboardingPaymentEvents
import com.asfoundation.wallet.ui.iab.InAppPurchaseInteractor
import com.asfoundation.wallet.ui.iab.PaymentMethodsView
import com.wallet.appcoins.feature.support.data.SupportInteractor
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class OnboardingGooglePayViewModel @Inject constructor(
  private val getGooglePayUrlUseCase: GetGooglePayUrlUseCase,
  private val createGooglePayWebTransactionUseCase: CreateGooglePayWebTransactionUseCase,
  private val buildGooglePayUrlUseCase: BuildGooglePayUrlUseCase,
  private val waitForSuccessGooglePayWebUseCase: WaitForSuccessUseCase,
  private val getGooglePayResultUseCase: GetGooglePayResultUseCase,
  private val supportInteractor: SupportInteractor,
  private val inAppPurchaseInteractor: InAppPurchaseInteractor,
  private val rxSchedulers: RxSchedulers,
  private val events: OnboardingPaymentEvents,
  private val getResponseCodeWebSocketUseCase: GetResponseCodeWebSocketUseCase,
  private val setResponseCodeWebSocketUseCase: SetResponseCodeWebSocketUseCase,
  savedStateHandle: SavedStateHandle,
) : ViewModel() {

  sealed class State {
    object Start : State()
    data class Error(val stringRes: Int) : State()
    data class WebAuthentication(val url: String) : State()
    object SuccessPurchase : State()
    object GooglePayBack : State()
    data class BackToGame(val domain: String) : State()
    object ExploreWallet : State()
  }

  private val _state = MutableLiveData<State>(State.Start)
  val state = _state.toSingleEvent()

  private var compositeDisposable: CompositeDisposable = CompositeDisposable()

  private var args: OnboardingGooglePayFragmentArgs =
    OnboardingGooglePayFragmentArgs.fromSavedStateHandle(savedStateHandle)

  val networkScheduler = rxSchedulers.io
  val viewScheduler = rxSchedulers.main

  var uid: String? = null
  var shouldStartPayment = true
  var isFirstRun: Boolean = true
  var runningCustomTab: Boolean = false
  var purchaseUid: String? = null

  fun startPayment(
    amount: BigDecimal,
    currency: String,
    transactionBuilder: TransactionBuilder,
    origin: String?
  ) {
    if (shouldStartPayment) {
      shouldStartPayment = false
      events.sendPaymentConfirmationGooglePayEvent(transactionBuilder)
      compositeDisposable.add(getGooglePayUrlUseCase().subscribeOn(networkScheduler)
        .observeOn(viewScheduler).flatMap { urls ->
          createTransaction(
            amount = amount,
            currency = currency,
            transactionBuilder = transactionBuilder,
            origin = origin,
            returnUrl = urls.returnUrl,
          ).map { transaction ->
            uid = transaction.uid
            val googlePayUrl = buildGooglePayUrlUseCase(
              url = urls.url,
              sessionId = transaction.sessionId ?: "",
              sessionData = transaction.sessionData ?: "",
              price = amount.toString(),
              currency = currency,
            )
            Log.d("url", googlePayUrl)
            _state.postValue(State.WebAuthentication(googlePayUrl))
          }
        }.subscribe({}, {
          Log.d(TAG, it.toString())
          events.sendPaymentErrorMessageEvent(
            errorMessage = "GooglePayWeb transaction error.",
            transactionBuilder = transactionBuilder,
            paymentMethod = BillingAnalytics.PAYMENT_METHOD_GOOGLE_PAY_WEB,
          )
          _state.postValue(State.Error(R.string.purchase_error_google_pay))
        })
      )
    }
  }

  fun createTransaction(
    amount: BigDecimal,
    currency: String,
    transactionBuilder: TransactionBuilder,
    origin: String?,
    returnUrl: String,
  ): Single<GooglePayWebTransaction> {
    return createGooglePayWebTransactionUseCase(
      value = (amount.toString()),
      currency = currency,
      transactionBuilder = transactionBuilder,
      origin = origin,
      method = PaymentType.GOOGLEPAY_WEB.subTypes[0],
      returnUrl = returnUrl,
    )
      .subscribeOn(networkScheduler)
      .observeOn(viewScheduler)
      .doOnSuccess {
        when (it?.validity) {
          GooglePayWebTransaction.GooglePayWebValidityState.COMPLETED -> {
            handleSuccess(it.uid, transactionBuilder)
          }

          GooglePayWebTransaction.GooglePayWebValidityState.PENDING -> {
          }

          GooglePayWebTransaction.GooglePayWebValidityState.ERROR -> {
            Log.d(TAG, "GooglePayWeb transaction error")
            events.sendPaymentErrorMessageEvent(
              errorMessage = "GooglePayWeb transaction error.",
              transactionBuilder = transactionBuilder,
              paymentMethod = BillingAnalytics.PAYMENT_METHOD_GOOGLE_PAY_WEB,
            )
            _state.postValue(State.Error(R.string.purchase_error_google_pay))
          }

          null -> {
            Log.d(TAG, "GooglePayWeb transaction error")
            events.sendPaymentErrorMessageEvent(
              errorMessage = "GooglePayWeb transaction error.",
              transactionBuilder = transactionBuilder,
              paymentMethod = BillingAnalytics.PAYMENT_METHOD_GOOGLE_PAY_WEB,
            )
            _state.postValue(State.Error(R.string.purchase_error_google_pay))
          }
        }
      }
  }

  private fun waitForSuccess(
    uid: String?,
    transactionBuilder: TransactionBuilder,
    wasNonSuccess: Boolean = false,
  ) {
    val disposableSuccessCheck = waitForSuccessGooglePayWebUseCase(uid ?: "")
      .subscribeOn(networkScheduler)
      .observeOn(viewScheduler)
      .subscribe({
        when (it.status) {
          PaymentModel.Status.COMPLETED -> {
            purchaseUid = it.purchaseUid
            handleSuccess(it.uid, transactionBuilder)
          }

          PaymentModel.Status.FAILED, PaymentModel.Status.FRAUD, PaymentModel.Status.CANCELED, PaymentModel.Status.INVALID_TRANSACTION -> {
            Log.d(TAG, "Error on transaction on Settled transaction polling")
            events.sendPaymentErrorMessageEvent(
              errorMessage = "Error on transaction on Settled transaction polling ${it.status.name}",
              transactionBuilder = transactionBuilder,
              paymentMethod = BillingAnalytics.PAYMENT_METHOD_GOOGLE_PAY_WEB,
            )
            _state.postValue(State.Error(R.string.unknown_error))
          }

          else -> { /* pending */
          }
        }
      }, {
        Log.d(TAG, "Error on Settled transaction polling")
        events.sendPaymentErrorMessageEvent(
          errorMessage = "Error on Settled transaction polling",
          transactionBuilder = transactionBuilder,
          paymentMethod = BillingAnalytics.PAYMENT_METHOD_GOOGLE_PAY_WEB,
        )
      })
    // disposes the check after x seconds
    viewModelScope.launch {
      delay(GooglePayConst.GOOGLE_PAY_TIMEOUT)
      try {
        if (state.value !is State.SuccessPurchase && wasNonSuccess)
          _state.postValue(State.Error(R.string.purchase_error_google_pay))
        disposableSuccessCheck.dispose()
      } catch (_: Exception) {
      }
    }
  }

  fun processGooglePayResult(transactionBuilder: TransactionBuilder) {
    if (isFirstRun) {
      isFirstRun = false
    } else {
      if (runningCustomTab) {
        runningCustomTab = false
        val result = getGooglePayResultUseCase()
        when (result) {
          GooglePayResult.SUCCESS.key -> {
            waitForSuccess(uid, transactionBuilder)
          }

          GooglePayResult.ERROR.key -> {
            events.sendPaymentErrorMessageEvent(
              errorMessage = "Error received from Web",
              transactionBuilder = transactionBuilder,
              paymentMethod = BillingAnalytics.PAYMENT_METHOD_GOOGLE_PAY_WEB,
            )
            _state.postValue(State.Error(R.string.purchase_error_google_pay))
          }

          GooglePayResult.CANCEL.key -> {
            waitForSuccess(uid, transactionBuilder, true)
          }

          else -> {
            waitForSuccess(uid, transactionBuilder, true)
          }
        }
      }
    }
  }

  fun openUrlCustomTab(context: Context, url: String) {
    if (runningCustomTab) return
    runningCustomTab = true
    val customTabsBuilder = CustomTabsIntent.Builder().build()
    customTabsBuilder.intent.setPackage(GooglePayWebFragment.CHROME_PACKAGE_NAME)
    customTabsBuilder.launchUrl(context, Uri.parse(url))
  }

  fun handleSuccess(
    purchaseUid: String?,
    transactionBuilder: TransactionBuilder
  ) {
    inAppPurchaseInteractor.savePreSelectedPaymentMethod(
      PaymentMethodsView.PaymentMethodId.GOOGLEPAY_WEB.id
    )
    events.sendGooglePaySuccessFinishEvents(
      transactionBuilder = transactionBuilder,
      purchaseUid ?: ""
    )
    _state.postValue(State.SuccessPurchase)
  }


  fun showSupport() {
    compositeDisposable.add(
      supportInteractor.showSupport(uid = uid).subscribe({}, { it.printStackTrace() })
    )
  }


  fun handleBackToGameClick() {
    events.sendPaymentConclusionNavigationEvent(OnboardingPaymentEvents.BACK_TO_THE_GAME)
    _state.postValue(State.BackToGame(args.transactionBuilder.domain))
  }

  fun handleExploreWalletClick() {
    events.sendPaymentConclusionNavigationEvent(OnboardingPaymentEvents.EXPLORE_WALLET)
    _state.postValue(State.ExploreWallet)
  }

  fun getResponseCodeWebSocket() = getResponseCodeWebSocketUseCase()

  fun setResponseCodeWebSocket(responseCode: Int) = setResponseCodeWebSocketUseCase(responseCode)

  companion object {
    private val TAG = GooglePayWebViewModel::class.java.simpleName
  }

}