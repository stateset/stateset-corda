import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction


// Sell Stock Flow Between two Parties

@StartableByRPC
    @InitiatingFlow
    class SellStockFlow(val stock: Stock, val newHolder: Party) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            TODO("Implement delivery versus payment logic.")
        }
    }