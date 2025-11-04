package com.avikmakwana.networky

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.avikmakwana.networky.presentation.MonitorViewModel
import com.avikmakwana.networky.ui.theme.NetworkyTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NetworkyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Semantics: Use the standard Composable pattern for screen content.
                    MonitorScreen(Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun MonitorScreen(
    modifier: Modifier = Modifier,
    viewModel: MonitorViewModel = hiltViewModel()
) {
    val viewState by viewModel.viewState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // SCALES MANDATE: Added contentDescription for accessibility compliance.
        val statusDescription = "Current network status: ${viewState.statusText}"

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = statusDescription },
            colors = CardDefaults.cardColors(containerColor = viewState.statusColor.copy(alpha = 0.8f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "GLOBAL NETWORK STATUS",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = viewState.statusText,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = viewModel::toggleSocketConnection,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                // CRITICAL FIX: Use the stable boolean state, not a string match.
                enabled = !viewState.isChecking
            ) {
                Text(if (viewState.isSocketActive) "PAUSE Monitor (Socket Active)" else "RESUME Monitor (Socket Idle)")
            }

            Button(
                onClick = viewModel::checkInternetNow,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                // CRITICAL: Can only force check if socket is not active AND monitor isn't busy.
                enabled = !viewState.isSocketActive && !viewState.isChecking
            ) {
                Text("Force Check Now")
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = if (viewState.isSocketActive) {
                "Socket.IO is CONNECTED. Background monitoring is PAUSED to save battery."
            } else {
                "Socket.IO is IDLE. Background monitoring is RUNNING with exponential backoff." // Added 'exponential' for clarity
            },
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        )
    }
}
