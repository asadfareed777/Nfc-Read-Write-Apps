package com.horizam.nfc.tagauthenticatortwo.nfc_utils

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.*
import android.nfc.NdefRecord
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.util.Log
import android.widget.Toast
import com.horizam.nfc.tagauthenticatortwo.App
import com.horizam.nfc.tagauthenticatortwo.MainActivity
import java.util.*
import kotlin.experimental.and
import kotlin.experimental.or


open class NFCUtil() {

    private var ndefGen: Ndef? = null

    fun createNFCMessage(payload: String, intent: Intent?): Boolean {

        val nfcRecord = NdefRecord.createUri(payload)

        val nfcMessage = NdefMessage(arrayOf(nfcRecord))
        intent?.let {
            val tag = it.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            return writeMessageToTag(nfcMessage, tag)
        }
        return false
    }

    private fun writeMessageToTag(nfcMessage: NdefMessage, tag: Tag?): Boolean {

        try {
            val nDefTag = Ndef.get(tag)
            nDefTag?.let {
                if (it.isWritable){
                    it.connect()
                    if (it.maxSize < nfcMessage.toByteArray().size) {
                        //Message to large to write to NFC tag
                        return false
                    }
                    return if (it.isWritable) {
                        it.writeNdefMessage(nfcMessage)
                        //it.makeReadOnly()
                        it.close()
                        //Message is written to tag
                        true
                    } else {
                        //NFC tag is read-only
                        false
                    }
                }else{
                    return false
                }
            }
        } catch (e: Exception) {
            //Write operation has failed
            e.printStackTrace()
        }
        return false
    }

    public fun <T> enableNFCInForeground(
        nfcAdapter: NfcAdapter,
        activity: Activity,
        classType: Class<T>
    ) {
        val pendingIntent = PendingIntent.getActivity(
            activity, 0,
            Intent(activity, classType).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0
        )
        val nfcIntentFilter = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
        val filters = arrayOf(nfcIntentFilter)

        val TechLists =
            arrayOf(arrayOf(Ndef::class.java.name), arrayOf(NdefFormatable::class.java.name))

        nfcAdapter.enableForegroundDispatch(activity, pendingIntent, filters, TechLists)
    }

    public open fun disableNFCInForeground(nfcAdapter: NfcAdapter, activity: Activity) {
        nfcAdapter.disableForegroundDispatch(activity)
    }

    /////////////////////////////
    open fun writeAndProtectTag(intent: Intent) {
        // Run the entire process in its own thread as MifareUltralight.transceive(byte[] data);
        // Should not be run in main thread according to <https://developer.android.com/reference/android/nfc/tech/MifareUltralight.html#transceive(byte[])>
        Log.i("Tag", "Nice Tag")
        Thread(object : Runnable {
            var pwd = "asad".toByteArray()
            var pack = "cC".toByteArray()
            override fun run() {

                val tag: Tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)!!
                var mifare: MifareUltralight? = null
                try {
                    var response: ByteArray
                    mifare = MifareUltralight.get(tag);
                    mifare.connect();
                    while (!mifare.isConnected());
//Read page 41 on NTAG213, will be different for other tags
                    response = mifare.transceive(
                        byteArrayOf(
                            0x30.toByte(),  // READ
                            41 // page address
                        )
                    )

// Authenticate with the tag first
// only if the Auth0 byte is not 0xFF,
// which is the default value meaning unprotected

                    if (response[3] != 0xFF.toByte()) {
                        try {
                            response = mifare.transceive(
                                byteArrayOf(
                                    0x1B.toByte(),  // PWD_AUTH
                                    pwd.get(0), pwd.get(1), pwd.get(2), pwd.get(3)
                                )
                            )
                            // Check if PACK is matching expected PACK
                            // This is a (not that) secure method to check if tag is genuine
                            if (response != null && response.size >= 2) {
                                val packResponse = Arrays.copyOf(response, 2)
                                if (!(pack.get(0) === packResponse[0] && pack.get(1) === packResponse[1])) {
                                    ((MainActivity.ctx) as Activity).runOnUiThread {
                                        Toast.makeText(
                                            App.ctx, """Tag could not be authenticated:$packResponse≠${pack.toString()}""".trimIndent(), Toast.LENGTH_LONG
                                        ).show()
                                        MainActivity.showViews(false)
                                    }
                                } else {
                                    ((MainActivity.ctx) as Activity).runOnUiThread {
                                        Toast.makeText(
                                            App.ctx,
                                            "Tag successfully authenticated!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        MainActivity.showViews(true)
                                    }
                                    // unlock Tag
                                   unlockTag(mifare)
                                }
                            }
                        } catch (e: TagLostException) {
                            e.printStackTrace()
                        }
                    } else {

                        // Protect tag with your password in case
                        // it's not protected yet

                        // Get Page 2Ah
                        response = mifare.transceive(
                            byteArrayOf(
                                0x30.toByte(),  // READ
                                0x2A.toByte() // page address
                            )
                        )
                        // configure tag as write-protected with unlimited authentication tries
                        if (response != null && response.size >= 16) {    // read always returns 4 pages
                            val prot =
                                false // false = PWD_AUTH for write only, true = PWD_AUTH for read and write
                            val authlim = 0 // 0 = unlimited tries
                            mifare.transceive(
                                byteArrayOf(
                                    0xA2.toByte(),  // WRITE
                                    0x2A.toByte(),  // page address
                                    (response[0] and 0x078 or (if (prot) 0x080.toByte() else 0x000) or (authlim and 0x007).toByte()),  // set ACCESS byte according to our settings
                                    0,
                                    0,
                                    0 // fill rest as zeros as stated in datasheet (RFUI must be set as 0b)
                                )
                            )
                        }
                        // Get page 29h
                        response = mifare.transceive(
                            byteArrayOf(
                                0x30.toByte(),  // READ
                                0x29.toByte() // page address
                            )
                        )
                        // Configure tag to protect entire storage (page 0 and above)
                        if (response != null && response.size >= 16) {  // read always returns 4 pages
                            val auth0 = 0 // first page to be protected
                            mifare.transceive(
                                byteArrayOf(
                                    0xA2.toByte(),  // WRITE
                                    0x29.toByte(),  // page address
                                    response[0],
                                    0,
                                    response[2],  // Keep old mirror values and write 0 in RFUI byte as stated in datasheet
                                    (auth0 and 0x0ff).toByte()
                                )
                            )
                        }

                        // Send PACK and PWD
                        // set PACK:
                        mifare.transceive(
                            byteArrayOf(
                                0xA2.toByte(),
                                0x2C.toByte(),
                                pack.get(0),
                                pack.get(1),
                                0,
                                0 // Write PACK into first 2 Bytes and 0 in RFUI bytes
                            )
                        )
                        // set PWD:
                        mifare.transceive(
                            byteArrayOf(
                                0xA2.toByte(),
                                0x2B.toByte(),
                                pwd.get(0),
                                pwd.get(1),
                                pwd.get(2),
                                pwd.get(3) // Write all 4 PWD bytes into Page 43
                            )
                        )
                        ((MainActivity.ctx) as Activity).runOnUiThread {
                            Toast.makeText(
                                App.ctx, "Tag Locked Successfully", Toast.LENGTH_LONG
                            ).show()
                            MainActivity.showViews(false)
                        }

                        mifare.close()
                    }

                } catch (e: TagLostException) {
                    e.printStackTrace()
                }
            }
        }).start()
    }

    private fun unlockTag(mifare: MifareUltralight?){
        var isOkay = false
        // Get Page 2Ah
        var response = mifare!!.transceive(byteArrayOf(
            0x30.toByte(),  // READ
            0x2A.toByte() // page address
        ))

        if (response != null && response.size >= 16) {
            mifare.transceive(byteArrayOf(
                0xA2.toByte(),  // WRITE
                0x2A.toByte(),  // page address
                0,  // set ACCESS byte according to our settings
                0, 0, 0 // fill rest as zeros as stated in datasheet (RFUI must be set as 0b)
            ))
        }
        // Get page 29h
        response = mifare.transceive(byteArrayOf(
            0x30.toByte(),  // READ
            0x29.toByte() // page address
        ))

        if (response != null && response.size >= 16) {
            mifare.transceive(byteArrayOf(
                0xA2.toByte(),  // WRITE
                0x29.toByte(),  // page address
                response[0], response[1], response[2],  // Keep old mirror values and write 0 in RFUI byte as stated in datasheet
                0x0ff.toByte()
            ))

            // Send PACK and PWD
            // set PACK:
            mifare.transceive(byteArrayOf(
                0xA2.toByte(),
                0x2C.toByte(),
                0x00.toByte(), 0x00.toByte(), 0, 0
            ))
            // set PWD:
            mifare.transceive(byteArrayOf(
                0xA2.toByte(),
                0x2B.toByte(),
                0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte()
            ))

            mifare.close()
            (MainActivity.ctx as Activity).runOnUiThread {
                Toast.makeText(MainActivity.ctx, "Tag unlocked", Toast.LENGTH_SHORT).show()
            }

        }

    }

}

