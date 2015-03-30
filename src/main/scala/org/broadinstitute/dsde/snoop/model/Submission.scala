package org.broadinstitute.dsde.snoop.model

import java.sql.Timestamp

/**
 * Created by dvoet on 3/30/15.
 */
case class Submission
(
  submissionId: String,
  submissionDate: Option[Timestamp] = None,
  modifiedDate: Option[Timestamp] = None,
  callbackUri: String,
  status: String,
  id: Option[Int] = None
)
