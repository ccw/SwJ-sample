package chart;

import com.sas.graphics.components.AnalysisVariable;
import com.sas.graphics.components.ClassificationVariable;
import com.sas.graphics.components.GraphConstants;
import com.sas.graphics.components.GraphStyle;
import com.sas.graphics.components.barchart.BarChart;
import com.sas.graphics.components.barchart.BarChartTableDataModel;
import com.sas.graphics.components.barlinechart.BarLineChart;
import com.sas.graphics.components.barlinechart.BarLineChartTableDataModel;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.*;

/**
 *
 */
public class SasBarChart extends JFrame {

    public SasBarChart() throws Exception {
        super("Test Chart Viewer");
        setMinimumSize(new Dimension(600, 600));
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        TableModel data = new SampleDataGenerator().generate();
        BarLineChartTableDataModel model = new BarLineChartTableDataModel(data);
        BarLineChart chart = new BarLineChart(model, new GraphStyle(GraphStyle.STYLE_MAGNIFY));
        model.setCategoryVariable(new ClassificationVariable("LOT_ID", 10406));
        model.setLineResponseVariable(new AnalysisVariable("ATT1", GraphConstants.STATISTIC.valueOfName("STATISTIC_MEAN")));
        model.setLineResponse2Variable(new AnalysisVariable("ATT2", GraphConstants.STATISTIC.valueOfName("STATISTIC_MEAN")));

        getContentPane().add(chart, BorderLayout.CENTER);
    }

    public static void main(String[] args) throws Exception {
        new SasBarChart().setVisible(true);
    }

}
