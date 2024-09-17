package com.r3.developers.musicLicense.contracts

import net.corda.v5.ledger.utxo.Command

interface MusicLicenseCommands : Command {
    class Request : MusicLicenseCommands
    class Agree : MusicLicenseCommands
    class Reject : MusicLicenseCommands
    class Complete : MusicLicenseCommands
    class Activate : MusicLicenseCommands
    class Expire: MusicLicenseCommands
}