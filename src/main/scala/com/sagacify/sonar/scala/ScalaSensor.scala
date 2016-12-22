package com.sagacify.sonar.scala

import scala.io.Source
import scala.collection.JavaConversions._
import org.sonar.api.batch.fs.FileSystem
import org.sonar.api.batch.Sensor
import org.sonar.api.batch.SensorContext
import org.sonar.api.measures.{CoreMetrics => CM}
import org.sonar.api.resources.Project

class ScalaSensor(scala: Scala, fs: FileSystem) extends Sensor {

  def shouldExecuteOnProject(project: Project): Boolean = {
    fs.hasFiles(fs.predicates().hasLanguage(scala.getKey))
  }

  def analyse(project: Project, context: SensorContext): Unit = {
    val charset = fs.encoding().toString
    val version = "2.11.8"

    val inputFiles = fs.inputFiles(fs.predicates().hasLanguage(scala.getKey))

    inputFiles.foreach { inputFile =>
      context.saveMeasure(inputFile, CM.FILES, 1.0)

      val sourceCode = Source.fromFile(inputFile.file, charset).mkString
      val tokens = Scala.tokenize(sourceCode, version)

      context.saveMeasure(inputFile, CM.COMMENT_LINES, Measures.countCommentLines(tokens))
      context.saveMeasure(inputFile, CM.NCLOC, Measures.countNCLoC(tokens))
      context.saveMeasure(inputFile, CM.CLASSES, Measures.countClasses(tokens))

      Scala.parse(sourceCode, version).map { ast =>
        val functions = Measures.extractFunctions(ast)
        context.saveMeasure(inputFile, CM.FUNCTIONS, functions.length)
        context.saveMeasure(inputFile, CM.COMPLEXITY, functions.map(Measures.calculateComplexity).sum)
      } getOrElse {
        throw new RuntimeException("Source code does not contain AST")
      }

      // Unsupported
      // CM.FUNCTION_COMPLEXITY

      // context.saveMeasure(input, CM.ACCESSORS, accessors)
      // context.saveMeasure(input, CM.COMPLEXITY_IN_FUNCTIONS, complexityInMethods)
      // context.saveMeasure(input, CM.COMPLEXITY_IN_CLASSES, fileComplexity)
      // context.saveMeasure(input, CM.PUBLIC_API, publicApiChecker.getPublicApi())
      // context.saveMeasure(input, CM.PUBLIC_DOCUMENTED_API_DENSITY, publicApiChecker.getDocumentedPublicApiDensity())
      // context.saveMeasure(input, CM.PUBLIC_UNDOCUMENTED_API, publicApiChecker.getUndocumentedPublicApi())
    }
  }
}

