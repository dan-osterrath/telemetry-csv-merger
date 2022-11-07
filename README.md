# Telemetry CSV merger

This tool merges multiple GoPro telemetry CSV files into a single CSV file. These files are created
by [DashWare](http://www.dashware.net/) when merging multiple GoPro videos. It extracts the embedded telemetry data and
writes them to a CSV file per video. To use them in DashWare you have to merge them and update incrementing timestamps
and min and max values. Additionally, the telemetry data is not always in sync with the merged video. This tool also
applies a factor to the global time to make video and telemetry in sync.

This tool is tested with videos from a GoPro Hero 7 Black. If you can not use GoPro 5 or up telemetry data in DashWare,
see [this link](https://community.gopro.com/s/question/0D53b00008BtEN8CAN/hero-7-black-telemetry-not-showing-in-dashware-191)
.

![Maven build](https://github.com/dan-osterrath/telemetry-csv-merger/actions/workflows/maven.yml/badge.svg)

## Usage

Compile the project to a single jar file:

```shell
mvn clean package
```

Run the jar file with the following parameters:

```shell
java -jar target/telemetry-csv-merger.jar <path-to-csv-files> ...
```

### Help

To get a help of available options, use:

```shell
java -jar target/telemetry-csv-merger.jar -h
```

### Value Factors

In some cases the telemetry data is not in sync with the merged video. To fix this you have to apply a factor to
the `GlobalTime` column. This can be done with the command line argument `-f` or `--factor`.

Example: In DashWare your merged video has a length of 37:27.500. In the "Synchronization" tab of DashWare you can see
that the merged telemetry data only has a length of 36:00.950. The telemetry data is compressed by 01:26.650. To stretch
the telemetry data you have to apply a factor of `1.040051829056665` to the `GlobalTime` field.

```
01:26.650 = 86.55s
36:00.950 = 2160.95s
37:27.500 = 2247.50s

1 + (86.55 / 2160.95) = 1.040051829056665
2247.50 / 2160.95 = 1.040051829056665
```

To apply this factor, run:

```shell
java -jar target/telemetry-csv-merger.jar -f GlobalTime=1.040051829056665 <path-to-csv-files> ...
```

You can also use the `-f` argument multiple times for other numeric fields, i.e. when your sensor data has to be
adjusted.

```shell
java -jar target/telemetry-csv-merger.jar -f GlobalTime=1.040051829056665 -f "Speed KPH=1000" -f "Speed MPH=1000" <path-to-csv-files> ...
```

## Credits

This project is based on [GoPro-Telemetry-Joiner](https://github.com/jamesdesmond/GoPro-Telemetry-Joiner)
by [James Desmond](https://github.com/jamesdesmond).
