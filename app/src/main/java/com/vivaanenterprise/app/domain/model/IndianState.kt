package com.vivaanenterprise.app.domain.model

data class IndianState(
    val code: String,
    val name: String
) {
    val displayName: String get() = "$name ($code)"

    companion object {
        val ALL_STATES: List<IndianState> = listOf(
            IndianState("01", "Jammu and Kashmir"),
            IndianState("02", "Himachal Pradesh"),
            IndianState("03", "Punjab"),
            IndianState("04", "Chandigarh"),
            IndianState("05", "Uttarakhand"),
            IndianState("06", "Haryana"),
            IndianState("07", "Delhi"),
            IndianState("08", "Rajasthan"),
            IndianState("09", "Uttar Pradesh"),
            IndianState("10", "Bihar"),
            IndianState("11", "Sikkim"),
            IndianState("12", "Arunachal Pradesh"),
            IndianState("13", "Nagaland"),
            IndianState("14", "Manipur"),
            IndianState("15", "Mizoram"),
            IndianState("16", "Tripura"),
            IndianState("17", "Meghalaya"),
            IndianState("18", "Assam"),
            IndianState("19", "West Bengal"),
            IndianState("20", "Jharkhand"),
            IndianState("21", "Odisha"),
            IndianState("22", "Chhattisgarh"),
            IndianState("23", "Madhya Pradesh"),
            IndianState("24", "Gujarat"),
            IndianState("25", "Daman and Diu"),
            IndianState("26", "Dadra and Nagar Haveli"),
            IndianState("27", "Maharashtra"),
            IndianState("28", "Andhra Pradesh (Old)"),
            IndianState("29", "Karnataka"),
            IndianState("30", "Goa"),
            IndianState("31", "Lakshadweep"),
            IndianState("32", "Kerala"),
            IndianState("33", "Tamil Nadu"),
            IndianState("34", "Puducherry"),
            IndianState("35", "Andaman and Nicobar Islands"),
            IndianState("36", "Telangana"),
            IndianState("37", "Andhra Pradesh (New)"),
            IndianState("38", "Ladakh"),
            IndianState("97", "Other Territory")
        )

        fun findByCode(code: String?): IndianState? {
            if (code == null) return null
            val trimmed = code.trim()
            return ALL_STATES.firstOrNull { it.code == trimmed }
        }
    }
}
