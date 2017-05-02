package com.mdstech.sample

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql._

/**
  * Created by Srini on 5/1/17.
  */
object ReadMultilineRecord {
  def main(args: Array[String]) = {
    val spark = SparkSession
      .builder()
      .appName("ReadMultilineRecord")
      .master("local[*]")
      .getOrCreate()

    readMultilineData(spark)
  }

  abstract class Base extends Serializable with Product
  case class DealStart(num:Long, name: String, amount: Double, principal: Double, interest: Double) extends Base
  case class DealEnd(num:Long, name: String) extends Base
  case class DealLoan(num:Long, name: String, amount: Double) extends Base

  def readMultilineData(spark: SparkSession) = {
    var df: DataFrame = null

    df = spark.read.format("com.databricks.spark.csv").option("header", "false").option("delimiter", "|").load("datafiles/multiline.csv")
    df.createOrReplaceTempView("multilines")

    import spark.implicits._

    var filteredDF: DataFrame = df.filter(r =>r.get(0).equals("10")).map {
      case Row(num:String, name:String, amount: String, principal: String, interest: String) =>
        DealStart(num = num.toLong, name= name, amount = amount.toDouble, principal = principal.toDouble, interest = interest.toDouble)
    }.toDF()

    var filteredDF1: DataFrame = df.filter(r =>r.get(0).equals("20")).map {
      case Row(num: String, name: String, amount: String, null, null) =>
        DealLoan(num = num.toLong, name = name, amount = amount.toDouble)
    }.toDF()

//    var filteredDF2: DataFrame = df.filter(r =>r.get(0).equals("80")).map {
//      case Row(num:String, name:String, null, null, null) =>
//        DealEnd(num = num.toLong, name= name)
//    }.toDF()

    filteredDF.write
      .format("com.databricks.spark.csv")
      .option("header", "true")
      .save("datafiles/dealsstart.csv")

    filteredDF1.write
      .format("com.databricks.spark.csv")
      .option("header", "true")
      .save("datafiles/dealsloan.csv")
  }
}
