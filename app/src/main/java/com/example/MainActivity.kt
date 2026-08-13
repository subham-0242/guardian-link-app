package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.guest.GuestDashboardScreen
import com.example.ui.landing.RoleSelectorScreen
import com.example.ui.responder.ResponderTriageScreen
import com.example.ui.staff.StaffCommandScreen
import com.example.ui.theme.GuardianLinkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GuardianLinkTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    GuardianLinkApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun GuardianLinkApp(modifier: Modifier = Modifier) {
    var currentScreen by remember { mutableStateOf("role_selector") }
    var guestRoomId by remember { mutableStateOf("402") }

    when (currentScreen) {
        "role_selector" -> {
            RoleSelectorScreen(
                onSelectGuest = { roomId ->
                    guestRoomId = if (roomId.isNotBlank()) roomId else "402"
                    currentScreen = "guest_dashboard"
                },
                onSelectStaff = {
                    currentScreen = "staff_command"
                },
                onSelectResponder = {
                    currentScreen = "responder_triage"
                },
                modifier = modifier
            )
        }
        "guest_dashboard" -> {
            GuestDashboardScreen(
                roomId = guestRoomId,
                onBackToHome = {
                    currentScreen = "role_selector"
                },
                modifier = modifier
            )
        }
        "staff_command" -> {
            StaffCommandScreen(
                onBackToHome = {
                    currentScreen = "role_selector"
                },
                modifier = modifier
            )
        }
        "responder_triage" -> {
            ResponderTriageScreen(
                onBackToHome = {
                    currentScreen = "role_selector"
                },
                modifier = modifier
            )
        }
    }
}
