package net.packsam.telemetrycsvmerger;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import net.packsam.telemetrycsvmerger.model.DataColumn;
import net.packsam.telemetrycsvmerger.model.DataSet;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Stream;

public class DataSetReader {
	static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSSSSS]");

	public DataSet parseFile(File file) {
		try (var csvReader = new CSVReader(new FileReader(file))) {
			var lines = csvReader.readAll();
			var iterator = lines.iterator();

			var columns = parseHeader(iterator.next());

			var allDataRows = new LinkedList<Map<String, Comparable<? extends Comparable<?>>>>();
			while (iterator.hasNext()) {
				var line = iterator.next();
				var data = parseData(line, columns);
				allDataRows.add(data);
			}

			return new DataSet(columns, allDataRows);
		} catch (IOException e) {
			throw new RuntimeException("Can not read file " + file, e);
		} catch (CsvException | ParseException e) {
			throw new RuntimeException("Can not parse file " + file, e);
		}
	}

	private Map<String, Comparable<? extends Comparable<?>>> parseData(String[] line, DataColumn[] columns) throws ParseException {
		if (line.length != columns.length) {
			throw new RuntimeException("Invalid number of columns in line");
		}

		var data = new LinkedHashMap<String, Comparable<? extends Comparable<?>>>();
		for (var i = 0; i < line.length; i++) {
			var column = columns[i];
			var cellString = line[i];

			var cellValue = switch (column.type()) {
				case GLOBALTIME, NUMERIC -> new BigDecimal(cellString);
				case DATETIME -> LocalDateTime.parse(cellString, DATETIME_FORMAT);
			};
			data.put(column.name(), cellValue);
		}

		return data;
	}

	private DataColumn[] parseHeader(String[] headerData) {
		return Stream.of(headerData)
				.map(columnName -> new DataColumn(
						columnName,
						getColumnType(columnName),
						getColumnAggregationType(columnName)
				))
				.toArray(DataColumn[]::new);
	}

	private DataColumn.Type getColumnType(String columnName) {
		if ("GlobalTime".equals(columnName)) {
			return DataColumn.Type.GLOBALTIME;
		} else if ("DateTime".equals(columnName)) {
			return DataColumn.Type.DATETIME;
		} else {
			return DataColumn.Type.NUMERIC;
		}
	}

	private DataColumn.AggregationType getColumnAggregationType(String columnName) {
		if (columnName.endsWith("[Time]")) {
			return DataColumn.AggregationType.INC;
		} else if ("GlobalTime".equals(columnName)) {
			return DataColumn.AggregationType.INC;
		} else if (columnName.startsWith("Total ")) {
			return DataColumn.AggregationType.INC;
		} else if (columnName.startsWith("Distance ")) {
			return DataColumn.AggregationType.INC;
		} else if (columnName.contains("GPSU[")) {
			return DataColumn.AggregationType.INC;
		} else if (columnName.contains(" Min ")) {
			return DataColumn.AggregationType.MIN;
		} else if (columnName.contains(" Max ")) {
			return DataColumn.AggregationType.MAX;
		} else {
			return DataColumn.AggregationType.NONE;
		}
	}

}
