package com.mdstech.sample

import java.util.Properties

import org.apache.spark.sql.types.{DataTypes, Metadata, StructField, StructType}
import org.apache.spark.sql.{DataFrame, SQLContext, SparkSession}

import scala.collection.immutable.HashMap

/**
  * Created by Srini on 4/28/17.
  */
object ETLProcess {

  def main(args: Array[String]) = {
    val spark = SparkSession
      .builder()
      .appName("ETLProcess")
      .master("local[*]")
      .getOrCreate()

    //load users csv file
    loadBooksData(spark)

//    createLargeCSV(spark)
  }

  def createLargeCSV(spark: SparkSession) = {

    var df: DataFrame = null

    import spark.implicits._
    case class Person(id: Long, first: String, last: String)

    df = spark.range(1 ,1000000).map(i => (i, s"first_$i", s"last_$i")).select("_1", "_2", "_3")

    df.coalesce(1).write.format("com.databricks.spark.csv").option("header", "true")
      .option("delimiter", "|").save("datafiles/users100.csv")
  }



  def loadBooksData(spark: SparkSession) = {
    var df: DataFrame = null
    var newDf : DataFrame = null

    import spark.implicits._

    val customSchema : StructType = StructType(Array(
      StructField("id", DataTypes.IntegerType, true, Metadata.empty),
      StructField("fname", DataTypes.StringType, true, Metadata.empty),
      StructField("lname", DataTypes.StringType, true, Metadata.empty)
    ));

    df = spark.read
      .format("com.databricks.spark.csv")
        .schema(customSchema)
      .option("inferSchema", "true")
      .option("header", "true")
        .option("delimiter", "|")
      .load("datafiles/users1M.csv")

    df.printSchema()

    df.createOrReplaceTempView("users")

//    spark.sql(""" select * from users where upper(fname) like '%RUBY%' """).show()
    val properties: Properties =  new java.util.Properties
    properties.setProperty("user", "postgres")
    properties.setProperty("password", "postgres")
    properties.setProperty("driver", "org.postgresql.Driver")

    df.write
      .option("driver", "org.postgresql.Driver").mode("append")
      .jdbc("jdbc:postgresql://localhost:5432/postgres", "members", properties)
  }
}
