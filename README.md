# comp433-assignment-04

## Sesame Street Characters

They are used as Drawable objects to represent the commenters. The name of each
drawable has two words separated by an adjective that describes how the
character comments on the selected picture. The images come from here:

<https://sesameworkshop.org/our-work/shows/sesame-street/sesame-street-characters/>

## Add adb (Android Debug Bridge) to $PATH

```bash
# Add the following lines to .zshrc

# Add Android SDK platform-tools to PATH
command -v ~/Library/Android/sdk/platform-tools/adb > /dev/null
if [ $? -eq 0 ]; then
    export PATH=$PATH:~/Library/Android/sdk/platform-tools
    echo "Added Android SDK platform-tools to PATH."
fi
```

### Making sure that the emulator (and your application) has internet access

At one point, I was getting the following error from my Android application:

```text
Unable to resolve host "generativelanguage.googleapis.com": No address associated with hostname
```

In `AndroidManfiest.xml`, make sure that you have the following line:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

In your emulator, open Google Chrome and make sure you can go to any website.

If that does not work, you'll need to use `adb` to troubleshoot further:

```bash
adb version
adb devices
adb shell ping -c 3 8.8.8.8
adb shell ping -c 3 google.com
```

My issue was that the ping to `google.com` was failing,. which points to a DNS
issue.

On macOS, go to System Settings --> Network --> Wi-Fi --> Details --> DNS.

Under DNS Servers, I had the following:

192.168.1.1
2001:1998:f00:2::1
2001:1998:f00:1::1

ChatGPT suggested adding the following:

`8.8.8.8`
`8.8.4.4`

The first three were there by default and went away once I added `8.8.8.8`.

Cold boot the emulator and try the ping commands again in the macOS terminal. To
cold boot, go to Tools --> Device Manager. Stop the emulator if it is running
and click the three vertical dots to enable to "Cold Boot Now" option.

Opening Google Chrome on the emulator should load a page successfully and these
commands should now work:

```bash
adb shell ping -c 3 8.8.8.8
adb shell ping -c 3 google.com
```

Now, communication with the Google Gemini API should be possible.
