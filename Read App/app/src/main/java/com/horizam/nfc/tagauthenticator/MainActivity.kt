package com.horizam.nfc.tagauthenticator

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.horizam.nfc.tagauthenticator.nfc_utils.BaseUtils
import com.horizam.threeTCard.nfc_utils.NFCUtil
import kotlin.experimental.and

class MainActivity : AppCompatActivity() {

    private var nfcUtil: NFCUtil = NFCUtil()
    private var nfcAdapter: NfcAdapter? = null
    private lateinit var buttonScan:Button
    lateinit var ctx: Context
    private lateinit var textViewAuthentic: TextView
    private lateinit var textViewProductNumber: TextView
    private lateinit var imageViewAuthentic: ImageView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViews()
        checkIFAdapterNull()
        setClickListeners()
    }

    private fun setClickListeners() {
        buttonScan.setOnClickListener {
            if (buttonScan.text.equals("SCAN")){
                buttonScan.text = "CANCEL"
                showViews(false)
                enableNFC()
            }else{
                buttonScan.text="SCAN"
                disableNFC()
            }
        }
    }

    private fun initViews() {
        ctx = this
        buttonScan = findViewById(R.id.btn_scan)
        imageViewAuthentic = findViewById(R.id.iv_authentic)
        textViewAuthentic = findViewById(R.id.tv_authentic)
        textViewProductNumber = findViewById(R.id.tv_product_no)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
    }


    private fun checkIFAdapterNull() {
        if (nfcAdapter == null) {
            Toast.makeText(this, "This device doesn't support NFC.",
                Toast.LENGTH_LONG).show()
            finish()
        } else if(!nfcAdapter!!.isEnabled) {
            Toast.makeText(this, "NFC is disabled.",
                Toast.LENGTH_LONG).show()
            finish()
        }else{
            Toast.makeText(this, "This device supports NFC.",
                Toast.LENGTH_LONG).show()
        }
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
                    inMessage = BaseUtils.decode(inMessage)
                    if (inMessage.isNotEmpty()) {
                        if (inMessage.contains("opyt")){
                            val pNumber = inMessage.substringAfter("opyt=")
                            textViewProductNumber.text = pNumber
                            showViews(true)
                            buttonScan.text = "SCAN"
                        }else{
                            Toast.makeText(
                                applicationContext,"Authentication Failed: Keyword didn't matched", Toast.LENGTH_SHORT
                            ).show()
                            showViews(false)
                            buttonScan.text = "SCAN"
                        }
                    }else{
                        // writeMessage(intent)
                        //startActivity(Intent(this@MainActivity,WriteActivity::class.java))
                        Toast.makeText(applicationContext, "Empty Tag", Toast.LENGTH_SHORT).show()
                        showViews(false)
                        buttonScan.text = "SCAN"
                    }

                } catch (ex: Exception) {
                    /*Toast.makeText(
                        applicationContext,"Authentication Failed: "+ex.message, Toast.LENGTH_SHORT
                    ).show()*/
                    Toast.makeText(
                        applicationContext,
                        "Empty Tag",
                        Toast.LENGTH_SHORT
                    ).show()
                    //writeMessage(intent)
                    //startActivity(Intent(this@MainActivity,WriteActivity::class.java))
                    showViews(false)
                    buttonScan.text = "SCAN"
                }
            }


        }
        /*val messageWrittenSuccessfully = nfcUtil.createNFCMessage("aacc",intent)
        if (messageWrittenSuccessfully) {
            Toast.makeText(this, "Message written successfully ", Toast.LENGTH_SHORT)
                .show()
        } else {
            Toast.makeText(this, "Failed...", Toast.LENGTH_SHORT).show()

        }*/
    }

    private fun writeMessage(intent: Intent) {
        val messageWrittenSuccessfully = nfcUtil.createNFCMessage("opyt", intent)
        if (messageWrittenSuccessfully) {
            Toast.makeText(this@MainActivity, "Message written successfully ", Toast.LENGTH_SHORT)
                .show()
            showViews(false)
            buttonScan.text = "SCAN"
        } else {
            Toast.makeText(this@MainActivity, "Write Failed: Please try again", Toast.LENGTH_SHORT).show()
            showViews(false)
            buttonScan.text = "SCAN"
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

    fun showViews(b:Boolean){
        if (b){
            textViewAuthentic.visibility = View.VISIBLE
            textViewProductNumber.visibility = View.VISIBLE
            imageViewAuthentic.visibility = View.VISIBLE
        }else{
            textViewAuthentic.visibility = View.INVISIBLE
            textViewProductNumber.visibility = View.INVISIBLE
            imageViewAuthentic.visibility = View.INVISIBLE
        }
    }
}