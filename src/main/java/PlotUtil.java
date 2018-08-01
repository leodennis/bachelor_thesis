import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;

/**
 * Source to original file:
 */
public class PlotUtil {

	public static void plot(double[][] values, String[] labels, String name, String title) {
		double[] index = new double[values[0].length];
		for (int i = 0; i < values[0].length; i++)
			index[i] = i;
		int min = minValue(values);
		int max = maxValue(values);
		final XYSeriesCollection dataSet = new XYSeriesCollection();
		for (int i = 0; i < values.length; i++) {
			addSeries(dataSet, index, values[i], labels[i]);
		}
		final JFreeChart chart = ChartFactory.createXYLineChart(
				title, // chart title
				"Index", // x axis label
				name, // y axis label
				dataSet, // data
				PlotOrientation.VERTICAL,
				true, // include legend
				true, // tooltips
				false // urls
		);
		XYPlot xyPlot = chart.getXYPlot();
		// X-axis
		final NumberAxis domainAxis = (NumberAxis) xyPlot.getDomainAxis();
		domainAxis.setRange((int) index[0], (int) (index[index.length - 1] + 2));
		domainAxis.setTickUnit(new NumberTickUnit(20));
		domainAxis.setVerticalTickLabels(true);
		// Y-axis
		final NumberAxis rangeAxis = (NumberAxis) xyPlot.getRangeAxis();
		rangeAxis.setRange(min, max);
		rangeAxis.setTickUnit(new NumberTickUnit(50));
		final ChartPanel panel = new ChartPanel(chart);
		final JFrame f = new JFrame();
		f.add(panel);
		f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		f.pack();
		f.setVisible(true);
	}

	public static void plot(double[] predicts, double[] actuals, String name, String title) {
		plot(new double[][]{predicts, actuals}, new String[]{"Predicts", "Actuals"}, name, title);
	}

	public static void plot(double[] predicts, String name) {
		double[] index = new double[predicts.length];
		for (int i = 0; i < predicts.length; i++)
			index[i] = i;
		int min = minValue(predicts);
		int max = maxValue(predicts);
		final XYSeriesCollection dataSet = new XYSeriesCollection();
		addSeries(dataSet, index, predicts, "Predicts");
		final JFreeChart chart = ChartFactory.createXYLineChart(
				"Prediction Result", // chart title
				"Index", // x axis label
				name, // y axis label
				dataSet, // data
				PlotOrientation.VERTICAL,
				true, // include legend
				true, // tooltips
				false // urls
		);
		XYPlot xyPlot = chart.getXYPlot();
		// X-axis
		final NumberAxis domainAxis = (NumberAxis) xyPlot.getDomainAxis();
		domainAxis.setRange((int) index[0], (int) (index[index.length - 1] + 2));
		domainAxis.setTickUnit(new NumberTickUnit(20));
		domainAxis.setVerticalTickLabels(true);
		// Y-axis
		final NumberAxis rangeAxis = (NumberAxis) xyPlot.getRangeAxis();
		rangeAxis.setRange(min, max);
		rangeAxis.setTickUnit(new NumberTickUnit(50));
		final ChartPanel panel = new ChartPanel(chart);
		final JFrame f = new JFrame();
		f.add(panel);
		f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		f.pack();
		f.setVisible(true);
	}

	private static void addSeries (final XYSeriesCollection dataSet, double[] x, double[] y, final String label){
		final XYSeries s = new XYSeries(label);
		for( int j = 0; j < x.length; j++ ) s.add(x[j], y[j]);
		dataSet.addSeries(s);
	}

	private static int minValue (double[] predicts, double[] actuals) {
		double min = Integer.MAX_VALUE;
		for (int i = 0; i < predicts.length; i++) {
			if (min > predicts[i]) min = predicts[i];
			if (min > actuals[i]) min = actuals[i];
		}
		return (int) (min * 0.98);
	}

	private static int maxValue (double[] predicts, double[] actuals) {
		double max = Integer.MIN_VALUE;
		for (int i = 0; i < predicts.length; i++) {
			if (max < predicts[i]) max = predicts[i];
			if (max < actuals[i]) max = actuals[i];
		}
		return (int) (max * 1.02);
	}

	private static int minValue (double[] predicts) {
		double min = Integer.MAX_VALUE;
		for (int i = 0; i < predicts.length; i++) {
			if (min > predicts[i]) min = predicts[i];
		}
		return (int) (min * 0.98);
	}

	private static int maxValue (double[] predicts) {
		double max = Integer.MIN_VALUE;
		for (int i = 0; i < predicts.length; i++) {
			if (max < predicts[i]) max = predicts[i];
		}
		return (int) (max * 1.02);
	}

	private static int minValue (double[][] values) {
		double min = Integer.MAX_VALUE;
		for (int i = 0; i < values[0].length; i++) {
			for (int j = 0; j < values.length; j++) {
				if (min > values[j][i]) min = values[j][i];
			}
		}
		return (int) (min * 0.98);
	}

	private static int maxValue (double[][] values) {
		double max = Integer.MIN_VALUE;
		for (int i = 0; i < values[0].length; i++) {
			for (int j = 0; j < values.length; j++) {
				if (max < values[j][i]) max = values[j][i];
			}
		}
		return (int) (max * 1.02);
	}

}
