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
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.utxo.UtxoLedgerService
import org.slf4j.LoggerFactory
import java.security.PublicKey
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@InitiatingFlow(protocol = "update-music-license")
class UpdateLicenseFlow: ClientStartableFlow {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var notaryLookup: NotaryLookup

    @CordaInject
    lateinit var memberLookup: MemberLookup

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    // Injects the UtxoLedgerService to enable the flow to make use of the Ledger API.
    @CordaInject
    lateinit var utxoLedgerService: UtxoLedgerService

    private data class UpdateLicenseRequest(
        val id: UUID,
        val status: AgreementStatus,
        val rejectedBy: PublicKey?,
        val rejectionReason: String?,
        val requester: MemberX500Name,
        val approver: MemberX500Name
    )

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {
        val request = requestBody.getRequestBodyAs(jsonMarshallingService, UpdateLicenseRequest::class.java)

        // Look up the latest unconsumed LicenseAgreement with the given id.
        // Note, this code brings all unconsumed states back, then filters them.
        // This is an inefficient way to perform this operation when there are a large number of states.
        // Note, you will get this error if you input an id which has no corresponding LicenseAgreement (common error).
        val inputLicenseState = utxoLedgerService.findUnconsumedStatesByExactType(LicenseAgreement::class.java, 100, Instant.now()).results.singleOrNull {it.state.contractState.id == request.id}
            ?: throw CordaRuntimeException("Did not find an unique unconsumed LicenseAgreement state with id ${request.id}")

        log.info("UpdateLicenseFlow: state id: ${inputLicenseState.state.contractState.id}")
        log.info("UpdateLicenseFlow: state song: ${inputLicenseState.state.contractState.song}")
        log.info("UpdateLicenseFlow: state participants: ${inputLicenseState.state.contractState.participants}")

        val notary = notaryLookup.notaryServices.single()
        val requesterName = request.requester
        val requester = memberLookup.lookup(requesterName)?.ledgerKeys?.first()
            ?: throw IllegalArgumentException("The requester does not exist within the network")
        val approverName = request.approver
        val approver = memberLookup.lookup(approverName)?.ledgerKeys?.first()
            ?: throw IllegalArgumentException("The approver does not exist within the network")
        val inputLicense = inputLicenseState.state.contractState

        val updatedLicenseAgreement = inputLicense.updateLicense(request.status, request.rejectionReason, request.rejectedBy)

        val command = if(request.status == AgreementStatus.AGREED) MusicLicenseCommands.Agree() else MusicLicenseCommands.Reject()

        log.info("UpdateLicenseFlow: command: ${command}")

        val transaction = utxoLedgerService.createTransactionBuilder()
            .setNotary(notary.name)
            .addInputState(inputLicenseState.ref)
            .addOutputState(updatedLicenseAgreement)
            .addCommand(command)
            .setTimeWindowUntil(Instant.now().plus(1, ChronoUnit.DAYS))
            .addSignatories(listOf(requester, approver))
            .toSignedTransaction()

        val session = flowMessaging.initiateFlow(approverName)

        return try {
            // Send the transaction and state to the counterparty and let them sign it
            // Then notarise and record the transaction in both parties' vaults.
            utxoLedgerService.finalize(transaction, listOf(session))
            updatedLicenseAgreement.id.toString()
        } catch (ex: Exception) {
            "Flow failed, message: ${ex.message}"
        }
    }
}


