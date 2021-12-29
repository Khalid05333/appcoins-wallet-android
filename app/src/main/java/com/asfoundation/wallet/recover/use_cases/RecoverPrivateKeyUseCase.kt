package com.asfoundation.wallet.recover.use_cases

import com.asfoundation.wallet.recover.RecoverWalletResult
import com.asfoundation.wallet.recover.RecoverWalletResultMapper
import com.asfoundation.wallet.recover.SuccessfulWalletRecover
import com.asfoundation.wallet.repository.BackupRestorePreferencesRepository
import com.asfoundation.wallet.repository.PasswordStore
import com.asfoundation.wallet.repository.WalletRepositoryType
import com.asfoundation.wallet.util.CurrencyFormatUtils
import com.asfoundation.wallet.wallets.usecases.GetWalletInfoUseCase
import com.asfoundation.wallet.wallets.usecases.ObserveWalletInfoUseCase
import io.reactivex.Single

class RecoverPrivateKeyUseCase(private val walletRepository: WalletRepositoryType,
                               private val passwordStore: PasswordStore,
                               private val backupRestorePreferencesRepository: BackupRestorePreferencesRepository,
                               private val getWalletInfoUseCase: GetWalletInfoUseCase,
                               private val currencyFormatUtils: CurrencyFormatUtils) {

  operator fun invoke(privateKey: String?): Single<RecoverWalletResult> {
    return passwordStore.generatePassword()
        .flatMap { newPassword ->
          walletRepository.restorePrivateKeyToWallet(privateKey, newPassword)
        }
        .flatMap {
          RecoverWalletResultMapper(getWalletInfoUseCase, currencyFormatUtils).map(it)
        }
        .doOnSuccess {
          when (it) {
            is SuccessfulWalletRecover -> backupRestorePreferencesRepository.setWalletRestoreBackup(
                it.address)
            else -> Unit
          }
        }
  }
}