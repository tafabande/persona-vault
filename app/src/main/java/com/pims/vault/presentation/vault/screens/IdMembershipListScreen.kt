package com.pims.vault.presentation.vault.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.core.model.VaultCategory
import com.pims.vault.domain.model.VaultItemHeader
import com.pims.vault.presentation.ui.theme.tactilePress

@Composable
fun IdMembershipListScreen(
    initialCategory: String = "All",
    items: List<VaultItemHeader>,
    onOpenItem: (VaultItemHeader) -> Unit,
    onAddNew: (String) -> Unit,
    onBack: () -> Unit,
    onCopyNumber: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    val categories = listOf("All", "National ID", "School", "Library", "Medical aid", "Custom")

    val idItems = items.filter { it.category == VaultCategory.IDENTITY_CREDENTIAL }

    val filteredItems = idItems.filter { item ->
        val matchesCategory = when (selectedCategory) {
            "All" -> true
            "National ID" -> item.title.contains("National", ignoreCase = true) || item.accountIdentifier?.contains("NATIONAL_ID", ignoreCase = true) == true
            "School" -> item.title.contains("School", ignoreCase = true) || item.accountIdentifier?.contains("SCHOOL_ID", ignoreCase = true) == true
            "Library" -> item.title.contains("Library", ignoreCase = true) || item.accountIdentifier?.contains("LIBRARY_ID", ignoreCase = true) == true
            "Medical aid" -> item.title.contains("Medical", ignoreCase = true) || item.accountIdentifier?.contains("MEDICAL_AID", ignoreCase = true) == true
            "Custom" -> item.title.contains("Custom", ignoreCase = true) || item.accountIdentifier?.contains("CUSTOM", ignoreCase = true) == true
            else -> item.title.contains(selectedCategory, ignoreCase = true)
        }

        val matchesSearch = searchQuery.isBlank() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                (item.accountIdentifier?.contains(searchQuery, ignoreCase = true) == true)

        matchesCategory && matchesSearch
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(getVaultBackground())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = getVaultTextPrimary(),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                if (!isSearchActive) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (selectedCategory == "All") "IDs & memberships" else "$selectedCategory IDs",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = getVaultTextPrimary()
                        )
                        Text(
                            text = "${filteredItems.size} credential${if (filteredItems.size != 1) "s" else ""}",
                            fontSize = 12.sp,
                            color = getVaultTextSecondary()
                        )
                    }

                    IconButton(
                        onClick = { isSearchActive = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = getVaultTextSecondary(),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = { onAddNew(if (selectedCategory == "All") "National ID" else selectedCategory) },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(getVaultCardSurface())
                            .border(1.dp, getVaultBorder(), CircleShape)
                            .tactilePress()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add ID",
                            tint = getVaultTextPrimary(),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(getVaultCardSurface())
                            .border(1.dp, getVaultBorder(), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = getVaultTextSecondary(),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = getVaultTextPrimary(),
                                fontSize = 14.sp
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = getVaultTextSecondary(),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            isSearchActive = false
                            searchQuery = ""
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close search",
                            tint = getVaultTextSecondary(),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Category Filter Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    val isSelected = selectedCategory == category
                    val pillBg = if (isSelected) getVaultTextPrimary() else getVaultCardSurface()
                    val pillText = if (isSelected) getVaultBackground() else getVaultTextSecondary()

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .border(
                                width = 1.dp,
                                color = if (isSelected) getVaultTextPrimary() else getVaultBorder(),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { selectedCategory = category }
                            .tactilePress(),
                        shape = RoundedCornerShape(20.dp),
                        color = pillBg
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (category != "All") {
                                IdCategoryThemedBadge(
                                    categoryName = category,
                                    size = 18.dp,
                                    iconSize = 12.dp
                                )
                            }
                            Text(
                                text = category,
                                fontSize = 12.5.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = pillText
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content List or Empty State
            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        IdCategoryThemedBadge(
                            categoryName = if (selectedCategory == "All") "National ID" else selectedCategory,
                            size = 64.dp,
                            iconSize = 34.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (selectedCategory == "All") "No IDs or Memberships Added" else "No $selectedCategory Added",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = getVaultTextPrimary()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Add and store your $selectedCategory cards.",
                            fontSize = 13.sp,
                            color = getVaultTextSecondary(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { onAddNew(if (selectedCategory == "All") "National ID" else selectedCategory) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = getVaultTextPrimary(),
                                contentColor = getVaultBackground()
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.tactilePress()
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Add $selectedCategory",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        IdCredentialCard(
                            item = item,
                            onClick = { onOpenItem(item) },
                            onCopy = onCopyNumber
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IdCredentialCard(
    item: VaultItemHeader,
    onClick: () -> Unit,
    onCopy: (String, String) -> Unit
) {
    val identifier = item.accountIdentifier ?: ""
    val rawId = if (identifier.contains("•")) {
        identifier.substringAfter("•").trim()
    } else {
        identifier
    }

    val maskedId = if (rawId.length > 4) {
        "•••• " + rawId.takeLast(4)
    } else if (rawId.isNotBlank()) {
        rawId
    } else {
        "Verified ID"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, getVaultBorder(), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .tactilePress(),
        shape = RoundedCornerShape(16.dp),
        color = getVaultCardSurface()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IdCategoryThemedBadge(
                categoryName = item.title,
                size = 44.dp,
                iconSize = 22.dp
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = getVaultTextPrimary(),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = maskedId,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        color = getVaultTextSecondary()
                    )
                    if (rawId.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = { onCopy(rawId, "${item.title} ID Number") },
                            modifier = Modifier.size(20.dp).tactilePress()
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy ID",
                                tint = getVaultTextSecondary().copy(alpha = 0.7f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = getVaultTextSecondary().copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
