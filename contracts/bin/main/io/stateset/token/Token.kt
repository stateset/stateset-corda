import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.BTC
import com.r3.corda.lib.tokens.money.EUR
import com.r3.corda.lib.tokens.money.GBP
import net.corda.core.contracts.Amount
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import net.corda.core.utilities.toBase58String


@CordaSerializable
data class IssuedTokenType(
        val issuer: Party,
        val tokenType: StatesetTokenType
) : StatesetTokenType(tokenType.tokenIdentifier, tokenType.fractionDigits) {


    override fun toString(): String {
        val issuerString = (issuer as? Party)?.name?.organisation ?: issuer.owningKey.toBase58String()
        val holderString = (holder as? Party)?.name?.organisation ?: holder.owningKey.toBase58String()
        return "Issuance: $issuer has issued $tokenType to $holder"
    }
}

val pounds: TokenType = GBP
val euros: TokenType = EUR
val bitcoin: TokenType = BTC

val StatesetTokenType: TokenType = TokenType(tokenIdentifier = "STEIL", fractionDigits = 2)

IssueTokens(10000 of StatesetTokenType, issue, holder)

// Issuer and Holder of the Token Types
val issuer: Party = Stateset
val holder: Party = PrincetonCapital

// Stateset Token Type
val statesetTokenType = StatesetTokenType("STEIL", 2)
val myIssuedTokenType: IssuedTokenType = statesetTokenType issuedBy issuer

// Ten of my Issued Token Type
val tenOfMyIssuedTokenType = 10 of myIssuedTokenType
val tenStates: Amount<IssuedTokenType> = 10 of STEIL issuedBy issuer

// One Hundred of My Issued Token Type
val oneHundredOfMyIssuedTokenType = 100 of myIssuedTokenType

// One Thousand of My Issued Token Type
val oneThousandOfMyIssuedTokenType = 1000 of myIssuedTokenType

// Ten Thousand of My Issued Token Type
val tenThousandOfMyIssuedTokenType = 10000 of myIssuedTokenType

// One Hundred Thousand of My Issued Token Type
val oneHundredThousandOfMyIssuedTokenType = 100000 of myIssuedTokenType

// One Million of My Issued Token Type
val oneMillionOfMyIssuedTokenType = 1000000 of myIssuedTokenType

// Ten Million of My Issued Token Type
val tenMillionOfMyIssuedTokenType = 100000000 of myIssuedTokenType

// One Hundred Million of My Issued Token Type
val oneHundredMillionOfMyIssuedTokenType = 100000000 of myIssuedTokenType