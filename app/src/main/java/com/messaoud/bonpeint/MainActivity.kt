package com.messaoud.bonpeint

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
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
                }
                row.addView(tvEmpty)
                tableItems.addView(row)
                return
            }

            // Header Row
            val headerRow = TableRow(this).apply { setBackgroundColor(Color.parseColor("#E2E8F0")) }
            val thNum = TextView(this).apply { text = "N°"; setPadding(8, 8, 8, 8); textStyle = Typeface.BOLD }
            val thName = TextView(this).apply { text = if (isFrench) "Désignation" else "التعيين"; setPadding(8, 8, 8, 8); textStyle = Typeface.BOLD }
            val thQty = TextView(this).apply { text = if (isFrench) "Qté" else "الكمية"; setPadding(8, 8, 8, 8); textStyle = Typeface.BOLD }
            val thDel = TextView(this).apply { text = "Action"; setPadding(8, 8, 8, 8); textStyle = Typeface.BOLD }
            headerRow.addView(thNum); headerRow.addView(thName); headerRow.addView(thQty); headerRow.addView(thDel)
            tableItems.addView(headerRow)

            // Items Rows
            items.forEachIndexed { idx, item ->
                val row = TableRow(this).apply {
                    setBackgroundColor(if (idx % 2 == 0) Color.WHITE else Color.parseColor("#F8FAFC"))
                }
                val tvNum = TextView(this).apply { text = "${idx + 1}"; setPadding(8, 8, 8, 8) }
                val tvName = TextView(this).apply { text = item.name; setPadding(8, 8, 8, 8) }
                val tvQty = TextView(this).apply { text = item.quantity; setPadding(8, 8, 8, 8) }
                val btnDel = Button(this).apply {
                    text = "X"
                    setTextColor(Color.RED)
                    setBackgroundColor(Color.TRANSPARENT)
                    setOnClickListener {
                        items.removeAt(idx)
                        renderTable()
                    }
                }
                row.addView(tvNum); row.addView(tvName); row.addView(tvQty); row.addView(btnDel)
                tableItems.addView(row)
            }
        }

        refreshSpinner()
        renderTable()

        btnLang.setOnClickListener {
            isFrench = !isFrench
            btnLang.text = if (isFrench) "FR | AR" else "عربي | FR"
            refreshSpinner()
            renderTable()
        }

        btnAdd.setOnClickListener {
            val name = editItemName.text.toString().trim()
            val qty = editItemQty.text.toString().trim()
            if (name.isNotEmpty() && qty.isNotEmpty()) {
                items.add(Item(name, qty))
                editItemName.text.clear()
                editItemQty.text.clear()
                renderTable()
            } else {
                Toast.makeText(this, if (isFrench) "Veuillez remplir les deux champs" else "يرجى ملء الحقلين", Toast.LENGTH_SHORT).show()
            }
        }

        btnGenerate.setOnClickListener {
            if (items.isEmpty()) {
                Toast.makeText(this, if (isFrench) "Ajoutez au moins un article!" else "أضف مادة واحدة على الأقل!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val title = spinnerDocType.selectedItem.toString()
            val project = editProject.text.toString().trim().ifEmpty { "VILLA DE MONSIEUR RAFIK AZZOUZ" }
            val client = editClient.text.toString().trim().ifEmpty { "Monsieur Rafik Azzouz" }
            val city = editCity.text.toString().trim().ifEmpty { "Hassi Messaoud" }

            generatePdfDocument(title, project, client, city, items, isFrench)
        }
    }

    private fun generatePdfDocument(
        title: String,
        project: String,
        client: String,
        city: String,
        list: List<Item>,
        isFr: Boolean
    ) {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Format A4
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        val paintTitle = Paint().apply {
            color = Color.BLACK
            textSize = 17f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val paintSubTitle = Paint().apply {
            color = Color.BLACK
            textSize = 13.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val paintText = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val paintBold = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val paintLine = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 0.8f
            style = Paint.Style.STROKE
        }
        val paintHeaderFill = Paint().apply {
            color = Color.parseColor("#EEEEEE")
            style = Paint.Style.FILL
        }

        val centerX = 595f / 2f
        val leftX = 45f
        val rightX = 550f

        // 1. العناوين العريضة في الوسط طبق الأصل
        canvas.drawText(title.uppercase(), centerX, 80f, paintTitle)
        canvas.drawText(project.uppercase(), centerX, 105f, paintSubTitle)

        // 2. التاريخ مع المدينة على اليمين
        val dateStr = SimpleDateFormat("dd MMMM yyyy", if (isFr) Locale.FRENCH else Locale("ar")).format(Date())
        val dateFull = "$city, le $dateStr"
        val paintRight = Paint(paintText).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText(dateFull, rightX, 140f, paintRight)

        // 3. بيانات المستلم والموضوع
        canvas.drawText(if (isFr) "Destinataire : $client" else "المستلم: $client", leftX, 175f, paintBold)
        canvas.drawText(
            if (isFr) "Objet : Récapitulatif des fournitures de peinture achetées pour les travaux de la villa."
            else "الموضوع: بيان مقتنيات ولوازم الدهان المشتراة لأشغال الورشة.",
            leftX,
            195f,
            paintText
        )

        // 4. ديباجة الخطاب
        canvas.drawText(if (isFr) "Monsieur," else "سيدي،", leftX, 225f, paintText)
        canvas.drawText(
            if (isFr) "Veuillez trouver ci-dessous la liste des fournitures de peinture que nous avons"
            else "تجدون أدناه قائمة بلوازم الدهان التي تم اقتناؤها ومطابقتها لاحتياجات ورشتكم،",
            leftX,
            245f,
            paintText
        )
        canvas.drawText(
            if (isFr) "achetées pour les travaux de votre villa, conformément aux besoins du chantier."
            else "وفقاً لمتطلبات واستمرار العمل في أحسن الظروف.",
            leftX,
            260f,
            paintText
        )

        // 5. رسم الجدول طبق الأصل
        var y = 290f
        val rowHeight = 22f

        // رأس الجدول
        canvas.drawRect(leftX, y, rightX, y + rowHeight, paintHeaderFill)
        canvas.drawRect(leftX, y, rightX, y + rowHeight, paintLine)
        canvas.drawLine(85f, y, 85f, y + rowHeight, paintLine)
        canvas.drawLine(430f, y, 430f, y + rowHeight, paintLine)

        val headerY = y + 15f
        canvas.drawText("N°", 55f, headerY, paintBold)
        canvas.drawText(if (isFr) "Désignation" else "التعيين / المادة", 100f, headerY, paintBold)
        canvas.drawText(if (isFr) "Quantité" else "الكمية", 450f, headerY, paintBold)

        y += rowHeight

        // أسطر المواد
        list.forEachIndexed { i, itm ->
            canvas.drawRect(leftX, y, rightX, y + rowHeight, paintLine)
            canvas.drawLine(85f, y, 85f, y + rowHeight, paintLine)
            canvas.drawLine(430f, y, 430f, y + rowHeight, paintLine)

            val textY = y + 15f
            canvas.drawText("${i + 1}", 60f, textY, paintText)
            canvas.drawText(itm.name, 100f, textY, paintText)
            canvas.drawText(itm.quantity, 450f, textY, paintText)
            y += rowHeight
        }

        // 6. نص التوفر والخاتمة تحت الجدول
        y += 30f
        canvas.drawText(
            if (isFr) "Nous vous informons que ces fournitures ont été achetées et sont désormais"
            else "نحيطكم علماً بأن هذه المواد متوفرة حالياً بالورشة لضمان استمرارية",
            leftX,
            y,
            paintText
        )
        y += 15f
        canvas.drawText(
            if (isFr) "disponibles pour assurer la bonne continuité des travaux de votre villa."
            else "الأشغال وحسن سيرها التام.",
            leftX,
            y,
            paintText
        )

        y += 25f
        canvas.drawText(
            if (isFr) "Veuillez agréer, Monsieur $client, nos salutations respectueuses."
            else "وتفضلوا بقبول فائق التقدير والاحترام.",
            leftX,
            y,
            paintText
        )

        doc.finishPage(page)

        // حفظ ومشاركة المستند
        val file = File(getExternalFilesDir(null), "Etat_Fournitures.pdf")
        doc.writeTo(FileOutputStream(file))
        doc.close()

        val uri: Uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Partager le Bon"))
    }
}
