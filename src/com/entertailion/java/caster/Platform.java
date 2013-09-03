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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Iterator;

/**
 * Platform-specific capabilities
 */
public class Platform {
	private static final String LOG_TAG = "Platform";

	public static final int NAME = 0;
	public static final int CERTIFICATE_NAME = 1;
	public static final int UNIQUE_ID = 2;
	public static final int NETWORK_NAME = 3;
	public static final int MODE_PRIVATE = 0;

	/**
	 * Open a file for output
	 * 
	 * @param name
	 * @param mode
	 * @return
	 * @throws FileNotFoundException
	 */
	public FileOutputStream openFileOutput(String name, int mode) throws FileNotFoundException {
		// TODO support mode parameter
		return new FileOutputStream(name);
	}

	/**
	 * Open a file for input
	 * 
	 * @param name
	 * @return
	 * @throws FileNotFoundException
	 */
	public FileInputStream openFileInput(String name) throws FileNotFoundException {
		return new FileInputStream(name);
	}

	private static InterfaceAddress getPreferredInetAddress(String prefix) {
		InterfaceAddress selectedInterfaceAddress = null;
		try {
			Enumeration<NetworkInterface> list = NetworkInterface.getNetworkInterfaces();

			while (list.hasMoreElements()) {
				NetworkInterface iface = list.nextElement();
				if (iface == null)
					continue;
				Log.d(LOG_TAG, "interface=" + iface.getName());
				Iterator<InterfaceAddress> it = iface.getInterfaceAddresses().iterator();
				while (it.hasNext()) {
					InterfaceAddress interfaceAddress = it.next();
					if (interfaceAddress == null)
						continue;
					InetAddress address = interfaceAddress.getAddress();
					Log.d(LOG_TAG, "address=" + address);
					if (address instanceof Inet4Address) {
						// Only pick an interface that is likely to be on the
						// same subnet as the selected ChromeCast device
						if (address.getHostAddress().toString().startsWith(prefix)) {
							return interfaceAddress;
						}
					}
				}
			}
		} catch (Exception ex) {
		}
		return selectedInterfaceAddress;
	}

	/**
	 * Get the network address.
	 * 
	 * @return
	 */
	public Inet4Address getNetworAddress(String dialServerAddress) {
		Inet4Address selectedInetAddress = null;
		try {
			InterfaceAddress interfaceAddress = null;
			if (dialServerAddress != null) {
				String prefix = dialServerAddress.substring(0, dialServerAddress.indexOf('.') + 1);
				Log.d(LOG_TAG, "prefix=" + prefix);
				interfaceAddress = getPreferredInetAddress(prefix);
			} else {
				InterfaceAddress oneNineTwoInetAddress = getPreferredInetAddress("192.");
				if (oneNineTwoInetAddress != null) {
					interfaceAddress = oneNineTwoInetAddress;
				} else {
					InterfaceAddress oneSevenTwoInetAddress = getPreferredInetAddress("172.");
					if (oneSevenTwoInetAddress != null) {
						interfaceAddress = oneSevenTwoInetAddress;
					} else {
						interfaceAddress = getPreferredInetAddress("10.");
					}
				}
			}
			if (interfaceAddress != null) {
				InetAddress networkAddress = interfaceAddress.getAddress();
				Log.d(LOG_TAG, "networkAddress=" + networkAddress);
				if (networkAddress != null) {
					return (Inet4Address) networkAddress;
				}
			}
		} catch (Exception ex) {
		}

		return selectedInetAddress;
	}

	/**
	 * Get the platform version code
	 * 
	 * @return versionCode
	 */
	public int getVersionCode() {
		return 1;
	}

	/**
	 * Get platform strings
	 * 
	 * @param id
	 * @return
	 */
	public String getString(int id) {
		switch (id) {
		case NAME:
			return "Raspberry PI";
		case CERTIFICATE_NAME:
			return "java";
		case UNIQUE_ID:
			return "emulator";
		case NETWORK_NAME:
			return "wired"; // (Wifi would be SSID)
		default:
			return null;
		}
	}
}
