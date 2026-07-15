package com.trader.salesmanager.util.export

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * كاتب XLSX خفيف الوزن — لا يحتاج Apache POI.
 * يولّد ملف .xlsx بصيغة OpenXML مباشرةً.
 */
class XlsxWriter {

    data class CellStyle(
        val bold: Boolean = false,
        val bgColor: String? = null,       // ARGB hex مثل "FF10B981"
        val fontColor: String = "FF000000",
        val fontSize: Int = 10,
        val wrapText: Boolean = false,
        val hAlign: String = "right"       // left, center, right
    )

    private val rows = mutableListOf<List<Pair<Any?, CellStyle>>>()
    private val colWidths = mutableMapOf<Int, Double>()
    private val styles = mutableListOf<CellStyle>()
    private val styleIndex = mutableMapOf<CellStyle, Int>()
    private val sharedStrings = mutableListOf<String>()
    private val ssIndex = mutableMapOf<String, Int>()

    // ── API عام ────────────────────────────────────────────────

    fun addRow(vararg cells: Pair<Any?, CellStyle>) {
        rows.add(cells.toList())
        cells.forEachIndexed { col, (value, style) ->
            getStyleIndex(style) // register style
            val strVal = value?.toString() ?: ""
            if (value is String && strVal.isNotEmpty()) {
                colWidths[col] = maxOf(colWidths[col] ?: 8.0, strVal.length * 1.3)
            }
        }
    }

    fun addEmptyRow() = addRow()

    fun setColWidth(col: Int, width: Double) { colWidths[col] = width }

    fun write(file: File) {
        ZipOutputStream(FileOutputStream(file)).use { zip ->
            zip.putNextEntry(ZipEntry("[Content_Types].xml")); zip.write(contentTypes().toByteArray(Charsets.UTF_8)); zip.closeEntry()
            zip.putNextEntry(ZipEntry("_rels/.rels"));         zip.write(rels().toByteArray(Charsets.UTF_8));        zip.closeEntry()
            zip.putNextEntry(ZipEntry("xl/workbook.xml"));     zip.write(workbook().toByteArray(Charsets.UTF_8));    zip.closeEntry()
            zip.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels")); zip.write(workbookRels().toByteArray(Charsets.UTF_8)); zip.closeEntry()
            zip.putNextEntry(ZipEntry("xl/sharedStrings.xml")); zip.write(sharedStringsXml().toByteArray(Charsets.UTF_8)); zip.closeEntry()
            zip.putNextEntry(ZipEntry("xl/styles.xml"));       zip.write(stylesXml().toByteArray(Charsets.UTF_8));  zip.closeEntry()
            zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml")); zip.write(sheetXml().toByteArray(Charsets.UTF_8)); zip.closeEntry()
        }
    }

    // ── Style helpers ─────────────────────────────────────────

    private fun getStyleIndex(style: CellStyle): Int {
        return styleIndex.getOrPut(style) {
            val idx = styles.size
            styles.add(style)
            idx
        }
    }

    private fun getStringIndex(s: String): Int {
        return ssIndex.getOrPut(s) {
            val idx = sharedStrings.size
            sharedStrings.add(s)
            idx
        }
    }

    // ── XML generators ────────────────────────────────────────

    private fun contentTypes() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml"  ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""

    private fun rels() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private fun workbook() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
  xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <bookViews><workbookView xWindow="0" yWindow="0" windowWidth="16384" windowHeight="8192"/></bookViews>
  <sheets><sheet name="Sheet1" sheetId="1" r:id="rId1"/></sheets>
</workbook>"""

    private fun workbookRels() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

    private fun sharedStringsXml(): String {
        val sb = StringBuilder("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="${sharedStrings.size}" uniqueCount="${sharedStrings.size}">""")
        sharedStrings.forEach { s ->
            sb.append("<si><t xml:space=\"preserve\">${s.xmlEscape()}</t></si>")
        }
        sb.append("</sst>")
        return sb.toString()
    }

    private fun stylesXml(): String {
        // Build fills
        val fills = StringBuilder("""<fills count="${styles.count { it.bgColor != null } + 2}">
  <fill><patternFill patternType="none"/></fill>
  <fill><patternFill patternType="gray125"/></fill>""")
        val fillIdMap = mutableMapOf<String?, Int>()
        fillIdMap[null] = 0
        styles.forEach { style ->
            if (style.bgColor != null && !fillIdMap.containsKey(style.bgColor)) {
                val id = fillIdMap.size
                fillIdMap[style.bgColor] = id
                fills.append("""<fill><patternFill patternType="solid"><fgColor rgb="${style.bgColor}"/></patternFill></fill>""")
            }
        }
        fills.append("</fills>")

        // Build fonts
        val fonts = StringBuilder("""<fonts count="${styles.size + 1}">
  <font><sz val="10"/><name val="Arial"/></font>""")
        val fontIdMap = mutableMapOf<CellStyle, Int>()
        styles.forEach { style ->
            val id = fontIdMap.size + 1
            fontIdMap[style] = id
            val bold = if (style.bold) "<b/>" else ""
            fonts.append("""<font>$bold<sz val="${style.fontSize}"/><name val="Arial"/><color rgb="${style.fontColor}"/></font>""")
        }
        fonts.append("</fonts>")

        // Build xfs
        val xfs = StringBuilder("""<cellXfs count="${styles.size + 1}">
  <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>""")
        styles.forEachIndexed { i, style ->
            val fontId = fontIdMap[style] ?: 0
            val fillId = fillIdMap[style.bgColor] ?: 0
            val wrap = if (style.wrapText) "<alignment wrapText=\"1\" readingOrder=\"2\" horizontal=\"${style.hAlign}\"/>" else "<alignment readingOrder=\"2\" horizontal=\"${style.hAlign}\"/>"
            xfs.append("""<xf numFmtId="0" fontId="$fontId" fillId="$fillId" borderId="0" xfId="0" applyFont="1" applyFill="1" applyAlignment="1">$wrap</xf>""")
        }
        xfs.append("</cellXfs>")

        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  $fonts
  $fills
  <borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
  $xfs
</styleSheet>"""
    }

    private fun sheetXml(): String {
        val sb = StringBuilder("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")

        // Column widths
        if (colWidths.isNotEmpty()) {
            sb.append("<cols>")
            colWidths.forEach { (col, width) ->
                val c = col + 1
                sb.append("""<col min="$c" max="$c" width="${width.coerceIn(8.0, 50.0)}" customWidth="1"/>""")
            }
            sb.append("</cols>")
        }

        sb.append("<sheetData>")
        rows.forEachIndexed { rowIdx, cells ->
            if (cells.isEmpty()) { sb.append("<row r=\"${rowIdx + 1}\"/>"); return@forEachIndexed }
            val ht = if (cells.any { it.second.wrapText }) " ht=\"30\" customHeight=\"1\"" else " ht=\"18\" customHeight=\"1\""
            sb.append("<row r=\"${rowIdx + 1}\"$ht>")
            cells.forEachIndexed { colIdx, (value, style) ->
                val col = colLetter(colIdx)
                val ref = "$col${rowIdx + 1}"
                val sIdx = (getStyleIndex(style) + 1)  // +1 because xfs[0] is default
                when {
                    value == null -> sb.append("<c r=\"$ref\" s=\"$sIdx\"/>")
                    value is Number -> sb.append("<c r=\"$ref\" s=\"$sIdx\" t=\"n\"><v>${value}</v></c>")
                    else -> {
                        val ssIdx = getStringIndex(value.toString())
                        sb.append("<c r=\"$ref\" s=\"$sIdx\" t=\"s\"><v>$ssIdx</v></c>")
                    }
                }
            }
            sb.append("</row>")
        }
        sb.append("</sheetData></worksheet>")
        return sb.toString()
    }

    private fun colLetter(col: Int): String {
        val letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        return if (col < 26) letters[col].toString()
        else letters[col / 26 - 1].toString() + letters[col % 26].toString()
    }

    private fun String.xmlEscape() = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
}
