/*
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jitsi.meet.test;

import org.jitsi.meet.test.util.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.*;
import org.openqa.selenium.firefox.*;
import org.openqa.selenium.ie.*;
import org.openqa.selenium.remote.*;
import org.openqa.selenium.safari.*;

/**
 * The static fixture which holds the drivers to access the conference
 * participant pages.
 *
 * @author Damian Minkov
 * @author Pawel Domas
 */
public class ConferenceFixture
{
    public static final String JITSI_MEET_URL_PROP = "jitsi-meet.instance.url";

    public static final String FAKE_AUDIO_FNAME_PROP
        = "jitsi-meet.fakeStreamAudioFile";

    /**
     * The property to change tested browser.
     */
    public static final String BROWSER_NAME_PROP = "browser";

    /**
     * The available browser type value.
     */
    public enum BrowserType
    {
        chrome, // default one
        firefox,
        safari,
        ie
    }

    /**
     * The currently used browser that runs tests.
     */
    private static BrowserType currentBrowserType = null;

    public static String currentRoomName;

    /**
     * Full name of wav file which will be streamed through participant's fake
     * audio device.
     */
    private static String fakeStreamAudioFName;

    /**
     * The conference owner in the tests.
     */
    private static WebDriver owner;

    /**
     * The second participant.
     */
    private static WebDriver secondParticipant;

    /**
     * The third participant.
     */
    private static WebDriver thirdParticipant;

    /**
     * Returns the currently allocated conference owner.
     * @return the currently allocated conference owner.
     */
    public static WebDriver getOwner()
    {
        return owner;
    }

    /**
     * Returns the currently allocated second participant. If missing
     * will create it.
     * @return the currently allocated second participant.
     */
    public static WebDriver getSecondParticipant()
    {
        if(secondParticipant == null)
        {
            // this will only happen if some test that quits the
            // second participant fails, and couldn't open it
            startParticipant();
        }

        return secondParticipant;
    }

    /**
     * Returns the currently allocated second participant.
     * @return the currently allocated second participant.
     */
    public static WebDriver getSecondParticipantInstance()
    {
        return secondParticipant;
    }

    /**
     * Returns the currently allocated third participant. If missing
     * will create it.
     * @return the currently allocated third participant.
     */
    public static WebDriver getThirdParticipant()
    {
        if(thirdParticipant == null)
        {
            // this will only happen if some test that quits the
            // third participant fails, and couldn't open it
            startThirdParticipant();
        }

        return thirdParticipant;
    }

    /**
     * Returns the currently allocated third participant.
     * @return the currently allocated third participant.
     */
    public static WebDriver getThirdParticipantInstance()
    {
        return thirdParticipant;
    }

    /**
     * Starts the conference owner, the first participant that generates
     * the random room name.
     * @param fragment adds the given string to the fragment part of the URL
     */
    public static void startOwner(String fragment)
    {
        System.out.println("Starting owner participant.");

        owner = startChromeInstance();

        currentRoomName = "torture"
            + String.valueOf((int)(Math.random()*1000000));

        openRoom(owner, fragment);

        ((JavascriptExecutor) owner)
            .executeScript("document.title='Owner'");
    }

    /**
     * Opens the room for the given participant.
     * @param participant to open the current test room.
     * @param fragment adds the given string to the fragment part of the URL
     */
    public static void openRoom(WebDriver participant, String fragment)
    {
        String URL = System.getProperty(JITSI_MEET_URL_PROP) + "/"
            + currentRoomName;
        URL += "#config.requireDisplayName=false";
        if(fragment != null)
            URL += "&" + fragment;

        String browser = System.getProperty(BROWSER_NAME_PROP);
        if(browser != null
            && browser.equalsIgnoreCase(BrowserType.firefox.toString()))
            URL += "&config.firefox_fake_device=true";

        participant.get(URL);

        // disables animations
        ((JavascriptExecutor) participant)
            .executeScript("try { jQuery.fx.off = true; } catch(e) {}");
        // Disables toolbar hiding
        ((JavascriptExecutor) participant).executeScript(
            "config.alwaysVisibleToolbar = true");

        ((JavascriptExecutor) participant)
            .executeScript("APP.UI.dockToolbar(true);");
    }

    /**
     * Starts chrome instance using some default settings.
     * @return the webdriver instance.
     */
    private static WebDriver startChromeInstance()
    {
        String browser = System.getProperty(BROWSER_NAME_PROP);

        // by default we load chrome, but we can load safari or firefox
        if(browser != null
            && browser.equalsIgnoreCase(BrowserType.firefox.toString()))
        {
            FirefoxProfile profile = new FirefoxProfile();
            profile.setPreference("media.navigator.permission.disabled", true);
            profile.setAcceptUntrustedCertificates(true);

            profile.setPreference("browser.download.folderList", 1);
            //String downloadDir
            //    = System.getProperty("user.home") + File.separator
            //          + "Downloads";
            //profile.setPreference("browser.download.dir", downloadDir);
            profile.setPreference("browser.helperApps.neverAsk.saveToDisk",
                "text/plain;text/csv");
            profile.setPreference("browser.helperApps.alwaysAsk.force", false);
            profile.setPreference("browser.download.manager.showWhenStarting", false );

            currentBrowserType = BrowserType.firefox;

            return new FirefoxDriver(profile);
        }
        else if(browser != null
            && browser.equalsIgnoreCase(BrowserType.safari.toString()))
        {
            currentBrowserType = BrowserType.safari;
            return new SafariDriver();
        }
        else if(browser != null
            && browser.equalsIgnoreCase(BrowserType.ie.toString()))
        {
            DesiredCapabilities caps = DesiredCapabilities.internetExplorer();
            caps.setCapability("ignoreZoomSetting", true);
            System.setProperty("webdriver.ie.driver.silent", "true");

            currentBrowserType = BrowserType.ie;

            return new InternetExplorerDriver(caps);
        }
        else
        {
            ChromeOptions ops = new ChromeOptions();
            ops.addArguments("use-fake-ui-for-media-stream");
            ops.addArguments("use-fake-device-for-media-stream");

            if (fakeStreamAudioFName != null)
            {
                ops.addArguments(
                    "use-file-for-fake-audio-capture=" + fakeStreamAudioFName);
            }

            ops.addArguments("vmodule=\"*media/*=3,*turn*=3\"");

            currentBrowserType = BrowserType.chrome;

            return new ChromeDriver(ops);
        }
    }

    /**
     * Start another participant reusing the already generated room name.
     */
    public static void startParticipant()
    {
        System.out.println("Starting second participant.");
        secondParticipant = startChromeInstance();

        openRoom(secondParticipant, null);

        ((JavascriptExecutor) secondParticipant)
            .executeScript("document.title='SecondParticipant'");
    }

    /**
     * Start the third participant reusing the already generated room name.
     */
    public static void startThirdParticipant()
    {
        System.out.println("Starting third participant.");

        thirdParticipant = startChromeInstance();

        openRoom(thirdParticipant, null);

        ((JavascriptExecutor) thirdParticipant)
            .executeScript("document.title='ThirdParticipant'");
    }

    /**
     * Checks whether participant has joined the room
     * @param participant where we check
     * @param timeout the time to wait for the event.
     */
    public static void checkParticipantToJoinRoom(
        WebDriver participant, long timeout)
    {
        TestUtils.waitsForBoolean(
            participant,
            "return APP.xmpp.isMUCJoined();",
            timeout);
    }

    /**
     * Waits the participant to get event for iceConnectionState that changes
     * to connected.
     * @param participant driver instance used by the participant for whom we
     *                    want to wait to join the conference.
     */
    public static void waitsParticipantToJoinConference(WebDriver participant)
    {
        TestUtils.waitsForBoolean(
            participant,
            "for (sid in APP.xmpp.getSessions()) {" +
                "if (APP.xmpp.getSessions()[sid]."
                + "peerconnection.iceConnectionState " +
                "!== 'connected')" +
                "return false;" +
                "}" +
                "return true;"
            ,30);
    }

    /**
     * Quits the participant.
     * @param participant to quit.
     */
    public static void quit(WebDriver participant)
    {
        try
        {
            if(participant != null)
                MeetUIUtils.clickOnToolbarButtonByClass(
                    participant, "icon-hangup");

            TestUtils.waits(500);
        }
        catch(Throwable t)
        {
            t.printStackTrace();
        }

        try
        {
            if(participant != null)
            {
                participant.close();

                TestUtils.waits(500);

                participant.quit();

                TestUtils.waits(500);
            }
        }
        catch(Throwable t)
        {
            t.printStackTrace();
        }

        if(participant == owner)
        {
            System.out.println("Quited owner.");
            owner = null;
        }
        else if(participant == secondParticipant)
        {
            System.out.println("Quited second participant.");
            secondParticipant = null;
        }
        else if(participant == thirdParticipant)
        {
            System.out.println("Quited third participant.");
            thirdParticipant = null;
        }
    }

    /**
     * Sets the name of wav audio file which will be streamed through fake audio
     * device by participants. The file is not looped, so must be long enough
     * for all tests to finish.
     *
     * @param fakeStreamAudioFile full name of wav file for the fake audio
     *                            device.
     */
    public static void setFakeStreamAudioFile(String fakeStreamAudioFile)
    {
        fakeStreamAudioFName = fakeStreamAudioFile;
    }

    /**
     * The current used browser.
     * @return the current used browser.
     */
    public static BrowserType getCurrentBrowserType()
    {
        return currentBrowserType;
    }
}
