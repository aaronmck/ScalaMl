/**
 * Copyright (c) 2013-2015  Patrick Nicolas - Scala for Machine Learning - All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License") you may not use this file 
 * except in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software is distributed on an 
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * 
 * The source code in this file is provided by the author for the sole purpose of illustrating the 
 * concepts and algorithms presented in "Scala for Machine Learning". 
 * ISBN: 978-1-783355-874-2 Packt Publishing.
 * 
 * Version 0.99
 */
package org.scalaml.app.chap8

import scala.util.Try
import scala.collection._

import org.apache.log4j.Logger
	
import org.scalaml.workflow.data.DataSource
import org.scalaml.trading.GoogleFinancials
import org.scalaml.supervised.svm.kernel.RbfKernel
import org.scalaml.supervised.svm.formulation.SVRFormulation
import org.scalaml.supervised.svm.{SVMConfig, SVM}
import org.scalaml.stats.XTSeries
import org.scalaml.core.Types.{ScalaMl, emptyString}
import org.scalaml.supervised.regression.linear.SingleLinearRegression
import org.scalaml.util.{FormatUtils, DisplayUtils, LoggingUtils}
import org.scalaml.app.Eval
import LoggingUtils._, DisplayUtils._, SingleLinearRegression._, GoogleFinancials._, ScalaMl._, FormatUtils._


		/**
		 * '''Purpose:'''Singleton to evaluate the support vector machine regression
		 * @author Patrick Nicolas
		 * @see Scala for Machine Learning Chapter 8 ''Kernel Models and Support Vector Machines''.
		 */
object SVREval extends Eval {


		/**
		 * Name of the evaluation 
		 */
	val name: String = "SVREval"
	
	private val path = "resources/data/chap8/SPY.csv"
	private val C = 12
	private val GAMMA = 0.3
	private val EPS = 1e-3
	private val EPSILON = 2.5
	private val NUM_DISPLAYED_VALUES = 128

		/** Execution of the scalatest for evaluating the support vector regression.
		 * This method is invoked by the  actor-based test framework function, ScalaMlTest.evaluate
		 * 	
		 * Exceptions thrown during the execution of the tests are caught by the wrapper or handler
		 * test method in Eval trait defined as follows:
		 * {{{
		 *    def test(args: Array[String]) =
		 *      Try(run(args)) match {
		 *        case Success(n) => ...
		 *        case Failure(e) => ...
		 * }}}
		 * The tests can be executed through ''sbt run'' or individually by calling 
		 * ''TestName.test(args)'' (i.e. DKalmanEval.test(Array[String]("IBM") )
		 * 
		 * @param args array of arguments used in the test
		 */
	override protected def run(args: Array[String]): Int = {
		show(s"$header Support Vector Regression")
		
		val config = SVMConfig(new SVRFormulation(C, EPSILON), new RbfKernel(GAMMA))
		
		def getLabeledData(numObs: Int): Try[(Vector[DblArray], DblVector)] = Try {
			val y = Vector.tabulate(numObs)(_.toDouble)
			val xt = Vector.tabulate(numObs)(Array[Double](_))
			(xt, y)
		}
		  
		(for {
			price <-  DataSource(path, false, true, 1) get close
			(xt, y) <- getLabeledData(price.size)
			linRg <- SingleLinearRegression[Double](y, price)
			svr <- SVM[Double](config, xt, price)
		} 
		yield {
			show(s"First $NUM_DISPLAYED_VALUES time series datapoints\n")
			display("Support Vector vs. Linear Regression", 
							collect(svr, linRg, price),
							List[String]("Support vector regression", "Linear regression", "Stock Price"))
			1
		}).get
	}
   
		/**
		 * Collect the data generated by the simple regression and and support vector regression models
		 */
	private def collect(
			svr: SVM[Double], 
			lin: SingleLinearRegression[Double], 
			price: Vector[Double]): List[Vector[DblPair]] = {
	  
		import scala.language.postfixOps

		val pfSvr = svr |>
		val pfLin = lin |>
	 
			// Create buffers to collect data from the two regression models
		val svrResults = new mutable.ArrayBuffer[DblPair]
		val linResults = new mutable.ArrayBuffer[DblPair]
	
		val r = Range(1, price.size - 80)
		val selectedPrice = r.map(x => (x.toDouble, price(x)))
	  
		r.foreach( n => {
			for {
				x <- pfSvr(n.toDouble) 
				if pfLin.isDefinedAt(n)
					y <- pfLin(n)
			} yield  {
				svrResults.append((n.toDouble, x))
				linResults.append((n.toDouble, y))
			}
		})
		show(s"Price\n${price.mkString(",")}")
		show(s"Linear Regression\n${linResults.map(_._2).mkString(",")}")
		show(s"Support vector regression\n${svrResults.map(_._2).mkString(",")}")
		
		List[Vector[DblPair]](svrResults.toVector, linResults.toVector, selectedPrice.toVector)
	}

	
	
	private def display(label: String, xs: List[Vector[DblPair]], lbls: List[String]): Unit = {
		import org.scalaml.plots.{ScatterPlot, LightPlotTheme, Legend}
		require( !xs.isEmpty, s"$name Cannot display an undefined time series")
       
		val plotter = new ScatterPlot(Legend("SVREval", "SVM regression SPY prices", label, "SPY"), new LightPlotTheme)
		plotter.display(xs, lbls, 340, 250)
	}
}


// --------------------------  EOF -----------------------------------------------