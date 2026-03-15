@file:Suppress("FunctionName")

package dk.frankbille.iou.session

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import dk.frankbille.iou.auth.ParentAuthFormState
import dk.frankbille.iou.auth.ParentAuthMode
import dk.frankbille.iou.dashboard.Clay
import dk.frankbille.iou.dashboard.ClaySoft
import dk.frankbille.iou.dashboard.Gold
import dk.frankbille.iou.dashboard.GoldSoft
import dk.frankbille.iou.dashboard.Pine
import dk.frankbille.iou.dashboard.PineSoft

@Composable
internal fun AuthScreen(
    form: ParentAuthFormState,
    onModeChange: (ParentAuthMode) -> Unit,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    SessionFrame {
        Surface(
            modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp),
            shape = RoundedCornerShape(30.dp),
            color = Color.White.copy(alpha = 0.92f),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "IOU",
                    style = MaterialTheme.typography.labelLarge,
                    color = Pine,
                )
                Text(
                    text =
                        if (form.mode == ParentAuthMode.LOGIN) {
                            "Sign in as a parent"
                        } else {
                            "Create a parent account"
                        },
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text =
                        if (form.mode == ParentAuthMode.LOGIN) {
                            "Your session stays on this device until you log out."
                        } else {
                            "Register once, store the returned JWT locally, and load your household before entering the app."
                        },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModeButton(
                        label = "Login",
                        selected = form.mode == ParentAuthMode.LOGIN,
                        onClick = { onModeChange(ParentAuthMode.LOGIN) },
                    )
                    ModeButton(
                        label = "Register",
                        selected = form.mode == ParentAuthMode.REGISTER,
                        onClick = { onModeChange(ParentAuthMode.REGISTER) },
                    )
                }

                if (form.mode == ParentAuthMode.REGISTER) {
                    OutlinedTextField(
                        value = form.name,
                        onValueChange = onNameChange,
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                OutlinedTextField(
                    value = form.email,
                    onValueChange = onEmailChange,
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = form.password,
                    onValueChange = onPasswordChange,
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )

                form.error?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Button(
                    onClick = onSubmit,
                    enabled = !form.isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (form.isSubmitting) {
                            "Working..."
                        } else if (form.mode == ParentAuthMode.LOGIN) {
                            "Log in"
                        } else {
                            "Register"
                        },
                    )
                }
            }
        }
    }
}

@Composable
internal fun LoadingScreen(message: String) {
    SessionFrame {
        Surface(
            shape = RoundedCornerShape(30.dp),
            color = Color.White.copy(alpha = 0.88f),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CircularProgressIndicator(color = Pine)
                Text(text = "Loading", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun NoFamiliesScreen(
    viewerName: String,
    viewerSummary: String,
    onRetry: () -> Unit,
    onLogout: () -> Unit,
) {
    MessageScreen(
        eyebrow = viewerName,
        title = "No family is connected yet",
        body = "$viewerSummary Once a family invites or adds this parent, reload from here.",
        primaryLabel = "Retry",
        onPrimary = onRetry,
        secondaryLabel = "Log out",
        onSecondary = onLogout,
    )
}

@Composable
internal fun SessionErrorScreen(
    message: String,
    onRetry: () -> Unit,
    onLogout: () -> Unit,
) {
    MessageScreen(
        eyebrow = "Session problem",
        title = "The dashboard could not load",
        body = message,
        primaryLabel = "Retry",
        onPrimary = onRetry,
        secondaryLabel = "Log out",
        onSecondary = onLogout,
    )
}

@Composable
internal fun SessionSummaryCard(
    viewerSummary: String,
    onLogout: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.78f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = 0.18f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Parent session",
                    style = MaterialTheme.typography.labelLarge,
                    color = Gold,
                )
                Text(
                    text = viewerSummary,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            OutlinedButton(onClick = onLogout) {
                Text("Log out")
            }
        }
    }
}

@Composable
private fun MessageScreen(
    eyebrow: String,
    title: String,
    body: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String,
    onSecondary: () -> Unit,
) {
    SessionFrame {
        Surface(
            modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
            shape = RoundedCornerShape(30.dp),
            color = Color.White.copy(alpha = 0.9f),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = eyebrow.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = Clay,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onPrimary) {
                        Text(primaryLabel)
                    }
                    TextButton(onClick = onSecondary) {
                        Text(secondaryLabel)
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) Pine else Pine.copy(alpha = 0.18f)
    val background = if (selected) PineSoft else Color.White
    val textColor = if (selected) Pine else MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = Modifier.clip(RoundedCornerShape(999.dp)),
        color = background,
        shape = RoundedCornerShape(999.dp),
    ) {
        TextButton(
            onClick = onClick,
            modifier = Modifier.border(1.dp, borderColor, RoundedCornerShape(999.dp)),
        ) {
            Text(text = label, color = textColor)
        }
    }
}

@Composable
private fun SessionFrame(content: @Composable () -> Unit) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    brush =
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFFF2E6D4), Color(0xFFF8F2E8), Color(0xFFE7E8E1)),
                        ),
                ).safeContentPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        SessionGlow(
            alignment = Alignment.TopEnd,
            size = 320.dp,
            brush = Brush.radialGradient(listOf(ClaySoft.copy(alpha = 0.65f), Color.Transparent)),
        )
        SessionGlow(
            alignment = Alignment.BottomStart,
            size = 300.dp,
            brush = Brush.radialGradient(listOf(GoldSoft.copy(alpha = 0.45f), Color.Transparent)),
        )

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

@Composable
private fun BoxScope.SessionGlow(
    alignment: Alignment,
    size: androidx.compose.ui.unit.Dp,
    brush: Brush,
) {
    Box(
        modifier =
            Modifier
                .align(alignment)
                .size(size)
                .clip(CircleShape)
                .background(brush),
    )
}
