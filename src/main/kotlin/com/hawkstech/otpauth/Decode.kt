package com.hawkstech.otpauth

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.*
import kotlin.collections.HashMap

internal class Decode {
    companion object {
        const val printHex = true

        @JvmStatic
        fun main(args: Array<String>) {
            if( args.size == 1 ) {
                Decode().run(File(args[0]))
            } else {
                println("one file argument must be past")
            }
        }
    }

    val payload2:HashMap<Int,Payload> = HashMap()
    var singleI = 100

    @OptIn(ExperimentalSerializationApi::class)
    private fun run(file: File) {
        val fr = FileReader(file)
        val br = BufferedReader(fr)
        var entryNum = 0
        br.lines().forEach { myLine: String ->
            val entry = processEntry(myLine)
            if( entry != null ) {
                val payload = Payload()
                payload.version     = entry.version
                payload.batch_size  = entry.batch_size
                payload.batch_index = entry.batch_index
                payload.batch_id    = entry.batch_id
                payload.otp_parameters.addAll( entry.otp_parameters )
                payload2[entryNum++] = payload
            } else {
                println("#### NO PAYLOAD DATA ####")
                println("#### line:${myLine}")
            }
        }
        payload2.forEach { (i, payload) ->
            val bytes = ProtoBuf.encodeToByteArray(Payload.serializer(), payload)
            val myb64plain = Base163264().encodeToBase64(bytes)
            val myb64Encoded = URLEncoder.encode(myb64plain, Charsets.UTF_8.toString())
            val myUrlString = "otpauth-migration://offline?data=${myb64Encoded}"
            val myUrl = URI.create(myUrlString)
            println("----------------- generated all accounts migrate url --------------")
            println(myUrl)
            println("===================================================================")
            File("migration.bin").writeBytes(bytes)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun processEntry(line: String): Payload? {
        var result: Payload? = null
        if (!line.startsWith("#")) {
            if (line.length > 0) {
                val uri = URI(line)
                if (uri.scheme == "otpauth-migration") {
                    if (uri.host == "offline") {
                        val data = line.substring("otpauth-migration://offline?data=".length).replace(" ", "+")
                        //println("data  [${data}]")
                        val ddata = URLDecoder.decode(data, StandardCharsets.UTF_8.toString())
                        //println("ddata [${ddata}]")
                        val protoData = Base163264().decodeFromBase64(ddata)
                        if (printHex) {
                            protoData.forEachIndexed { index, b ->
                                if (index % 16 == 0) println()
                                val d = b.toUByte().toUInt().toInt()
                                val hex = String.format("0x%02x", d)
                                print("[${hex}]")
                            }
                            println()
                        }
                        result = ProtoBuf.decodeFromByteArray(Payload.serializer(), protoData)
                        //result = MyPayload().parseFrom(protoData)
                        println("version            [${result.version}]")
                        println("batch_size         [${result.batch_size}]")
                        println("batch_index        [${result.batch_index}]")
                        println("batch_id           [${result.batch_id}]")
                        println("otpParametersCount [${result.otp_parameters.size}]")
                        println(String.format("%-6s %-20s (%-40s)", "code", "name", "login"))
                        println("====== ====================  ========================================")
                        result.otp_parameters.forEachIndexed { i, p ->
                            val dig = if (p.digits == DigitCount.DIGIT_COUNT_SIX) 6 else 8
                            val totp = TOTP(p.name, "", p.issuer, p.algorithm, dig, p.counter, p.secret)
                            val state = Math.floorDiv(Instant.now().epochSecond, 30L)
                            val key1 = totp.generateTOTP(state)
                            val u = p.toUri().toString()
                            val du = URLDecoder.decode(u, Charsets.UTF_8.name())
                            val tmp1 = du.substring("otpauth://totp/".length)
                            val tmp2 = tmp1.indexOf(":")
                            val tmp3 = tmp1.indexOf("?")
                            val name = tmp1.substring(0, tmp2)
                            val login = tmp1.substring(tmp2 + 1, tmp3)
                            val namef = String.format("%-20s (%-40s)", name, login)
                            println("${key1} ${namef} | entry[${i}] ${du}")
                            //val sd = Base32().decode( p.secret )
                            //sd.forEach { b ->
                            //    val d = b.toUByte().toUInt().toInt()
                            //    val hex = String.format("0x%02x", d)
                            //    print("[${hex}]")
                            //}
                            //println()
                        }
                    } else {
                        println("not an offline migration [${line}]")
                    }
                } else if(uri.scheme == "otpauth" ) {
                    val tmp0 = line.substring("otpauth://".length)
                    val (otpauth_type, tmp1) = if( tmp0.startsWith("totp/") ) {
                        Pair(
                            OtpType.OTP_TYPE_TOTP,
                            tmp0.substring("totp/".length)
                        )
                    } else if( tmp0.startsWith("hotp/") ) {
                        Pair(
                            OtpType.OTP_TYPE_HOTP,
                            tmp0.substring("hotp/".length)
                        )
                    } else {
                        throw Exception( "unknown otpauth type" )
                    }
                    val tmp2 = tmp1.indexOf(":")
                    val tmp3 = tmp1.indexOf("?")
                    val name = tmp1.substring(0, tmp2)
                    val login = tmp1.substring(tmp2 + 1, tmp3)
                    val parms = tmp1.substring( tmp3+1 )
                    val namef = String.format("%-20s (%-40s)", name, login)
                    val pairs = parms.split( "&" )
                    var otpauth_secret:ByteArray     = byteArrayOf()
                    val otpauth_name:String          = name
                    var otpauth_issuer:String?       = null
                    var otpauth_algorithm:Algorithm? = null
                    var otpauth_digits:DigitCount?   = null
                    var otpauth_counter:Long?        = null
                    pairs.forEach { entry ->
                        val (key, value) = entry.split( "=" )
                        when( key ) {
                            "issuer" -> otpauth_issuer = value
                            "period" -> otpauth_counter = value.toLong()
                            "secret" -> {
                                otpauth_secret = value.toByteArray( Charsets.UTF_8 )
                            }
                            "digits" -> {
                                if( value == "6" ) otpauth_digits = DigitCount.DIGIT_COUNT_SIX
                                if( value == "8" ) otpauth_digits = DigitCount.DIGIT_COUNT_EIGHT
                            }
                            "algorithm" -> {
                                otpauth_algorithm = when(value.uppercase(Locale.getDefault())) {
                                    "MD5"    -> Algorithm.ALGORITHM_MD5
                                    "SHA1"   -> Algorithm.ALGORITHM_SHA1
                                    "SHA256" -> Algorithm.ALGORITHM_SHA256
                                    "SHA512" -> Algorithm.ALGORITHM_SHA512
                                    else -> Algorithm.ALGORITHM_UNSPECIFIED
                                }
                            }
                            else -> println( "unknown parameter name [${key}]" )
                        }
                    }
                    val p  = OtpParameters(
                        type      = otpauth_type,
                        name      = otpauth_name,
                        secret    = otpauth_secret,
                        issuer    = otpauth_issuer!!,
                        algorithm = otpauth_algorithm!!,
                        digits    = otpauth_digits!!,
                        counter   = otpauth_counter!!
                    )
                    val dig = if (p.digits == DigitCount.DIGIT_COUNT_SIX) 6 else 8
                    val totp = TOTP(p.name, "", p.issuer, p.algorithm, dig, p.counter, p.secret)
                    val state = Math.floorDiv(Instant.now().epochSecond, 30L)
                    val key1 = totp.generateTOTP(state)
                    val u = p.toUri().toString()
                    val du = URLDecoder.decode(u, Charsets.UTF_8.name())
                    println("${key1} ${namef} | entry[${singleI++}] ${du}")
                } else {
                    println("not an otpauth migration [${line}]")
                }
            }
        }
        return result
    }

}
