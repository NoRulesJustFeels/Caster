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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

/**
 * @author leon_nicholls
 * 
 */
public class DeviceFinder implements BroadcastDiscoveryHandler {

	private static final String LOG_TAG = "DeviceFinder";

	private static final String HEADER_APPLICATION_URL = "Application-URL";
	private static final String CHROME_CAST_MODEL_NAME = "Eureka Dongle";
	private static final int DISCOVERY_PERIOD = 10000;

	private BroadcastDiscoveryClient broadcastClient;
	private Thread broadcastClientThread;
	private TrackedDialServers trackedDialServers = new TrackedDialServers();
	private DeviceFinderListener deviceFinderListener;

	public DeviceFinder(DeviceFinderListener deviceFinderListener) {
		this.deviceFinderListener = deviceFinderListener;
	}

	public void discoverDevices() {
		broadcastClient = new BroadcastDiscoveryClient(this);
		broadcastClientThread = new Thread(broadcastClient);

		// discovering devices can take time, so do it in a thread
		new Thread(new Runnable() {
			public void run() {
				try {
					deviceFinderListener.discoveringDevices(DeviceFinder.this);
					broadcastClientThread.start();

					// wait a while...
					// TODO do this better
					Thread.sleep(DISCOVERY_PERIOD);

					broadcastClient.stop();

					deviceFinderListener.discoveredDevices(DeviceFinder.this);
				} catch (InterruptedException e) {
					Log.e(LOG_TAG, "discoverDevices", e);
				}
			}
		}).start();
	}

	public void onBroadcastFound(final BroadcastAdvertisement advert) {
		if (advert.getLocation() != null) {
			new Thread(new Runnable() {
				public void run() {
					Log.d(LOG_TAG, "location=" + advert.getLocation());
					HttpResponse response = new HttpRequestHelper().sendHttpGet(advert.getLocation());
					if (response != null) {
						String appsUrl = null;
						Header header = response.getLastHeader(HEADER_APPLICATION_URL);
						if (header != null) {
							appsUrl = header.getValue();
							if (!appsUrl.endsWith("/")) {
								appsUrl = appsUrl + "/";
							}
							Log.d(LOG_TAG, "appsUrl=" + appsUrl);
						}
						try {
							InputStream inputStream = response.getEntity().getContent();
							BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

							InputSource inStream = new org.xml.sax.InputSource();
							inStream.setCharacterStream(reader);
							SAXParserFactory spf = SAXParserFactory.newInstance();
							SAXParser sp = spf.newSAXParser();
							XMLReader xr = sp.getXMLReader();
							BroadcastHandler broadcastHandler = new BroadcastHandler();
							xr.setContentHandler(broadcastHandler);
							xr.parse(inStream);
							Log.d(LOG_TAG, "modelName=" + broadcastHandler.getDialServer().getModelName());
							// Only handle ChromeCast devices; not other DIAL
							// devices like ChromeCast devices
							if (broadcastHandler.getDialServer().getModelName().equals(CHROME_CAST_MODEL_NAME)) {
								Log.d(LOG_TAG, "ChromeCast device found: " + advert.getIpAddress().getHostAddress());
								DialServer dialServer = new DialServer(advert.getLocation(), advert.getIpAddress(), advert.getPort(), appsUrl, broadcastHandler
										.getDialServer().getFriendlyName(), broadcastHandler.getDialServer().getUuid(), broadcastHandler.getDialServer()
										.getManufacturer(), broadcastHandler.getDialServer().getModelName());
								trackedDialServers.add(dialServer);
							}
						} catch (Exception e) {
							Log.e(LOG_TAG, "parse device description", e);
						}
					}
				}
			}).start();
		}
	}

	public TrackedDialServers getTrackedDialServers() {
		return trackedDialServers;
	}

}
