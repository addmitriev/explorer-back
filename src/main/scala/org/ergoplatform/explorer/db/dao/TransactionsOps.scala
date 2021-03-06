package org.ergoplatform.explorer.db.dao

import cats.data.NonEmptyList
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import org.ergoplatform.explorer.db.models.Transaction

object TransactionsOps {

  val fields = Seq(
    "id",
    "header_id",
    "coinbase",
    "timestamp"
  )

  val fieldsString = fields.mkString(", ")
  val holdersString = fields.map(_ => "?").mkString(", ")
  val fieldsFr = Fragment.const(fieldsString)
  val insertSql = s"INSERT INTO node_transactions ($fieldsString) VALUES ($holdersString)"

  def findAllByBlockId(blockId: String)(implicit c: Composite[Transaction]): Query0[Transaction] =
    (fr"SELECT" ++ fieldsFr ++ fr"FROM node_transactions WHERE header_id = $blockId").query[Transaction]

  def countTxsNumbersByBlocksIds(blockIds: NonEmptyList[String]): Query0[(String, Long)] =
    (fr"SELECT header_id, count(*) FROM node_transactions WHERE" ++
      Fragments.in(fr"header_id", blockIds) ++ fr"GROUP BY header_id").query[(String, Long)]

  def getTxsByAddressId(addressId: String, offset: Int, limit: Int)
                       (implicit c: Composite[Transaction]): Query0[Transaction] =
    fr"""
        SELECT t.id, t.header_id, t.coinbase, t.timestamp
        FROM node_transactions t
        WHERE EXISTS (
          SELECT 1
          FROM node_outputs os
          WHERE (os.tx_id = t.id AND os.hash = $addressId)
        )
        OFFSET ${offset.toLong} LIMIT ${limit.toLong};
      """.query[Transaction]

  def countTxsByAddressId(addressId: String): Query0[Long] = {
      fr"""
         SELECT COUNT(t.id)
         FROM node_transactions t
         WHERE EXISTS (
           SELECT 1
           FROM node_outputs os
           WHERE (os.tx_id = t.id AND os.hash = $addressId)
         )
         """.query[Long]
  }

  def insert: Update[Transaction] = Update[Transaction](insertSql)

  def select(id: String): Query0[Transaction] =
    (fr"SELECT" ++ fieldsFr ++ fr"FROM node_transactions WHERE id = $id").query[Transaction]

  def searchById(substring: String): Query0[String] =
    fr"SELECT id FROM node_transactions WHERE id LIKE ${"%" + substring + "%" }".query[String]
}
