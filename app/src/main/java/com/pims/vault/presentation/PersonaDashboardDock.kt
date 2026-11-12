package com.pims.vault.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
    val isReducedMotion = com.pims.vault.presentation.ui.theme.LocalReducedMotion.current

    val tabs = remember {
        listOf(
            Triple("Home", PersonaIcons.Home, PersonaIcons.HomeOutlined),
            Triple("Notes", PersonaIcons.Notes, PersonaIcons.NotesOutlined),
            Triple("Me", PersonaIcons.Me, PersonaIcons.MeOutlined),
            Triple("People", PersonaIcons.People, PersonaIcons.PeopleOutlined),
            Triple("Settings", PersonaIcons.Settings, PersonaIcons.SettingsOutlined)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            shadowElevation = 10.dp,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                val tabCount = tabs.size
                val tabWidth = maxWidth / tabCount

                val indicatorOffset by androidx.compose.animation.core.animateDpAsState(
                    targetValue = tabWidth * selectedTab,
                    animationSpec = com.pims.vault.presentation.ui.theme.PersonaMotion.pillSpring(isReducedMotion),
                    label = "dockPillOffset"
                )

                // Smooth sliding active pill background
                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset)
                        .width(tabWidth)
                        .height(52.dp)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { index, (label, filledIcon, outlinedIcon) ->
                        val isSelected = selectedTab == index
                        val activeColor = MaterialTheme.colorScheme.primary
                        val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f)

                        val tintColor by androidx.compose.animation.animateColorAsState(
                            targetValue = if (isSelected) activeColor else inactiveColor,
                            animationSpec = com.pims.vault.presentation.ui.theme.PersonaMotion.smoothSpring(isReducedMotion),
                            label = "dockItemColor_$index"
                        )

                        val iconScale by androidx.compose.animation.core.animateFloatAsState(
                            targetValue = if (isSelected) 1.15f else 1.0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "dockItemScale_$index"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(22.dp))
                                .tactilePress(targetScale = 0.92f) {
                                    if (selectedTab != index) {
                                        haptics.selection()
                                        onTabSelected(index)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (isSelected) filledIcon else outlinedIcon,
                                    contentDescription = label,
                                    tint = tintColor,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .scale(iconScale)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = tintColor,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Persistent navigation for medium and expanded windows; compact phones keep the bottom dock. */
@Composable
internal fun PersonaDashboardNavigationRail(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = remember {
        listOf(
            Triple("Home", PersonaIcons.Home, PersonaIcons.HomeOutlined),
            Triple("Notes", PersonaIcons.Notes, PersonaIcons.NotesOutlined),
            Triple("Me", PersonaIcons.Me, PersonaIcons.MeOutlined),
            Triple("People", PersonaIcons.People, PersonaIcons.PeopleOutlined),
            Triple("Settings", PersonaIcons.Settings, PersonaIcons.SettingsOutlined)
        )
    }
    val haptics = rememberPimsHaptics()

    NavigationRail(
        modifier = modifier.fillMaxHeight().navigationBarsPadding(),
        containerColor = MaterialTheme.colorScheme.background,
        header = {
            Text(
                text = "P",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 20.dp)
            )
        }
    ) {
        tabs.forEachIndexed { index, (label, selectedIcon, unselectedIcon) ->
            val selected = selectedTab == index
            NavigationRailItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        haptics.selection()
                        onTabSelected(index)
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (selected) selectedIcon else unselectedIcon,
                        contentDescription = label
                    )
                },
                label = { Text(label, maxLines = 1) },
                alwaysShowLabel = false
            )
        }
    }
}
