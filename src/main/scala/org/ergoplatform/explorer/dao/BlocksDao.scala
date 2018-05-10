package org.ergoplatform.explorer.dao

import org.ergoplatform.explorer.models.Header

class BlocksDao extends BaseDoobieDao[String, Header] {

  override val table: String = "blocks"
  override val fields: Seq[String] = Seq(
    "id",
    "parent_id",
    "version",
    "height",
    "ad_proofs_root",
    "state_root",
    "transactions_root",
    "ts",
    "n_bits",
    "nonce",
    "votes",
    "equihash_solution"
  )
}
