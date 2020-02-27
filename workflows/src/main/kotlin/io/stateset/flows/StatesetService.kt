package io.stateset


import io.bluebank.braid.corda.services.transaction
import io.bluebank.braid.core.annotation.MethodDescription
import io.stateset.loan.Loan
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.Vault
import rx.Observable

class StatesetService(private val serviceHub: AppServiceHub) {

  @MethodDescription(
    description = "listens for loan state updates in the vault",
    returnType = Vault.Update::class
  )
  
  fun listenForCashUpdates(): Observable<Vault.Update<Loan>> {
    return serviceHub.transaction {
      serviceHub.vaultService.trackBy(Loan::class.java).updates
    }
  }
}
