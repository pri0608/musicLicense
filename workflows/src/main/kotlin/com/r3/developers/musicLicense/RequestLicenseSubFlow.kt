package com.r3.developers.musicLicense

import com.r3.developers.musicLicense.states.AgreementStatus
import com.r3.developers.musicLicense.states.LicenseAgreement
import net.corda.v5.application.flows.*
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import java.math.BigDecimal
import java.util.*

@InitiatingFlow(protocol = "request-license")
class RequestLicenseSubFlow(
    private val request: CreateLicenseRequest,
    private val session: FlowSession
) : SubFlow<LicenseAgreement> {

    @CordaSerializable
    data class CreateLicenseRequest(
        val id: UUID?,
        val song: String,
        val cost: BigDecimal,
        val validTill: Date,
        val requester: MemberX500Name,
        val requestedTo: MemberX500Name,
        val otherMember: MemberX500Name
    )

    @Suspendable
    override fun call(): LicenseAgreement {
        return session.sendAndReceive(LicenseAgreement::class.java, request)
    }
}

@InitiatedBy(protocol = "request-license")
class RequestLicenseSubFlowResponder : ResponderFlow {
    @CordaInject
    lateinit var memberLookup: MemberLookup

    @Suspendable
    override fun call(session: FlowSession) {
        val request = session.receive(RequestLicenseSubFlow.CreateLicenseRequest::class.java)
        val id = request.id ?: UUID.randomUUID()
        val song = request.song
        val cost = request.cost
        val validTill = request.validTill
        val requesterName = request.requester
        val approverName = request.requestedTo
        val otherMemberName = request.otherMember

        val requester = memberLookup.lookup(requesterName)?.ledgerKeys?.first()
            ?: throw IllegalArgumentException("The requester $requesterName does not exist within the network")
        val approver = memberLookup.lookup(approverName)?.ledgerKeys?.first()
            ?: throw IllegalArgumentException("The approver $approverName does not exist within the network")
        val otherMember = memberLookup.lookup(otherMemberName)?.ledgerKeys?.first()
            ?: throw IllegalArgumentException("The observer $otherMemberName does not exist within the network")

        val newLicenseAgreement = LicenseAgreement(
            id = id,
            licenseId = null,
            proposer = requester,
            consenter = approver,
            otherMember = otherMember,
            song = song,
            cost = cost,
            validTill = validTill,
            status = AgreementStatus.REQUESTED,
            rejectionReason = null,
            rejectedBy = null,
            participants = listOf(requester, approver)
        )
        session.send(newLicenseAgreement)
    }
}

