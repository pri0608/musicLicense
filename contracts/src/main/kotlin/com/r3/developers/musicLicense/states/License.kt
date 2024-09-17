package com.r3.developers.musicLicense.states

import com.r3.developers.musicLicense.contracts.LicenseStateContract
import net.corda.v5.ledger.utxo.BelongsToContract
import net.corda.v5.ledger.utxo.ContractState
import java.math.BigDecimal
import java.security.PublicKey
import java.util.*

@BelongsToContract(LicenseStateContract::class)
class License(
    val id: UUID,
    val proposer: PublicKey,
    val consenter: PublicKey,
    val song: String,
    val cost: BigDecimal,
    val validTill: Date,
    val status: LicenseStatus,
    private val participants: List<PublicKey>
) : ContractState {
    override fun getParticipants(): List<PublicKey> = participants
}

enum class LicenseStatus {
    ACTIVE,
    EXPIRED
}