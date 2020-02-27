 @StartableByRPC
    @InitiatingFlow
    class SellStockFlow(val stock: Stock, val newHolder: Party) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            TODO("Implement delivery versus payment logic.")
        }
    }