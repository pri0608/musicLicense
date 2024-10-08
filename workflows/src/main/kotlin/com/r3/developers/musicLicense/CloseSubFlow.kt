package com.r3.developers.musicLicense

import com.r3.developers.musicLicense.states.AgreementStatus
import com.r3.developers.musicLicense.states.LicenseAgreement
import net.corda.v5.application.flows.InitiatedBy
import net.corda.v5.application.flows.InitiatingFlow
import net.corda.v5.application.flows.ResponderFlow
import net.corda.v5.application.flows.SubFlow
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.Suspendable

@InitiatingFlow(protocol = "completion-flow")
class CloseSubFlow(
    val licenseAgreement: LicenseAgreement,
    val session: FlowSession
) : SubFlow<LicenseAgreement> {

    @Suspendable
    override fun call(): LicenseAgreement {
        return session.sendAndReceive(LicenseAgreement::class.java, licenseAgreement)
    }
}

@InitiatedBy(protocol = "completion-flow")
class CompleteResponderFlow: ResponderFlow {

    @Suspendable
    override fun call(session: FlowSession) {
        val licenseAgreement = session.receive(LicenseAgreement::class.java)
        session.send(licenseAgreement.updateAgreementStatus(AgreementStatus.CLOSED))
    }

}