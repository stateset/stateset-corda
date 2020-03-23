import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

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