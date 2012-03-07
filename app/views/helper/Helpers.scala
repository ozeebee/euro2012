import play.api.mvc.RequestHeader
import play.api.templates._
import controllers.Security

package views.html.helper {

	object Sec {
		
		/**
		 * Helper method that can be used in template to generate html content
		 * for users logged with the role 'Admin'
		 */
		def IsAdmin(html:Html)(implicit request: RequestHeader): Html = {
			if (Security.isAdmin(request)) {
				html
			} else {
				Html.empty
			}
		}
		
	}
	
}
