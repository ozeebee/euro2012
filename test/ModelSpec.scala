import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._

object ModelSpec extends Specification {
	
	import models._
	
	"User model" should {
		"return existing users" in {
			running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
				val user = User("toto1", "toto1@gmail.com", "welcome1")
				User.create(user)

				User.exists(user.name, "anything") must beTrue
				User.exists("anything", user.email) must beTrue
				User.exists(user.name, user.email) must beTrue
				User.exists("anything", "anything") must beFalse
			}
		}
	}
}