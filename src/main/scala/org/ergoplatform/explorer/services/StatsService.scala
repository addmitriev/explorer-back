package org.ergoplatform.explorer.services

import cats._
import cats.effect._
import cats.implicits._
import doobie.implicits._
import doobie.util.transactor.Transactor
import org.ergoplatform.explorer.db.dao.StatsDao
import org.ergoplatform.explorer.db.models.StatRecord
import org.ergoplatform.explorer.http.protocol.{BlockchainInfo, ChartSingleData, StatsSummary, UsdPriceInfo}

import scala.concurrent.ExecutionContext

trait StatsService[F[_]] {

  def findLastStats: F[Option[StatsSummary]]

  def findBlockchainInfo: F[Option[BlockchainInfo]]

  def totalCoinsForDuration(daysBack: Int): F[List[ChartSingleData[Long]]]

  def avgBlockSizeForDuration(daysBack: Int): F[List[ChartSingleData[Long]]]

  def marketPriceUsdForDuration(daysBack: Int): F[List[ChartSingleData[UsdPriceInfo]]]
}

class StatsServiceIOImpl[F[_]](xa: Transactor[F], ec: ExecutionContext)
                        (implicit F: Monad[F], A: Async[F]) extends StatsService[F] {

  val statsDao = new StatsDao

  override def findLastStats: F[Option[StatsSummary]] = for {
    _ <- Async.shift[F](ec)
    result <- statsDao.findLast.map(statRecordToStatsSummary).transact[F](xa)
  } yield result

  override def findBlockchainInfo: F[Option[BlockchainInfo]] = for {
    _ <- Async.shift[F](ec)
    result <- statsDao.findLast.map(statRecordToBlockchainInfo).transact[F](xa)
  } yield result

  override def totalCoinsForDuration(d: Int): F[List[ChartSingleData[Long]]] = for {
    _ <- Async.shift[F](ec)
    result <- statsDao.totalCoinsGroupedByDay(d).map(pairsToChartData).transact[F](xa)
  } yield result

  override def avgBlockSizeForDuration(d: Int): F[List[ChartSingleData[Long]]] = for {
    _ <- Async.shift[F](ec)
    result <- statsDao.avgBlockSizeGroupedByDay(d).map(pairsToChartData).transact[F](xa)
  } yield result

  def marketPriceUsdForDuration(d: Int): F[List[ChartSingleData[UsdPriceInfo]]] = for {
    _ <- Async.shift[F](ec)
    result <- statsDao.marketPriceGroupedByDay(d).map(statsToUsdPriceInfo).transact[F](xa)
  } yield result


  private def statRecordToStatsSummary(s: Option[StatRecord]): Option[StatsSummary] = s.map(StatsSummary.apply)

  private def statRecordToBlockchainInfo(s: Option[StatRecord]): Option[BlockchainInfo] = s.map(BlockchainInfo.apply)

  private def pairsToChartData(list: List[(Long, Long)]): List[ChartSingleData[Long]] =
    list.map{ case (ts, data) => ChartSingleData(ts, data)}

  private def statsToUsdPriceInfo(list: List[(Long, Long)]): List[ChartSingleData[UsdPriceInfo]] =
    list.map{ case (ts, price) => ChartSingleData(ts, UsdPriceInfo(price))}
}
