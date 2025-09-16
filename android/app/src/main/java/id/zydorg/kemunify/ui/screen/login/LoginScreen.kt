package id.zydorg.kemunify.ui.screen.login

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.identity.Identity
import id.zydorg.kemunify.R
import id.zydorg.kemunify.data.model.User
import id.zydorg.kemunify.ui.common.UiState

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
    navigateToHome: () -> Unit,
) {
    val context = LocalContext.current
    val credentialManager = CredentialManager.create(context)
    var userState by remember { mutableStateOf(User("", "","", isLogin = false)) }


    viewModel.userUiState.collectAsState(initial = UiState.Loading).value.let { uiState ->
        when (uiState) {
            is UiState.Loading -> {
                viewModel.fetchUser()
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is UiState.Success -> {
                userState = uiState.data
            }

            is UiState.Error -> {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Gagal Memuat Data", color = Color.DarkGray)
                }
            }
        }
    }

    val authorizationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null){
            val authorizationResult = Identity.getAuthorizationClient(context)
                .getAuthorizationResultFromIntent(result.data!!)

            Toast.makeText(context, "Permission Granted first", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Permission failed", Toast.LENGTH_SHORT).show()
        }

    }


    LaunchedEffect(userState.isLogin) {
        if (userState.isLogin) {
            Log.d("userEmail", "userEmail: ${userState.email}")
            try {
                viewModel.requestDriveAuthorization(
                    context = context,
                    authorizationLauncher = authorizationLauncher,
                    navigateToHome = { navigateToHome() }
                )
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Gagal meminta izin Google Drive: $e",
                    Toast.LENGTH_LONG
                ).show()
                navigateToHome()
            }

        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Image(
                painter = painterResource(id = R.drawable.kemuning_logo),
                contentDescription = "App Logo",
                modifier = Modifier.size(120.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))
            Text(
                text = "Selamat Datang",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Silakan masuk untuk melanjutkan",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(40.dp))
            GoogleLoginButton(
                context = context,
                credentialManager = credentialManager,
                navigateToHome = {navigateToHome()},
                isLogin = userState.isLogin
            )

            Spacer(modifier = Modifier.weight(0.5f))
        }
    }
}

@Composable
fun GoogleLoginButton(
    context: Context,
    viewModel: LoginViewModel = hiltViewModel(),
    credentialManager: CredentialManager,
    navigateToHome: () -> Unit,
    isLogin: Boolean
) {
    Button(
        onClick = {
            if (isLogin){
                Toast.makeText(context, "Sudah Login", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.signInWithGoogle(
                    context = context,
                    credentialManager = credentialManager,
                )
            }
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color.Black
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        ),
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .testTag("Login Button")
        ,
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_google_logo),
            contentDescription = "Google Logo",
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))

        Text(
            text = "Login dengan Google",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
