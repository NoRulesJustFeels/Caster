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

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.util.Properties;

import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;

import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;

/**
 * @author leon_nicholls
 * 
 */
public class Playback {

	private static final String LOG_TAG = "Playback";

	private static final String VLC_MAC = "/Applications/VLC.app/Contents/MacOS/lib";
	private static final String VLC_WINDOWS1 = "C:\\Program Files\\VideoLAN\\VLC";
	private static final String VLC_WINDOWS2 = "C:\\Program Files (x86)\\VideoLAN\\VLC﻿";

	public static final String TRANSCODING_PARAMETERS = "vcodec=VP80,vb=1000,vfilter=canvas{width=640,height=360},acodec=vorb,ab=128,channels=2,samplerate=44100,threads=2";

	private static EmbeddedServer embeddedServer;
	private static int port = EmbeddedServer.HTTP_PORT;

	private static MediaPlayerFactory mediaPlayerFactory;
	private static MediaPlayer mediaPlayer;
	private static String transcodingParameterValues = TRANSCODING_PARAMETERS;

	private PlaybackListener playbackListener;
	private RampClient rampClient;
	private String appId;
	private DialServer dialServer;
	private Platform platform;
	private boolean isTranscoding;

	public Playback(Platform platform, String appId, DialServer dialServer, PlaybackListener playbackListener) {
		this.platform = platform;
		this.appId = appId;
		this.dialServer = dialServer;
		this.playbackListener = playbackListener;
		this.rampClient = new RampClient(this, playbackListener);
	}

	public void stream(final String u) {
		if (!rampClient.isClosed()) {
			rampClient.closeCurrentApp(dialServer);
		}
		if (dialServer != null) {
			rampClient.launchApp(appId, dialServer);
			// wait for socket to be ready...
			new Thread(new Runnable() {
				public void run() {
					while (!rampClient.isStarted() && !rampClient.isClosed()) {
						try {
							// make less than 3 second ping time
							Thread.sleep(500);
						} catch (InterruptedException e) {
						}
					}
					if (!rampClient.isClosed()) {
						try {
							Thread.sleep(500);
						} catch (InterruptedException e) {
						}
						rampClient.load(u);
					}
				}
			}).start();
		} else {
			Log.d(LOG_TAG, "stream: dialserver null");
		}
	}

	public void setTranscodingParameters(String transcodingParameterValues) {
		this.transcodingParameterValues = transcodingParameterValues;
	}

	public void play(final String file, final boolean isTranscoding) {
		Log.d(LOG_TAG, "play: " + rampClient);
		this.isTranscoding = isTranscoding;
		if (isTranscoding) {
			initializeTranscoder();

			mediaPlayerFactory = new MediaPlayerFactory();
			mediaPlayer = mediaPlayerFactory.newHeadlessMediaPlayer();
			// Add a component to be notified of player events
			mediaPlayer.addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
				public void opening(MediaPlayer mediaPlayer) {
					Log.d(LOG_TAG, "VLC Transcoding: Opening");
				}

				public void buffering(MediaPlayer mediaPlayer, float newCache) {
					Log.d(LOG_TAG, "VLC Transcoding: Buffering");
				}

				public void playing(MediaPlayer mediaPlayer) {
					Log.d(LOG_TAG, "VLC Transcoding: Playing");
					if (playbackListener != null) {
						playbackListener.updateDuration(Playback.this, (int) (mediaPlayer.getLength() / 1000.0f));
					}
				}

				public void paused(MediaPlayer mediaPlayer) {
					Log.d(LOG_TAG, "VLC Transcoding: Paused");
				}

				public void stopped(MediaPlayer mediaPlayer) {
					Log.d(LOG_TAG, "VLC Transcoding: Stopped");
				}

				public void finished(MediaPlayer mediaPlayer) {
					Log.d(LOG_TAG, "VLC Transcoding: Finished");
				}

				public void error(MediaPlayer mediaPlayer) {
					Log.d(LOG_TAG, "VLC Transcoding: Error");
				}

				public void videoOutput(MediaPlayer mediaPlayer, int newCount) {
					Log.d(LOG_TAG, "VLC Transcoding: VideoOutput");
				}
			});

			// Find a port for VLC HTTP server
			boolean started = false;
			int vlcPort = port + 1;
			while (!started) {
				try {
					ServerSocket serverSocket = new ServerSocket(vlcPort);
					Log.d(LOG_TAG, "Available port for VLC: " + vlcPort);
					started = true;
					serverSocket.close();
				} catch (IOException ioe) {
					vlcPort++;
				} catch (Exception ex) {
					break;
				}
			}
			port = vlcPort;
		}

		Properties systemProperties = System.getProperties();
		systemProperties.setProperty(EmbeddedServer.CURRENT_FILE, file); // EmbeddedServer.serveFile

		int pos = file.lastIndexOf('.');
		String extension = "";
		if (pos > -1) {
			extension = file.substring(pos);
		}
		if (dialServer != null) {
			Inet4Address address = platform.getNetworAddress(dialServer.getIpAddress().getHostAddress());
			if (address != null) {
				String mediaUrl = null;
				if (isTranscoding) {
					// http://192.168.0.8:8087/cast.webm
					mediaUrl = "http://" + address.getHostAddress() + ":" + port + "/cast.webm";
				} else {
					startWebserver(null);

					mediaUrl = "http://" + address.getHostAddress() + ":" + port + "/video" + extension;
				}
				Log.d(LOG_TAG, "mediaUrl=" + mediaUrl);
				/*
				 * final RampClient rampClient = new RampClient(new
				 * PlaybackListener() { private int time; private int duration;
				 * private int state;
				 * 
				 * @Override public void updateTime(RampClient rampClient, int
				 * time) { Log.d(LOG_TAG, "updateTime: " + time); this.time =
				 * time; }
				 * 
				 * @Override public void updateDuration(RampClient rampClient,
				 * int duration) { Log.d(LOG_TAG, "updateDuration: " +
				 * duration); this.duration = duration; }
				 * 
				 * @Override public void updateState(RampClient rampClient, int
				 * state) { Log.d(LOG_TAG, "updateState: " + state); // Stop the
				 * app if the video reaches the end if (time > 0 && time ==
				 * duration && state == 0) { stop(rampClient); } }
				 * 
				 * private void stop(RampClient rampClient) { doStop();
				 * System.exit(0); }
				 * 
				 * });
				 */
				if (!rampClient.isClosed()) {
					rampClient.closeCurrentApp(dialServer);
				}
				rampClient.launchApp(appId, dialServer);
				final String playbackUrl = mediaUrl;
				// wait for socket to be ready...
				new Thread(new Runnable() {
					public void run() {
						while (!rampClient.isStarted() && !rampClient.isClosed()) {
							try {
								// make less than 3 second ping time
								Thread.sleep(500);
							} catch (InterruptedException e) {
							}
						}
						if (!rampClient.isClosed()) {
							try {
								Thread.sleep(500);
							} catch (InterruptedException e) {
							}
							if (!rampClient.isClosed()) {
								if (isTranscoding) {
									final String transcodingOptions[] = {
											":sout=#transcode{" + transcodingParameterValues + "}:http{mux=webm,dst=:" + port + "/cast.webm}", ":sout-keep" };
									mediaPlayer.playMedia(file, transcodingOptions);
								}
								rampClient.load(playbackUrl);
							}
						}
					}
				}).start();
			} else {
				Log.d(LOG_TAG, "could not find a network interface");
			}
		} else {
			Log.d(LOG_TAG, "play: dialserver null");
		}
	}

	/**
	 * Start a web server to serve the videos to the media player on the
	 * ChromeCast device
	 */
	public static void startWebserver(WebListener weblistener) {
		startWebserver(EmbeddedServer.HTTP_PORT, weblistener);
	}
	
	public static void startWebserver(int customPort, WebListener weblistener) {
		if (customPort>0) {
			port = customPort;
		}
		boolean started = false;
		while (!started) {
			try {
				embeddedServer = new EmbeddedServer(port);
				Log.d(LOG_TAG, "Started web server on port " + port);
				started = true;
				embeddedServer.setWebListener(weblistener);
			} catch (IOException ioe) {
				// ioe.printStackTrace();
				port++;
			} catch (Exception ex) {
				break;
			}
		}
	}

	private static void initializeTranscoder() {
		// VLC wrapper for Java:
		// http://www.capricasoftware.co.uk/projects/vlcj/index.html
		if (Log.getVerbose()) {
			System.setProperty("vlcj.log", "DEBUG");
		} else {
			// System.setProperty("vlcj.log", "INFO");
		}
		try {
			Log.d(LOG_TAG, System.getProperty("os.name"));
			if (System.getProperty("os.name").startsWith("Mac")) {
				NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), VLC_MAC);
				Native.loadLibrary(RuntimeUtil.getLibVlcLibraryName(), uk.co.caprica.vlcj.binding.LibVlc.class);
				Log.d(LOG_TAG, "VLC available");
			} else if (System.getProperty("os.name").startsWith("Windows")) {
				File vlcDirectory1 = new File(VLC_WINDOWS1);
				File vlcDirectory2 = new File(VLC_WINDOWS2);
				if (vlcDirectory1.exists()) {
					Log.d(LOG_TAG, "Found VLC at " + VLC_WINDOWS1);
					NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), VLC_WINDOWS1);
				} else if (vlcDirectory2.exists()) {
					Log.d(LOG_TAG, "Found VLC at " + VLC_WINDOWS2);
					NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), VLC_WINDOWS2);
				}
				Native.loadLibrary(RuntimeUtil.getLibVlcLibraryName(), uk.co.caprica.vlcj.binding.LibVlc.class);
				Log.d(LOG_TAG, "VLC available");
			} else {
				Native.loadLibrary(RuntimeUtil.getLibVlcLibraryName(), uk.co.caprica.vlcj.binding.LibVlc.class);
				Log.d(LOG_TAG, "VLC available");
			}
		} catch (Throwable ex) {
			// Try for other OS's
			try {
				Native.loadLibrary(RuntimeUtil.getLibVlcLibraryName(), uk.co.caprica.vlcj.binding.LibVlc.class);
				Log.d(LOG_TAG, "VLC available");
			} catch (Throwable ex2) {
				Log.d(LOG_TAG, "VLC not available");
			}
		}
	}

	public void doStop() {
		if (rampClient != null) {
			rampClient.closeCurrentApp(dialServer);
			rampClient = null;
		}
		if (!isTranscoding) {
			if (embeddedServer != null) {
				embeddedServer.stop();
				embeddedServer = null;
			}
		} else {
			if (mediaPlayer != null) {
				mediaPlayer.release();
				mediaPlayer = null;
			}
			if (mediaPlayerFactory != null) {
				mediaPlayerFactory.release();
				mediaPlayerFactory = null;
			}
		}
	}

	public void doPlay() {
		if (rampClient != null) {
			rampClient.play();
		}
	}

	public void doPause() {
		Log.d(LOG_TAG, "doPause: " + rampClient);
		if (rampClient != null) {
			rampClient.pause();
		}
	}

}
