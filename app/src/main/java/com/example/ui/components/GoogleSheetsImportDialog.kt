package com.example.ui.components

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.BorderHighlight
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CardDark
import com.example.ui.theme.CardElevated
import com.example.ui.theme.MatteBlack
import com.example.ui.theme.PlatinumSteel
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.TitaniumSilver
import com.example.ui.theme.TitaniumWhite
import com.example.viewmodel.GymViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GoogleSheetsImportDialog(
  viewModel: GymViewModel,
  onDismiss: () -> Unit,
  onImportCompleted: () -> Unit = {}
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current

  val authState by viewModel.googleAuthState.collectAsState()
  val syncState by viewModel.googleSheetsUiState.collectAsState()
  val useLbs by viewModel.useLbs.collectAsState()
  val allWorkouts by viewModel.allWorkouts.collectAsState()

  var spreadsheetInput by remember(authState.linkedSpreadsheetId) {
    mutableStateOf(authState.linkedSpreadsheetId ?: "")
  }
  var showAdvancedSettings by remember { mutableStateOf(false) }
  var manualTokenInput by remember { mutableStateOf(authState.accessToken ?: "") }
  var customApiKeyInput by remember { mutableStateOf(authState.customApiKey ?: "") }
  var expandedPreviewIndex by remember { mutableStateOf<Int?>(null) }
  var pastedDataInput by remember { mutableStateOf("") }
  var isPasteModeExpanded by remember { mutableStateOf(false) }

  Dialog(
    onDismissRequest = {
      viewModel.clearGoogleSheetState()
      onDismiss()
    },
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MatteBlack,
      border = androidx.compose.foundation.BorderStroke(1.dp, BorderHighlight),
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .fillMaxHeight(0.92f)
        .testTag("google_sheets_dialog")
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        // TOP HEADER
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(CardDark)
            .padding(horizontal = 16.dp, vertical = 14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0xFF0F9D58).copy(alpha = 0.2f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                Icons.Default.TableChart,
                contentDescription = null,
                tint = Color(0xFF34A853),
                modifier = Modifier.size(22.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Google Sheets Live Sync",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TitaniumWhite
              )
              Text(
                text = "Direct Google Sign-In & Sheets REST API",
                fontSize = 11.sp,
                color = TextSecondary
              )
            }
          }

          IconButton(
            onClick = {
              viewModel.clearGoogleSheetState()
              onDismiss()
            },
            modifier = Modifier.testTag("close_google_sheets_dialog")
          ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = TitaniumSilver)
          }
        }

        HorizontalDivider(color = BorderSubtle)

        LazyColumn(
          modifier = Modifier
            .weight(1f)
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          // 1. GOOGLE ACCOUNT & OAUTH CARD
          item {
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = CardElevated,
              border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      Icons.Default.AccountCircle,
                      contentDescription = null,
                      tint = if (authState.isConnected) Color(0xFF34A853) else PlatinumSteel,
                      modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = "Google Account & OAuth",
                      fontWeight = FontWeight.Bold,
                      fontSize = 13.sp,
                      color = TitaniumWhite
                    )
                  }

                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (authState.isConnected) Color(0xFF0F9D58).copy(alpha = 0.2f) else SurfaceDark,
                    border = androidx.compose.foundation.BorderStroke(
                      1.dp,
                      if (authState.isConnected) Color(0xFF34A853) else BorderSubtle
                    )
                  ) {
                    Text(
                      text = if (authState.isConnected) "Connected" else "Ready",
                      color = if (authState.isConnected) Color(0xFF34A853) else TextSecondary,
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (authState.isConnected && authState.email != null) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = authState.displayName ?: "Google User",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TitaniumWhite
                      )
                      Text(
                        text = authState.email ?: "",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                    }

                    TextButton(
                      onClick = { viewModel.disconnectGoogleAccount() },
                      colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                    ) {
                      Text("Disconnect", fontSize = 11.sp)
                    }
                  }
                } else {
                  Text(
                    text = "Sign in to access your personal or restricted Google Sheets, or sync public spreadsheets using the Google Sheets REST API.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                  )

                  Spacer(modifier = Modifier.height(10.dp))

                  Button(
                    onClick = { viewModel.connectGoogleAccount(context) },
                    colors = ButtonDefaults.buttonColors(
                      containerColor = TitaniumWhite,
                      contentColor = MatteBlack
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("google_sign_in_button"),
                    enabled = !authState.isAuthenticating
                  ) {
                    if (authState.isAuthenticating) {
                      CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MatteBlack,
                        strokeWidth = 2.dp
                      )
                      Spacer(modifier = Modifier.width(8.dp))
                      Text("Signing In...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    } else {
                      Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(8.dp))
                      Text("Connect Google Account", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                }

                if (authState.message != null) {
                  Spacer(modifier = Modifier.height(8.dp))
                  Text(
                    text = authState.message ?: "",
                    fontSize = 11.sp,
                    color = if (authState.isConnected) Color(0xFF34A853) else TitaniumSilver
                  )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Advanced OAuth / Key toggle
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAdvancedSettings = !showAdvancedSettings }
                    .padding(vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text(
                    text = "OAuth Details & Custom API Key",
                    fontSize = 11.sp,
                    color = PlatinumSteel
                  )
                  Icon(
                    imageVector = if (showAdvancedSettings) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = TitaniumSilver,
                    modifier = Modifier.size(16.dp)
                  )
                }

                AnimatedVisibility(visible = showAdvancedSettings) {
                  Column(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    Text(
                      text = "Project ID: ${viewModel.googleAuthManager.defaultProjectId}\nOAuth Client: ${viewModel.googleAuthManager.defaultOAuthClientId.take(28)}...",
                      fontSize = 10.sp,
                      color = TextTertiary,
                      lineHeight = 14.sp
                    )

                    OutlinedTextField(
                      value = manualTokenInput,
                      onValueChange = {
                        manualTokenInput = it
                        viewModel.setManualOAuthToken(it)
                      },
                      label = { Text("Manual OAuth Bearer Token (Optional)", fontSize = 10.sp) },
                      placeholder = { Text("ya29.a0Af...", fontSize = 11.sp, color = TextTertiary) },
                      modifier = Modifier.fillMaxWidth(),
                      colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TitaniumWhite,
                        unfocusedTextColor = TitaniumSilver,
                        focusedBorderColor = Color(0xFF34A853),
                        unfocusedBorderColor = BorderSubtle
                      ),
                      textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                      singleLine = true
                    )

                    OutlinedTextField(
                      value = customApiKeyInput,
                      onValueChange = {
                        customApiKeyInput = it
                        viewModel.setCustomGoogleApiKey(it)
                      },
                      label = { Text("Custom Google Sheets API Key (Optional)", fontSize = 10.sp) },
                      placeholder = { Text("AIzaSy...", fontSize = 11.sp, color = TextTertiary) },
                      modifier = Modifier.fillMaxWidth(),
                      colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TitaniumWhite,
                        unfocusedTextColor = TitaniumSilver,
                        focusedBorderColor = Color(0xFF34A853),
                        unfocusedBorderColor = BorderSubtle
                      ),
                      textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                      singleLine = true
                    )
                  }
                }
              }
            }
          }

          // LINKED SPREADSHEET (2-WAY SYNC) CARD
          if (!authState.linkedSpreadsheetId.isNullOrBlank()) {
            item {
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = CardElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0F9D58)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(14.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(
                        Icons.Default.Sync,
                        contentDescription = null,
                        tint = Color(0xFF34A853),
                        modifier = Modifier.size(20.dp)
                      )
                      Spacer(modifier = Modifier.width(8.dp))
                      Column {
                        Text(
                          text = authState.linkedSpreadsheetTitle ?: "Linked 'Gym' Spreadsheet",
                          fontWeight = FontWeight.Bold,
                          fontSize = 14.sp,
                          color = TitaniumWhite
                        )
                        Text(
                          text = "Active Tab: ${authState.linkedTabTitle ?: "Sheet1"}",
                          fontSize = 11.sp,
                          color = Color(0xFF34A853)
                        )
                      }
                    }

                    IconButton(
                      onClick = { viewModel.clearLinkedSpreadsheet() },
                      modifier = Modifier.size(28.dp)
                    ) {
                      Icon(
                        Icons.Default.LinkOff,
                        contentDescription = "Unlink Spreadsheet",
                        tint = TitaniumSilver,
                        modifier = Modifier.size(18.dp)
                      )
                    }
                  }

                  Spacer(modifier = Modifier.height(10.dp))

                  // Auto-export toggle
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(8.dp))
                      .background(SurfaceDark)
                      .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = "Auto-Sync Logged Workouts",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TitaniumWhite
                      )
                      Text(
                        text = "Appends sets to your sheet whenever you finish a workout",
                        fontSize = 10.sp,
                        color = TextSecondary
                      )
                    }
                    Switch(
                      checked = authState.autoExportEnabled,
                      onCheckedChange = { viewModel.setAutoExportToGoogleSheets(it) },
                      colors = SwitchDefaults.colors(
                        checkedThumbColor = TitaniumWhite,
                        checkedTrackColor = Color(0xFF34A853)
                      )
                    )
                  }

                  Spacer(modifier = Modifier.height(10.dp))

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    // Pull button
                    Button(
                      onClick = {
                        viewModel.fetchGoogleSpreadsheet(
                          authState.linkedSpreadsheetId!!,
                          authState.linkedTabTitle
                        )
                      },
                      colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0F9D58),
                        contentColor = TitaniumWhite
                      ),
                      shape = RoundedCornerShape(8.dp),
                      modifier = Modifier.weight(1f),
                      enabled = !syncState.isLoading
                    ) {
                      Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(15.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text("Pull Past Data", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Put button
                    Button(
                      onClick = {
                        viewModel.exportAllWorkoutsToGoogleSheet(
                          authState.linkedSpreadsheetId,
                          authState.linkedTabTitle
                        )
                      },
                      colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4285F4),
                        contentColor = TitaniumWhite
                      ),
                      shape = RoundedCornerShape(8.dp),
                      modifier = Modifier.weight(1f),
                      enabled = !syncState.isExporting && allWorkouts.isNotEmpty()
                    ) {
                      Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(15.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text("Put (${allWorkouts.size}) to Sheet", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }
            }
          }

          // 2. SPREADSHEET INPUT CARD
          item {
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = CardElevated,
              border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    Icons.Default.Link,
                    contentDescription = null,
                    tint = Color(0xFF34A853),
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "Google Spreadsheet URL or ID",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TitaniumWhite
                  )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                  value = spreadsheetInput,
                  onValueChange = { spreadsheetInput = it },
                  label = { Text("Paste Google Sheet link or spreadsheet ID", fontSize = 11.sp) },
                  placeholder = { Text("https://docs.google.com/spreadsheets/d/...", fontSize = 11.sp, color = TextTertiary) },
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("spreadsheet_url_input"),
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TitaniumWhite,
                    unfocusedTextColor = TitaniumSilver,
                    focusedBorderColor = Color(0xFF34A853),
                    unfocusedBorderColor = BorderSubtle,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark
                  ),
                  textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                  trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      if (spreadsheetInput.isNotBlank()) {
                        IconButton(onClick = { spreadsheetInput = "" }) {
                          Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextTertiary, modifier = Modifier.size(16.dp))
                        }
                      }
                      IconButton(
                        onClick = {
                          val clip = clipboardManager.getText()?.text
                          if (!clip.isNullOrBlank()) {
                            spreadsheetInput = clip.trim()
                          }
                        },
                        modifier = Modifier.testTag("paste_spreadsheet_url_button")
                      ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = TitaniumSilver, modifier = Modifier.size(18.dp))
                      }
                    }
                  }
                )

                val isLikelyNameOnly = remember(spreadsheetInput) {
                  val clean = spreadsheetInput.trim()
                  clean.isNotBlank() && !clean.contains("/") && !clean.contains(".") && (clean.length < 18 || clean.contains(" "))
                }

                if (isLikelyNameOnly) {
                  Spacer(modifier = Modifier.height(8.dp))
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(8.dp))
                      .background(Color(0xFFF59E0B).copy(alpha = 0.15f))
                      .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = "\"$spreadsheetInput\" looks like a sheet name. Please copy the full link from Google Sheets (e.g. https://docs.google.com/spreadsheets/d/...) or use Option 2 below to paste your cells directly.",
                      fontSize = 11.sp,
                      color = Color(0xFFFDE68A),
                      lineHeight = 15.sp
                    )
                  }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                  text = "💡 Sharing tip: In Google Sheets, tap Share → General access → 'Anyone with the link can view' for instant sync.",
                  fontSize = 10.sp,
                  color = TextSecondary,
                  lineHeight = 14.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Button(
                    onClick = {
                      viewModel.fetchGoogleSpreadsheet(spreadsheetInput)
                    },
                    colors = ButtonDefaults.buttonColors(
                      containerColor = Color(0xFF0F9D58),
                      contentColor = TitaniumWhite
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                      .weight(1f)
                      .testTag("fetch_sheet_button"),
                    enabled = spreadsheetInput.isNotBlank() && !syncState.isLoading
                  ) {
                    if (syncState.isLoading) {
                      CircularProgressIndicator(modifier = Modifier.size(16.dp), color = TitaniumWhite, strokeWidth = 2.dp)
                      Spacer(modifier = Modifier.width(8.dp))
                      Text("Fetching...", fontSize = 12.sp)
                    } else {
                      Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(8.dp))
                      Text("Fetch Sheet Live", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                  }
                }
              }
            }
          }

          // 2B. OPTION 2: DIRECT PASTE (NO LINK OR SHARING NEEDED)
          item {
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = CardElevated,
              border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isPasteModeExpanded = !isPasteModeExpanded },
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      Icons.Default.ContentPaste,
                      contentDescription = null,
                      tint = Color(0xFF4285F4),
                      modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = "Option 2: Direct Paste (No Link Needed)",
                      fontWeight = FontWeight.Bold,
                      fontSize = 13.sp,
                      color = TitaniumWhite
                    )
                  }
                  Icon(
                    if (isPasteModeExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = TitaniumSilver,
                    modifier = Modifier.size(20.dp)
                  )
                }

                if (isPasteModeExpanded) {
                  Spacer(modifier = Modifier.height(10.dp))
                  Text(
                    text = "If your 'Gym' sheet is private or you prefer not to change sharing settings, simply copy your rows/columns in Google Sheets or Excel and paste them here:",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 15.sp
                  )
                  Spacer(modifier = Modifier.height(8.dp))
                  OutlinedTextField(
                    value = pastedDataInput,
                    onValueChange = { pastedDataInput = it },
                    placeholder = {
                      Text(
                        "Date\tExercise\tWeight\tReps\n2026-03-01\tBench Press\t100\t8",
                        fontSize = 11.sp,
                        color = TextTertiary
                      )
                    },
                    modifier = Modifier
                      .fillMaxWidth()
                      .heightIn(min = 80.dp, max = 150.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                      focusedTextColor = TitaniumWhite,
                      unfocusedTextColor = TitaniumSilver,
                      focusedBorderColor = Color(0xFF4285F4),
                      unfocusedBorderColor = BorderSubtle,
                      focusedContainerColor = SurfaceDark,
                      unfocusedContainerColor = SurfaceDark
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                  )
                  Spacer(modifier = Modifier.height(10.dp))
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    OutlinedButton(
                      onClick = {
                        val clip = clipboardManager.getText()?.text
                        if (!clip.isNullOrBlank()) {
                          pastedDataInput = clip
                        }
                      },
                      shape = RoundedCornerShape(8.dp),
                      modifier = Modifier.weight(1f)
                    ) {
                      Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("Paste Clipboard", fontSize = 11.sp)
                    }

                    Button(
                      onClick = {
                        viewModel.parseAndPreviewPastedSheetData(pastedDataInput)
                      },
                      enabled = pastedDataInput.isNotBlank(),
                      colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4285F4),
                        contentColor = TitaniumWhite
                      ),
                      shape = RoundedCornerShape(8.dp),
                      modifier = Modifier.weight(1f)
                    ) {
                      Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("Parse & Preview", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }
            }
          }

          // STATUS OR ERROR NOTICE
          if (syncState.statusMessage != null || syncState.errorMessage != null) {
            item {
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (syncState.errorMessage != null) Color(0xFF7F1D1D).copy(alpha = 0.3f) else Color(0xFF0F9D58).copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(
                  1.dp,
                  if (syncState.errorMessage != null) Color(0xFFEF4444) else Color(0xFF34A853)
                ),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = if (syncState.errorMessage != null) Icons.Default.Close else Icons.Default.CheckCircle,
                      contentDescription = null,
                      tint = if (syncState.errorMessage != null) Color(0xFFEF4444) else Color(0xFF34A853),
                      modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                      text = syncState.errorMessage ?: syncState.statusMessage ?: "",
                      fontSize = 11.sp,
                      color = if (syncState.errorMessage != null) Color(0xFFFCA5A5) else TitaniumWhite,
                      lineHeight = 15.sp,
                      fontWeight = if (syncState.errorMessage != null) FontWeight.SemiBold else FontWeight.Normal
                    )
                  }

                  if (syncState.errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.End
                    ) {
                      TextButton(
                        onClick = { isPasteModeExpanded = true }
                      ) {
                        Text("📋 Try Direct Paste Instead", fontSize = 11.sp, color = Color(0xFF60A5FA))
                      }
                    }
                  }
                }
              }
            }
          }

          // 3. SHEET METADATA & TAB SELECTOR
          if (syncState.metadata != null) {
            item {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = syncState.metadata!!.title,
                      fontWeight = FontWeight.Bold,
                      fontSize = 13.sp,
                      color = TitaniumWhite
                    )
                    Text(
                      text = "${syncState.metadata!!.sheetTabs.size} tab(s) • Tab: ${syncState.selectedTabTitle ?: "Sheet1"}",
                      fontSize = 11.sp,
                      color = TextSecondary
                    )
                  }

                  if (authState.linkedSpreadsheetId != syncState.metadata!!.spreadsheetId) {
                    Button(
                      onClick = {
                        viewModel.saveLinkedSpreadsheet(
                          syncState.metadata!!.spreadsheetId,
                          syncState.metadata!!.title,
                          syncState.selectedTabTitle ?: "Sheet1"
                        )
                        viewModel.setAutoExportToGoogleSheets(true)
                      },
                      colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0F9D58),
                        contentColor = TitaniumWhite
                      ),
                      shape = RoundedCornerShape(8.dp)
                    ) {
                      Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text("Link for 2-Way Sync", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }
            }
          }

          if (syncState.metadata != null && syncState.metadata!!.sheetTabs.size > 1) {
            item {
              Column {
                Text(
                  text = "SELECT TAB / WORKSHEET",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = TitaniumSilver
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  for (tab in syncState.metadata!!.sheetTabs) {
                    val isSelected = tab.title == syncState.selectedTabTitle
                    Surface(
                      shape = RoundedCornerShape(8.dp),
                      color = if (isSelected) Color(0xFF0F9D58).copy(alpha = 0.25f) else CardElevated,
                      border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) Color(0xFF34A853) else BorderSubtle
                      ),
                      modifier = Modifier.clickable {
                        viewModel.fetchGoogleSpreadsheet(spreadsheetInput, tab.title)
                      }
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Text(
                          text = tab.title,
                          fontSize = 11.sp,
                          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                          color = if (isSelected) Color(0xFF34A853) else TitaniumWhite
                        )
                        if (tab.rowCount > 0) {
                          Spacer(modifier = Modifier.width(4.dp))
                          Text("(${tab.rowCount})", fontSize = 9.sp, color = TextTertiary)
                        }
                      }
                    }
                  }
                }
              }
            }
          }

          // 4. PUT DATA TO GOOGLE SHEETS (EXPORT) CARD
          item {
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = CardElevated,
              border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      Icons.Default.CloudUpload,
                      contentDescription = null,
                      tint = Color(0xFF4285F4),
                      modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = "Put Data to Google Sheets (Export)",
                      fontWeight = FontWeight.Bold,
                      fontSize = 13.sp,
                      color = TitaniumWhite
                    )
                  }
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SurfaceDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                  ) {
                    Text(
                      text = "${allWorkouts.size} logged in App",
                      color = TitaniumSilver,
                      fontSize = 10.sp,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = "Push your app workout logs directly into your Google Spreadsheet. It appends Date, Split, Exercise, Weight, Reps, and Notes in clean tabular format.",
                  fontSize = 11.sp,
                  color = TextSecondary,
                  lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceDark)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "Auto-Sync Future Workouts",
                      fontSize = 12.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = TitaniumWhite
                    )
                    Text(
                      text = "Automatically append sessions to sheet as you log them",
                      fontSize = 10.sp,
                      color = TextTertiary
                    )
                  }
                  Switch(
                    checked = authState.autoExportEnabled,
                    onCheckedChange = { viewModel.setAutoExportToGoogleSheets(it) },
                    colors = SwitchDefaults.colors(
                      checkedThumbColor = TitaniumWhite,
                      checkedTrackColor = Color(0xFF34A853)
                    )
                  )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                  onClick = {
                    val targetId = spreadsheetInput.ifBlank { authState.linkedSpreadsheetId }
                    val targetTab = syncState.selectedTabTitle ?: authState.linkedTabTitle ?: "Sheet1"
                    viewModel.exportAllWorkoutsToGoogleSheet(targetId, targetTab)
                  },
                  colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4285F4),
                    contentColor = TitaniumWhite
                  ),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.fillMaxWidth().testTag("export_workouts_to_sheets_button"),
                  enabled = !syncState.isExporting && allWorkouts.isNotEmpty() && (spreadsheetInput.isNotBlank() || !authState.linkedSpreadsheetId.isNullOrBlank())
                ) {
                  if (syncState.isExporting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = TitaniumWhite, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Exporting to Sheet...", fontSize = 12.sp)
                  } else {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export All (${allWorkouts.size}) to Sheet", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                  }
                }
              }
            }
          }

          // EXPORT SUCCESS CELEBRATION
          if (syncState.exportSuccessCount != null) {
            item {
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF1E3A8A).copy(alpha = 0.4f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B82F6)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(
                  modifier = Modifier.padding(16.dp),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF60A5FA),
                    modifier = Modifier.size(36.dp)
                  )
                  Spacer(modifier = Modifier.height(8.dp))
                  Text(
                    text = "Export Successful!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TitaniumWhite
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = "${syncState.exportSuccessCount} rows were appended to your Google Spreadsheet.",
                    fontSize = 11.sp,
                    color = Color(0xFF93C5FD),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                  )
                }
              }
            }
          }

          // 4. IMPORT SUCCESS CELEBRATION
          if (syncState.importSuccessCount != null) {
            item {
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF064E3B).copy(alpha = 0.4f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(
                  modifier = Modifier.padding(16.dp),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(36.dp)
                  )
                  Spacer(modifier = Modifier.height(8.dp))
                  Text(
                    text = "Import Successful!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TitaniumWhite
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = "${syncState.importSuccessCount} workout sessions have been logged to your history and PR vault.",
                    fontSize = 11.sp,
                    color = Color(0xFF6EE7B7),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                  )
                  Spacer(modifier = Modifier.height(12.dp))
                  Button(
                    onClick = {
                      viewModel.clearGoogleSheetState()
                      onImportCompleted()
                      onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                      containerColor = Color(0xFF10B981),
                      contentColor = MatteBlack
                    ),
                    shape = RoundedCornerShape(8.dp)
                  ) {
                    Text("View in Workouts", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                  }
                }
              }
            }
          }

          // 5. PARSED WORKOUTS PREVIEW
          if (syncState.parsedWorkouts.isNotEmpty()) {
            item {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "PREVIEW (${syncState.parsedWorkouts.size} SESSIONS)",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = TitaniumSilver
                )
                Text(
                  text = "${syncState.parsedWorkouts.sumOf { it.exercises.size }} Exercises Found",
                  fontSize = 10.sp,
                  color = Color(0xFF34A853)
                )
              }
            }

            items(syncState.parsedWorkouts.withIndex().toList()) { (index, workout) ->
              val isExpanded = expandedPreviewIndex == index
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = CardElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    expandedPreviewIndex = if (isExpanded) null else index
                  }
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = workout.workoutTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TitaniumWhite
                      )
                      Text(
                        text = "${workout.dateDisplay} • ${workout.exercises.size} exercises",
                        fontSize = 11.sp,
                        color = TextSecondary
                      )
                    }

                    Icon(
                      imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                      contentDescription = null,
                      tint = TitaniumSilver,
                      modifier = Modifier.size(18.dp)
                    )
                  }

                  if (isExpanded) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = BorderSubtle)
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                      for (ex in workout.exercises) {
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                          ) {
                            Icon(
                              Icons.Default.FitnessCenter,
                              contentDescription = null,
                              tint = TitaniumSilver,
                              modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                              text = ex.exerciseName,
                              fontSize = 12.sp,
                              color = TitaniumWhite,
                              maxLines = 1,
                              overflow = TextOverflow.Ellipsis
                            )
                          }

                          val setsSummary = ex.sets.take(4).joinToString(", ") { set ->
                            val displayWeight = if (useLbs) {
                              "${(set.weightKg * 2.20462).toInt()} lbs"
                            } else {
                              "${set.weightKg.toInt()} kg"
                            }
                            "$displayWeight × ${set.reps.toInt()}"
                          }
                          Text(
                            text = "${ex.sets.size} sets ($setsSummary)",
                            fontSize = 10.sp,
                            color = TextSecondary
                          )
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }

        // BOTTOM ACTION BAR
        if (syncState.parsedWorkouts.isNotEmpty()) {
          HorizontalDivider(color = BorderSubtle)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(CardDark)
              .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "${syncState.parsedWorkouts.size} Workouts Ready",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = TitaniumWhite
              )
              Text(
                text = "Will be saved to local Room database",
                fontSize = 10.sp,
                color = TextSecondary
              )
            }

            Button(
              onClick = {
                viewModel.importFetchedGoogleSheetWorkouts()
              },
              colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0F9D58),
                contentColor = TitaniumWhite
              ),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.testTag("confirm_import_google_sheet_button"),
              enabled = !syncState.isLoading
            ) {
              if (syncState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = TitaniumWhite, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Importing...", fontSize = 12.sp)
              } else {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Import All to History", fontWeight = FontWeight.Bold, fontSize = 12.sp)
              }
            }
          }
        }
      }
    }
  }
}
