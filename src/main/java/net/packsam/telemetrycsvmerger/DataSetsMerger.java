package net.packsam.telemetrycsvmerger;

import net.packsam.telemetrycsvmerger.model.DataColumn;
import net.packsam.telemetrycsvmerger.model.DataSet;
import org.apache.commons.collections4.SetUtils;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DataSetsMerger {
	public DataSet merge(List<DataSet> dataSets) {
		var allColumns = dataSets.stream()
				.map(DataSet::columns)
				.toList();
		var mergedColumns = mergeColumns(allColumns);
		var allData = dataSets.stream()
				.map(DataSet::data)
				.toList();
		var mergedData = mergeData(allData, mergedColumns);

		return new DataSet(mergedColumns, mergedData);
	}

	private DataColumn[] mergeColumns(List<DataColumn[]> allColumns) {
		Iterator<DataColumn[]> iterator = allColumns.iterator();

		DataColumn[] mergedColumns = iterator.next();
		Set<String> mergedColumnNames = Stream.of(mergedColumns)
				.map(DataColumn::name)
				.collect(Collectors.toSet());

		while (iterator.hasNext()) {
			DataColumn[] next = iterator.next();
			if (next.length != mergedColumns.length) {
				throw new RuntimeException("Number of header columns in CSV files does not match.");
			}

			Set<String> nextColumnNames = Stream.of(next)
					.map(DataColumn::name)
					.collect(Collectors.toSet());
			Set<String> disjunctColumnNames = SetUtils.disjunction(mergedColumnNames, nextColumnNames);
			if (!disjunctColumnNames.isEmpty()) {
				throw new RuntimeException("Header columns in CSV files do not match. (" + disjunctColumnNames + ")");
			}
		}

		return mergedColumns;
	}

	private List<Map<String, Comparable<? extends Comparable<?>>>> mergeData(List<List<Map<String, Comparable<? extends Comparable<?>>>>> allData, DataColumn[] columns) {
		var flattedMap = allData.stream()
				.flatMap(List::stream)
				.toList();
		var minValues = findMinValues(flattedMap, columns);
		var maxValues = findMaxValues(flattedMap, columns);

		var columnMap = Stream.of(columns)
				.collect(Collectors.toMap(DataColumn::name, column -> column));

		var iterator = allData.iterator();
		var firstDataSet = iterator.next();

		var mergedData = new LinkedList<Map<String, Comparable<? extends Comparable<?>>>>(
				mergeFirstDataSet(firstDataSet, columnMap, minValues, maxValues)
		);

		while (iterator.hasNext()) {
			var lastValues = getLastValues(mergedData);
			var lastValueDiffs = getLastValueDiffs(mergedData, lastValues);

			var nextDataSet = iterator.next();
			mergedData.addAll(
					mergeNextDataSet(nextDataSet, columnMap, minValues, maxValues, lastValues, lastValueDiffs)
			);
		}

		return mergedData;
	}

	private Collection<? extends Map<String, Comparable<? extends Comparable<?>>>> mergeFirstDataSet(
			List<Map<String, Comparable<? extends Comparable<?>>>> firstDataSet,
			Map<String, DataColumn> columnMap,
			Map<String, BigDecimal> minValues,
			Map<String, BigDecimal> maxValues
	) {
		//noinspection unchecked
		return (Collection<? extends Map<String, Comparable<? extends Comparable<?>>>>) firstDataSet.stream()
				.map(Map::entrySet)
				.map(entrySet -> entrySet.stream()
						.map(mapEntry -> mergeFirstDataSetEntry(mapEntry, columnMap.get(mapEntry.getKey()), minValues, maxValues))
						.collect(Collectors.toMap(
								Map.Entry::getKey,
								Map.Entry::getValue
						))
				)
				.toList();
	}

	private Map.Entry<String, Comparable<? extends Comparable<?>>> mergeFirstDataSetEntry(
			Map.Entry<String, Comparable<? extends Comparable<?>>> mapEntry,
			DataColumn column,
			Map<String, BigDecimal> minValues,
			Map<String, BigDecimal> maxValues
	) {
		var columnName = mapEntry.getKey();
		var aggregationType = column.aggregationType();
		return switch (aggregationType) {
			case INC, NONE -> mapEntry;
			case MIN -> Map.entry(columnName, minValues.get(columnName));
			case MAX -> Map.entry(columnName, maxValues.get(columnName));
		};
	}

	private Collection<? extends Map<String, Comparable<? extends Comparable<?>>>> mergeNextDataSet(
			List<Map<String, Comparable<? extends Comparable<?>>>> nextDataSet,
			Map<String, DataColumn> columnMap,
			Map<String, BigDecimal> minValues,
			Map<String, BigDecimal> maxValues,
			Map<String, BigDecimal> lastValues,
			Map<String, BigDecimal> lastValueDiffs
	) {
		//noinspection unchecked
		return (Collection<? extends Map<String, Comparable<? extends Comparable<?>>>>) nextDataSet.stream()
				.map(Map::entrySet)
				.map(entrySet -> entrySet.stream()
						.map(mapEntry -> mergeNextDataSetEntry(mapEntry, columnMap.get(mapEntry.getKey()), minValues, maxValues, lastValues, lastValueDiffs))
						.collect(Collectors.toMap(
								Map.Entry::getKey,
								Map.Entry::getValue
						))
				)
				.toList();
	}

	private Map.Entry<String, Comparable<? extends Comparable<?>>> mergeNextDataSetEntry(
			Map.Entry<String, Comparable<? extends Comparable<?>>> mapEntry,
			DataColumn column,
			Map<String, BigDecimal> minValues,
			Map<String, BigDecimal> maxValues,
			Map<String, BigDecimal> lastValues,
			Map<String, BigDecimal> lastValueDiffs
	) {
		var columnName = mapEntry.getKey();
		var aggregationType = column.aggregationType();
		return switch (aggregationType) {
			case NONE -> mapEntry;
			case INC -> Map.entry(columnName, lastValues.get(columnName).add((BigDecimal) mapEntry.getValue()).add(lastValueDiffs.get(columnName)));
			case MIN -> Map.entry(columnName, minValues.get(columnName));
			case MAX -> Map.entry(columnName, maxValues.get(columnName));
		};
	}

	private Map<String, BigDecimal> getLastValues(List<Map<String, Comparable<? extends Comparable<?>>>> data) {
		return data.get(data.size() - 1)
				.entrySet()
				.stream()
				.filter(entry -> entry.getValue() instanceof BigDecimal)
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						entry -> (BigDecimal) entry.getValue()
				));
	}

	private Map<String, BigDecimal> getLastValueDiffs(List<Map<String, Comparable<? extends Comparable<?>>>> data, Map<String, BigDecimal> lastValues) {
		return data.get(data.size() - 2)
				.entrySet()
				.stream()
				.filter(entry -> entry.getValue() instanceof BigDecimal)
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						entry -> lastValues.get(entry.getKey()).subtract((BigDecimal) entry.getValue())
				));
	}

	private Map<String, BigDecimal> findMinValues(List<Map<String, Comparable<? extends Comparable<?>>>> data, DataColumn[] columns) {
		return Stream.of(columns)
				.filter(c -> c.aggregationType() == DataColumn.AggregationType.MIN)
				.filter(c -> c.type() == DataColumn.Type.NUMERIC)
				.collect(Collectors.toMap(
						DataColumn::name,
						c -> data.stream()
								.map(d -> ((BigDecimal) d.get(c.name())))
								.min(BigDecimal::compareTo)
								.orElseThrow()
				));
	}

	private Map<String, BigDecimal> findMaxValues(List<Map<String, Comparable<? extends Comparable<?>>>> data, DataColumn[] columns) {
		return Stream.of(columns)
				.filter(c -> c.aggregationType() == DataColumn.AggregationType.MAX)
				.filter(c -> c.type() == DataColumn.Type.NUMERIC)
				.collect(Collectors.toMap(
						DataColumn::name,
						c -> data.stream()
								.map(d -> ((BigDecimal) d.get(c.name())))
								.max(BigDecimal::compareTo)
								.orElseThrow()
				));
	}

}
