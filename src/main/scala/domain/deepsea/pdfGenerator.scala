package domain.deepsea

import com.itextpdf.kernel.events.IEventHandler
import com.itextpdf.io.font.{FontProgramFactory, PdfEncodings}
import com.itextpdf.kernel.events.{Event, PdfDocumentEvent}
import com.itextpdf.kernel.font.{PdfFont, PdfFontFactory}
import com.itextpdf.kernel.font.PdfFontFactory.EmbeddingStrategy
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.{PdfDocument, PdfWriter}
import com.itextpdf.layout.element.{AreaBreak, Cell, Paragraph, Table}
import com.itextpdf.layout.Document
import com.itextpdf.layout.properties.{HorizontalAlignment, TextAlignment}
import domain.deepsea.ForanManager.{CableNodes, CablesPdf}

import java.nio.file.{Files, Paths}
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
//
object pdfGenerator {
  def createPdf(data: Seq[CablesPdf], filteredNodes: Seq[CableNodes]): String = {
    println("cableData in def")
    try {

      val file = Files.createTempFile("spec", ".pdf")
//      //дата
//      val currentDate = LocalDate.now()
//
//      val year = currentDate.format(DateTimeFormatter.ofPattern("yyyy"))
//      val month = currentDate.format(DateTimeFormatter.ofPattern("MM"))
//      val day = currentDate.format(DateTimeFormatter.ofPattern("dd"))
//
//      // создание директории
//      val dirPath = Paths.get("/files")
//      val yearPath = dirPath.resolve(year)
//      if (!Files.exists(yearPath)) {
//        Files.createDirectories(yearPath)
//      }
//
//      val monthPath = yearPath.resolve(month)
//      if (!Files.exists(monthPath)) {
//        Files.createDirectories(monthPath)
//      }
//
//      val dayPath = monthPath.resolve(day)
//      if (!Files.exists(dayPath)) {
//        Files.createDirectories(dayPath)
//      }
//
//      val randomDirName = UUID.randomUUID().toString.take(10)
//      val randomDirPath = dayPath.resolve(randomDirName)
//
//      if (!Files.exists(randomDirPath)) {
//        Files.createDirectories(randomDirPath)
//      }
//
//
//      // Создаем файл PDF в соответствующей директории
//      var fileName = "cables3.pdf"
//      var filePath = randomDirPath.resolve(fileName)
//      val file = Files.createFile(filePath)

//      val file = Files.createFile(dirPath.resolve("file2.pdf"))
      println(file)
      val gostFont = PdfFontFactory.createFont(FontProgramFactory.createFont("fonts/GOSTtypeA.ttf"), PdfEncodings.IDENTITY_H, EmbeddingStrategy.PREFER_NOT_EMBEDDED)
      val gostFontBold = PdfFontFactory.createFont(FontProgramFactory.createFont("fonts/gost_2.304_Bold.ttf"), PdfEncodings.IDENTITY_H, EmbeddingStrategy.PREFER_NOT_EMBEDDED)
      val writer = new PdfWriter(file.toString)
      val pdf = new PdfDocument(writer)
      val document = new Document(pdf,PageSize.A4.rotate())
      val totalWidth = PageSize.A4.getHeight

      printTitle(document, 1, totalWidth, gostFont);  //добавляем таблицу с титульника
      document.add(new AreaBreak());  //разрыв страницы

//      // Создаем новую таблицу с 2 столбцами и 2 строками
//      val columnPercentagesTitle = Array(100F, 200F, 50F, 50F)
//
//      val pointColumnWidthsTitle = columnPercentagesTitle.map(p => (totalWidth * p / 100).toFloat)
//      val titleTable = new Table(pointColumnWidthsTitle)
//
////      1ая строка
//      titleTable.addCell(new Cell(1, 2).add(new Paragraph("КАБЕЛЬНЫЙ ЖУРНАЛ МАГИСТРАЛЬНЫХ КАБЕЛЕЙ").setFont(gostFont)))
//      titleTable.addCell(new Cell(1, 2).add(new Paragraph("Дата").setFont(gostFont)))
//
////      2ая строка
//      titleTable.addCell(new Cell(2, 1).add(new Paragraph("ЛОГО").setFont(gostFont)))
//      titleTable.addCell(new Cell(2, 1).add(new Paragraph("Номер чертежа").setFont(gostFont)))
//      titleTable.addCell(new Cell().add(new Paragraph("Рев.").setFont(gostFont)))
//      titleTable.addCell(new Cell().add(new Paragraph("Лист").setFont(gostFont)))
//      titleTable.addCell(new Cell().add(new Paragraph("С").setFont(gostFont)))
//      titleTable.addCell(new Cell().add(new Paragraph(pdf.getNumberOfPages.toString).setFont(gostFont)))
//
//      // Устанавливаем позицию таблицы в правом нижнем углу
//      titleTable.setFixedLayout()
//      titleTable.setMarginTop(PageSize.A4.getWidth - 150F)
//
//      // Добавляем таблицу в документ
//      document.add(titleTable)
//      document.add(new AreaBreak());  //переход на новую страницу

      //footer
//      pdf.addEventHandler(PdfDocumentEvent.END_PAGE, new FooterEventListener(document, 1, totalWidth, gostFont))
//      создаем основную таблицу
      val columnPercentages = Array(10F, 20F, 10F, 7F, 10F, 10F, 10F, 10F,10F)
      val pointColumnWidths = columnPercentages.map(p => (totalWidth * p / 100).toFloat)
      val table = new Table(pointColumnWidths)
      table.setMarginBottom(250F)


      //заполняем шапку основной таблицы
      table.addCell(new Cell(2, 1).add(new Paragraph("Индекс кабеля").setFont(gostFont)))
      table.addCell(new Cell(2, 1).add(new Paragraph("Марка кабеля").setFont(gostFont)))
      table.addCell(new Cell(2, 1).add(new Paragraph("Число жил и сечение, мм2").setFont(gostFont)))
      table.addCell(new Cell(2, 1).add(new Paragraph("Проектная длина, м").setFont(gostFont)))

      val fromCell = new Cell(1, 2)
      fromCell.add(new Paragraph("Откуда идет кабель").setFont(gostFont))
      table.addCell(fromCell)
      val toCell = new Cell(1, 2)
      toCell.add(new Paragraph("Куда идет кабель").setFont(gostFont))
      table.addCell(toCell)
      // Примечание
      table.addCell(new Cell().add(new Paragraph("Примечание").setFont(gostFont)))


      val arr = Seq("индекс", "помещ", "помещ", "индекс", "")
      arr.foreach(i => {
        val call = new Cell()
        call.add(new Paragraph(i).setFont(gostFont))
        table.addCell(call)
      })

     pdf.addEventHandler(PdfDocumentEvent.END_PAGE, new FooterEventListener(document, totalWidth, gostFont))

      //заполняем данными
      try {
        val groupedData = data.groupBy(_.system)
        groupedData.foreach { case (system, cables) =>
          // Проверяем, есть ли хотя бы один кабель с нодами
          val cablesWithNodes = cables.filter(cable => getNodes(cable.cable_id, filteredNodes).nonEmpty)

          if (cablesWithNodes.nonEmpty) { // Если есть хотя бы один кабель с нодами
            val systemCell = new Cell(1, 9).add(new Paragraph(system).setTextAlignment(TextAlignment.CENTER).setFont(gostFont).setBold())
            systemCell.setHorizontalAlignment(HorizontalAlignment.CENTER)
            table.addCell(systemCell)

            // Добавляем строки с информацией о кабелях
            cablesWithNodes.foreach { cable =>
              val nodes = getNodes(cable.cable_id, filteredNodes) // Ноды конкретного кабеля
              val cable_specN = cable.cable_spec_short.replaceAll("""^.*? - """, "")
              table.addCell(new Cell().add(new Paragraph(cable.cable_id).setFont(gostFont)))
              table.addCell(new Cell().add(new Paragraph(cable_specN).setFont(gostFont))) // Марка кабеля
              table.addCell(new Cell().add(new Paragraph(cable.section).setFont(gostFont)))
              table.addCell(new Cell().add(new Paragraph(cable.total_length.toString).setFont(gostFont)))
              table.addCell(new Cell().add(new Paragraph(cable.from_e_id).setFont(gostFont))) // Индекс откуда
              table.addCell(new Cell().add(new Paragraph(cable.from_zone_id).setFont(gostFont))) // Помещение откуда
              table.addCell(new Cell().add(new Paragraph(cable.to_zone_id).setFont(gostFont))) // Помещение куда
              table.addCell(new Cell().add(new Paragraph(cable.to_e_id).setFont(gostFont))) // Индекс куда
              table.addCell(new Cell().add(new Paragraph("").setFont(gostFont))) // Примечание

              // Строка с нодами кабеля
              val nodesCell = new Cell(1, 9)
              nodesCell.add(new Paragraph(nodes).setFont(gostFont))
              table.addCell(nodesCell)
            }
          }
        }
        document.add(table)
        document.close()

        println(file.toString)
        file.toString
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

  def getNodes(cable_id: String, data: Seq[CableNodes]): String = {  //выбираю ноды конкретного кабеля и формирую строку из них
    data.filter(_.cable_id == cable_id)
      .map(_.node)
      .mkString(", ")
  }

  //заполняем титульник на каждой странице
  class FooterEventListener(document: Document, totalWidth: Float, gostFont: PdfFont) extends IEventHandler {
    override def handleEvent(event: Event): Unit = {
      if (event.isInstanceOf[PdfDocumentEvent]) {
        printTitle(document, 1, totalWidth, gostFont)
      }
    }
  }

  def printTitle(document: Document, pageNumber: Number, totalWidth: Float, gostFont: PdfFont): Unit = {
    val columnWidthsTitle = Array(100F, 200F, 50F, 50F)
    val tableWidth = columnWidthsTitle.sum

    val titleTable = new Table(columnWidthsTitle.map(_.toFloat))

    // 1ая строка
    titleTable.addCell(new Cell(1, 2).add(new Paragraph("КАБЕЛЬНЫЙ ЖУРНАЛ МАГИСТРАЛЬНЫХ КАБЕЛЕЙ").setFont(gostFont)))
    titleTable.addCell(new Cell(1, 2).add(new Paragraph("Дата").setFont(gostFont)))

    // 2ая строка
    titleTable.addCell(new Cell(2, 1).add(new Paragraph("ЛОГО").setFont(gostFont)))
    titleTable.addCell(new Cell(2, 1).add(new Paragraph("Номер чертежа").setFont(gostFont)))
    titleTable.addCell(new Cell().add(new Paragraph("Рев.").setFont(gostFont)))
    titleTable.addCell(new Cell().add(new Paragraph("Лист").setFont(gostFont)))
    titleTable.addCell(new Cell().add(new Paragraph("С").setFont(gostFont)))
    titleTable.addCell(new Cell().add(new Paragraph(pageNumber.toString).setFont(gostFont)))

    // Устанавливаем позицию таблицы в правом нижнем углу
    titleTable.setFixedPosition(PageSize.A4.getHeight-450F, 50F, 400F)

    // Добавляем таблицу в документ
    document.add(titleTable)
  }
}
