package com.pims.vault.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.core.util.CountryUtils

/**
 * Country Calling Code metadata model.
 */
data class CountryCallingInfo(
    val countryCode: String, // 2-letter ISO, e.g. "ZW"
    val name: String,        // e.g. "Zimbabwe"
    val flag: String,        // e.g. "🇿🇼"
    val callingCode: String, // e.g. "+263"
    val example: String = "77 123 4567"
)

/**
 * Top Regional & Popular Calling Codes.
 */
val PopularCallingCodes: List<CountryCallingInfo> = listOf(
    CountryCallingInfo("ZW", "Zimbabwe", "🇿🇼", "+263", "77 123 4567"),
    CountryCallingInfo("ZA", "South Africa", "🇿🇦", "+27", "82 123 4567"),
    CountryCallingInfo("BW", "Botswana", "🇧🇼", "+267", "71 234 567"),
    CountryCallingInfo("ZM", "Zambia", "🇿🇲", "+260", "97 123 4567"),
    CountryCallingInfo("MZ", "Mozambique", "🇲🇿", "+258", "84 123 4567"),
    CountryCallingInfo("NA", "Namibia", "🇳🇦", "+264", "81 123 4567"),
    CountryCallingInfo("MW", "Malawi", "🇲🇼", "+265", "99 123 4567"),
    CountryCallingInfo("KE", "Kenya", "🇰🇪", "+254", "712 345 678"),
    CountryCallingInfo("NG", "Nigeria", "🇳🇬", "+234", "802 123 4567"),
    CountryCallingInfo("GB", "United Kingdom", "🇬🇧", "+44", "7911 123456"),
    CountryCallingInfo("US", "United States", "🇺🇸", "+1", "202 555 0123"),
    CountryCallingInfo("CA", "Canada", "🇨🇦", "+1", "416 555 0123"),
    CountryCallingInfo("AU", "Australia", "🇦🇺", "+61", "412 345 678"),
    CountryCallingInfo("AE", "United Arab Emirates", "🇦🇪", "+971", "50 123 4567"),
    CountryCallingInfo("IN", "India", "🇮🇳", "+91", "98765 43210"),
    CountryCallingInfo("CN", "China", "🇨🇳", "+86", "138 0013 8000"),
    CountryCallingInfo("DE", "Germany", "🇩🇪", "+49", "151 23456789")
)

/**
 * Comprehensive World Country Calling Codes list.
 */
val AllWorldCallingCodes: List<CountryCallingInfo> by lazy {
    listOf(
        CountryCallingInfo("AF", "Afghanistan", "🇦🇫", "+93"),
        CountryCallingInfo("AL", "Albania", "🇦🇱", "+355"),
        CountryCallingInfo("DZ", "Algeria", "🇩🇿", "+213"),
        CountryCallingInfo("AD", "Andorra", "🇦🇩", "+376"),
        CountryCallingInfo("AO", "Angola", "🇦🇴", "+244"),
        CountryCallingInfo("AR", "Argentina", "🇦🇷", "+54"),
        CountryCallingInfo("AM", "Armenia", "🇦🇲", "+374"),
        CountryCallingInfo("AU", "Australia", "🇦🇺", "+61", "412 345 678"),
        CountryCallingInfo("AT", "Austria", "🇦🇹", "+43"),
        CountryCallingInfo("AZ", "Azerbaijan", "🇦🇿", "+994"),
        CountryCallingInfo("BS", "Bahamas", "🇧🇸", "+1242"),
        CountryCallingInfo("BH", "Bahrain", "🇧🇭", "+973"),
        CountryCallingInfo("BD", "Bangladesh", "🇧🇩", "+880"),
        CountryCallingInfo("BB", "Barbados", "🇧🇧", "+1246"),
        CountryCallingInfo("BY", "Belarus", "🇧🇾", "+375"),
        CountryCallingInfo("BE", "Belgium", "🇧🇪", "+32"),
        CountryCallingInfo("BZ", "Belize", "🇧🇿", "+501"),
        CountryCallingInfo("BJ", "Benin", "🇧🇯", "+229"),
        CountryCallingInfo("BT", "Bhutan", "🇧🇹", "+975"),
        CountryCallingInfo("BO", "Bolivia", "🇧🇴", "+591"),
        CountryCallingInfo("BA", "Bosnia and Herzegovina", "🇧🇦", "+387"),
        CountryCallingInfo("BW", "Botswana", "🇧🇼", "+267", "71 234 567"),
        CountryCallingInfo("BR", "Brazil", "🇧🇷", "+55"),
        CountryCallingInfo("BN", "Brunei", "🇧🇳", "+673"),
        CountryCallingInfo("BG", "Bulgaria", "🇧🇬", "+359"),
        CountryCallingInfo("BF", "Burkina Faso", "🇧🇫", "+226"),
        CountryCallingInfo("BI", "Burundi", "🇧🇮", "+257"),
        CountryCallingInfo("KH", "Cambodia", "🇰🇭", "+855"),
        CountryCallingInfo("CM", "Cameroon", "🇨🇲", "+237"),
        CountryCallingInfo("CA", "Canada", "🇨🇦", "+1", "416 555 0123"),
        CountryCallingInfo("CV", "Cape Verde", "🇨🇻", "+238"),
        CountryCallingInfo("CF", "Central African Republic", "🇨🇫", "+236"),
        CountryCallingInfo("TD", "Chad", "🇹🇩", "+235"),
        CountryCallingInfo("CL", "Chile", "🇨🇱", "+56"),
        CountryCallingInfo("CN", "China", "🇨🇳", "+86", "138 0013 8000"),
        CountryCallingInfo("CO", "Colombia", "🇨🇴", "+57"),
        CountryCallingInfo("KM", "Comoros", "🇰🇲", "+269"),
        CountryCallingInfo("CG", "Congo", "🇨🇬", "+242"),
        CountryCallingInfo("CD", "Congo (DRC)", "🇨🇩", "+243"),
        CountryCallingInfo("CR", "Costa Rica", "🇨🇷", "+506"),
        CountryCallingInfo("HR", "Croatia", "🇭🇷", "+385"),
        CountryCallingInfo("CU", "Cuba", "🇨🇺", "+53"),
        CountryCallingInfo("CY", "Cyprus", "🇨🇾", "+357"),
        CountryCallingInfo("CZ", "Czech Republic", "🇨🇿", "+420"),
        CountryCallingInfo("DK", "Denmark", "🇩🇰", "+45"),
        CountryCallingInfo("DJ", "Djibouti", "🇩🇯", "+253"),
        CountryCallingInfo("DM", "Dominica", "🇩🇲", "+1767"),
        CountryCallingInfo("DO", "Dominican Republic", "🇩🇴", "+1809"),
        CountryCallingInfo("EC", "Ecuador", "🇪🇨", "+593"),
        CountryCallingInfo("EG", "Egypt", "🇪🇬", "+20"),
        CountryCallingInfo("SV", "El Salvador", "🇸🇻", "+503"),
        CountryCallingInfo("GQ", "Equatorial Guinea", "🇬🇶", "+240"),
        CountryCallingInfo("ER", "Eritrea", "🇪🇷", "+291"),
        CountryCallingInfo("EE", "Estonia", "🇪🇪", "+372"),
        CountryCallingInfo("SZ", "Eswatini", "🇸🇿", "+268"),
        CountryCallingInfo("ET", "Ethiopia", "🇪🇹", "+251"),
        CountryCallingInfo("FJ", "Fiji", "🇫🇯", "+679"),
        CountryCallingInfo("FI", "Finland", "🇫🇮", "+358"),
        CountryCallingInfo("FR", "France", "🇫🇷", "+33"),
        CountryCallingInfo("GA", "Gabon", "🇬🇦", "+241"),
        CountryCallingInfo("GM", "Gambia", "🇬🇲", "+220"),
        CountryCallingInfo("GE", "Georgia", "🇬🇪", "+995"),
        CountryCallingInfo("DE", "Germany", "🇩🇪", "+49", "151 23456789"),
        CountryCallingInfo("GH", "Ghana", "🇬🇭", "+233"),
        CountryCallingInfo("GR", "Greece", "🇬🇷", "+30"),
        CountryCallingInfo("GT", "Guatemala", "🇬🇹", "+502"),
        CountryCallingInfo("GN", "Guinea", "🇬🇳", "+224"),
        CountryCallingInfo("GY", "Guyana", "🇬🇾", "+592"),
        CountryCallingInfo("HT", "Haiti", "🇭🇹", "+509"),
        CountryCallingInfo("HN", "Honduras", "🇭🇳", "+504"),
        CountryCallingInfo("HK", "Hong Kong", "🇭🇰", "+852"),
        CountryCallingInfo("HU", "Hungary", "🇭🇺", "+36"),
        CountryCallingInfo("IS", "Iceland", "🇮🇸", "+354"),
        CountryCallingInfo("IN", "India", "🇮🇳", "+91", "98765 43210"),
        CountryCallingInfo("ID", "Indonesia", "🇮🇩", "+62"),
        CountryCallingInfo("IR", "Iran", "🇮🇷", "+98"),
        CountryCallingInfo("IQ", "Iraq", "🇮🇶", "+964"),
        CountryCallingInfo("IE", "Ireland", "🇮🇪", "+353"),
        CountryCallingInfo("IL", "Israel", "🇮🇱", "+972"),
        CountryCallingInfo("IT", "Italy", "🇮🇹", "+39"),
        CountryCallingInfo("JM", "Jamaica", "🇯🇲", "+1876"),
        CountryCallingInfo("JP", "Japan", "🇯🇵", "+81"),
        CountryCallingInfo("JO", "Jordan", "🇯🇴", "+962"),
        CountryCallingInfo("KZ", "Kazakhstan", "🇰🇿", "+7"),
        CountryCallingInfo("KE", "Kenya", "🇰🇪", "+254", "712 345 678"),
        CountryCallingInfo("KW", "Kuwait", "🇰🇼", "+965"),
        CountryCallingInfo("KG", "Kyrgyzstan", "🇰🇬", "+996"),
        CountryCallingInfo("LA", "Laos", "🇱🇦", "+856"),
        CountryCallingInfo("LV", "Latvia", "🇱🇻", "+371"),
        CountryCallingInfo("LB", "Lebanon", "🇱🇧", "+961"),
        CountryCallingInfo("LS", "Lesotho", "🇱🇸", "+266"),
        CountryCallingInfo("LR", "Liberia", "🇱🇷", "+231"),
        CountryCallingInfo("LY", "Libya", "🇱🇾", "+218"),
        CountryCallingInfo("LI", "Liechtenstein", "🇱🇮", "+423"),
        CountryCallingInfo("LT", "Lithuania", "🇱🇹", "+370"),
        CountryCallingInfo("LU", "Luxembourg", "🇱🇺", "+352"),
        CountryCallingInfo("MG", "Madagascar", "🇲🇬", "+261"),
        CountryCallingInfo("MW", "Malawi", "🇲🇼", "+265", "99 123 4567"),
        CountryCallingInfo("MY", "Malaysia", "🇲🇾", "+60"),
        CountryCallingInfo("MV", "Maldives", "🇲🇻", "+960"),
        CountryCallingInfo("ML", "Mali", "🇲🇱", "+223"),
        CountryCallingInfo("MT", "Malta", "🇲🇹", "+356"),
        CountryCallingInfo("MR", "Mauritania", "🇲🇷", "+222"),
        CountryCallingInfo("MU", "Mauritius", "🇲🇺", "+230"),
        CountryCallingInfo("MX", "Mexico", "🇲🇽", "+52"),
        CountryCallingInfo("MD", "Moldova", "🇲🇩", "+373"),
        CountryCallingInfo("MC", "Monaco", "🇲🇨", "+377"),
        CountryCallingInfo("MN", "Mongolia", "🇲🇳", "+976"),
        CountryCallingInfo("ME", "Montenegro", "🇲🇪", "+382"),
        CountryCallingInfo("MA", "Morocco", "🇲🇦", "+212"),
        CountryCallingInfo("MZ", "Mozambique", "🇲🇿", "+258", "84 123 4567"),
        CountryCallingInfo("MM", "Myanmar", "🇲🇲", "+95"),
        CountryCallingInfo("NA", "Namibia", "🇳🇦", "+264", "81 123 4567"),
        CountryCallingInfo("NP", "Nepal", "🇳🇵", "+977"),
        CountryCallingInfo("NL", "Netherlands", "🇳🇱", "+31"),
        CountryCallingInfo("NZ", "New Zealand", "🇳🇿", "+64"),
        CountryCallingInfo("NI", "Nicaragua", "🇳🇮", "+505"),
        CountryCallingInfo("NE", "Niger", "🇳🇪", "+227"),
        CountryCallingInfo("NG", "Nigeria", "🇳🇬", "+234", "802 123 4567"),
        CountryCallingInfo("NO", "Norway", "🇳🇴", "+47"),
        CountryCallingInfo("OM", "Oman", "🇴🇲", "+968"),
        CountryCallingInfo("PK", "Pakistan", "🇵🇰", "+92"),
        CountryCallingInfo("PS", "Palestine", "🇵🇸", "+970"),
        CountryCallingInfo("PA", "Panama", "🇵🇦", "+507"),
        CountryCallingInfo("PG", "Papua New Guinea", "🇵🇬", "+675"),
        CountryCallingInfo("PY", "Paraguay", "🇵🇾", "+595"),
        CountryCallingInfo("PE", "Peru", "🇵🇪", "+51"),
        CountryCallingInfo("PH", "Philippines", "🇵🇭", "+63"),
        CountryCallingInfo("PL", "Poland", "🇵🇱", "+48"),
        CountryCallingInfo("PT", "Portugal", "🇵🇹", "+351"),
        CountryCallingInfo("QA", "Qatar", "🇶🇦", "+974"),
        CountryCallingInfo("RO", "Romania", "🇷🇴", "+40"),
        CountryCallingInfo("RU", "Russia", "🇷🇺", "+7"),
        CountryCallingInfo("RW", "Rwanda", "🇷🇼", "+250"),
        CountryCallingInfo("SA", "Saudi Arabia", "🇸🇦", "+966"),
        CountryCallingInfo("SN", "Senegal", "🇸🇳", "+221"),
        CountryCallingInfo("RS", "Serbia", "🇷🇸", "+381"),
        CountryCallingInfo("SC", "Seychelles", "🇸🇨", "+248"),
        CountryCallingInfo("SL", "Sierra Leone", "🇸🇱", "+232"),
        CountryCallingInfo("SG", "Singapore", "🇸🇬", "+65"),
        CountryCallingInfo("SK", "Slovakia", "🇸🇰", "+421"),
        CountryCallingInfo("SI", "Slovenia", "🇸🇮", "+386"),
        CountryCallingInfo("SO", "Somalia", "🇸🇴", "+252"),
        CountryCallingInfo("ZA", "South Africa", "🇿🇦", "+27", "82 123 4567"),
        CountryCallingInfo("KR", "South Korea", "🇰🇷", "+82"),
        CountryCallingInfo("SS", "South Sudan", "🇸🇸", "+211"),
        CountryCallingInfo("ES", "Spain", "🇪🇸", "+34"),
        CountryCallingInfo("LK", "Sri Lanka", "🇱🇰", "+94"),
        CountryCallingInfo("SD", "Sudan", "🇸🇩", "+249"),
        CountryCallingInfo("SE", "Sweden", "🇸🇪", "+46"),
        CountryCallingInfo("CH", "Switzerland", "🇨🇭", "+41"),
        CountryCallingInfo("SY", "Syria", "🇸🇾", "+963"),
        CountryCallingInfo("TW", "Taiwan", "🇹🇼", "+886"),
        CountryCallingInfo("TZ", "Tanzania", "🇹🇿", "+255"),
        CountryCallingInfo("TH", "Thailand", "🇹🇭", "+66"),
        CountryCallingInfo("TG", "Togo", "🇹🇬", "+228"),
        CountryCallingInfo("TT", "Trinidad and Tobago", "🇹🇹", "+1868"),
        CountryCallingInfo("TN", "Tunisia", "🇹🇳", "+216"),
        CountryCallingInfo("TR", "Turkey", "🇹🇷", "+90"),
        CountryCallingInfo("UG", "Uganda", "🇺🇬", "+256"),
        CountryCallingInfo("UA", "Ukraine", "🇺🇦", "+380"),
        CountryCallingInfo("AE", "United Arab Emirates", "🇦🇪", "+971", "50 123 4567"),
        CountryCallingInfo("GB", "United Kingdom", "🇬🇧", "+44", "7911 123456"),
        CountryCallingInfo("US", "United States", "🇺🇸", "+1", "202 555 0123"),
        CountryCallingInfo("UY", "Uruguay", "🇺🇾", "+598"),
        CountryCallingInfo("UZ", "Uzbekistan", "🇺🇿", "+998"),
        CountryCallingInfo("VE", "Venezuela", "🇻🇪", "+58"),
        CountryCallingInfo("VN", "Vietnam", "🇻🇳", "+84"),
        CountryCallingInfo("YE", "Yemen", "🇾🇪", "+967"),
        CountryCallingInfo("ZM", "Zambia", "🇿🇲", "+260", "97 123 4567"),
        CountryCallingInfo("ZW", "Zimbabwe", "🇿🇼", "+263", "77 123 4567")
    ).sortedBy { it.name }
}

/**
 * Parses raw input into a matching CountryCallingInfo and stripped national digits.
 */
fun parsePhoneInput(input: String, defaultCountry: CountryCallingInfo = PopularCallingCodes[0]): Pair<CountryCallingInfo, String> {
    val clean = input.trim()
    if (clean.isBlank()) return Pair(defaultCountry, "")

    if (clean.startsWith("+")) {
        // Try to match longest calling code first to avoid prefix collisions (e.g. +1876 before +1)
        val sortedByCodeLen = AllWorldCallingCodes.sortedByDescending { it.callingCode.length }
        for (info in sortedByCodeLen) {
            if (clean.startsWith(info.callingCode)) {
                val rest = clean.removePrefix(info.callingCode).filter { it.isDigit() }.trimStart('0')
                return Pair(info, rest)
            }
        }
    }

    // No '+' prefix or not matched: check if starts with known calling code digits without '+'
    for (info in PopularCallingCodes) {
        val bareCode = info.callingCode.removePrefix("+")
        if (clean.startsWith(bareCode) && clean.length > bareCode.length + 5) {
            val rest = clean.removePrefix(bareCode).filter { it.isDigit() }.trimStart('0')
            return Pair(info, rest)
        }
    }

    // Default to provided default country, stripping trunk 0
    val rawDigits = clean.filter { it.isDigit() }
    val nationalDigits = if (rawDigits.startsWith("0")) rawDigits.drop(1) else rawDigits
    return Pair(defaultCountry, nationalDigits)
}

/**
 * Format local digits into human-readable spaced chunks.
 */
fun formatNationalDigits(digits: String, callingCode: String): String {
    val d = digits.filter { it.isDigit() }
    return when {
        d.isEmpty() -> ""
        // North America (+1): 3-3-4
        callingCode == "+1" -> {
            when {
                d.length <= 3 -> d
                d.length <= 6 -> "${d.take(3)} ${d.drop(3)}"
                else -> "${d.take(3)} ${d.substring(3, 6)} ${d.drop(6)}"
            }
        }
        // Standard mobile (e.g. Zimbabwe +263, South Africa +27, UK +44): 2-3-4 or 2-3-3-4
        else -> {
            when {
                d.length <= 2 -> d
                d.length <= 5 -> "${d.take(2)} ${d.drop(2)}"
                d.length <= 9 -> "${d.take(2)} ${d.substring(2, 5)} ${d.drop(5)}"
                else -> "${d.take(2)} ${d.substring(2, 5)} ${d.substring(5, 8)} ${d.drop(8)}"
            }
        }
    }
}

/**
 * VisualTransformation that formats phone digits with clean spaced grouping (e.g. "77 123 4567")
 * while preserving pure digits in the input buffer.
 * With bidirectional OffsetMapping, the cursor NEVER jumps around or shifts unpredictably.
 */
class PhoneVisualTransformation(private val callingCode: String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val formatted = formatNationalDigits(digits, callingCode)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                val clamped = offset.coerceAtMost(digits.length)
                val sub = digits.take(clamped)
                return formatNationalDigits(sub, callingCode).length
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                val clamped = offset.coerceAtMost(formatted.length)
                val sub = formatted.take(clamped)
                return sub.count { it.isDigit() }.coerceAtMost(digits.length)
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}

/**
 * Universal International Phone Input with Country Picker.
 *
 * Places a clickable country flag and dial code prefix (such as +263 for Zimbabwe or +27 for South Africa)
 * in an attached prefix box, auto-formatting the local number as the user types, and providing
 * a searchable modal country picker.
 *
 * @param value The raw or canonical phone number.
 * @param onValueChange Callback emitting the canonical E.164 phone number (e.g. "+263771234567") or "" when blank.
 * @param label Optional input label displayed above the field.
 * @param placeholder Optional placeholder text for the local number.
 * @param isError Whether an error state should be indicated.
 * @param errorMessage Optional error message shown below the input.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InternationalPhoneInput(
    value: String,
    onValueChange: (canonicalPhone: String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = "Phone Number",
    placeholder: String? = null,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    // Initial parsing
    val initialPair = remember(value) { parsePhoneInput(value) }
    var selectedCountry by remember { mutableStateOf(initialPair.first) }
    var rawDigits by remember { mutableStateOf(initialPair.second) }
    var isPickerOpen by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }

    // Sync state when external value changes meaningfully
    LaunchedEffect(value) {
        val (parsedCountry, parsedDigits) = parsePhoneInput(value, selectedCountry)
        if (value.startsWith("+") || parsedCountry.callingCode != selectedCountry.callingCode || parsedDigits != rawDigits) {
            selectedCountry = parsedCountry
            rawDigits = parsedDigits
        }
    }

    val phoneVisualTransformation = remember(selectedCountry.callingCode) {
        PhoneVisualTransformation(selectedCountry.callingCode)
    }

    val effectivePlaceholder = placeholder ?: selectedCountry.example

    Column(modifier = modifier.fillMaxWidth()) {
        if (!label.isNullOrBlank()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 6.dp, start = 1.dp)
            )
        }

        // Unified Attached Input Container
        val borderColor = when {
            isError -> MaterialTheme.colorScheme.error
            isFocused -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            border = androidx.compose.foundation.BorderStroke(if (isFocused || isError) 1.5.dp else 1.dp, borderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Attached Country Prefix Box
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                        .clickable { isPickerOpen = true }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = selectedCountry.flag,
                        fontSize = 20.sp
                    )
                    Text(
                        text = selectedCountry.callingCode,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select country calling code",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Vertical Divider between prefix box and number field
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(28.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                )

                // National Number Text Field
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (rawDigits.isEmpty()) {
                        Text(
                            text = effectivePlaceholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }

                    BasicTextField(
                        value = rawDigits,
                        onValueChange = { input ->
                            // If user pasted a number with '+' or country code
                            if (input.contains("+")) {
                                val (pCountry, pDigits) = parsePhoneInput(input, selectedCountry)
                                selectedCountry = pCountry
                                rawDigits = pDigits
                                val canonical = if (pDigits.isNotBlank()) "${pCountry.callingCode}$pDigits" else ""
                                onValueChange(canonical)
                            } else {
                                val cleaned = input.filter { it.isDigit() }
                                // Strip leading 0 if entered (e.g. 077 -> 77)
                                val sanitizedDigits = if (cleaned.startsWith("0")) cleaned.drop(1) else cleaned
                                rawDigits = sanitizedDigits
                                val canonical = if (sanitizedDigits.isNotBlank()) "${selectedCountry.callingCode}$sanitizedDigits" else ""
                                onValueChange(canonical)
                            }
                        },
                        visualTransformation = phoneVisualTransformation,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Done
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isFocused = it.isFocused }
                    )
                }

                // Clear Button (when digits exist)
                if (rawDigits.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            rawDigits = ""
                            onValueChange("")
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear phone number",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Error message if any
        if (isError && !errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
    }

    // Searchable Country Picker Bottom Sheet
    if (isPickerOpen) {
        CountryPickerSheet(
            selectedCountry = selectedCountry,
            onCountrySelected = { country ->
                selectedCountry = country
                isPickerOpen = false
                val canonical = if (rawDigits.isNotBlank()) "${country.callingCode}$rawDigits" else ""
                onValueChange(canonical)
            },
            onDismiss = { isPickerOpen = false }
        )
    }
}

/**
 * Searchable Country Code Selector Sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryPickerSheet(
    selectedCountry: CountryCallingInfo,
    onCountrySelected: (CountryCallingInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    val filteredCountries = remember(searchQuery) {
        val query = searchQuery.trim().lowercase()
        if (query.isEmpty()) {
            AllWorldCallingCodes
        } else {
            AllWorldCallingCodes.filter { country ->
                country.name.lowercase().contains(query) ||
                country.callingCode.contains(query) ||
                country.countryCode.lowercase().contains(query)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header with Close
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Select Country",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Choose country calling code prefix",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search country, dial code or ISO...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            // Country List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Show Popular section if search is empty
                if (searchQuery.isBlank()) {
                    item {
                        Text(
                            text = "POPULAR & REGIONAL",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                        )
                    }

                    items(PopularCallingCodes) { country ->
                        CountryRowItem(
                            country = country,
                            isSelected = country.countryCode == selectedCountry.countryCode,
                            onSelect = { onCountrySelected(country) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "ALL COUNTRIES",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                        )
                    }
                }

                items(filteredCountries) { country ->
                    CountryRowItem(
                        country = country,
                        isSelected = country.countryCode == selectedCountry.countryCode,
                        onSelect = { onCountrySelected(country) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CountryRowItem(
    country: CountryCallingInfo,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onSelect)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = country.flag,
                fontSize = 24.sp
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = country.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = country.countryCode,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = country.callingCode,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
