package net.packsam.telemetrycsvmerger;

import net.packsam.telemetrycsvmerger.model.DataColumn;
import net.packsam.telemetrycsvmerger.model.DataSet;
import org.apache.commons.collections4.SetUtils;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TelemetryComparator {
	public static void main(String[] args) {
		if (args.length != 2) {
			throw new IllegalArgumentException("You have to pass 2 telemetry files to compare.");
		}

		var file1 = new File(args[0]);
		var file2 = new File(args[1]);

		var reader = new DataSetReader();
		DataSet dataSet1 = reader.parseFile(file1);
		DataSet dataSet2 = reader.parseFile(file2);

		compareColumns(dataSet1.columns(), dataSet2.columns());
		compareData(dataSet1.data(), dataSet2.data(), dataSet1.columns());

		System.out.println("Files are equal.");
	}

	private static void compareData(List<Map<String, Comparable<? extends Comparable<?>>>> data1, List<Map<String, Comparable<? extends Comparable<?>>>> data2, DataColumn[] columns) {
		if (data1.size() != data2.size()) {
			throw new RuntimeException("Number of data rows in CSV files does not match.");
		}

		var columnNames = Stream.of(columns)
				.map(DataColumn::name)
				.toList();

		for (var i = 0; i < data1.size(); i++) {
			var row1 = data1.get(i);
			var row2 = data2.get(i);

			for (var column : columnNames) {
				var value1 = row1.get(column);
				var value2 = row2.get(column);

				if (value1 instanceof BigDecimal bigDecimal1 && value2 instanceof BigDecimal bigDecimal2) {
					if (bigDecimal1.setScale(10, RoundingMode.HALF_UP).compareTo(bigDecimal2.setScale(10, RoundingMode.HALF_UP)) != 0) {
						throw new RuntimeException("Decimal value in CSV files does not match. (Row " + i + ", Column " + column + ": " + value1 + " != " + value2 + ")");
					}
				} else if (value1 instanceof LocalDateTime localDateTime1 && value2 instanceof LocalDateTime localDateTime2) {
					if (localDateTime1.compareTo(localDateTime2) != 0) {
						throw new RuntimeException("Date value in CSV files does not match. (Row " + i + ", Column " + column + ": " + value1 + " != " + value2 + ")");
					}
				} else if (!value1.equals(value2)) {
					throw new RuntimeException("Data in CSV files does not match. (Row " + i + ", Column " + column + ": " + value1 + " != " + value2 + ")");
				}
			}
		}
	}

	private static void compareColumns(DataColumn[] columns1, DataColumn[] columns2) {
		if (columns1.length != columns2.length) {
			throw new IllegalArgumentException("The number of columns is not equal.");
		}

		var columnNames1 = Stream.of(columns1)
				.map(DataColumn::name)
				.collect(Collectors.toSet());
		var columnNames2 = Stream.of(columns2)
				.map(DataColumn::name)
				.collect(Collectors.toSet());

		var disjunctColumnNames = SetUtils.disjunction(columnNames1, columnNames2);
		if (!disjunctColumnNames.isEmpty()) {
			throw new IllegalArgumentException("The column names are not equal. (" + disjunctColumnNames + ")");
		}
	}
}
