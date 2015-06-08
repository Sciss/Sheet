# POI

Apache POI DSL for Scala

__Note:__ This is a fork from George Leontiev (folone)'s [original project](https://github.com/folone/poi.scala).
What I did here is remove the Scalaz dependency, and publish an independent artifact as:

    "de.sciss" %% "poi-scala" % v
    
The current version `v` is `"0.1.0"`.

The example usage below is similar, except that you don't have to go through the `IO` monad and `unsafePerformIO`.

## Usage

```scala
scala> import de.sciss.poi._
import de.sciss.poi._

scala> val bookOne = Workbook {
   Set(Sheet("foo") {
     Set(Row(1) {
       Set(NumericCell(1, 13.0/5), FormulaCell(2, "ABS(A1)"))
     },
     Row(2) {
       Set(StringCell(1, "data"), StringCell(2, "data2"))
     })
   },
   Sheet("bar") {
     Set(Row(2) {
       Set(BooleanCell(1, true), NumericCell(2, 2.4))
     })
   })
 }
bookOne: de.sciss.poi.Workbook = Workbook(Sheet("bar")(Row(2)(BooleanCell(1, true), NumericCell(2, 2.4))), Sheet("foo")(Row(1)(NumericCell(1, 2.6), FormulaCell(2, "=ABS(A1)")), Row(2)(StringCell(1, "data"), StringCell(2, "data2"))))

scala> val path = "/tmp/workbook.xls"
path: String = /tmp/workbook.xls

scala> bookOne.saveToFile(path)

scala> val bookTwo = Workbook {
        Set(Sheet("foo") {
          Set(Row(1) {
            Set(StringCell(1, "newdata"), StringCell(2, "data2"), StringCell(3, "data3"))
          },
          Row(2) {
            Set(StringCell(1, "data"), StringCell(2, "data2"))
          },
          Row(3) {
            Set(StringCell(1, "data"), StringCell(2, "data2"))
          })
        },
        Sheet("bar") {
          Set(Row(2) {
            Set(StringCell(1, "data"), StringCell(2, "data2"))
          })
        })
      }
bookTwo: de.sciss.poi.Workbook = Workbook(Sheet("bar")(Row(2)(StringCell(1, "data"), StringCell(2, "data2"))), Sheet("foo")(Row(1)(StringCell(1, "newdata"), StringCell(2, "data2"), StringCell(3, "data3")), Row(2)(StringCell(1, "data"), StringCell(2, "data2")), Row(3)(StringCell(1, "data"), StringCell(2, "data2"))))

scala> bookTwo.sheetMap("foo").matrix(rows = 2 to 3, columns = 2 to 3) { 
        case Some(StringCell(_, x)) => x;
        case _ => "n/a"
       } .flatten
res1: scala.collection.immutable.IndexedSeq[String] = Vector(data2, n/a, data2, n/a)

scala> val bookOneReloaded = Workbook.fromFile(path)
bookOneReloaded: de.sciss.poi.Workbook = Workbook(Sheet("bar")(Row(2)(BooleanCell(1, true), NumericCell(2, 2.4))), Sheet("foo")(Row(1)(NumericCell(1, 2.6), FormulaCell(2, "=ABS(A1)")), Row(2)(StringCell(1, "data"), StringCell(2, "data2"))))

scala> bookOne == bookOneReloaded
res2: Boolean = true
```
