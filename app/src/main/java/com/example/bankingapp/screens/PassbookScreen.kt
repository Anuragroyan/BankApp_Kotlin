package com.example.bankingapp.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bankingapp.data.PassbookExporter
import com.example.bankingapp.data.model.AccountHolder
import com.example.bankingapp.data.model.Transaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val PassbookCream = Color(0xFFFBF3E3)
private val PassbookInk = Color(0xFF2B2118)
private val PassbookRed = Color(0xFFB33A3A)
private val PassbookLine = Color(0xFFC9B896)

@Composable
fun PassbookScreen(account: AccountHolder, onBack: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    BackHandler(onBack = onBack)

    Column(
        Modifier
            .fillMaxSize()
            .background(PassbookCream)
    ) {
        // Top bar
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onBack) { Text("← Back", color = PassbookInk) }
            Button(onClick = {
                val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                view.draw(canvas)
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "passbook_${account.accountNumber}_$timestamp.png"
                PassbookExporter.saveBitmapToPictures(context, bitmap, fileName)
            }) {
                Text("📷 Save as Image")
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            // Bank header
            Text(
                "🏦 OVERSEAS BANK",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = PassbookRed,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "PASS BOOK",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 4.sp,
                color = PassbookInk,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = PassbookInk, thickness = 1.dp)
            Spacer(Modifier.height(12.dp))

            // Account holder details block
            PassbookDetailRow("Account Holder", account.name)
            PassbookDetailRow("Account Number", account.accountNumber)
            PassbookDetailRow("Current Balance", "₹${"%.2f".format(account.balance)}")

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = PassbookInk, thickness = 1.dp)
            Spacer(Modifier.height(8.dp))

            // Table header
            PassbookRowHeader()
            HorizontalDivider(color = PassbookInk, thickness = 1.dp)
        }

        // Ledger rows
        var running = 0.0
        val rows = account.transactions.map { tx ->
            running += if (tx.type == "Deposit") tx.amount else -tx.amount
            tx to running
        }

        if (rows.isEmpty()) {
            Box(
                Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No transactions yet", color = PassbookInk.copy(alpha = 0.6f))
            }
        } else {
            LazyColumn(Modifier.padding(horizontal = 20.dp)) {
                items(rows) { (tx, balanceAfter) ->
                    PassbookLedgerRow(tx, balanceAfter)
                    HorizontalDivider(color = PassbookLine, thickness = 0.7.dp)
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun PassbookDetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(
            "$label:",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = PassbookInk.copy(alpha = 0.7f),
            modifier = Modifier.width(140.dp)
        )
        Text(
            value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = PassbookInk
        )
    }
}

@Composable
private fun PassbookRowHeader() {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text("Date", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PassbookInk, modifier = Modifier.weight(1.1f))
        Text("Particulars", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PassbookInk, modifier = Modifier.weight(1f))
        Text("Withdrawal", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PassbookInk, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        Text("Deposit", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PassbookInk, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        Text("Balance", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PassbookInk, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
    }
}

@Composable
private fun PassbookLedgerRow(tx: Transaction, balanceAfter: Double) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(tx.date, fontSize = 11.sp, color = PassbookInk, modifier = Modifier.weight(1.1f))
        Text(tx.type, fontSize = 11.sp, color = PassbookInk, modifier = Modifier.weight(1f))
        Text(
            if (tx.type == "Withdrawal") "₹${"%.2f".format(tx.amount)}" else "-",
            fontSize = 11.sp,
            color = PassbookRed,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
        Text(
            if (tx.type == "Deposit") "₹${"%.2f".format(tx.amount)}" else "-",
            fontSize = 11.sp,
            color = Color(0xFF2E7D32),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
        Text(
            "₹${"%.2f".format(balanceAfter)}",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = PassbookInk,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )

    }
}