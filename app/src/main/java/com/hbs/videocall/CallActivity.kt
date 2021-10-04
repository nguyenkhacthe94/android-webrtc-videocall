package com.hbs.videocall

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_call.*
import java.util.*

class CallActivity : AppCompatActivity() {
    var username = ""
    var friendUsername = ""
    var isPeerConnected = false

    var firebaseRef =
        Firebase.database("https://hbs-videocall-default-rtdb.asia-southeast1.firebasedatabase.app")
            .getReference("users")

    var isAudio = true
    var isVideo = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)

        username = intent.getStringExtra("username")!!

        callBtn.setOnClickListener {
            friendUsername = friendNameEdit.text.toString()
            sendCallRequest()
        }

        toggleAudioBtn.setOnClickListener {
            isAudio = !isAudio
            callJavascriptFunction("javascript:toggleAudio(\"${isAudio}\")")
            toggleAudioBtn.setImageResource(if (isAudio) R.drawable.ic_baseline_mic_24 else R.drawable.ic_baseline_mic_off_24)
        }

        toggleVideoBtn.setOnClickListener {
            isVideo = !isVideo
            callJavascriptFunction("javascript:toggleVideo(\"${isVideo}\")")
            toggleVideoBtn.setImageResource(if (isVideo) R.drawable.ic_baseline_videocam_24 else R.drawable.ic_baseline_videocam_off_24)
        }

        stopCallBtn.setOnClickListener {
            callJavascriptFunction("javascript:stopCall()")
            stopCall()
        }

        setupWebView()
    }

    private fun sendCallRequest() {
        if (!isPeerConnected) {
            Toast.makeText(
                this,
                "Your are not connected. Please check your internet connection",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        firebaseRef.child(username).child("status").setValue("onCall")
        firebaseRef.child(friendUsername).child("incoming").setValue(username)
        firebaseRef.child(friendUsername).child("status").setValue("onCall")
        firebaseRef.child(friendUsername).child("isAvailable")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.value.toString() == "true") {
                        listenForConnId()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d("ERROR", "Cannot read from DATABASE")
                }

            })

        stopCallBtn.setOnClickListener {
            firebaseRef.child(friendUsername).child("incoming").setValue(null)
            firebaseRef.child(friendUsername).child("isAvailable").setValue(null)
            firebaseRef.child(friendUsername).child("status").setValue(null)
        }
    }

    private fun listenForConnId() {
        firebaseRef.child(friendUsername).child("connId")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.value == null) {
                        return
                    }
                    switchToController()
                    callJavascriptFunction("javascript:startCall(\"${snapshot.value}\")")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d("ERROR", "Cannot read from DATABASE")
                }

            })
    }

    private fun setupWebView() {
        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest?) {
                request?.grant(request.resources)
            }
        }

        webView.settings.javaScriptEnabled = true
        webView.settings.mediaPlaybackRequiresUserGesture = false
        webView.addJavascriptInterface(JavascriptInterface(this), "Android")

        loadVideoCall()
    }

    private fun loadVideoCall() {
        var filePath = "file:android_asset/call.html"
        webView.loadUrl(filePath)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                initializePeer()
            }
        }
    }

    var uniqueId = ""

    private fun initializePeer() {
        uniqueId = getUniqueID()
        firebaseRef.child(username).child("connId").setValue(uniqueId)

        callJavascriptFunction("javascript:init(\"${uniqueId}\")")
        firebaseRef.child(username).child("incoming")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    onCallRequest(snapshot.value as? String)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d("ERROR", "Cannot read from DATABASE")
                }

            })
    }

    private fun onCallRequest(caller: String?) {
        if (caller == null) return
        callLayout.visibility = View.VISIBLE
        incomingCallTxt.text = "$caller is calling..."
        acceptBtn.setOnClickListener {
            firebaseRef.child(username).child("isAvailable").setValue(true)

            callLayout.visibility = View.GONE
            switchToController()
        }
        rejectBtn.setOnClickListener {
            firebaseRef.child(username).child("incoming").setValue(null)
            callLayout.visibility = View.GONE
        }
    }

    private fun stopCall() {
        callControlLayout.visibility = View.GONE
        inputLayout.visibility = View.VISIBLE
        firebaseRef.child(username).child("status").setValue(null)
    }

    private fun switchToController() {
        inputLayout.visibility = View.GONE
        callControlLayout.visibility = View.VISIBLE
        firebaseRef.child(friendUsername).child("status").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                firebaseRef.child(username).child("status").setValue(null)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("ERROR", "Cannot read from DATABASE")
            }
        })

    }

    private fun getUniqueID(): String {
        return UUID.randomUUID().toString()
    }

    fun onPeerConnected() {
        isPeerConnected = true
    }

    private fun callJavascriptFunction(functionString: String) {
        webView.post {
            webView.evaluateJavascript(functionString, null)
        }
    }

    override fun onBackPressed() {
        finish()
    }

    override fun onDestroy() {
        firebaseRef.child(username).setValue(null)
        webView.loadUrl("about:blank")
        super.onDestroy()
    }


}