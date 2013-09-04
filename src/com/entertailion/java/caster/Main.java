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
import java.util.HashMap;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.entertailion.java.caster.HttpServer.Response;

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

	public static final String VERSION = "0.2";

	private static Platform platform = new Platform();
	private static String appId = APP_ID;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// http://commons.apache.org/proper/commons-cli/usage.html
		Option help = new Option("h", "help", false, "Print this help message");
		Option version = new Option("V", "version", false, "Print version information");
		Option list = new Option("l", "list", false, "List ChromeCast devices");
		Option verbose = new Option("v", "verbose", false, "Verbose debug logging");
		Option transcode = new Option("t", "transcode", false, "Transcode media; -f also required");
		Option rest = new Option("r", "rest", false, "REST API server");

		Option url = OptionBuilder.withLongOpt("stream").hasArg().withValueSeparator().withDescription("HTTP URL for streaming content; -d also required")
				.create("s");

		Option server = OptionBuilder.withLongOpt("device").hasArg().withValueSeparator().withDescription("ChromeCast device IP address").create("d");

		Option id = OptionBuilder.withLongOpt("app-id").hasArg().withValueSeparator().withDescription("App ID for whitelisted device").create("id");

		Option mediaFile = OptionBuilder.withLongOpt("file").hasArg().withValueSeparator().withDescription("Local media file; -d also required").create("f");

		Option transcodingParameters = OptionBuilder.withLongOpt("transcode-parameters").hasArg().withValueSeparator()
				.withDescription("Transcode parameters; -t also required").create("tp");
		
		Option restPort = OptionBuilder.withLongOpt("rest-port").hasArg().withValueSeparator().withDescription("REST API port; default 8080")
		.create("rp");

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
		options.addOption(rest);
		options.addOption(restPort);
		// create the command line parser
		CommandLineParser parser = new PosixParser();

		//String[] arguments = new String[] { "-vr" };

		try {
			// parse the command line arguments
			CommandLine line = parser.parse(options, args);
			Option[] lineOptions = line.getOptions();
			if (lineOptions.length == 0) {
				System.out.println("caster: try 'java -jar caster.jar -h' for more information");
				System.exit(0);
			}

			Log.setVerbose(line.hasOption("v"));

			// Custom app-id
			if (line.hasOption("id")) {
				Log.d(LOG_TAG, line.getOptionValue("id"));
				appId = line.getOptionValue("id");
			}

			// Print version
			if (line.hasOption("V")) {
				System.out.println("Caster version " + VERSION);
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
							Log.d(LOG_TAG, dialServer.toString());
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
						private int time;
						private int duration;
						private int state;

						@Override
						public void updateTime(Playback playback, int time) {
							Log.d(LOG_TAG, "updateTime: " + time);
							this.time = time;
						}

						@Override
						public void updateDuration(Playback playback, int duration) {
							Log.d(LOG_TAG, "updateDuration: " + duration);
							this.duration = duration;
						}

						@Override
						public void updateState(Playback playback, int state) {
							Log.d(LOG_TAG, "updateState: " + state);
							// Stop the app if the video reaches the end
							if (time > 0 && time == duration && state == 0) {
								playback.doStop();
								System.exit(0);
							}
						}

						public int getTime() {
							return time;
						}

						public int getDuration() {
							return duration;
						}

						public int getState() {
							return state;
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
						private int time;
						private int duration;
						private int state;

						@Override
						public void updateTime(Playback playback, int time) {
							Log.d(LOG_TAG, "updateTime: " + time);
							this.time = time;
						}

						@Override
						public void updateDuration(Playback playback, int duration) {
							Log.d(LOG_TAG, "updateDuration: " + duration);
							this.duration = duration;
						}

						@Override
						public void updateState(Playback playback, int state) {
							Log.d(LOG_TAG, "updateState: " + state);
							// Stop the app if the video reaches the end
							if (time > 0 && time == duration && state == 0) {
								playback.doStop();
								System.exit(0);
							}
						}

						public int getTime() {
							return time;
						}

						public int getDuration() {
							return duration;
						}

						public int getState() {
							return state;
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

			// REST API server
			if (line.hasOption("r")) {
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
							Log.d(LOG_TAG, dialServer.toString());
						}
					}

				});
				deviceFinder.discoverDevices();
				
				int port = 0;
				if (line.hasOption("rp")) {
					try {
						port = Integer.parseInt(line.getOptionValue("rp"));
					} catch (NumberFormatException e) {
						Log.e(LOG_TAG, "invalid rest port", e);
					}
				}

				Playback.startWebserver(port, new WebListener() {
					String[] prefixes = { "/playback", "/devices" };
					HashMap<String, Playback> playbackMap = new HashMap<String, Playback>();
					HashMap<String, RestPlaybackListener> playbackListenerMap = new HashMap<String, RestPlaybackListener>();

					final class RestPlaybackListener implements PlaybackListener {
						private String device;
						private int time;
						private int duration;
						private int state;

						public RestPlaybackListener(String device) {
							this.device = device;
						}

						@Override
						public void updateTime(Playback playback, int time) {
							Log.d(LOG_TAG, "updateTime: " + time);
							this.time = time;
						}

						@Override
						public void updateDuration(Playback playback, int duration) {
							Log.d(LOG_TAG, "updateDuration: " + duration);
							this.duration = duration;
						}

						@Override
						public void updateState(Playback playback, int state) {
							Log.d(LOG_TAG, "updateState: " + state);
							this.state = state;
							// Stop the app if the video reaches the end
							if (this.time > 0 && this.time == this.duration && state == 0) {
								playback.doStop();
								playbackMap.remove(device);
								playbackListenerMap.remove(device);
							}
						}

						public int getTime() {
							return time;
						}

						public int getDuration() {
							return duration;
						}

						public int getState() {
							return state;
						}

					}

					@Override
					public Response handleRequest(String uri, String method, Properties header, Properties parms) {
						Log.d(LOG_TAG, "handleRequest: " + uri);

						if (method.equals("GET")) {
							if (uri.startsWith(prefixes[0])) { // playback
								String device = parms.getProperty("device");
								if (device != null) {
									RestPlaybackListener playbackListener = playbackListenerMap.get(device);
									if (playbackListener != null) {
										// https://code.google.com/p/json-simple/wiki/EncodingExamples
										JSONObject obj = new JSONObject();
										obj.put("time", playbackListener.getTime());
										obj.put("duration", playbackListener.getDuration());
										switch (playbackListener.getState()) {
										case 0:
											obj.put("state", "idle");
											break;
										case 1:
											obj.put("state", "stopped");
											break;
										case 2:
											obj.put("state", "playing");
											break;
										default:
											obj.put("state", "idle");
											break;
										}
										return new Response(HttpServer.HTTP_OK, "text/plain", obj.toJSONString());
									} else {
										// Nothing is playing
										JSONObject obj = new JSONObject();
										obj.put("time", 0);
										obj.put("duration", 0);
										obj.put("state", "stopped");
										return new Response(HttpServer.HTTP_OK, "text/plain", obj.toJSONString());
									}
								}
							} else if (uri.startsWith(prefixes[1])) { // devices
								// https://code.google.com/p/json-simple/wiki/EncodingExamples
								JSONArray list = new JSONArray();
								TrackedDialServers trackedDialServers = deviceFinder.getTrackedDialServers();
								for (DialServer dialServer : trackedDialServers) {
									JSONObject obj = new JSONObject();
									obj.put("name", dialServer.getFriendlyName());
									obj.put("ip_address", dialServer.getIpAddress().getHostAddress());
									list.add(obj);
								}
								return new Response(HttpServer.HTTP_OK, "text/plain", list.toJSONString());
							}
						} else if (method.equals("POST")) {
							if (uri.startsWith(prefixes[0])) { // playback
								String device = parms.getProperty("device");
								if (device != null) {
									String stream = parms.getProperty("stream");
									String file = parms.getProperty("file");
									String state = parms.getProperty("state");
									String transcode = parms.getProperty("transcode");
									String transcodeParameters = parms.getProperty("transcode-parameters");
									Log.d(LOG_TAG, "transcodeParameters="+transcodeParameters);
									if (stream != null) {
										try {
											if (playbackMap.get(device) == null) {
												DialServer dialServer = deviceFinder.getTrackedDialServers().findDialServer(InetAddress.getByName(device));
												if (dialServer != null) {
													RestPlaybackListener playbackListener = new RestPlaybackListener(device);
													playbackMap.put(device, new Playback(platform, appId, dialServer, playbackListener));
													playbackListenerMap.put(device, playbackListener);
												}
											}
											Playback playback = playbackMap.get(device);
											if (playback != null) {
												playback.stream(stream);
												return new Response(HttpServer.HTTP_OK, "text/plain", "Ok");
											}
										} catch (Exception e1) {
											Log.e(LOG_TAG, "playback", e1);
										}
									} else if (file != null) {
										try {
											if (playbackMap.get(device) == null) {
												DialServer dialServer = deviceFinder.getTrackedDialServers().findDialServer(InetAddress.getByName(device));
												if (dialServer != null) {
													RestPlaybackListener playbackListener = new RestPlaybackListener(device);
													playbackMap.put(device, new Playback(platform, appId, dialServer, playbackListener));
													playbackListenerMap.put(device, playbackListener);
												}
											}
											Playback playback = playbackMap.get(device);
											if (transcodeParameters!=null) {
												playback.setTranscodingParameters(transcodeParameters);
											}
											if (playback != null) {
												playback.play(file, transcode!=null);
												return new Response(HttpServer.HTTP_OK, "text/plain", "Ok");
											}
										} catch (Exception e1) {
											Log.e(LOG_TAG, "playback", e1);
										}
									} else if (state != null) {
										try {
											if (playbackMap.get(device) == null) {
												DialServer dialServer = deviceFinder.getTrackedDialServers().findDialServer(InetAddress.getByName(device));
												if (dialServer != null) {
													RestPlaybackListener playbackListener = new RestPlaybackListener(device);
													playbackMap.put(device, new Playback(platform, appId, dialServer, playbackListener));
													playbackListenerMap.put(device, playbackListener);
												}
											}
											Playback playback = playbackMap.get(device);
											if (playback != null) {
												if (state.equals("play")) {
													playback.doPlay();
													return new Response(HttpServer.HTTP_OK, "text/plain", "Ok");
												} else if (state.equals("pause")) {
													playback.doPause();
													return new Response(HttpServer.HTTP_OK, "text/plain", "Ok");
												} else if (state.equals("stop")) {
													playback.doStop();
													playbackMap.remove(device);
													playbackListenerMap.remove(device);
													return new Response(HttpServer.HTTP_OK, "text/plain", "Ok");
												}
											}
										} catch (Exception e1) {
											Log.e(LOG_TAG, "playback", e1);
										}
									}
								}
							}
						}

						return new Response(HttpServer.HTTP_BADREQUEST, "text/plain", "Bad Request");
					}

					@Override
					public String[] uriPrefixes() {
						return prefixes;
					}

				});

				// Run forever...
				while (true) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
					}
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
