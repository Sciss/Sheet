/*
 *  Cell.scala
 *  (POI)
 *
 *  Copyright (c) 2011-2015 George Leontiev and others. Fork by Hanns Holger Rutz.
 *
 *	This software is published under the Apache License 2.0
 */

package de.sciss.sheet

sealed trait Cell {
  def index: Int
  def style: Option[CellStyle]

  def styles(sheet: String, row: Int): Map[CellStyle, List[CellAddr]] = style match {
    case None => Map()
    case Some(s) => Map(s -> List(CellAddr(sheet, row, index)))
  }
  override def toString: String = this match {
    case StringCell (index0, data) => s"""StringCell($index0, "$data")"""
    case NumericCell(index0, data) => s"NumericCell($index0, $data)"
    case BooleanCell(index0, data) => s"BooleanCell($index0, $data)"
    case FormulaCell(index0, data) => s"""FormulaCell($index0, "=$data")"""
    case StyledCell (cell, style0) => s"StyledCell($cell, <style>)"
  }
}
sealed trait UnstyledCell extends Cell { final def style = Option.empty[CellStyle] }
final case class StringCell (index: Int, data: String ) extends UnstyledCell
final case class NumericCell(index: Int, data: Double ) extends UnstyledCell
final case class BooleanCell(index: Int, data: Boolean) extends UnstyledCell

object FormulaCell {
  def apply(index: Int, data: String): FormulaCell =
    new FormulaCell(index, data.dropWhile(_ == '='))

  def unapply(cell: FormulaCell): Option[(Int, String)] = Some((cell.index, cell.data))
}
final class FormulaCell(val index: Int, val data: String) extends UnstyledCell {
  override def equals(obj: Any) = obj match {
    case f2: FormulaCell =>
      val f1 = this
      f1.index == f2.index && f1.data == f2.data
    case _ => false
  }

  override def hashCode: Int = index.hashCode + data.hashCode
}

object StyledCell {
  def apply(cell: Cell, style: CellStyle): StyledCell = new StyledCell(cell.index, Some(style), cell)
  def unapply(cell: StyledCell): Option[(Cell, CellStyle)] = cell.style map { case style => (cell.nestedCell, style) }
}
final class StyledCell private (val index: Int, val style: Option[CellStyle], val nestedCell: Cell)
  extends Cell {

  def unstyled: Cell = nestedCell match {
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
