import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._

class ModelSpec extends Specification {
	
	import models._
	import play.api.cache.Cache
	
	"User model" should {
		"return existing users" in {
			running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
				val user = User("toto1", "toto1@gmail.com", "welcome1", None, true, null)
				User.create(user)

				User.exists(user.name, "anything") must beTrue
				User.exists("anything", user.email) must beTrue
				User.exists(user.name, user.email) must beTrue
				User.exists("anything", "anything") must beFalse
			}
		}
	}
	
}