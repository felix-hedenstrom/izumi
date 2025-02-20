package izumi.fundamentals.platform

import java.util.concurrent.TimeUnit
import izumi.fundamentals.platform.files.IzFiles
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.Try

class IzFilesTest extends AnyWordSpec {

  "File tools" should {
    "resolve path entries on nix-like systems" in {
      assert(IzFiles.which("bash").nonEmpty)
    }
  }

  import izumi.fundamentals.platform.resources.IzResources
  import scala.concurrent.Future

  "Resource tools" should {
    "support concurrent queries" in {
      // unfortunately this test is unreliable, the filesystem logic might occasionally fail during initialization
      import scala.concurrent.ExecutionContext.Implicits.global

      while (Try(IzResources.getPath("library.properties")).isFailure) {
        Thread.sleep(100)
      }

      val seq = (0 to 200).map {
        _ =>
          Future(IzResources.getPath("library.properties"))
      }
      val res = Await.result(Future.sequence(seq), Duration.apply(1, TimeUnit.MINUTES))
      assert(res.forall(_.isDefined))
    }
  }

}
