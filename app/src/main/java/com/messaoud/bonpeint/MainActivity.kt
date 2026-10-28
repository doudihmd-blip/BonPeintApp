package com.messaoud.bonpeint

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.core.content.FileProvider
import java.io.File
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
        try {
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

            // بيانات افتراضية أولية لتجربة التطبيق فور فتحه
            if (items.isEmpty()) {
                items.add(Item("Papier abrasif girafe - grain 400", "10 pièces"))
                items.add(Item("Papier abrasif en rouleau - grain 280", "10 pièces"))
                items.add(Item("Papier abrasif en rouleau - grain 400", "6 pièces"))
                items.add(Item("Fixateur Vinxya - bidon de 18 kg", "2 bidons"))
                items.add(Item("Silicone acrylique blanc", "2 unités"))
                items.add(Item("Spécial plafond - bidon de 10 kg", "2 bidons"))
            }

            fun updateDocTypes() {
                val list = if (isFrench) docTypesFr else docTypesAr
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, list)
                spinnerDocType.adapter = adapter
                if (editCity.text.isNullOrEmpty()) {
                    editCity.setText(if (isFrench) "Hassi Messaoud" else "حاسي مسعود")
                }
            }

            fun updateItemsTable() {
                tableItems.removeAllViews()

                // Header
                val headerRow = TableRow(this)
                headerRow.setBackgroundColor(Color.LTGRAY)
                val h1 = TextView(this).apply { text = " N° "; setTypeface(null, Typeface.BOLD); setPadding(6,6,6,6) }
                val h2 = TextView(this).apply { text = if (isFrench) " Désignation " else " المادة "; setTypeface(null, Typeface.BOLD); setPadding(6,6,6,6) }
                val h3 = TextView(this).apply { text = if (isFrench) " Qté " else " الكمية "; setTypeface(null, Typeface.BOLD); setPadding(6,6,6,6) }
                val h4 = TextView(this).apply { text = " X "; setTypeface(null, Typeface.BOLD); setPadding(6,6,6,6) }
                headerRow.addView(h1); headerRow.addView(h2); headerRow.addView(h3); headerRow.addView(h4)
                tableItems.addView(headerRow)

                for (i in 0 until items.size) {
                    val itm = items[i]
                    val row = TableRow(this)
                    row.setBackgroundColor(if (i % 2 == 0) Color.WHITE else Color.parseColor("#F1F5F9"))

                    val t1 = TextView(this).apply { text = " ${i + 1} "; setPadding(6,6,6,6) }
                    val t2 = TextView(this).apply { text = " ${itm.name} "; setPadding(6,6,6,6) }
                    val t3 = TextView(this).apply { text = " ${itm.quantity} "; setPadding(6,6,6,6) }
                    val bDel = Button(this).apply {
                        text = "حذف"
                        textSize = 10f
                        setTextColor(Color.RED)
                        setOnClickListener {
                            items.removeAt(i)
                            updateItemsTable()
                        }
                    }
                    row.addView(t1); row.addView(t2); row.addView(t3); row.addView(bDel)
                    tableItems.addView(row)
                }
            }

            updateDocTypes()
            updateItemsTable()

            btnLang.setOnClickListener {
                isFrench = !isFrench
                btnLang.text = if (isFrench) "FR | AR" else "عربي | FR"
                updateDocTypes()
                updateItemsTable()
            }

            btnAdd.setOnClickListener {
                val n = editItemName.text.toString().trim()
                val q = editItemQty.text.toString().trim()
                if (n.isNotEmpty() && q.isNotEmpty()) {
                    items.add(Item(n, q))
                    editItemName.text.clear()
                    editItemQty.text.clear()
                    updateItemsTable()
                } else {
                    Toast.makeText(this, "يرجى كتابة المادة والكمية", Toast.LENGTH_SHORT).show()
                }
            }

            btnGenerate.setOnClickListener {
                if (items.isEmpty()) {
                    Toast.makeText(this, "القائمة فارغة!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val t = spinnerDocType.selectedItem?.toString() ?: docTypesFr[0]
                val p = editProject.text.toString().trim().ifEmpty { "VILLA DE MONSIEUR RAFIK AZZOUZ" }
                val c = editClient.text.toString().trim().ifEmpty { "Monsieur Rafik Azzouz" }
                val city = editCity.text.toString().trim().ifEmpty { "Hassi Messaoud" }

                createPdf(t, p, c, city, items, isFrench)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "خطأ: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun createPdf(title: String, proj: String, client: String, city: String, list: List<Item>, fr: Boolean) {
        try {
            val doc = PdfDocument()
            val info = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = doc.startPage(info)
            val canvas = page.canvas

            val pTitle = Paint().apply { color = Color.BLACK; textSize = 16f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.CENTER }
            val pSub = Paint().apply { color = Color.BLACK; textSize = 13f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.CENTER }
            val pText = Paint().apply { color = Color.BLACK; textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL) }
            val pBold = Paint().apply { color = Color.BLACK; textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
            val pLine = Paint().apply { color = Color.GRAY; strokeWidth = 0.8f; style = Paint.Style.STROKE }
            val pFill = Paint().apply { color = Color.parseColor("#EAEAEA"); style = Paint.Style.FILL }

            val cx = 595f / 2f
            val lx = 45f
            val rx = 550f

            canvas.drawText(title.uppercase(), cx, 75f, pTitle)
            canvas.drawText(proj.uppercase(), cx, 100f, pSub)

            val df = SimpleDateFormat("dd MMMM yyyy", if (fr) Locale.FRENCH else Locale("ar")).format(Date())
            val pRight = Paint(pText).apply { textAlign = Paint.Align.RIGHT }
            canvas.drawText("$city, le $df", rx, 135f, pRight)

            canvas.drawText(if (fr) "Destinataire : $client" else "المستلم: $client", lx, 165f, pBold)
            canvas.drawText(if (fr) "Objet : Récapitulatif des fournitures de peinture achetées pour les travaux de la villa." else "الموضوع: بيان مقتنيات ولوازم الدهان المشتراة لأشغال الورشة.", lx, 185f, pText)

            canvas.drawText(if (fr) "Monsieur," else "سيدي،", lx, 215f, pText)
            canvas.drawText(if (fr) "Veuillez trouver ci-dessous la liste des fournitures de peinture que nous avons" else "تجدون أدناه قائمة بلوازم الدهان التي تم اقتناؤها ومطابقتها لاحتياجات ورشتكم،", lx, 235f, pText)
            canvas.drawText(if (fr) "achetées pour les travaux de votre villa, conformément aux besoins du chantier." else "وفقاً لمتطلبات واستمرار العمل في أحسن الظروف.", lx, 250f, pText)

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
            canvas.drawText(if (fr) "Nous vous informons que ces fournitures ont été achetées et sont désormais" else "نحيطكم علماً بأن هذه المواد متوفرة حالياً بالورشة لضمان استمرارية", lx, y, pText)
            y += 15f
            canvas.drawText(if (fr) "disponibles pour assurer la bonne continuité des travaux de votre villa." else "الأشغال وحسن سيرها التام.", lx, y, pText)
            y += 25f
            canvas.drawText(if (fr) "Veuillez agréer, Monsieur $client, nos salutations respectueuses." else "وتفضلوا بقبول فائق التقدير والاحترام.", lx, y, pText)

            doc.finishPage(page)

            val f = File(getExternalFilesDir(null), "Etat_Fournitures.pdf")
            doc.writeTo(FileOutputStream(f))
            doc.close()

            val u: Uri = FileProvider.getUriForFile(this, "$packageName.provider", f)
            val sIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, u)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(sIntent, "Partager le Bon"))
        } catch (e: Exception) {
            Toast.makeText(this, "تعذر استخراج PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
