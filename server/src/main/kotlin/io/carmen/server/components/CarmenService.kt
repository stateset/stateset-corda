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

import com.github.manosbatsis.corbeans.spring.boot.corda.rpc.NodeRpcConnection
import com.github.manosbatsis.corbeans.spring.boot.corda.service.CordaNodeServiceImpl
import io.carmen.*
import io.carmen.case.CasePriority
import io.carmen.case.CaseStatus
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.getOrThrow
import org.slf4j.LoggerFactory


class CarmenService(
        nodeRpcConnection: NodeRpcConnection
) : CordaNodeServiceImpl(nodeRpcConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(CordaNodeServiceImpl::class.java)
    }

    /** Send a Message! */
    fun sendMessage(to: String, userId: String, message: String): Unit {
        val proxy = this.nodeRpcConnection.proxy

        val matches = proxy.partiesFromName(to, exactMatch = true)
        logger.debug("sendMessage, peers: {}", this.peers())
        logger.debug("sendMessage, target: {}, matches: {}", to, matches)

        val to: Party = when {
            matches.isEmpty() -> throw IllegalArgumentException("Target string \"$to\" doesn't match any nodes on the network.")
            matches.size > 1 -> throw IllegalArgumentException("Target string \"$to\"  matches multiple nodes on the network.")
            else -> matches.single()
        }
        // Start the flow, block and wait for the response.
        return proxy.startFlowDynamic(SendMessage::class.java, to, userId, message).returnValue.getOrThrow()
    }


    /** Create an Application */
    fun createApplication(applicationId: String, applicationName: String, industry: String, applicationStatus: String, partyName: String ): SignedTransaction {
        val proxy = this.nodeRpcConnection.proxy

        val matches = proxy.partiesFromName(partyName, exactMatch = true)
        logger.debug("createAccount, peers: {}", this.peers())
        logger.debug("createAccount, target: {}, matches: {}", partyName, matches)

        val processor: Party = when {
            matches.isEmpty() -> throw IllegalArgumentException("Target string \"$partyName\" doesn't match any nodes on the network.")
            matches.size > 1 -> throw IllegalArgumentException("Target string \"$partyName\"  matches multiple nodes on the network.")
            else -> matches.single()
        }
        // Start the flow, block and wait for the response.
        return proxy.startFlowDynamic(CreateAccountFlow.Controller::class.java, applicationId, applicationName, industry, applicationStatus, processor).returnValue.getOrThrow()
    }


    /** Approve an Application! */
    fun approveApplication(applicationId: String): SignedTransaction {
        val proxy = this.nodeRpcConnection.proxy

        // Start the flow, block and wait for the response.
        return proxy.startFlowDynamic(ApproveApplicationFlow::class.java, applicationId).returnValue.getOrThrow()
    }



    /** Reject an Application! */
    fun rejectApplication(applicationId: String): SignedTransaction {
        val proxy = this.nodeRpcConnection.proxy

        // Start the flow, block and wait for the response.
        return proxy.startFlowDynamic(RejectApplicationFlow::class.java, applicationId).returnValue.getOrThrow()
    }



    /** Create an Account! */
    fun createAccount(accountId: String, accountName: String, accountType: String, industry: String, phone: String, processor: String): SignedTransaction {
        val proxy = this.nodeRpcConnection.proxy

        val matches = proxy.partiesFromName(processor, exactMatch = true)
        logger.debug("createAccount, peers: {}", this.peers())
        logger.debug("createAccount, target: {}, matches: {}", processor, matches)

        val processor: Party = when {
            matches.isEmpty() -> throw IllegalArgumentException("Target string \"$processor\" doesn't match any nodes on the network.")
            matches.size > 1 -> throw IllegalArgumentException("Target string \"$processor\"  matches multiple nodes on the network.")
            else -> matches.single()
        }
        // Start the flow, block and wait for the response.
        return proxy.startFlowDynamic(CreateAccountFlow.Controller::class.java, accountId, accountName, accountType, industry, phone, processor).returnValue.getOrThrow()
    }


    /** Create a Contact! */
    fun createContact(contactId: String, firstName: String, lastName: String, phone: String, email: String, processor: String): SignedTransaction {
        val proxy = this.nodeRpcConnection.proxy

        val matches = proxy.partiesFromName(processor, exactMatch = true)
        logger.debug("sendMessage, peers: {}", this.peers())
        logger.debug("sendMessage, target: {}, matches: {}", processor, matches)

        val processor: Party = when {
            matches.isEmpty() -> throw IllegalArgumentException("Target string \"$processor\" doesn't match any nodes on the network.")
            matches.size > 1 -> throw IllegalArgumentException("Target string \"$processor\"  matches multiple nodes on the network.")
            else -> matches.single()
        }
        // Start the flow, block and wait for the response.
        return proxy.startFlowDynamic(CreateContactFlow.Controller::class.java, contactId, firstName, lastName, phone, email, processor).returnValue.getOrThrow()
    }


    /** Create a Lead */
    fun createLead(leadId: String, firstName: String, lastName: String, company: String, title: String, phone: String, email: String, country: String, processor: String): SignedTransaction {
        val proxy = this.nodeRpcConnection.proxy

        val matches = proxy.partiesFromName(processor, exactMatch = true)
        logger.debug("sendMessage, peers: {}", this.peers())
        logger.debug("sendMessage, target: {}, matches: {}", processor, matches)

        val processor: Party = when {
            matches.isEmpty() -> throw IllegalArgumentException("Target string \"$processor\" doesn't match any nodes on the network.")
            matches.size > 1 -> throw IllegalArgumentException("Target string \"$processor\"  matches multiple nodes on the network.")
            else -> matches.single()
        }
        // Start the flow, block and wait for the response.
        return proxy.startFlowDynamic(CreateLeadFlow.Controller::class.java, leadId, firstName, lastName, company, title, phone, email, country, processor).returnValue.getOrThrow()
    }


    /** Create a Case */
    fun createCase(caseId: String, caseName: String, caseNumber: String, description: String, caseStatus: CaseStatus, casePriority: CasePriority, resolver: String): SignedTransaction {
        val proxy = this.nodeRpcConnection.proxy

        val matches = proxy.partiesFromName(resolver, exactMatch = true)
        logger.debug("sendMessage, peers: {}", this.peers())
        logger.debug("sendMessage, target: {}, matches: {}", resolver, matches)

        val resolver: Party = when {
            matches.isEmpty() -> throw IllegalArgumentException("Target string \"$resolver\" doesn't match any nodes on the network.")
            matches.size > 1 -> throw IllegalArgumentException("Target string \"$resolver\"  matches multiple nodes on the network.")
            else -> matches.single()
        }
        // Start the flow, block and wait for the response.
        return proxy.startFlowDynamic(CreateCaseFlow.Initiator::class.java, caseId, caseName, caseNumber, description, caseStatus, casePriority, resolver).returnValue.getOrThrow()
    }

    /** Close a Case! */
    fun closeCase(caseId: String): SignedTransaction {
        val proxy = this.nodeRpcConnection.proxy

        // Start the flow, block and wait for the response.
        return proxy.startFlowDynamic(CloseCaseFlow::class.java, caseId).returnValue.getOrThrow()
    }

    /** Resolve a Case! */
    fun resolveCase(caseId: String): SignedTransaction {
        val proxy = this.nodeRpcConnection.proxy

        // Start the flow, block and wait for the response.
        return proxy.startFlowDynamic(ResolveCaseFlow::class.java, caseId).returnValue.getOrThrow()
    }

    /** Escalate a Case! */
    fun escalateCase(caseId: String): SignedTransaction {
        val proxy = this.nodeRpcConnection.proxy

        // Start the flow, block and wait for the response.
        return proxy.startFlowDynamic(EscalateCaseFlow::class.java, caseId).returnValue.getOrThrow()
    }


}