import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "euro2012"
    val appVersion      = "1.0"

    val appDependencies = Seq(
      // Add your project dependencies here,
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(

    	// specify *explicitly* which less files are to be compiled
    	lessEntryPoints <<=	baseDirectory(base =>
    			(base / "app" / "assets" / "stylesheets" / "main.less")
    			+++ (base / "app" / "assets" / "stylesheets" / "bootstrap" / "bootstrap.less")
    			+++ (base / "app" / "assets" / "stylesheets" / "bootstrap" / "responsive.less")
    		)

    )

}
