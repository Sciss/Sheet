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

scala> val sheetOne = Workbook {
   Set(Sheet("name") {
     Set(Row(1) {
       Set(NumericCell(1, 13.0/5), FormulaCell(2, "ABS(A1)"))
     },
     Row(2) {
       Set(StringCell(1, "data"), StringCell(2, "data2"))
     })
   },
   Sheet("name2") {
     Set(Row(2) {
       Set(BooleanCell(1, true), NumericCell(2, 2.4))
     })
   })
 }
sheetOne: de.sciss.poi.Workbook = Workbook(Set(Sheet ("name")(Set(Row (1)(Set(NumericCell(1, 2.6), FormulaCell(2, "=ABS(A1)"))), Row (2)(Set(StringCell(1, "data"), StringCell(2, "data2"))))), Sheet ("name2")(Set(Row (2)(Set(BooleanCell(1, true), NumericCell(2, 2.4)))))))

scala> val path = "/tmp/workbook.xls"
path: String = /tmp/workbook.xls

scala> sheetOne.saveToFile(path)

scala> val sheetTwo = Workbook {
        Set(Sheet("name") {
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
        Sheet("name") {
          Set(Row(2) {
            Set(StringCell(1, "data"), StringCell(2, "data2"))
          })
        })
      }
sheetTwo: de.sciss.poi.Workbook = Workbook(Set(Sheet ("name")(Set(Row (1)(Set(StringCell(1, "newdata"), StringCell(2, "data2"), StringCell(3, "data3"))), Row (2)(Set(StringCell(1, "data"), StringCell(2, "data2"))), Row (3)(Set(StringCell(1, "data"), StringCell(2, "data2"))))), Sheet ("name")(Set(Row (2)(Set(StringCell(1, "data"), StringCell(2, "data2")))))))

scala> val sheetOneReloaded = Workbook.fromFile(path)
sheetOneReloaded: de.sciss.poi.Workbook = Workbook(Set(Sheet ("name2")(Set(Row (2)(Set(BooleanCell(1, true), NumericCell(2, 2.4))))), Sheet ("name")(Set(Row (2)(Set(StringCell(1, "data"), StringCell(2, "data2"))), Row (1)(Set(NumericCell(1, 2.6), FormulaCell(2, "=ABS(A1)")))))))

scala> sheetOne == sheetOneReloaded
res1: Boolean = true
```
