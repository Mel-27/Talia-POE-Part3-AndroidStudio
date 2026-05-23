package com.gia.poe_demo

import androidx.appcompat.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.gia.poe_demo.R
import com.gia.poe_demo.data.entities.Category
import com.gia.poe_demo.data.entities.Expense
import com.gia.poe_demo.databinding.ActivityAddExpenseBinding
import com.gia.poe_demo.data.database.AppDatabase
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AddExpenseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddExpenseBinding
    private lateinit var db: AppDatabase
    private var selectedCategoryId: Long = -1
    private var categories: List<Category> = emptyList()

    // Date and time as Long timestamps
    private var selectedDateTimestamp: Long = System.currentTimeMillis()
    private var selectedStartTimeTimestamp: Long = System.currentTimeMillis()
    private var selectedEndTimeTimestamp: Long = System.currentTimeMillis()

    // For photo capture
    private var currentPhotoPath: String? = null
    private var categoryAdapter: ArrayAdapter<String>? = null

    // Register activity result launcher for camera
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            setPhotoPreview()
            android.util.Log.d("AddExpense", "Photo captured and saved at: $currentPhotoPath")
            Toast.makeText(this, "Receipt photo attached", Toast.LENGTH_SHORT).show()
        }
    }

    // Register launcher for gallery selection
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val imageUri = result.data?.data
            imageUri?.let { uri ->
                val file = copyImageToInternalStorage(uri)
                currentPhotoPath = file.absolutePath
                setPhotoPreview()

                android.util.Log.d("AddExpense", "Photo copied to: $currentPhotoPath")
                Toast.makeText(this, "Receipt photo attached", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddExpenseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getInstance(this)

        setupBackButton()
        setupDateAndTimePickers()
        setupPhotoUploadArea()
        setupSaveButton()
        loadCategories()
    }

    private fun setupBackButton() {
        binding.tvBack.setOnClickListener {
            finish()
        }
    }

    private fun setupDateAndTimePickers() {
        // Date picker
        binding.etDate.setOnClickListener {
            showDatePicker()
        }

        // Start time picker
        binding.etStartTime.setOnClickListener {
            showTimePicker(isStartTime = true)
        }

        // End time picker
        binding.etEndTime.setOnClickListener {
            showTimePicker(isStartTime = false)
        }

        // Set initial values
        updateDateDisplay()
        updateTimeDisplay()
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = selectedDateTimestamp

        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                selectedDateTimestamp = calendar.timeInMillis
                updateDateDisplay()
                android.util.Log.d("AddExpense", "Date selected: ${getDateString()}")
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker(isStartTime: Boolean) {
        val calendar = Calendar.getInstance()
        val currentTimestamp = if (isStartTime) selectedStartTimeTimestamp else selectedEndTimeTimestamp
        calendar.timeInMillis = currentTimestamp

        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                if (isStartTime) {
                    selectedStartTimeTimestamp = calendar.timeInMillis
                    updateStartTimeDisplay()
                } else {
                    selectedEndTimeTimestamp = calendar.timeInMillis
                    updateEndTimeDisplay()
                }
                android.util.Log.d("AddExpense", "${if (isStartTime) "Start" else "End"} time selected: ${getTimeString(calendar.timeInMillis)}")
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun updateDateDisplay() {
        binding.etDate.setText(getDateString())
    }

    private fun updateStartTimeDisplay() {
        binding.etStartTime.setText(getTimeString(selectedStartTimeTimestamp))
    }

    private fun updateEndTimeDisplay() {
        binding.etEndTime.setText(getTimeString(selectedEndTimeTimestamp))
    }

    private fun updateTimeDisplay() {
        updateStartTimeDisplay()
        updateEndTimeDisplay()
    }

    private fun getDateString(): String {
        val format = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return format.format(Date(selectedDateTimestamp))
    }

    private fun getTimeString(timestamp: Long): String {
        val format = SimpleDateFormat("h:mm a", Locale.getDefault())
        return format.format(Date(timestamp))
    }

    private fun setupPhotoUploadArea() {
        // Handle photo upload area click
        binding.photoUploadArea.setOnClickListener {
            showPhotoOptionsDialog()
        }

        // Handle take photo button
        binding.btnTakePhoto.setOnClickListener {
            dispatchTakePictureIntent()
        }

        // Handle choose from gallery button
        binding.btnChooseFromGallery.setOnClickListener {
            openGallery()
        }

        // Handle remove photo button
        binding.btnRemovePhoto.setOnClickListener {
            removePhoto()
        }
    }

    private fun showPhotoOptionsDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")
        AlertDialog.Builder(this)
            .setTitle("Add Receipt Photo")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> dispatchTakePictureIntent()
                    1 -> openGallery()
                }
            }
            .show()
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                val photoFile: File? = createImageFile()
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "${packageName}.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    takePictureLauncher.launch(takePictureIntent)
                }
            }
        }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(imageFileName, ".jpg", storageDir).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun copyImageToInternalStorage(uri: Uri): File {
        val inputStream = contentResolver.openInputStream(uri)
        val fileName = "IMG_${System.currentTimeMillis()}.jpg"
        val file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), fileName)

        inputStream?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        return file
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
        }
        galleryLauncher.launch(Intent.createChooser(intent, "Select Picture"))
    }

    private fun setPhotoPreview() {
        currentPhotoPath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                // Hide placeholder views
                binding.tvPhotoPlaceholderIcon.visibility = TextView.GONE
                binding.tvPhotoPlaceholderTitle.visibility = TextView.GONE
                binding.tvPhotoPlaceholderSub.visibility = TextView.GONE

                // Show preview
                binding.ivReceiptPreview.visibility = ImageView.VISIBLE
                binding.tvPhotoAttachedLabel.visibility = TextView.VISIBLE
                binding.btnRemovePhoto.visibility = Button.VISIBLE

                // Set image
                binding.ivReceiptPreview.setImageURI(Uri.fromFile(file))
            }
        }
    }

    private fun removePhoto() {
        currentPhotoPath = null

        // Show placeholder views
        binding.tvPhotoPlaceholderIcon.visibility = TextView.VISIBLE
        binding.tvPhotoPlaceholderTitle.visibility = TextView.VISIBLE
        binding.tvPhotoPlaceholderSub.visibility = TextView.VISIBLE

        // Hide preview and buttons
        binding.ivReceiptPreview.visibility = ImageView.GONE
        binding.tvPhotoAttachedLabel.visibility = TextView.GONE
        binding.btnRemovePhoto.visibility = Button.GONE

        // Clear image
        binding.ivReceiptPreview.setImageDrawable(null)

        Toast.makeText(this, "Photo removed", Toast.LENGTH_SHORT).show()
        android.util.Log.d("AddExpense", "Receipt photo removed")
    }

    private fun getRealPathFromURI(uri: Uri): String? {
        return if (uri.scheme == "file") {
            uri.path
        } else {
            // For content URIs, get the actual file path
            var filePath: String? = null
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                if (cursor.moveToFirst()) {
                    filePath = cursor.getString(columnIndex)
                }
            }
            filePath
        }
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            categories = db.categoryDao().getAll()
            setupCategorySpinner()
        }
    }

    private fun setupCategorySpinner() {
        val categoryNames = categories.map { "${it.iconEmoji} ${it.name}" }
        categoryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categoryNames)
        binding.actvCategory.setAdapter(categoryAdapter)

        binding.actvCategory.setOnItemClickListener { _, _, position, _ ->
            selectedCategoryId = categories[position].id
            android.util.Log.d("AddExpense", "Category selected: ${categories[position].name}")
        }
    }

    private fun setupSaveButton() {
        binding.btnSaveExpense.setOnClickListener {
            saveExpense()
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun saveExpense() {
        val description = binding.etDescription.text.toString().trim()
        val amountText = binding.etAmount.text.toString().trim()
        val categoryText = binding.actvCategory.text.toString()

        // Input validation
        when {
            description.isEmpty() -> {
                binding.etDescription.error = "Please enter a description"
                return
            }
            amountText.isEmpty() -> {
                binding.etAmount.error = "Please enter an amount"
                return
            }
            amountText.toDoubleOrNull() == null -> {
                binding.etAmount.error = "Please enter a valid amount"
                return
            }
            categoryText.isEmpty() || categoryText == "Category" -> {
                Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val amount = amountText.toDouble()

        // Combined date with start time for the expense timestamp
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = selectedDateTimestamp

        val startTimeCalendar = Calendar.getInstance()
        startTimeCalendar.timeInMillis = selectedStartTimeTimestamp

        calendar.set(Calendar.HOUR_OF_DAY, startTimeCalendar.get(Calendar.HOUR_OF_DAY))
        calendar.set(Calendar.MINUTE, startTimeCalendar.get(Calendar.MINUTE))

        val expenseDateTimeStamp = calendar.timeInMillis

        // Format times as strings for display
        val startTimeStr = getTimeString(selectedStartTimeTimestamp)
        val endTimeStr = getTimeString(selectedEndTimeTimestamp)

        val expense = Expense(
            categoryId = selectedCategoryId,
            description = description,
            amount = amount,
            date = expenseDateTimeStamp,
            startTime = startTimeStr,
            endTime = endTimeStr,
            receiptPhotoPath = currentPhotoPath
        )

        lifecycleScope.launch {
            try {
                db.expenseDao().insert(expense)

                // Award +5 Honey Points for adding an expense
                // Reference: GamificationManager.POINTS_ADD_EXPENSE
                val prefs  = getSharedPreferences("BudgetBeePrefs", MODE_PRIVATE)
                val userId = prefs.getInt("USER_ID", -1)
                if (userId != -1) {
                    val existing = db.honeyPointsDao().getPointsForUser(userId)
                    if (existing == null) {
                        db.honeyPointsDao().upsert(HoneyPoints(userId = userId, points = 5))
                    } else {
                        db.honeyPointsDao().addPoints(userId, GamificationManager.POINTS_ADD_EXPENSE)
                    }
                    Log.d("AddExpense", "Awarded ${GamificationManager.POINTS_ADD_EXPENSE} Honey Points to userId=$userId")
                }



                android.util.Log.d("AddExpense", "Expense saved: $description - R$amount")
                android.util.Log.d("AddExpense", "Photo attached: ${currentPhotoPath != null}")

                Toast.makeText(this@AddExpenseActivity, "Expense saved successfully!", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            } catch (e: Exception) {
                android.util.Log.e("AddExpense", "Error saving expense", e)
                Toast.makeText(this@AddExpenseActivity, "Error saving expense: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

/*

References:

GeeksforGeeks, 2022. Material Design Date Picker in Android using Kotlin.
Available at: https://www.geeksforgeeks.org/kotlin/material-design-date-picker-in-android-using-kotlin/
[Accessed 27 April 2026].

Android Developers, 2026. Use Kotlin coroutines with lifecycle-aware components.
Available at: https://developer.android.com/topic/libraries/architecture/coroutines
[Accessed 27 April 2026].

Android Ideas (Medium), 2018. findViewById in Kotlin.
Available at: https://medium.com/android-ideas/findviewbyid-in-kotlin-ce4d22193c79
[Accessed 27 April 2026].

 */

