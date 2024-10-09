package com.asfoundation.wallet.topup.amazonPay

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Nullable
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import com.appcoins.wallet.core.analytics.analytics.common.ButtonsAnalytics
import com.appcoins.wallet.ui.common.theme.WalletColors
import com.appcoins.wallet.ui.common.theme.WalletColors.styleguide_blue
import com.appcoins.wallet.ui.widgets.GenericError
import com.appcoins.wallet.ui.widgets.component.Animation
import com.appcoins.wallet.ui.widgets.component.ButtonType
import com.appcoins.wallet.ui.widgets.component.ButtonWithText
import com.asf.wallet.R
import com.asfoundation.wallet.billing.amazonPay.models.AmazonConsts.Companion.APP_LINK_HOST
import com.asfoundation.wallet.billing.amazonPay.models.AmazonConsts.Companion.APP_LINK_PATH
import com.asfoundation.wallet.billing.amazonPay.models.AmazonConsts.Companion.CHECKOUT_LANGUAGE
import com.asfoundation.wallet.topup.TopUpAnalytics
import com.asfoundation.wallet.topup.TopUpPaymentData
import com.asfoundation.wallet.topup.adyen.TopUpNavigator
import com.wallet.appcoins.core.legacy_base.BasePageViewFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class AmazonPayTopUpFragment : BasePageViewFragment(){

  private val viewModel: AmazonPayTopUpViewModel by viewModels()

  @Inject
  lateinit var navigator: TopUpNavigator

  @Inject
  lateinit var analytics: TopUpAnalytics

  @Inject
  lateinit var buttonsAnalytics: ButtonsAnalytics
  private val fragmentName = this::class.java.simpleName


  override fun onCreateView(
    inflater: LayoutInflater, @Nullable container: ViewGroup?,
    @Nullable savedInstanceState: Bundle?
  ): View {
    if (arguments?.getSerializable(PAYMENT_DATA) != null) {
      viewModel.paymentData =
        arguments?.getSerializable(PAYMENT_DATA) as TopUpPaymentData
    }
    viewModel.getPaymentLink()
    return ComposeView(requireContext()).apply { setContent { MainContent() } }
  }

  @Composable
  @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
  fun MainContent() {
    Scaffold(
      containerColor = styleguide_blue,
    ) { _ ->
      when (viewModel.uiState.collectAsState().value) {
        is UiState.Success -> {
          SuccessScreen()
        }
        UiState.Idle,
        UiState.Loading -> {
          Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Animation(modifier = Modifier.size(104.dp), animationRes = R.raw.loading_wallet)
          }
        }
        is UiState.Error -> {
          Log.d("amazonpaytransaction", "fragment: UiState.Error")
          analytics.sendErrorEvent(
            value = viewModel.paymentData.appcValue.toDouble(),
            paymentMethod = "amazon_pay",
            status = "error",
            errorCode = viewModel.amazonTransaction?.errorCode ?: "",
            errorDetails = viewModel.amazonTransaction?.errorContent ?: ""
          )
          GenericError(
            message = stringResource(R.string.activity_iab_error_message),
            onSupportClick = {
              viewModel.launchChat()
            },
            onTryAgain = {
              navigator.navigateBack()
            },
            fragmentName = fragmentName,
            buttonAnalytics = buttonsAnalytics)
        }
        UiState.PaymentLinkSuccess -> {
          createAmazonPayLink()
          Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Animation(modifier = Modifier.size(104.dp), animationRes = R.raw.loading_wallet)
          }
        }
      }
    }
  }

  @Composable
  fun SuccessScreen() {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(24.dp)
    ) {
      Spacer(modifier = Modifier.weight(72f))
      Animation(
        modifier = Modifier.size(104.dp),
        animationRes = R.raw.success_animation,
        iterations = 1
      )
      Text(
        text = stringResource(id = R.string.activity_iab_transaction_completed_title),
        color = WalletColors.styleguide_light_grey,
        modifier = Modifier.padding(top = 28.dp),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.weight(304f))
      ButtonWithText(
        modifier = Modifier
          .padding(top = 40.dp)
          .widthIn(max = 360.dp),
        label = stringResource(id = R.string.got_it_button),
        onClick = {
          //analytics.sendSuccessScreenEvent(action = GOT_IT)
          navigator.navigateBack()
        },
        labelColor = WalletColors.styleguide_white,
        backgroundColor = WalletColors.styleguide_pink,
        buttonType = ButtonType.LARGE,
        fragmentName = fragmentName,
        buttonsAnalytics = buttonsAnalytics
      )
    }
  }

  private fun createAmazonPayLink() {
     val params = mapOf(
      //Comes from MS
      "merchantId" to viewModel.amazonTransaction?.merchantId,
      //Currency comes from MS
      "ledgerCurrency" to "EUR",
      //Region comes from MS
      "checkoutLanguage" to CHECKOUT_LANGUAGE.getValue("UK"),
      //productType Comes FROM MS
      "productType" to "PayOnly",
      //Only needs in DEV
      "environment" to "SANDBOX",
    "amazonCheckoutSessionId" to viewModel.amazonTransaction?.checkoutSessionId,
    "integrationType" to "NativeMobile",
    "payloadJSON" to viewModel.amazonTransaction?.payload
    )
    buildURL(params, "DE")
  }

  override fun onResume() {
    super.onResume()
    viewModel.startTransactionStatusTimer()
  }


  private fun buildURL(parameters: Map<String, String?>, region: String) {
    val uriBuilder = Uri.Builder()
    for ((key, value) in parameters) {
      uriBuilder.appendQueryParameter(key, value)
    }
    uriBuilder.scheme("https")
        .authority(APP_LINK_HOST.getValue(region))
        .path(APP_LINK_PATH.getValue(region))
      redirectUsingUniversalLink(uriBuilder.build().toString())
  }

  private fun redirectUsingUniversalLink(url: String) {
    viewModel.runningCustomTab = true
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    // Opens url in Amazon Shopping App, if App is installed else open in CCT
    startActivity(intent)
    Log.d("amazonpaytransaction", "redirectUsingUniversalLink: runningCustomTab")
    val customTabsBuilder = CustomTabsIntent.Builder().build()
    //customTabsBuilder.intent.setPackage(CHROME_PACKAGE_NAME)
    //customTabsBuilder.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    //customTabsBuilder.launchUrl(requireContext(), Uri.parse(url))
  }



  companion object {
    const val PAYMENT_DATA = "data"
    internal const val TOP_UP_AMOUNT = "top_up_amount"
    internal const val TOP_UP_CURRENCY = "currency"
    internal const val TOP_UP_CURRENCY_SYMBOL = "currency_symbol"
    internal const val BONUS = "bonus"
    const val CHROME_PACKAGE_NAME = "com.android.chrome"
  }
}
