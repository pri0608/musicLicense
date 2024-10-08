package com.r3.developers.musicLicense

import com.r3.developers.musicLicense.states.License
import com.r3.developers.musicLicense.states.LicenseStatus
import net.corda.v5.application.flows.*
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import java.math.BigDecimal
import java.util.*

@InitiatingFlow(protocol = "prepare-license")
class PrepareLicenseSubFlow(
    private val licenseRequest: LicenseRequest,
    private val session: FlowSession
) : SubFlow<License> {

    @CordaSerializable
    data class LicenseRequest(
        val producer: MemberX500Name,
        val soundBank: MemberX500Name,
        val artist: MemberX500Name,
        val song: String,
        val cost: BigDecimal,
        val validTill: Date,
        val status: LicenseStatus
    )

    @Suspendable
    override fun call(): License {
        return session.sendAndReceive(License::class.java, licenseRequest)
    }
}

@InitiatedBy(protocol = "prepare-license")
class PrepareLicenseResponderFlow: ResponderFlow {

    @CordaInject
    lateinit var memberLookup: MemberLookup

    @Suspendable
    override fun call(session: FlowSession) {
        val licenseRequest = session.receive(PrepareLicenseSubFlow.LicenseRequest::class.java)

        val producer = memberLookup.lookup(licenseRequest.producer)?.ledgerKeys?.first()
            ?: throw IllegalArgumentException("The producer ${licenseRequest.producer} does not exist within the network")
        val soundBank = memberLookup.lookup(licenseRequest.soundBank)?.ledgerKeys?.first()
            ?: throw IllegalArgumentException("The soundBank ${licenseRequest.soundBank} does not exist within the network")
        val artist = memberLookup.lookup(licenseRequest.artist)?.ledgerKeys?.first()
            ?: throw IllegalArgumentException("The artist ${licenseRequest.artist} does not exist within the network")

        val license = License(
            UUID.randomUUID(),
            producer,
            soundBank,
            artist,
            licenseRequest.song,
            licenseRequest.cost,
            licenseRequest.validTill,
            licenseRequest.status,
            listOf(producer, soundBank, artist)
        )
        session.send(license)
    }
}