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

internal class Decode {
    companion object {
        const val printHex = false

        @JvmStatic
        fun main(args: Array<String>) {
            if( args.size == 1 ) {
                Decode().run(File(args[0]))
            } else {
                println("one file argument must be past")
            }
        }
    }

    val payload2 = Payload()

    @OptIn(ExperimentalSerializationApi::class)
    private fun run(file: File) {
        val fr = FileReader(file)
        val br = BufferedReader(fr)
        br.lines().forEach { myLine: String ->
            val entry = processEntry(myLine)
            if( entry != null ) {
                payload2.version     = entry.version
                payload2.batch_size  = entry.batch_size
                payload2.batch_index = entry.batch_index
                payload2.batch_id    = entry.batch_id
                payload2.otp_parameters.addAll( entry.otp_parameters )
            }
        }
        val bytes = ProtoBuf.encodeToByteArray(Payload.serializer(), payload2)
        val myb64plain   = Base163264().encodeToBase64( bytes )
        val myb64Encoded = URLEncoder.encode(myb64plain, Charsets.UTF_8.toString())
        val myUrlString  = "otpauth-migration://offline?data=${myb64Encoded}"
        val myUrl        = URI.create( myUrlString )
        println("----------------- generated all accounts migrate url --------------")
        println(myUrl)
        println("===================================================================")
        File( "migration.bin").writeBytes( bytes )
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
                        if( printHex ) {
                            protoData.forEachIndexed { index, b ->
                                if (index % 16 == 0) println()
                                val d = b.toUByte().toUInt().toInt()
                                val dec = String.format("%03d", d)
                                val hex = String.format("0x%02x", d)
                                print("[${hex}/${dec}]")
                            }
                            println()
                        }
                        result = ProtoBuf.decodeFromByteArray(Payload.serializer(),protoData)
                        //result = MyPayload().parseFrom(protoData)
                        println("version            [${result.version}]")
                        println("batch_size         [${result.batch_size}]")
                        println("batch_index        [${result.batch_index}]")
                        println("batch_id           [${result.batch_id}]")
                        println("otpParametersCount [${result.otp_parameters.size}]")
                        println( String.format("%-6s %-20s (%-40s)", "code", "name", "login") )
                        println("====== ====================  ========================================")
                        result.otp_parameters.forEachIndexed { i, p ->
                            val dig = if( p.digits == DigitCount.DIGIT_COUNT_SIX ) 6 else 8
                            val totp = TOTP(p.name, "", p.issuer, p.algorithm, dig, p.counter, p.secret)
                            val state = Math.floorDiv(Instant.now().epochSecond, 30L)
                            val key1 = totp.generateTOTP( state )
                            val u = p.toUri().toString()
                            val du = URLDecoder.decode( u, Charsets.UTF_8.name() )
                            val tmp1 = du.substring( "otpauth://totp/".length )
                            val tmp2 = tmp1.indexOf( ":" )
                            val tmp3 = tmp1.indexOf( "?" )
                            val name = tmp1.substring(0, tmp2)
                            val login = tmp1.substring(tmp2+1, tmp3)
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
                } else {
                    println("not an otpauth migration [${line}]")
                }
            }
        }
        return result
    }

}
