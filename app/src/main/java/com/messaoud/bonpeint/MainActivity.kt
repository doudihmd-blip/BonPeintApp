package com.messaoud.bonpeint

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.view.Gravity
import android.widget.*
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    data class Item(val name: String, val quantity: String)

    private val items = ArrayList<Item>()
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
        val editPainterName = findViewById<EditText>(R.id.editPainterName)
        val editPainterPhone = findViewById<EditText>(R.id.editPainterPhone)
        val editProject = findViewById<EditText>(R.id.editProject)
        val editClient = findViewById<EditText>(R.id.editClient)
        val editCity = findViewById<EditText>(R.id.editCity)
        val editDate = findViewById<EditText>(R.id.editDate)
        val editItemName = findViewById<EditText>(R.id.editItemName)
        val editItemQty = findViewById<EditText>(R.id.editItemQty)
        val btnAdd = findViewById<Button>(R.id.btnAdd)
        val btnGenerate = findViewById<Button>(R.id.btnGenerate)
        val btnPrint = findViewById<Button>(R.id.btnPrint)
        val btnLang = findViewById<Button>(R.id.btnLang)
        val btnReset = findViewById<Button>(R.id.btnReset)
        val tableItems = findViewById<TableLayout>(R.id.tableItems)

        fun getDateStr(): String {
            val loc = if (isFrench) Locale.FRENCH else Locale("ar")
            return SimpleDateFormat("dd MMMM yyyy", loc).format(Date())
        }

        fun resetFields() {
            editDate.setText(getDateStr())
            editCity.setText(if (isFrench) "Hassi Messaoud" else "حاسي مسعود")
        }

        fun refreshSpinner() {
            val list = if (isFrench) docTypesFr else docTypesAr
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, list)
            spinnerDocType.adapter = adapter
            resetFields()
        }

        fun refreshTable() {
            tableItems.removeAllViews()
            if (items.isEmpty()) {
                val row = TableRow(this)
                val tvEmpty = TextView(this)
                tvEmpty.text = if (isFrench) "Aucun article." else "القائمة فارغة."
                tvEmpty.setTextColor(Color.GRAY)
                tvEmpty.setPadding(12, 12, 12, 12)
                row.addView(tvEmpty)
                tableItems.addView(row)
                return
            }

            val header = TableRow(this).apply {
                setBackgroundColor(Color.LTGRAY)
                gravity = Gravity.CENTER_VERTICAL
            }

            val h1 = TextView(this).apply {
                text = " N° "
                setTypeface(null, Typeface.BOLD)
                setPadding(6, 8, 6, 8)
                gravity = Gravity.CENTER
            }
            val h2 = TextView(this).apply {
                text = if (isFrench) " Désignation " else " المادة "
                setTypeface(null, Typeface.BOLD)
                setPadding(6, 8, 6, 8)
            }
            val h3 = TextView(this).apply {
                text = if (isFrench) " Qté " else " الكمية "
                setTypeface(null, Typeface.BOLD)
                setPadding(6, 8, 6, 8)
                gravity = Gravity.CENTER
            }
            val h4 = TextView(this).apply {
                text = " X "
                setTypeface(null, Typeface.BOLD)
                setPadding(6, 8, 6, 8)
                gravity = Gravity.CENTER
            }

            header.addView(h1)
            header.addView(h2)
            header.addView(h3)
            header.addView(h4)
            tableItems.addView(header)

            for (i in 0 until items.size) {
                val itm = items[i]
                val row = TableRow(this).apply {
                    setBackgroundColor(if (i % 2 == 0) Color.WHITE else Color.parseColor("#F8FAFC"))
                    gravity = Gravity.CENTER_VERTICAL
                }

                val t1 = TextView(this).apply {
                    text = "${i + 1}"
                    setPadding(6, 10, 6, 10)
                    gravity = Gravity.CENTER
                }
                val t2 = TextView(this).apply {
                    text = itm.name
                    setPadding(6, 10, 6, 10)
                    isSingleLine = false
                }
                val t3 = TextView(this).apply {
                    text = itm.quantity
                    setPadding(6, 10, 6, 10)
                    gravity = Gravity.CENTER
                }

                val bDel = Button(this).apply {
                    text = "X"
                    textSize = 12f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(Color.WHITE)
                    setBackgroundColor(Color.parseColor("#DC2626"))
                    layoutParams = TableRow.LayoutParams(85, 75).apply {
                        setMargins(4, 4, 4, 4)
                    }
                    setOnClickListener {
                        items.removeAt(i)
                        refreshTable()
                    }
                }

                row.addView(t1)
                row.addView(t2)
                row.addView(t3)
                row.addView(bDel)
                tableItems.addView(row)
            }
        }

        refreshSpinner()
        refreshTable()

        btnLang.setOnClickListener {
            isFrench = !isFrench
            btnLang.text = if (isFrench) "FR | AR" else "عربي | FR"
            refreshSpinner()
            refreshTable()
        }

        btnReset.setOnClickListener {
            items.clear()
            editProject.text.clear()
            editClient.text.clear()
            editItemName.text.clear()
            editItemQty.text.clear()
            resetFields()
            refreshTable()
            Toast.makeText(this, if (isFrench) "Nouveau bon!" else "بون جديد جاهز!", Toast.LENGTH_SHORT).show()
        }

        btnAdd.setOnClickListener {
            val name = editItemName.text.toString().trim()
            val qty = editItemQty.text.toString().trim()
            if (name.isNotEmpty() && qty.isNotEmpty()) {
                items.add(Item(name, qty))
                editItemName.text.clear()
                editItemQty.text.clear()
                refreshTable()
            } else {
                Toast.makeText(this, if (isFrench) "Champs vides" else "يرجى ملء الحقول", Toast.LENGTH_SHORT).show()
            }
        }

        fun createPdf(): File {
            val docIndex = spinnerDocType.selectedItemPosition
            val title = spinnerDocType.selectedItem.toString()
            val project = editProject.text.toString().trim().ifEmpty { "CHANTIER" }
            val client = editClient.text.toString().trim().ifEmpty { "" }
            val painter = editPainterName.text.toString().trim()
            val phone = editPainterPhone.text.toString().trim()
            val city = editCity.text.toString().trim().ifEmpty { "Hassi Messaoud" }
            val date = editDate.text.toString().trim().ifEmpty { getDateStr() }

            return writePdf(items, title, project, client, painter, phone, city, date, isFrench, docIndex)
        }

        btnGenerate.setOnClickListener {
            if (items.isEmpty()) {
                Toast.makeText(this, if (isFrench) "Liste vide!" else "القائمة فارغة!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val file = createPdf()
            val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Partager"))
        }

        btnPrint.setOnClickListener {
            if (items.isEmpty()) {
                Toast.makeText(this, if (isFrench) "Liste vide!" else "القائمة فارغة!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val file = createPdf()
            val pm = getSystemService(Context.PRINT_SERVICE) as PrintManager
            pm.print(
                "BonPeint_Print",
                object : PrintDocumentAdapter() {
                    override fun onLayout(
                        oldAttributes: PrintAttributes?,
                        newAttributes: PrintAttributes?,
                        cancellationSignal: CancellationSignal?,
                        callback: LayoutResultCallback?,
                        extras: Bundle?
                    ) {
                        if (cancellationSignal?.isCanceled == true) {
                            callback?.onLayoutCancelled()
                            return
                        }
                        val info = PrintDocumentInfo.Builder(file.name)
                            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                            .setPageCount(1)
                            .build()
                        callback?.onLayoutFinished(info, true)
                    }

                    override fun onWrite(
                        pages: Array<out PageRange>?,
                        destination: ParcelFileDescriptor?,
                        cancellationSignal: CancellationSignal?,
                        callback: WriteResultCallback?
                    ) {
                        try {
                            FileInputStream(file).use { input ->
                                FileOutputStream(destination?.fileDescriptor).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                        } catch (e: Exception) {
                            callback?.onWriteFailed(e.message)
                        }
                    }
                },
                PrintAttributes.Builder().build()
            )
        }
    }

    private fun writePdf(
        list: List<Item>,
        title: String,
        proj: String,
        client: String,
        painter: String,
        phone: String,
        city: String,
        dateStr: String,
        fr: Boolean,
        docIndex: Int
    ): File {
        val doc = PdfDocument()
        val info = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(info)
        val canvas = page.canvas

        val pTitle = Paint().apply {
            color = Color.BLACK
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val pSub = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val pText = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val pBold = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val pLine = Paint().apply {
            color = Color.GRAY
            strokeWidth = 0.8f
            style = Paint.Style.STROKE
        }
        val pFill = Paint().apply {
            color = Color.parseColor("#EAEAEA")
            style = Paint.Style.FILL
        }

        val cx = 595f / 2f
        val lx = 45f
        val rx = 550f

        canvas.drawText(title.uppercase(), cx, 65f, pTitle)
        canvas.drawText(proj.uppercase(), cx, 85f, pSub)

        // ترويسة الحرفي ورقم هاتفه والتاريخ
        val pRight = Paint(pText).apply { textAlign = Paint.Align.RIGHT }
        val dateLabel = if (fr) "$city, le $dateStr" else "$city، في $dateStr"
        canvas.drawText(dateLabel, rx, 115f, pRight)

        var currentY = 115f
        if (painter.isNotEmpty() || phone.isNotEmpty()) {
            val painterLabel = buildString {
                append(if (fr) "Peintre : " else "الحرفي (الدهان): ")
                append(painter)
                if (phone.isNotEmpty()) {
                    append(if (fr) "  |  Tél : " else "  |  هاتف: ")
                    append(phone)
                }
            }
            canvas.drawText(painterLabel, lx, currentY, pBold)
            currentY += 20f
        }

        if (client.isNotEmpty()) {
            val destLabel = if (fr) "Destinataire : $client" else "المستلم: $client"
            canvas.drawText(destLabel, lx, currentY, pBold)
            currentY += 20f
        }

        val isOrder = (docIndex == 1)

        val objText = if (isOrder) {
            if (fr) "Objet : Commande de fournitures de peinture pour chantier."
            else "الموضوع: طلب توريد مستلزمات دهان للورشة."
        } else {
            if (fr) "Objet : Récapitulatif des fournitures de peinture achetées pour les travaux."
            else "الموضوع: بيان مقتنيات ولوازم الدهان المشتراة لأشغال الورشة."
        }
        canvas.drawText(objText, lx, currentY, pText)
        currentY += 25f

        canvas.drawText(if (fr) "Monsieur," else "سيدي،", lx, currentY, pText)
        currentY += 18f

        val body1 = if (isOrder) {
            if (fr) "Nous vous prions de bien vouloir nous préparer les fournitures de peinture"
            else "يرجى التكرم بتجهيز وتوفير مستلزمات ولوازم الدهان الموضحة أدناه،"
        } else {
            if (fr) "Veuillez trouver ci-dessous la liste des fournitures de peinture que nous avons"
            else "تجدون أدناه قائمة بلوازم الدهان التي تم اقتناؤها ومطابقتها لاحتياجات ورشتكم،"
        }
        canvas.drawText(body1, lx, currentY, pText)
        currentY += 15f

        val body2 = if (isOrder) {
            if (fr) "mentionnées ci-dessous, nécessaires pour la continuité des travaux :"
            else "والمطلوبة لمواصلة واستمرار الأشغال في أفضل الظروف:"
        } else {
            if (fr) "achetées pour les travaux, conformément aux besoins du chantier."
            else "وفقاً لمتطلبات واستمرار العمل في أحسن الظروف."
        }
        canvas.drawText(body2, lx, currentY, pText)
        currentY += 22f

        var y = currentY
        val rh = 22f

        canvas.drawRect(lx, y, rx, y + rh, pFill)
        canvas.drawRect(lx, y, rx, y + rh, pLine)
        canvas.drawLine(85f, y, 85f, y + rh, pLine)
        canvas.drawLine(430f, y, 430f, y + rh, pLine)

        canvas.drawText("N°", 55f, y + 15f, pBold)
        canvas.drawText(if (fr) "Désignation" else "التعيين / المادة", 100f, y + 15f, pBold)
        canvas.drawText(if (fr) "Quantité" else "الكمية", 450f, y + 15f, pBold)

        y += rh
        for (i in 0 until list.size) {
            val itm = list[i]
            canvas.drawRect(lx, y, rx, y + rh, pLine)
            canvas.drawLine(85f, y, 85f, y + rh, pLine)
            canvas.drawLine(430f, y, 430f, y + rh, pLine)

            canvas.drawText("${i + 1}", 60f, y + 15f, pText)
            canvas.drawText(itm.name, 100f, y + 15f, pText)
            canvas.drawText(itm.quantity, 450f, y + 15f, pText)
            y += rh
        }

        y += 28f
        val note1 = if (isOrder) {
            if (fr) "Veuillez nous informer dès que la commande sera prête pour l'enlèvement."
            else "يرجى إعلامنا بمجرد أن تصبح الطلبية جاهزة للاستلام."
        } else {
            if (fr) "Nous vous informons que ces fournitures ont été achetées et sont désormais"
            else "نحيطكم علماً بأن هذه المواد متوفرة حالياً بالورشة لضمان استمرارية"
        }
        canvas.drawText(note1, lx, y, pText)

        if (!isOrder) {
            y += 15f
            val note2 = if (fr) "disponibles pour assurer la bonne continuité des travaux."
            else "الأشغال وحسن سيرها التام."
            canvas.drawText(note2, lx, y, pText)
        }

        y += 25f
        val closingName = if (client.isNotEmpty()) " $client" else ""
        canvas.drawText(
            if (fr) "Veuillez agréer, Monsieur$closingName, nos salutations respectueuses."
            else "وتفضلوا بقبول فائق التقدير والاحترام.",
            lx,
            y,
            pText
        )

        doc.finishPage(page)

        val fileName = "Bon_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".pdf"
        val file = File(getExternalFilesDir(null), fileName)
        doc.writeTo(FileOutputStream(file))
        doc.close()

        return file
    }
}
