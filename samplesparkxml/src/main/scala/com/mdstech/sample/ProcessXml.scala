package com.mdstech.sample

import org.apache.spark.sql.{DataFrame, SparkSession}

/**
  * Created by Srini on 4/16/17.
  */
object ProcessXml {
  def main(args: Array[String]) = {
    val spark = SparkSession
      .builder()
      .appName("ProcessXml")
      .master("local")
      .getOrCreate()

    //load books xml file
    loadBooksData(spark)
  }

  def loadBooksData(spark: SparkSession) = {
    var df: DataFrame = null
    var newDf : DataFrame = null

    import spark.implicits._

    df = spark.read
      .format("xml")
      .option("rowTag", "book")
      .load("datafiles/books.xml")

    df.printSchema()

    df.createOrReplaceTempView("books")

    spark.sql(""" select extra.four.six.seven[0] from books where upper(description) like '%KIDS%' """).show()
  }
}
