package com.example.bankingapp.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.bankingapp.data.model.AccountHolder
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.content.ContentValues
import android.widget.Toast
import android.graphics.Bitmap
import java.io.FileOutputStream

@Serializable
data class TransactionEntry(
    val srNo: Int,
    val date: String,
    val type: String,
    val amount: Double,
    val balanceAfter: Double
)

@Serializable
data class AccountSummary(
    val totalDeposits: Double,
    val totalWithdrawals: Double,
    val transactionCount: Int,
    val currentBalance: Double
)

@Serializable
data class AccountHolderInfo(
    val name: String,
    val accountNumber: String
)

@Serializable
data class PassbookExport(
    val bankName: String = "Overseas Bank App",
    val exportedAt: String,
    val accountHolder: AccountHolderInfo,
    val summary: AccountSummary,
    val transactions: List<TransactionEntry>
)

@Serializable
data class BankExport(
    val bankName: String = "Overseas Bank App",
    val exportedAt: String,
    val totalAccounts: Int,
    val accounts: List<PassbookExport>
)

object PassbookExporter {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    fun buildPassbook(account: AccountHolder): PassbookExport {
        var running = 0.0
        val entries = account.transactions.mapIndexed { index, tx ->
            running += if (tx.type == "Deposit") tx.amount else -tx.amount
            TransactionEntry(
                srNo = index + 1,
                date = tx.date,
                type = tx.type,
                amount = tx.amount,
                balanceAfter = running
            )
        }

        val totalDeposits = account.transactions.filter { it.type == "Deposit" }.sumOf { it.amount }
        val totalWithdrawals = account.transactions.filter { it.type == "Withdrawal" }.sumOf { it.amount }

        return PassbookExport(
            exportedAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()),
            accountHolder = AccountHolderInfo(account.name, account.accountNumber),
            summary = AccountSummary(
                totalDeposits = totalDeposits,
                totalWithdrawals = totalWithdrawals,
                transactionCount = account.transactions.size,
                currentBalance = account.balance
            ),
            transactions = entries
        )
    }

    fun buildBankExport(accounts: List<AccountHolder>): BankExport {
        return BankExport(
            exportedAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()),
            totalAccounts = accounts.size,
            accounts = accounts.map { buildPassbook(it) }
        )
    }

    // ---------- Share via intent (existing feature) ----------

    fun exportToFile(context: Context, account: AccountHolder): File {
        val passbook = buildPassbook(account)
        val jsonString = json.encodeToString(passbook)

        val dir = File(context.getExternalFilesDir(null), "exports")
        if (!dir.exists()) dir.mkdirs()

        val safeAccNo = account.accountNumber.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val file = File(dir, "passbook_${safeAccNo}.txt")
        file.writeText(jsonString)
        return file
    }

    fun exportAllToFile(context: Context, accounts: List<AccountHolder>): File {
        val bankExport = buildBankExport(accounts)
        val jsonString = json.encodeToString(bankExport)

        val dir = File(context.getExternalFilesDir(null), "exports")
        if (!dir.exists()) dir.mkdirs()

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(dir, "all_accounts_$timestamp.txt")
        file.writeText(jsonString)
        return file
    }

    fun exportAndShare(context: Context, account: AccountHolder) {
        val file = exportToFile(context, account)
        shareFile(context, file, "Passbook - ${account.accountNumber}", "Export Passbook")
    }

    fun exportAllAndShare(context: Context, accounts: List<AccountHolder>) {
        if (accounts.isEmpty()) return
        val file = exportAllToFile(context, accounts)
        shareFile(context, file, "All Accounts Passbook Export", "Export All Accounts")
    }

    private fun shareFile(context: Context, file: File, subject: String, chooserTitle: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
    }

    // ---------- Download directly to phone's Downloads folder (new feature) ----------

    fun downloadPassbook(context: Context, account: AccountHolder) {
        val passbook = buildPassbook(account)
        val jsonString = json.encodeToString(passbook)
        val safeAccNo = account.accountNumber.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val fileName = "passbook_${safeAccNo}.txt"

        saveJsonToDownloads(context, fileName, jsonString)
    }

    fun downloadAllPassbooks(context: Context, accounts: List<AccountHolder>) {
        if (accounts.isEmpty()) {
            Toast.makeText(context, "No accounts to export", Toast.LENGTH_SHORT).show()
            return
        }
        val bankExport = buildBankExport(accounts)
        val jsonString = json.encodeToString(bankExport)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "all_accounts_$timestamp.txt"

        saveJsonToDownloads(context, fileName, jsonString)
    }

    private fun saveJsonToDownloads(context: Context, fileName: String, jsonString: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+: use MediaStore, no permission needed
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/plain")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        out.write(jsonString.toByteArray())
                    }
                    Toast.makeText(context, "Saved to Downloads: $fileName", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Failed to save file", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Android 9 and below: write directly to public Downloads dir
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val file = File(downloadsDir, fileName)
                file.writeText(jsonString)
                Toast.makeText(context, "Saved to Downloads: $fileName", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun saveBitmapToPictures(context: Context, bitmap: Bitmap, fileName: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    Toast.makeText(context, "Saved to Pictures: $fileName", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
                }
            } else {
                @Suppress("DEPRECATION")
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                if (!picturesDir.exists()) picturesDir.mkdirs()
                val file = File(picturesDir, fileName)
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                Toast.makeText(context, "Saved to Pictures: $fileName", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Image export failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}