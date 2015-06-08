/*
 *  Workbook.scala
 *  (POI)
 *
 *  Copyright (c) 2011-2015 George Leontiev and others. Fork by Hanns Holger Rutz.
 *
 *	This software is published under the Apache License 2.0
 */

package de.sciss.sheet

import java.io.{File, FileOutputStream, InputStream, OutputStream}

import org.apache.poi.ss.usermodel.{Cell => POICell, CellStyle => POICellStyle, Row => POIRow, Workbook => POIWorkbook, WorkbookFactory}

import scala.collection.breakOut
import scala.collection.immutable.{IndexedSeq => Vec}

object Workbook {
  def apply(sheets: Set[Sheet], format: Version = HSSF): Workbook =
    new Workbook(sheets.map(s => (s.name, s))(breakOut): Map[String, Sheet], format)

  def fromFile(path: String, format: Version = HSSF): Workbook = {
    val f = new File(path)
    readWorkbook[File](format, WorkbookFactory.create)(f)
  }

  def fromInputStream(is: InputStream, format: Version = HSSF): Workbook =
    readWorkbook[InputStream](format, WorkbookFactory.create)(is)

  sealed trait Version
  case object HSSF extends Version
  case object XSSF extends Version

  private def readWorkbook[T](format: Version, workbookF: T => POIWorkbook)(is: T): Workbook = {
    val wb   = workbookF(is)
    val data = for {
      i     <- 0 until wb.getNumberOfSheets
      sheet = wb.getSheetAt(i) if sheet != null
      k     <- 0 to sheet.getLastRowNum
      row   = sheet.getRow(k) if row != null
      j     <- 0 until row.getLastCellNum
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
class Workbook(val sheetMap: Map[String, Sheet], format: Workbook.Version = Workbook.HSSF) {
  lazy val sheets: Set[Sheet] = sheetMap.values.toSet

  /** Sorted by name (case sensitive) */
  def sortedSheets: Vec[Sheet] = sheets.toIndexedSeq.sortBy(_.name)

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

  private lazy val book: POIWorkbook = {
    val workbook = format match {
      case Workbook.HSSF => new org.apache.poi.hssf.usermodel.HSSFWorkbook
      case Workbook.XSSF => new org.apache.poi.xssf.usermodel.XSSFWorkbook
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

  private def applyStyling(wb: POIWorkbook, styles: Map[CellStyle, List[CellAddr]]): POIWorkbook = {
    def pStyle(cs: CellStyle): POICellStyle = {
      val pStyle = wb.createCellStyle()
      pStyle setFont       cs.font      .appliedTo(wb.createFont      )
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

  /** '''Note:''' method mutates this workbook. Returns `this` for convenience. */
  def styled(styles: Map[CellStyle, List[CellAddr]]): Workbook = {
    applyStyling(book, styles)
    this
  }

  /** '''Note:''' method mutates this workbook. Returns `this` for convenience. */
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

  /** The underlying Apache POI object. */
  def peer: POIWorkbook = book

  override def toString: String = s"Workbook(${sortedSheets.mkString(", ")})"
  
  override def equals(obj: Any): Boolean = obj match {
    case that: Workbook => this.sortedSheets == that.sortedSheets
    case _ => false
  }

  override def hashCode: Int = sheetMap.hashCode()
}