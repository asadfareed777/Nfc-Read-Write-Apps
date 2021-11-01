package com.horizam.nfc.tagauthenticatortwo.nfc_utils

import android.content.Context
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.widget.Toast
import java.nio.charset.Charset

class WriteOnNfcCard(
    private var xuserEmail: String,
    private val xtype: String,
    private val mContext: Context
) : ReadFromNfcCard(mContext) {

    var profileId = "";

    public override fun onIntentAction(intent: Intent?, context: Context): Boolean {

        try {

            if (xuserEmail.isNotEmpty()||xtype.isNotEmpty()) {



                if (NfcAdapter.ACTION_TECH_DISCOVERED == intent!!.action
                    || NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action
                ) {

                    val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return false
                    val ndef = Ndef.get(tag) ?: return false

                    if (ndef.isWritable) {

                        var prefixByte = 0x01.toByte()
                        if ("http://www." == xuserEmail) {
                            prefixByte = 0x01.toByte()
                            xuserEmail = xuserEmail.substring(11)
                        } else if ("https://www." == xuserEmail) {
                            prefixByte = 0x02.toByte()
                            xuserEmail = xuserEmail.substring(12)
                        } else if ("http://" == xuserEmail) {
                            prefixByte = 0x03.toByte()
                            xuserEmail = xuserEmail.substring(7)
                        } else if ("https://" == xuserEmail) {
                            prefixByte = 0x04.toByte()
                            xuserEmail = xuserEmail.substring(8)
                        }

                        val uriField: ByteArray = xuserEmail.toByteArray(Charset.forName("UTF-8"))
                        val payload = ByteArray(uriField.size + 1)
                        payload[0] = prefixByte
                        System.arraycopy(uriField, 0, payload, 1, uriField.size)
                        val rtdUriRecord = NdefRecord(
                            NdefRecord.TNF_WELL_KNOWN,
                            NdefRecord.RTD_URI, ByteArray(0),
                            payload
                        )

                        var message = NdefMessage(
                            arrayOf(
                                NdefRecord.createTextRecord("en", xuserEmail),
                                NdefRecord.createTextRecord("en", xtype)
//                        NdefRecord.createTextRecord("en", userid)

                            )
                        )


                        ndef.connect()
                        ndef.writeNdefMessage(message)
                        ndef.close()


                        Toast.makeText(
                            context,
                            "Successfully Wroted!",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                }
            } else {
                Toast.makeText(context, "Write on text box!", Toast.LENGTH_SHORT).show()
            }
        } catch (Ex: Exception) {
            Toast.makeText(context, Ex.message, Toast.LENGTH_SHORT).show()
        }

        return false
    }




}
