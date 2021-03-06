
package com.microsoft.spark.insight.fetcher

import java.util.concurrent.TimeoutException

import org.apache.hadoop.conf.Configuration
import org.apache.log4j.Logger

import scala.concurrent.duration.{Duration, HOURS, SECONDS}
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.io.Source
import scala.util.{Failure, Success, Try}


/**
  * A fetcher that gets Spark-related data from a combination of the Spark monitoring REST API and Spark event logs.
  */
object SparkFetcher extends Fetcher {

  import ExecutionContext.Implicits.global

  private val logger: Logger = Logger.getLogger(SparkFetcher.getClass)

  private val DEFAULT_TIMEOUT = Duration(1, HOURS)

  private[fetcher] lazy val hadoopConfiguration: Configuration = new Configuration()

  private[fetcher] lazy val sparkRestClient: SparkRestClient = new SparkRestClient()

  override def fetchData(trackingUrl: String): SparkApplicationData = {
    doFetchData(trackingUrl) match {
      case Success(data) => data
      case Failure(e) => {
        logger.error(e)
        throw e
      }
    }
  }

  private def doFetchData(trackingUrl: String): Try[SparkApplicationData] = {
    Try {
      Await.result(doFetchSparkApplicationData(trackingUrl), DEFAULT_TIMEOUT)
    }.transform(
      data => {
        logger.info(s"Succeeded fetching data for ${trackingUrl}")
        Success(data)
      },
      e => {
        logger.warn(s"Failed fetching data for ${trackingUrl}." + " I will retry after some time! " + "Exception Message is: " + e.getMessage)
        Failure(e)
      }
    )
  }

  private def doFetchSparkApplicationData(trackingUrl: String): Future[SparkApplicationData] = {
    doFetchDataUsingRestClients(trackingUrl)
  }

  private def doFetchDataUsingRestClients(trackingUrl: String): Future[SparkApplicationData] = Future {
    Await.result(sparkRestClient.fetchData(trackingUrl), DEFAULT_TIMEOUT)
  }
}
