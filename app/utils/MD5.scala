package app.utils

object MD5 {
	/**
	 * Generates a hexadecimal hash from the given input string 
	 */
	def hash(str: String): String = {
		hex(java.security.MessageDigest.getInstance("MD5").digest(str.getBytes))
	}
	
	def hex(byteArray: Array[Byte]): String = {
		byteArray.map { byte =>
			(if (( byte & 0xff ) < 0x10 ) "0" else "" ) + java.lang.Long.toString(byte & 0xff, 16)
		}.mkString
	}	

}