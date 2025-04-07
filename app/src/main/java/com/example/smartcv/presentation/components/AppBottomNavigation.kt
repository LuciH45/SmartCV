package com.example.smartcv.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.KeyboardVoice
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartcv.utils.AppColors

/**
 * Custom bottom navigation component
 *
 * @param selectedTab Currently selected tab identifier
 * @param onTabSelected Callback when a tab is selected
 */
@Composable
fun AppBottomNavigation(
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    NavigationBar(
        containerColor = androidx.compose.ui.graphics.Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        // Custom styling for navigation items to remove the selection indicator
        val navItems = listOf(
            Triple("home", if (selectedTab == "home") Icons.Filled.Home else Icons.Outlined.Home, "Home"),
            Triple("library", if (selectedTab == "library") Icons.Filled.MenuBook else Icons.Outlined.MenuBook, "Library"),
            Triple("scan", if (selectedTab == "scan") Icons.Filled.DocumentScanner else Icons.Outlined.DocumentScanner, "Scan"),
            Triple("generate", if (selectedTab == "generate") Icons.Filled.KeyboardVoice else Icons.Outlined.KeyboardVoice, "Generate"),
            Triple("settings", if (selectedTab == "settings") Icons.Filled.Settings else Icons.Outlined.Settings, "Settings")
        )

        navItems.forEach { (id, icon, label) ->
            // Custom navigation item without selection indicator
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onTabSelected(id) }
                    .padding(vertical = 8.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (selectedTab == id) AppColors.primaryTeal else AppColors.darkGray,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    color = if (selectedTab == id) AppColors.primaryTeal else AppColors.darkGray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

