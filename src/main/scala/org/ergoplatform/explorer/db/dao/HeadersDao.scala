package org.ergoplatform.explorer.db.dao

import cats.data._, cats.implicits._
import doobie._, doobie.implicits._, doobie.postgres.implicits._
import org.ergoplatform.explorer.db.models.Header


class HeadersDao {

  val fields = HeadersOps.fields

  def insert(h: Header): ConnectionIO[Header] = {
    HeadersOps
      .insert
      .withUniqueGeneratedKeys[Header](fields: _*)(h)
  }

  def insertMany(list: List[Header]): ConnectionIO[List[Header]] = {
    HeadersOps
      .insert
      .updateManyWithGeneratedKeys[Header](fields: _*)(list)
      .compile
      .to[List]
  }

  def update(h: Header): ConnectionIO[Header] = {
    HeadersOps
      .update
      .withUniqueGeneratedKeys[Header](fields: _*)(h -> h.id)
  }

  def updateMany(list: List[Header]): ConnectionIO[List[Header]] = {
    HeadersOps
      .update
      .updateManyWithGeneratedKeys[Header](fields: _*)(list.map(h => h -> h.id))
      .compile
      .to[List]
  }

  def find(id: String): ConnectionIO[Option[Header]] = HeadersOps.select(id).option

  def findByParentId(parentId: String): ConnectionIO[Option[Header]] = HeadersOps.selectByParentId(parentId).option

  def get(id: String): ConnectionIO[Header] = find(id).flatMap {
    case Some(h) => h.pure[ConnectionIO]
    case None => doobie.free.connection.raiseError(
      new NoSuchElementException(s"Cannot find header with id = $id")
    )
  }

  def getByParentId(parentId: String): ConnectionIO[Header] = findByParentId(parentId).flatMap {
    case Some(h) => h.pure[ConnectionIO]
    case None => doobie.free.connection.raiseError(
      new NoSuchElementException(s"Cannot find header with parentId = $parentId")
    )
  }

  def count(startTs: Long, endTs: Long): ConnectionIO[Long] = HeadersOps.count(startTs, endTs).unique

  def getLast(limit: Int = 20): ConnectionIO[List[Header]] = HeadersOps.selectLast(limit).to[List]

  def getHeightById(id: String): ConnectionIO[Long] = HeadersOps.selectHeight(id).option.flatMap {
    case Some(h) => h.pure[ConnectionIO]
    case None => (-1L).pure[ConnectionIO]
  }

  def list(offset: Int = 0,
           limit: Int = 20,
           sortBy: String = "height",
           sortOrder: String = "DESC",
           startTs: Long,
           endTs: Long): ConnectionIO[List[Header]] =
    HeadersOps.list(offset, limit, sortBy, sortOrder, startTs, endTs).to[List]

  /** Search headers by the fragment of the identifier */
  def searchById(substring: String): ConnectionIO[List[Header]] =
    HeadersOps.searchById(substring).to[List]

}
