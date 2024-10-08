package com.r3.developers.musicLicense.states

import com.r3.developers.musicLicense.contracts.AgreementStateContract
import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.ledger.utxo.BelongsToContract
import net.corda.v5.ledger.utxo.ContractState
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.security.PublicKey
import java.util.Date
import java.util.UUID

@BelongsToContract(AgreementStateContract::class)
class LicenseAgreement (
    val id: UUID,
    val licenseId: UUID?,
    val proposer: PublicKey,
    val consenter: PublicKey,
    val otherMember: PublicKey,
    val song: String,
    val cost: BigDecimal,
    val validTill: Date,
    val status: AgreementStatus,
    val rejectionReason: String?,
    val rejectedBy: PublicKey?,
    private val participants: List<PublicKey>
) : ContractState {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    override fun getParticipants(): List<PublicKey> = participants

    fun updateLicenseAgreement(updatedStatus: AgreementStatus, updatedRejectionReason: String?, updatedRejectedBy: PublicKey?): LicenseAgreement {
        log.info("UpdateLicenseFlow: state id: $id")

        return LicenseAgreement(id, licenseId, proposer, consenter, otherMember, song, cost, validTill, updatedStatus, updatedRejectionReason, updatedRejectedBy, participants)
    }
    fun updateLicenseId(updatedLicenseId: UUID) : LicenseAgreement {
        return LicenseAgreement(id, updatedLicenseId, proposer, consenter, otherMember, song, cost, validTill, status, rejectionReason, rejectedBy, participants)
    }
    fun updateAgreementStatus(updatedStatus: AgreementStatus) : LicenseAgreement {
        return LicenseAgreement(id, licenseId, proposer, consenter, otherMember, song, cost, validTill, updatedStatus, rejectionReason, rejectedBy, participants)
    }
}

@CordaSerializable
enum class AgreementStatus {
    REQUESTED,
    AGREED_AND_REQUESTED,
    AGREED,
    REJECTED,
    CLOSED
}


