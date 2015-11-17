package de.sciss.sheet

import de.sciss.sheet
import org.apache.poi.hssf.usermodel.HSSFSheet
import org.scalacheck.Arbitrary._
import org.scalacheck._
import org.specs2.ScalaCheck
import org.specs2.mutable._
import org.specs2.specification.Scope

class PoiSpec extends Specification with ScalaCheck {
    "Poi" should {
      "create workbook" in {
        val wb = Workbook {
          Set(Sheet("name") {
            Set(Row(1) {
              Set(NumericCell(1, -13.0/5), FormulaCell(2, "ABS(B2)"))
            },
            Row(2) {
              Set(StringCell(1, "data"), StringCell(2, "data2"))
            })
          },
          Sheet("name2") {
            Set(Row(2) {
              Set(BooleanCell(1, data = true), NumericCell(2, 2.4))
            })
          })
        }
        val path = "/tmp/book.xls"
        wb.saveToFile(path)
        Workbook.fromFile(path) === wb
      }
    }

  "Workbook" should {
    "have sheets in it" in new Workbook {
      book.peer.getSheet("test") must beAnInstanceOf[HSSFSheet]
    }
  }

  "Sheet" can {
    "have filled cells" in new Workbook {
      val cellText = book.peer.getSheet("test").getRow(0).getCell(0).getStringCellValue
      cellText must beEqualTo("theCell")
    }
  }

  def positiveInt = Gen.choose(0, Integer.MAX_VALUE)

  implicit def arbCell: Arbitrary[Cell] = Arbitrary(for {
    index      <- positiveInt
    stringData <- Gen.alphaStr
    boolData   <- arbitrary[Boolean]
    doubleData <- arbitrary[Double]
    res        <- Gen.oneOf(StringCell(index, stringData),
      BooleanCell(index, boolData), NumericCell(index, doubleData))
  } yield res)

  implicit def arbRow: Arbitrary[Row] = Arbitrary(for {
    index <- positiveInt
    cells <- arbitrary[Set[Cell]]
  } yield Row(index)(cells))

  implicit def arbSheet: Arbitrary[Sheet] = Arbitrary(for {
    name <- arbitrary[String]
    rows <- arbitrary[Set[Row]]
  } yield Sheet(name)(rows))

  implicit def arbWorkbook: Arbitrary[sheet.Workbook] = Arbitrary(for {
    sheets <- arbitrary[Set[Sheet]]
  } yield Workbook(sheets))

  trait Workbook extends Scope {
    val book = Workbook(Set(Sheet("test")(Set(Row(0)(Set(StringCell(0, "theCell")))))))
  }
}
