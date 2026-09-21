package com.eslam.bakingapp.core.common.phone

/**
 * Kenya MSISDN helpers shared by login, signup, and payments.
 * National form (no country code): 9 digits starting with 7 (e.g. 712345678).
 */
object KenyaPhoneNormalizer {

    /**
     * Returns national 9-digit mobile (without leading 0 / 254), or null if invalid.
     */
    fun toNationalMobile(raw: String): String? {
        val digits = raw.filter { it.isDigit() }
        return when {
            digits.length == 12 && digits.startsWith("254") -> {
                val national = digits.drop(3)
                national.takeIf { isKeNational(it) }
            }
            digits.length == 10 && digits.startsWith("0") -> {
                val national = digits.drop(1)
                national.takeIf { isKeNational(it) }
            }
            digits.length == 9 && isKeNational(digits) -> digits
            else -> null
        }
    }

    /**
     * Full MSISDN digits without plus: 254XXXXXXXXX, or null if invalid.
     */
    fun toMsisdn254(raw: String): String? {
        val national = toNationalMobile(raw) ?: return null
        return "254$national"
    }

    private fun isKeNational(national: String): Boolean =
        national.length == 9 && national.first() == '7'
}
