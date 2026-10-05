package com.example.workermanager.ui.screens.sites

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.workermanager.data.entity.SiteEntity
import com.example.workermanager.data.entity.SiteStatus
import com.example.workermanager.viewmodel.SiteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SitesScreen(
    viewModel: SiteViewModel,
    onSiteClick: (Long) -> Unit
) {
    val sites by viewModel.allSites.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Project Sites") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Site")
            }
        }
    ) { padding ->
        if (sites.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No sites added yet.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sites) { site ->
                    SiteItem(site = site, onClick = { onSiteClick(site.id) })
                }
            }
        }

        if (showAddDialog) {
            AddSiteDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { name, address, manager ->
                    viewModel.addSite(name, address, manager)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun SiteItem(site: SiteEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(site.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(site.address, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                StatusChip(site.status)
            }
        }
    }
}

@Composable
fun StatusChip(status: SiteStatus) {
    val color = when (status) {
        SiteStatus.ACTIVE -> Color(0xFF4CAF50)
        SiteStatus.COMPLETED -> Color(0xFF2196F3)
        SiteStatus.ON_HOLD -> Color(0xFFFF9800)
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = status.name,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSiteDialog(onDismiss: () -> Unit, onConfirm: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var manager by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Site") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Site Name") })
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") })
                OutlinedTextField(value = manager, onValueChange = { manager = it }, label = { Text("Site Manager") })
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotEmpty()) onConfirm(name, address, manager) }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
