/*
package com.r3.developers.musicLicense

import com.r3.developers.musicLicense.states.LicenseAgreement
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.FlowEngine
import net.corda.v5.application.flows.InitiatedBy
import net.corda.v5.application.flows.ResponderFlow
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.utxo.UtxoLedgerService

@InitiatedBy(protocol = "create-and-issue-music-license")
class CreateAndIssueLicenseResponderFlow : ResponderFlow {

    @CordaInject
    lateinit var utxoLedgerService: UtxoLedgerService

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @Suspendable
    override fun call(session: FlowSession) {
        val request = session.receive(CreateAndIssueLicenseFlow.CreateAndIssueLicenseRequest::class.java)

        val licenseAgreement: LicenseAgreement = flowEngine.subFlow(RequestLicenseSubFlow(request, session))

        session.send(licenseAgreement)
    }
}*/
