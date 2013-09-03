/*
 * Copyright (C) 2013 ENTERTAILION, LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.entertailion.java.caster;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

/**
 * Command line ChromeCast client: java -jar caster.jar -h
 * https://github.com/entertailion/Caster
 * 
 * @author leon_nicholls
 * 
 */
public class Main {

	private static final String LOG_TAG = "Main";

	// TODO Add your own app id here
	private static final String APP_ID = "YOUR_APP_ID_HERE";

	public static final String VERSION = "0.1";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// http://commons.apache.org/proper/commons-cli/usage.html
		Option help = new Option("h", "help", false, "Print this help message");
		Option version = new Option("V", "version", false, "Print version information");
		Option list = new Option("l", "list", false, "List ChromeCast devices");
		Option verbose = new Option("v", "verbose", false, "Verbose logging");
		Option transcode = new Option("t", "transcode", false, "Transcode media; -f also required");

		Option url = OptionBuilder.withLongOpt("stream").hasArg().withValueSeparator().withDescription("HTTP URL for streaming content; -d also required")
				.create("s");

		Option server = OptionBuilder.withLongOpt("device").hasArg().withValueSeparator().withDescription("ChromeCast device IP address").create("d");

		Option id = OptionBuilder.withLongOpt("app-id").hasArg().withValueSeparator().withDescription("App ID for whitelisted device").create("id");

		Option mediaFile = OptionBuilder.withLongOpt("file").hasArg().withValueSeparator().withDescription("Local media file; -d also required").create("f");

		Option transcodingParameters = OptionBuilder.withLongOpt("transcode-parameters").hasArg().withValueSeparator()
				.withDescription("Transcode parameters; -t also required").create("tp");

		Options options = new Options();
		options.addOption(help);
		options.addOption(version);
		options.addOption(list);
		options.addOption(verbose);
		options.addOption(url);
		options.addOption(server);
		options.addOption(id);
		options.addOption(mediaFile);
		options.addOption(transcode);
		options.addOption(transcodingParameters);
		// create the command line parser
		CommandLineParser parser = new PosixParser();

		try {
			// parse the command line arguments
			CommandLine line = parser.parse(options, args);
			Option[] lineOptions = line.getOptions();
			if (lineOptions.length==0) {
				System.out.println("caster: try 'java -jar caster.jar -h' for more information");
				System.exit(0);
			}
			
			Platform platform = new Platform();
			String appId = APP_ID;

			Log.setVerbose(line.hasOption("v"));

			// Custom app-id
			if (line.hasOption("id")) {
				Log.d(LOG_TAG, line.getOptionValue("id"));
				appId = line.getOptionValue("id");
			}

			// Print version
			if (line.hasOption("V")) {
				System.out.println("Caster version "+VERSION);
			}

			// List ChromeCast devices
			if (line.hasOption("l")) {
				final DeviceFinder deviceFinder = new DeviceFinder(new DeviceFinderListener() {

					@Override
					public void discoveringDevices(DeviceFinder deviceFinder) {
						Log.d(LOG_TAG, "discoveringDevices");
					}

					@Override
					public void discoveredDevices(DeviceFinder deviceFinder) {
						Log.d(LOG_TAG, "discoveredDevices");
						TrackedDialServers trackedDialServers = deviceFinder.getTrackedDialServers();
						for (DialServer dialServer : trackedDialServers) {
							System.out.println(dialServer);
						}
					}

				});
				deviceFinder.discoverDevices();
			}

			// Stream media from internet
			if (line.hasOption("s") && line.hasOption("d")) {
				Log.d(LOG_TAG, line.getOptionValue("d"));
				Log.d(LOG_TAG, line.getOptionValue("s"));
				try {
					Playback playback = new Playback(platform, appId, new DialServer(InetAddress.getByName(line.getOptionValue("d"))), new PlaybackListener() {
						@Override
						public void updateTime(RampClient rampClient, int time) {
							Log.d(LOG_TAG, "updateTime: " + time);
						}

						@Override
						public void updateDuration(RampClient rampClient, int duration) {
							Log.d(LOG_TAG, "updateDuration: " + duration);
						}

						@Override
						public void updateState(RampClient rampClient, int state) {
							Log.d(LOG_TAG, "updateState: " + state);
						}

					});
					playback.stream(line.getOptionValue("s"));
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
			}

			// Play local media file
			if (line.hasOption("f") && line.hasOption("d")) {
				Log.d(LOG_TAG, line.getOptionValue("d"));
				Log.d(LOG_TAG, line.getOptionValue("f"));

				final String file = line.getOptionValue("f");
				String device = line.getOptionValue("d");

				try {
					Playback playback = new Playback(platform, appId, new DialServer(InetAddress.getByName(device)), new PlaybackListener() {
						@Override
						public void updateTime(RampClient rampClient, int time) {
							Log.d(LOG_TAG, "updateTime: " + time);
						}

						@Override
						public void updateDuration(RampClient rampClient, int duration) {
							Log.d(LOG_TAG, "updateDuration: " + duration);
						}

						@Override
						public void updateState(RampClient rampClient, int state) {
							Log.d(LOG_TAG, "updateState: " + state);
						}

					});
					if (line.hasOption("t") && line.hasOption("tp")) {
						playback.setTranscodingParameters(line.getOptionValue("tp"));
					}
					playback.play(file, line.hasOption("t"));
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
			}

			// Print help
			if (line.hasOption("h")) {
				printHelp(options);
			}
		} catch (ParseException exp) {
			System.out.println("ERROR: " + exp.getMessage());
			System.out.println();
			printHelp(options);
		}
	}

	private static void printHelp(Options options) {
		StringWriter out = new StringWriter();
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(new PrintWriter(out), 80, "java -jar caster.jar", "\n", options, 2, 2, "", true);
		System.out.println(out.toString());
	}

}
