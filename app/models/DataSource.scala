package models

import play.api.db._
import play.api.libs.json._
import play.api.libs.json.Json._
import play.api.Play.current
import anorm._
import anorm.SqlParser._
import java.sql.ResultSet
import java.sql.Connection

/**
 * Helper for pagination.
 * Taken fron the Play 2.0 computer database sample
 */
case class Page[A](items: Seq[A], page: Int, offset: Long, total: Long) {
  lazy val prev = Option(page - 1).filter(_ >= 0)
  lazy val next = Option(page + 1).filter(_ => (offset + items.size) < total)
}

object DataSource {
	val schema = "PUBLIC"
	
	private def getTableNames(connection: Connection): Seq[String] = {
		val rs = connection.getMetaData().getTables(null, schema, null, null)
		// create a stream of (resultSet, resultSet.next()), take resultset until no more results, transform the stream of Tuple[ResultSet, Boolean] in stream of String
		val tmp = Stream.continually(rs,rs.next()).takeWhile(_._2).map(_._1.getString("TABLE_NAME"))
		tmp.toList
	}

	private def getTableCount(table:String)(con: Connection): Long = {
		SQL("select count(*) from %s" format table).as(scalar[Long].single)(con)
	}
	
	private def getTableMetaData(table: String, connection: Connection): TableMetaData = {
		val rs = connection.getMetaData().getColumns(null, schema, table, null)
		// get a stream of Tuple2[String, String] item1=Column name, item2=Type name 
		val tmp = Stream.continually(rs,rs.next()).takeWhile(_._2).map { case (rs: ResultSet, _) =>
			(rs.getString("COLUMN_NAME"), rs.getString("TYPE_NAME"))
		}
		val cols = tmp.toList.map(t => (ColumnMetaData.apply _).tupled(t)) // the (XX _).tupled notation allows us to create a ColumnMetaData object by passing a tuple as arg
		val count = getTableCount(table)(connection)
		TableMetaData(table, cols, count)
	} 

	def getTableMetaData(table: String): TableMetaData = {
		DB.withConnection { implicit connection =>
			getTableMetaData(table, connection)
		}
	}
	
	def getTableMetaDataList(): Seq[TableMetaData] = {
		DB.withConnection { implicit connection =>
			val tables = getTableNames(connection)
			tables map(getTableMetaData(_, connection))
		}
	}
	
	def getTableData(table: String): Seq[Map[String, Any]] = {
		DB.withConnection { implicit connection =>
			val stream = SQL("""select * from %s""" format (table))()
			val tmp = stream.map(row => row.asMap.toMap/*necessary to convert map to an immutable map*/)
			tmp.toList
		}
	}
	
	def getTableData(table: String, page: Int = 0, pageSize: Int = 10): Seq[List[String]] = {
		val offset = page * pageSize
		DB.withConnection { implicit connection =>
			val stream = SQL("""
				select rownum(),* from %s
				limit {pageSize} offset {offset}
			""" format (table)
			).on('pageSize -> pageSize, 'offset -> offset)
			.apply()
			//val tmp = stream.map(row => row.asList)
			val tmp = stream.map { row =>
				val columnsWithIndices = row.metaData.availableColumns.zipWithIndex
				val data = row.asList
				columnsWithIndices.map { case (col, idx) => formatColumnValue(col, data(idx)) }
			}
			tmp.toList
		}
	}

	def getTablePage(table: String, offset: Int = 0, pageSize: Int = 10, orderBy: Option[Int] = None, orderDir: String = "ASC") = {
		val pageNum = offset / pageSize
		DB.withConnection { implicit connection =>
			val total = getTableCount(table)(connection)
			val orderBySql = orderBy.map("order by %d %s" format (_, orderDir)).getOrElse("")
			val stream = SQL("""
							| select rownum(),* from %s
							| %s
							| limit {pageSize} offset {offset}
							 """ format (table, orderBySql) stripMargin
							).on('pageSize -> pageSize, 'offset -> offset)
							.apply()
	
			// transform Stream[SqlRow] into List[List[String]] :
			//   get data as String and mask all password data with asterisks
			val tmp = stream.map { row =>
				val columnsWithIndices = row.metaData.availableColumns.zipWithIndex
				val data = row.asList
				columnsWithIndices.map { case (col, idx) => formatColumnValue(col, data(idx)) }
			}.toList
			
			val page = Page(tmp, pageNum, offset, total)
			page
		}
	}
	
	private def formatColumnValue(column: String, value: Any): String = {
		if (column.toLowerCase().contains("password"))
			"******"
		else
			value.toString()
	}
}

case class ColumnMetaData(name: String, ctype: String)
case class TableMetaData(name: String, columns: Seq[ColumnMetaData], count: Long)

object DataSourceJsonSupport {
	implicit object ColumnMetaDataWrites extends Writes[ColumnMetaData] {
		def writes(o: ColumnMetaData): JsValue = {
			toJson(
				Map(
					"name" -> toJson(o.name),
					"ctype" -> toJson(o.ctype)
				)
			)
		}
	}

	implicit object TableMetaDataWrites extends Writes[TableMetaData] {
		def writes(o: TableMetaData): JsValue = {
			toJson(
				Map(
					"name" -> toJson(o.name),
					"columns" -> toJson(o.columns.map(toJson(_))),
					"count" -> toJson(o.count)
				)
			)
		}
	}
	
	/**
	 * Serializer for Page[A] types.
	 */
	implicit def pageWrites[A](implicit fmt: Writes[A]): Writes[Page[A]] = new Writes[Page[A]] {
		def writes(ts: Page[A]) = {//JsArray(ts.map(t => toJson(t)(fmt)))
			toJson(
				Map(
					"items" -> toJson(ts.items),
					"page" -> toJson(ts.page),
					"offset" -> toJson(ts.offset),
					"total" -> toJson(ts.total)
				)
			)
		}
	}
	
}
