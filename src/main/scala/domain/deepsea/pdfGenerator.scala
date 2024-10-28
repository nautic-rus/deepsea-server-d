package domain.deepsea

import com.itextpdf.io.font.{FontProgramFactory, PdfEncodings}
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.font.PdfFontFactory.EmbeddingStrategy
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.{PdfDocument, PdfWriter, PdfXrefTable}
import com.itextpdf.layout.element.{Cell, Paragraph, Table}
import com.itextpdf.layout.Document
import com.itextpdf.layout.properties.{HorizontalAlignment, TextAlignment, VerticalAlignment}
import domain.deepsea.ForanManager.{CableNodes, CablesPdf}
import domain.deepsea.pdfGenerator.getNodes

import java.nio.file.Files
//
object pdfGenerator {
  def createPdf(data: Seq[CablesPdf], filteredNodes: Seq[CableNodes]): String = {
    println("cableData in def")
    try {
      val file = Files.createTempFile("spec", ".pdf")
      val gostFont = PdfFontFactory.createFont(FontProgramFactory.createFont("fonts/GOSTtypeA.ttf"), PdfEncodings.IDENTITY_H, EmbeddingStrategy.PREFER_NOT_EMBEDDED)
      val gostFontBold = PdfFontFactory.createFont(FontProgramFactory.createFont("fonts/gost_2.304_Bold.ttf"), PdfEncodings.IDENTITY_H, EmbeddingStrategy.PREFER_NOT_EMBEDDED)
      val writer = new PdfWriter(file.toString)
      val pdf = new PdfDocument(writer)
      val document = new Document(pdf,PageSize.A4.rotate())

      val columnPercentages = Array(10F, 20F, 10F, 7F, 10F, 10F, 10F, 10F,10F)

      val totalWidth = PageSize.A4.getHeight

      val pointColumnWidths = columnPercentages.map(p => (totalWidth * p / 100).toFloat)

      val table = new Table(pointColumnWidths)

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


            val sortedCables = sortWithNumbers(cablesWithNodes.map(_.cable_id)).map(cableId => cablesWithNodes.find(_.cable_id == cableId).get)
            // Добавляем строки с информацией о кабелях
            sortedCables.foreach { cable =>
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

        // Добавление таблицы в документ
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

  def sortWithNumbers(strings: Seq[String]) = {
    strings.sortBy { str =>
    }
  }
}
