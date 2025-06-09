import CsvUtils.{readFromFileAsList, readFromFileAsListSkipHeader, writeToFile}

import java.io.File
import java.nio.file.{Files, Paths}
import scala.io.StdIn.readLine

object Main {
  def main(args: Array[String]): Unit = {

    println("Enter separated by space (Path to the movies description file)" +
      " (Path to the training dataset directory) (Report output file path) please")
    println("Example: C:\\Folder1\\movie_titles.txt C:\\Folder1\\training_set\\ C:\\Folder2\\result.csv")

    // task requirements
    val minYear = "1970"
    val maxYear = "1990"
    val minReviewsCount = 1000

    val enteredData = readLine().split(" ").toList
    val movies_folder_path = enteredData(0)
    val training_set_folder_path = enteredData(1)
    val report_output_path = enteredData(2)

//    val movies_folder_path = "C:\\Folder1\\movie_titles.txt"
//    val training_set_folder_path = "C:\\Folder1\\training_set\\"
//    val report_output_path = "C:\\Folder2\\result.csv"


    if (Files.exists(Paths.get(movies_folder_path))) {

      val movieTitlesFilePath = movies_folder_path
      val movieTitlesFile = new File(movieTitlesFilePath)


      val movieList: List[Movie] =readFromFileFilterByYearRange(movieTitlesFile, minYear, maxYear)
      val movieAndRatingFilePath = getMovieRatingFilePath(movieList, training_set_folder_path)
      //println(movieAndRatingFilePath)
      val movieAndAllReviews = getMovieAllReviews(movieAndRatingFilePath, minReviewsCount)
      //println(movieAndAllReviews)
      val movieAvgRatingNumberReviews = getMovieAvgRating(movieAndAllReviews)
      val movieInfoList = formMovieInfo(movieAvgRatingNumberReviews)
      //println(movieInfoList)

      generateReport(movieInfoList, report_output_path)

    } else
      println("File or path do not exists")
  }


  def readFromFileFilterByYearRange(file: File, minYear: String, maxYear: String): List[Movie] = {
    readFromFileAsList(file)
      .filter(_.values().toList(1) >= minYear)
      .filter(_.values().toList(1) <= maxYear)
      .map(record => Movie(record.values().toList.head, record.values().toList(1).toInt, record.values().toList(2)))
  }

  def getMovieRatingFilePath(movies: List[Movie], folderPath: String): Map[Movie, String] = {
    movies.flatMap { movie =>
      movie.id match {
        case id if id.length == 1 => Map(movie -> (folderPath + "mv_000000" + id + ".txt"))
        case id if id.length == 2 => Map(movie -> (folderPath + "mv_00000" + id + ".txt"))
        case id if id.length == 3 => Map(movie -> (folderPath + "mv_0000" + id + ".txt"))
        case id if id.length == 4 => Map(movie -> (folderPath + "mv_000" + id + ".txt"))
        case id if id.length == 5 => Map(movie -> (folderPath + "mv_00" + id + ".txt"))
        case id if id.length == 6 => Map(movie -> (folderPath + "mv_0" + id + ".txt"))
        case id if id.length == 7 => Map(movie -> (folderPath + "mv_" + id + ".txt"))
        case _ => Map()
      }
    }.toMap
  }

  def getMovieAllReviews(movieRatingFilePath: Map[Movie, String], minReviewsCount: Int): Map[Movie, List[String]] = {
    movieRatingFilePath.map { movie =>
      if (Files.exists(Paths.get(movie._2))) {
        Some(Map(movie._1 -> readFromFileAsListSkipHeader(new File(movie._2)).map(_.get(1))))
      } //get only user rating for movie
      else {
        None // file does not exist
      }
    }.filter(_.nonEmpty)
      .flatMap(el => el.get)
      .filter(_._2.size > minReviewsCount).toMap  // minimum reviews count
  }

  def getMovieAvgRating(movieAllReviews: Map[Movie, List[String]]): Map[Movie, (Double, Int)] = {
    movieAllReviews.map { movie =>
      val reviewsCount = movie._2.size.toDouble
      val ratingSum = movie._2.map(_.toInt).sum
      val avgRating = if (reviewsCount != 0) ratingSum/reviewsCount else 0
      movie._1 -> (avgRating, reviewsCount.toInt)
    }
  }

  def formMovieInfo(movies: Map[Movie, (Double, Int)]): List[MovieInfo] = {
    movies.map { movie =>
      MovieInfo(movie._1.id, movie._1.releaseYear, movie._1.title, movie._2._1, movie._2._2)
    }
  }.toList.sortBy(_.avgRating).reverse  //sorting

  // avg presented as Double with 2 digits after comma
  def generateReport(movieInfoList: List[MovieInfo], filePath: String): Unit = {
    val listOfLines = movieInfoList.map { movie =>
      val avgRating = movie.avgRating
      List(movie.title, movie.releaseYear.toString,  f"$avgRating%.2f", movie.reviewsNumber.toString)
    }
    writeToFile(listOfLines, new File(filePath))
    println("Report has been generated into file " + filePath)
  }

}

case class Movie(id: String, releaseYear: Int, title: String)
case class MovieInfo(id: String, releaseYear: Int, title: String, avgRating: Double, reviewsNumber: Int)

