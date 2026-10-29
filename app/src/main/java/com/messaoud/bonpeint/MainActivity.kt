package com.messaoud.bonpeint

import android.app.Activity
import android.app.AlertDialog
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
import android.widget.*
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    data class Item(val name: String, val quantity: String)

    private val items = ArrayList<Item>()
    private var isFrench = true
    private val currentVersionTag = "v1.0.4"

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

        checkAppUpdate()

        val spinnerDocType = findViewById<Spinner>(R.id.spinnerDocType)
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
                tvEmpty.setPadding(10, 10, 10, 10)
                row.addView(tvEmpty)
                tableItems.addView(row)
                return
            }

            val header = TableRow(this)
            header.setBackgroundColor(Color.LTGRAY)

            val h1 = TextView(this).apply { text = " N° "; setTypeface(null, Typeface.BOLD); setPadding(6, 6, 6, 6) }
            val h2 = TextView(this).apply { text = if (isFrench) " Désignation " else " المادة "; setTypeface(null, Typeface.BOLD); setPadding(6, 6, 6, 6) }
            val h3 = TextView(this).apply { text = if (isFrench) " Qté " else " الكمية "; setTypeface(null, Typeface.BOLD); setPadding(6, 6, 6, 6) }
            val h4 = TextView(this).apply { text = " X "; setTypeface(null, Typeface.BOLD); setPadding(6, 6, 6, 6) }

            header.addView(h1)
            header.addView(h2)
            header.addView(h3)
            header.addView(h4)
            tableItems.addView(header)

            for (i in 0 until items.size) {
                val itm = items[i]
                val row = TableRow(this)
                row.setBackgroundColor(Color.WHITE)

                val t1 = TextView(this).apply { text = " ${i + 1} "; setPadding(6, 6, 6, 6) }
                val t2 = TextView(this).apply { text = " ${itm.name} "; setPadding(6, 6, 6, 6) }
                val t3 = TextView(this).apply { text = " ${itm.quantity} "; setPadding(6, 6, 6, 6) }
                val bDel = Button(this).apply {
                    text = "X"
                    setTextColor(Color.RED)
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
            val project = editProject.text.toString().trim().ifEmpty { "TRAVAUX DE PEINTURE" }
            val client = editClient.text.toString().trim().ifEmpty { "Client" }
            val city = editCity.text.toString().trim().ifEmpty { "Hassi Messaoud" }
            val date = editDate.text.toString().trim().ifEmpty { getDateStr() }

            return writePdf(items, title, project, client, city, date, isFrench, docIndex)
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

    private fun checkAppUpdate() {
        Thread {
            try {
                val url = URL("https://api.github.com/repos/doudihmd-blip/BonPeintApp/releases/latest")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "BonPeintApp")
                conn.connectTimeout = 4000
                conn.readTimeout = 4000

                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val latestTag = json.optString("tag_name", "")

                    if (latestTag.isNotEmpty() && latestTag != currentVersionTag) {
                        var downloadUrl = ""
                        val assets = json.optJSONArray("assets")
                        if (assets != null) {
                            for (i in 0 until assets.length()) {
                                val asset = assets.getJSONObject(i)
                                if (asset.getString("name").endsWith(".apk")) {
                                    downloadUrl = asset.getString("browser_download_url")
                                    break
                                }
                            }
                        }

                        if (downloadUrl.isNotEmpty()) {
                            runOnUiThread {
                                showUpdateDialog(latestTag, downloadUrl)
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }.start()
    }

    private fun showUpdateDialog(newVersion: String, downloadUrl: String) {
        if (isFinishing) return
        val title = if (isFrench) "Mise à jour disponible" else "تحديث جديد متوفر"
        val msg = if (isFrench) {
            "Une nouvelle version ($newVersion) est disponible. Voulez-vous la télécharger maintenant ?"
        } else {
            "يتوفر إصدار جديد ($newVersion) لتطبيق Bon Peint. هل ترغب في التحميل الآن؟"
        }
        val btnOk = if (isFrench) "Mettre à jour" else "تحديث الآن"
        val btnCancel = if (isFrench) "Plus tard" else "لاحقاً"

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(msg)
            .setCancelable(true)
            .setPositiveButton(btnOk) { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                startActivity(intent)
            }
            .setNegativeButton(btnCancel, null)
            .show()
    }

    private fun writePdf(
        list: List<Item>,
        title: String,
        proj: String,
        client: String,
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
            textSize = 13f
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

        canvas.drawText(title.uppercase(), cx, 75f, pTitle)
        canvas.drawText(proj.uppercase(), cx, 100f, pSub)

        val pRight = Paint(pText).apply { textAlign = Paint.Align.RIGHT }
        val dateLabel = if (fr) "$city, le $dateStr" else "$city، في $dateStr"
        canvas.drawText(dateLabel, rx, 135f, pRight)

        val isOrder = (docIndex == 1)

        val destLabel = if (fr) "Destinataire : $client" else "المستلم: $client"
        canvas.drawText(destLabel, lx, 165f, pBold)

        val objText = if (isOrder) {
            if (fr) "Objet : Commande de fournitures de peinture pour chantier."
            else "الموضوع: طلب توريد مستلزمات دهان للورشة."
        } else {
            if (fr) "Objet : Récapitulatif des fournitures de peinture achetées pour les travaux."
            else "الموضوع: بيان مقتنيات ولوازم الدهان المشتراة لأشغال الورشة."
        }
        canvas.drawText(objText, lx, 185f, pText)

        canvas.drawText(if (fr) "Monsieur," else "سيدي،", lx, 215f, pText)

        val body1 = if (isOrder) {
            if (fr) "Nous vous prions de bien vouloir nous préparer les fournitures de peinture"
            else "يرجى التكرم بتجهيز وتوفير مستلزمات ولوازم الدهان الموضحة أدناه،"
        } else {
            if (fr) "Veuillez trouver ci-dessous la liste des fournitures de peinture que nous avons"
            else "تجدون أدناه قائمة بلوازم الدهان التي تم اقتناؤها ومطابقتها لاحتياجات ورشتكم،"
        }
        canvas.drawText(body1, lx, 235f, pText)

        val body2 = if (isOrder) {
            if (fr) "mentionnées ci-dessous, nécessaires pour la continuité des travaux :"
            else "والمطلوبة لمواصلة واستمرار الأشغال في أفضل الظروف:"
        } else {
            if (fr) "achetées pour les travaux de votre villa, conformément aux besoins du chantier."
            else "وفقاً لمتطلبات واستمرار العمل في أحسن الظروف."
        }
        canvas.drawText(body2, lx, 250f, pText)

        var y = 280f
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
            val note2 = if (fr) "disponibles pour assurer la bonne continuité des travaux de votre villa."
            else "الأشغال وحسن سيرها التام."
            canvas.drawText(note2, lx, y, pText)
        }

        y += 25f
        canvas.drawText(
            if (fr) "Veuillez agréer, Monsieur $client, nos salutations respectueuses."
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
