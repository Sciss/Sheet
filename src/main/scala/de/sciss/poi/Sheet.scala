/*
 *  Sheet.scala
 *  (POI)
 *
 *  Copyright (c) 2011-2015 George Leontiev and others. Fork by Hanns Holger Rutz.
 *
 *	This software is published under the Apache License 2.0
 */

package de.sciss.poi

import scala.collection.breakOut
import scala.collection.immutable.{IndexedSeq => Vec}

object Sheet {
  def apply(name: String)(rows: Set[Row]): Sheet = new Sheet(name)(rows)
  def unapply(sheet: Sheet): Option[(String, Set[Row])] = Some((sheet.name, sheet.rows))
}
final class Sheet(val name: String)(val rows: Set[Row]) {
  /** Maps indices to rows. */
  lazy val rowMap: Map[Int, Row] = rows.map(r => (r.index, r))(breakOut)

  def styles: Map[CellStyle, List[CellAddr]] =
    rows.foldRight(Map.empty[CellStyle, List[CellAddr]]) {
      case (row, map) => (map /: row.styles(name)) { case (m, (k, v)) =>  m + (k -> (m.getOrElse(k, Nil) ++ v)) }
    }

  /** Sorted by index. */
  def sortedRows: Vec[Row] = rows.toIndexedSeq.sortBy(_.index)

  override def toString: String = s"""Sheet("$name")(${sortedRows.mkString(", ")})"""

  override def equals(obj: Any): Boolean = obj match {
    case that: Sheet => this.name == that.name && this.sortedRows == that.sortedRows
    case _ => false
  }

  /** Constructs a rectangular matrix from a given row and column index vector.
    *
    * Example:
    * {{{
    *   sheet.getMatrix(rows = 0 until 10, cols = 1 to 3) {
    *     case Some(NumericCell(_, v)) => v
    *     case _ => Double.NaN
    *   }
    * }}}
    *
    * @param rows     the rows to scan
    * @param columns  the columns to scan
    * @param fun      the function to extract a matrix cell value for a given cell option
    * @tparam A       the matrix cell type
    */
  def matrix[A](rows: Vec[Int], columns: Vec[Int])(fun: Option[Cell] => A): Vec[Vec[A]] =
    rows.map { ri =>
      rowMap.get(ri).fold(Vec.fill(columns.size)(fun(None))) { row =>
        columns.map(ci => fun(row.cellMap.get(ci)))
      }
    }

  override def hashCode: Int = name.hashCode + rows.hashCode
}