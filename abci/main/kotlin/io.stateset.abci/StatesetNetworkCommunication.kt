package io.stateset.abci

import sun.plugin2.util.PojoUtil.toJson
import java.util.concurrent.TimeUnit
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.HashMap
import jetbrains.exodus.ArrayByteIterable
import jetbrains.exodus.env.Environment
import jetbrains.exodus.env.Store
import jetbrains.exodus.env.StoreConfig
import jetbrains.exodus.env.Transaction
import io.grpc.stub.StreamObserver
import types.ABCIApplicationGrpc
import types.Types.*
import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import com.google.protobuf.Method


class StatesetNetworkCommunication : ABCIApplicationGrpc.ABCIApplicationImplBase() {

    fun receivedDeliverTx(req: RequestDeliverTx): ResponseDeliverTx {

        val byteArray = req.getTx().toByteArray()
        val msg = gson.fromJson(String(byteArray), Message::class.java)
        val account = gson.fromJson(String(byteArray), Account::class.java)
        val lead = gson.fromJson(String(byteArray), Lead::class.java)
        val contact = gson.fromJson(String(byteArrayO), Contact::class.java)
        val application = gson.fromJson(String(byteArray), Application::class.java)
        val approval = gson.fromJson(String(byteArray), Approval::class.java)
        val proposal = gson.fromJson(String(byteArray), Proposal::class.java)
        val agreement = gson.fromJson(String(byteArray), Agreement::class.java)
        val loan = gson.fromJson(String(byteArray), Loan::class.java)
        val invoice = gson.fromJson(String(byteArray), Invoice::class.java)
        val case = gson.fromJson(String(byteArray), Case::class.java)
        val token = gson.fromJson(String(byteAray), Token::class.java)


        return ResponseDeliverTx.newBuilder().setCode(CodeType.OK).build()
    }

    fun requestCheckTx(req: RequestCheckTx): ResponseCheckTx {
        return ResponseCheckTx.newBuilder().setCode(CodeType.OK).build()
    }

    fun requestCommit(requestCommit: RequestCommit): ResponseCommit {
        return ResponseCommit.newBuilder().setData(ByteString.copyFrom(ByteUtil.toBytes(hashCount))).build()
    }

    fun sendMessage(m: Message) {
        val rpc = StringParam(Method.BROADCAST_TX_ASYNC, gson.toJson(m).getBytes())
        proto.sendMessage(rpc, { e ->

        })
    }

    fun sendLead(l: Lead) {
        val rpc = StringParam(Method.BROADCAST_TX_ASYNC, gson.toJson(m).getBytes())
        proto.sendLead(rpc, { e ->

        })
    }

    fun sendAccount(a: Account) {
        val rpc = StringParam(Method.BROADCAST_TX_ASYNC, gson.toJson(a).getBytes())
        proto.sendAccount(rpc, { e ->

        })
    }

    fun sendContact(c: Contact) {
        val rpc = StringParam(Method.BROADCAST_TX_ASYNC, gson.toJson(m).getBytes())
        proto.sendContact(rpc, { e ->

        })
    }

    fun sendProposal(p: Proposal) {
        val rpc = StringParam(Method.BROADCAST_TX_ASYNC, gson.toJson(m).getBytes())
        proto.sendProposal(rpc, { e ->

        })
    }

    fun sendApplication(a: Application) {
        val rpc = StringParam(Method.BROADCAST_TX_ASYNC, gson.toJson(m).getBytes())
        proto.sendApplication(rpc, { e ->

        })
    }

    fun sendCase(c: Case) {
        val rpc = StringParam(Method.BROADCAST_TX_ASYNC, gson.toJson(m).getBytes())
        proto.sendCase(rpc, { e ->

        })
    }


    fun sendAgreement(a: Agreement) {
        val rpc = StringParam(Method.BROADCAST_TX_ASYNC, gson.toJson(m).getBytes())
        proto.sendAgreement(rpc, { e ->

        })
    }

}