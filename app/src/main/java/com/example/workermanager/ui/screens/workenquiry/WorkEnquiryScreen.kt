package com.example.workermanager.ui.screens.workenquiry

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkEnquiryScreen(onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var workType by remember { mutableStateOf("") }
    var experience by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf(false) }
    var mobileError by remember { mutableStateOf(false) }
    var workTypeError by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val companyWhatsappNumber = "919838868696" // Configurable company/manager number

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Work Enquiry", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Submit your details below and we will contact you on WhatsApp.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = name,
                onValueChange = { 
                    name = it
                    nameError = false
                },
                label = { Text("Full Name *") },
                isError = nameError,
                supportingText = if (nameError) { { Text("Name is required") } } else null,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = mobile,
                onValueChange = { 
                    mobile = it
                    mobileError = false
                },
                label = { Text("Mobile Number *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                isError = mobileError,
                supportingText = if (mobileError) { { Text("Valid 10-digit mobile number required") } } else null,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = workType,
                onValueChange = { 
                    workType = it
                    workTypeError = false
                },
                label = { Text("Work/Job Type *") },
                isError = workTypeError,
                supportingText = if (workTypeError) { { Text("Work type is required") } } else null,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = experience,
                onValueChange = { experience = it },
                label = { Text("Experience (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Preferred Location (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Message / Additional Details (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(modifier = Modifier.weight(1f, fill = false))

            Button(
                onClick = {
                    nameError = name.isBlank()
                    mobileError = mobile.isBlank() || !mobile.matches(Regex("^[0-9]{10}$"))
                    workTypeError = workType.isBlank()

                    if (!nameError && !mobileError && !workTypeError) {
                        val whatsappMessage = """
                            Hello, I am looking for work.
                            
                            Name: $name
                            Mobile: $mobile
                            Work/Job Type: $workType
                            Experience: ${if (experience.isBlank()) "N/A" else experience}
                            Preferred Location: ${if (location.isBlank()) "N/A" else location}
                            
                            Additional Details:
                            ${if (message.isBlank()) "None" else message}
                            
                            Please let me know if there is any suitable work available.
                        """.trimIndent()

                        try {
                            val url = Uri.parse("https://api.whatsapp.com/send?phone=$companyWhatsappNumber&text=${URLEncoder.encode(whatsappMessage, "UTF-8")}")
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = url
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to open WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Send on WhatsApp", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
