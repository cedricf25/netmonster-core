package cz.mroczis.netmonster.core.feature.detect

import android.Manifest
import android.os.Build
import android.telephony.ServiceState
import androidx.annotation.RequiresPermission
import androidx.annotation.VisibleForTesting
import cz.mroczis.netmonster.core.INetMonster
import cz.mroczis.netmonster.core.db.NetworkTypeTable
import cz.mroczis.netmonster.core.db.model.NetworkType
import cz.mroczis.netmonster.core.model.annotation.SinceSdk
import cz.mroczis.netmonster.core.telephony.ITelephonyManagerCompat


/**
 * Attempts to detect LTE Advanced / LTE Carrier aggregation and NR in NSA mode
 *
 * Based on [ServiceState]'s contents added in Android P which describe if aggregation is currently active.
 */
class DetectorLteAdvancedNrServiceState : INetworkDetector {

    @RequiresPermission(
        allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_PHONE_STATE]
    )
    @SinceSdk(Build.VERSION_CODES.P)
    override fun detect(netmonster: INetMonster, telephony: ITelephonyManagerCompat): NetworkType? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            telephony.getTelephonyManager()?.serviceState?.toString()?.let {
                val lteA = isUsingCarrierAggregation(it)
                val nr = is5gActive(it)
                when {
                    lteA && nr -> NetworkTypeTable.get(NetworkType.NR_LTE_CA)
                    nr -> NetworkTypeTable.get(NetworkType.NR_LTE)
                    lteA -> NetworkTypeTable.get(NetworkType.LTE_CA)
                    else -> null
                }
            }
        } else {
            null
        }

    /**
     * Android P - mIsUsingCarrierAggregation=true
     * Android 10 - mIsUsingCarrierAggregation = true
     */
    @VisibleForTesting
    internal fun isUsingCarrierAggregation(serviceState: String) =
        (serviceState.contains("mIsUsingCarrierAggregation ?= ?true".toRegex()) && serviceState.contains("cellIdentity=CellIdentityLte"))

    /**
     * AOSP documentation:
     * The device is camped on an LTE cell that supports E-UTRA-NR Dual Connectivity(EN-DC) and
     * also connected to at least one 5G cell as a secondary serving cell.
     *
     * NR_STATE_CONNECTED / 3
     */
    @VisibleForTesting
    internal fun is5gActive(serviceState: String) =
        serviceState.contains("nrState=CONNECTED")

}