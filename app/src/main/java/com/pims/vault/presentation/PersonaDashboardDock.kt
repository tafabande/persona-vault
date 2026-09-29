package com.pims.vault.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.presentation.ui.theme.PersonaIcons
import com.pims.vault.presentation.ui.theme.tactilePress
import com.pims.vault.presentation.ui.util.rememberPimsFeedback
import com.pims.vault.presentation.ui.util.rememberPimsHaptics

/**
 * Extracted from PersonaDashboardScreen.kt — bottom-dock navigation components.
 * Internal visibility so the dashboard entry point keeps working unchanged.
 */

/**
 * Floating dock bottom navigation item:
 * Pill-style indicator with terracotta filled pill for selected state.
 * Icon scales up (1.08f) on selection with smooth animation.
 */
@Composable
internal fun FloatingDockItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String
) {
    val feedback = rememberPimsFeedback()
    val isReducedMotion = com.pims.vault.presentation.ui.theme.LocalReducedMotion.current
    // Terracotta filled pill for selected state
    val activeBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant

    val animatedBg by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) activeBg else Color.Transparent,
        animationSpec = com.pims.vault.presentation.ui.theme.PersonaMotion.smoothSpring(isReducedMotion),
        label = "dockItemBg"
    )
    val iconTint by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) activeColor else inactiveColor,
        animationSpec = com.pims.vault.presentation.ui.theme.PersonaMotion.smoothSpring(isReducedMotion),
        label = "dockIconTint"
    )
    // 1.08f scale for selected icon pop effect
    val iconScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (selected) 1.08f else 1.0f,
        animationSpec = com.pims.vault.presentation.ui.theme.PersonaMotion.snappySpring(isReducedMotion),
        label = "dockIconScale"
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = animatedBg,
        modifier = Modifier
            .tactilePress(targetScale = 0.94f) {
                feedback.tap()
                onClick()
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier
                    .size(20.dp)
                    .scale(iconScale)
            )
            // Label text only shown for selected tab with expand/shrink animation
            androidx.compose.animation.AnimatedVisibility(
                visible = selected,
                enter = androidx.compose.animation.fadeIn() +
                        androidx.compose.animation.expandHorizontally(androidx.compose.animation.core.spring()),
                exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(100)) +
                        androidx.compose.animation.shrinkHorizontally(androidx.compose.animation.core.tween(100))
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = activeColor
                )
            }
        }
    }
}


@Composable
internal fun PersonaDashboardBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val haptics = rememberPimsHaptics()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            shadowElevation = 8.dp,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FloatingDockItem(
                    selected = selectedTab == 0,
                    icon = PersonaIcons.navIcon(selectedTab == 0, PersonaIcons.Home, PersonaIcons.HomeOutlined),
                    label = "Home",
                    onClick = { if (selectedTab != 0) { haptics.light(); onTabSelected(0) } }
                )
                FloatingDockItem(
                    selected = selectedTab == 1,
                    icon = PersonaIcons.navIcon(selectedTab == 1, PersonaIcons.Notes, PersonaIcons.NotesOutlined),
                    label = "Notes",
                    onClick = { if (selectedTab != 1) { haptics.light(); onTabSelected(1) } }
                )
                FloatingDockItem(
                    selected = selectedTab == 2,
                    icon = PersonaIcons.navIcon(selectedTab == 2, PersonaIcons.Me, PersonaIcons.MeOutlined),
                    label = "Me",
                    onClick = { if (selectedTab != 2) { haptics.light(); onTabSelected(2) } }
                )
                FloatingDockItem(
                    selected = selectedTab == 3,
                    icon = PersonaIcons.navIcon(selectedTab == 3, PersonaIcons.People, PersonaIcons.PeopleOutlined),
                    label = "People",
                    onClick = { if (selectedTab != 3) { haptics.light(); onTabSelected(3) } }
                )
                FloatingDockItem(
                    selected = selectedTab == 4,
                    icon = PersonaIcons.navIcon(selectedTab == 4, PersonaIcons.Settings, PersonaIcons.SettingsOutlined),
                    label = "Settings",
                    onClick = { if (selectedTab != 4) { haptics.light(); onTabSelected(4) } }
                )
            }
        }
    }
}

@Composable
internal fun PersonaNavDockItem(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val haptics = rememberPimsHaptics()
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant
    val color = if (selected) activeColor else inactiveColor

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                haptics.light()
                onClick()
            }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = color
        )
    }
}
