package domain.deepsea

import com.itextpdf.io.font.{FontProgramFactory, PdfEncodings}
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.font.PdfFontFactory.EmbeddingStrategy
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.{PdfDocument, PdfWriter, PdfXrefTable}
import com.itextpdf.layout.element.{Cell, Paragraph, Table}
import com.itextpdf.layout.Document
import domain.deepsea.ForanManager.{CableNodes, Cables}
import domain.deepsea.pdfGenerator.getNodes

import java.nio.file.Files
//
object pdfGenerator {
  def createPdf(data: Seq[Cables], filteredNodes: Seq[CableNodes]): String = {
    println("cableData in def")
    try {
      val file = Files.createTempFile("spec", ".pdf")
      val gostFont = PdfFontFactory.createFont(FontProgramFactory.createFont("fonts/GOSTtypeA.ttf"), PdfEncodings.IDENTITY_H, EmbeddingStrategy.PREFER_NOT_EMBEDDED)
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

      try {
        val arr = groupDataBySystem(data)
        println(arr)
      } catch {
        case e: Throwable => println(e.toString)
      }

      //заполняем данными
      try {
        data.foreach { cable => {
          val nodes = getNodes(cable.cable_id, filteredNodes)  //ноды конкрутного кабеля
          if (nodes.nonEmpty) { //если у кабеля есть ноды, то вывожу их
            val cable_specN = cable.cable_spec_short.replaceAll("""^.*? - """, "")
            table.addCell(new Cell().add(new Paragraph(cable.cable_id).setFont(gostFont)))
            table.addCell(new Cell().add(new Paragraph(cable_specN).setFont(gostFont))) //марка кабеля
            table.addCell(new Cell().add(new Paragraph(cable.section).setFont(gostFont)))
            table.addCell(new Cell().add(new Paragraph(cable.total_length.toString).setFont(gostFont)))
            table.addCell(new Cell().add(new Paragraph(cable.from_e_id).setFont(gostFont))) //индекс откуда
            table.addCell(new Cell().add(new Paragraph(cable.from_zone_id).setFont(gostFont))) //помещение откуда
            table.addCell(new Cell().add(new Paragraph(cable.to_zone_id).setFont(gostFont))) //помещение куда
            table.addCell(new Cell().add(new Paragraph(cable.to_e_id).setFont(gostFont))) //индекс куда
            table.addCell(new Cell().add(new Paragraph("").setFont(gostFont))) //примечание

            //строка с нодами кабеля
            val nodesCell = new Cell(1, 9)
            nodesCell.add(new Paragraph(nodes).setFont(gostFont))
            table.addCell(nodesCell)
          }
        }}

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

  def groupDataBySystem(data: Seq[Cables]) {
    data.groupBy(cable => cable.system)
  }
}
