package com.example.ui.screens

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.example.ui.components.ComfortAccessibilityBar
import com.example.ui.components.JungleAtmosphereBackground
import com.example.ui.components.LocalAccessibilityUi
import com.example.ui.components.speakOnTouch
import com.example.ui.theme.DewTealAccent
import com.example.ui.theme.FireflyGoldAccent
import com.example.ui.theme.StarlightVioletAccent
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val AUTH_TAG = "Auth"

@Composable
fun AuthScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val calmMode = LocalAccessibilityUi.current.calmMode

    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    suspend fun handleGoogleIdToken(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        Firebase.auth.signInWithCredential(credential).await()
    }

    // Separate silent auto-sign-in request using GetGoogleIdOption only
    LaunchedEffect(Unit) {
        try {
            val webClientId = context.getString(R.string.default_web_client_id)
            val silentOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(true)
                .build()
            val silentRequest = GetCredentialRequest.Builder()
                .addCredentialOption(silentOption)
                .build()
            val result = credentialManager.getCredential(context, silentRequest)
            val cred = result.credential
            if (cred is CustomCredential &&
                cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                isLoading = true
                val googleIdCred = GoogleIdTokenCredential.createFrom(cred.data)
                handleGoogleIdToken(googleIdCred.idToken)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: NoCredentialException) {
            Log.d(AUTH_TAG, "No authorized Google credentials found for silent sign-in ${e.message}")
        } catch (e: GetCredentialCancellationException) {
            Log.d(AUTH_TAG, "Silent Google sign-in cancelled: ${e.message}")
        } catch (e: Exception) {
            Log.d(AUTH_TAG, "Silent Google sign-in skipped: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    fun launchInteractiveGoogleSignIn() {
        if (isLoading) return
        scope.launch {
            isLoading = true
            statusMessage = null
            try {
                val webClientId = context.getString(R.string.default_web_client_id)
                val signInOption = GetSignInWithGoogleOption.Builder(webClientId).build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(signInOption)
                    .build()

                val result = credentialManager.getCredential(context, request)
                val cred = result.credential
                if (cred is CustomCredential &&
                    cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleCred = GoogleIdTokenCredential.createFrom(cred.data)
                    handleGoogleIdToken(googleCred.idToken)
                } else {
                    statusMessage = "Unexpected sign-in credential returned. Please try again."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: GetCredentialCancellationException) {
                Log.w(AUTH_TAG, "Google Sign-In cancelled or dismissed: ${e.message}", e)
                statusMessage = "Sign-in was cancelled. Select a Google account whenever you're ready."
            } catch (e: GetCredentialException) {
                Log.e(AUTH_TAG, "Google Sign-In credential failure", e)
                statusMessage = e.localizedMessage ?: "Couldn't complete Google Sign-In. Please try again."
            } catch (e: Exception) {
                Log.e(AUTH_TAG, "Firebase Google Auth failure", e)
                statusMessage = e.localizedMessage ?: "Authentication failed. Please try again."
            } finally {
                isLoading = false
            }
        }
    }

    fun launchAnonymousSignIn() {
        if (isLoading) return
        scope.launch {
            isLoading = true
            statusMessage = null
            try {
                Firebase.auth.signInAnonymously().await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(AUTH_TAG, "Anonymous sign-in failed", e)
                statusMessage =
                    e.localizedMessage ?: "Couldn't sign in anonymously. Please try again."
            } finally {
                isLoading = false
            }
        }
    }

    JungleAtmosphereBackground {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                ComfortAccessibilityBar()

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 540.dp)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(if (calmMode) 18.dp else 16.dp)
                    ) {
                        if (!calmMode) {
                            Surface(
                                shape = RoundedCornerShape(28.dp),
                                tonalElevation = 6.dp,
                                border = BorderStroke(1.dp, FireflyGoldAccent.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .speakOnTouch("Fernfable. Solari Magitech Canopy Mythical Jungle RPG.")
                            ) {
                                Box(modifier = Modifier.height(210.dp)) {
                                    Image(
                                        painter = painterResource(id = R.drawable.img_jungle_canopy_hero),
                                        contentDescription = stringResource(R.string.cd_jungle_hero),
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(
                                                        Color(0x440B1A13),
                                                        Color(0xB80B1A13),
                                                        Color(0xF0091610)
                                                    )
                                                )
                                            )
                                    )
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(20.dp),
                                        verticalArrangement = Arrangement.Bottom
                                    ) {
                                        Text(
                                            text = stringResource(R.string.app_name),
                                            style = MaterialTheme.typography.displayMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Solari Magitech Canopy · Mythical Jungle RPG",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Color(0xFFFFE7A3)
                                        )
                                    }
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(26.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = if (calmMode) 1f else 0.96f),
                            tonalElevation = if (calmMode) 2.dp else 4.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .speakOnTouch(
                                    "Fernfable Sign In. Sign in with Google to save your explorers, creatures, quests, and puzzle streaks."
                                )
                        ) {
                            Column(
                                modifier = Modifier.padding(if (calmMode) 24.dp else 22.dp),
                                verticalArrangement = Arrangement.spacedBy(if (calmMode) 16.dp else 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (calmMode) {
                                    Text(
                                        text = stringResource(R.string.app_name),
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                Text(
                                    text = "Step into the Whispering Canopy",
                                    style = MaterialTheme.typography.headlineSmall,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Sign in with Google to save your Solari explorers, mythical creature bonds, quest lore, and 7 brain-teaser puzzle streaks across devices.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )

                                if (!calmMode) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        AuthFeatureRow(
                                            icon = Icons.Rounded.Extension,
                                            tint = FireflyGoldAccent,
                                            text = "7 anytime mind-teasers for Memory, Logic, Observation & Spatial skills"
                                        )
                                        AuthFeatureRow(
                                            icon = Icons.Rounded.Pets,
                                            tint = DewTealAccent,
                                            text = "Befriend magical creatures & unlock heroes of every realm"
                                        )
                                        AuthFeatureRow(
                                            icon = Icons.Rounded.CloudDone,
                                            tint = StarlightVioletAccent,
                                            text = "Automatic Cloud Firestore sync scoped to your explorer profile"
                                        )
                                    }
                                }

                                if (statusMessage != null) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .speakOnTouch(statusMessage ?: "")
                                    ) {
                                        Text(
                                            text = statusMessage!!,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }

                                Button(
                                    onClick = ::launchInteractiveGoogleSignIn,
                                    enabled = !isLoading,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .speakOnTouch("Continue with Google button. Sign in to start playing Fernfable.")
                                        .testTag("google_sign_in_button")
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.5.dp
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Connecting Sanctuary...")
                                    } else {
                                        Icon(
                                            imageVector = Icons.Rounded.AutoAwesome,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Continue with Google",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Button(
                                    onClick = ::launchAnonymousSignIn,
                                    enabled = !isLoading,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .speakOnTouch("Anonymous Sign-In button. Sign in to start playing Fernfable.")
                                        .testTag("anonymous_sign_in_button")
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.5.dp
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Connecting Sanctuary...")
                                    } else {
                                        Icon(
                                            imageVector = Icons.Rounded.AutoAwesome,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Anonymous Sign-In (No Google Account)",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthFeatureRow(
    icon: ImageVector,
    tint: Color,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = tint.copy(alpha = 0.16f),
            modifier = Modifier.size(34.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
