package com.r3.developers.musicLicense.contracts

import com.r3.developers.musicLicense.states.License
import com.r3.developers.musicLicense.states.LicenseStatus
import net.corda.v5.ledger.utxo.Contract
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction
import java.util.*

class LicenseStateContract : Contract{
    override fun verify(transaction: UtxoLedgerTransaction) {
        when (val command = transaction.commands.first()) {
            is MusicLicenseCommands.Activate -> {
                val output = transaction.getOutputStates(License::class.java).first()
                require(output.validTill.after(Date()) ) {
                    "The validTill of output state must be after today"
                }
                require(output.status == LicenseStatus.ACTIVE) {
                    "The status of output state must be ACTIVE"
                }
            }
        }
        when (val command = transaction.commands.first()) {
            is MusicLicenseCommands.Expire -> {
                val input = transaction.getInputStates(License::class.java).first()
                val output = transaction.getOutputStates(License::class.java).first()
                require(output.validTill.before(Date()) ) {
                    "The validTill of output state must be before today"
                }
                require(input.status == LicenseStatus.ACTIVE) {
                    "The status of input state must be ACTIVE"
                }
                require(output.status == LicenseStatus.EXPIRED) {
                    "The status of output state must be EXPIRED"
                }
            }
        }
    }

}
