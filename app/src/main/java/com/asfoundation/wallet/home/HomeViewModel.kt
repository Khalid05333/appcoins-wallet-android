package com.asfoundation.wallet.home

import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.appcoins.wallet.core.analytics.analytics.compatible_apps.CompatibleAppsAnalytics
import com.appcoins.wallet.core.analytics.analytics.email.EmailAnalytics
import com.appcoins.wallet.core.analytics.analytics.legacy.WalletsAnalytics
import com.appcoins.wallet.core.analytics.analytics.legacy.WalletsEventSender
import com.appcoins.wallet.core.arch.BaseViewModel
import com.appcoins.wallet.core.arch.SideEffect
import com.appcoins.wallet.core.arch.ViewState
import com.appcoins.wallet.core.arch.data.Async
import com.appcoins.wallet.core.network.base.call_adapter.ApiException
import com.appcoins.wallet.core.network.base.call_adapter.ApiFailure
import com.appcoins.wallet.core.network.base.call_adapter.ApiSuccess
import com.appcoins.wallet.core.network.base.compat.PostUserEmailUseCase
import com.appcoins.wallet.core.utils.android_common.DateFormatterUtils.getDay
import com.appcoins.wallet.core.utils.android_common.RxSchedulers
import com.appcoins.wallet.core.utils.jvm_common.Logger
import com.appcoins.wallet.feature.backup.ui.triggers.TriggerUtils.toJson
import com.appcoins.wallet.feature.changecurrency.data.use_cases.GetSelectedCurrencyUseCase
import com.appcoins.wallet.feature.walletInfo.data.balance.TokenBalance
import com.appcoins.wallet.feature.walletInfo.data.balance.WalletBalance
import com.appcoins.wallet.feature.walletInfo.data.wallet.domain.Wallet
import com.appcoins.wallet.feature.walletInfo.data.wallet.usecases.GetWalletInfoUseCase
import com.appcoins.wallet.feature.walletInfo.data.wallet.usecases.ObserveWalletInfoUseCase
import com.appcoins.wallet.gamification.repository.Levels
import com.appcoins.wallet.gamification.repository.PromotionsGamificationStats
import com.appcoins.wallet.sharedpreferences.BackupTriggerPreferencesDataSource
import com.appcoins.wallet.sharedpreferences.BackupTriggerPreferencesDataSource.TriggerSource.NEW_LEVEL
import com.appcoins.wallet.sharedpreferences.CommonsPreferencesDataSource
import com.appcoins.wallet.sharedpreferences.EmailPreferencesDataSource
import com.appcoins.wallet.ui.widgets.CardPromotionItem
import com.appcoins.wallet.ui.widgets.GameData
import com.appcoins.wallet.ui.widgets.R
import com.asfoundation.wallet.entity.GlobalBalance
import com.asfoundation.wallet.gamification.ObserveUserStatsUseCase
import com.asfoundation.wallet.home.HomeViewModel.UiState.Success
import com.asfoundation.wallet.home.usecases.DisplayChatUseCase
import com.asfoundation.wallet.home.usecases.FetchTransactionsHistoryUseCase
import com.asfoundation.wallet.home.usecases.FindDefaultWalletUseCase
import com.asfoundation.wallet.home.usecases.FindNetworkInfoUseCase
import com.asfoundation.wallet.home.usecases.GetCardNotificationsUseCase
import com.asfoundation.wallet.home.usecases.GetGamesListingUseCase
import com.asfoundation.wallet.home.usecases.GetImpressionUseCase
import com.asfoundation.wallet.home.usecases.GetLastShownUserLevelUseCase
import com.asfoundation.wallet.home.usecases.GetLevelsUseCase
import com.asfoundation.wallet.home.usecases.GetUnreadConversationsCountEventsUseCase
import com.asfoundation.wallet.home.usecases.GetUserLevelUseCase
import com.asfoundation.wallet.home.usecases.ObserveDefaultWalletUseCase
import com.asfoundation.wallet.home.usecases.RegisterSupportUserUseCase
import com.asfoundation.wallet.home.usecases.ShouldOpenRatingDialogUseCase
import com.asfoundation.wallet.home.usecases.UpdateLastShownUserLevelUseCase
import com.asfoundation.wallet.promotions.model.PromotionsModel
import com.asfoundation.wallet.promotions.usecases.GetPromotionsUseCase
import com.asfoundation.wallet.promotions.usecases.SetSeenPromotionsUseCase
import com.asfoundation.wallet.referrals.CardNotification
import com.asfoundation.wallet.transactions.TransactionModel
import com.asfoundation.wallet.transactions.toModel
import com.asfoundation.wallet.ui.widget.entity.TransactionsModel
import com.asfoundation.wallet.viewmodel.TransactionsWalletModel
import com.github.michaelbull.result.unwrap
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.rxSingle
import java.math.BigDecimal
import java.util.concurrent.TimeUnit
import javax.inject.Inject

sealed class HomeSideEffect : SideEffect {
  data class NavigateToBrowser(val uri: Uri) : HomeSideEffect()

  data class NavigateToRateUs(val shouldNavigate: Boolean) : HomeSideEffect()

  data class NavigateToSettings(val turnOnFingerprint: Boolean = false) : HomeSideEffect()

  data class NavigateToBackup(val walletAddress: String, val walletName: String) : HomeSideEffect()

  data class NavigateToIntent(val intent: Intent) : HomeSideEffect()

  data class NavigateToBalanceDetails(val balance: WalletBalance) : HomeSideEffect()

  object NavigateToTopUp : HomeSideEffect()

  object NavigateToTransfer : HomeSideEffect()

  object NavigateToTransactionsList : HomeSideEffect()

  object NavigateToRecover : HomeSideEffect()
}

data class HomeState(
  val transactionsModelAsync: Async<TransactionsModel> = Async.Uninitialized,
  val promotionsModelAsync: Async<PromotionsModel> = Async.Uninitialized,
  val defaultWalletBalanceAsync: Async<GlobalBalance> = Async.Uninitialized,
  val unreadMessages: Boolean = false,
  val hasBackup: Async<Boolean> = Async.Uninitialized
) : ViewState

data class PromotionsState(
  val promotionsModelAsync: Async<PromotionsModel> = Async.Uninitialized,
  val promotionsGamificationStatsAsync: Async<PromotionsGamificationStats> = Async.Uninitialized
) :
  ViewState


@HiltViewModel
class HomeViewModel
@Inject
constructor(
  private val compatibleAppsAnalytics: CompatibleAppsAnalytics,
  private val backupTriggerPreferences: BackupTriggerPreferencesDataSource,
  private val observeWalletInfoUseCase: ObserveWalletInfoUseCase,
  private val getWalletInfoUseCase: GetWalletInfoUseCase,
  private val getPromotionsUseCase: GetPromotionsUseCase,
  private val setSeenPromotionsUseCase: SetSeenPromotionsUseCase,
  private val shouldOpenRatingDialogUseCase: ShouldOpenRatingDialogUseCase,
  private val findNetworkInfoUseCase: FindNetworkInfoUseCase,
  private val findDefaultWalletUseCase: FindDefaultWalletUseCase,
  private val observeDefaultWalletUseCase: ObserveDefaultWalletUseCase,
  private val getGamesListingUseCase: GetGamesListingUseCase,
  private val getLevelsUseCase: GetLevelsUseCase,
  private val getUserLevelUseCase: GetUserLevelUseCase,
  private val observeUserStatsUseCase: ObserveUserStatsUseCase,
  private val getLastShownUserLevelUseCase: GetLastShownUserLevelUseCase,
  private val updateLastShownUserLevelUseCase: UpdateLastShownUserLevelUseCase,
  private val getCardNotificationsUseCase: GetCardNotificationsUseCase,
  private val registerSupportUserUseCase: RegisterSupportUserUseCase,
  private val getUnreadConversationsCountEventsUseCase: GetUnreadConversationsCountEventsUseCase,
  private val displayChatUseCase: DisplayChatUseCase,
  private val fetchTransactionsHistoryUseCase: FetchTransactionsHistoryUseCase,
  private val getSelectedCurrencyUseCase: GetSelectedCurrencyUseCase,
  private val walletsEventSender: WalletsEventSender,
  private val rxSchedulers: RxSchedulers,
  private val logger: Logger,
  private val commonsPreferencesDataSource: CommonsPreferencesDataSource,
  private val postUserEmailUseCase: PostUserEmailUseCase,
  private val emailPreferencesDataSource: EmailPreferencesDataSource,
  private val emailAnalytics: EmailAnalytics,
  private val getImpressionUseCase: GetImpressionUseCase
) : BaseViewModel<HomeState, HomeSideEffect>(initialState()) {

  private lateinit var defaultCurrency: String
  private val UPDATE_INTERVAL = 30 * DateUtils.SECOND_IN_MILLIS
  private val refreshData = BehaviorSubject.createDefault(true)
  private val refreshCardNotifications = BehaviorSubject.createDefault(true)
  var showBackup by mutableStateOf(false)
  var newWallet by mutableStateOf(false)
  var isLoadingTransactions by mutableStateOf(false)
  var hasNotificationBadge by mutableStateOf(false)
  var gamesList by mutableStateOf(listOf<GameData>())
  val activePromotions = mutableStateListOf<CardPromotionItem>()
  var hasSavedEmail by mutableStateOf(hasWalletEmailPreferencesData())
  var isEmailError by mutableStateOf(false)
  var emailErrorText by mutableStateOf(0)
  private var alreadyGetImpression by mutableStateOf(false)

  companion object {
    private val TAG = HomeViewModel::class.java.name

    private const val SUCCESS_EMAIL_ANALYTICS = "success"
    private const val ERROR_EMAIL_ANALYTICS = "erorr"

    fun initialState() = HomeState()
  }

  private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
  var uiState: StateFlow<UiState> = _uiState

  private val _uiBalanceState = MutableStateFlow<UiBalanceState>(UiBalanceState.Idle)
  var uiBalanceState: StateFlow<UiBalanceState> = _uiBalanceState

  init {
    handleWalletData()
    verifyUserLevel()
    handleUnreadConversationCount()
    handleRateUsDialogVisibility()
    fetchPromotions()
    hasNotificationBadge = commonsPreferencesDataSource.getUpdateNotificationBadge()
  }

  private fun handleWalletData() {
    observeRefreshData()
      .switchMap { observeNetworkAndWallet() }
      .switchMap { observeWalletData(it) }
      .scopedSubscribe { e -> e.printStackTrace() }
  }

  private fun observeRefreshData(): Observable<Boolean> {
    return refreshData.filter { refreshData: Boolean? -> refreshData!! }
  }

  fun updateData() {
    refreshData.onNext(true)
  }

  fun stopRefreshingData() {
    refreshData.onNext(false)
  }

  private fun observeNetworkAndWallet(): Observable<TransactionsWalletModel> {
    return Observable.combineLatest(
      findNetworkInfoUseCase().toObservable(), observeDefaultWalletUseCase()
    ) { networkInfo,
        wallet ->
      val previousModel: TransactionsWalletModel? =
        state.transactionsModelAsync.value?.transactionsWalletModel
      val isNewWallet =
        previousModel == null || !previousModel.wallet.hasSameAddress(wallet.address)
      TransactionsWalletModel(networkInfo, wallet, isNewWallet)
    }
  }

  private fun observeWalletData(model: TransactionsWalletModel): Observable<Unit> {
    return Observable.mergeDelayError(
      observeBalance(),
      updateTransactions(model).subscribeOn(rxSchedulers.io),
      updateRegisterUser(model.wallet).toObservable(),
      observeBackup()
    )
      .map {}
      .doOnError {
        it.printStackTrace()
      }
      .subscribeOn(rxSchedulers.io)
  }

  private fun fetchTransactionData() {
    Observable.combineLatest(
      rxSingle { getSelectedCurrencyUseCase(false) }.toObservable(),
      observeDefaultWalletUseCase()
    ) { selectedCurrency, wallet ->
      defaultCurrency = selectedCurrency.unwrap()
      fetchTransactions(wallet, defaultCurrency)
    }.doOnError { it.printStackTrace() }
      .subscribe()
  }

  fun postUserEmail(email: String) {
    emailAnalytics.walletAppEmailSubmitClick()
    postUserEmailUseCase(email).doOnComplete {
      emailAnalytics.walletAppEmailSubmitted(SUCCESS_EMAIL_ANALYTICS)
      hasSavedEmail = true
    }.scopedSubscribe { e ->
      e.printStackTrace()
      emailAnalytics.walletAppEmailSubmitted(ERROR_EMAIL_ANALYTICS)
      isEmailError = true
      emailErrorText = if (e.message.equals("HTTP 422 ")) {
        R.string.e_skills_withdraw_invalid_email_error_message
      } else {
        R.string.error_general
      }
    }
  }

  private fun hasWalletEmailPreferencesData(): Boolean {
    return !emailPreferencesDataSource.getWalletEmail().isNullOrEmpty()
  }

  fun getWalletEmailPreferencesData(): String {
    return emailPreferencesDataSource.getWalletEmail().toString()
  }

  fun saveHideWalletEmailCardPreferencesData(hasEmailSaved: Boolean) {
    emailPreferencesDataSource.saveHideWalletEmailCard(hasEmailSaved)
  }

  fun isHideWalletEmailCardPreferencesData(): Boolean {
    return emailPreferencesDataSource.isHideWalletEmailCard()
  }

  fun getImpression() {
    if (!alreadyGetImpression) {
      getImpressionUseCase().doOnComplete {
        alreadyGetImpression = true
      }.scopedSubscribe { e ->
        e.printStackTrace()
      }
    }
  }

  private fun updateRegisterUser(wallet: Wallet): Completable {
    return getUserLevelUseCase()
      .subscribeOn(rxSchedulers.io)
      .map { userLevel ->
        registerSupportUser(userLevel, wallet.address)
        true
      }
      .ignoreElement()
      .doOnError {
        it.printStackTrace()
      }
      .subscribeOn(rxSchedulers.io)
  }

  private fun registerSupportUser(level: Int, walletAddress: String) {
    registerSupportUserUseCase(level, walletAddress)
  }

  /**
   * Balance is refreshed every [UPDATE_INTERVAL] seconds, and stops while [refreshData] is false
   */
  private fun observeBalance(): Observable<GlobalBalance> {
    return Observable.interval(0, UPDATE_INTERVAL, TimeUnit.MILLISECONDS)
      .flatMap { observeRefreshData() }
      .switchMap {
        observeWalletInfoUseCase(null, update = true)
          .map { walletInfo -> mapWalletValue(walletInfo.walletBalance) }
          .asAsyncToState(HomeState::defaultWalletBalanceAsync) {
            copy(defaultWalletBalanceAsync = it)
          }
      }
      .doOnNext { fetchTransactionData() }
  }

  /**
   * Balance is refreshed every [UPDATE_INTERVAL] seconds, and stops while [refreshData] is false
   */
  private fun observeBackup(): Observable<Boolean> {
    return Observable.interval(0, UPDATE_INTERVAL, TimeUnit.MILLISECONDS)
      .flatMap { observeRefreshData() }
      .switchMap {
        observeWalletInfoUseCase(null, update = true)
          .map { walletInfo -> walletInfo.hasBackup }
          .asAsyncToState(HomeState::hasBackup) { copy(hasBackup = it) }
      }
  }

  private fun mapWalletValue(walletBalance: WalletBalance): GlobalBalance {
    return GlobalBalance(
      walletBalance,
      shouldShow(walletBalance.appcBalance, 0.01),
      shouldShow(walletBalance.creditsBalance, 0.01),
      shouldShow(walletBalance.ethBalance, 0.0001)
    )
  }

  private fun shouldShow(tokenBalance: TokenBalance, threshold: Double): Boolean {
    return (tokenBalance.token.amount >= BigDecimal(threshold) &&
        tokenBalance.fiat.amount.toDouble() >= threshold)
  }

  private fun updateTransactions(
    walletModel: TransactionsWalletModel?
  ): Observable<TransactionsWalletModel> {
    if (walletModel == null) return Observable.empty()
    val retainValue = if (walletModel.isNewWallet) null else HomeState::transactionsModelAsync
    return Observable.combineLatest(
      getCardNotifications(), getMaxBonus(), observeNetworkAndWallet()
    ) { notifications: List<CardNotification>,
        maxBonus: Double,
        transactionsWalletModel: TransactionsWalletModel ->
      createTransactionsModel(notifications, maxBonus, transactionsWalletModel)
    }
      .subscribeOn(rxSchedulers.io)
      .observeOn(rxSchedulers.main)
      .asAsyncToState(retainValue) { copy(transactionsModelAsync = it) }
      .map { walletModel }
  }

  private fun createTransactionsModel(
    notifications: List<CardNotification>,
    maxBonus: Double,
    transactionsWalletModel: TransactionsWalletModel
  ): TransactionsModel {
    return TransactionsModel(notifications, maxBonus, transactionsWalletModel)
  }

  private fun fetchTransactions(wallet: Wallet, selectedCurrency: String) {
    viewModelScope.launch {
      fetchTransactionsHistoryUseCase(
        wallet = wallet.address, limit = 4, currency = selectedCurrency
      )
        .catch { logger.log(TAG, it) }
        .collect { result ->
          when (result) {
            is ApiSuccess -> {
              newWallet = result.data.items.isEmpty()
              isLoadingTransactions = true
              _uiState.value =
                Success(
                  result.data.items
                    .map { it.toModel(defaultCurrency) }
                    .take(
                      with(result.data.items) {
                        if (size < 4 || last().txId == get(lastIndex - 1).parentTxId) size
                        else size - 1
                      })
                    .groupBy { it.date.getDay() })
            }

            is ApiFailure -> {
              isLoadingTransactions = true
            }

            is ApiException -> {
              isLoadingTransactions = true
            }

            else -> {}
          }
        }
    }
  }

  private fun getCardNotifications(): Observable<List<CardNotification>> {
    return refreshCardNotifications
      .flatMapSingle { getCardNotificationsUseCase() }
      .subscribeOn(rxSchedulers.io)
      .onErrorReturnItem(emptyList())
  }

  private fun getMaxBonus(): Observable<Double> {
    return getLevelsUseCase()
      .subscribeOn(rxSchedulers.io)
      .flatMap { (status, list) ->
        if (status == Levels.Status.OK) {
          return@flatMap Single.just(list[list.size - 1].bonus)
        }
        Single.error(IllegalStateException(status.name))
      }
      .toObservable()
  }

  fun fetchGamesListing() {
    getGamesListingUseCase()
      .subscribeOn(rxSchedulers.io)
      .scopedSubscribe({ gamesList = it }, { e -> e.printStackTrace() })
  }

  private fun verifyUserLevel() {
    findDefaultWalletUseCase()
      .flatMapObservable { wallet ->
        observeUserStatsUseCase().flatMapSingle { gamificationStats ->
          val userLevel = gamificationStats.level
          getLastShownUserLevelUseCase(wallet.address).doOnSuccess { lastShownLevel ->
            if (userLevel > lastShownLevel) {
              updateLastShownUserLevelUseCase(wallet.address, userLevel)
              backupTriggerPreferences.setTriggerState(
                walletAddress = wallet.address,
                active = true,
                triggerSource = NEW_LEVEL.toJson()
              )
            }
          }
        }
      }
      .scopedSubscribe { e -> e.printStackTrace() }
  }

  private fun handleUnreadConversationCount() {
    observeRefreshData()
      .switchMap {
        getUnreadConversationsCountEventsUseCase().subscribeOn(rxSchedulers.main)
          .doOnNext { count: Int? ->
            setState { copy(unreadMessages = (count != null && count != 0)) }
          }
      }
      .scopedSubscribe { e -> e.printStackTrace() }
  }

  private fun handleRateUsDialogVisibility() {
    shouldOpenRatingDialogUseCase()
      .observeOn(AndroidSchedulers.mainThread())
      .doOnSuccess { shouldShow ->
        sendSideEffect { HomeSideEffect.NavigateToRateUs(shouldShow) }
      }
      .scopedSubscribe { e -> e.printStackTrace() }
  }

  fun showSupportScreen() {
    commonsPreferencesDataSource.setUpdateNotificationBadge(false)
    displayChatUseCase()
  }

  fun onBalanceArrowClick(balance: WalletBalance) {
    sendSideEffect { HomeSideEffect.NavigateToBalanceDetails(balance) }
  }

  fun onSettingsClick() {
    sendSideEffect { HomeSideEffect.NavigateToSettings() }
  }

  fun onTopUpClick() {
    sendSideEffect { HomeSideEffect.NavigateToTopUp }
  }

  fun onTransferClick() {
    sendSideEffect { HomeSideEffect.NavigateToTransfer }
  }

  fun onBackupClick() {
    val model: TransactionsWalletModel? =
      state.transactionsModelAsync.value?.transactionsWalletModel
    if (model != null) {
      getWalletInfoUseCase(null, cached = false)
        .doOnSuccess { walletInfo ->
          sendSideEffect { HomeSideEffect.NavigateToBackup(walletInfo.wallet, walletInfo.name) }
          walletsEventSender.sendCreateBackupEvent(
            WalletsAnalytics.ACTION_CREATE,
            WalletsAnalytics.CONTEXT_CARD,
            WalletsAnalytics.STATUS_SUCCESS
          )
        }
        .scopedSubscribe()
    }
  }

  fun onSeeAllTransactionsClick() = sendSideEffect { HomeSideEffect.NavigateToTransactionsList }

  fun fetchPromotions() {
    getPromotionsUseCase()
      .subscribeOn(rxSchedulers.io)
      .asAsyncToState(HomeState::promotionsModelAsync) { copy(promotionsModelAsync = it) }
      .doOnNext { promotionsModel ->
        if (promotionsModel.error == null) {
          setSeenPromotionsUseCase(promotionsModel.promotions, promotionsModel.wallet.address)
        }
      }
      .repeatableScopedSubscribe(PromotionsState::promotionsModelAsync.name) { e ->
        e.printStackTrace()
      }
  }

  fun isLoadingOrIdleBalanceState(): Boolean {
    return _uiBalanceState.value == UiBalanceState.Loading ||
        _uiBalanceState.value == UiBalanceState.Idle
  }

  fun isLoadingOrIdlePromotionState(): Boolean {
    return state.promotionsModelAsync == Async.Loading(null) ||
        state.promotionsModelAsync == Async.Uninitialized
  }

  fun updateBalance(uiBalanceState: UiBalanceState) {
    _uiBalanceState.value = uiBalanceState
  }

  fun referenceSendPromotionClickEvent(): (String?, String) -> Unit {
    return compatibleAppsAnalytics::sendPromotionClickEvent
  }

  sealed class UiState {
    object Idle : UiState()

    object Loading : UiState()

    data class Success(val transactions: Map<String, List<TransactionModel>>) : UiState()
  }

  sealed class UiBalanceState {
    object Idle : UiBalanceState()

    object Loading : UiBalanceState()

    data class Success(val balance: WalletBalance) : UiBalanceState()
  }
}
