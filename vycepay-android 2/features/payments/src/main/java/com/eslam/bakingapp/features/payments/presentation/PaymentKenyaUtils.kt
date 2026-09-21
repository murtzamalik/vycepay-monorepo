package com.eslam.bakingapp.features.payments.presentation

import com.eslam.bakingapp.core.common.payment.PaymentFlowType
import com.eslam.bakingapp.core.common.phone.KenyaPhoneNormalizer

internal object PaymentKenyaUtils {

    fun normalizeKenyaMsisdn(raw: String): String? = KenyaPhoneNormalizer.toMsisdn254(raw)

    fun estimateFeeKes(amount: Long, flow: PaymentFlowType): Long {
        if (amount <= 0) return 0L
        return when (flow) {
            PaymentFlowType.PAY_BILLS, PaymentFlowType.MOBILE_RECHARGE -> when {
                amount <= 100 -> 0
                amount <= 500 -> 6
                amount <= 2_500 -> 23
                amount <= 70_000 -> 33
                else -> 108
            }
            PaymentFlowType.SEND_MONEY, PaymentFlowType.REQUEST_MONEY -> when {
                amount <= 500 -> 7
                amount <= 2_500 -> 33
                amount <= 70_000 -> 57
                else -> 108
            }
        }
    }

    fun formatKes(amount: Long): String =
        java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(amount)
}
