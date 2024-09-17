package com.r3.developers.musicLicense.states

import com.r3.developers.musicLicense.contracts.AgreementStateContract
import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.ledger.utxo.BelongsToContract
import net.corda.v5.ledger.utxo.ContractState
import java.math.BigDecimal
import java.security.PublicKey
import java.util.Date
import java.util.UUID

@BelongsToContract(AgreementStateContract::class)
class LicenseAgreement (
    val id: UUID,
    val proposer: PublicKey,
    val consenter: PublicKey,
    val song: String,
    val cost: BigDecimal,
    val validTill: Date,
    val status: AgreementStatus,
    val rejectionReason: String?,
    val rejectedBy: PublicKey?,
    private val participants: List<PublicKey>
) : ContractState {
    override fun getParticipants(): List<PublicKey> = participants

    fun updateLicense(updatedStatus: AgreementStatus, updatedRejectionReason: String?, updatedRejectedBy: PublicKey?): LicenseAgreement {
        return LicenseAgreement(id, proposer, consenter, song, cost, validTill, updatedStatus, updatedRejectionReason, updatedRejectedBy, participants)
    }
}

@CordaSerializable
enum class AgreementStatus {
    REQUESTED,
    AGREED,
    REJECTED,
    COMPLETED
}


