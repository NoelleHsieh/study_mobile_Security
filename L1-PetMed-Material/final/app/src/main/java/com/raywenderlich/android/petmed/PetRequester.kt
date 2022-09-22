/*
 * Copyright (c) 2020 Razeware LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Notwithstanding the foregoing, you may not use, copy, modify, merge, publish,
 * distribute, sublicense, create a derivative work, and/or sell copies of the
 * Software in any work that is designed, intended, or marketed for pedagogical or
 * instructional purposes related to programming, coding, application development,
 * or information technology.  Permission for such use, copying, modification,
 * merger, publication, distribution, sublicensing, creation of derivative works,
 * or sale is expressly withheld.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.raywenderlich.android.petmed

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.Base64
import android.util.Log
import com.babylon.certificatetransparency.certificateTransparencyHostnameVerifier
import com.datatheorem.android.trustkit.TrustKit
import com.google.gson.Gson
import com.google.gson.JsonParser
import java.io.FileInputStream
import java.net.URL
import java.nio.charset.Charset
import java.security.KeyStore
import java.security.Security
import java.security.cert.CertPathBuilder
import java.security.cert.PKIXBuilderParameters
import java.security.cert.PKIXRevocationChecker
import java.security.cert.X509CertSelector
import java.util.*
import javax.net.ssl.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val SERVER_PUBLIC_KEY = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEZmhp0EzuDRq0FK0AcV/10RzrTYp+HiGU457hCNgcn0uun0gYz1rmhsAZaieQoiqubCgXwP/XkVKYKOZ8CHGkWA=="

class PetRequester(listeningActivity: Activity) {

  interface RequestManagerResponse {
    fun receivedNewPets(results: PetResults)
  }

  private val responseListener: RequestManagerResponse
  private val context: Context

  init {
    responseListener = listeningActivity as RequestManagerResponse
    context = listeningActivity.applicationContext

    System.setProperty("com.sun.net.ssl.checkRevocation", "true")
    Security.setProperty("ocsp.enable", "true")
    //Options to further config OCSP
    /*
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      val ts = KeyStore.getInstance("AndroidKeyStore")
      ts.load(null)
      val kmf =  KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      // init cert path checking for offered certs and revocation checks against CLRs
      val cpb = CertPathBuilder.getInstance("PKIX")
      val rc: PKIXRevocationChecker = cpb.revocationChecker as PKIXRevocationChecker
      rc.options = (EnumSet.of(
          PKIXRevocationChecker.Option.PREFER_CRLS, // use CLR over OCSP
          PKIXRevocationChecker.Option.ONLY_END_ENTITY,
          PKIXRevocationChecker.Option.NO_FALLBACK)) // no fall back to OCSP
      val pkixParams = PKIXBuilderParameters(ts, X509CertSelector())
      pkixParams.addCertPathChecker(rc)
      tmf.init( CertPathTrustManagerParameters(pkixParams) )
      val ctx = SSLContext.getInstance("TLS")
      ctx.init(kmf.keyManagers, tmf.trustManagers, null)
    }
     */
  }

  fun retrievePets() {
    val urlString = "https://kolinsturt.github.io/posts.json"
    val url = URL(urlString)
    val connection = url.openConnection() as HttpsURLConnection
    connection.hostnameVerifier =
        certificateTransparencyHostnameVerifier(connection.hostnameVerifier) {
      // Enable for the provided hosts
      +"*.github.io"

      // Exclude specific hosts
      //-"kolinsturt.github.io"
    }

    val authenticator = Authenticator()
    val bytesToSign = urlString.toByteArray(Charsets.UTF_8)
    val signedData = authenticator.sign(bytesToSign)
    val requestSignature = android.util.Base64.encodeToString(signedData, android.util.Base64.DEFAULT)
    Log.d("PetRequester", "signature for request : $requestSignature")

    val signingSuccess = authenticator.verify(signedData, bytesToSign)
    Log.d("PetRequester", "success : $signingSuccess")

    connection.sslSocketFactory = TrustKit.getInstance().getSSLSocketFactory(url.host)

    GlobalScope.launch(Default) {
      val json = connection.inputStream.bufferedReader().readText()
      connection.disconnect()

      withContext(Main) {
        // Verify received signature
        val jsonElement = JsonParser().parse(json)
        val jsonObject = jsonElement.asJsonObject
        val result = jsonObject.get("items").toString()
        val resultBytes = result.toByteArray(Charsets.UTF_8)

        val signature = jsonObject.get("signature").toString()
        val signatureBytes = Base64.decode(signature, Base64.DEFAULT)

        val success = authenticator.verify(signatureBytes, resultBytes, SERVER_PUBLIC_KEY)

        if (success) {
          // Process data
          val receivedPets = Gson().fromJson(json, PetResults::class.java)
          responseListener.receivedNewPets(receivedPets)
        }
      }
    }
  }
}