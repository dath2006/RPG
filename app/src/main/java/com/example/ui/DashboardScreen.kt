package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LedgerEntry
import com.example.data.Oath
import com.example.data.DistractionRule
import com.example.data.EvidenceSubmission
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val balancePaise by viewModel.balance.collectAsState()
    val rawEntries by viewModel.ledgerEntries.collectAsState()
    val activeNfc by viewModel.activeNfcSession.collectAsState()
    val streakValue by viewModel.streak.collectAsState()
    val multiplierValue by viewModel.multiplier.collectAsState()
    val creditValue by viewModel.creditScore.collectAsState()
    val rules by viewModel.distractionRules.collectAsState()
    val subs by viewModel.evidenceSubmissions.collectAsState()
    val activeOaths by viewModel.oaths.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "PRODUCTIVITY ECONOMY",
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        color = SoftGreen,
                        letterSpacing = 1.sp,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "App logo Shield icon",
                        tint = SoftGreen,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundLight
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceVariant,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard Core Navigation Tab") },
                    label = { Text("Core") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SurfaceVariant,
                        selectedTextColor = SoftGreen,
                        indicatorColor = SoftGreen,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Face, contentDescription = "Evidence Submissions Navigation Tab") },
                    label = { Text("Evidence") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SurfaceVariant,
                        selectedTextColor = SoftGreen,
                        indicatorColor = SoftGreen,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Warning, contentDescription = "Distractions Log Navigation Tab") },
                    label = { Text("App Drain") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SurfaceVariant,
                        selectedTextColor = SoftGreen,
                        indicatorColor = SoftGreen,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.List, contentDescription = "Oaths Status Navigation Tab") },
                    label = { Text("Oaths") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SurfaceVariant,
                        selectedTextColor = SoftGreen,
                        indicatorColor = SoftGreen,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Configurations Control Tab") },
                    label = { Text("Config") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SurfaceVariant,
                        selectedTextColor = SoftGreen,
                        indicatorColor = SoftGreen,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
            }
        },
        containerColor = BackgroundLight
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> CoreTab(
                    balancePaise = balancePaise,
                    entries = rawEntries,
                    activeNfc = activeNfc,
                    streak = streakValue,
                    multiplier = multiplierValue,
                    creditScore = creditValue,
                    onStartNfc = { viewModel.startNfcSession(it) },
                    onEndNfc = { mins -> viewModel.endNfcSession(mins) },
                    onDiscardNfc = { viewModel.discardNfcSession() },
                    onFlagDispute = { entryId -> viewModel.flagDispute(entryId, "DUPLICATE_CHARGE", "User flagged via UI") }
                )
                1 -> EvidenceTab(
                    submissions = subs,
                    onSubmit = { claim, amount -> viewModel.submitStudyEvidence(claim, amount, "ic_evidence_file_icon") },
                    onEvaluate = { viewModel.runAutoAIVerifier(it) }
                )
                2 -> DistractionsTab(
                    rules = rules,
                    onScroll = { pkg, min -> viewModel.simulateDistractionScroll(pkg, min) }
                )
                3 -> OathsTab(
                    activeOaths = activeOaths,
                    creditScore = creditValue,
                    onCreateOath = { loan, desc, hours -> viewModel.createOath(loan, desc, hours) },
                    onComplete = { viewModel.completeOath(it) },
                    onDefault = { viewModel.defaultOath(it) }
                )
                4 -> {
                    val modelState = viewModel.modelDownloadState.collectAsState().value
                    val mercyTokens = viewModel.mercyTokens.collectAsState().value
                    val exportStr = viewModel.exportString.collectAsState().value
                    ConfigTab(
                        streak = streakValue,
                        multiplier = multiplierValue,
                        creditScore = creditValue,
                        mercyTokens = mercyTokens,
                        modelDownloadState = modelState,
                        onSimulateMidnightAudit = { viewModel.simulateMidnightAudit() },
                        onSimulateSalaryDay = { viewModel.simulateSalaryDay() },
                        onSimulateStepIncome = { viewModel.simulateStepIncome(8500) },
                        onSpendMercyToken = { viewModel.spendMercyToken() },
                        onGenerateExport = { viewModel.generateExport() },
                        exportString = exportStr,
                        onDismissExport = { viewModel.dismissExport() },
                        onInsertEarning = { amount, sub -> viewModel.insertManualAdd(amount, "LOOT_DROP", sub) },
                        onInsertDebit = { amount, sub -> viewModel.insertManualDeduct(amount, "LAZY_TAX", sub) },
                        onDownloadModel = { viewModel.downloadAiModel() }
                    )
                }
            }
        }
    }
}

@Composable
fun CoreTab(
    balancePaise: Long,
    entries: List<LedgerEntry>,
    activeNfc: com.example.data.NfcSession?,
    streak: Int,
    multiplier: Double,
    creditScore: Int,
    onStartNfc: (String) -> Unit,
    onEndNfc: (Int) -> Unit,
    onDiscardNfc: () -> Unit,
    onFlagDispute: (String) -> Unit
) {
    val isDebt = balancePaise < 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 2.dp,
                        color = if (isDebt) SoftRed else SoftGreen,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .testTag("balance_card"),
                colors = CardDefaults.cardColors(
                    containerColor = SurfaceLight
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isDebt) "🚨 SYSTEM IN JAIL DEBT" else "🏦 IMMUTABLE LEDGER BALANCE",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 12.sp,
                        color = if (isDebt) SoftRed else Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "₹${"%,.2f".format(balancePaise / 100.0)}",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDebt) SoftRed else SoftGreen
                    )
                    if (isDebt) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = BackgroundLight),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Every scroll on unapproved apps costs real-time Surge Pricing. Clear your debt!",
                                fontSize = 11.sp,
                                color = SoftRed,
                                modifier = Modifier.padding(12.dp),
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("STREAK", fontSize = 10.sp, color = Color.Gray, fontFamily = FontFamily.SansSerif)
                        Text(
                            text = "🔥 $streak Days",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = SoftOrange,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("MULTIPLIER", fontSize = 10.sp, color = Color.Gray, fontFamily = FontFamily.SansSerif)
                        Text(
                            text = "✨ ${multiplier}x",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = SoftGreen,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("CREDIT SCORE", fontSize = 10.sp, color = Color.Gray, fontFamily = FontFamily.SansSerif)
                        Text(
                            text = "🛡️ $creditScore",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Cyan,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "🔌 SECURE NFC DESK TAP FOCUS SESSION",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SoftGreen
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (activeNfc == null) {
                        Text(
                            text = "No focus study session is currently running. Tap your desk companion NFC tag to start earning focal wages securely.",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { onStartNfc("NFC-UID-${UUID.randomUUID().toString().take(6)}") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("nfc_start_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = SoftGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "SIMULATE SECURE NFC TAG COMPANION TAP",
                                fontFamily = FontFamily.SansSerif,
                                color = SurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        var ticksSince by remember { mutableStateOf(0L) }
                        LaunchedEffect(Unit) {
                            while (true) {
                                delay(1000)
                                ticksSince += 1
                            }
                        }

                        val formatter = SimpleDateFormat("HH:mm:ss", Locale.US)
                        val elapsedSec = (System.currentTimeMillis() - activeNfc.startTimeMs) / 1000L

                        Text(
                            text = "ACTIVE SESSION RUNNING!",
                            color = SoftOrange,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Started at: ${formatter.format(Date(activeNfc.startTimeMs))}",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Elapsed Time: ${elapsedSec / 60} min ${elapsedSec % 60} sec",
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 18.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { onEndNfc(34) },
                                modifier = Modifier
                                    .weight(1.5f)
                                    .minimumInteractiveComponentSize(),
                                colors = ButtonDefaults.buttonColors(containerColor = SoftGreen),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "END STUDY (SIM 34m)",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 11.sp,
                                    color = SurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = { onEndNfc(0) },
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize(),
                                colors = ButtonDefaults.buttonColors(containerColor = SoftGray),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "END SEC",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                            }

                            IconButton(
                                onClick = onDiscardNfc,
                                modifier = Modifier
                                    .background(SoftRed.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Discard and delete current focus session", tint = SoftRed)
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "📜 REAL-TIME LEDGER AUDITING REGISTRY",
                fontFamily = FontFamily.SansSerif,
                fontSize = 12.sp,
                color = SoftGreen,
                fontWeight = FontWeight.Bold
            )
        }

        if (entries.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight.copy(alpha = 0.5f))
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "Ledger registry is pristine. Earn rewards by submitting evidence sheets or tapping companion tags!",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        } else {
            items(entries) { entry ->
                LedgerItemCard(entry, onFlagDispute)
            }
        }
    }
}

@Composable
fun LedgerItemCard(entry: LedgerEntry, onFlagDispute: (String) -> Unit) {
    val isCredit = entry.amountPaise > 0
    val formattingDate = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US).format(Date(entry.timestampMs))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.category.replace("_", " "),
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 11.sp,
                    color = if (isCredit) SoftGreen else SoftRed,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = entry.subcategory ?: "Direct balancing adjustment",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formattingDate,
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }

            Text(
                text = "${if (isCredit) "+" else ""}₹${"%,.2f".format(entry.amountPaise / 100.0)}",
                fontFamily = FontFamily.SansSerif,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isCredit) SoftGreen else SoftRed
            )

            IconButton(onClick = { onFlagDispute(entry.id) }) {
                Icon(Icons.Default.Warning, contentDescription = "Flag Dispute", tint = SoftOrange)
            }
        }
    }
}

@Composable
fun EvidenceTab(
    submissions: List<EvidenceSubmission>,
    onSubmit: (String, Long) -> Unit,
    onEvaluate: (String) -> Unit
) {
    var subjectInput by remember { mutableStateOf("") }
    var rewardInput by remember { mutableStateOf("100") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "📸 SUBMIT NEW MANUAL STUDY LOG",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SoftGreen
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    TextField(
                        value = subjectInput,
                        onValueChange = { subjectInput = it },
                        label = { Text("Study Activity (e.g. Kotlin)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = BackgroundLight,
                            unfocusedContainerColor = BackgroundLight,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    TextField(
                        value = rewardInput,
                        onValueChange = { rewardInput = it },
                        label = { Text("Claim Target Reward (₹)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = BackgroundLight,
                            unfocusedContainerColor = BackgroundLight,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val reward = (rewardInput.toLongOrNull() ?: 100L) * 100L
                            if (subjectInput.isNotBlank()) {
                                onSubmit(subjectInput, reward)
                                subjectInput = ""
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize(),
                        colors = ButtonDefaults.buttonColors(containerColor = SoftGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "SUBMIT STUDY PROOF (SIMULATED PHOTO)",
                            color = SurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            }
        }

        item {
            Text(
                "📂 STUDY LOG ENTRIES REGISTERED",
                fontFamily = FontFamily.SansSerif,
                fontSize = 12.sp,
                color = SoftGreen,
                fontWeight = FontWeight.Bold
            )
        }

        if (submissions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight.copy(alpha = 0.5f))
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "No registered logs found. Log your focal accomplishments!",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            items(submissions) { sub ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    sub.claimDescription,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    "Amount Claimed: ₹${sub.claimAmountPaise / 100}",
                                    fontSize = 12.sp,
                                    color = SoftGreen,
                                    fontFamily = FontFamily.SansSerif
                                )
                            }

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = when (sub.status) {
                                        "VERIFIED" -> SoftGreen.copy(alpha = 0.15f)
                                        "REJECTED" -> SoftRed.copy(alpha = 0.15f)
                                        else -> SoftOrange.copy(alpha = 0.15f)
                                    }
                                ),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = sub.status,
                                    color = when (sub.status) {
                                        "VERIFIED" -> SoftGreen
                                        "REJECTED" -> SoftRed
                                        else -> SoftOrange
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (sub.status == "PENDING_AI") {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { onEvaluate(sub.id) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize(),
                                colors = ButtonDefaults.buttonColors(containerColor = SoftGray),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "AUTO-COACH VERIFY WITH LOCAL AI",
                                    fontSize = 10.sp,
                                    color = Color.White,
                                    fontFamily = FontFamily.SansSerif
                                )
                            }
                        } else if (sub.aiReasoning != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Coach evaluation: " + sub.aiReasoning,
                                fontSize = 11.sp,
                                color = Color.LightGray,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DistractionsTab(
    rules: List<DistractionRule>,
    onScroll: (String, Int) -> Unit
) {
    val presetTimes = listOf(5, 15, 30)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "💀 THE DISTRACTION APP DRAIN REGULATOR",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SoftGreen
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Every minute of unmonitored scrolling deducts virtual balance. Under study focus times, Surge Pricing increases deduction rates significantly.",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        item {
            Text(
                "📱 REGISTERED DISTRAC APP DRAIN THREATS",
                fontFamily = FontFamily.SansSerif,
                fontSize = 12.sp,
                color = SoftGreen,
                fontWeight = FontWeight.Bold
            )
        }

        items(rules) { app ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                app.appDisplayName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                "Package: " + app.packageName,
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }

                        Text(
                            "₹${app.costPerMinutePaise / 100.0}/min",
                            color = SoftRed,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Trigger Simulated Mindfulness Scroll Drainage:",
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        presetTimes.forEach { mins ->
                            Button(
                                onClick = { onScroll(app.packageName, mins) },
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize(),
                                colors = ButtonDefaults.buttonColors(containerColor = SoftGray),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    "-$mins min",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    color = SoftRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OathsTab(
    activeOaths: List<Oath>,
    creditScore: Int,
    onCreateOath: (Long, String, Int) -> Unit,
    onComplete: (String) -> Unit,
    onDefault: (String) -> Unit
) {
    var oathDesc by remember { mutableStateOf("") }
    var oathLoanInput by remember { mutableStateOf("150") }
    var oathHoursInput by remember { mutableStateOf("4") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "⚔️ TAKE A SMART PRODUCTIVITY OATH",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SoftGreen
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Take a virtual smart contract Loan instantly. Complete your target task on time to expand your credit score. Fail, and compound daily interest triggers default penalties!",
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    TextField(
                        value = oathDesc,
                        onValueChange = { oathDesc = it },
                        label = { Text("Oath Covenant (e.g. Finish Android Homework)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = BackgroundLight,
                            unfocusedContainerColor = BackgroundLight,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    TextField(
                        value = oathLoanInput,
                        onValueChange = { oathLoanInput = it },
                        label = { Text("Oath Microloan Amount (₹)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = BackgroundLight,
                            unfocusedContainerColor = BackgroundLight,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    TextField(
                        value = oathHoursInput,
                        onValueChange = { oathHoursInput = it },
                        label = { Text("Covenant Deadline (Hours)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = BackgroundLight,
                            unfocusedContainerColor = BackgroundLight,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val loanAmt = (oathLoanInput.toLongOrNull() ?: 150L) * 100L
                            val hours = oathHoursInput.toIntOrNull() ?: 4
                            if (oathDesc.isNotBlank()) {
                                onCreateOath(loanAmt, oathDesc, hours)
                                oathDesc = ""
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize(),
                        colors = ButtonDefaults.buttonColors(containerColor = SoftGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "SIGN COVENANT AND UNLOCK LOAN",
                            fontFamily = FontFamily.SansSerif,
                            color = SurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Text(
                "🛡️ CURRENT ACTIVE COVENANTS OATHS",
                fontFamily = FontFamily.SansSerif,
                fontSize = 12.sp,
                color = SoftGreen,
                fontWeight = FontWeight.Bold
            )
        }

        val actives = activeOaths.filter { it.status == "ACTIVE" }
        if (actives.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight.copy(alpha = 0.5f))
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "No active smart Oaths signed. Create one above to test your commitment!",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        } else {
            items(actives) { oath ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            oath.taskDescription,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Collateral Loan: ₹${oath.initialLoanAmountPaise / 100}",
                            color = SoftGreen,
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { onComplete(oath.id) },
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize(),
                                colors = ButtonDefaults.buttonColors(containerColor = SoftGreen),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "COMPLETE/REPAY",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    color = SurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = { onDefault(oath.id) },
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize(),
                                colors = ButtonDefaults.buttonColors(containerColor = SoftRed),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "DEFAULT/FAIL",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    color = SurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConfigTab(
    streak: Int,
    multiplier: Double,
    creditScore: Int,
    mercyTokens: Int,
    modelDownloadState: com.example.service.ModelDownloadState,
    onSimulateMidnightAudit: () -> Unit,
    onSimulateSalaryDay: () -> Unit,
    onSimulateStepIncome: () -> Unit,
    onSpendMercyToken: () -> Unit,
    onGenerateExport: () -> Unit,
    exportString: String?,
    onDismissExport: () -> Unit,
    onInsertEarning: (Long, String) -> Unit,
    onInsertDebit: (Long, String) -> Unit,
    onDownloadModel: () -> Unit
) {
    var manualAddAmount by remember { mutableStateOf("100") }
    var manualAddDesc by remember { mutableStateOf("Testing Manual Override") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            val context = androidx.compose.ui.platform.LocalContext.current
            val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
            ) { _ -> }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "🔐 APP PERMISSIONS REQUIRED",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SoftRed
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "To track UPI payments from Slice/GPay and SMS alerts seamlessly, the following permissions must be granted natively.",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            permissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.RECEIVE_SMS,
                                    android.Manifest.permission.READ_SMS,
                                    android.Manifest.permission.POST_NOTIFICATIONS
                                )
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize(),
                        colors = ButtonDefaults.buttonColors(containerColor = BackgroundLight),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SoftGreen)
                    ) {
                        Text(
                            "GRANT SMS & NOTIFICATIONS (Standard)",
                            fontFamily = FontFamily.SansSerif,
                            color = SoftGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val intent = android.content.Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize(),
                        colors = ButtonDefaults.buttonColors(containerColor = BackgroundLight),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SoftOrange)
                    ) {
                        Text(
                            "GRANT UPI TRACKING (Special Access)",
                            fontFamily = FontFamily.SansSerif,
                            color = SoftOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "🧠 LOCAL AI MODEL MANAGER",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SoftGreen
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Gemma-4-E4B on-device language model for classifying receipts and verifying study evidence photos locally.",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    when (modelDownloadState) {
                        is com.example.service.ModelDownloadState.Ready -> {
                            Text(
                                "✅ Model is Loaded & Ready",
                                color = SoftGreen,
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        is com.example.service.ModelDownloadState.Downloading -> {
                            Text(
                                "🔄 Downloading: ${modelDownloadState.progressPercent}%",
                                color = SoftOrange,
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        else -> {
                            Button(
                                onClick = onDownloadModel,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize(),
                                colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariant),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SoftGreen)
                            ) {
                                Text(
                                    "DOWNLOAD MODEL (Simulated)",
                                    fontFamily = FontFamily.SansSerif,
                                    color = SoftGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "⚙️ SYSTEM SIMULATORS & CONFIGURATIONS",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SoftGreen
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onSimulateMidnightAudit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize(),
                        colors = ButtonDefaults.buttonColors(containerColor = SoftOrange),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "SIMULATE MIDNIGHT COMPOUNDING AUDIT",
                            fontFamily = FontFamily.SansSerif,
                            color = SurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "MERCY TOKENS:",
                            color = Color.LightGray,
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 11.sp
                        )
                        Text(
                            "🪙 $mercyTokens",
                            color = SoftOrange,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 14.sp
                        )
                    }
                    
                    Button(
                        onClick = onSpendMercyToken,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariant),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SoftOrange),
                        shape = RoundedCornerShape(12.dp),
                        enabled = mercyTokens > 0
                    ) {
                        Text(
                            "SPEND MERCY TOKEN (Protect Streak)",
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 10.sp,
                            color = SoftOrange
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onSimulateSalaryDay,
                        modifier = Modifier
                            .fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariant),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SoftGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "SIMULATE SALARY DAY BONUS",
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 10.sp,
                            color = SoftGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onSimulateStepIncome,
                        modifier = Modifier
                            .fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariant),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SoftRed),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "SIMULATE STEP SYNC (8,500 Steps)",
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 10.sp,
                            color = SoftRed
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onGenerateExport,
                        modifier = Modifier
                            .fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariant),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "EXPORT ECONOMY BACKUP (Phase 5)",
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }

                    if (exportString != null) {
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = onDismissExport,
                            title = { Text("Exported Data (Base64)") },
                            text = { 
                                androidx.compose.foundation.text.selection.SelectionContainer {
                                    Text(exportString, fontSize = 10.sp, color = TextPrimary, maxLines = 10, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                }
                            },
                            confirmButton = {
                                androidx.compose.material3.TextButton(onClick = onDismissExport) {
                                    Text("CLOSE")
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "DEBUG MANUAL TRANSACTIONS",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 12.sp,
                        color = SoftGreen
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    TextField(
                        value = manualAddAmount,
                        onValueChange = { manualAddAmount = it },
                        label = { Text("Amount (₹)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = BackgroundLight,
                            unfocusedContainerColor = BackgroundLight,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    TextField(
                        value = manualAddDesc,
                        onValueChange = { manualAddDesc = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = BackgroundLight,
                            unfocusedContainerColor = BackgroundLight,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                val amt = (manualAddAmount.toLongOrNull() ?: 0L) * 100L
                                if (amt > 0) {
                                    onInsertEarning(amt, manualAddDesc)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = SoftGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("+ EARN", color = SurfaceVariant, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val amt = (manualAddAmount.toLongOrNull() ?: 0L) * 100L
                                if (amt > 0) {
                                    onInsertDebit(amt, manualAddDesc)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = SoftRed),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("- FINE", color = SurfaceVariant, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
