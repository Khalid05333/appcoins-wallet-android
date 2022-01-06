package cm.aptoide.skills.usecase

import cm.aptoide.skills.interfaces.EwtObtainer
import cm.aptoide.skills.repository.LoginRepository
import io.reactivex.Single
import javax.inject.Inject

class LoginUseCase @Inject constructor(private val ewtObtainer: EwtObtainer,
                   private val loginRepository: LoginRepository) {

  fun login(roomId: String, ticketId: String): Single<String> {
    return ewtObtainer.getEWT()
        .flatMap {
          loginRepository.login(it, roomId, ticketId)
              .map { it.token }
        }
  }
}