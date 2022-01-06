package com.asfoundation.wallet.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import cm.aptoide.skills.api.RoomApi
import cm.aptoide.skills.api.TicketApi
import cm.aptoide.skills.repository.*
import com.asfoundation.wallet.di.annotations.DefaultHttpClient
import com.asfoundation.wallet.entity.NetworkInfo
import com.asfoundation.wallet.interact.DefaultTokenProvider
import com.asfoundation.wallet.poa.BlockchainErrorMapper
import com.asfoundation.wallet.repository.*
import com.asfoundation.wallet.service.AccountKeystoreService
import com.asfoundation.wallet.skills.SkillsModule
import com.asfoundation.wallet.transactions.TransactionsMapper
import com.asfoundation.wallet.ui.iab.AppCoinsOperationMapper
import com.asfoundation.wallet.ui.iab.AppCoinsOperationRepository
import com.asfoundation.wallet.ui.iab.database.AppCoinsOperationDatabase
import com.asfoundation.wallet.ui.iab.raiden.MultiWalletNonceObtainer
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class RepositoryModule {

  @Singleton
  @Provides
  fun providesAppCoinsOperationRepository(
    @ApplicationContext context: Context
  ): AppCoinsOperationRepository {
    return AppCoinsOperationRepository(
      Room.databaseBuilder(
        context.applicationContext, AppCoinsOperationDatabase::class.java,
        "appcoins_operations_data"
      )
        .build()
        .appCoinsOperationDao(), AppCoinsOperationMapper()
    )
  }

  @Singleton
  @Provides
  fun provideTransactionRepository(
    networkInfo: NetworkInfo,
    accountKeystoreService: AccountKeystoreService,
    defaultTokenProvider: DefaultTokenProvider,
    nonceObtainer: MultiWalletNonceObtainer,
    transactionsNetworkRepository: OffChainTransactions,
    sharedPreferences: SharedPreferences,
    transactionsDao: TransactionsDao,
    transactionLinkIdDao: TransactionLinkIdDao
  ): TransactionRepositoryType {
    val localRepository: TransactionsRepository =
      TransactionsLocalRepository(transactionsDao, sharedPreferences, transactionLinkIdDao)
    return BackendTransactionRepository(
      networkInfo, accountKeystoreService, defaultTokenProvider,
      BlockchainErrorMapper(), nonceObtainer, Schedulers.io(), transactionsNetworkRepository,
      localRepository, TransactionMapper(), TransactionsMapper(), CompositeDisposable(),
      Schedulers.io()
    )
  }
}