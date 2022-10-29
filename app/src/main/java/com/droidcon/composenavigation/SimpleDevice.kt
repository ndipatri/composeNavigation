package com.droidcon.composenavigation

import com.google.gson.annotations.SerializedName
import java.util.*

/**
 * Represents a single Particle device in the list returned
 * by a call to "GET /v1/devices"
 */
data class SimpleDevice (
    val id: String,
    val name: String,
    val cellular: Boolean,
    val imei: String,
    @SerializedName("last_iccid") val lastIccid: String,
    @SerializedName("current_build_target") val currentBuild: String,
    @SerializedName("default_build_target") val defaultBuild: String,
    @SerializedName("connected") val isConnected: Boolean,
    @SerializedName("platform_id") val platformId: Int,
    @SerializedName("product_id") val productId: Int,
    @SerializedName("last_ip_address") val ipAddress: String,
    @SerializedName("status") val status: String,
    @SerializedName("last_heard") val lastHeard: Date
)