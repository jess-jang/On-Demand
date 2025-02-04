/*
 * Copyright 2018 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jess.ondemand.feature

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.play.core.splitinstall.SplitInstallSessionState
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import com.jess.ondemand.*
import java.text.DecimalFormat

class IdCardActivity : BaseSplitActivity() {

    private lateinit var installConfirmDialogLauncher: ActivityResultLauncher<IntentSenderRequest>

    private val onDemandIdCard: OnDemandDelivery by lazy {
        OnDemandDeliveryImpl(OnDemandModuleName.IDCARD)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_on_demand)

        lifecycle.addObserver(onDemandIdCard)

        initView()
        initLauncher()
        initOnDemand()

    }

    private fun initView() {

        findViewById<TextView>(R.id.title).text = OnDemandModuleName.IDCARD.name()

        findViewById<Button>(R.id.bt_idcard).setOnClickListener {
            onDemandIdCard.loadAndLaunchModule()
        }
    }

    private fun initLauncher() {
        installConfirmDialogLauncher =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
                when (it.resultCode) {
                    Activity.RESULT_CANCELED -> {
                        // 취소
                        Toast.makeText(
                            this,
                            "REQUIRES_USER_CONFIRMATION Canceled",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
    }

    private fun initOnDemand() {
        onDemandIdCard.setOnSplitInstallListener(onInstallEvent = { event ->
            when (event.status) {
                SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION -> {
                    event.state?.let { state ->
                        onDemandIdCard.splitInstallManager.startConfirmationDialogForResult(
                            state,
                            { intent, _, fillInIntent, flagsMask, flagsValues, _, _ ->
                                installConfirmDialogLauncher.launch(
                                    IntentSenderRequest.Builder(intent)
                                        .setFillInIntent(fillInIntent)
                                        .setFlags(flagsValues, flagsMask).build()
                                )
                            }, 1000
                        )
                    }
                }

                SplitInstallSessionStatus.DOWNLOADING -> {
                    updateProgress(event.state)
                }

                SplitInstallSessionStatus.INSTALLED -> {

                }

                else -> Unit
            }

            updateStatus(event.state, event.status)

        }, onInstallError = { error ->
            findViewById<TextView>(R.id.error_module).text = error.moduleName
            findViewById<TextView>(R.id.error_text).text =
                "${error.errorMessage}(${error.errorCode})"
        })
    }

    private fun updateStatus(
        state: SplitInstallSessionState?,
        @SplitInstallSessionStatus status: Int
    ) {

        val statusModule = findViewById<TextView>(R.id.status_module)
        statusModule.text = state?.moduleNames()?.joinToString(separator = ", ")

        val statusText = findViewById<TextView>(R.id.status_text)
        statusText.text = status.getStatusText()

        val sessionIdStatus = findViewById<TextView>(R.id.session_id_status_text)
        sessionIdStatus.text = onDemandIdCard.getInstallSessionStatus().getStatusText()
    }

    private fun updateProgress(state: SplitInstallSessionState?) {
        state?.let {
            val progressBar = findViewById<ProgressBar>(R.id.progress_bar)
            progressBar.max = state.totalBytesToDownload().toInt()
            progressBar.progress = state.bytesDownloaded().toInt()

            val dec = DecimalFormat("#,###")

            val current = dec.format(state.bytesDownloaded().toInt())
            val total = dec.format(state.totalBytesToDownload().toInt())

            val progressSize = findViewById<TextView>(R.id.progress_size)
            progressSize.text = "$current / $total"
        }
    }
}
