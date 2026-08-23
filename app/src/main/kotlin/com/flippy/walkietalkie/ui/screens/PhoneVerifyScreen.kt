package com.flippy.walkietalkie.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flippy.walkietalkie.ui.theme.*

// ============================================================
//  «Ingresa tu número de teléfono» · selector de país con
//  bandera (hoja deslizante con buscador) + código automático
// ============================================================

data class CountryInfo(val name: String, val iso: String, val dial: String)

private val countries = listOf(
    CountryInfo("Afganistán", "AF", "+93"),
    CountryInfo("Albania", "AL", "+355"),
    CountryInfo("Alemania", "DE", "+49"),
    CountryInfo("Angola", "AO", "+244"),
    CountryInfo("Arabia Saudita", "SA", "+966"),
    CountryInfo("Argelia", "DZ", "+213"),
    CountryInfo("Argentina", "AR", "+54"),
    CountryInfo("Australia", "AU", "+61"),
    CountryInfo("Austria", "AT", "+43"),
    CountryInfo("Bahamas", "BS", "+1242"),
    CountryInfo("Bangladés", "BD", "+880"),
    CountryInfo("Barbados", "BB", "+1246"),
    CountryInfo("Bélgica", "BE", "+32"),
    CountryInfo("Belice", "BZ", "+501"),
    CountryInfo("Bielorrusia", "BY", "+375"),
    CountryInfo("Bolivia", "BO", "+591"),
    CountryInfo("Bosnia y Herzegovina", "BA", "+387"),
    CountryInfo("Brasil", "BR", "+55"),
    CountryInfo("Bulgaria", "BG", "+359"),
    CountryInfo("Camboya", "KH", "+855"),
    CountryInfo("Camerún", "CM", "+237"),
    CountryInfo("Canadá", "CA", "+1"),
    CountryInfo("Catar", "QA", "+974"),
    CountryInfo("Kazajistán", "KZ", "+7"),
    CountryInfo("Chile", "CL", "+56"),
    CountryInfo("China", "CN", "+86"),
    CountryInfo("Chipre", "CY", "+357"),
    CountryInfo("Colombia", "CO", "+57"),
    CountryInfo("Corea del Sur", "KR", "+82"),
    CountryInfo("Costa de Marfil", "CI", "+225"),
    CountryInfo("Costa Rica", "CR", "+506"),
    CountryInfo("Croacia", "HR", "+385"),
    CountryInfo("Cuba", "CU", "+53"),
    CountryInfo("Dinamarca", "DK", "+45"),
    CountryInfo("Ecuador", "EC", "+593"),
    CountryInfo("Egipto", "EG", "+20"),
    CountryInfo("El Salvador", "SV", "+503"),
    CountryInfo("Emiratos Árabes Unidos", "AE", "+971"),
    CountryInfo("Eslovaquia", "SK", "+421"),
    CountryInfo("Eslovenia", "SI", "+386"),
    CountryInfo("España", "ES", "+34"),
    CountryInfo("Estados Unidos", "US", "+1"),
    CountryInfo("Estonia", "EE", "+372"),
    CountryInfo("Etiopía", "ET", "+251"),
    CountryInfo("Filipinas", "PH", "+63"),
    CountryInfo("Finlandia", "FI", "+358"),
    CountryInfo("Fiyi", "FJ", "+679"),
    CountryInfo("Francia", "FR", "+33"),
    CountryInfo("Ghana", "GH", "+233"),
    CountryInfo("Grecia", "GR", "+30"),
    CountryInfo("Guatemala", "GT", "+502"),
    CountryInfo("Guyana", "GY", "+592"),
    CountryInfo("Haití", "HT", "+509"),
    CountryInfo("Honduras", "HN", "+504"),
    CountryInfo("Hong Kong", "HK", "+852"),
    CountryInfo("Hungría", "HU", "+36"),
    CountryInfo("India", "IN", "+91"),
    CountryInfo("Indonesia", "ID", "+62"),
    CountryInfo("Irak", "IQ", "+964"),
    CountryInfo("Irán", "IR", "+98"),
    CountryInfo("Irlanda", "IE", "+353"),
    CountryInfo("Islandia", "IS", "+354"),
    CountryInfo("Israel", "IL", "+972"),
    CountryInfo("Italia", "IT", "+39"),
    CountryInfo("Jamaica", "JM", "+1876"),
    CountryInfo("Japón", "JP", "+81"),
    CountryInfo("Kenia", "KE", "+254"),
    CountryInfo("Kuwait", "KW", "+965"),
    CountryInfo("Laos", "LA", "+856"),
    CountryInfo("Letonia", "LV", "+371"),
    CountryInfo("Lituania", "LT", "+370"),
    CountryInfo("Luxemburgo", "LU", "+352"),
    CountryInfo("Macedonia del Norte", "MK", "+389"),
    CountryInfo("Malasia", "MY", "+60"),
    CountryInfo("Malta", "MT", "+356"),
    CountryInfo("Marruecos", "MA", "+212"),
    CountryInfo("México", "MX", "+52"),
    CountryInfo("Moldavia", "MD", "+373"),
    CountryInfo("Mongolia", "MN", "+976"),
    CountryInfo("Montenegro", "ME", "+382"),
    CountryInfo("Myanmar", "MM", "+95"),
    CountryInfo("Nepal", "NP", "+977"),
    CountryInfo("Nicaragua", "NI", "+505"),
    CountryInfo("Nigeria", "NG", "+234"),
    CountryInfo("Noruega", "NO", "+47"),
    CountryInfo("Nueva Zelanda", "NZ", "+64"),
    CountryInfo("Países Bajos", "NL", "+31"),
    CountryInfo("Pakistán", "PK", "+92"),
    CountryInfo("Panamá", "PA", "+507"),
    CountryInfo("Papúa Nueva Guinea", "PG", "+675"),
    CountryInfo("Paraguay", "PY", "+595"),
    CountryInfo("Perú", "PE", "+51"),
    CountryInfo("Polonia", "PL", "+48"),
    CountryInfo("Portugal", "PT", "+351"),
    CountryInfo("Puerto Rico", "PR", "+1787"),
    CountryInfo("Reino Unido", "GB", "+44"),
    CountryInfo("República Checa", "CZ", "+420"),
    CountryInfo("República Dominicana", "DO", "+1809"),
    CountryInfo("Rumania", "RO", "+40"),
    CountryInfo("Rusia", "RU", "+7"),
    CountryInfo("Senegal", "SN", "+221"),
    CountryInfo("Serbia", "RS", "+381"),
    CountryInfo("Singapur", "SG", "+65"),
    CountryInfo("Sri Lanka", "LK", "+94"),
    CountryInfo("Sudáfrica", "ZA", "+27"),
    CountryInfo("Suecia", "SE", "+46"),
    CountryInfo("Suiza", "CH", "+41"),
    CountryInfo("Surinam", "SR", "+597"),
    CountryInfo("Tailandia", "TH", "+66"),
    CountryInfo("Taiwán", "TW", "+886"),
    CountryInfo("Tanzania", "TZ", "+255"),
    CountryInfo("Trinidad y Tobago", "TT", "+1868"),
    CountryInfo("Túnez", "TN", "+216"),
    CountryInfo("Turquía", "TR", "+90"),
    CountryInfo("Ucrania", "UA", "+380"),
    CountryInfo("Uganda", "UG", "+256"),
    CountryInfo("Uruguay", "UY", "+598"),
    CountryInfo("Uzbekistán", "UZ", "+998"),
    CountryInfo("Venezuela", "VE", "+58"),
    CountryInfo("Vietnam", "VN", "+84")
)

/** Bandera emoji a partir del código ISO del país. */
private fun flagEmoji(iso: String): String {
    val first = 0x1F1E6 + (iso[0].uppercaseChar() - 'A')
    val second = 0x1F1E6 + (iso[1].uppercaseChar() - 'A')
    return String(intArrayOf(first, second), 0, 2)
}

@Composable
fun PhoneVerifyScreen(
    onSavePhone: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf(countries.first { it.iso == "PE" }) }
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var showCountrySheet by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 22.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // ── Atrás ──
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f))
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Atrás",
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Ingresa tu número de teléfono",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "Al verificar tu número de teléfono podrás iniciar sesión " +
                "en esta cuenta usando este número. Además, tus amigos podrán " +
                "encontrarte más fácil.",
            fontSize = 14.sp,
            lineHeight = 19.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(4.dp))

        // ── Campo 1: país (nombre + bandera) → abre la hoja ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.14f),
                    RoundedCornerShape(18.dp)
                )
                .clickable { showCountrySheet = true }
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = flagEmoji(selected.iso), fontSize = 24.sp)
            Text(
                text = selected.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "(${selected.dial})",
                fontSize = 14.sp,
                color = TextSecondary
            )
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = "Elegir país",
                tint = TextSecondary
            )
        }

        // ── Campo 2: el código se pone SOLO según el país ──
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it.filter(Char::isDigit).take(15) },
            placeholder = { Text("999 123 456") },
            prefix = {
                Text(
                    text = selected.dial + " ",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Accent
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color.White.copy(alpha = 0.11f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.07f),
                cursorColor = Accent,
                focusedBorderColor = Accent.copy(alpha = 0.9f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.14f),
                focusedPlaceholderColor = TextTertiary,
                unfocusedPlaceholderColor = TextTertiary
            )
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                if (phoneNumber.isNotBlank()) {
                    onSavePhone(selected.dial + phoneNumber)
                    Toast.makeText(
                        context, "¡Número verificado! 📱", Toast.LENGTH_SHORT
                    ).show()
                    onBack()
                }
            },
            enabled = phoneNumber.length >= 6,
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Accent,
                contentColor = Color.Black,
                disabledContainerColor = Accent.copy(alpha = 0.35f),
                disabledContentColor = Color.Black.copy(alpha = 0.5f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Text(
                text = "Continuar",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = "Usamos tu número solo para que tus amigos te encuentren en Flippy.",
            fontSize = 12.sp,
            color = TextTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }

    // ── Hoja deslizante: Seleccionar país/región 🌎 ──
    if (showCountrySheet) {
        CountryPickerSheet(
            onSelect = {
                selected = it
                showCountrySheet = false
            },
            onDismiss = { showCountrySheet = false }
        )
    }
}

@Composable
private fun CountryPickerSheet(
    onSelect: (CountryInfo) -> Unit,
    onDismiss: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(query) {
        countries.filter {
            query.isBlank() ||
                it.name.contains(query, ignoreCase = true) ||
                it.dial.contains(query)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.78f)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Color(0xF2171D27))
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.14f),
                    RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                )
                .clickable(enabled = false) {} // no cerrar al tocar la hoja
                .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Seleccionar país/región",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Buscar por nombre o número") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color.White.copy(alpha = 0.11f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.07f),
                    cursorColor = Accent,
                    focusedBorderColor = Accent.copy(alpha = 0.9f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.14f),
                    focusedPlaceholderColor = TextTertiary,
                    unfocusedPlaceholderColor = TextTertiary
                )
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(filtered, key = { it.iso }) { country ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelect(country) }
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = flagEmoji(country.iso), fontSize = 22.sp)
                        Text(
                            text = country.name,
                            fontSize = 15.sp,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = country.dial,
                            fontSize = 14.sp,
                            color = Accent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
