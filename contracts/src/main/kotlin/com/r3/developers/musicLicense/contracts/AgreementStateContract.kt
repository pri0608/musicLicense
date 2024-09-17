package com.r3.developers.musicLicense.contracts

import com.r3.developers.musicLicense.states.LicenseAgreement
import com.r3.developers.musicLicense.states.AgreementStatus
import net.corda.v5.ledger.utxo.Contract
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction
import org.slf4j.LoggerFactory
import java.math.BigDecimal

class AgreementStateContract : Contract{
    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }
    override fun verify(transaction: UtxoLedgerTransaction) {
        log.info("AgreementStateContract: commands size: ${transaction.commands.size}")
        log.info("AgreementStateContract: command: ${transaction.commands.first()}")

        when (val command = transaction.commands.first()) {
            is MusicLicenseCommands.Request -> {
                val output = transaction.getOutputStates(LicenseAgreement::class.java).first()

                require(transaction.inputContractStates.size == 0 && transaction.outputContractStates.size == 1) {
                    "This transaction must only have one AgreementState state as output"
                }
                require(output.status == AgreementStatus.REQUESTED) {
                    "The status of output state must be REQUESTED"
                }
                require(output.song.isNotBlank() && output.cost > BigDecimal.ZERO) {
                    "The output state must have clear details about requested song and its proposed cost"
                }
            }
            is MusicLicenseCommands.Agree -> {
                log.info("AgreementStateContract: InputState size: ${transaction.getInputStates(LicenseAgreement::class.java).size}")
                log.info("AgreementStateContract: InputState : ${transaction.getInputStates(LicenseAgreement::class.java).first()}")
                log.info("AgreementStateContract: OutputStates size: ${transaction.getOutputStates(LicenseAgreement::class.java).size}")
                log.info("AgreementStateContract: OutputStates : ${transaction.getOutputStates(LicenseAgreement::class.java).first()}")

                val input = transaction.getInputStates(LicenseAgreement::class.java).first()
                val output = transaction.getOutputStates(LicenseAgreement::class.java).first()

                log.info("AgreementStateContract: input: ${input.song}")
                log.info("AgreementStateContract: output: ${output.song}")

                require(transaction.getInputStates(LicenseAgreement::class.java).size == 1) {
                    "This transaction must only have one AgreementState state as input"
                }
                require(output.status == AgreementStatus.AGREED) {
                    "The status of output state must be AGREED"
                }
                require(transaction.signatories.contains(input.consenter)) {
                    "The consenter of the input state must be a signatory to the transaction"
                }
            }
            is MusicLicenseCommands.Reject -> {
                val input = transaction.getInputStates(LicenseAgreement::class.java).first()
                val output = transaction.getOutputStates(LicenseAgreement::class.java).first()
                require(transaction.getInputStates(LicenseAgreement::class.java).size == 1) {
                    "This transaction must only have one AgreementState state as input"
                }
                require(transaction.signatories.contains(input.consenter)) {
                    "The consenter of the input state must be a signatory to the transaction"
                }
                require(output.status == AgreementStatus.REJECTED) {
                    "The status of output state must be REJECTED"
                }
                require(output.rejectedBy != null && output.rejectionReason != null) {
                    "The fields rejectedBy and rejectionReason can not be null"
                }
                require(output.rejectedBy == input.consenter) {
                    "The consenter of the input state must be the same party as rejectedBy of the output state"
                }
            }
            is MusicLicenseCommands.Complete -> {
                val input = transaction.getInputStates(LicenseAgreement::class.java).first()
                val output = transaction.getOutputStates(LicenseAgreement::class.java).first()
                require(input.status == AgreementStatus.AGREED || input.status == AgreementStatus.REJECTED) {
                    "The status of input state must either be AGREED or REJECTED"
                }
                require(output.status == AgreementStatus.COMPLETED) {
                    "The status of output state must be COMPLETED"
                }            }
            else -> {
                throw IllegalArgumentException("Incorrect type of command: ${command::class.java.name}")
            }
        }
    }

}
