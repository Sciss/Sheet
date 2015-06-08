/*
 *  Workbook.scala
 *  (poi-scala)
 *
 *  Copyright (c) 2011-2015 George Leontiev and others. Fork by Hanns Holger Rutz.
 *
 *	This software is published under the Apache License 2.0
 */

package de.sciss.poi

import java.io.{File, FileOutputStream, InputStream, OutputStream}

import org.apache.poi.ss.usermodel.{Cell => POICell, CellStyle => POICellStyle, Row => POIRow, Workbook => POIWorkbook, WorkbookFactory}

class Workbook(val sheetMap: Map[String, Sheet], format: WorkbookVersion = HSSF) {
  val sheets: Set[Sheet] = sheetMap.values.toSet

  private def setPoiCell(row: POIRow, cell: Cell, poiCell: POICell): Unit = {
    cell match {
      case StringCell(index, data)  =>
        poiCell.setCellValue(data)
        val height = data.split("\n").size * row.getHeight
        row setHeight height.asInstanceOf[Short]
      case BooleanCell(index, data) => poiCell.setCellValue(data)
      case NumericCell(index, data) => poiCell.setCellValue(data)
      case FormulaCell(index, data) => poiCell.setCellFormula(data)
      case styledCell @ StyledCell(_, _) =>
        setPoiCell(row, styledCell.nestedCell, poiCell)
    }
  }

  private lazy val book = {
    val workbook = format match {
      case HSSF => new org.apache.poi.hssf.usermodel.HSSFWorkbook
      case XSSF => new org.apache.poi.xssf.usermodel.XSSFWorkbook
    }
    sheets foreach { sh =>
      val Sheet((name), (rows)) = sh
      val sheet = workbook createSheet name
      rows foreach { rw =>
        val Row((index), (cells)) = rw
        val row = sheet createRow index
        cells foreach { cl =>
          val poiCell = row createCell cl.index
          setPoiCell(row, cl, poiCell)
        }
      }
    }
    workbook
  }

  private def applyStyling(wb: POIWorkbook, styles: Map[CellStyle, List[CellAddr]]) = {
    def pStyle(cs: CellStyle): POICellStyle = {
      val pStyle = wb.createCellStyle()
      pStyle setFont cs.font.appliedTo(wb.createFont)
      pStyle setDataFormat cs.dataFormat.appliedTo(wb.createDataFormat)
      pStyle
    }

    styles.keys.foreach { s =>
      val cellAddresses = styles(s)
      val cellStyle = pStyle(s)
      cellAddresses.foreach { addr =>
        val cell = wb.getSheet(addr.sheet).getRow(addr.row).getCell(addr.col)
        cell setCellStyle cellStyle
      }
    }
    wb
  }

  def styled(styles: Map[CellStyle, List[CellAddr]]): Workbook = {
    applyStyling(book, styles)
    this
  }

  def styled: Workbook = {
    val styles: Map[CellStyle, List[CellAddr]] = sheets.foldRight(Map.empty[CellStyle, List[CellAddr]]) {
      case (sheet, map) => (map /: sheet.styles) { case (m, (k, v)) =>  m + (k -> (m.getOrElse(k, Nil) ++ v)) }
    }
    styled(styles)
  }

  /** Fits column's width to maximum width of non-empty cell at cell address.
    * Quite expensive. Use as late as possible.
    *
    * @param addrs addresses of cells, which columns size should fit cells content
    */
  def autosizeColumns(addrs: Set[CellAddr]): Workbook = {
    addrs foreach { a => book.getSheet(a.sheet).autoSizeColumn(a.col) }
    this
  }

  def saveToFile(path: String): Unit = {
    val fos = new FileOutputStream(new File(path))
    try {
      saveToStream(fos)
    } finally {
      fos.close()
    }
  }

  def saveToStream(stream: OutputStream): Unit = book.write(stream)

  def asPoi: POIWorkbook = book

  override def toString: String = "Workbook(" + this.sheets.toIndexedSeq.sortBy(_.name) + ")"
  
  override def equals(obj: Any): Boolean = obj match {
    case a2: Workbook =>
      val a1 = this
      (a1.sheets.toIndexedSeq.sortBy((x: Sheet) => x.name) zip
        a2.sheets.toIndexedSeq.sortBy((x: Sheet) => x.name)).forall(v => v._1 == v._2)
    case _ => false
  }

  override def hashCode: Int = this.sheetMap.hashCode()
}

object Workbook {

  def apply(sheets: Set[Sheet], format: WorkbookVersion = HSSF): Workbook =
    new Workbook(sheets.map( s => (s.name, s)).toMap, format)

  def fromFile(path: String, format: WorkbookVersion = HSSF): Workbook = {
    val f = new File(path)
    readWorkbook[File](format, f => WorkbookFactory.create(f))(f)
  }

  def fromInputStream(is: InputStream, format: WorkbookVersion = HSSF): Workbook =
    readWorkbook[InputStream](format, t => WorkbookFactory.create(t))(is)

  private def readWorkbook[T](format: WorkbookVersion, workbookF: T => POIWorkbook)(is: T): Workbook = {
    val wb   = workbookF(is)
    val data = for {
      i     ← 0 until wb.getNumberOfSheets
      sheet = wb.getSheetAt(i) if sheet != null
      k     ← 0 to sheet.getLastRowNum
      row   = sheet.getRow(k) if row != null
      j     ← 0 until row.getLastCellNum
      cell  = row.getCell(j) if cell != null
        } yield (sheet, row, cell)
    val result = data.groupBy(_._1).mapValues { lst =>
      lst.map { case (s, r, c) => (r, c)}.groupBy(_._1)
        .mapValues(l => l.map { case (r, c) => c }.toList)
    }
    val sheets = result.map { case (sheet, rowLst) =>
      Sheet(sheet.getSheetName) {
        rowLst.map { case (row, cellLst) =>
          Row(row.getRowNum) {
            cellLst.flatMap { cell =>
              val index = cell.getColumnIndex
              cell.getCellType match {
                case POICell.CELL_TYPE_NUMERIC =>
                  Some(NumericCell(index, cell.getNumericCellValue))
                case POICell.CELL_TYPE_BOOLEAN =>
                  Some(BooleanCell(index, cell.getBooleanCellValue))
                case POICell.CELL_TYPE_FORMULA =>
                  Some(FormulaCell(index, cell.getCellFormula))
                case POICell.CELL_TYPE_STRING  =>
                  Some(StringCell(index, cell.getStringCellValue))
                case _                      => None
              }
            }.toSet
          }
        }.toSet
      }
    }.toSet
    Workbook(sheets)
  }
}

class Sheet(val name: String)(val rows: Set[Row]) {
  def styles: Map[CellStyle, List[CellAddr]] =
    rows.foldRight(Map.empty[CellStyle, List[CellAddr]]) {
      case (row, map) => (map /: row.styles(name)) { case (m, (k, v)) =>  m + (k -> (m.getOrElse(k, Nil) ++ v)) }
    }

  override def toString: String = "Sheet (\"" + this.name + "\")(" + this.rows.toIndexedSeq.sortBy(_.index) + ")"

  override def equals(obj: Any): Boolean = obj match {
    case a2: Sheet =>
      val a1 = this
      a1.name == a2.name &&
        (a1.rows.toIndexedSeq.sortBy((x: Row) => x.index) zip
          a2.rows.toIndexedSeq.sortBy((x: Row) => x.index)).forall(v => v._1 == v._2)
    case _ => false
  }

  override def hashCode: Int = name.hashCode + rows.hashCode
}
object Sheet {
  def apply(name: String)(rows: Set[Row]): Sheet = new Sheet(name)(rows)
  def unapply(sheet: Sheet): Option[(String, Set[Row])] = Some((sheet.name, sheet.rows))
}

class Row(val index: Int)(val cells: Set[Cell]) {
  def styles(sheet: String): Map[CellStyle, List[CellAddr]] = cells.foldRight(Map.empty[CellStyle, List[CellAddr]]) {
    case (cell, map) => (map /: cell.styles(sheet, index)) { case (m, (k, v)) =>  m + (k -> (m.getOrElse(k, Nil) ++ v)) }
  }
  override def toString: String = "Row (" + this.index + ")(" + this.cells.toIndexedSeq.sortBy(_.index) + ")"
  
  override def equals(obj: Any): Boolean = obj match {
    case a2: Row =>
      val a1 = this
      a1.index == a2.index && a1.cells.toStream.corresponds(a2.cells.toStream)(_ == _)
    case _ => false
  }

  override def hashCode: Int = index.hashCode + cells.hashCode
}
object Row {
  def apply(index: Int)(cells: Set[Cell]): Row = new Row(index)(cells)
  def unapply(row: Row): Option[(Int, Set[Cell])] = Some((row.index, row.cells))
}

sealed abstract class Cell(val index: Int, val style: Option[CellStyle]) {
  def styles(sheet: String, row: Int): Map[CellStyle, List[CellAddr]] = style match {
    case None => Map()
    case Some(s) => Map(s -> List(CellAddr(sheet, row, index)))
  }
  override def toString: String = this match {
    case StringCell(index0, data)  => "StringCell("  + index0 + ", \""  + data + "\")"
    case NumericCell(index0, data) => "NumericCell(" + index0 + ", "    + data + ")"
    case BooleanCell(index0, data) => "BooleanCell(" + index0 + ", "    + data + ")"
    case FormulaCell(index0, data) => "FormulaCell(" + index0 + ", \"=" + data + "\")"
    case StyledCell(cell, style0) => "StyledCell(" + cell.toString /* shows(cell) */ + ", <style>)"
  }
}
case class StringCell (override val index: Int, data: String ) extends Cell(index, None)
case class NumericCell(override val index: Int, data: Double ) extends Cell(index, None)
case class BooleanCell(override val index: Int, data: Boolean) extends Cell(index, None)

class FormulaCell(override val index: Int, val data: String) extends Cell(index, None) {
  override def equals(obj: Any) = obj match {
    case f2: FormulaCell =>
      val f1 = this
      f1.index == f2.index && f1.data == f2.data
    case _ => false
  }

  override def hashCode: Int = index.hashCode + data.hashCode
}
object FormulaCell {
  def apply(index: Int, data: String): FormulaCell =
    new FormulaCell(index, data.dropWhile(_ == '='))
  def unapply(cell: FormulaCell): Option[(Int, String)] = Some((cell.index, cell.data))
}

class StyledCell private (override val index: Int, override val style: Option[CellStyle], val nestedCell: Cell) extends Cell(index, style) {
  def unstyledCell: Cell = nestedCell match {
    case cell: StyledCell => cell.nestedCell
    case _ => nestedCell
  }
  override def equals(obj: Any) = obj match {
    case s2: StyledCell =>
      val s1 = this
      s1.index == s2.index && s1.style == s2.style && s1.nestedCell == s2.nestedCell
    case _ => false
  }

  override def hashCode: Int = index.hashCode + style.hashCode + nestedCell.hashCode()
}
object StyledCell {
  def apply(cell: Cell, style: CellStyle): StyledCell = new StyledCell(cell.index, Some(style), cell)
  def unapply(cell: StyledCell): Option[(Cell, CellStyle)] = cell.style map { case style => (cell.nestedCell, style) }
}

case class CellAddr(sheet: String, row: Int, col: Int)

sealed abstract class WorkbookVersion
case object HSSF extends WorkbookVersion
case object XSSF extends WorkbookVersion
