package com.pims.vault.presentation.vault.card

data class CardReceipt(
    val bytes: ByteArray,
    val displayName: String,
    val mimeType: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CardReceipt
        if (!bytes.contentEquals(other.bytes)) return false
        if (displayName != other.displayName) return false
        if (mimeType != other.mimeType) return false
        return true
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + displayName.hashCode()
        result = 31 * result + mimeType.hashCode()
        return result
    }
}
