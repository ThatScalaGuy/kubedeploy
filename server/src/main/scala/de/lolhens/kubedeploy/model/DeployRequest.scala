package de.lolhens.kubedeploy.model

import cats.syntax.functor._
import de.lolhens.kubedeploy.model.DeployRequest.Locator
import io.circe.generic.semiauto._
import io.circe.{Codec, Decoder, Encoder, Json}

case class DeployRequest(
                          resource: String,
                          value: String,
                          locator: Option[Locator],
                          awaitStatus: Option[Boolean],
                        ) {
  def locatorOrDefault: Locator = locator.getOrElse(Locator.Version)

  def awaitStatusOrDefault: Boolean = awaitStatus.getOrElse(false)
}

object DeployRequest {
  implicit val codec: Codec[DeployRequest] = deriveCodec

  case class DeployRequests(deployRequests: Seq[DeployRequest])

  object DeployRequests {
    implicit val codec: Codec[DeployRequests] = Codec.from(
      Decoder[Seq[DeployRequest]].or(Decoder[DeployRequest].map(Seq(_))).map(DeployRequests(_)),
      Encoder[Seq[DeployRequest]].contramap(_.deployRequests)
    )
  }

  sealed trait Locator

  object Locator {
    case object Version extends Locator {
      implicit val codec: Codec[Version.type] = Codec.from(
        Decoder.decodeString.emap {
          case "version" => Right(Version)
          case _ => Left("not a version locator")
        },
        Encoder.instance(_ => Json.fromString("version"))
      )
    }

    case class Regex(regex: String) extends Locator

    object Regex {
      implicit val codec: Codec[Regex] = deriveCodec
    }

    implicit val codec: Codec[Locator] = Codec.from(
      Decoder[Version.type].widen[Locator]
        .or(Decoder[Regex].widen[Locator]),
      Encoder.instance {
        case Version => Encoder[Version.type].apply(Version)
        case regex: Regex => Encoder[Regex].apply(regex)
      }
    )
  }
}
