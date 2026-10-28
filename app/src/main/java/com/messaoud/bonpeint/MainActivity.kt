package com.messaoud.bonpeint

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

data class Item(val name: String, val quantity: String)

class MainActivity : Activity() {

    private val items = mutableListOf<Item>()
    private var isFrench = true

    private val docTypesFr = arrayOf(
        "ÉTAT DES FOURNITURES ACHETÉES",
        "BON DE COMMANDE",
        "BON DE LIVRAISON",
        "BON D’ACHAT"
    )

    private val docTypesAr = arrayOf(
        "كشف المواد المشتراة",
        "طلب شراء (سلعة)",
        "وصل تسليم السلعة",
        "وصل شراء"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val spinnerDocType = findViewById<Spinner>(R.id.spinnerDocType)
        val editProject = findViewById<EditText>(R.id.editProject)
        val editClient = findViewById<EditText>(R.id.editClient)
        val editCity = findViewById<EditText>(R.id.editCity)
        val editItemName = findViewById<EditText>(R.id.editItemName)
        val editItemQty = findViewById<EditText>(R.id.editItemQty)
        val btnAdd = findViewById<Button>(R.id.btnAdd)
        val btnGenerate = findViewById<Button>(R.id.btnGenerate)
        val btnLang = findViewById<Button>(R.id.btnLang)
        val tableItems = findViewById<TableLayout>(R.id.tableItems)

        fun refreshSpinner() {
            val list = if (isFrench) docTypesFr else docTypesAr
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, list)
            spinnerDocType.adapter = adapter
            editCity.setText(if (isFrench) "Hassi Messaoud" else "حاسي مسعود")
        }

        fun renderTable() {
            tableItems.removeAllViews()
            if (items.isEmpty()) {
                val row = TableRow(this)
                val tvEmpty = TextView(this).apply {
                    text = if (isFrench) "Aucun article ajouté." else "لم يتم إضافة أي مادة بعد."
                    setTextColor(Color.GRAY)
                    setPadding(10, 10, 10, 10)

