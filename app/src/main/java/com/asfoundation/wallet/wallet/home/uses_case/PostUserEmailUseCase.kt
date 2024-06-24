package com.asfoundation.wallet.wallet.home.uses_case

import com.appcoins.wallet.core.network.base.EwtAuthenticatorService
import com.appcoins.wallet.core.utils.android_common.RxSchedulers
import com.appcoins.wallet.feature.walletInfo.data.email.EmailRepository
import com.appcoins.wallet.sharedpreferences.EmailPreferencesDataSource
import io.reactivex.Completable
import javax.inject.Inject

class PostUserEmailUseCase
@Inject
constructor(
  private val ewtObtainer: EwtAuthenticatorService,
  private val emailRepository: EmailRepository,
  private val emailPreferencesDataSource: EmailPreferencesDataSource,
  private val schedulers: RxSchedulers
) {
  operator fun invoke(email: String): Completable {
    return Completable.defer {
      ewtObtainer.getEwtAuthentication()
        .observeOn(schedulers.io)
        .flatMapCompletable { ewt ->
          emailRepository.postUserEmail(ewt, email)
        }
    }
  }
}