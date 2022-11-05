package net.packsam.telemetrycsvmerger.model;

import java.util.List;
import java.util.Map;

public record DataSet(DataColumn[] columns, List<Map<String, Comparable<? extends Comparable<?>>>> data) {
}
