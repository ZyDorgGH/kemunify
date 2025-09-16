package id.zydorg.kemunify

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import androidx.activity.compose.setContent
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import id.zydorg.kemunify.data.di.AppModule
import id.zydorg.kemunify.ui.navigation.Screen
import id.zydorg.kemunify.ui.screen.detail.DetailScreen
import id.zydorg.kemunify.ui.screen.home.HomeScreen
import id.zydorg.kemunify.ui.screen.login.LoginScreen
import id.zydorg.kemunify.ui.screen.waste.AddWasteScreen
import id.zydorg.kemunify.ui.theme.KemunifyTheme
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsNot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.task.vision.detector.Detection
import java.io.InputStream

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
@UninstallModules(AppModule::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class KemunifyAppTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()


    private val dummyResult = listOf(
        Detection.create(
            RectF(142.0f, 142.0f, 482.0f, 485.0f),
            listOf<Category>(Category.create("plastic_cup", "plastic_cup", 0.9768708f))
        ),
    )

    @Before
    fun setUp(){
        hiltRule.inject()
        composeTestRule.activity.setContent {

            KemunifyTheme {
                val navController = rememberNavController()
                NavHost(
                    startDestination = Screen.Login.route,
                    navController = navController

                ) {
                    composable(
                        route = Screen.Login.route
                    ){
                        LoginScreen(
                            navigateToHome = {navController.navigate(Screen.Home.route)},
                        )
                    }
                    composable(
                        route = Screen.Home.route
                    ) {
                        HomeScreen(
                            navigateToDetail = { customer ->
                                navController.navigate(Screen.DetailCustomer.createRoute(customer))
                            },
                            navigateToAddWaste = { navController.navigate(Screen.AddWaste.route) },
                            onLogout = { navController.navigate(Screen.Login.route) }
                        )
                    }

                    composable(
                        route = Screen.DetailCustomer.route,
                        arguments = listOf(navArgument("customer") { type = NavType.StringType }),
                    ){
                        val customerId = it.arguments?.getString("customer") ?: ""
                        DetailScreen(
                            customerId = customerId,
                            navigateBack = {navController.popBackStack()}
                        )
                    }
                    composable(
                        route = Screen.AddWaste.route
                    ){
                        AddWasteScreen(onNavigateUp = { navController.navigate(Screen.Home.route) },)
                    }
                }
            }
        }
    }

    @Test
    fun permissionCamera_isNotGranted () {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertTrue(context.checkCallingOrSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        composeTestRule.onNodeWithTag("Login Button").assertDoesNotExist()
    }

    @Test
    fun saveWasteCustomerData_isCancel() {
        //Login to Home Screen
        composeTestRule.onNodeWithTag("Login Button").performClick()

        //Open Add Customer Data Screen
        composeTestRule.onNodeWithContentDescription("Tambah Nasabah").performClick()

        //Edit Customer Form
        composeTestRule
            .onNodeWithContentDescription("Gelas bersih")
            .isDisplayed()
        composeTestRule
            .onNodeWithContentDescription("Input Customer")
            .performTextInput("Tono")
        composeTestRule
            .onNodeWithContentDescription("Gelas bersih")
            .performTextClearance()
        composeTestRule
            .onNodeWithContentDescription("Gelas bersih")
            .performTextInput("0.10")
        composeTestRule
            .onNodeWithContentDescription("Plastik rongsok")
            .performTextClearance()
        composeTestRule
            .onNodeWithContentDescription("Plastik rongsok")
            .performTextInput("0.20")

        //Cancel input
        composeTestRule.onNodeWithText("Cancel").performClick()

        //Customer Name Displayed
        composeTestRule.onNodeWithText("Rekap Data Tono").assertDoesNotExist()
    }
    @Test
    fun saveWasteCustomerData_ExportToExcel () {

        //Login to Home Screen
        composeTestRule.onNodeWithTag("Login Button").performClick()

        //Open Add Customer Data Scree
        composeTestRule.onNodeWithContentDescription("Menu Anomali").performClick()

        composeTestRule.onNodeWithText("Share & Upload Excel").performClick()

    }


    @Test
    fun permissionGrantedAndDetectImage_InCorrect () {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertTrue(context.checkCallingOrSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)

        val kemunifyObjectDetector = ObjectDetectorHelper(context){ results, _, _ ->
            ViewMatchers.assertThat(dummyResult.size, IsNot.not(equalTo(results!!.size)))


            for (i in dummyResult.indices) {
                // Bounding boxes must not be the same
                ViewMatchers.assertThat(
                    results[i].boundingBox,
                    IsNot.not(equalTo(dummyResult[i].boundingBox))
                )

                // The number of categories must be equal
                assertEquals(
                    results[i].categories.size,
                    dummyResult[i].categories.size
                )

                for (j in dummyResult[i].categories.indices) {
                    // Labels must not be the same
                    assertThat(
                        results[i].categories[j].label,
                        not(equalTo(dummyResult[i].categories[j].label))
                    )
                }
            }
        }
        val bitmap = loadImage("image-incorrect.jpg")
        kemunifyObjectDetector.detect(bitmap, 0)
    }

    @Test
    fun permissionGrantedAndDetectImage_isCorrect () {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertTrue(context.checkCallingOrSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)

        val kemunifyObjectDetector = ObjectDetectorHelper(context) { results, _, _ ->
            assertEquals(dummyResult.size, results!!.size)

            for (i in results.indices) {
                // Compare the bounding box coordinates with the expected values
                assertEquals(results[i].boundingBox, dummyResult[i].boundingBox)

                // Check if both results contain the same amount of categories
                assertEquals(
                    results[i].categories.size,
                    dummyResult[i].categories.size
                )

                for (j in dummyResult[i].categories.indices) {
                    // Ensure the detected label matches the expected label
                    assertEquals(
                        results[i].categories[j].label,
                        dummyResult[i].categories[j].label
                    )
                }
            }
        }
        val bitmap = loadImage("plastic-cup.jpg")
        kemunifyObjectDetector.detect(bitmap, 0)
    }


    private fun loadImage(fileName: String): Bitmap {
        // context is test app's context to grab assets that are in src/androidTest/assets
        val assetManager: AssetManager =
            InstrumentationRegistry.getInstrumentation().context.assets
        println("loadImage: ${assetManager.locales.size}")
        val inputStream: InputStream = assetManager.open(fileName)
        return BitmapFactory.decodeStream(inputStream)
    }
}