package controllers

import play.api.libs.json._
import play.api.libs.json.Json._
import play.api.mvc._
import play.api.Logger
import Security.Secured
import models.DataSource
import models.TableMetaData
import models.DataSourceJsonSupport
import models.DataSourceJsonSupport._

object DataAccess extends Controller with Secured with Debuggable {
	val logger = Logger(this.getClass())

	def showDatabase = IsAdmin { _ => implicit request => Ok(views.html.database()) }
	
	def getTableMetaDataList = IsAdmin { _ => implicit request =>
		import DataSourceJsonSupport._
		val list = DataSource.getTableMetaDataList()
		Ok(toJson(list))
	}
	
	def getTableData(table: String) = IsAdmin { _ => implicit request =>
		// decode request coming from datatables.net js lib (see http://datatables.net/usage/server-side)
		val sEcho = request.queryString("sEcho")(0)
		val pageNum = request.queryString("iDisplayStart")(0).toInt
		val pageSize = request.queryString("iDisplayLength")(0).toInt
		logger.debug("getTableData table=%s pageNum=%d pageSize=%d" format (table, pageNum, pageSize))
		
		val iSortingCols = request.queryString("iSortingCols")(0).toInt
		val orderBy = if (iSortingCols > 0) Some(request.queryString("iSortCol_0")(0).toInt + 1) else None
		val orderDir = request.queryString("sSortDir_0")(0)
		val page = DataSource.getTablePage(table, pageNum, pageSize, orderBy, orderDir)
		
		Ok(toDataTablesResp(page, sEcho))
	}
	
	/**
	 * Convert Page object to DataTables.net JSon compatible response (see datatables.net)
	 */
	def toDataTablesResp(page: models.Page[List[String]], sEcho: String): JsValue = {
		toJson(
			Map(
				"iTotalRecords" -> toJson(page.total),
				"iTotalDisplayRecords" -> toJson(page.total),
				"sEcho" -> toJson(sEcho),
				"aaData" -> toJson(page.items)
			)
		)
	}
	
	/*
	def getTableData(table: String, pageNum: Int) = IsAdmin { _ => implicit request =>

		val page = DataSource.getTableData3(table, pageNum)
		
		val json = toJson(page)
		Ok(json)
	}
	
	def getTableData__OLD__(table: String) = IsAdmin { _ => implicit request =>
		val columns = DataSource.getTableMetaData(table).columns.map(_.name)
		// create a list of column indices which must be masked (ex: passwords...)
		val columnIndicesToBeMasked = columns.zipWithIndex.collect { 
			case (col, idx) if (col.toLowerCase().contains("password")) => idx+1 // add 1 because the first column is the rownum 
		}
		val data = DataSource.getTableData2(table)
		//val tmp = data.map(l => l.map(value => value.toString())) // transform Seq[List[Any]] into Seq[List[String]]
		val tmp = data.map { list => 
			val tmp2 = for (idx <- list.indices) yield {
				if (columnIndicesToBeMasked.contains(idx))
					"******"
				else
					list(idx).toString()
			} 
			tmp2.toList
		}
		
		val json = toJson(tmp)
		Ok(json)
	}
	*/
	
}