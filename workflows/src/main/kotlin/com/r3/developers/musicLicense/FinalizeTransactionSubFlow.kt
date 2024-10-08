package com.r3.developers.musicLicense

import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.InitiatedBy
import net.corda.v5.application.flows.InitiatingFlow
import net.corda.v5.application.flows.ResponderFlow
import net.corda.v5.application.flows.SubFlow
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.utxo.UtxoLedgerService
import net.corda.v5.ledger.utxo.transaction.UtxoSignedTransaction
import org.slf4j.LoggerFactory
import java.time.Instant

@InitiatingFlow(protocol = "finalize")
internal class FinalizeTransactionSubFlow(
private val transaction: UtxoSignedTransaction,
private val sessions: List<FlowSession>
): SubFlow<UtxoSignedTransaction> {

    private companion object {
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
        const val FLOW_CALL = "FinalizeTransactionSubFlow.call() called."
    }

    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    @Suspendable
    override fun call(): UtxoSignedTransaction {
        log.info(FLOW_CALL)
        return ledgerService.finalize(transaction, sessions).transaction
    }

}

@InitiatedBy(protocol = "finalize")
internal class FinalizeTransactionSubFlowResponder(): ResponderFlow {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
        const val FLOW_CALL = "FinalizeTransactionSubFlowResponder.call() called."
    }

    @CordaInject
    lateinit var utxoLedgerService: UtxoLedgerService

    @Suspendable
    override fun call(session: FlowSession) {
        log.info(FLOW_CALL)
        utxoLedgerService.receiveFinality(session){
            // Implement any pre-signing checks here...
            transaction -> transaction.timeWindow.until.isAfter(Instant.now())
        }
    }
}