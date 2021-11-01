package com.horizam.threeTCard.nfc_utils

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.tech.NfcF
import android.widget.Toast

open class ReadFromNfcCard(
    mcontext: Context
) {
//    lateinit var context:Context
//    constructor( mcontext: Context){
//        context=mcontext
//    }

     var intentFiltersArray: Array<IntentFilter>? = null
     val techListsArray = arrayOf(arrayOf(NfcF::class.java.name))

    var iswrite = "0"
    var userEmail = "";
    var type = "";


     val nfcAdapter: NfcAdapter? by lazy {
        NfcAdapter.getDefaultAdapter(mcontext)
    }
     var pendingIntent: PendingIntent? = null

    init {

        startReadingNFC(mcontext)
    }

    private fun startReadingNFC(
        context: Context
    ) {
        try {


            //nfc process start
            pendingIntent = PendingIntent.getActivity(
                context, 0, Intent(context, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0
            )
            val ndef = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
            try {
                ndef.addDataType("text/plain")
            } catch (e: IntentFilter.MalformedMimeTypeException) {
                throw RuntimeException("fail", e)
            }
            intentFiltersArray = arrayOf(ndef)

        } catch (ex: Exception) {
            Toast.makeText(context, ex.message, Toast.LENGTH_SHORT).show()
        }

    }


    //below code will be use in on resume
    public fun activeNfc(activity: Activity) {


        nfcAdapter?.enableForegroundDispatch(
            activity,
            pendingIntent,
            intentFiltersArray,
            techListsArray
        )
    }


    //below code will be use in overriding onIntentAction and pass intent
    public open fun onIntentAction(intent: Intent?, context: Context): Boolean {
        val action = intent!!.action
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == action) {

            val parcelables = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            with(parcelables) {
                try {
                    val inNdefMessage = this?.get(0) as NdefMessage
                    val inNdefRecords = inNdefMessage.records
                    //if there are many records, you can call inNdefRecords[1] as array
                    var ndefRecord_0 = inNdefRecords[0]
                    var inMessage = String(ndefRecord_0.payload)

                    userEmail = inMessage.drop(3);

                    ndefRecord_0 = inNdefRecords[1]
                    inMessage = String(ndefRecord_0.payload)
                    type = inMessage.drop(3);

                    Toast.makeText(
                        context, userEmail + "  :  " + type,
                        Toast.LENGTH_SHORT
                    )
                        .show()

                    return true


                } catch (ex: java.lang.Exception) {
                    Toast.makeText(
                        context,
                        "There are no Machine and Shop information found!, please click write data to write those!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }


        }

        return false

    }


}