package net.packsam.telemetrycsvmerger;

import com.opencsv.CSVWriter;
import net.packsam.telemetrycsvmerger.model.DataColumn;
import net.packsam.telemetrycsvmerger.model.DataSet;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Stream;

import static com.opencsv.ICSVWriter.NO_ESCAPE_CHARACTER;
import static com.opencsv.ICSVWriter.NO_QUOTE_CHARACTER;
import static net.packsam.telemetrycsvmerger.DataSetReader.DATETIME_FORMAT;

public class DataSetWriter {
	private final Map<String, BigDecimal> factors;

	public DataSetWriter(Map<String, BigDecimal> factors) {
		this.factors = factors;
	}

	public void write(DataSet dataSet, File file) {
		try (var csvWriter = new CSVWriter(new FileWriter(file), ',', NO_QUOTE_CHARACTER, NO_ESCAPE_CHARACTER, "\r\n")) {
			var lines = new LinkedList<String[]>();
			DataColumn[] columns = dataSet.columns();

			lines.add(writeHeader(columns));
			for (var dataRow : dataSet.data()) {
				lines.add(writeData(dataRow, columns));
			}

			csvWriter.writeAll(lines);
		} catch (IOException e) {
			throw new RuntimeException("Can not write file", e);
		}
	}

	private String[] writeData(Map<String, Comparable<? extends Comparable<?>>> dataRow, DataColumn[] columns) {
		return Stream.of(columns)
				.map(column -> {
					String columnName = column.name();
					var cellValue = dataRow.get(columnName);
					return switch (column.type()) {
						case NUMERIC -> applyFactor((BigDecimal) cellValue, columnName).toPlainString();
						case DATETIME -> ((LocalDateTime) cellValue).format(DATETIME_FORMAT);
					};
				})
				.toArray(String[]::new);
	}

	private BigDecimal applyFactor(BigDecimal value, String columnName) {
		BigDecimal factor = factors.get(columnName);
		if (factor == null) {
			return value;
		}

		return value.multiply(factor);
	}

	private String[] writeHeader(DataColumn[] columns) {
		return Stream.of(columns)
				.map(DataColumn::name)
				.toArray(String[]::new);
	}
}
