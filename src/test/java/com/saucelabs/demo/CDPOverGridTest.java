package com.saucelabs.demo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.devtools.Command;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.HasDevTools;
import org.openqa.selenium.devtools.v91.network.Network;
import org.openqa.selenium.devtools.v91.network.model.BlockedReason;
import org.openqa.selenium.devtools.v91.network.model.ResourceType;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.Assert.assertEquals;

public class CDPOverGridTest {

	@Test
	public void blockUrls() throws InterruptedException, MalformedURLException {
		URL gridUrl = new URL("http://localhost:4444");
		ChromeOptions options = new ChromeOptions();
		options.setCapability("se:recordVideo", true);
		options.setCapability("se:timeZone", "US/Pacific");
		options.setCapability("se:screenResolution", "1920x1080");
		WebDriver webDriver = new RemoteWebDriver(gridUrl, options);
		webDriver = new Augmenter().augment(webDriver);

		try (DevTools devTools = ((HasDevTools) webDriver).getDevTools()) {
			devTools.createSessionIfThereIsNotOne();
			// Network enabled
			devTools.send(new Command<>("Network.enable", ImmutableMap.of()));

			// Block urls that have png and css
			devTools.send(Network.setBlockedURLs(ImmutableList.of("*.css", "*.png")));

			// Listening to events and check that the urls are actually blocked
			devTools.addListener(Network.loadingFailed(), loadingFailed -> {
				if (loadingFailed.getType().equals(ResourceType.STYLESHEET) ||
						loadingFailed.getType().equals(ResourceType.IMAGE)) {
					BlockedReason blockedReason = loadingFailed.getBlockedReason().orElse(null);
					assertEquals(blockedReason, BlockedReason.INSPECTOR);
				}
			});

			webDriver.get("https://www.diemol.com/selenium-4-demo/relative-locators-demo.html");
			// Thread.sleep only meant for demo purposes!
			Thread.sleep(5000);

			// Disabling network interception and reloading the site
			devTools.send(Network.disable());
			webDriver.get("https://www.diemol.com/selenium-4-demo/relative-locators-demo.html");
			// Thread.sleep only meant for demo purposes!
			Thread.sleep(5000);
		} finally {
			webDriver.quit();
		}
	}

}
