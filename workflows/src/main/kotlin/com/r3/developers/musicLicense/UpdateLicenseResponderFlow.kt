package com.r3.developers.musicLicense

import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.InitiatedBy
import net.corda.v5.application.flows.ResponderFlow
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.ledger.utxo.UtxoLedgerService

@InitiatedBy(protocol = "update-music-license")
class UpdateLicenseResponderFlow : ResponderFlow {

    @CordaInject
    lateinit var utxoLedgerService: UtxoLedgerService

    override fun call(session: FlowSession) {
        // Receive, verify, validate, sign and record the transaction sent from the initiator
        utxoLedgerService.receiveFinality(session) {
            /*
             * [receiveFinality] will automatically verify the transaction and its signatures before signing it.
             * However, just because a transaction is contractually valid doesn't mean we necessarily want to sign.
             * What if we don't want to deal with the counterparty in question, or the value is too high,
             * or we're not happy with the transaction's structure? [UtxoTransactionValidator] (the lambda created
             * here) allows us to define the additional checks. If any of these conditions are not met,
             * we will not sign the transaction - even if the transaction and its signatures are contractually valid.
             */
        }
    }
}