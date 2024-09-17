package com.r3.developers.musicLicense

import com.r3.developers.musicLicense.contracts.MusicLicenseCommands
import com.r3.developers.musicLicense.states.AgreementStatus
import com.r3.developers.musicLicense.states.LicenseAgreement
import net.corda.v5.application.flows.ClientRequestBody
import net.corda.v5.application.flows.ClientStartableFlow
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.InitiatingFlow
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
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
    lateinit var memberLookup: MemberLookup

    @CordaInject
    lateinit var utxoLedgerService: UtxoLedgerService

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    private data class CreateAndIssueLicenseRequest(
        val song: String,
        val cost: BigDecimal,
        val validTill: Date,
        val requester: MemberX500Name,
        val requestedTo: MemberX500Name
        )

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {
        val request = requestBody.getRequestBodyAs(
            jsonMarshallingService,
            CreateAndIssueLicenseRequest::class.java)
        val song = request.song
        val cost = request.cost
        val validTill = request.validTill
        val requesterName = request.requester
        val approverName = request.requestedTo
        // Retrieve the notaries public key (this will change)
        val notaryInfo = notaryLookup.notaryServices.single()
        val requester = memberLookup.lookup(requesterName)?.ledgerKeys?.first()
            ?: throw IllegalArgumentException("The holder $requesterName does not exist within the network")
        val approver = memberLookup.lookup(approverName)?.ledgerKeys?.first()
            ?: throw IllegalArgumentException("The holder $approverName does not exist within the network")

        val newLicenseAgreement = LicenseAgreement(
            id = UUID.randomUUID(),
            proposer = requester,
            consenter = approver,
            song = song,
            cost = cost,
            validTill = validTill,
            status = AgreementStatus.REQUESTED,
            rejectionReason = null,
            rejectedBy = null,
            participants = listOf(requester, approver),
        )

        val transaction = utxoLedgerService.createTransactionBuilder()
            .setNotary(notaryInfo.name)
            .addOutputState(newLicenseAgreement)
            .addCommand(MusicLicenseCommands.Request())
            .setTimeWindowUntil(Instant.now().plus(1, ChronoUnit.DAYS))
            .addSignatories(listOf(requester, approver))
            .toSignedTransaction()

        val session = flowMessaging.initiateFlow(requesterName)

        return try {
            // Send the transaction and state to the counterparty and let them sign it
            // Then notarise and record the transaction in both parties' vaults.
            utxoLedgerService.finalize(transaction, listOf(session))
            newLicenseAgreement.id.toString()
        } catch (ex: Exception) {
            "Flow failed, message: ${ex.message}"
        }

    }
}