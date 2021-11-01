package com.horizam.nfc.tagauthenticator.nfc_utils

import android.util.Log
import java.lang.Exception
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.and
import kotlin.math.pow


class BaseUtils {

    companion object{
        fun encode(s: String): String {
            // create a string to add in the initial
            // binary code for extra security
            val ini = "11111111"
            var cu = 0

            // create an array
            val arr = IntArray(11111111)

            // iterate through the string
            for (i in s.indices) {
                // put the ascii value of
                // each character in the array
                arr[i] = s[i].toInt()
                cu++
            }
            var res = ""

            // create another array
            val bin = IntArray(111)
            var idx = 0

            // run a loop of the size of string
            for (i1 in 0 until cu) {

                // get the ascii value at position
                // i1 from the first array
                var temp = arr[i1]

                // run the second nested loop of same size
                // and set 0 value in the second array
                for (j in 0 until cu) bin[j] = 0
                idx = 0

                // run a while for temp > 0
                while (temp > 0) {
                    // store the temp module
                    // of 2 in the 2nd array
                    bin[idx++] = temp % 2
                    temp /= 2
                }
                var dig = ""
                var temps: String

                // run a loop of size 7
                for (j in 0..6) {

                    // convert the integer to string
                    temps = bin[j].toString()

                    // add the string using
                    // concatenation function
                    dig += temps
                }
                var revs = ""

                // reverse the string
                for (j in dig.length - 1 downTo 0) {
                    val ca = dig[j]
                    revs += ca.toString()
                }
                res += revs
            }
            // add the extra string to the binary code
            res = ini + res

            // return the encrypted code
            return res
        }

        fun decode(s: String): String {
            val invalid = "Invalid Code"

            // create the same initial
            // string as in encode class
            val ini = "11111111"
            var flag = true

            // run a loop of size 8
            for (i in 0..7) {
                // check if the initial value is same
                if (ini[i] != s[i]) {
                    flag = false
                    break
                }
            }
            var `val` = ""

            // reverse the encrypted code
            for (i in 8 until s.length) {
                val ch = s[i]
                `val` += ch.toString()
            }

            // create a 2 dimensional array
            val arr = Array(11101) { IntArray(8) }
            var ind1 = -1
            var ind2 = 0

            // run a loop of size of the encrypted code
            for (i in `val`.indices) {

                // check if the position of the
                // string if divisible by 7
                if (i % 7 == 0) {
                    // start the value in other
                    // column of the 2D array
                    ind1++
                    ind2 = 0
                    val ch = `val`[i]
                    arr[ind1][ind2] = ch - '0'
                    ind2++
                } else {
                    // otherwise store the value
                    // in the same column
                    val ch = `val`[i]
                    arr[ind1][ind2] = ch - '0'
                    ind2++
                }
            }
            // create an array
            val num = IntArray(11111)
            var nind = 0
            var tem = 0
            var cu = 0

            // run a loop of size of the column
            for (i in 0..ind1) {
                tem = 0
                // convert binary to decimal and add them
                // from each column and store in the array
                for ((cu, j) in (6 downTo 0).withIndex()) {
                    val tem1 = 2.0.pow(cu.toDouble()).toInt()
                    tem += arr[i][j] * tem1
                }
                num[nind++] = tem
            }
            var ret = ""
            var ch: Char
            // convert the decimal ascii number to its
            // char value and add them to form a decrypted
            // string using conception function
            for (i in 0 until nind) {
                ch = num[i].toChar()
                ret += ch.toString()
            }
            Log.e("dec", "text 11 - $ret")

            // check if the encrypted code was
            // generated for this algorithm
            return if (`val`.length % 7 == 0 && flag) {
                // return the decrypted code
                ret
            } else {
                // otherwise return an invalid message
                invalid
            }
        }
    }
}