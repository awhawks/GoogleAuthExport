package com.hawkstech.otpauth

import java.lang.reflect.UndeclaredThrowableException
import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class TOTP(
    val userName: String,
    val userId: String,
    val userIssuer: String,
    val userAlgorithm: Algorithm,
    val userCodeDigits: Int,
    val userPeriod: Long,
    val userSecret: ByteArray
) {
    companion object {
        //                                    0  1   2    3     4      5       6        7         8
        private val DIGITS_POWER = intArrayOf(1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000)

        // Seed for HMAC-SHA1 - 20 bytes
        //val seed = "3132333435363738393031323334353637383930"
        private val TESTSEED20 = byteArrayOf(
            0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x30,
            0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x30
        )

        // Seed for HMAC-SHA256 - 32 bytes
        //val seed32 = "3132333435363738393031323334353637383930313233343536373839303132"
        private val TESTSEED32 = byteArrayOf(
            0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x30,
            0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x30,
            0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x30,
            0x31, 0x32
        )

        // Seed for HMAC-SHA512 - 64 bytes
        //val seed64 = "31323334353637383930313233343536373839303132333435363738393031323334353637383930313233343536373839303132333435363738393031323334"
        private val TESTSEED64 = byteArrayOf(
            0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x30,
            0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x30,
            0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x30,
            0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x30,
            0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x30,
            0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x30,
            0x31, 0x32, 0x33, 0x34
        )

        @JvmStatic
        fun main(args: Array<String>) {
            val totp1 = TOTP( "test", "testId", "testIssuer",
                Algorithm.ALGORITHM_SHA1, 8, 30, TESTSEED20
            )
            val totp256 = TOTP("test", "testId", "testIssuer",
                Algorithm.ALGORITHM_SHA256, 8, 30, TESTSEED32
            )
            val totp512 = TOTP("test", "testId", "testIssuer",
                Algorithm.ALGORITHM_SHA512, 8, 30, TESTSEED64
            )
            println(         "+---------------+-----------------------+------------------+--------+--------+------+------------------+" )
            println(         "|  Time(sec)    |   Time (UTC format)   | Value of T(Hex)  |  TOTP  |  REF   | test | Mode             |" )
            println(         "+---------------+-----------------------+------------------+--------+--------+------+------------------+" )
            listOf( totp1, totp256, totp512 ).forEach { t ->
                val tr = t.test()
                tr.toSortedMap().forEach { (_, d) ->
                    println( "|  ${d[0]}  |  ${d[1]}  | ${d[2]} |${d[3]} ${d[4]} ${d[5]} | ${t.userAlgorithm.name} |")
                }
                println(     "+---------------+-----------------------+------------------+--------+--------+------+------------------+")
            }
        }
    }

    fun test(): HashMap<Long, ArrayList<String>> {
        val result = HashMap<Long, ArrayList<String>>()
        val t0: Long = 0
        val x: Long  = 30
        /*
          This section provides test values that can be used for the HOTP time-
             based variant algorithm interoperability test.

          The test token shared secret uses the ASCII string value
             "12345678901234567890".  With Time Step X = 30, and the Unix epoch as
             the initial value to count time steps, where T0 = 0, the TOTP
             algorithm will display the following values for specified modes and
             timestamps.
         */
        val testData: Map<Long, Triple<String, String, String>> = mapOf(
            59L          to Triple("94287082", "46119246", "90693936" ),
            1111111109L  to Triple("07081804", "68084774", "25091201" ),
            1111111111L  to Triple("14050471", "67062674", "99943326" ),
            1234567890L  to Triple("89005924", "91819424", "93441116" ),
            2000000000L  to Triple("69279037", "90698825", "38618901" ),
            20000000000L to Triple("65353130", "77737706", "47863826" )
        )
        var steps = "0"
        val df: DateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        df.timeZone = TimeZone.getTimeZone("UTC")
        try {
            testData.forEach { (testTime, keys) ->
                val time = (testTime - t0) / x
                val k    = generateTOTP( time )
                val result1 = ArrayList<String>()
                result1.add( String.format("%1$-11s", testTime)    )
                result1.add( df.format(Date(testTime * 1000)) )
                result1.add( String.format("%1$16s", steps )       )
                result1.add( k                                     )
                val valid = when( userAlgorithm ) {
                    Algorithm.ALGORITHM_SHA1   -> keys.first
                    Algorithm.ALGORITHM_SHA256 -> keys.second
                    Algorithm.ALGORITHM_SHA512 -> keys.third
                    else -> "UNK"
                }
                result1.add( valid )
                result1.add( "${k == valid}" )
                result[testTime] = result1
            }
        } catch (e: Exception) {
            println("Error : $e")
        }
        return result
    }

    /**
     * This method uses the JCE to provide the crypto algorithm.
     * HMAC computes a Hashed Message Authentication Code with the
     * crypto hash algorithm as a parameter.
     *
     * @param crypto: the crypto algorithm (HmacSHA1, HmacSHA256,
     *                             HmacSHA512)
     * @param keyBytes: the bytes to use for the HMAC key
     * @param text: the message or text to be authenticated
     */
    private fun hmac_sha(
        keyBytes: ByteArray,
        text: ByteArray
    ): ByteArray {
        return try {
            val crypto = when( userAlgorithm ){
                Algorithm.ALGORITHM_UNSPECIFIED -> ""
                Algorithm.ALGORITHM_SHA1        -> "HmacSHA1"
                Algorithm.ALGORITHM_SHA256      -> "HmacSHA256"
                Algorithm.ALGORITHM_SHA512      -> "HmacSHA512"
                Algorithm.ALGORITHM_MD5         -> "HmacMD5"
                Algorithm.UNRECOGNIZED          -> TODO()
            }
            val hmac = Mac.getInstance(crypto)
            val macKey = SecretKeySpec(keyBytes, "RAW")
            hmac.init(macKey)
            hmac.doFinal(text)
        } catch (gse: GeneralSecurityException) {
            throw UndeclaredThrowableException(gse)
        }
    }

    fun longToBytes(x: Long): ByteArray {
        val buffer = ByteBuffer.allocate(java.lang.Long.BYTES)
        buffer.putLong(x)
        return buffer.array()
    }

    fun bytesToLong(bytes: ByteArray): Long {
        val buffer = ByteBuffer.allocate(java.lang.Long.BYTES)
        buffer.put(bytes)
        buffer.flip() //need flip
        return buffer.long
    }

    fun generateTOTP( time: Long ): String {
        var result: String

        val msg = longToBytes(time)
        val hash = hmac_sha( userSecret, msg)

        // put selected bytes into result int
        val offset: Int = hash[hash.size - 1].toInt() and 0xf
        val binary: Int = hash[offset    ].toInt() and 0x7f shl 24 or
                (hash[offset + 1].toInt() and 0xff shl 16) or
                (hash[offset + 2].toInt() and 0xff shl 8) or
                (hash[offset + 3].toInt() and 0xff)
        val otp = binary % DIGITS_POWER[userCodeDigits]
        result = otp.toString()
        while (result.length < userCodeDigits) {
            result = "0$result"
        }
        return result
    }
}
/*
This section provides test values that can be used for the HOTP time-
   based variant algorithm interoperability test.

The test token shared secret uses the ASCII string value
   "12345678901234567890".  With Time Step X = 30, and the Unix epoch as
   the initial value to count time steps, where T0 = 0, the TOTP
   algorithm will display the following values for specified modes and
   timestamps.

  +-------------+----------------------+------------------+----------+--------+
  |  Time (sec) |   UTC Time           | Value of T (hex) |   TOTP   |  Mode  |
  +-------------+----------------------+------------------+----------+--------+
  |      59     |  1970-01-01 00:00:59 | 0000000000000001 | 94287082 |  SHA1  |
  |      59     |  1970-01-01 00:00:59 | 0000000000000001 | 46119246 | SHA256 |
  |      59     |  1970-01-01 00:00:59 | 0000000000000001 | 90693936 | SHA512 |
  |  1111111109 |  2005-03-18 01:58:29 | 00000000023523EC | 07081804 |  SHA1  |
  |  1111111109 |  2005-03-18 01:58:29 | 00000000023523EC | 68084774 | SHA256 |
  |  1111111109 |  2005-03-18 01:58:29 | 00000000023523EC | 25091201 | SHA512 |
  |  1111111111 |  2005-03-18 01:58:31 | 00000000023523ED | 14050471 |  SHA1  |
  |  1111111111 |  2005-03-18 01:58:31 | 00000000023523ED | 67062674 | SHA256 |
  |  1111111111 |  2005-03-18 01:58:31 | 00000000023523ED | 99943326 | SHA512 |
  |  1234567890 |  2009-02-13 23:31:30 | 000000000273EF07 | 89005924 |  SHA1  |
  |  1234567890 |  2009-02-13 23:31:30 | 000000000273EF07 | 91819424 | SHA256 |
  |  1234567890 |  2009-02-13 23:31:30 | 000000000273EF07 | 93441116 | SHA512 |
  |  2000000000 |  2033-05-18 03:33:20 | 0000000003F940AA | 69279037 |  SHA1  |
  |  2000000000 |  2033-05-18 03:33:20 | 0000000003F940AA | 90698825 | SHA256 |
  |  2000000000 |  2033-05-18 03:33:20 | 0000000003F940AA | 38618901 | SHA512 |
  | 20000000000 |  2603-10-11 11:33:20 | 0000000027BC86AA | 65353130 |  SHA1  |
  | 20000000000 |  2603-10-11 11:33:20 | 0000000027BC86AA | 77737706 | SHA256 |
  | 20000000000 |  2603-10-11 11:33:20 | 0000000027BC86AA | 47863826 | SHA512 |
  +-------------+----------------------+------------------+----------+--------+

 */
