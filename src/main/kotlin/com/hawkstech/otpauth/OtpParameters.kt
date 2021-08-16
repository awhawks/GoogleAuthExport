package com.hawkstech.otpauth

import kotlinx.serialization.Serializable
import java.net.URI
import java.net.URLEncoder

@Serializable
data class OtpParameters(
    var secret: ByteArray,
    var name: String,
    var issuer: String       = "",
    var algorithm: Algorithm = Algorithm.ALGORITHM_UNSPECIFIED,
    var digits: DigitCount   = DigitCount.DIGIT_COUNT_UNSPECIFIED,
    var type: OtpType        = OtpType.OTP_TYPE_UNSPECIFIED,
    var counter: Long        = 0
) {
    fun toUri(): URI {
        val (type, period) = when (type) {
            OtpType.OTP_TYPE_TOTP -> Pair("totp", 30)
            OtpType.OTP_TYPE_HOTP -> Pair("hotp", 30)
            OtpType.OTP_TYPE_UNSPECIFIED -> TODO()
            OtpType.UNRECOGNIZED -> TODO()
        }
        val alg = when (algorithm) {
            Algorithm.ALGORITHM_SHA1 -> "SHA1"
            Algorithm.ALGORITHM_SHA256 -> "SHA256"
            Algorithm.ALGORITHM_SHA512 -> "SHA512"
            Algorithm.ALGORITHM_MD5 -> "MD5"
            Algorithm.ALGORITHM_UNSPECIFIED -> TODO()
            Algorithm.UNRECOGNIZED -> TODO()
        }
        val dig = when (digits) {
            DigitCount.DIGIT_COUNT_SIX -> 6
            DigitCount.DIGIT_COUNT_EIGHT -> 8
            DigitCount.DIGIT_COUNT_UNSPECIFIED -> TODO()
            DigitCount.UNRECOGNIZED -> TODO()
        }
        val secr =
            Base163264().encodeToBase32( secret )
                .removeSuffix("=")
                .removeSuffix("=")
                .removeSuffix("=")
                .removeSuffix("=")
        var uriStr = "otpauth://"
        uriStr += type
        uriStr += "/$name"
        uriStr += "?algorithm=${alg}"
        uriStr += "&digits=${dig}"
        uriStr += "&issuer=${issuer}"
        uriStr += "&period=${period}"
        uriStr += "&secret=${secr}"
        return URI.create( URLEncoder.encode( uriStr ) )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OtpParameters

        if (!secret.contentEquals(other.secret)) return false
        if (name != other.name) return false
        if (issuer != other.issuer) return false
        if (algorithm != other.algorithm) return false
        if (digits != other.digits) return false
        if (type != other.type) return false
        if (counter != other.counter) return false

        return true
    }

    override fun hashCode(): Int {
        var result = secret.contentHashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + issuer.hashCode()
        result = 31 * result + algorithm.hashCode()
        result = 31 * result + digits.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + counter.hashCode()
        return result
    }
}
