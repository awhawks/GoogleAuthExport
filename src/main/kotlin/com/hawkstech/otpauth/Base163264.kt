package com.hawkstech.otpauth

import java.io.ByteArrayOutputStream

/*
https://datatracker.ietf.org/doc/html/rfc4648
 */
class Base163264 {
    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            val b = Base163264()
            mapOf(
                ""       to arrayOf( "",             "",                 "",                 "",         ""         ),
                "f"      to arrayOf( "66",           "MY======",         "CO======",         "Zg==",     "Zg=="     ),
                "fo"     to arrayOf( "666F",         "MZXQ====",         "CPNG====",         "Zm8=",     "Zm8="     ),
                "foo"    to arrayOf( "666F6F",       "MZXW6===",         "CPNMU===",         "Zm9v",     "Zm9v"     ),
                "foob"   to arrayOf( "666F6F62",     "MZXW6YQ=",         "CPNMUOG=",         "Zm9vYg==", "Zm9vYg==" ),
                "fooba"  to arrayOf( "666F6F6261",   "MZXW6YTB",         "CPNMUOJ1",         "Zm9vYmE=", "Zm9vYmE=" ),
                "foobar" to arrayOf( "666F6F626172", "MZXW6YTBOI======", "CPNMUOJ1E8======", "Zm9vYmFy", "Zm9vYmFy" ),
            ).toSortedMap().forEach { (tst, values) ->
                println("------------test of encoding [${tst}]-------------------")
                val b16    = b.encodeToBase16(    tst.toByteArray(Charsets.UTF_8) )
                val b32    = b.encodeToBase32(    tst.toByteArray(Charsets.UTF_8) )
                val b32hex = b.encodeToBase32Hex( tst.toByteArray(Charsets.UTF_8) )
                val b64    = b.encodeToBase64(    tst.toByteArray(Charsets.UTF_8) )
                val b64alt = b.encodeToBase64Alt( tst.toByteArray(Charsets.UTF_8) )
                println( "Base16    ${values[0] == b16    } [${values[0]}] [${b16   }]" )
                println( "Base32    ${values[1] == b32    } [${values[1]}] [${b32   }]" )
                println( "Base32Hex ${values[2] == b32hex } [${values[2]}] [${b32hex}]" )
                println( "Base64    ${values[3] == b64    } [${values[3]}] [${b64   }]" )
                println( "Base64Alt ${values[4] == b64alt } [${values[4]}] [${b64alt}]" )
                val db16    = String( b.decodeFromBase16(    b16    ))
                val db32hex = String( b.decodeFromBase32Hex( b32hex ))
                val db32    = String( b.decodeFromBase32(    b32    ))
                val db64    = String( b.decodeFromBase64(    b64    ))
                val db64alt = String( b.decodeFromBase64Alt( b64alt ))
                println( "Decode Base16    ${tst == db16    } [${tst}] [${db16   }]" )
                println( "Decode Base32    ${tst == db32    } [${tst}] [${db32   }]" )
                println( "Decode Base32Hex ${tst == db32hex } [${tst}] [${db32hex}]" )
                println( "Decode Base64    ${tst == db64    } [${tst}] [${db64   }]" )
                println( "Decode Base64Alt ${tst == db64alt } [${tst}] [${db64alt}]" )
            }
            println("------------test of base64 padding----------------------")
            val (t1v, t1b) = Pair( "FPucA9l+", byteArrayOf( 0x14.toByte(), 0xfb.toByte(), 0x9c.toByte(), 0x03.toByte(), 0xd9.toByte(), 0x7e.toByte()) )
            val t1 = b.encodeToBase64( t1b )
            val t1a = b.encodeToBase64Alt( t1b )
            println("Base64    ${t1 == t1v} [${t1v}] [${t1}] [${t1a}]" )
            val (t2v, t2b) = Pair( "FPucA9k=", byteArrayOf( 0x14.toByte(), 0xfb.toByte(), 0x9c.toByte(), 0x03.toByte(), 0xd9.toByte()) )
            val t2 = b.encodeToBase64( t2b )
            val t2a = b.encodeToBase64Alt( t2b )
            println("Base64    ${t2 == t2v} [${t2v}] [${t2}] [${t2a}]" )
            val (t3v, t3b) = Pair( "FPucAw==", byteArrayOf( 0x14.toByte(), 0xfb.toByte(), 0x9c.toByte(), 0x03.toByte()) )
            val t3 = b.encodeToBase64( t3b )
            val t3a = b.encodeToBase64Alt( t3b )
            println("Base64    ${t3 == t3v} [${t3v}] [${t3}] [${t3a}]" )
        }
    }

    enum class BASE {
        BASE16,
        BASE32,
        BASE32HEX,
        BASE64,
        BASE64ALT
    }

    private enum class SHIFTTYPE {
        NONE,
        LEFT,
        RIGHT
    }

    private data class TD(
        var offset: Int,
        var b1Mask: Int,
        var b2Mask: Int,
        var b1sh: SHIFTTYPE,
        var b1shCount: Int,
        var b2sh: SHIFTTYPE,
        var b2shCount: Int,
    )

    fun encodeToBase16(data: ByteArray): String {
        return encode(data, BASE.BASE16)
    }

    fun decodeFromBase16( encodedData: String): ByteArray {
        return decode( encodedData, BASE.BASE16 )
    }

    fun encodeToBase32(data: ByteArray): String {
        return encode(data, BASE.BASE32)
    }

    fun decodeFromBase32( encodedData: String): ByteArray {
        return decode( encodedData, BASE.BASE32 )
    }

    fun encodeToBase32Hex(data: ByteArray): String {
        return encode(data, BASE.BASE32HEX)
    }

    fun decodeFromBase32Hex( encodedData: String): ByteArray {
        return decode( encodedData, BASE.BASE32HEX )
    }

    fun encodeToBase64(data: ByteArray): String {
        return encode(data, BASE.BASE64)
    }

    fun decodeFromBase64( encodedData: String): ByteArray {
        return decode( encodedData, BASE.BASE64 )
    }

    fun encodeToBase64Alt(data: ByteArray): String {
        return encode(data, BASE.BASE64ALT)
    }

    fun decodeFromBase64Alt( encodedData: String): ByteArray {
        return decode( encodedData, BASE.BASE64ALT )
    }

    // base16
    // 0 1 2 3 4 5 6 7
    // 0 1 2 3|0 1 2 3
    // base32
    // 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
    // 0 1 2 3 4|0 1 2 3 4|0 1 2 3 4|0 1 2 3 4|0 1 2 3 4|0 1 2 3 4|0 1 2 3 4|0 1 2 3 4
    // base64
    // 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
    // 0 1 2 3 4 5|0 1 2 3 4 5|0 1 2 3 4 5|0 1 2 3 4 5
    fun encode(data: ByteArray, base: BASE ): String {
        val result = ByteArrayOutputStream()
        val (encodeSize, encodePaddingSize, encodeChars) = when( base ){
            BASE.BASE16    -> Triple( 4,-1, "0123456789ABCDEF" )
            BASE.BASE32    -> Triple( 5, 8, "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567" )
            BASE.BASE32HEX -> Triple( 5, 8, "0123456789ABCDEFGHIJKLMNOPQRSTUV" )
            BASE.BASE64    -> Triple( 6, 4, "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/" )
            BASE.BASE64ALT -> Triple( 6, 4, "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_" )
        }

        var sbinary = ""
        if( data.isNotEmpty() ) {
            val endIndex = data.size - 1
            for ( i in 0..endIndex ) {
                val sbyte = data[i].toInt() and 0xFF
                var str = sbyte.toString(2)
                while (str.length != 8) {
                    str = "0$str"
                }
                sbinary += str
                while (sbinary.length >= encodeSize) {
                    val binary = sbinary.substring(0, encodeSize)
                    sbinary = sbinary.substring(encodeSize)
                    val charIndex = binary.toInt(2)
                    result.write(encodeChars[charIndex].toInt())
                }
            }
            if( sbinary != "" ) {
                while (sbinary.length < encodeSize ) {
                    sbinary += "0"
                }
                val binary = sbinary.substring(0, encodeSize)
                sbinary = sbinary.substring(encodeSize)
                if( sbinary != "" ){
                    println("Why is this not empty")
                }
                val charIndex = binary.toInt(2)
                result.write(encodeChars[charIndex].toInt())
                if( encodePaddingSize != -1 ) {
                    val rsize = result.toByteArray().size
                    val mod   = rsize.mod( encodePaddingSize )
                    val paddingSize = encodePaddingSize - mod
                    var padding = ""
                    while (padding.length != paddingSize) {
                        padding += "="
                    }
                    result.write(padding.toByteArray())
                }
            }
        }
        return String( result.toByteArray() )
    }

    fun decode( paddedData: String, base: BASE ): ByteArray {
        val result = ByteArrayOutputStream()
        val (decodeSize, encodeChars) = when( base ){
            BASE.BASE16    -> Pair( 4, "0123456789ABCDEF" )
            BASE.BASE32    -> Pair( 5, "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567" )
            BASE.BASE32HEX -> Pair( 5, "0123456789ABCDEFGHIJKLMNOPQRSTUV" )
            BASE.BASE64    -> Pair( 6, "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/" )
            BASE.BASE64ALT -> Pair( 6, "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_" )
        }
        var data = paddedData
        while( data.endsWith("=") ) data = data.removeSuffix("=")

        var sbinary = ""
        if( data.isNotEmpty() ) {
            val endIndex = data.length - 1
            for (i in 0..endIndex) {
                val sidx = encodeChars.indexOf( data[i] )
                var str = sidx.toString(2)
                while (str.length != decodeSize ) {
                    str = "0$str"
                }
                sbinary += str
                if( sbinary.length >= 8 ){
                    val binary = sbinary.substring(0, 8)
                    sbinary = sbinary.substring(8)
                    val b = binary.toInt(2)
                    result.write( b )
                }
            }
            if( sbinary != "" ){
                var tmp = sbinary
                while( tmp.endsWith("0") ) tmp = tmp.removeSuffix("0")
                if( tmp != "") {
                    println("Why is this not empty")
                }
            }
        }
        return result.toByteArray()
    }
}
