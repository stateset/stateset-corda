import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.BTC
import com.r3.corda.lib.tokens.money.EUR
import com.r3.corda.lib.tokens.money.GBP
import com.r3.corda.lib.tokens.money.USD
import net.corda.core.contracts.*
import net.corda.core.contracts.Requirements.using
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.utilities.toBase58String
import java.lang.IllegalArgumentException
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table


// ************
// * Token State
// ************

@BelongsToContract(StatesetTokenContract::class)
data class StatesetTokenToken(val id: UniqueIdentifier,
                   val memo: String,
                   val fromUserId: String,
                   val holder: Party,
                   val issuer: Party,
                   val toUserId: String,
                   val amount: String,
                   val sentReceipt: Boolean?,
                   val deliveredReceipt: Boolean?,
                   val fromMe: Boolean,
                   val time: String?,
                   val tokenType: StatesetToken,
                   val transactionNumber: String
                    ) : StatesetToken(tokenType.tokenIdentifier, tokenType.fractionDigits) {

    override fun toString(): String {
        val issuerString = (issuer as? Party)?.name?.organisation ?: issuer.owningKey.toBase58String()
        val holderString = (holder as? Party)?.name?.organisation ?: holder.owningKey.toBase58String()
        return "Issuance: $issuer has issued $tokenType to $holder"
    }


    val statesetTokenType = TokenType("Stateset Token", 2)
    val issuedStatesetToken: IssuedTokenType = statesetTokenType issuedBy issuer


    val states: TokenType = TokenType(tokenIdentifier = "STE", fractionDigits = 2)
    val dollars: TokenType = USD
    val pounds: TokenType = GBP
    val euros: TokenType = EUR
    val bitcoin: TokenType = BTC


    // Ten of StatesetToken
    val tenOfStatesetTokenType = 10 of issuedStatesetToken

    // One Hundred of My Issued Token Type
    val oneHundredOfStatesetTokenType = 100 of issuedStatesetToken

    // One Thousand of My Issued Token Type
    val oneThousandOfStatesetTokenType = 1000 of issuedStatesetToken

    // Ten Thousand of My Issued Token Type
    val tenThousandOfMyStatesetTokenType = 10000 of issuedStatesetToken

    // One Hundred Thousand of My Issued Token Type
    val oneHundredThousandOfStatesetTokenType = 100000 of issuedStatesetToken

    // One Million of My Issued Token Type
    val oneMillionOfStatesetTokenType = 1000000 of issuedStatesetToken

    // Ten Million of My Issued Token Type
    val tenMillionOfStatesetTokenType = 100000000 of issuedStatesetToken

    // One Hundred Million of My Issued Token Type
    val oneHundredMillionOfStatesetTokenType = 100000000 of issuedStatesetToken

    val fungibleToken: FungibleToken = tenOfStatesetTokenType heldBy holder
}


// **********************
// * StatesetToken Contract   *
// **********************

class StatesetTokenContract : Contract {

    companion object {
        val STATESET_TOKEN_CONTRACT_ID = StatesetTokenContract::class.java.canonicalName
    }

    interface Commands : CommandData {

        class IssueToken : TypeOnlyCommandData(), Commands
        class MoveToken : TypeOnlyCommandData(), Commands
        class RedeemToken : TypeOnlyCommandData(), Commands
    }

    override fun verify(tx: LedgerTransaction) {
        val tokenInputs = tx.inputsOfType<StatesetToken>()
        val tokenOutputs = tx.outputsOfType<StatesetToken>()
        val tokenCommand = tx.commandsOfType<StatesetTokenContract.Commands>().single()

        when (tokenCommand.value) {
            is Commands.MoveToken -> requireThat {

                val tokenOutput = tokenOutputs.single()
                "the holder should be different to the recipient" using (tokenOutput.holder != tokenOutput.recipient)
                "this amount of the token should be greater than 0" using (tokenOutput.amount > 0)

            }

            is Commands.IssueToken -> requireThat {

                val tokenInput = tokenInputs.single()
                "Stateset is the only issuer of the Stateset Token" using (tokenInput.issuer)
            }


            is Commands.RedeemToken -> requireThat {

                val tokenInput = tokenInputs.single()
                val tokenOutput = tokenOutputs.single()
                "The holder is the only organization that can redeem the Stateset Token" using (tokenInput.holder != tokenOutput.holder)


            }


            else -> throw IllegalArgumentException("Unrecognised command.")

        }
    }
}


object StatesetTokenSchema

object StatesetTokenSchemaV1 : MappedSchema(StatesetTokenSchema.javaClass, 1, listOf(PersistentStatesetTokens::class.java)) {
    @Entity
    @Table(name = "stateset_tokens")
    class PersistentStatesetTokens(
            @Column(name = "id")
            var id: String = "",
            @Column(name = "memo")
            var memo: String = "",
            @Column(name = "fromUserId")
            var fromUserId: String = "",
            @Column(name = "to")
            var to: String = "",
            @Column(name = "from")
            var from: String = "",
            @Column(name = "toUserId")
            var toUserId: String = "",
            @Column(name = "sentReceipt")
            var sentReceipt: String = "",
            @Column(name = "deliveredReceipt")
            var deliveredReceipt: String = "",
            @Column(name = "fromMe")
            var fromMe: String = "",
            @Column(name = "time")
            var time: String = "",
            @Column(name = "transactionNumber")
            var transactionNumber: String = "",
            @Column(name = "linearId")
            var linearId: String = ""
    ) : PersistentState()
}