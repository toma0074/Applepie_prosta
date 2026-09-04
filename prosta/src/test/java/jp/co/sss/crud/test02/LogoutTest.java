package jp.co.sss.crud.test02;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.google.common.io.Files;

@TestMethodOrder(OrderAnnotation.class)
@DisplayName("02_ログアウト機能")
public class LogoutTest {

	private WebDriver webDriver;

	/**
	 * テストメソッドを実行する前に実行されるメソッド
	 */
	@BeforeEach
	public void createDriver() {
		System.setProperty("webdriver.chrome.driver", "driver/chromedriver.exe");
		ChromeOptions ops = new ChromeOptions();
		ops.addArguments("--remote-allow-origins=*");
		webDriver = new ChromeDriver(ops);
	}

	@AfterEach
	public void quitDriver() {
		webDriver.close();
	}

	@Test
	@Order(1)
	public void 正常系_ログアウト操作() {
		// ウェイト
		WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(2));
		// スクリーンショットのリスト
		ArrayList<File> tempFileList = new ArrayList<File>();
		// スクショ保存パス
		String screenshotPath = "screenshots\\02_LogoutTest\\";
		
		// 指定したURLに遷移する
		webDriver.get("http://localhost:7779/spring_crud/");

//		wait.until(ExpectedConditions.titleContains("タイムアウト"));

		WebElement loginIdElement = webDriver.findElement(By.name("empId"));
		loginIdElement.clear();
		loginIdElement.sendKeys("1");

		WebElement password = webDriver.findElement(By.name("empPass"));
		password.clear();
		password.sendKeys("1111");
		
		// スクリーンショット
		tempFileList.add(((TakesScreenshot) webDriver).getScreenshotAs(OutputType.FILE));
		
		webDriver.findElement(By.cssSelector("input[type='submit']")).submit();

//		wait.until(ExpectedConditions.titleContains("タイムアウト"));
//		webDriver.manage().timeouts().pageLoadTimeout(5, TimeUnit.SECONDS);

		WebElement logoutWebElement = webDriver.findElement(By.linkText("ログアウト"));
		
		// スクリーンショット
		tempFileList.add(((TakesScreenshot) webDriver).getScreenshotAs(OutputType.FILE));

		logoutWebElement.click();

//		wait.until(ExpectedConditions.titleContains("タイムアウト"));
//		webDriver.manage().timeouts().pageLoadTimeout(5, TimeUnit.SECONDS);

		// スクリーンショット
		tempFileList.add(((TakesScreenshot) webDriver).getScreenshotAs(OutputType.FILE));
		
		// スクリーンショット出力
		int count = 0;
		try {
			for (File file : tempFileList) {
				count++;
				Files.move(file, new File(screenshotPath + new Object() {
				}.getClass().getEnclosingMethod().getName() + "_" + count + ".png"));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// 検証
		assertEquals("http://localhost:7779/spring_crud/", webDriver.getCurrentUrl());

	}

}
