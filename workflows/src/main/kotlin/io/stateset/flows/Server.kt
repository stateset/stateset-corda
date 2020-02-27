package io.stateset

import io.bluebank.braid.corda.BraidConfig
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken

@CordaService
class Server(private val serviceHub: AppServiceHub) : SingletonSerializeAsToken() {

  init {
    BraidConfig.fromResource(configFileName)?.bootstrap()
  }

  private fun BraidConfig.bootstrap() {
    this.withFlow("activateAgreement", ActivateAgreementFlow::class)
      .withFlow("amendAgreement", AmendAgreementFlow::class)
      .withFlow("expireAgreement", ExpireAgreementFlow::class)
      .withFlow("renewAgreement", RenewAgreementFlow::class)
      .withFlow("createAgreement", CreateAgreementFlow::class)
      .withFlow("createAccount", CreateAccountFlow::class)
      .withFlow("createContact", CreateContactFlow::class)
      .withFlow("createLead", CreateLeadFlow::class)
      .withFlow("createCase", CreateCaseFlow::class)
      .withFlow("closeCase", CloseCaseFlow::class)
      .withFlow("resolveCase", ResolveCaseFlow::class)
      .withFlow("escalateCase", EscalateCaseFlow::class)
      .withFlow("sendMessage", SendMessageFlow::class)
      .withFlow("createApproval", CreateApprovalFlow::class)
      .withFlow("approve", ApproveFlow::class)
      .withFlow("reject", RejectFlow::class)
      .withFlow("createInvoice", CreateInvoiceFlow::class)
      .withFlow("createLoan", CreateLoanFlow::class)
      .withFlow("createApplication", CreateApplicationFlow::class)
      .withFlow("approveApplication", ApproveApplicationFlow::class)
      .withFlow("rejectApplication", RejectApplication::class)
      .withFlow("reviewedApplication", ReviewApplication::class)
      .withFlow("payInvoice", PayInvoice::class)
      .withService("StatesetService", StatesetService(serviceHub))
      .withAuthConstructor { StatesetAuthProvider() }
      .bootstrapBraid(serviceHub)
  }

  /**
   * config file name based on the node legal identity
   */

  private val configFileName: String
    get() {
      val name = serviceHub.myInfo.legalIdentities.first().name.organisation
      return "braid-$name.json"
    }
}
