/*
package com.r3.developers.musicLicense

import com.r3.developers.musicLicense.states.AgreementStatus
import com.r3.developers.musicLicense.states.LicenseAgreement
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.FlowEngine
import net.corda.v5.application.flows.InitiatedBy
import net.corda.v5.application.flows.ResponderFlow
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.ledger.utxo.UtxoLedgerService

@InitiatedBy(protocol = "update-music-license")
class UpdateLicenseResponderFlow : ResponderFlow {

    @CordaInject
    lateinit var memberLookup: MemberLookup



    override fun call(session: FlowSession) {
        val message = session.receive<LicenseAgreement>(LicenseAgreement::class.java)

    }
}*/
