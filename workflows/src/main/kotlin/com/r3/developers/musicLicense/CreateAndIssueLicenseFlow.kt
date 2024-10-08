package com.r3.developers.musicLicense

import com.r3.developers.musicLicense.contracts.MusicLicenseCommands
import com.r3.developers.musicLicense.states.LicenseAgreement
import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.utxo.UtxoLedgerService
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@InitiatingFlow(protocol = "create-and-issue-music-license")
class CreateAndIssueLicenseFlow : ClientStartableFlow {

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var notaryLookup: NotaryLookup

    @CordaInject
    lateinit var utxoLedgerService: UtxoLedgerService

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @CordaInject
    lateinit var memberLookup: MemberLookup

    @CordaSerializable
    data class CreateAndIssueLicenseRequest(
        val song: String,
        val cost: BigDecimal,
        val validTill: Date,
        val requestedTo: MemberX500Name,
        val otherMember: MemberX500Name
        )

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {
        val request = requestBody.getRequestBodyAs(
            jsonMarshallingService,
            CreateAndIssueLicenseRequest::class.java)
        // Retrieve the notaries public key (this will change)
        val notaryInfo = notaryLookup.notaryServices.single()
        val createLicenseRequest = RequestLicenseSubFlow.CreateLicenseRequest(null, request.song, request.cost, request.validTill, memberLookup.myInfo().name, request.requestedTo, request.otherMember)
        val session: FlowSession = flowMessaging.initiateFlow(request.requestedTo)
        val licenseAgreement: LicenseAgreement = flowEngine.subFlow(RequestLicenseSubFlow(createLicenseRequest, session))

        val transaction = utxoLedgerService.createTransactionBuilder()
            .setNotary(notaryInfo.name)
            .addOutputState(licenseAgreement)
            .addCommand(MusicLicenseCommands.Request())
            .setTimeWindowUntil(Instant.now().plus(1, ChronoUnit.DAYS))
            .addSignatories(listOf(licenseAgreement.proposer, licenseAgreement.consenter))
            .toSignedTransaction()

        val finalizeSession: FlowSession = flowMessaging.initiateFlow(request.requestedTo)

        return try {
            // Send the transaction and state to the counterparty and let them sign it
            // Then notarise and record the transaction in both parties' vaults.
            flowEngine.subFlow(FinalizeTransactionSubFlow(transaction,listOf(finalizeSession)))
            "The licenseAgreement has been created with id ${licenseAgreement.id} and status ${licenseAgreement.status}, requested to approver ${request.requestedTo}"
        } catch (ex: Exception) {
            "Flow failed, message: ${ex.message}"
        }

    }
}