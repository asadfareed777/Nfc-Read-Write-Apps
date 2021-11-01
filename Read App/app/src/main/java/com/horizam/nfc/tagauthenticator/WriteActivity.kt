package com.horizam.nfc.tagauthenticator

import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.*
import com.horizam.threeTCard.nfc_utils.NFCUtil
import java.lang.NumberFormatException
import kotlin.experimental.and

class WriteActivity : AppCompatActivity() {

    private var nfcUtil: NFCUtil = NFCUtil()
    private var nfcAdapter: NfcAdapter? = null
    private lateinit var buttonWrite: Button
    private lateinit var editTextProductNo: EditText
    private lateinit var textViewStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_write)
        initViews()
        setClickListeners()
    }

    private fun setClickListeners() {
        buttonWrite.setOnClickListener {
            if (buttonWrite.text.equals("WRITE")){
                buttonWrite.text = "CANCEL"
                enableNFC()
            }else{
                buttonWrite.text="WRITE"
                disableNFC()
            }
        }
    }

    private fun initViews() {
        buttonWrite = findViewById(R.id.btn_write)
        editTextProductNo = findViewById(R.id.et_product_no)
        textViewStatus = findViewById(R.id.tv_status)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // nfcUtil.writeAndProtectTag(intent)
        val action = intent.action
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == action || NfcAdapter.ACTION_TECH_DISCOVERED==action) {

            val parcelables = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            with(parcelables) {
                try {
                    val inNdefMessage = this!![0] as NdefMessage
                    val inNdefRecords = inNdefMessage.records

                    val ndefRecord_0 = inNdefRecords[0]
                    // var inMessage = String(ndefRecord_0.payload)
                    //inMessage = inMessage.replace(" ","")
                    // figure out if we need to take out the " en" at the beginning
                    val payload = inNdefRecords[0].payload
                    val textEncoding = if(payload[0] and 128.toByte() == 0.toByte()) "UTF-8" else "UTF-16"
                    val langCodeLength = payload[0] and 63.toByte()

                    // create a string starting by skipping the first 3 characters
                    // based on the language code length
                    var inMessage = String(
                        payload,
                        langCodeLength + 1,
                        payload.count() - langCodeLength - 1,
                        charset(textEncoding))
                    if (inMessage.isNotEmpty()) {
                        finish()
                    }else{
                         writeMessage(intent)
                    }

                } catch (ex: Exception) {
                    /*Toast.makeText(
                        applicationContext,"Authentication Failed: "+ex.message, Toast.LENGTH_SHORT
                    ).show()*/
                    Toast.makeText(
                        applicationContext,
                        "Writing new data...",
                        Toast.LENGTH_SHORT
                    ).show()
                    writeMessage(intent)
                }
            }


        }
    }

    fun isNumeric(str: String): Boolean {
        return try {
            str.toDouble()
            true
        } catch (e: NumberFormatException) {
            false
        }
    }

    private fun writeMessage(intent: Intent) {
        val productNumber = editTextProductNo.text.toString()
        if (productNumber.isNullOrEmpty()){
            return
        }
        if (isNumeric(productNumber)){
            val message = "opyt=${productNumber.toInt()}"
            val messageWrittenSuccessfully = nfcUtil.createNFCMessage(message, intent)
            if (messageWrittenSuccessfully) {
                Toast.makeText(this, "Message written successfully ", Toast.LENGTH_SHORT)
                    .show()
                showViews(false)
                buttonWrite.text = "WRITE"
            } else {
                Toast.makeText(this, "Write Failed: Please try again", Toast.LENGTH_SHORT).show()
                showViews(false)
                buttonWrite.text = "WRITE"
            }
        }else{
            Toast.makeText(this, "Product id must be numeric", Toast.LENGTH_SHORT)
                .show()
        }

    }

    fun showViews(b:Boolean){
        if (b){
            textViewStatus.visibility = View.VISIBLE
        }else{
            textViewStatus.visibility = View.INVISIBLE
        }
    }

    private fun enableNFC() {
        nfcAdapter?.let {
            nfcUtil.enableNFCInForeground(it, this, javaClass)
        }
    }

    private fun disableNFC() {
        nfcAdapter?.let {
            nfcUtil.disableNFCInForeground(it, this)
        }
    }
}