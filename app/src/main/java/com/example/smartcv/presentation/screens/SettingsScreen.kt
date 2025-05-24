package com.example.smartcv.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartcv.utils.AppColors

/**
 * Settings screen for app configuration
 */
@Composable
fun SettingsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.darkGray,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 1.dp
            )
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SettingsSwitchItem(
                    title = "Dark Mode",
                    description = "Enable dark theme",
                    icon = Icons.Default.DarkMode,
                    isChecked = false,
                    onCheckedChange = { /* Handle switch */ }
                )

                Divider(modifier = Modifier.padding(horizontal = 16.dp))

                SettingsSwitchItem(
                    title = "Notifications",
                    description = "Enable push notifications",
                    icon = Icons.Default.Notifications,
                    isChecked = true,
                    onCheckedChange = { /* Handle switch */ }
                )

                Divider(modifier = Modifier.padding(horizontal = 16.dp))

                SettingsSwitchItem(
                    title = "Auto Backup",
                    description = "Backup documents to cloud",
                    icon = Icons.Default.Backup,
                    isChecked = false,
                    onCheckedChange = { /* Handle switch */ }
                )
            }
        }

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 1.dp
            )
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SettingsClickItem(
                    title = "Clear Cache",
                    description = "Free up storage space",
                    icon = Icons.Default.DeleteSweep
                )

                Divider(modifier = Modifier.padding(horizontal = 16.dp))

                SettingsClickItem(
                    title = "Privacy Policy",
                    description = "Read our privacy policy",
                    icon = Icons.Default.Policy
                )

                Divider(modifier = Modifier.padding(horizontal = 16.dp))

                SettingsClickItem(
                    title = "Terms of Service",
                    description = "Read our terms of service",
                    icon = Icons.Default.Description
                )

                Divider(modifier = Modifier.padding(horizontal = 16.dp))

                SettingsClickItem(
                    title = "About",
                    description = "Version 1.0.0",
                    icon = Icons.Default.Info
                )
            }
        }
    }
}

/**
 * Component for displaying a settings item with a switch
 */
@Composable
fun SettingsSwitchItem(
    title: String,
    description: String,
    icon: ImageVector,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AppColors.primaryTeal,
            modifier = Modifier.size(24.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(
                text = title,
                color = AppColors.darkGray,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = description,
                color = Color.Gray,
                fontSize = 14.sp
            )
        }

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AppColors.primaryTeal,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.LightGray
            )
        )
    }
}

/**
 * Component for displaying a clickable settings item
 */
@Composable
fun SettingsClickItem(
    title: String,
    description: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Handle click */ }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AppColors.primaryTeal,
            modifier = Modifier.size(24.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(
                text = title,
                color = AppColors.darkGray,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = description,
                color = Color.Gray,
                fontSize = 14.sp
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.Gray
        )
    }
}

