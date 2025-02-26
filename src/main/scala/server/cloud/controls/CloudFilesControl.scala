package server.cloud.controls

import io.circe.generic.JsonCodec
import io.circe.parser.decode
import io.circe.syntax.EncoderOps
import server.app.Envs.{TextCodes, TextCodesRu}
import server.cloud.tables.CloudFilesTable
import server.http.AppMessage.{AppSender, CloudManagerMessage, ErrorTextResponse, NotAllowedTextResponse, SuccessTextResponse}
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.slf4j.LoggerFactory

import java.util.Date
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

trait CloudFilesControl extends CloudFilesTable {

  private val logger = LoggerFactory.getLogger(this.toString)

  case class GetCloudFiles(path: String, count: Boolean = false) extends CloudManagerMessage
  case class CreateCloudFilesPath(path: String) extends CloudManagerMessage
  case class GetCloudFilesAll() extends CloudManagerMessage
  case class GetCloudFilesAllFilter(path: String) extends CloudManagerMessage
  case class PostCloudFile(json: String) extends CloudManagerMessage
  case class PutCloudFile(json: String) extends CloudManagerMessage
  case class DeleteCloudFile(id: Int) extends CloudManagerMessage


  @JsonCodec case class CloudFilePost(name: String, path: String, url_path: String, kind: String, file_size: Long)
  @JsonCodec case class CloudFilePut(id: Int, file_name: String, file_path: String, url_path: String, date_changed: Long)

  def cloudFilesControl: PartialFunction[(CloudManagerMessage, AppSender), Behavior[(CloudManagerMessage, AppSender)]] = {
    case (GetCloudFiles(path, count), sender) =>
      sender.ref.tell(SuccessTextResponse(
        (if (count){
          getCloudFilesWithCount(path).sortBy(_.file_name)
        }
        else{
          getCloudFiles(path).sortBy(_.file_name)
        })
        .asJson.noSpaces))
      Behaviors.same
    case (GetCloudFilesAll(), sender) =>
      sender.ref.tell(SuccessTextResponse(getCloudFilesAll().asJson.noSpaces))
      Behaviors.same
    case (GetCloudFilesAllFilter(path), sender) =>
      sender.ref.tell(SuccessTextResponse(getCloudFilesAll(path).asJson.noSpaces))
      Behaviors.same
    case (PostCloudFile(json), sender) =>
      decode[CloudFilePost](json) match {
        case Right(js) =>
          val d = new Date().getTime
          val u = "user"
          postCloudFileCheck(js.path, js.name).onComplete{
            case Success(exists) =>
              if (!exists){
                val update = CloudFile(0, js.name, js.path, js.url_path, js.kind, js.file_size, d, u, 0, "", 0)
                postCloudFile(update).onComplete {
                  case Success(value) =>
                    sender.ref.tell(SuccessTextResponse(update.copy(id = value).asJson.noSpaces))
                  case Failure(exception) =>
                    logger.error(exception.toString)
                    sender.ref.tell(ErrorTextResponse(exception.toString.asJson.noSpaces))
                }
              }
              else{
                sender.ref.tell(NotAllowedTextResponse(TextCodesRu.FileInDirectoryExists.asJson.noSpaces))
              }
            case Failure(exception) =>
              logger.error(exception.toString)
              sender.ref.tell(ErrorTextResponse(exception.toString.asJson.noSpaces))
          }

        case Left(value) => sender.ref.tell(ErrorTextResponse(value.toString.asJson.noSpaces))
      }
      Behaviors.same
    case (CreateCloudFilesPath(path), sender) =>
      val d = new Date().getTime
      val u = "user"
      var fullPath = "/"
      path.split("/").filter(_ != "").foreach(p => {
        if (!getCloudFiles(fullPath).filter(_.kind == "DIRECTORY").exists(_.file_name == p)){
          val cf = CloudFile(0, p, fullPath, "", "DIRECTORY", 0, d, u, 0, "", 0)
          postCloudFile(cf).onComplete {
            case Success(value) => None
            case Failure(exception) => logger.error(exception.toString)
          }
        }
        fullPath = fullPath + p + "/"
      })
      sender.ref.tell(SuccessTextResponse(TextCodes.OK))
      Behaviors.same
    case (PutCloudFile(json), sender) =>
      decode[CloudFilePut](json) match {
        case Right(js) =>
          val d = new Date().getTime
          val u = "user"
          putCloudFilesCheck(js.id, js.date_changed).onComplete{
            case Success(existsDate) =>
              if (existsDate){
                putCloudFiles(js.id, js.file_name, js.file_path, js.url_path, u, d).onComplete {
                  case Success(value) =>
                    sender.ref.tell(SuccessTextResponse(TextCodes.Updated))
                  case Failure(exception) =>
                    logger.error(exception.toString)
                    sender.ref.tell(ErrorTextResponse(exception.toString.asJson.noSpaces))
                }
              }
              else{
                sender.ref.tell(NotAllowedTextResponse(TextCodesRu.Outdated.asJson.noSpaces))
              }
            case Failure(exception) =>
              logger.error(exception.toString)
              sender.ref.tell(ErrorTextResponse(exception.toString.asJson.noSpaces))
          }
        case Left(value) => sender.ref.tell(ErrorTextResponse(value.toString.asJson.noSpaces))
      }
      Behaviors.same
    case (DeleteCloudFile(id), sender) =>
      val d = new Date().getTime
      val u = "user"
      deleteCloudFile(id, u, d)
      sender.ref.tell(SuccessTextResponse(TextCodes.Removed))
      Behaviors.same
    case (_, _) =>
      Behaviors.same
  }
}
