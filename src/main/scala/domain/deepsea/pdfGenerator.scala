package domain.deepsea

import com.itextpdf.kernel.events.IEventHandler
import com.itextpdf.io.font.{FontProgramFactory, PdfEncodings}
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.events.{Event, PdfDocumentEvent}
import com.itextpdf.kernel.font.{PdfFont, PdfFontFactory}
import com.itextpdf.kernel.font.PdfFontFactory.EmbeddingStrategy
import com.itextpdf.kernel.geom.{Line, PageSize, Rectangle}
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.kernel.pdf.{PdfDocument, PdfPage, PdfReader, PdfWriter}
import com.itextpdf.layout.element.{AreaBreak, Cell, Image, Paragraph, Table}
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.{Border, SolidBorder}
import com.itextpdf.layout.properties.{HorizontalAlignment, TextAlignment}
import domain.deepsea.ForanManager.{CableNodes, CablesPdf}

import java.io.FileInputStream
import java.nio.file.{Files, Paths}
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import scala.collection.immutable.TreeMap
//
object pdfGenerator {
  def createPdf(data: Seq[CablesPdf], filteredNodes: Seq[CableNodes]): String = {
    println("cableData in def")
    try {
//      val file = Files.createTempFile("spec", ".pdf")
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

      printTitle(document, 1, totalWidth, gostFont)  //добавляем таблицу с титульника
      document.add(new AreaBreak())  //разрыв страницы

//      создаем основную таблицу
      val columnPercentages = Array(15F, 20F, 15F, 7F, 10F, 10F, 10F, 10F, 15F)
      val pointColumnWidths = columnPercentages.map(p => (totalWidth * p / 100).toFloat)
      val table = new Table(pointColumnWidths).useAllAvailableWidth()
      table.setMarginBottom(55F)

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


      pdf.addEventHandler(PdfDocumentEvent.END_PAGE, new FooterEventListener(pdf, document, totalWidth, gostFont))

      //заполняем данными
      try {
        val groupedData = data.groupBy(_.system)
        val sortedGroupedData = TreeMap(groupedData.toSeq.sortBy(_._1): _*)
        sortedGroupedData.foreach { case (system, cables) =>
          // Проверяем, есть ли хотя бы один кабель с нодами
          val cablesWithNodes = cables.filter(cable => getNodes(cable.cable_id, filteredNodes).nonEmpty)
//          println(sort(cablesWithNodes))
          val sortedCablesWithNodes = sort(cablesWithNodes)


          if (sortedCablesWithNodes.nonEmpty) { // Если есть хотя бы один кабель с нодами
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
              table.addCell(new Cell().add(new Paragraph(cable.total_length.toString.replace('.', ',')).setFont(gostFont)))  // длина
              table.addCell(new Cell().add(new Paragraph(cable.from_e_id).setFont(gostFont))) // Индекс откуда
              table.addCell(new Cell().add(new Paragraph(cable.from_zone_id).setFont(gostFont))) // Помещение откуда
              table.addCell(new Cell().add(new Paragraph(cable.to_zone_id).setFont(gostFont))) // Помещение куда
              table.addCell(new Cell().add(new Paragraph(cable.to_e_id).setFont(gostFont))) // Индекс куда
              table.addCell(new Cell().add(new Paragraph("").setFont(gostFont))) // Примечание

              // Строка с нодами кабеля
              val nodesCell = new Cell(1, 9)
              nodesCell.add(new Paragraph(nodes).setFont(gostFontBold))
              table.addCell(nodesCell)
            }
          }
        }

//        printPageBorder(pdf, 1)  //delete
//        printPageBorder(pdf, 2)

        document.add(table)
        document.close()

        println(file.toString)
//        file.toString
        printDocBorder(file.toString)  //второй файл с бордерами уже
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

  def sort (arr: Seq[CablesPdf]): Seq[CablesPdf] = {
    arr.sortBy(_.cable_id)
  }


  //заполняем титульник на каждой странице
  class FooterEventListener(pdf: PdfDocument, document: Document, totalWidth: Float, gostFont: PdfFont) extends IEventHandler {
    var p = 2
    override def handleEvent(event: Event): Unit = {
      if (event.isInstanceOf[PdfDocumentEvent]) {
        printTitle(document, p, totalWidth, gostFont)
        p = p + 1
      }
    }
  }

  def printTitle(document: Document, pageNumber: Int, totalWidth: Float, gostFont: PdfFont): Unit = {
    val currentDate = LocalDate.now()
    val fullDate = currentDate.format(DateTimeFormatter.ofPattern("dd.MM.yy"))

    val columnWidthsTitle = Array(100F, 200F, 50F, 50F)
    val titleTable = new Table(columnWidthsTitle)

    // 1ая строка
    titleTable.addCell(new Cell(1, 2).add(new Paragraph("КАБЕЛЬНЫЙ ЖУРНАЛ МАГИСТРАЛЬНЫХ КАБЕЛЕЙ").setFont(gostFont)))
    titleTable.addCell(new Cell(1, 2).add(new Paragraph(s"Дата $fullDate").setFont(gostFont).setTextAlignment(TextAlignment.CENTER)))

    // 2ая строка
    try {  //строка с логотипом
      val imgCell = new Cell(2, 1)
//      val imageFile = "C:\\img\\logo450.png"   //не забыть заменить!!!!!!!!!!!!!!!!!
      val imageFile = "/files/logo450.png" //закинула картинку на сервак ручками
      val data = ImageDataFactory.create(imageFile)   //
      val img = new Image(data)
      imgCell.add(img.setAutoScale(true))
      titleTable.addCell(imgCell)
    }
    catch {
      case e: Throwable => println(s"image error $e.toString")
    }

    titleTable.addCell(new Cell(2, 1).add(new Paragraph("Номер чертежа").setFont(gostFont)))
    titleTable.addCell(new Cell().add(new Paragraph("Рев.").setFont(gostFont).setTextAlignment(TextAlignment.CENTER)))
    titleTable.addCell(new Cell().add(new Paragraph("Лист").setFont(gostFont).setTextAlignment(TextAlignment.CENTER)))
    titleTable.addCell(new Cell().add(new Paragraph("").setFont(gostFont)))
    titleTable.addCell(new Cell().add(new Paragraph(pageNumber.toString).setFont(gostFont).setTextAlignment(TextAlignment.CENTER)))

    // Устанавливаем позицию таблицы в правом нижнем углу
    titleTable.setFixedPosition(PageSize.A4.getHeight-436F, 30F, 400F)

    // Добавляем таблицу в документ
    document.add(titleTable)
  }

  def printDocBorder(inputPdfPath: String): String = {  //так как нельзя менять существующий файл, то создаем новый и постранично копируем из старого
    try {
      // Открываем исходный файл
      val reader = new PdfReader(inputPdfPath)
      val inputDocument = new PdfDocument(reader)

      // Создаем путь к выходному файлу
      val outputPdfPath = inputPdfPath.replace("file.pdf", "cables.pdf")

      // Создаем выходной файл (если не существует)
      Files.createFile(Paths.get(outputPdfPath))

      // Открываем выходной файл для записи
      val writer = new PdfWriter(outputPdfPath)
      val outputPdf = new PdfDocument(writer)
      val outputDocument = new Document(outputPdf, PageSize.A4.rotate())

      // Копируем страницы
      val numberOfPages = inputDocument.getNumberOfPages
      for (i <- 1 to numberOfPages) {
        val page = inputDocument.getPage(i)
        val p = page.copyTo(outputPdf)
        outputPdf.addPage(p)
        printPageBorder(outputPdf, i)
      }

      // Закрываем документы
      outputDocument.close()
      inputDocument.close()

      outputPdfPath  //возвращаем ссылку на ужеотредактированный файл с бордерами
    } catch {
      case e: Throwable => e.toString
    }
  }

  def printPageBorder(pdf: PdfDocument, pageNumber: Int): Unit = {
    try {
        val pdfPage = pdf.getPage(pageNumber)
        val canvas = new PdfCanvas(pdfPage)
        canvas.rectangle(36, 30, 770, 529)
//        //горизонтальная сверху
//        canvas.moveTo(35, 560)
//        canvas.lineTo(806, 560)
//        //горизонтальная снизу
//        canvas.moveTo(35, 30)
//        canvas.lineTo(806, 30)
//        //вертикальная слева
//        canvas.moveTo(36, 30)
//        canvas.lineTo(36, 560)
//        //вертикальная справа
//        canvas.moveTo(806, 30)
//        canvas.lineTo(806, 560)
        canvas.closePathStroke()
    } catch {
      case e: Throwable => println(e.toString)
    }
  }

}
