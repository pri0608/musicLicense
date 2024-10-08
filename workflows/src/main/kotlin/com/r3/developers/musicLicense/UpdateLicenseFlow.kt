package com.r3.developers.musicLicense

import com.r3.developers.musicLicense.contracts.MusicLicenseCommands
import com.r3.developers.musicLicense.states.AgreementStatus
import com.r3.developers.musicLicense.states.LicenseAgreement
import com.r3.developers.musicLicense.states.LicenseStatus
import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.utxo.UtxoLedgerService
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@InitiatingFlow(protocol = "update-music-license")
class UpdateLicenseFlow : ClientStartableFlow {

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

    @CordaInject
    lateinit var flowEngine: FlowEngine

    // Injects the UtxoLedgerService to enable the flow to make use of the Ledger API.
    @CordaInject
    lateinit var utxoLedgerService: UtxoLedgerService

    private data class UpdateLicenseRequest(
        val id: UUID,
        val status: AgreementStatus,
        val rejectedBy: MemberX500Name?,
        val rejectionReason: String?
    )

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {
        val request = requestBody.getRequestBodyAs(jsonMarshallingService, UpdateLicenseRequest::class.java)

        log.info("Unconsumed states" + utxoLedgerService.findUnconsumedStatesByExactType(
            LicenseAgreement::class.java,
            100,
            Instant.now()
        ).results)
        // Look up the latest unconsumed LicenseAgreement with the given id.
        // Note, this code brings all unconsumed states back, then filters them.
        // This is an inefficient way to perform this operation when there are a large number of states.
        // Note, you will get this error if you input an id which has no corresponding LicenseAgreement (common error).
        val inputLicenseState = utxoLedgerService.findUnconsumedStatesByExactType(
            LicenseAgreement::class.java,
            100,
            Instant.now()
        ).results.singleOrNull { it.state.contractState.id == request.id }
            ?: throw CordaRuntimeException("Did not find a unique unconsumed LicenseAgreement state with id ${request.id}")

        log.info("UpdateLicenseFlow: state id: ${inputLicenseState.state.contractState.id}")
        log.info("UpdateLicenseFlow: state song: ${inputLicenseState.state.contractState.song}")
        log.info("UpdateLicenseFlow: state participants: ${inputLicenseState.state.contractState.participants}")

        val notary = notaryLookup.notaryServices.single()
        val approver = memberLookup.myInfo().ledgerKeys.first()
        val approverName = memberLookup.myInfo().name
        val inputLicenseAgreement = inputLicenseState.state.contractState
        val requester = inputLicenseAgreement.proposer
        val requesterName = memberLookup.lookup(requester)?.name
            ?: throw IllegalArgumentException("The requester $requester does not exist within the network")
        val otherMember = inputLicenseAgreement.otherMember
        val otherMemberName = memberLookup.lookup(otherMember)?.name
            ?: throw IllegalArgumentException("The member $otherMember does not exist within the network")
        val rejectedByParty = request.rejectedBy?.let { memberLookup.lookup(it)?.ledgerKeys?.first() }
        var updatedLicenseAgreement =
            inputLicenseAgreement.updateLicenseAgreement(request.status, request.rejectionReason, rejectedByParty)
        log.info("UpdateLicenseFlow: updatedLicenseAgreement id: ${updatedLicenseAgreement.id}")

        val command = if (request.status == AgreementStatus.AGREED_AND_REQUESTED)
            MusicLicenseCommands.AgreeAndRequest()
        else if (request.status == AgreementStatus.AGREED)
            MusicLicenseCommands.Agree()
        else if (request.status == AgreementStatus.REJECTED)
            MusicLicenseCommands.Reject()
        else
            null

        if(command == null)
            throw IllegalArgumentException("The command $command is undefined")
        log.info("UpdateLicenseFlow: command: ${command}")

        var outputMessage = ""
        var participants = listOf(requester, approver)
        var initiatorName = requesterName
        when (updatedLicenseAgreement.status) {
           AgreementStatus.AGREED_AND_REQUESTED -> {
                val createAndIssueLicenseRequest = RequestLicenseSubFlow.CreateLicenseRequest(
                    updatedLicenseAgreement.id,
                    updatedLicenseAgreement.song,
                    updatedLicenseAgreement.cost,
                    updatedLicenseAgreement.validTill,
                    approverName,
                    otherMemberName,
                    requesterName
                )
                initiatorName = otherMemberName
                val session: FlowSession = flowMessaging.initiateFlow(initiatorName)

                updatedLicenseAgreement = flowEngine.subFlow(RequestLicenseSubFlow(createAndIssueLicenseRequest, session))
                participants = listOf(approver, otherMember)
                outputMessage = "The request for licenseAgreement with id ${updatedLicenseAgreement.id} has been sent to the artist $otherMemberName and is in status ${updatedLicenseAgreement.status}."
            }
            AgreementStatus.AGREED -> {
                val licenseRequest = PrepareLicenseSubFlow.LicenseRequest(
                    otherMemberName,
                    requesterName,
                    approverName,
                    updatedLicenseAgreement.song,
                    updatedLicenseAgreement.cost,
                    updatedLicenseAgreement.validTill,
                    LicenseStatus.ACTIVE
                )
                var session: FlowSession = flowMessaging.initiateFlow(initiatorName)

                val newLicense = flowEngine.subFlow(PrepareLicenseSubFlow(licenseRequest, session))
                updatedLicenseAgreement = updatedLicenseAgreement.updateLicenseId(newLicense.id)
                session = flowMessaging.initiateFlow(initiatorName)
                updatedLicenseAgreement = flowEngine.subFlow(CloseSubFlow(updatedLicenseAgreement, session))
                outputMessage = "The request for licenseAgreement with id ${updatedLicenseAgreement.id} has been approved by the artist $approverName and is in status ${updatedLicenseAgreement.status}. The new license has been created with id ${newLicense.id} and status ${newLicense.status}, valid until ${newLicense.validTill}"
            }
            AgreementStatus.REJECTED -> {
                val session: FlowSession = flowMessaging.initiateFlow(initiatorName)
                updatedLicenseAgreement = flowEngine.subFlow(CloseSubFlow(updatedLicenseAgreement, session))
                outputMessage = "The request for licenseAgreement with id ${updatedLicenseAgreement.id} has been rejected by $approverName due to ${updatedLicenseAgreement.rejectionReason} and is in status ${updatedLicenseAgreement.status}."
            }
            else -> outputMessage = "No action required for status ${updatedLicenseAgreement.status} or the status ${updatedLicenseAgreement.status} is incorrect."
        }

        val transaction = utxoLedgerService.createTransactionBuilder()
            .setNotary(notary.name)
            .addInputState(inputLicenseState.ref)
            .addOutputState(updatedLicenseAgreement)
            .addCommand(command)
            .setTimeWindowUntil(Instant.now().plus(1, ChronoUnit.DAYS))
            .addSignatories(participants)
            .toSignedTransaction()

        val finalizeSession: FlowSession = flowMessaging.initiateFlow(initiatorName)
        return try {
            // Send the transaction and state to the counterparty and let them sign it
            // Then notarise and record the transaction in both parties' vaults.
            flowEngine.subFlow(FinalizeTransactionSubFlow(transaction, listOf(finalizeSession)))
            outputMessage
        } catch (ex: Exception) {
            "Flow failed, message: ${ex.message}"
        }
    }
}


