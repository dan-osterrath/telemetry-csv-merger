package net.packsam.telemetrycsvmerger.model;

import java.util.Objects;

public record DataColumn(String name, Type type, AggregationType aggregationType) {

	@Override
	public String toString() {
		return "DataColumn{" +
				"name='" + name + '\'' +
				", type=" + type +
				", aggregationType=" + aggregationType +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		DataColumn that = (DataColumn) o;
		return Objects.equals(name, that.name) && type == that.type && aggregationType == that.aggregationType;
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, type, aggregationType);
	}

	public enum Type {
		NUMERIC, DATETIME, GLOBALTIME
	}

	public enum AggregationType {
		NONE, INC, MIN, MAX
	}
}
