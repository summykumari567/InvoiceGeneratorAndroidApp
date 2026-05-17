package com.kaap.invoicegenerator.utils

object NumberToWords {

    private val ones = arrayOf(
        "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
        "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
        "Seventeen", "Eighteen", "Nineteen"
    )

    private val tens = arrayOf(
        "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    )

    fun convert(amount: Double): String {
        val rupees = amount.toLong()
        val paise = Math.round((amount - rupees) * 100).toInt()

        val rupeesWords = if (rupees == 0L) "Zero" else convertLong(rupees)
        return if (paise > 0) {
            "$rupeesWords and ${convertInt(paise)} Paise Only"
        } else {
            "$rupeesWords Only"
        }
    }

    private fun convertLong(n: Long): String {
        return when {
            n >= 10_00_00_000L -> {
                val crore = n / 10_00_00_000L
                val rem = n % 10_00_00_000L
                "${convertInt(crore.toInt())} Crore${if (rem > 0) " ${convertLong(rem)}" else ""}"
            }
            n >= 1_00_000L -> {
                val lakh = n / 1_00_000L
                val rem = n % 1_00_000L
                "${convertInt(lakh.toInt())} Lakh${if (rem > 0) " ${convertLong(rem)}" else ""}"
            }
            n >= 1000L -> {
                val thousands = n / 1000L
                val rem = n % 1000L
                "${convertInt(thousands.toInt())} Thousand${if (rem > 0) " ${convertInt(rem.toInt())}" else ""}"
            }
            else -> convertInt(n.toInt())
        }
    }

    private fun convertInt(n: Int): String {
        return when {
            n == 0 -> ""
            n < 20 -> ones[n]
            n < 100 -> tens[n / 10] + if (n % 10 != 0) " ${ones[n % 10]}" else ""
            else -> "${ones[n / 100]} Hundred${if (n % 100 != 0) " ${convertInt(n % 100)}" else ""}"
        }
    }
}
