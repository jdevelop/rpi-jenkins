package pi.jenkins.build

import java.io.InputStream
import java.security.cert.X509Certificate

import org.apache.http.HttpResponse
import org.apache.http.client.config.RequestConfig
import org.apache.http.config.{RegistryBuilder, SocketConfig}
import org.apache.http.conn.socket.{ConnectionSocketFactory, PlainConnectionSocketFactory}
import org.apache.http.conn.ssl.{SSLConnectionSocketFactory, SSLContexts, TrustStrategy}
import org.apache.http.impl.client.{CloseableHttpClient, HttpClients}
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.slf4j.LoggerFactory

object HTTPClient {

  type StreamHandler = (String, HttpResponse) ⇒ InputStream

  private final val LOG = LoggerFactory.getLogger(HTTPClient.getClass)

  private val TIMEOUT: Int = 20 * 1000

  private val socketConfig: SocketConfig = SocketConfig.custom()
    .setSoTimeout(TIMEOUT)
    .setTcpNoDelay(true)
    .build()

  private val requestConfig: RequestConfig = RequestConfig.custom()
    .setSocketTimeout(TIMEOUT)
    .setConnectTimeout(TIMEOUT)
    .build()


  private val sslContext = SSLContexts.custom().loadTrustMaterial(null, new TrustStrategy {
    override def isTrusted(chain: Array[X509Certificate], authType: String): Boolean = true
  }).build()

  private val sslsf = new SSLConnectionSocketFactory(
    sslContext, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)

  private val socketFactoryRegistry = RegistryBuilder.create[ConnectionSocketFactory]().
    register("https", sslsf).
    register("http", PlainConnectionSocketFactory.INSTANCE).build()

  private val cm = new PoolingHttpClientConnectionManager(socketFactoryRegistry)
  cm.setDefaultSocketConfig(socketConfig)
  cm.setMaxTotal(200)
  cm.setDefaultMaxPerRoute(20)

  val client: CloseableHttpClient = HttpClients.custom()
    .setDefaultRequestConfig(requestConfig)
    .setConnectionManager(cm)
    .build()

}