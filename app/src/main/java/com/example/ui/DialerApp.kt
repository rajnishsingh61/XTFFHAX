package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.CallLog
import com.example.data.Contact
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialerApp(viewModel: DialerViewModel) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf("recents") } // "favorites", "recents", "contacts"

    // Core States
    val contacts by viewModel.contacts.collectAsState()
    val favorites by viewModel.favoriteContacts.collectAsState()
    val callLogs by viewModel.callLogs.collectAsState()
    val searchVal by viewModel.searchQuery.collectAsState()
    val dialDigits by viewModel.dialpadDigits.collectAsState()
    val isDialpadVis by viewModel.isDialpadVisible.collectAsState()

    // Dialog / Secondary States in Views
    var showAddContactDialog by remember { mutableStateOf(false) }
    var selectedContactForDetails by remember { mutableStateOf<Contact?>(null) }
    var showEditContactDialog by remember { mutableStateOf<Contact?>(null) }

    // Active Calling States
    val isCallActive by viewModel.isCallActive.collectAsState()
    val activeCallName by viewModel.activeCallName.collectAsState()
    val activeCallNumber by viewModel.activeCallNumber.collectAsState()
    val activeCallTimer by viewModel.activeCallTimer.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val isSpeakerOn by viewModel.isSpeakerOn.collectAsState()
    val activeVoiceEffect by viewModel.activeVoiceEffect.collectAsState()
    val isIncomingCall by viewModel.isIncomingCall.collectAsState()
    val inCallKeypadOpen by viewModel.inCallKeypadOpen.collectAsState()
    val inCallDigits by viewModel.inCallDialpadDigits.collectAsState()

    Scaffold(
        topBar = {
            if (!isCallActive) {
                // Outer Search Bar styled like modern Google Phone App
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("search_card"),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Contacts",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        TextField(
                            value = searchVal,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            placeholder = { Text("Search contacts...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("contact_search_input"),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true
                        )
                        if (searchVal.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.updateSearchQuery("") },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear Search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            // Demo Simulator Button in search bar to test voice changer
                            Button(
                                onClick = {
                                    viewModel.simulateIncomingCall(
                                        number = "+1 (555) 012-7491",
                                        contactName = "Sophia Martinez"
                                    )
                                    Toast.makeText(context, "Incoming Call Simulated!", Toast.LENGTH_SHORT).show()
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .testTag("simulate_call_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "Test Voice",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Test Voice", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (!isCallActive) {
                NavigationBar(
                    modifier = Modifier.navigationBarsPadding(),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab == "favorites",
                        onClick = { currentTab = "favorites" },
                        icon = { Icon(Icons.Default.Star, contentDescription = "Favorites") },
                        label = { Text("Favorites") },
                        modifier = Modifier.testTag("tab_favorites")
                    )
                    NavigationBarItem(
                        selected = currentTab == "recents",
                        onClick = { currentTab = "recents" },
                        icon = { Icon(Icons.Default.Refresh, contentDescription = "Recents") },
                        label = { Text("Recents") },
                        modifier = Modifier.testTag("tab_recents")
                    )
                    NavigationBarItem(
                        selected = currentTab == "contacts",
                        onClick = { currentTab = "contacts" },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Contacts") },
                        label = { Text("Contacts") },
                        modifier = Modifier.testTag("tab_contacts")
                    )
                }
            }
        },
        floatingActionButton = {
            if (!isCallActive) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Floating dial pad toggle
                    if (!isDialpadVis) {
                        FloatingActionButton(
                            onClick = { viewModel.setDialpadVisible(true) },
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            shape = FloatingActionButtonDefaults.largeShape,
                            modifier = Modifier
                                .size(56.dp)
                                .testTag("open_dialpad_fab")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Show Dialpad",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main tabs Switcher
            when (currentTab) {
                "favorites" -> FavoritesTab(
                    favorites = favorites,
                    onContactClick = { selectedContactForDetails = it },
                    onCallClick = { viewModel.initiateCall(it.phoneNumber, it.name) }
                )
                "recents" -> RecentsTab(
                    callLogs = callLogs,
                    onCallClick = { viewModel.initiateCall(it.phoneNumber, it.callerName) },
                    onDeleteClick = { viewModel.deleteCallLog(it) },
                    onClearAll = { viewModel.clearCallLogs() }
                )
                "contacts" -> ContactsTab(
                    contacts = contacts,
                    onAddContactClick = { showAddContactDialog = true },
                    onContactClick = { selectedContactForDetails = it },
                    onCallClick = { viewModel.initiateCall(it.phoneNumber, it.name) }
                )
            }

            // Google sliding Dial Pad overlay
            AnimatedVisibility(
                visible = isDialpadVis,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                ) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                DialpadLayout(
                    digits = dialDigits,
                    onDigitClick = { viewModel.appendDialpadDigit(it) },
                    onDeleteClick = { viewModel.deleteDialpadDigit() },
                    onCloseClick = { viewModel.setDialpadVisible(false) },
                    onCallClick = {
                        viewModel.initiateCall(dialDigits)
                        viewModel.clearDialpadDigits()
                    },
                    onAddContactClick = {
                        showAddContactDialog = true
                    }
                )
            }

            // Dialogue: Detail popup
            selectedContactForDetails?.let { contact ->
                ContactDetailDialog(
                    contact = contact,
                    onDismiss = { selectedContactForDetails = null },
                    onCall = {
                        viewModel.initiateCall(contact.phoneNumber, contact.name)
                        selectedContactForDetails = null
                    },
                    onToggleFavorite = { viewModel.toggleContactFavorite(contact) },
                    onDelete = {
                        viewModel.deleteContact(contact)
                        selectedContactForDetails = null
                    },
                    onEdit = {
                        showEditContactDialog = contact
                        selectedContactForDetails = null
                    }
                )
            }

            // Dialogue: Add contact
            if (showAddContactDialog) {
                AddContactDialog(
                    initialNumber = dialDigits,
                    onDismiss = { showAddContactDialog = false },
                    onSave = { name, phone, email, isFav ->
                        viewModel.addContact(name, phone, email, isFav)
                        showAddContactDialog = false
                    }
                )
            }

            // Dialogue: Edit contact
            showEditContactDialog?.let { contact ->
                EditContactDialog(
                    contact = contact,
                    onDismiss = { showEditContactDialog = null },
                    onSave = { name, phone, email ->
                        viewModel.updateContactDetails(contact, name, phone, email)
                        showEditContactDialog = null
                    }
                )
            }

            // Calling Screen full Overlay
            AnimatedVisibility(
                visible = isCallActive,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("call_screen_overlay")
            ) {
                ActiveCallScreen(
                    name = activeCallName,
                    number = activeCallNumber,
                    timerSeconds = activeCallTimer,
                    isMuted = isMuted,
                    isSpeakerOn = isSpeakerOn,
                    activeEffect = activeVoiceEffect,
                    isIncoming = isIncomingCall,
                    inCallKeypadOpen = inCallKeypadOpen,
                    inCallDigits = inCallDigits,
                    effectsList = viewModel.voiceEffects,
                    onAccept = { viewModel.acceptIncomingCall() },
                    onDecline = { viewModel.endActiveCall() },
                    onEndCall = { viewModel.endActiveCall() },
                    onMuteToggle = { viewModel.toggleMute() },
                    onSpeakerToggle = { viewModel.toggleSpeaker() },
                    onKeypadToggle = { viewModel.toggleInCallKeypad() },
                    onKeypadDigit = { viewModel.appendInCallKeypadDigit(it) },
                    onEffectChange = { effect ->
                        viewModel.changeVoiceEffect(effect)
                        Toast.makeText(context, "VoxMic modified: $effect processing active", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

// ==========================================
// TABS IMPLEMENTATION
// ==========================================

@Composable
fun FavoritesTab(
    favorites: List<Contact>,
    onContactClick: (Contact) -> Unit,
    onCallClick: (Contact) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("favorites_tab_content")
    ) {
        Text(
            text = "FAVORITES",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(bottom = 16.dp, start = 4.dp)
        )

        if (favorites.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "No Favorites",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No favorites yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Star contacts to see them listed here",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(favorites) { contact ->
                    FavoriteCard(
                        contact = contact,
                        onClick = { onContactClick(contact) },
                        onCallClick = { onCallClick(contact) }
                    )
                }
            }
        }
    }
}

@Composable
fun FavoriteCard(
    contact: Contact,
    onClick: () -> Unit,
    onCallClick: () -> Unit
) {
    val avatarColor = remember(contact.avatarColorHex) {
        try {
            Color(android.graphics.Color.parseColor(contact.avatarColorHex))
        } catch (e: Exception) {
            Color(0xFF2563EB)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("favorite_card_${contact.id}"),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(avatarColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.name.take(1).uppercase(),
                    color = avatarColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = contact.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = contact.phoneNumber,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            IconButton(
                onClick = onCallClick,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "Call ${contact.name}",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun RecentsTab(
    callLogs: List<CallLog>,
    onCallClick: (CallLog) -> Unit,
    onDeleteClick: (CallLog) -> Unit,
    onClearAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("recents_tab_content")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RECENT CALLS",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
            if (callLogs.isNotEmpty()) {
                TextButton(
                    onClick = onClearAll,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear all logs", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear All", fontSize = 12.sp)
                }
            }
        }

        if (callLogs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "No Recents",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Nice and quiet call history",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Your dialed, missed, and received calls appear here",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(callLogs) { log ->
                    CallLogItem(
                        log = log,
                        onCallClick = { onCallClick(log) },
                        onDeleteClick = { onDeleteClick(log) }
                    )
                }
            }
        }
    }
}

@Composable
fun CallLogItem(
    log: CallLog,
    onCallClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val dateText = remember(log.timestamp) {
        val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        sdf.format(Date(log.timestamp))
    }

    val durationText = remember(log.durationSeconds) {
        if (log.durationSeconds == 0) ""
        else {
            val min = log.durationSeconds / 60
            val sec = log.durationSeconds % 60
            if (min > 0) "${min}m ${sec}s" else "${sec}s"
        }
    }

    // Colors can represent call logs elegantly
    val typeColor = when (log.callType) {
        "OUTGOING" -> MaterialTheme.colorScheme.primary
        "INCOMING" -> Color(0xFF10B981) // Clean Emerald green
        else -> MaterialTheme.colorScheme.error // Missed call -> Red
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("call_log_item_${log.id}"),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(typeColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                // Design-centric rotation matching call status direction
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = log.callType,
                    tint = typeColor,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer(
                           rotationZ = when (log.callType) {
                               "OUTGOING" -> 45f
                               "INCOMING" -> 225f
                               else -> 135f
                           }
                        )
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = log.callerName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = log.phoneNumber,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    if (durationText.isNotEmpty()) {
                        Text(
                            text = " • $durationText",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
                Text(
                    text = dateText,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.padding(top = 2.dp)
                )
                // Filter badge
                if (log.voiceEffectUsed != "Normal" && log.voiceEffectUsed != "None") {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(vertical = 3.dp, horizontal = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Vox: ${log.voiceEffectUsed}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            Row {
                IconButton(onClick = onCallClick) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Simulate call logging item",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Delete call log item",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun ContactsTab(
    contacts: List<Contact>,
    onAddContactClick: () -> Unit,
    onContactClick: (Contact) -> Unit,
    onCallClick: (Contact) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("contacts_tab_content")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MY CONTACTS",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
            Button(
                onClick = onAddContactClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("add_contact_top_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Contact", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Contact", fontSize = 12.sp)
            }
        }

        if (contacts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "No contacts",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Start adding friends!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            // Sort contacts alphabetically and group
            val groupedContacts = contacts.groupBy { it.name.trim().take(1).uppercase() }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                groupedContacts.entries.sortedBy { it.key }.forEach { (initial, contactList) ->
                    item {
                        Text(
                            text = initial,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp)
                        )
                    }
                    items(contactList) { contact ->
                        ContactItemRow(
                            contact = contact,
                            onClick = { onContactClick(contact) },
                            onCallClick = { onCallClick(contact) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ContactItemRow(
    contact: Contact,
    onClick: () -> Unit,
    onCallClick: () -> Unit
) {
    val avatarColor = remember(contact.avatarColorHex) {
        try {
            Color(android.graphics.Color.parseColor(contact.avatarColorHex))
        } catch (e: Exception) {
            Color(0xFF2563EB)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .testTag("contact_row_${contact.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(avatarColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = contact.name.trim().take(1).uppercase(),
                color = avatarColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = contact.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = contact.phoneNumber,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
        if (contact.isFavorite) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Starred",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(20.dp)
                    .padding(end = 4.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = onCallClick,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                contentColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Default.Phone, contentDescription = "Call ${contact.name}", modifier = Modifier.size(16.dp))
        }
    }
}

// ==========================================
// DIAL PAD LAYOUT (Classic sliding up)
// ==========================================

@Composable
fun DialpadLayout(
    digits: String,
    onDigitClick: (Char) -> Unit,
    onDeleteClick: () -> Unit,
    onCloseClick: () -> Unit,
    onCallClick: () -> Unit,
    onAddContactClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .border(
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            )
            .testTag("dialpad_panel"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Slider handle bar
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                    .clickable { onCloseClick() }
            )

            // Digits Display Panel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(64.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = digits,
                    fontSize = if (digits.length > 12) 24.sp else 32.sp,
                    fontWeight = FontWeight.Light,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dialed_digits_text")
                )
                if (digits.isNotEmpty()) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("backspace_char_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Backspace",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Quick Add Contact indicator if digits entered
            AnimatedVisibility(
                visible = digits.trim().isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                TextButton(
                    onClick = onAddContactClick,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .testTag("quick_save_contact")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Save Contact",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Create Contact / Add to List", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Dialpad Key grid (12 standard button set using clean built-in Kotlin structure)
            val buttons = listOf(
                '1' to "",
                '2' to "A B C",
                '3' to "D E F",
                '4' to "G H I",
                '5' to "J K L",
                '6' to "M N O",
                '7' to "P Q R S",
                '8' to "T U V",
                '9' to "W X Y Z",
                '*' to "",
                '0' to "+",
                '#' to ""
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (row in 0..3) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(28.dp)
                        ) {
                            for (col in 0..2) {
                                val button = buttons[row * 3 + col]
                                KeypadButton(
                                    key = button.first,
                                    subtitle = button.second,
                                    onClick = { onDigitClick(button.first) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Row: Close button on left, Call green in Center
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onCloseClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Collapse Pad",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                // Green dial button
                Button(
                    onClick = onCallClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary, // Beautiful Green
                        contentColor = Color.White
                    ),
                    shape = CircleShape,
                    modifier = Modifier
                        .size(64.dp)
                        .testTag("dialpad_call_btn"),
                    contentPadding = PaddingValues(0.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Trigger outgoing call simulation",
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Empty spacing block placeholder or secondary visual haptic indicator
                Spacer(modifier = Modifier.size(48.dp))
            }
        }
    }
}

@Composable
fun KeypadButton(
    key: Char,
    subtitle: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .clickable { onClick() }
            .testTag("keypad_btn_$key"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = key.toString(),
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}


// ==========================================
// ACTIVE CALLED SYSTEM OVERLAY
// ==========================================

@Composable
fun ActiveCallScreen(
    name: String,
    number: String,
    timerSeconds: Int,
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    activeEffect: String,
    isIncoming: Boolean,
    inCallKeypadOpen: Boolean,
    inCallDigits: String,
    effectsList: List<VoiceEffect>,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onEndCall: () -> Unit,
    onMuteToggle: () -> Unit,
    onSpeakerToggle: () -> Unit,
    onKeypadToggle: () -> Unit,
    onKeypadDigit: (Char) -> Unit,
    onEffectChange: (String) -> Unit
) {
    val durationText = remember(timerSeconds) {
        val min = timerSeconds / 60
        val sec = timerSeconds % 60
        String.format(Locale.getDefault(), "%02d:%02d", min, sec)
    }

    // Call Screen uses standard theme background to elegantly support both pristine light mode and sleek dark mode
    Surface(
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Subtitle status display (Clean uppercase minimalist tracking)
            Text(
                text = if (isIncoming) "INCOMING CALL" else "CALL IN PROGRESS",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Contact Avatar Holder (Beautiful light pastel theme alignment)
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                // Wave pulsing background outline if connected
                if (timerSeconds > 0 && !isIncoming) {
                    val infiniteTransition = rememberInfiniteTransition()
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 1.0f,
                        targetValue = 1.3f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        )
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(
                                border = BorderStroke(
                                    width = 1.5.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 1f - (pulseScale - 1f) * 3f)
                                ),
                                shape = CircleShape
                            )
                    )
                }

                // Initial Letter
                Text(
                    text = name.trim().take(1).uppercase(),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Text Info Panel (Clean Typography with Medium Weight)
            Text(
                text = name,
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.5).sp,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Text(
                text = number,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            // Dynamic Status Label with LED light dot
            Row(
                modifier = Modifier.padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (timerSeconds > 0 && !isIncoming) {
                    val infiniteTransition = rememberInfiniteTransition()
                    val dotAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        )
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = dotAlpha))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }

                Text(
                    text = if (isIncoming) {
                        "Voice Changer Active"
                    } else if (timerSeconds == 0) {
                        "Connecting..."
                    } else {
                        "$durationText  •  Voice Changer Active"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }

            // Animated Telemetry Audio waveform canvas
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (timerSeconds > 0 && !isIncoming) {
                    VoxVoiceWaveform(effect = activeEffect)
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action row buttons (In-Call Utilities Grid)
            if (!isIncoming) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // MUTE ACTION BUTTON
                        InCallButton(
                            enabled = isMuted,
                            icon = Icons.Default.Phone, // Handled internally
                            label = if (isMuted) "Muted" else "Mute",
                            onClick = onMuteToggle,
                            testTag = "incall_mute_btn"
                        )

                        // VOICE CHANGING KEYPAD
                        InCallButton(
                            enabled = inCallKeypadOpen,
                            icon = Icons.Default.List,
                            label = "Keypad",
                            onClick = onKeypadToggle,
                            testTag = "incall_keypad_btn"
                        )

                        // SPEAKER CONTROL
                        InCallButton(
                            enabled = isSpeakerOn,
                            icon = Icons.Default.Notifications,
                            label = if (isSpeakerOn) "Speaker On" else "Speaker",
                            onClick = onSpeakerToggle,
                            testTag = "incall_speaker_btn"
                        )
                    }

                    // In-Call keypad digits show
                    AnimatedVisibility(visible = inCallKeypadOpen) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f), RoundedCornerShape(16.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (inCallDigits.isEmpty()) "Touch keys to dial" else inCallDigits,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            val keypadKeys = listOf('1','2','3','4','5','6','7','8','9','*','0','#')
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                keypadKeys.take(4).forEach { key ->
                                    Button(
                                        onClick = { onKeypadDigit(key) },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)),
                                        shape = CircleShape,
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Text(key.toString(), color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // VOICE EFFECTS SELECTOR PANEL
                Text(
                    text = "VOICE FILTER EFFECTS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(effectsList) { item ->
                        val isSelected = item.name == activeEffect
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEffectChange(item.name) }
                                .testTag("incall_effect_${item.name.replace(" ", "_").lowercase()}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                            ),
                            border = if (!isSelected) BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            ) else null,
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                val vectorIcon = when (item.iconName) {
                                    "mic" -> Icons.Default.Phone
                                    "volume_up" -> Icons.Default.Refresh
                                    "android" -> Icons.Default.Build
                                    "face" -> Icons.Default.Face
                                    "speed" -> Icons.Default.Build
                                    else -> Icons.Default.Info
                                }

                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) Color.White.copy(alpha = 0.18f) 
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = vectorIcon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = item.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = item.description.take(18) + if (item.description.length > 18) ".." else "",
                                    fontSize = 8.sp,
                                    color = if (isSelected) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            } else {
                // Incoming ring action buttons
                Spacer(modifier = Modifier.weight(1f))
            }

            // Bottom controls: Accept/Hang up ring actions
            if (isIncoming) {
                // Dual Action Accept vs red Decline center-horizontal layout
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp, vertical = 32.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Button(
                            onClick = onDecline,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = CircleShape,
                            modifier = Modifier
                                .size(64.dp)
                                .testTag("decline_call_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone, 
                                contentDescription = "Decline Call", 
                                modifier = Modifier
                                    .size(28.dp)
                                    .graphicsLayer(rotationZ = 135f), 
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Decline", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Button(
                            onClick = onAccept,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = CircleShape,
                            modifier = Modifier
                                .size(64.dp)
                                .testTag("accept_call_btn")
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = "Accept Call", modifier = Modifier.size(28.dp), tint = Color.White)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Answer", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Active Call simply holds red CallEnd (represented by Phone rotated by 135 degrees) in bottom row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp, top = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onEndCall,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(72.dp)
                            .testTag("end_call_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Hang Up",
                            modifier = Modifier
                                .size(32.dp)
                                .graphicsLayer(rotationZ = 135f),
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InCallButton(
    enabled: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    if (enabled) MaterialTheme.colorScheme.primary 
                    else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f)
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            fontWeight = FontWeight.Medium
        )
    }
}


// ==========================================
// VOICE EFFECTS WAVEFORM CANVAS GENERATOR
// ==========================================

@Composable
fun VoxVoiceWaveform(effect: String) {
    val infiniteTransition = rememberInfiniteTransition()
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // Select styling color matching each effect vibe
    val waveColor = when (effect) {
        "Normal" -> Color(0xFF3B82F6) // Electric blue
        "Deep Voice" -> Color(0xFFEF4444) // Deep crimson red
        "Robot" -> Color(0xFF10B981) // Robot green
        "Baby Voice" -> Color(0xFFF43F5E) // Bright pink
        "Chipmunk" -> Color(0xFFF59E0B) // Fast orange
        "Echo" -> Color(0xFF8B5CF6) // Violet reverberation
        else -> Color(0xFF34D399)
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("voice_waveform_canvas")
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2

        val path = Path()

        when (effect) {
            "Normal" -> {
                path.moveTo(0f, centerY)
                for (x in 0..width.toInt() step 4) {
                    val angle = (x * 0.02f) + phase
                    val amp = sin(angle.toDouble()) * 18 * sin((x * 0.005f).toDouble())
                    path.lineTo(x.toFloat(), (centerY + amp).toFloat())
                }
                drawPath(path, waveColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
            }

            "Deep Voice" -> {
                // Highly spaced out heavy oscillations representing low bass pitch
                path.moveTo(0f, centerY)
                for (x in 0..width.toInt() step 5) {
                    // Slow sweeping waves
                    val angle = (x * 0.008f) - (phase * 0.5f)
                    val amp = sin(angle.toDouble()) * 26 * cos((x * 0.002f).toDouble())
                    path.lineTo(x.toFloat(), (centerY + amp).toFloat())
                }
                drawPath(path, waveColor, style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round))
            }

            "Robot" -> {
                // Blocky, jagged digital square waves representing voice modulation synthesis
                for (x in 0..width.toInt() step 12) {
                    val xPos = x.toFloat()
                    val state = sin((x * 0.06f + phase * 2f).toDouble())
                    val top = if (state > 0) centerY - 18f else centerY + 18f
                    drawLine(
                        color = waveColor,
                        start = Offset(xPos, centerY),
                        end = Offset(xPos, top),
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Square
                    )
                }
            }

            "Baby Voice" -> {
                // High frequency hyper-oscillating bubbly wavelets
                path.moveTo(0f, centerY)
                for (x in 0..width.toInt() step 3) {
                    val angle = (x * 0.06f) + (phase * 3f)
                    val amp = sin(angle.toDouble()) * 12 * sin((x * 0.015f).toDouble())
                    path.lineTo(x.toFloat(), (centerY + amp).toFloat())
                }
                drawPath(path, waveColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
            }

            "Chipmunk" -> {
                // Hyperactive micro-spikes clustered tightly together
                for (x in 0..width.toInt() step 6) {
                    val xPos = x.toFloat()
                    val frequency = sin((x * 0.1f + phase * 4f).toDouble())
                    val top = centerY - (frequency * 22).toFloat()
                    drawLine(
                        color = waveColor,
                        start = Offset(xPos, centerY - 2),
                        end = Offset(xPos, top),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }

            "Echo" -> {
                // Layered offset paths decaying visual echoes
                val layers = listOf(1.0f to 1.0f, 0.6f to 0.5f, 0.3f to 0.25f)
                layers.forEach { (scaleAmp, scaleAlpha) ->
                    val echoPath = Path()
                    echoPath.moveTo(0f, centerY)
                    for (x in 0..width.toInt() step 4) {
                        val angle = (x * 0.03f) + phase - (scaleAmp * 5f)
                        val amp = sin(angle.toDouble()) * 20 * sin((x * 0.005f).toDouble()) * scaleAmp
                        echoPath.lineTo(x.toFloat(), (centerY + amp).toFloat())
                    }
                    drawPath(
                        path = echoPath,
                        color = waveColor.copy(alpha = scaleAlpha),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}


// ==========================================
// DETAILS / CREATION DIALOGS
// ==========================================

@Composable
fun ContactDetailDialog(
    contact: Contact,
    onDismiss: () -> Unit,
    onCall: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("contact_detail_dialog"),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val avatarColor = remember(contact.avatarColorHex) {
                    try {
                        Color(android.graphics.Color.parseColor(contact.avatarColorHex))
                    } catch (e: Exception) {
                        Color(0xFF2563EB)
                    }
                }
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(avatarColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = contact.name.trim().take(1).uppercase(),
                        color = avatarColor,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Detail Items Box
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = contact.phoneNumber, style = MaterialTheme.typography.bodyLarge)
                    }

                    if (contact.email.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = contact.email, style = MaterialTheme.typography.bodyLarge, overflow = TextOverflow.Ellipsis, maxLines = 1)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Quick Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Star button
                    IconButton(
                        onClick = onToggleFavorite,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            tint = if (contact.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            contentDescription = "Favorite Toggle"
                        )
                    }

                    // Edit
                    IconButton(
                        onClick = onEdit,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Contact")
                    }

                    // Delete
                    IconButton(
                        onClick = onDelete,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Contact")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Call Action Button
                Button(
                    onClick = onCall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("detail_call_action_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(imageVector = Icons.Default.Call, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Call Screen Mode", fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Close Details")
                }
            }
        }
    }
}

@Composable
fun AddContactDialog(
    initialNumber: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf(initialNumber) }
    var email by remember { mutableStateOf("") }
    var isFavorite by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("add_contact_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Add New Contact",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("contact_form_name")
                )

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("contact_form_phone")
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (Optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("contact_form_email")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isFavorite,
                        onCheckedChange = { isFavorite = it },
                        modifier = Modifier.testTag("contact_form_fav")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add to Favorites")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.trim().isNotEmpty() && phoneNumber.trim().isNotEmpty()) {
                                onSave(name, phoneNumber, email, isFavorite)
                            }
                        },
                        enabled = name.trim().isNotEmpty() && phoneNumber.trim().isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("contact_form_save")
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun EditContactDialog(
    contact: Contact,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(contact.name) }
    var phoneNumber by remember { mutableStateOf(contact.phoneNumber) }
    var email by remember { mutableStateOf(contact.email) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("edit_contact_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Edit Contact",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (Optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.trim().isNotEmpty() && phoneNumber.trim().isNotEmpty()) {
                                onSave(name, phoneNumber, email)
                            }
                        },
                        enabled = name.trim().isNotEmpty() && phoneNumber.trim().isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Update")
                    }
                }
            }
        }
    }
}
