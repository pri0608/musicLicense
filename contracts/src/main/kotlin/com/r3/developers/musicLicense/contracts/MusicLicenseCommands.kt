package com.r3.developers.musicLicense.contracts

import net.corda.v5.ledger.utxo.Command

interface MusicLicenseCommands : Command {
    class Request : MusicLicenseCommands
    class Agree : MusicLicenseCommands
    class AgreeAndRequest : MusicLicenseCommands
    class Reject : MusicLicenseCommands
    class Close : MusicLicenseCommands
    class Activate : MusicLicenseCommands
    class Expire: MusicLicenseCommands
}