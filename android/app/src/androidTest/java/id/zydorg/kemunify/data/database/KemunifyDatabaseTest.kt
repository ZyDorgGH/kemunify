package id.zydorg.kemunify.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test


class KemunifyDatabaseTest{

    private lateinit var database: KemunifyDatabase
    private lateinit var wasteDao: WasteDao
    private lateinit var customerDao: CustomerDao
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun initDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        database = Room.inMemoryDatabaseBuilder(
            context,
            KemunifyDatabase::class.java
        ).allowMainThreadQueries()
            .build()

        wasteDao = database.wasteDao()
        customerDao = database.customerDao()
    }

    @After
    fun closeDb() = database.close()

    @Test
    fun insert_and_get_waste() = runTest {
        val waste = WasteEntity(wasteName = "Botol bersih", weightsJson = "{}")
        wasteDao.insert(waste)

        val retrieved = wasteDao.getWasteByName("Botol bersih")
        assertNotNull(retrieved)
        assertEquals("Botol bersih", retrieved?.wasteName)
    }

    @Test
    fun waste_table_count_should_increase_on_insert() = runTest {
        val countBefore = wasteDao.getCount()
        wasteDao.insert(WasteEntity(wasteName = "Kardus", weightsJson = "{}"))
        val countAfter = wasteDao.getCount()

        assertEquals(countBefore + 1, countAfter)
    }

    @Test
    fun delete_customer_by_name_should_work() = runTest {
        val customer = CustomerEntity(customerName = "Andi", "now")
        customerDao.insert(customer)

        customerDao.delete("Andi")
        val customers = customerDao.getAllCustomers().first()

        assertFalse(customers.any { it.customerName == "Andi" })
    }

    @Test
    fun update_waste_should_change_data() = runTest {
        val waste = WasteEntity(wasteName = "Kaca", weightsJson = "{}")
        wasteDao.insert(waste)

        val original = wasteDao.getWasteByName("Kaca")
        val updated = original?.copy(weightsJson = """{"Andi":1.0}""")
        if (updated != null) {
            wasteDao.update(updated)
        }

        val afterUpdate = wasteDao.getWasteByName("Kaca")
        assertEquals("""{"Andi":1.0}""", afterUpdate?.weightsJson)
    }
}