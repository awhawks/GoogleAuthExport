package com.hawkstech.otpauth

import kotlinx.serialization.Serializable

@Serializable
data class Payload(
    var otp_parameters: ArrayList<OtpParameters> = ArrayList(),
    var version: Int     = 0,
    var batch_size: Int  = 0,
    var batch_index: Int = 0,
    var batch_id: Int    = 0
)
