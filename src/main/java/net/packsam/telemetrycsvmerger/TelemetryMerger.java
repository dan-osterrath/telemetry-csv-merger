package net.packsam.telemetrycsvmerger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class TelemetryMerger {

	public static final Option OPTION_GTF = Option.builder()
			.option("gtf")
			.longOpt("global-time-factor")
			.hasArg()
			.argName("factor")
			.desc("Global time factor for adjusting the global time of all data sets.")
			.build();

	public static final Option OPTION_OUTPUT = Option.builder()
			.option("o")
			.longOpt("output")
			.hasArg()
			.argName("file")
			.desc("Output file.")
			.build();

	public static final Option OPTION_HELP = Option.builder()
			.option("h")
			.longOpt("help")
			.desc("Prints this help.")
			.build();

	public static void main(String[] args) throws ParseException {
		var commandLine = parseCommandLine(args);
		var gtf = new BigDecimal(commandLine.getOptionValue(OPTION_GTF, "1.0"));
		var outputFile = new File(commandLine.getOptionValue(OPTION_OUTPUT, "merged.csv"));
		var csvFiles = getArgumentsAsFiles(commandLine.getArgs());

		var reader = new DataSetReader();
		var dataSets = csvFiles.parallelStream()
				.map(reader::parseFile)
				.toList();

		var merger = new Merger();
		var merged = merger.merge(dataSets);

		var dataSetWriter = new DataSetWriter(gtf);
		dataSetWriter.write(merged, outputFile);
	}

	private static CommandLine parseCommandLine(String[] args) throws ParseException {
		var argsParser = new DefaultParser();
		var argsOptions = new Options()
				.addOption(OPTION_GTF)
				.addOption(OPTION_OUTPUT)
				.addOption(OPTION_HELP);
		var commandLine = argsParser.parse(argsOptions, args);

		if (commandLine.hasOption(OPTION_HELP)) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp(
					"java -jar telemetry-csv-merger.jar <input> [<input> ...]",
					"<input> can be a CSV file or a directory containing CSV files.",
					argsOptions,
					null,
					true
			);
			System.exit(0);
		}

		return commandLine;
	}

	private static List<File> getArgumentsAsFiles(String[] args) {
		var csvFiles = new ArrayList<File>();
		for (var arg : args) {
			var argFile = new File(arg);
			if (!argFile.exists()) {
				System.err.println("File " + argFile.getAbsolutePath() + " does not exist.");
				System.exit(1);
			}

			if (argFile.isFile()) {
				csvFiles.add(argFile);
			} else if (argFile.isDirectory()) {
				var files = argFile.listFiles(
						file -> file.isFile() && file.getName().toLowerCase().endsWith(".csv")
				);
				Stream.of(Objects.requireNonNull(files))
						.sorted(Comparator.comparing(File::getName))
						.forEach(csvFiles::add);
			}
		}

		return csvFiles;
	}
}
