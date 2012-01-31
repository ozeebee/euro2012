import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._

class CacheSpec extends Specification {
	
	import play.api.cache.Cache
	
	"Cache" should {
		"return cached items" in {
			running(FakeApplication()) {
				import play.api.Play.current

				Cache.set("key1", "value1", 600)

				Cache.get("key1") must beSome
				Cache.getAs[String]("key1") must beSome
				Cache.getAs[String]("key1") must beAnInstanceOf[Option[String]]
				Cache.getAs[String]("key1") must beEqualTo(Some("value1"))
				
				Cache.get("key2") must beNone
			}
		}
	}
}