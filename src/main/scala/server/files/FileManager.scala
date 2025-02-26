package server.files

import server.app.Envs.{aws_accessKey, aws_secretKey}
import com.amazonaws.HttpMethod
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.{CopyObjectRequest, GeneratePresignedUrlRequest, ObjectMetadata}
import com.typesafe.config.ConfigFactory
import server.http.AppMessage.{AppSender, ErrorTextResponse, FileManagerMessage, SuccessTextResponse}
import io.circe.generic.JsonCodec
import io.circe.syntax.EncoderOps
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.slf4j.LoggerFactory

import java.util.{Calendar, Date, UUID}
import scala.collection.mutable.ListBuffer

object FileManager {

  private val logger = LoggerFactory.getLogger(this.toString)
  private val config = ConfigFactory.load()


  private val credentials = new BasicAWSCredentials(aws_accessKey, aws_secretKey)
  private val endpointPrivate = new EndpointConfiguration(config.getString("aws.endpointPrivate"), config.getString("aws.region"))
  private val endpointPublic = new EndpointConfiguration(config.getString("aws.endpointPublic"), config.getString("aws.region"))

  val s3clientPrivate = AmazonS3ClientBuilder
    .standard()
    .withCredentials(new AWSStaticCredentialsProvider(credentials))
    .withPathStyleAccessEnabled(true)
    .withEndpointConfiguration(endpointPrivate)
    .build()
  private val s3clientPublic = AmazonS3ClientBuilder
    .standard()
    .withCredentials(new AWSStaticCredentialsProvider(credentials))
    .withPathStyleAccessEnabled(true)
    .withEndpointConfiguration(endpointPublic)
    .build()

  private val bucketPrivate = config.getString("aws.bucketPrivate")
  private val bucketPublic = config.getString("aws.bucketPublic")
  private val bucketPublicUrl = config.getString("aws.bucketPublicUrl")

  val expireMinutes = 5
  private val uidLength = 15


  case class UploadFile(fileName: String) extends FileManagerMessage
  case class GetShareFileUrl(filePath: String) extends FileManagerMessage
  case class GetFileUrl(filePath: String, forceDownload: Boolean = false) extends FileManagerMessage

  @JsonCodec
  case class UploadUrl(path: String, url: String)

  def apply(): Behavior[(FileManagerMessage, AppSender)] = Behaviors.setup(context => {
    Behaviors.receiveMessage({
      case (UploadFile(fileName), sender) =>
        try{
          val fileUrls = ListBuffer.empty[String]
          fileUrls += createFileUrl(fileName)
          val privateBucketDir = createPrivateBucketDir
          while (s3clientPrivate.doesObjectExist(privateBucketDir, fileUrls.last)){
            fileUrls += createFileUrl(fileName)
          }
          val filePath = fileUrls.last
          val url = s3clientPrivate.generatePresignedUrl(privateBucketDir, filePath, getExpire(expireMinutes), HttpMethod.PUT).toString
          sender.ref.tell(SuccessTextResponse(UploadUrl(privateBucketDir + "/" + filePath, url).asJson.noSpaces))
        }
        catch {
          case e: Throwable =>
            logger.error(e.toString)
            sender.ref.tell(ErrorTextResponse(e.toString.asJson.noSpaces))
        }
        Behaviors.same
      case (GetShareFileUrl(filePath), sender) =>
        try{
          s3clientPublic.copyObject(bucketPrivate, filePath, bucketPublic, filePath)
          val url = List(bucketPublicUrl, filePath).mkString("/")
          sender.ref.tell(SuccessTextResponse(url.asJson.noSpaces))
        }
        catch {
          case e: Throwable =>
            logger.error(e.toString)
            sender.ref.tell(ErrorTextResponse(e.toString.asJson.noSpaces))
        }
        Behaviors.same
      case (GetFileUrl(filePath, forceDownload), sender) =>
        try{
          val request = new GeneratePresignedUrlRequest("", filePath)
          request.setExpiration(getExpire(expireMinutes))
          if (forceDownload){
            val copyPath = "octet-stream" + "/" + filePath
            val copyObjectRequest = new CopyObjectRequest(bucketPrivate, filePath, bucketPrivate, copyPath)
            val metadata = new ObjectMetadata()
            metadata.setContentType("application/octet-stream")
            copyObjectRequest.setNewObjectMetadata(metadata)
            s3clientPublic.copyObject(copyObjectRequest)
            request.setKey(copyPath)
          }
          sender.ref.tell(SuccessTextResponse(s3clientPrivate.generatePresignedUrl(request).toString.asJson.noSpaces))
        }
        catch {
          case e: Throwable =>
            logger.error(e.toString)
            sender.ref.tell(ErrorTextResponse(e.toString.asJson.noSpaces))
        }
        Behaviors.same
      case (_, _) =>
        Behaviors.same
    })
  })

  def createFileUrl(fileName: String): String = {
    val c = Calendar.getInstance()
    val month = c.get(Calendar.MONTH) + 1
    val day = c.get(Calendar.DAY_OF_MONTH)
    List(month.toString, day.toString, uid, fileName).mkString("/")
  }
  def createPrivateBucketDir: String = {
    val c = Calendar.getInstance()
    c.get(Calendar.YEAR).toString
  }
  def getExpire(minutes: Int): Date = {
    val d = new Date()
    d.setTime(d.getTime + minutes * 1000 * 60)
    d
  }
  def getFileSize(filePath: String): Long = {
    s3clientPrivate.getObjectMetadata("", filePath).getContentLength / 1024
  }
  private def uid = UUID.randomUUID().toString.replace("-", "").take(uidLength)
}
