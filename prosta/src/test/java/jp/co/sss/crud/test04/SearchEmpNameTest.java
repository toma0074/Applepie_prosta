package jp.co.sss.crud.test04;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
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

import com.google.common.io.Files;

@TestMethodOrder(OrderAnnotation.class)
@DisplayName("04_社員名検索機能")
public class SearchEmpNameTest {

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

	private void doLogin() {		
		// 指定したURLに遷移する
		webDriver.get("http://localhost:7779/spring_crud/");

		WebElement loginIdElement = webDriver.findElement(By.name("empId"));
		loginIdElement.clear();
		loginIdElement.sendKeys("2");

		WebElement password = webDriver.findElement(By.name("empPass"));
		password.clear();
		password.sendKeys("2222");
		
		webDriver.findElement(By.cssSelector("input[type='submit']")).submit();


	}

	@Test
	@Order(1)
	public void 正常系_社員名検索操作_入力_郎() {
		// スクリーンショットのリスト
		ArrayList<File> tempFileList = new ArrayList<File>();
		// スクショ保存パス
		String screenshotPath = "screenshots\\04_SearchEmpNameTest\\";
		
		doLogin();

		WebElement searchEmpNameElement = webDriver.findElement(By.name("empName"));
		searchEmpNameElement.clear();
		searchEmpNameElement.sendKeys("郎");

		// スクリーンショット
		tempFileList.add(((TakesScreenshot) webDriver).getScreenshotAs(OutputType.FILE));
		
		searchEmpNameElement.submit();

		// スクリーンショット
		tempFileList.add(((TakesScreenshot) webDriver).getScreenshotAs(OutputType.FILE));
		
		WebElement empNameOfSuzuki = webDriver.findElement(By.cssSelector("table tr:nth-of-type(2) td:nth-of-type(2)"));
		WebElement empNameOfTanaka = webDriver.findElement(By.cssSelector("table tr:nth-of-type(3) td:nth-of-type(2)"));
		
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
		assertEquals("鈴木太郎", empNameOfSuzuki.getText());
		assertEquals("田中二郎", empNameOfTanaka.getText());

	}

	@Test
	@Order(2)
	public void 正常系_社員名検索操作_入力_空文字() {
		// スクリーンショットのリスト
		ArrayList<File> tempFileList = new ArrayList<File>();
		// スクショ保存パス
		String screenshotPath = "screenshots\\04_SearchEmpNameTest\\";
		
		doLogin();

		WebElement searchEmpNameElement = webDriver.findElement(By.name("empName"));
		searchEmpNameElement.clear();

		// スクリーンショット
		tempFileList.add(((TakesScreenshot) webDriver).getScreenshotAs(OutputType.FILE));
		
		searchEmpNameElement.submit();

		// スクリーンショット
		tempFileList.add(((TakesScreenshot) webDriver).getScreenshotAs(OutputType.FILE));

		WebElement empNameOfSuzuki = webDriver.findElement(By.cssSelector("table tr:nth-of-type(2) td:nth-of-type(2)"));
		WebElement empNameOfTanaka = webDriver.findElement(By.cssSelector("table tr:nth-of-type(3) td:nth-of-type(2)"));
		WebElement empNameOfWatanabe = webDriver
				.findElement(By.cssSelector("table tr:nth-of-type(4) td:nth-of-type(2)"));

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
		assertEquals("鈴木太郎", empNameOfSuzuki.getText());
		assertEquals("田中二郎", empNameOfTanaka.getText());
		assertEquals("渡辺花子", empNameOfWatanabe.getText());

	}

	@Test
	@Order(3)
	public void 正常系_社員名検索操作_入力_上() {
		// スクリーンショットのリスト
		ArrayList<File> tempFileList = new ArrayList<File>();
		// スクショ保存パス
		String screenshotPath = "screenshots\\04_SearchEmpNameTest\\";
		
		doLogin();

		WebElement searchEmpNameElement = webDriver.findElement(By.name("empName"));
		searchEmpNameElement.clear();
		searchEmpNameElement.sendKeys("上");

		// スクリーンショット
		tempFileList.add(((TakesScreenshot) webDriver).getScreenshotAs(OutputType.FILE));
		
		searchEmpNameElement.submit();

		// スクリーンショット
		tempFileList.add(((TakesScreenshot) webDriver).getScreenshotAs(OutputType.FILE));

		WebElement msgOfNoneEmployee = webDriver.findElement(By.className("back_to_top_message"));
		WebElement backToTopLink = webDriver.findElement(By.className("back_to_top_link"));

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
		assertEquals("該当する社員は存在しません。", msgOfNoneEmployee.getText());
		assertEquals("一覧表示に戻る", backToTopLink.getText());

	}

}
