package domain.deepsea

import com.itextpdf.kernel.events.IEventHandler
import com.itextpdf.io.font.{FontProgramFactory, PdfEncodings}
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.Color
import com.itextpdf.kernel.events.{Event, PdfDocumentEvent}
import com.itextpdf.kernel.font.{PdfFont, PdfFontFactory}
import com.itextpdf.kernel.font.PdfFontFactory.EmbeddingStrategy
import com.itextpdf.kernel.geom.{Line, PageSize, Rectangle}
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.kernel.pdf.{PdfDocument, PdfPage, PdfReader, PdfWriter}
import com.itextpdf.layout.element.{AreaBreak, Cell, Image, Paragraph, Table}
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.{Border, DashedBorder, SolidBorder}
import com.itextpdf.layout.properties.{HorizontalAlignment, TextAlignment}
import domain.deepsea.ForanManager.{CableNodes, CableRoutesList, CablesPdf}
import domain.deepsea.MongoEleManager.EleComplect
import domain.deepsea.pdfGenerator.{getNodes, printDocBorder, printTitle, sort}

import java.io.FileInputStream
import java.nio.file.{Files, Paths}
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import scala.collection.immutable.{List, TreeMap}

object pdfEleComplectGenerator {
  def createEleComplectPdf(data: Seq[CablesPdf], complect: List[EleComplect], filteredNodes: Seq[CableNodes], cablesRoutesList: Seq[CableRoutesList], rev: String): String = {
    println("createEleComplectPdf")
    try {
//            val file = Files.createTempFile("spec", ".pdf")
      //дата
      val currentDate = LocalDate.now()
      val year = currentDate.format(DateTimeFormatter.ofPattern("yyyy"))
      val month = currentDate.format(DateTimeFormatter.ofPattern("MM"))
      val day = currentDate.format(DateTimeFormatter.ofPattern("dd"))

      // создание директории
      val dirPath = Paths.get("/files")
      val yearPath = dirPath.resolve(year)
      if (!Files.exists(yearPath)) {
        Files.createDirectories(yearPath)
      }

      val monthPath = yearPath.resolve(month)
      if (!Files.exists(monthPath)) {
        Files.createDirectories(monthPath)
      }

      val dayPath = monthPath.resolve(day)
      if (!Files.exists(dayPath)) {
        Files.createDirectories(dayPath)
      }

      val randomDirName = UUID.randomUUID().toString.take(10)
      val randomDirPath = dayPath.resolve(randomDirName)

      if (!Files.exists(randomDirPath)) {
        Files.createDirectories(randomDirPath)
      }

      // Создаем файл PDF в соответствующей директории
      var fileName = "file.pdf"
      var filePath = randomDirPath.resolve(fileName)
      val file = Files.createFile(filePath)

      println(file)
      val gostFont = PdfFontFactory.createFont(FontProgramFactory.createFont("fonts/GOSTtypeA.ttf"), PdfEncodings.IDENTITY_H, EmbeddingStrategy.PREFER_NOT_EMBEDDED)
      val gostFontBold = PdfFontFactory.createFont(FontProgramFactory.createFont("fonts/gost_2.304_Bold.ttf"), PdfEncodings.IDENTITY_H, EmbeddingStrategy.PREFER_NOT_EMBEDDED)
      val writer = new PdfWriter(file.toString)
      val pdf = new PdfDocument(writer)
      val document = new Document(pdf,PageSize.A4.rotate())
      val totalWidth = PageSize.A4.getHeight


      val pdfNumber: String = complect(0).drawingId.concat("ВК") //номер чертежа для документа
      println(pdfNumber);
      printTitle(document, 1, totalWidth, gostFont, "КАБЕЛЬНЫЙ ЖУРНАЛ", pdfNumber, rev)  //добавляем таблицу с титульника
      document.add(new AreaBreak())  //разрыв страницы.

      //      создаем основную таблицу
      val columnPercentages = Array(15F, 20F, 15F, 7F, 10F, 10F, 10F, 10F, 15F)
      val pointColumnWidths = columnPercentages.map(p => (totalWidth * p / 100).toFloat)
      val table = new Table(pointColumnWidths).useAllAvailableWidth()
      table.setMarginBottom(60F)

      //заполняем шапку основной таблицы
      table.addHeaderCell(new Cell(2, 1).add(new Paragraph("Индекс кабеля").setFont(gostFont)))
      table.addHeaderCell(new Cell(2, 1).add(new Paragraph("Марка кабеля").setFont(gostFont)))
      table.addHeaderCell(new Cell(2, 1).add(new Paragraph("Число жил и сечение, мм2").setFont(gostFont)))
      table.addHeaderCell(new Cell(2, 1).add(new Paragraph("Проектная длина, м").setFont(gostFont)))

      val fromCell = new Cell(1, 2)
      fromCell.add(new Paragraph("Откуда идет кабель").setFont(gostFont))
      table.addHeaderCell(fromCell)
      val toCell = new Cell(1, 2)
      toCell.add(new Paragraph("Куда идет кабель").setFont(gostFont))
      table.addHeaderCell(toCell)
      // Примечание
      table.addHeaderCell(new Cell().add(new Paragraph("Примечание").setFont(gostFont)))


      val arr = Seq("индекс", "помещ", "помещ", "индекс", "")
      arr.foreach(i => {
        val call = new Cell()
        call.add(new Paragraph(i).setFont(gostFont))
        table.addHeaderCell(call)
      })


      pdf.addEventHandler(PdfDocumentEvent.END_PAGE, new FooterEventListener(pdf, document, totalWidth, gostFont, pdfNumber, rev))

      try {
      } catch {
        case e: Throwable => e.toString
      }
      //заполняем данными
      try {
        val zones = complect(0).zoneNames
        val dataByZones = filterDataByZones(data, zones)
        val groupedData = dataByZones.groupBy(_.system)
        val sortedGroupedData = TreeMap(groupedData.toSeq.sortBy(_._1): _*)
        sortedGroupedData.foreach { case (system, cables) =>
          val sortedCables = sort(cables)
          val systemCell = new Cell(1, 9).add(new Paragraph(system).setTextAlignment(TextAlignment.CENTER).setFont(gostFont).setBold())
          systemCell.setHorizontalAlignment(HorizontalAlignment.CENTER)
          table.addCell(systemCell)

          // Добавляем строки с информацией о кабелях
          sortedCables.foreach { cable =>
            val cable_specN = cable.cable_spec_short.replaceAll("""^.*? - """, "")
            table.addCell(new Cell().add(new Paragraph(cable.cable_id).setFont(gostFont)))
            table.addCell(new Cell().add(new Paragraph(cable_specN).setFont(gostFont))) // Марка кабеля
            table.addCell(new Cell().add(new Paragraph(cable.section).setFont(gostFont)))
            table.addCell(new Cell().add(new Paragraph(cable.total_length.toString.replace('.', ',')).setFont(gostFont)))  // длина
            table.addCell(new Cell().add(new Paragraph(cable.from_e_id).setFont(gostFont))) // Индекс откуда
            table.addCell(new Cell().add(new Paragraph(cable.from_zone_id).setFont(gostFont))) // Помещение откуда
            table.addCell(new Cell().add(new Paragraph(cable.to_zone_id).setFont(gostFont))) // Помещение куда
            table.addCell(new Cell().add(new Paragraph(cable.to_e_id).setFont(gostFont))) // Индекс куда
            table.addCell(new Cell().add(new Paragraph("").setFont(gostFont))) // Примечание

            // Строка с нодами кабеля
            val nodes = getNodes(cable.cable_id, filteredNodes, cablesRoutesList) // Ноды конкретного кабеля
            val nodesCell = new Cell(1, 9)
            if (nodes.nonEmpty) {
              nodesCell.add(new Paragraph(nodes).setFont(gostFontBold))
              table.addCell(nodesCell)
            }
//            else {
//              nodesCell.add(new Paragraph("").setFont(gostFontBold))
//            }
//            table.addCell(nodesCell)
          }
        }

        document.add(table)
        document.close()

//        println(file.toString)
//                file.toString
        printDocBorder(file.toString)  //ссылка на второй файл с бордерами уже
      } catch {
        case e: Throwable =>
          println(e.toString)
          e.toString
      }
    } catch {
      case e: Throwable => println(e.toString)
        e.toString
    }
  }


  def filterDataByZones(data: Seq[CablesPdf], zoneNames: Seq[String]): Seq[CablesPdf] = {  //отбираю кабели которые входят в эту зону
    data.filter(record =>
      zoneNames.contains(record.to_zone_id) || zoneNames.contains(record.from_zone_id)
    )
  }


  //заполняем титульник на каждой странице
  class FooterEventListener(pdf: PdfDocument, document: Document, totalWidth: Float, gostFont: PdfFont, pdfNumber: String, rev: String) extends IEventHandler {
    var p = 2
    override def handleEvent(event: Event): Unit = {
      if (event.isInstanceOf[PdfDocumentEvent]) {
        println("if" + p)
        printTitle(document, p, totalWidth, gostFont, "КАБЕЛЬНЫЙ ЖУРНАЛ", pdfNumber, rev)
      }
      p = p + 1
    }
  }
}
