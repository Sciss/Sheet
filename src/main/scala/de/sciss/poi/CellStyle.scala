/*
 *  CellStyle.scala
 *  (POI)
 *
 *  Copyright (c) 2011-2015 George Leontiev and others. Fork by Hanns Holger Rutz.
 *
 *	This software is published under the Apache License 2.0
 */

package de.sciss.poi

import org.apache.poi.hssf.usermodel.HSSFFont.FONT_ARIAL
import org.apache.poi.ss.usermodel.{Font => POIFont, DataFormat => POIDataFormat}

object CellStyle {
  final case class Font(name: String  = FONT_ARIAL,
                  bold: Boolean = false,
                  color: Short  = POIFont.COLOR_NORMAL) {

    def appliedTo(pf: POIFont): POIFont = {
      pf setFontName   name
      pf setBoldweight boldWeight
      pf setColor      color
      pf
    }

    private def boldWeight: Short = if (bold) POIFont.BOLDWEIGHT_BOLD else POIFont.BOLDWEIGHT_NORMAL
  }

  final case class DataFormat(format: String) {
    def appliedTo(poiDataFormat: POIDataFormat): Short = poiDataFormat.getFormat(format)
  }
}
final case class CellStyle(font: CellStyle.Font, dataFormat: CellStyle.DataFormat)
