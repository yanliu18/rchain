package coop.rchain.casper

import cats.Applicative
import cats.effect.Sync
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.option._
import com.google.protobuf.ByteString
import coop.rchain.casper.protocol.Signature
import coop.rchain.crypto.codec.Base16
import coop.rchain.crypto.signatures.{Secp256k1, SignaturesAlg}
import coop.rchain.crypto.{PrivateKey, PublicKey}
import coop.rchain.shared.{EnvVars, Log, LogSource}

final case class ValidatorIdentity(
    publicKey: PublicKey,
    privateKey: PrivateKey,
    sigAlgorithm: String
) {
  def signature(data: Array[Byte]): Signature = {
    val sig = SignaturesAlg(sigAlgorithm).map(_.sign(data, privateKey)).get
    Signature(ByteString.copyFrom(publicKey.bytes), sigAlgorithm, ByteString.copyFrom(sig))
  }
}

object ValidatorIdentity {
  private val RNodeValidatorPasswordEnvVar  = "RNODE_VALIDATOR_PASSWORD"
  implicit private val logSource: LogSource = LogSource(this.getClass)

  def apply[F[_]: Applicative](
      privateKey: PrivateKey
  ): ValidatorIdentity = {
    val publicKey = Secp256k1.toPublic(privateKey)

    ValidatorIdentity(
      publicKey,
      privateKey,
      Secp256k1.name
    )
  }

  def getEnvVariablePassword[F[_]: Sync: EnvVars]: F[String] =
    EnvVars[F].get(RNodeValidatorPasswordEnvVar) >>= (
      _.liftTo(new Exception(s"Environment variable $RNodeValidatorPasswordEnvVar is unspecified"))
    )

  def fromHex(privKeyHex: String): Option[ValidatorIdentity] =
    Base16.decode(privKeyHex).map(PrivateKey(_)).map(ValidatorIdentity(_))

  def fromPrivateKeyWithLogging[F[_]: Applicative: Log](
      privKey: Option[String]
  ): F[Option[ValidatorIdentity]] =
    privKey
      .map(fromHex)
      .fold(
        Log[F]
          .warn("No private key detected, cannot create validator identification.")
          .as(none[ValidatorIdentity])
      )(_.pure)
}
