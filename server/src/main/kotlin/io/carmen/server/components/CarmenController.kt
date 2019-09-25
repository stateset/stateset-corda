/**
 *   Copyright 2020, Dapps Incorporated.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */


package io.carmen.server.components

import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.annotation.RestController
import sun.security.timestamp.TSResponse
import org.springframework.web.bind.annotation.PostMapping
import com.github.manosbatsis.corbeans.spring.boot.corda.config.NodeParams
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User
import java.util.*
import javax.annotation.PostConstruct
import org.springframework.security.oauth2.client.OAuth2ClientContext
import org.springframework.security.oauth2.client.OAuth2RestTemplate
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails
import org.springframework.security.oauth2.provider.OAuth2Authentication
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.CrossOrigin
import io.carmen.account.*
import io.carmen.application.Application
import io.carmen.case.*
import io.carmen.contact.*
import io.carmen.chat.*
import io.carmen.lead.*
import javax.servlet.http.HttpServletRequest


/**
 * Carmen API Endpoints
 */

@CrossOrigin(origins = ["https://dapps.ngrok.io", "https://dsoa.network", "https://camila.network", "http://localhost:8080", "http://localhost:3000", "https://statesets.com"])
@RestController
@RequestMapping("/api/{nodeName}")
class CarmenController() {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }


    protected lateinit var defaultNodeName: String

    @Autowired
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    protected lateinit var services: Map<String, CarmenService>

    @PostConstruct
    fun postConstruct() {
        // if single node config, use the only node name as default, else reserve explicitly for cordform
        defaultNodeName = if (services.keys.size == 1) services.keys.first() else NodeParams.NODENAME_CORDFORM
        logger.debug("Auto-configured RESTful services for Corda nodes:: {}, default node: {}", services.keys, defaultNodeName)
    }

    /**
     * Handle both "api/sendMessage" and "api/message/{nodeName}" by using `cordform` as the default
     * node name to support optional dedicated server per node when using `runnodes`.
     */
    fun getService(optionalNodeName: Optional<String>): CarmenService {
        val nodeName = if (optionalNodeName.isPresent) optionalNodeName.get() else defaultNodeName
        return this.services.get("${nodeName}NodeService")
                ?: throw IllegalArgumentException("Node not found: $nodeName")
    }


    /** Maps an Application to a JSON object. */

    private fun Application.toJson(): Map<String, String> {
        return kotlin.collections.mapOf(
                "agent" to agent.name.organisation,
                "provider" to provider.name.organisation,
                "counterparty" to provider.name.toString(),
                "applicationId" to applicationId,
                "applicationName" to applicationName,
                "industry" to industry,
                "applicationStatus" to applicationStatus.toString())
    }


    /** Maps an Account to a JSON object. */

    private fun Account.toJson(): Map<String, String> {
        return kotlin.collections.mapOf(
                "accountId" to accountId,
                "accountName" to accountName,
                "accountType" to accountType,
                "industry" to industry,
                "phone" to phone,
                "controller" to controller.name.organisation,
                "processor" to processor.name.organisation)
    }


    /** Maps an Contact to a JSON object. */

    private fun Contact.toJson(): Map<String, String> {
        return kotlin.collections.mapOf(
                "contactId" to contactId,
                "firstName" to firstName,
                "lastName" to lastName,
                "email" to email,
                "phone" to phone,
                "controller" to controller.name.organisation,
                "processor" to processor.name.organisation,
                "linearId" to linearId.toString())
    }


    /** Maps an Lead to a JSON object. */


    private fun Lead.toJson(): Map<String, String> {
        return kotlin.collections.mapOf(
                "leadId" to leadId,
                "firstName" to firstName,
                "lastName" to lastName,
                "email" to email,
                "phone" to phone,
                "controller" to controller.name.organisation,
                "processor" to processor.name.organisation,
                "linearId" to linearId.toString())
    }


    /** Maps an Case to a JSON object. */

    private fun Case.toJson(): Map<String, String> {
        return kotlin.collections.mapOf(
                "caseId" to caseId,
                "description" to description,
                "caseNumber" to caseNumber,
                "caseStatus" to caseStatus.toString(),
                "priority" to casePriority.toString(),
                "submitter" to submitter.toString(),
                "resolver" to resolver.toString())
    }



    /** Maps an Chat to a JSON object. */

    private fun Chat.Message.toJson(): Map<String, String> {
        return kotlin.collections.mapOf(
                "id" to id.toString(),
                "body" to body,
                "to" to to.name.organisation,
                "from" to from.name.organisation,
                "sentReceipt" to sentReceipt.toString(),
                "deliveredReceipt" to deliveredReceipt.toString(),
                "fromMe" to fromMe.toString(),
                "time" to time.toString())
    }


    /** Returns a list of existing Messages. */

    @CrossOrigin(origins = ["https://dapps.ngrok.io", "https://dsoa.network", "https://camila.network", "http://localhost:8080", "http://localhost:3000", "https://statesets.com"])
    @GetMapping( "/getMessages", produces = arrayOf("application/json"))
    @ApiOperation(value = "Get Baton Messages")
    fun getMessages(@PathVariable nodeName: Optional<String>): List<Map<String, String>> {
        val messageStateAndRefs = this.getService(nodeName).proxy().vaultQueryBy<Chat.Message>().states
        val messageStates = messageStateAndRefs.map { it.state.data }
        return messageStates.map { it.toJson() }
    }


    /** Get Messages by UserId */

    @CrossOrigin(origins = ["https://dapps.ngrok.io", "https://dsoa.network", "https://camila.network", "http://localhost:8080", "http://localhost:3000", "https://statesets.com"])
    @GetMapping("/getMessages/userId", produces = arrayOf("application/json"))
    @ApiOperation(value = "Get Baton Messages by userId")
    fun getMessagesByUserId(@PathVariable nodeName: Optional<String>): List<Map<String, String>> {
        val messageStateAndRefs = this.getService(nodeName).proxy().vaultQueryBy<Chat.Message>().states
        val messageStates = messageStateAndRefs.map { it.state.data }
        return messageStates.map { it.toJson() }
    }


    /** Returns a list of received Messages. */

    @CrossOrigin(origins = ["https://dapps.ngrok.io", "https://dsoa.network", "https://camila.network", "http://localhost:8080", "http://localhost:3000", "https://statesets.com"])
    @GetMapping("/getReceivedMessages", produces = arrayOf("application/json"))
    @ApiOperation(value = "Get Received Baton Messages")
    fun getRecievedMessages(@PathVariable nodeName: Optional<String>): List<Map<String, String>> {
        val messageStateAndRefs = this.getService(nodeName).proxy().vaultQueryBy<Chat.Message>().states
        val messageStates = messageStateAndRefs.map { it.state.data }
        return messageStates.map { it.toJson() }
    }

    /** Returns a list of Sent Messages. */

    @CrossOrigin(origins = ["https://dapps.ngrok.io", "https://dsoa.network", "https://camila.network", "http://localhost:8080", "http://localhost:3000", "https://statesets.com"])
    @GetMapping("/getSentMessages", produces = arrayOf("application/json"))
    @ApiOperation(value = "Get Sent Baton Messages")
    fun getSentMessages(@PathVariable nodeName: Optional<String>): List<Map<String, String>> {
        val messageStateAndRefs = this.getService(nodeName).proxy().vaultQueryBy<Chat.Message>().states
        val messageStates = messageStateAndRefs.map { it.state.data }
        return messageStates.map { it.toJson() }
    }


    /** Send Message*/


    @CrossOrigin(origins = ["https://dapps.ngrok.io", "https://dsoa.network", "https://camila.network", "http://localhost:8080", "http://localhost:3000", "https://statesets.com"])
    @PostMapping("/sendMessage")
    @ApiOperation(value = "Send a message to the target party")
    fun sendMessage(@PathVariable nodeName: Optional<String>,
                    @ApiParam(value = "The target party for the message")
                    @RequestParam(required = true) to: String,
                    @ApiParam(value = "The user Id for the message")
                    @RequestParam(required = true) userId: String,
                    @ApiParam(value = "The message text")
                    @RequestParam("body") body: String): ResponseEntity<Any?> {


        val (status, message) = try {

            val result = getService(nodeName).sendMessage(to, userId, body)

            HttpStatus.CREATED to mapOf<String, String>(
                    "body" to "$body",
                    "to" to "$to",
                    "userId" to "$userId"
            )

        } catch (e: Exception) {
            logger.error("Error sending message to ${to}", e)
            e.printStackTrace()
            HttpStatus.BAD_REQUEST to e.message
        }
        return ResponseEntity<Any?>(message, status)
    }


    /** Returns a list of existing Applications. */


    @CrossOrigin(origins = ["https://dapps.ngrok.io", "https://dsoa.network", "https://camila.network", "http://localhost:8080", "http://localhost:3000", "https://statesets.com"])
    @GetMapping("/getApplications")
    @ApiOperation(value = "Get Applications")
    fun getApplications(@PathVariable nodeName: Optional<String>): List<Map<String, String>> {
        val applicationStateAndRefs = this.getService(nodeName).proxy().vaultQuery(Application::class.java).states
        val applicationStates = applicationStateAndRefs.map { it.state.data }
        return applicationStates.map { it.toJson() }
    }


    /** Returns a list of existing Accounts. */


    @CrossOrigin(origins = ["https://dapps.ngrok.io", "https://dsoa.network", "https://camila.network", "http://localhost:8080", "http://localhost:3000", "https://statesets.com"])
    @GetMapping("/getAccounts")
    @ApiOperation(value = "Get Accounts")
    fun getAccounts(@PathVariable nodeName: Optional<String>): List<Map<String, String>> {
        val accountStateAndRefs = this.getService(nodeName).proxy().vaultQuery(Account::class.java).states
        val accountStates = accountStateAndRefs.map { it.state.data }
        return accountStates.map { it.toJson() }
    }




    /** Returns a list of existing Contacts. */

    @CrossOrigin(origins = ["https://dapps.ngrok.io", "https://dsoa.network", "https://camila.network", "http://localhost:8080", "http://localhost:3000", "https://statesets.com"])
    @GetMapping("/getContacts")
    @ApiOperation(value = "Get Contacts")
    fun getContacts(@PathVariable nodeName: Optional<String>): List<Map<String, String>> {
        val contactStateAndRefs = this.getService(nodeName).proxy().vaultQuery(Contact::class.java).states
        val contactStates = contactStateAndRefs.map { it.state.data }
        return contactStates.map { it.toJson() }
    }




    /** Returns a list of existing Leads. */

    @CrossOrigin(origins = ["https://dapps.ngrok.io", "https://dsoa.network", "https://camila.network", "http://localhost:8080", "http://localhost:3000", "https://statesets.com"])
    @GetMapping("/getLeads")
    @ApiOperation(value = "Get Leads")
    fun getLeads(@PathVariable nodeName: Optional<String>): List<Map<String, String>> {
        val leadStateAndRefs = this.getService(nodeName).proxy().vaultQuery(Lead::class.java).states
        val leadStates = leadStateAndRefs.map { it.state.data }
        return leadStates.map { it.toJson() }
    }



    /** Returns a list of existing Cases. */

    @CrossOrigin(origins = ["https://dapps.ngrok.io", "https://dsoa.network", "https://camila.network", "http://localhost:8080", "http://localhost:3000", "https://statesets.com"])
    @GetMapping("/getCases")
    @ApiOperation(value = "Get Cases")
    fun getCases(@PathVariable nodeName: Optional<String>): List<Map<String, String>> {
        val caseStateAndRefs = this.getService(nodeName).proxy().vaultQuery(Case::class.java).states
        val caseStates = caseStateAndRefs.map { it.state.data }
        return caseStates.map { it.toJson() }
    }


    /** Creates an Account. */


    @CrossOrigin(origins = ["https://dapps.ngrok.io", "https://dsoa.network", "https://camila.network", "http://localhost:8080", "http://localhost:3000", "https://statesets.com"])
    @PostMapping("/createAccount")
    @ApiOperation(value = "Create Account")
    fun createAccount(@PathVariable nodeName: Optional<String>,
                      @RequestParam("accountId") accountId: String,
                      @RequestParam("accountName") accountName: String,
                      @RequestParam("accountType") accountType: String,
                      @RequestParam("industry") industry: String,
                      @RequestParam("phone") phone: String,
                      @RequestParam("processor") processor: String?): ResponseEntity<Any?> {


        if (processor == null) {
            return ResponseEntity.status(TSResponse.BAD_REQUEST).body("Query parameter 'counterPartyName' missing or has wrong format.\n")
        }


        val (status, message) = try {

            val result = getService(nodeName).createAccount(accountId, accountName, accountType, industry, phone, processor)

            HttpStatus.CREATED to mapOf<String, String>(
                    "accountId" to "$accountId",
                    "accountName" to "$accountName",
                    "accountType" to "$accountType",
                    "industy" to "$industry",
                    "phone" to "$phone",
                    "processor" to "$processor"
            )

        } catch (e: Exception) {
            logger.error("Error sending account to ${processor}", e)
            e.printStackTrace()
            HttpStatus.BAD_REQUEST to e.message
        }
        return ResponseEntity<Any?>(message, status)
    }


    /** Creates a Contact. */

    @CrossOrigin(origins = ["https://dapps.ngrok.io", "https://dsoa.network", "https://camila.network", "http://localhost:8080", "http://localhost:3000", "https://statesets.com"])
    @PostMapping("/createContact")
    @ApiOperation(value = "Create Contact")
    fun createContact(@PathVariable nodeName: Optional<String>,
                      @RequestParam("contactId") contactId: String,
                      @RequestParam("firstName") firstName: String,
                      @RequestParam("lastName") lastName: String,
                      @RequestParam("email") email: String,
                      @RequestParam("phone") phone: String,
                      @RequestParam("processor") processor: String?): ResponseEntity<Any?> {


        if (processor == null) {
            return ResponseEntity.status(TSResponse.BAD_REQUEST).body("Query parameter 'counterPartyName' missing or has wrong format.\n")
        }


        val (status, message) = try {

            val result = getService(nodeName).createContact(contactId, firstName, lastName, email, phone, processor)

            HttpStatus.CREATED to mapOf<String, String>(
                    "contactId" to "$contactId",
                    "processor" to "$processor"
            )

        } catch (e: Exception) {
            logger.error("Error sending Contact to ${processor}", e)
            e.printStackTrace()
            HttpStatus.BAD_REQUEST to e.message
        }
        return ResponseEntity<Any?>(message, status)
    }


    /** Creates a Lead. */


    @CrossOrigin(origins = ["https://dapps.ngrok.io", "https://dsoa.network", "https://camila.network", "http://localhost:8080", "http://localhost:3000", "https://statesets.com"])
    @PostMapping("/createLead")
    @ApiOperation(value = "Create Lead")
    fun createLead(@PathVariable nodeName: Optional<String>,
                   @RequestParam("leadId") leadId: String,
                   @RequestParam("firstName") firstName: String,
                   @RequestParam("lastName") lastName: String,
                   @RequestParam("company") company: String,
                   @RequestParam("title") title: String,
                   @RequestParam("email") email: String,
                   @RequestParam("phone") phone: String,
                   @RequestParam("country") country: String,
                   @RequestParam("processor") processor: String?): ResponseEntity<Any?> {


        if (processor == null) {
            return ResponseEntity.status(TSResponse.BAD_REQUEST).body("Query parameter 'counterPartyName' missing or has wrong format.\n")
        }

        val (status, message) = try {

            val result = getService(nodeName).createLead(leadId, firstName, lastName, company, title, email, phone, country, processor)

            HttpStatus.CREATED to mapOf<String, String>(
                    "leadId" to "$leadId",
                    "processor" to "$processor"
            )

        } catch (e: Exception) {
            logger.error("Error sending Lead to ${processor}", e)
            e.printStackTrace()
            HttpStatus.BAD_REQUEST to e.message
        }
        return ResponseEntity<Any?>(message, status)
    }


    /** Creates a Case. */

    @CrossOrigin(origins = ["https://dapps.ngrok.io", "https://dsoa.network", "https://camila.network", "http://localhost:8080", "http://localhost:3000", "https://statesets.com"])
    @PostMapping("/createCase")
    @ApiOperation(value = "Create Case")
    fun createCase(@PathVariable nodeName: Optional<String>,
                   @RequestParam("caseId") caseId: String,
                   @RequestParam("caseName") caseName: String,
                   @RequestParam("caseNumber") caseNumber: String,
                   @RequestParam("description") description: String,
                   @RequestParam("caseStatus") caseStatus: CaseStatus,
                   @RequestParam("casePriority") casePriority: CasePriority,
                   @RequestParam("resolver") resolver: String?): ResponseEntity<Any?> {


        if (resolver == null) {
            return ResponseEntity.status(TSResponse.BAD_REQUEST).body("Query parameter 'counterPartyName' missing or has wrong format.\n")
        }


        val (status, message) = try {

            val result = getService(nodeName).createCase(caseId, caseName, caseNumber, description, caseStatus, casePriority, resolver)

            HttpStatus.CREATED to mapOf<String, String>(
                    "caseId" to "$caseId",
                    "caseName" to "$caseName",
                    "caseNumber" to "$caseNumber",
                    "description" to "$description",
                    "casePriority" to "$casePriority",
                    "caseStatus" to "$caseStatus",
                    "resolver" to "$resolver"
            )

        } catch (e: Exception) {
            logger.error("Error sending case to ${resolver}", e)
            e.printStackTrace()
            HttpStatus.BAD_REQUEST to e.message
        }
        return ResponseEntity<Any?>(message, status)
    }


    /** Close Case. */

    @CrossOrigin(origins = ["https://dapps.ngrok.io", "https://dsoa.network", "https://camila.network", "http://localhost:8080", "http://localhost:3000", "https://statesets.com"])
    @PostMapping(value = "/closeCase")
    @ApiOperation(value = "Close Case")
    fun closeCase(@PathVariable nodeName: Optional<String>, @RequestParam("caseId") caseId: String, request: HttpServletRequest): ResponseEntity<Any?> {
        val caseId = request.getParameter("caseId")
        val (status, message) = try {

            val result = getService(nodeName).closeCase(caseId)

            HttpStatus.CREATED to mapOf<String, String>(
                    "caseId" to "$caseId"
            )

        } catch (e: Exception) {
            logger.error("Error closing case ${caseId}", e)
            e.printStackTrace()
            HttpStatus.BAD_REQUEST to e.message
        }
        return ResponseEntity<Any?>(message, status)
    }

    /** Resolve Case. */

    @CrossOrigin(origins = ["https://dapps.ngrok.io", "https://dsoa.network", "https://camila.network", "http://localhost:8080", "http://localhost:3000", "https://statesets.com"])
    @PostMapping(value = "/resolveCase")
    @ApiOperation(value = "Resolve Case")
    fun resolveCase(@PathVariable nodeName: Optional<String>, @RequestParam("caseId") caseId: String, request: HttpServletRequest): ResponseEntity<Any?> {
        val caseId = request.getParameter("caseId")
        val (status, message) = try {

            val result = getService(nodeName).resolveCase(caseId)

            HttpStatus.CREATED to mapOf<String, String>(
                    "caseId" to "$caseId"
            )

        } catch (e: Exception) {
            logger.error("Error resolving case ${caseId}", e)
            e.printStackTrace()
            HttpStatus.BAD_REQUEST to e.message
        }
        return ResponseEntity<Any?>(message, status)
    }

    /** Escalate Case. */

    @CrossOrigin(origins = ["https://dapps.ngrok.io", "https://dsoa.network", "https://camila.network", "http://localhost:8080", "http://localhost:3000", "https://statesets.com"])
    @PostMapping(value = "/escalateCase")
    @ApiOperation(value = "Escalate Case")
    fun escalateCase(@PathVariable nodeName: Optional<String>, @RequestParam("caseId") caseId: String, request: HttpServletRequest): ResponseEntity<Any?> {
        val caseId = request.getParameter("caseId")
        val (status, message) = try {

            val result = getService(nodeName).escalateCase(caseId)

            HttpStatus.CREATED to mapOf<String, String>(
                    "caseId" to "$caseId"
            )

        } catch (e: Exception) {
            logger.error("Error escalating case ${caseId}", e)
            e.printStackTrace()
            HttpStatus.BAD_REQUEST to e.message
        }
        return ResponseEntity<Any?>(message, status)
    }




    /** Creates an Application. */

    @CrossOrigin(origins = ["https://dapps.ngrok.io", "https://dsoa.network", "https://camila.network", "http://localhost:8080", "http://localhost:3000", "https://statesets.com"])
    @PostMapping("/createApplication")
    @ApiOperation(value = "Create Application")
    fun createApplication(@PathVariable nodeName: Optional<String>,
                   @RequestParam("applicationId") applicationId: String,
                   @RequestParam("applicationName") applicationName: String,
                   @RequestParam("industry") industry: String,
                   @RequestParam("applicationStatus") applicationStatus: String,
                   @RequestParam("partyName") partyName: String?): ResponseEntity<Any?> {


        if (partyName == null) {
            return ResponseEntity.status(TSResponse.BAD_REQUEST).body("Query parameter 'counterPartyName' missing or has wrong format.\n")
        }


        val (status, message) = try {

            val result = getService(nodeName).createApplication(applicationId, applicationName, industry, applicationStatus, partyName)

            HttpStatus.CREATED to mapOf<String, String>(
                    "applicationd" to "$applicationId",
                    "partyName" to "$partyName"
            )

        } catch (e: Exception) {
            logger.error("Error sending case to ${partyName}", e)
            e.printStackTrace()
            HttpStatus.BAD_REQUEST to e.message
        }
        return ResponseEntity<Any?>(message, status)
    }





    /** Approve Application. */

    @CrossOrigin(origins = ["https://dapps.ngrok.io", "https://dsoa.network", "https://camila.network", "http://localhost:8080", "http://localhost:3000", "https://statesets.com"])
    @PostMapping(value = "/approveApplication")
    @ApiOperation(value = "Approve Application")
    fun approveApplication(@PathVariable nodeName: Optional<String>, @RequestParam("applicationId") applicationId: String, request: HttpServletRequest): ResponseEntity<Any?> {
        val applicationId = request.getParameter("agreementId")
        val (status, message) = try {

            val result = getService(nodeName).approveApplication(applicationId)

            HttpStatus.CREATED to mapOf<String, String>(
                    "applicationId" to "$applicationId"
            )

        } catch (e: Exception) {
            logger.error("Error approving application ${applicationId}", e)
            e.printStackTrace()
            HttpStatus.BAD_REQUEST to e.message
        }
        return ResponseEntity<Any?>(message, status)
    }

    /** Reject Application. */

    @CrossOrigin(origins = ["https://dapps.ngrok.io", "https://dsoa.network", "https://camila.network", "http://localhost:8080", "http://localhost:3000", "https://statesets.com"])
    @PostMapping(value = "/rejectApplication")
    @ApiOperation(value = "Reject Application")
    fun rejectApplication(@PathVariable nodeName: Optional<String>, @RequestParam("applicationId") applicationId: String, request: HttpServletRequest): ResponseEntity<Any?> {
        val applicationId = request.getParameter("applicationId")
        val (status, message) = try {

            val result = getService(nodeName).rejectApplication(applicationId)

            HttpStatus.CREATED to mapOf<String, String>(
                    "applicationId" to "$applicationId"
            )

        } catch (e: Exception) {
            logger.error("Error rejecting Application ${applicationId}", e)
            e.printStackTrace()
            HttpStatus.BAD_REQUEST to e.message
        }
        return ResponseEntity<Any?>(message, status)
    }
































    /*

    /** Send UPI Payment */

    @CrossOrigin(origins = ["https://dapps.ngrok.io", "https://dsoa.network"])
    @PostMapping(value = "/pay")
    fun sendPayment(@RequestParam("pa") pa: String,
                    @RequestParam("pn") pn: String,
                    @RequestParam("mc") mc: String,
                    @RequestParam("tid") tid: String,
                    @RequestParam("tr") tr: String,
                    @RequestParam("tn") tn: String,
                    @RequestParam("am") am: String,
                    @RequestParam("mam") mam: String,
                    @RequestParam("cu") cu: String,
                    @RequestParam("url") url : String): ResponseEntity<Any?> {

        if (tid == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Query parameter 'tid' can not be null.\n")
        }

        if (pn == null) {
            return ResponseEntity.status(TSResponse.BAD_REQUEST).body("Query parameter 'pn' missing or has wrong format.\n")
        }

        val counterparty = CordaX500Name.parse(pn)


        val pn = proxy.wellKnownPartyFromX500Name(counterparty)
                ?: return ResponseEntity.status(TSResponse.BAD_REQUEST).body("Party named $pn cannot be found.\n")

        val (status, message) = try {


            val flowHandle = proxy.startFlowDynamic(SendPaymentFlow.InitiatePaymentRequest::class.java, pa, pn, mc, tid, tr, tn, am, mam, cu, url)

            val result = flowHandle.use { it.returnValue.getOrThrow() }

            HttpStatus.CREATED to "Payment sent."

        } catch (e: Exception) {
            HttpStatus.BAD_REQUEST to e.message
        }
        logger.info(message)
        return ResponseEntity<Any?>(message, status)
    }


/** Send Proxy Re-encryption Policy */

@CrossOrigin(origins = ["https://dapps.ngrok.io", "https://dsoa.network"])
@PostMapping(value = "/policy")
fun sendPolicy(@RequestParam("alice") alice: String,
                @RequestParam("enrico") enrico: String,
                @RequestParam("bob") bob: String,
                @RequestParam("policyName") policyName: String,
                @RequestParam("policyExpirationDate") policyExpirationDate: String,
                @RequestParam("policyPassword") policyPassword: String,
                @RequestParam("policyId") policyId: String): ResponseEntity<Any?> {

    if (policyId == null) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Query parameter 'tid' can not be null.\n")
    }

    if (bob == null) {
        return ResponseEntity.status(TSResponse.BAD_REQUEST).body("Query parameter 'pn' missing or has wrong format.\n")
    }

    val counterparty = CordaX500Name.parse(bob)

    val bob = proxy.wellKnownPartyFromX500Name(counterparty)
            ?: return ResponseEntity.status(TSResponse.BAD_REQUEST).body("Party named $bob cannot be found.\n")

    val (status, message) = try {


        val flowHandle = proxy.startFlowDynamic(SendPolicyFlow.InitiatePolicyRequest::class.java, alice, enrico, bob, policyName, policyExpirationDate, policyPassword, policyId)

        val result = flowHandle.use { it.returnValue.getOrThrow() }

        HttpStatus.CREATED to "Payment sent."

    } catch (e: Exception) {
        HttpStatus.BAD_REQUEST to e.message
    }
    logger.info(message)
    return ResponseEntity<Any?>(message, status)
}

*/
}
