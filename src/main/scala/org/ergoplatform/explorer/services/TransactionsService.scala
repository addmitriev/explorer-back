package org.ergoplatform.explorer.services

import cats._
import cats.effect._
import cats.implicits._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.transactor.Transactor
import org.ergoplatform.explorer.db.dao._
import org.ergoplatform.explorer.http.protocol.{TransactionInfo, TransactionSummaryInfo}
import org.ergoplatform.explorer.utils.Converter._
import org.ergoplatform.explorer.utils.Paging

import scala.concurrent.ExecutionContext

trait TransactionsService[F[_]] {

  def getTxInfo(id: String):  F[TransactionSummaryInfo]

  def getTxsByAddressId(addressId: String, p: Paging): F[List[TransactionInfo]]

  def countTxsByAddressId(addressId: String): F[Long]

}

class TransactionsServiceIOImpl[F[_]](xa: Transactor[F], ec: ExecutionContext)
                                     (implicit F: Monad[F], A: Async[F]) extends TransactionsService[F] {

  val headersDao = new HeadersDao
  val interlinksDao = new InterlinksDao
  val transactionsDao = new TransactionsDao
  val inputDao = new InputsDao
  val outputDao = new OutputsDao


  override def getTxInfo(id: String): F[TransactionSummaryInfo] = for {
    _ <- Async.shift[F](ec)
    base16Id <- F.pure(from58to16(id))
    result <- getTxInfoResult(base16Id)
  } yield result

  private def getTxInfoResult(id: String): F[TransactionSummaryInfo] = (for {
    tx <- transactionsDao.get(id)
    h <- headersDao.getHeightById(tx.blockId)
    currentHeight <- headersDao.getLastN(1).map(_.headOption.map(_.height).getOrElse(0))
    info = TransactionSummaryInfo.fromDb(tx, h, currentHeight)
  } yield info).transact(xa)

  def getTxsByAddressId(addressId: String, p: Paging): F[List[TransactionInfo]] = for {
    _ <- Async.shift[F](ec)
    base16Id <- F.pure(from58to16(addressId))
    result <- getTxsByAddressIdResult(base16Id, p)
  } yield result

  private def getTxsByAddressIdResult(addressId: String, p: Paging): F[List[TransactionInfo]] = (for {
    txs <- transactionsDao.getTxsByAddressId(addressId, p.offset, p.limit)
    ids = txs.map(_.id)
    is <- inputDao.findAllByTxsId(ids)
    os <- outputDao.findAllByTxsId(ids)
  } yield TransactionInfo.extractInfo(txs, is ,os)).transact(xa)

  def countTxsByAddressId(addressId: String): F[Long] = for {
    _ <- Async.shift[F](ec)
    base16Id <- F.pure(from58to16(addressId))
    result <- getTxsCountByAddressIdResult(base16Id)
  } yield result

  private def getTxsCountByAddressIdResult(addressId: String): F[Long] =
    transactionsDao.countTxsByAddressId(addressId).transact(xa)

}