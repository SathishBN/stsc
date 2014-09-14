package stsc.general.statistic.cost.function;

import java.text.ParseException;

import stsc.common.Settings;
import stsc.general.statistic.Statistics;
import stsc.general.testhelper.TestStatisticsHelper;
import junit.framework.TestCase;

public class CostWeightedProductFunctionTest extends TestCase {
	public void testCostWeightedProductFunction() throws ParseException {
		final Statistics statistics = TestStatisticsHelper.getStatistics();

		CostWeightedProductFunction function = new CostWeightedProductFunction();
		function.addParameter("getKelly", 0.8);
		final Double result = function.calculate(statistics);
		assertEquals(0.277441, result, Settings.doubleEpsilon);
	}
}