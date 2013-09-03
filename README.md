Caster
======

<p><img src="http://chromecast.entertailion.com/chromecastanimation100.gif"/></p>

<p>Caster is a command-line application to beam video files to <a href="https://www.google.com/intl/en/chrome/devices/chromecast/">ChromeCast</a> devices. It is useful for automation and scripting use cases.</p>

<p>To run the application, you need a <a href="http://www.oracle.com/technetwork/java/javase/downloads/index.html">Java runtime environment (JRE)</a> for your operating system. </p>

<p>There is a dependency on a web app that needs to be <a href="https://developers.google.com/cast/whitelisting#whitelist-receiver">registered with Google</a>.</p>

<p>Caster provides several command line options which are self-documented with the '-h' option:
```
java -jar caster.jar -h
usage: java -jar caster.jar [-d <arg>] [-f <arg>] [-h] [-id <arg>] [-l] [-s]
       [-t] [-tp <arg>] [-v] [-V]
  -d,--device <arg>                 ChromeCast device IP address
  -f,--file <arg>                   Local media file; -d also required
  -h,--help                         Print this help message
  -id,--app-id <arg>                App ID for whitelisted device
  -l,--list                         List ChromeCast devices
  -s,--stream                       HTTP URL for streaming content; -d also
                                    required
  -t,--transcode                    Transcode media; -f also required
  -tp,--transcode-parameters <arg>  Transcode parameters; -t also required
  -v,--verbose                      Verbose logging
  -V,--version                      Print version information
```
</p>

<p>Here are some examples of using Caster:
<ul>
<li>Get the list of ChromeCast devices (there is a 10 second delay for finding the devices on the local network):
<blockquote>
java -jar caster.jar -l
</blockquote>
</li>
<li>Play a stream from the internet on the ChromeCast device with IP address 192.168.0.22:
<blockquote>
java -jar caster.jar -d 192.168.0.22 -s "http://commondatastorage.googleapis.com/gtv-videos-bucket/big_buck_bunny_1080p.mp4"
</blockquote>
</li>
<li>Play a local file:
<blockquote>
java -jar caster.jar -d 192.168.0.22 -f "/Users/leon_nicholls/Downloads/video.mp4"
</blockquote>
</li>
<li>Transcode a local file:
<blockquote>
java -jar caster.jar -d 192.168.0.22 -t -f "/Users/leon_nicholls/Downloads/video.wmv"
</blockquote>
</li>
<li>Transcode a local file with custom VLC transcoding parameters:
<blockquote>
java -jar caster.jar -d 192.168.0.22 -t -tp "vcodec=VP80,vb=2000,vfilter=canvas{width=640,height=360}, acodec=vorb,ab=128,channels=2,samplerate=44100,threads=2" -f "/Users/leon_nicholls/Downloads/video.wmv"
</blockquote>
</li>
</ul>

<p>ChromeCast devices only support a limited number of <a href="https://developers.google.com/cast/supported_media_types">media formats</a>.
Caster has experimental support for converting other media formats into <a href="http://en.wikipedia.org/wiki/WebM">WebM</a> using the <a href="https://github.com/caprica/vlcj">vlcj library</a> for <a href="http://www.videolan.org/index.html">VLC</a>. 
You need to <a href="http://www.videolan.org/vlc/#download">download</a> and install VLC for your computer (the latest 64-bit version is preferred). For Windows, you need to install the <a href="http://download.videolan.org/pub/videolan/vlc/last/win64/">64-bit experimental version</a>. 
<b>If you have the 64-bit version of Java, you have to install the 64-bit version of VLC</b>. By default Caster uses the following <a href="http://www.videolan.org/doc/streaming-howto/en/ch03.html#id346868">VLC transcoding parameters</a>:
<blockquote>
:sout=#transcode{vcodec=VP80,vb=1000,vfilter=canvas{width=640,height=360}, acodec=vorb,ab=128,channels=2,samplerate=44100,threads=2} :http{mux=webm,dst=:8087/cast.webm} :sout-keep
</blockquote>
<b>Note that converting video is very CPU intensive. It might take several seconds for the video to start playing on your ChromeCast device</b>.
</p>

<p>The computer running the Caster application needs to be on the same network as the ChromeCast device. 
To play the video, a web server is created on port 8080 on your computer and you might have to configure your firewall to allow incoming connections to access the video.</p>

<p>Caster doesn't provide command-line playback controls. You can use an Android app like <a href="https://play.google.com/store/apps/details?id=com.benlc.camcast">RemoteCast</a> to remotely control the media playback.</p>

<p>Note for developers: You need to put your own app ID in the Main.java and receiver index.html files. Upload the receiver index.html file to your whitelisted app URL. Generate the caster.jar file from the source code.
This <a href="https://dl.dropboxusercontent.com/u/17958951/caster.jar">caster.jar</a> build can be used with your own "-id" app id as a command-line parameter.</p>

<p>Other apps developed by Entertailion:
<ul>
<li><a href="https://github.com/entertailion/Fling">Fling</a>: GUI to beam video files from computers to ChromeCast devices.</li> 
<li><a href="https://github.com/entertailion/DIAL">DIAL</a>: ChromeCast device discovery and YouTube playback.</li>
<li><a href="https://play.google.com/store/apps/details?id=com.entertailion.android.tvremote">Able Remote for Google TV</a>: The ultimate Google TV remote</li>
<li><a href="https://play.google.com/store/apps/details?id=com.entertailion.android.launcher">Open Launcher for Google TV</a>: The ultimate Google TV launcher</li>
<li><a href="https://play.google.com/store/apps/details?id=com.entertailion.android.overlay">Overlay for Google TV</a>: Live TV effects for Google TV</li>
<li><a href="https://play.google.com/store/apps/details?id=com.entertailion.android.overlaynews">Overlay News for Google TV</a>: News headlines over live TV</li>
<li><a href="https://play.google.com/store/apps/details?id=com.entertailion.android.videowall">Video Wall</a>: Wall-to-Wall Youtube videos</li>
<li><a href="https://play.google.com/store/apps/details?id=com.entertailion.android.tasker">GTV Tasker Plugin</a>: Control your Google TV with Tasker actions</li>
</ul>
</p>
