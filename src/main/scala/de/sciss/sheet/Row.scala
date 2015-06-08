/*
 *  Row.scala
 *  (POI)
 *
 *  Copyright (c) 2011-2015 George Leontiev and others. Fork by Hanns Holger Rutz.
 *
 *	This software is published under the Apache License 2.0
 */
package de.sciss.sheet

import scala.collection.breakOut
import scala.collection.immutable.{IndexedSeq => Vec}

object Row {
  def apply(index: Int)(cells: Set[Cell]): Row = new Row(index)(cells)
  def unapply(row: Row): Option[(Int, Set[Cell])] = Some((row.index, row.cells))
}
final class Row(val index: Int)(val cells: Set[Cell]) {
  /** Maps indices to cells. */
  lazy val cellMap: Map[Int, Cell] = cells.map(c => (c.index, c))(breakOut)

  def styles(sheet: String): Map[CellStyle, List[CellAddr]] = cells.foldRight(Map.empty[CellStyle, List[CellAddr]]) {
    case (cell, map) => (map /: cell.styles(sheet, index)) { case (m, (k, v)) =>  m + (k -> (m.getOrElse(k, Nil) ++ v)) }
  }
  override def toString: String = s"Row($index)(${sortedCells.mkString(", ")})"

  /** Sorted by index. */
  def sortedCells: Vec[Cell] = cells.toIndexedSeq.sortBy(_.index)

  override def equals(obj: Any): Boolean = obj match {
    case that: Row => this.index == that.index && this.sortedCells == that.sortedCells
    case _ => false
  }

  override def hashCode: Int = index.hashCode + cells.hashCode
}