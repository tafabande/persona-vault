package com.pims.vault.presentation.vault.password

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.presentation.ui.theme.LocalPimsDarkTheme
import java.security.SecureRandom

/**
 * Enterprise Add / Edit Password Component.
 * Features live credential card preview, brand monogram detector,
 * real-time entropy password strength evaluation, and an integrated
 * cryptographic password generator.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPasswordScreen(
    initialTitle: String = "",
    initialUsername: String = "",
    initialPassword: String = "",
    initialWebsite: String = "",
    initialNotes: String = "",
    onNavigateBack: () -> Unit,
    onSavePassword: (title: String, username: String, passwordPlain: String, websiteUrl: String?, notes: String?) -> Unit
) {
    val isDark = LocalPimsDarkTheme.current
    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current

    var title by remember { mutableStateOf(initialTitle) }
    var username by remember { mutableStateOf(initialUsername) }
    var password by remember { mutableStateOf(initialPassword) }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var websiteUrl by remember { mutableStateOf(initialWebsite) }
    var notes by remember { mutableStateOf(initialNotes) }
    var selectedCategory by remember { mutableStateOf("Personal") }

    // Generator configuration state
    var showGenerator by remember { mutableStateOf(false) }
    var genLength by remember { mutableFloatStateOf(16f) }
    var genIncludeSymbols by remember { mutableStateOf(true) }
    var genIncludeDigits by remember { mutableStateOf(true) }
    var genIncludeUpper by remember { mutableStateOf(true) }

    // Background & Surface colors
    val bgColor = if (isDark) Color(0xFF10131A) else Color(0xFFF8FAFC)
    val cardBg = if (isDark) Color(0xFF1E232F) else Color.White
    val borderColor = if (isDark) Color(0xFF2E384D) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    // Password strength metrics
    val strength = remember(password) { calculatePasswordStrength(password) }

    // Detected brand styling
    val brand = remember(title, websiteUrl) { detectBrand(title, websiteUrl) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF3B82F6).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (initialTitle.isNotBlank()) "Edit Password" else "Add New Password",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cardBg)
            )
        },
        containerColor = bgColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(2.dp))

                // 1. Live Interactive Credential Card Preview
                PasswordLivePreviewCard(
                    title = title,
                    username = username,
                    password = password,
                    isPasswordVisible = isPasswordVisible,
                    websiteUrl = websiteUrl,
                    brand = brand,
                    strength = strength,
                    isDark = isDark
                )

                // 2. Category Filter Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Personal", "Work", "Finance", "Social").forEach { cat ->
                        val isSelected = selectedCategory == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF3B82F6).copy(alpha = 0.15f),
                                selectedLabelColor = Color(0xFF3B82F6),
                                containerColor = cardBg,
                                labelColor = textSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) Color(0xFF3B82F6) else borderColor
                            )
                        )
                    }
                }

                // 3. Service / Account Name
                CleanPasswordTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "Service / Account Title",
                    placeholder = "e.g. GitHub, Google, Netflix",
                    leadingIcon = Icons.Default.Shield,
                    trailingIcon = if (title.isNotEmpty()) {
                        { IconButton(onClick = { title = "" }) { Icon(Icons.Default.Clear, contentDescription = "Clear", tint = textSecondary, modifier = Modifier.size(18.dp)) } }
                    } else null,
                    isDark = isDark,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )

                // 4. Username or Email
                CleanPasswordTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = "Username or Email",
                    placeholder = "e.g. alex@example.com",
                    leadingIcon = Icons.Default.Person,
                    trailingIcon = if (username.isNotEmpty()) {
                        { IconButton(onClick = { username = "" }) { Icon(Icons.Default.Clear, contentDescription = "Clear", tint = textSecondary, modifier = Modifier.size(18.dp)) } }
                    } else null,
                    isDark = isDark,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )

                // 5. Password Field with Reveal & Generator Trigger
                Column(modifier = Modifier.fillMaxWidth()) {
                    CleanPasswordTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        placeholder = "Enter or generate password",
                        leadingIcon = Icons.Default.Lock,
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (isPasswordVisible) "Hide" else "Show",
                                        tint = textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(onClick = {
                                    showGenerator = !showGenerator
                                    if (showGenerator && password.isEmpty()) {
                                        password = generateSecurePassword(
                                            length = genLength.toInt(),
                                            includeUppercase = genIncludeUpper,
                                            includeLowercase = true,
                                            includeDigits = genIncludeDigits,
                                            includeSymbols = genIncludeSymbols
                                        )
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.AutoFixHigh,
                                        contentDescription = "Generator",
                                        tint = if (showGenerator) Color(0xFF3B82F6) else textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        },
                        isDark = isDark,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )

                    // Strength Meter
                    if (password.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LinearProgressIndicator(
                                progress = { strength.progress },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = strength.color,
                                trackColor = if (isDark) Color(0xFF263044) else Color(0xFFE2E8F0)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = strength.label,
                                color = strength.color,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 6. Integrated Cryptographic Password Generator Panel
                AnimatedVisibility(visible = showGenerator) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoFixHigh,
                                        contentDescription = null,
                                        tint = Color(0xFF3B82F6),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Cryptographic Generator",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = textPrimary
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            password = generateSecurePassword(
                                                length = genLength.toInt(),
                                                includeUppercase = genIncludeUpper,
                                                includeLowercase = true,
                                                includeDigits = genIncludeDigits,
                                                includeSymbols = genIncludeSymbols
                                            )
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Regenerate",
                                            tint = Color(0xFF3B82F6),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(password))
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy",
                                            tint = textSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            // Length Slider
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Length", fontSize = 12.sp, color = textSecondary)
                                    Text("${genLength.toInt()} characters", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                }
                                Slider(
                                    value = genLength,
                                    onValueChange = {
                                        genLength = it
                                        password = generateSecurePassword(
                                            length = it.toInt(),
                                            includeUppercase = genIncludeUpper,
                                            includeLowercase = true,
                                            includeDigits = genIncludeDigits,
                                            includeSymbols = genIncludeSymbols
                                        )
                                    },
                                    valueRange = 8f..32f,
                                    steps = 23,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF3B82F6),
                                        activeTrackColor = Color(0xFF3B82F6)
                                    )
                                )
                            }

                            // Generator Switches
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Include Symbols (!@#$)", fontSize = 12.sp, color = textPrimary)
                                Switch(
                                    checked = genIncludeSymbols,
                                    onCheckedChange = {
                                        genIncludeSymbols = it
                                        password = generateSecurePassword(
                                            length = genLength.toInt(),
                                            includeUppercase = genIncludeUpper,
                                            includeLowercase = true,
                                            includeDigits = genIncludeDigits,
                                            includeSymbols = it
                                        )
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF3B82F6))
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Include Numbers (0-9)", fontSize = 12.sp, color = textPrimary)
                                Switch(
                                    checked = genIncludeDigits,
                                    onCheckedChange = {
                                        genIncludeDigits = it
                                        password = generateSecurePassword(
                                            length = genLength.toInt(),
                                            includeUppercase = genIncludeUpper,
                                            includeLowercase = true,
                                            includeDigits = it,
                                            includeSymbols = genIncludeSymbols
                                        )
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF3B82F6))
                                )
                            }
                        }
                    }
                }

                // 7. Website URL
                CleanPasswordTextField(
                    value = websiteUrl,
                    onValueChange = { websiteUrl = it },
                    label = "Website URL (Optional)",
                    placeholder = "https://example.com/login",
                    leadingIcon = Icons.Default.Language,
                    isDark = isDark,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )

                // 8. Secure Notes
                CleanPasswordTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = "Notes (Optional)",
                    placeholder = "Add recovery codes, security questions, or pin...",
                    leadingIcon = Icons.Default.Notes,
                    isDark = isDark,
                    singleLine = false,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                )
            }

            // 9. Bottom Primary Save Button
            Button(
                onClick = {
                    onSavePassword(
                        title.trim(),
                        username.trim(),
                        password,
                        websiteUrl.trim().ifBlank { null },
                        notes.trim().ifBlank { null }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0F172A),
                    contentColor = Color.White
                ),
                enabled = title.isNotBlank() && password.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Save Password to Vault",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ---------------------------------------------------------
// Live Interactive Credential Card
// ---------------------------------------------------------

@Composable
private fun PasswordLivePreviewCard(
    title: String,
    username: String,
    password: String,
    isPasswordVisible: Boolean,
    websiteUrl: String,
    brand: BrandInfo,
    strength: PasswordStrengthInfo,
    isDark: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = brand.gradientColors,
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(800f, 400f)
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Brand Monogram & Shield Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = brand.iconLetter,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (title.isNotBlank()) title else "Service Name",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (websiteUrl.isNotBlank()) websiteUrl.replace("https://", "").replace("http://", "").take(25) else "Persona Encrypted Vault",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = strength.label.uppercase(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Account identifier
            Text(
                text = if (username.isNotBlank()) username else "username@example.com",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Password Mask / Plaintext
            Text(
                text = if (password.isNotEmpty()) {
                    if (isPasswordVisible) password else "•".repeat(minOf(password.length, 16))
                } else "••••••••••••",
                color = Color.White,
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }
    }
}

// ---------------------------------------------------------
// Styled Text Field
// ---------------------------------------------------------

@Composable
private fun CleanPasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    isDark: Boolean = false,
    modifier: Modifier = Modifier
) {
    val cardBg = if (isDark) Color(0xFF1E232F) else Color.White
    val borderColor = if (isDark) Color(0xFF2E384D) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        placeholder = { Text(placeholder, fontSize = 13.sp, color = textSecondary.copy(alpha = 0.6f)) },
        leadingIcon = leadingIcon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        },
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = cardBg,
            unfocusedContainerColor = cardBg,
            focusedBorderColor = Color(0xFF3B82F6),
            unfocusedBorderColor = borderColor,
            focusedTextColor = textPrimary,
            unfocusedTextColor = textPrimary,
            focusedLabelColor = Color(0xFF3B82F6),
            unfocusedLabelColor = textSecondary
        ),
        modifier = modifier.fillMaxWidth()
    )
}

// ---------------------------------------------------------
// Brand Detector & Styling Info
// ---------------------------------------------------------

data class BrandInfo(
    val iconLetter: String,
    val gradientColors: List<Color>
)

private fun detectBrand(title: String, url: String): BrandInfo {
    val q = "$title $url".lowercase()
    return when {
        q.contains("google") -> BrandInfo("G", listOf(Color(0xFF4285F4), Color(0xFF1A73E8)))
        q.contains("github") -> BrandInfo("🐙", listOf(Color(0xFF24292E), Color(0xFF0F1419)))
        q.contains("apple") || q.contains("icloud") -> BrandInfo("🍎", listOf(Color(0xFF1C1C1E), Color(0xFF000000)))
        q.contains("netflix") -> BrandInfo("N", listOf(Color(0xFFE50914), Color(0xFF831010)))
        q.contains("spotify") -> BrandInfo("S", listOf(Color(0xFF1DB954), Color(0xFF128C3E)))
        q.contains("amazon") -> BrandInfo("a", listOf(Color(0xFFFF9900), Color(0xFF232F3E)))
        q.contains("microsoft") || q.contains("outlook") -> BrandInfo("M", listOf(Color(0xFF00A4EF), Color(0xFF0078D4)))
        q.contains("twitter") || q.contains(" x ") || q.startsWith("x") -> BrandInfo("𝕏", listOf(Color(0xFF14171A), Color(0xFF000000)))
        q.contains("slack") -> BrandInfo("#", listOf(Color(0xFF4A154B), Color(0xFF611f69)))
        q.contains("discord") -> BrandInfo("D", listOf(Color(0xFF5865F2), Color(0xFF404EED)))
        q.contains("bank") || q.contains("chase") || q.contains("citi") || q.contains("wells") -> BrandInfo("🏦", listOf(Color(0xFF0D9488), Color(0xFF115E59)))
        else -> {
            val letter = title.trim().firstOrNull()?.uppercase() ?: "P"
            BrandInfo(letter, listOf(Color(0xFF1E293B), Color(0xFF0F172A)))
        }
    }
}

// ---------------------------------------------------------
// Password Strength & Generator Logic
// ---------------------------------------------------------

data class PasswordStrengthInfo(
    val score: Int,
    val label: String,
    val progress: Float,
    val color: Color
)

private fun calculatePasswordStrength(password: String): PasswordStrengthInfo {
    if (password.isEmpty()) return PasswordStrengthInfo(0, "Empty", 0f, Color.Gray)

    var score = 0
    if (password.length >= 8) score++
    if (password.length >= 14) score++
    if (password.any { it.isUpperCase() } && password.any { it.isLowerCase() }) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { "!@#$%^&*()_+-=[]{}|;:,.<>?".contains(it) }) score++

    return when {
        score <= 1 -> PasswordStrengthInfo(1, "Weak", 0.25f, Color(0xFFEF4444))
        score in 2..3 -> PasswordStrengthInfo(2, "Moderate", 0.55f, Color(0xFFF59E0B))
        score == 4 -> PasswordStrengthInfo(3, "Strong", 0.85f, Color(0xFF10B981))
        else -> PasswordStrengthInfo(4, "Very Strong", 1.0f, Color(0xFF06B6D4))
    }
}

fun generateSecurePassword(
    length: Int = 16,
    includeUppercase: Boolean = true,
    includeLowercase: Boolean = true,
    includeDigits: Boolean = true,
    includeSymbols: Boolean = true
): String {
    val upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    val lower = "abcdefghijklmnopqrstuvwxyz"
    val digits = "0123456789"
    val symbols = "!@#$%^&*()_+-=[]{}|;:,.<>?"

    val pool = buildString {
        if (includeUppercase) append(upper)
        if (includeLowercase) append(lower)
        if (includeDigits) append(digits)
        if (includeSymbols) append(symbols)
    }.ifEmpty { lower + digits }

    val random = SecureRandom()
    val passwordChars = mutableListOf<Char>()

    // Guarantee at least one from each selected group
    if (includeUppercase) passwordChars.add(upper[random.nextInt(upper.length)])
    if (includeLowercase) passwordChars.add(lower[random.nextInt(lower.length)])
    if (includeDigits) passwordChars.add(digits[random.nextInt(digits.length)])
    if (includeSymbols) passwordChars.add(symbols[random.nextInt(symbols.length)])

    while (passwordChars.size < length) {
        passwordChars.add(pool[random.nextInt(pool.length)])
    }

    return passwordChars.shuffled(random).joinToString("")
}
