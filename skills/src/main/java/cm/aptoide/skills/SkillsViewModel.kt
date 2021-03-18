package cm.aptoide.skills

import cm.aptoide.skills.model.RoomResponse
import cm.aptoide.skills.usecase.CreateTicketUseCase
import cm.aptoide.skills.usecase.GetRoomUseCase
import io.reactivex.Observable

class SkillsViewModel(private val createTicketUseCase: CreateTicketUseCase,
                      private val getRoomUseCase: GetRoomUseCase) {

  fun getRoom(userId: String): Observable<RoomResponse> {
    return createTicketUseCase.createTicket(userId)
        .map { ticketResponse -> ticketResponse.ticketId }
        .flatMap { ticketId -> getRoomUseCase.getRoom(ticketId) }
  }
}
