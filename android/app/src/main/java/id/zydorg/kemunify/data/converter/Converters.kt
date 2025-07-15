package id.zydorg.kemunify.data.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.math.BigDecimal
import java.math.RoundingMode

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromString(value: String?): Map<String, BigDecimal> {
        if (value.isNullOrBlank()) return emptyMap()
        val type = object : TypeToken<Map<String, BigDecimal>>() {}.type
        return gson.fromJson(value, type) ?: emptyMap()
    }

    @TypeConverter
    fun fromMap(map: Map<String, BigDecimal>?): String {
        return gson.toJson(map ?: emptyMap<String, BigDecimal>())
    }

    fun isValidDecimal(value: String): Boolean {
        return try {
            val normalized = value.replace(',', '.')
            BigDecimal(normalized)
            true
        } catch (e: Exception) {
            false
        }
    }
}

fun String.toBigDecimalOrNull(): BigDecimal? {
    return try {
        val normalized = this.replace(',', '.').replace(" ", "")
        BigDecimal(normalized)
    } catch (e: Exception) {
        null
    }
}

fun String.toBigDecimalOrZero(): BigDecimal {
    return toBigDecimalOrNull() ?: BigDecimal.ZERO
}

fun isWeightValidDecimal(value: String): Boolean {
    return try {
        val normalized = value.replace(',', '.')
        BigDecimal(normalized)
        true
    } catch (e: Exception) {
        false
    }
}

fun String.toWeightBigDecimalOrZero(): BigDecimal {
    return try {
        val normalized = this.replace(',', '.').replace(" ", "")
        BigDecimal(normalized).setScale(2, RoundingMode.HALF_UP)
    } catch (e: Exception) {
        BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
    }
}