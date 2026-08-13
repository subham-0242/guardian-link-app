package com.example.ui.landing

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CrisisRed
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun RoleSelectorScreen(
    onSelectGuest: (roomId: String) -> Unit,
    onSelectStaff: () -> Unit,
    onSelectResponder: () -> Unit,
    modifier: Modifier = Modifier
) {
    var guestRoomInput by remember { mutableStateOf("402") }
    var staffPasscode by remember { mutableStateOf("") }
    var staffError by remember { mutableStateOf("") }
    var showStaffPasscodeBox by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkCanvas)
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Branding Banner
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(TacticalCyan.copy(alpha = 0.2f))
                    .border(2.dp, TacticalCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "GuardianLink Logo",
                    tint = TacticalCyan,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "GUARDIANLINK",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = TacticalCyan,
                letterSpacing = 2.sp
            )

            Text(
                text = "Decentralized Hospitality Emergency Mesh",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 1. Guest Role Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("role_card_guest")
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, TacticalCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Hotel,
                            contentDescription = null,
                            tint = TacticalCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "GUEST MODE",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Distress flagging, floor evacuation map & comms",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = guestRoomInput,
                            onValueChange = { guestRoomInput = it },
                            label = { Text("Room ID (e.g. 402)") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("guest_room_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TacticalCyan,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .testTag("simulated_qr_scan_button")
                                .clip(RoundedCornerShape(12.dp))
                                .background(TacticalCyan.copy(alpha = 0.2f))
                                .border(1.dp, TacticalCyan, RoundedCornerShape(12.dp))
                                .clickable {
                                    guestRoomInput = "402"
                                    onSelectGuest("402")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Simulate Room QR Scan",
                                tint = TacticalCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val id = if (guestRoomInput.isNotBlank()) guestRoomInput else "402"
                            onSelectGuest(id)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("launch_guest_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = TacticalCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("ENTER GUEST DASHBOARD (RM ${if (guestRoomInput.isNotBlank()) guestRoomInput else "402"})", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Staff Role Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("role_card_staff")
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, WarningAmber.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showStaffPasscodeBox = !showStaffPasscodeBox },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = WarningAmber,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "STAFF COMMAND CENTER",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Floor plan setup, plotting nodes, broadcasts",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Guarded",
                            tint = WarningAmber,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    AnimatedVisibility(visible = showStaffPasscodeBox) {
                        Column {
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedTextField(
                                value = staffPasscode,
                                onValueChange = {
                                    staffPasscode = it
                                    staffError = ""
                                },
                                label = { Text("Passcode (default: guardian-staff-demo)") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("staff_passcode_input"),
                                visualTransformation = PasswordVisualTransformation(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = WarningAmber,
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            if (staffError.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = staffError,
                                    fontSize = 12.sp,
                                    color = CrisisRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    if (staffPasscode == "guardian-staff-demo" || staffPasscode.isBlank()) {
                                        onSelectStaff()
                                    } else {
                                        staffError = "Incorrect passcode. Use: guardian-staff-demo"
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("launch_staff_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = WarningAmber, contentColor = Color.Black),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("ACCESS COMMAND CENTER", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. First Responder Role Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("role_card_responder")
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, CrisisRed.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SupportAgent,
                            contentDescription = null,
                            tint = CrisisRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "FIRST RESPONDER BRIDGE",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Building X-Ray vision, room triage & translation",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onSelectResponder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("launch_responder_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CrisisRed, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("OPEN RESPONDER TRIAGE GRID", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
