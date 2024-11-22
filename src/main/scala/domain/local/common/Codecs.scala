package domain.local.common


import domain.deepsea.MongoEleManager.{EleComplect, EleComplectTest}
import io.circe.generic.semiauto.{deriveCodec, deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.bson.codecs.configuration.CodecRegistry
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._

trait Codecs {
  implicit val EleComplectDecoder: Decoder[EleComplect] = deriveDecoder[EleComplect]
  implicit val EleComplectEncoder: Encoder[EleComplect] = deriveEncoder[EleComplect]

  val codecRegistry: CodecRegistry = fromRegistries(fromProviders(
    classOf[EleComplect],
//    classOf[EleComplectTest],
  ), DEFAULT_CODEC_REGISTRY)
}