

import com.r3.corda.lib.tokens.contracts.EvolvableTokenContract;
import net.corda.core.transactions.LedgerTransaction;
import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearPointer;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;
import java.util.Currency;
import java.util.List;



// Example stock token. This could be used for any stock type.
@BelongsToContract(StockContract::class)
data class Stock(
      override val symbol: String,
      override val name: String,
      override val displayTokenSize: BigDecimal,
      override val shares: Amount<TokenType>,
      override val valueInUSD: Long,
      override val maintainers: List<Party>,
      override val linearId: UniqueIdentifier = UniqueIdentifier()
) : EvolvableTokenType() {
  // Things one can do with stock.
  fun dividend(amount: TokenType): Stock
}

// Define MEGA CORP Stock reference data then issue it on the ledger (not shown).
val statesetCorpStock = Stock(
    symbol = "STST",
    name = "STATESET CORP",
    displayTokenSize = BigDecimal.ONE,
    maintainers = listOf(REF_DATA_MAINTAINER)
)

// Create a pointer to the MEGA CORP stock.
val stockPointer: TokenPointer<Stock> = statesetCorpStock.toPointer()
// Create an issued token type for the MEGA CORP stock.
val issuedStatesetCorpStock = stockPointer issuedBy CUSTODIAN
// Create a FungibleToken for some amount of MEGA CORP stock.
val stockTokensForAlice = 1_000 of issuedStatesetCorpStock heldBy ALICE

// MEGA CORP announces a dividend and commits the updated token type definition to ledger.
stateseetCorpStock.dividend(10.POUNDS)

// MEGA CORP distributes the updated token type definition to those who require it.

// Resolving the pointer gives us the updated token type definition.
val resolved = stockPointer.resolve(services)



/*
*  StockContract governs the evolution of Stock State token. Evolvable tokens must extend the EvolvableTokenContract abstract class, it defines the
*  additionalCreateChecks and additionalCreateChecks method to add custom logic to validate while creation adn updation of evolvable tokens respectively.
* */
public class StockContract : EvolvableTokenContract(), Contract {


    override fun additionalCreateChecks(tx: LedgerTransaction) {
      
        val newStock = tx.outputStates.single() as Stock
        newStock.apply {
            require(valuation > Amount.zero(valuation.token)) { "Valuation must be greater than zero." }
        }
    }

    override fun additionalUpdateChecks(tx: LedgerTransaction) {
        val oldHouse = tx.inputStates.single() as House
        val newHouse = tx.outputStates.single() as House
        require(oldHouse.address == newHouse.address) { "The address cannot change." }
        require(newHouse.valuation > Amount.zero(newHouse.valuation.token)) { "Valuation must be greater than zero." }
    }

    /* This method returns a TokenPointer by using the linear Id of the evolvable state */
    public TokenPointer<Stock> toPointer(){
        LinearPointer<Stock> linearPointer = new LinearPointer<>(linearId, Stock::class.java);
        return new TokenPointer<>(linearPointer, fractionDigits);
    }