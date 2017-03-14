package pi.jenkins.build

import com.rojoma.json.ast.JString
import com.rojoma.json.io.JsonReader
import com.rojoma.json.jpath.JPath
import org.apache.commons.io.IOUtils
import org.apache.http.HttpHost
import org.apache.http.auth.{AuthScheme, AuthScope, UsernamePasswordCredentials}
import org.apache.http.client.AuthCache
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.impl.auth.BasicScheme
import org.apache.http.impl.client.BasicCredentialsProvider

object JenkinsAdapter {

  sealed trait BuildStatus

  case object BuildSuccessful extends BuildStatus

  case object BuildFailure extends BuildStatus

  private lazy val CredentialsProvider = {
    val cp = new BasicCredentialsProvider()
    cp.setCredentials(AuthScope.ANY,
      new UsernamePasswordCredentials(
        Configuration.Api.Username,
        Configuration.Api.AccessKey)
    )
    cp
  }

  object JenkinsBuildAdapter {

    private val StaticScheme = new BasicScheme()

    private val cache = new AuthCache {

      override def get(host: HttpHost): AuthScheme = StaticScheme

      override def clear(): Unit = {}

      override def put(host: HttpHost, authScheme: AuthScheme): Unit = {}

      override def remove(host: HttpHost): Unit = {}
    }


    private def prepareContext(): HttpClientContext = {
      val ctx = HttpClientContext.create()
      ctx.setCredentialsProvider(CredentialsProvider)
      ctx.setAuthCache(cache)
      ctx
    }

  }

  trait JenkinsBuildAdapter {

    import HTTPClient.client

    val baseUrl: String

    def getLastBuildUrl(): String = {
      val get = new HttpGet(s"${baseUrl}/api/json?tree=lastBuild[url]")
      val resp = client.execute(get, JenkinsBuildAdapter.prepareContext())
      val lastUrl = JenkinsResponseParser.resolveLastBuildUrl(IOUtils.toString(resp.getEntity.getContent, "UTF-8"))
      get.releaseConnection()
      lastUrl
    }

    def getLastBuildStatus(url: String): BuildStatus = {
      val get = new HttpGet(s"${url}/api/json?tree=result")
      val resp = client.execute(get, JenkinsBuildAdapter.prepareContext())
      val buildStatus = JenkinsResponseParser.parseBuildStatus(IOUtils.toString(resp.getEntity.getContent, "UTF-8"))
      get.releaseConnection()
      buildStatus
    }

  }

  object JenkinsResponseParser {

    def resolveLastBuildUrl(src: String): String = {
      val jsonObj = JsonReader.fromString(src)
      new JPath(jsonObj).down("lastBuild").down("url").finish.head.asInstanceOf[JString].string
    }

    def parseBuildStatus(src: String): BuildStatus = {
      val jsonObj = JsonReader.fromString(src)
      new JPath(jsonObj).down("result").finish.head.asInstanceOf[JString].string match {
        case "FAILURE" ⇒ BuildFailure
        case "SUCCESS" ⇒ BuildSuccessful
      }
    }

  }

}