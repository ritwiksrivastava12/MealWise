package `in`.mealwise.app

import java.text.NumberFormat
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class MoneyTest {
    private val inr = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    @Test fun paiseFormatsAsRupees() {
        assertEquals("₹149.00", inr.format(14900 / 100.0))
    }
    @Test fun zeroIsFree() {
        assertEquals("₹0.00", inr.format(0 / 100.0))
    }
}
