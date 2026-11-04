package com.example.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Pengaturan
import com.example.ui.theme.SinarMasBlue

@Composable
fun ColumnHeaderDialog(
    currentSettings: Pengaturan,
    onSave: (judul: String, k1: String, k2: String, k3: String, k4: String, k5: String) -> Unit,
    onDismiss: () -> Unit
) {
    var judul by remember { mutableStateOf(currentSettings.judul) }
    var k1 by remember { mutableStateOf(currentSettings.kolom1) }
    var k2 by remember { mutableStateOf(currentSettings.kolom2) }
    var k3 by remember { mutableStateOf(currentSettings.kolom3) }
    var k4 by remember { mutableStateOf(currentSettings.kolom4) }
    var k5 by remember { mutableStateOf(currentSettings.kolom5) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Atur Judul & Label Kolom",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Sesuaikan judul tabel dan nama 5 kolom catatan stok sesuai kebutuhan gudang atau toko Anda:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = judul,
                    onValueChange = { judul = it },
                    label = { Text("Judul Tabel / Laporan") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = k1,
                    onValueChange = { k1 = it },
                    label = { Text("Judul Kolom 1") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = k2,
                    onValueChange = { k2 = it },
                    label = { Text("Judul Kolom 2") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = k3,
                    onValueChange = { k3 = it },
                    label = { Text("Judul Kolom 3") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = k4,
                    onValueChange = { k4 = it },
                    label = { Text("Judul Kolom 4") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = k5,
                    onValueChange = { k5 = it },
                    label = { Text("Judul Kolom 5") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(judul, k1, k2, k3, k4, k5)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SinarMasBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Simpan Judul", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
