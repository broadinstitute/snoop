package org.broadinstitute.dsde.snoop.model

import java.sql.Timestamp

/**
 * Created by dvoet on 3/30/15.
 */
case class Submission
(
  id: String,
  submissionId: String,
  submissionDate: Option[Timestamp] = None,
  modifiedDate: Option[Timestamp] = None,
  callbackUri: Option[String],
  status: String
)
