package pi.jenkins.build

import java.io.{File, FileFilter}

import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.LoggerFactory


object Configuration {

  /**
    * Configuration entry point
    */
  private val LOG = LoggerFactory.getLogger(getClass)

  private[this] def configDir =
    sys.props.get("JENKINS_CONFIG_HOME").map {
      fname ⇒ new File(fname)
    } getOrElse {
      val home = sys.env("HOME")
      new File(new File(home), ".jenkins-pi")
    }

  private lazy val cfg: Config = {
    val c = {
      val dir = configDir
      LOG.trace("Reading files from {}", configDir)
      if (dir.exists()) {
        dir.listFiles(new FileFilter {
          override def accept(pathname: File): Boolean =
            pathname.getName.endsWith(".conf") || pathname.getName.endsWith(".properties")
        }).foldLeft(ConfigFactory.empty()) {
          case (lc, src) ⇒
            LOG.trace("Loading configuration from '{}'", src)
            lc.withFallback(ConfigFactory.parseFile(src))
        }
      } else {
        ConfigFactory.empty()
      }
    }.withFallback(ConfigFactory.load("jenkins_config"))
      .withFallback(ConfigFactory.load("jenkins_config_reference"))
      .withFallback(ConfigFactory.load())
    LOG.trace(s"Config rendered as ${c.root().render()}")
    c
  }

  object Api {

    lazy val Username = cfg.getString("pi.jenkins.api.username")

    lazy val AccessKey = cfg.getString("pi.jenkins.api.accesskey")

  }


}