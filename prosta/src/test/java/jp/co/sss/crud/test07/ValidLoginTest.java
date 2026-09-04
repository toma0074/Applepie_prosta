package jp.co.sss.crud.test07;

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
@DisplayName("07_入力チェック機能_ログイン")
public class ValidLoginTest {

	private WebDriver webDriver;
	// スクショ保存パス
	private String screenshotPath= "screenshots\\07_ValidLoginTest\\";

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
	public void 異常系_ログイン操作_社員ID_空文字入力メッセージ出力() {
		// スクリーンショットのリスト
		ArrayList<File> tempFileList = new ArrayList<File>();
		
		// 指定したURLに遷移する
		webDriver.get("http://localhost:7779/spring_crud/");

		WebElement empIdElement = webDriver.findElement(By.name("empId"));
		empIdElement.clear();

		WebElement password = webDriver.findElement(By.name("empPass"));
		password.clear();
		password.sendKeys("2222");

		// スクリーンショット
		tempFileList.add(((TakesScreenshot) webDriver).getScreenshotAs(OutputType.FILE));
		
		webDriver.findElement(By.cssSelector("input[type='submit']")).submit();

		// スクリーンショット
		tempFileList.add(((TakesScreenshot) webDriver).getScreenshotAs(OutputType.FILE));

		WebElement errElement = webDriver.findElement(By.cssSelector("p"));

		String errMsgInputNUllOfEmpId = "社員IDを入力してください。";

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
		assertTrue(errElement.getText().contains(errMsgInputNUllOfEmpId), "エラーメッセージが違います：" + errElement.getText());

	}

	@Test
	@Order(2)
	public void 異常系_ログイン操作_社員ID_桁数オーバー入力メッセージ出力() {
		// スクリーンショットのリスト
		ArrayList<File> tempFileList = new ArrayList<File>();
		
		// 指定したURLに遷移する
		webDriver.get("http://localhost:7779/spring_crud/");

		WebElement empIdElement = webDriver.findElement(By.name("empId"));
		empIdElement.clear();
		empIdElement.sendKeys("111111");

		WebElement password = webDriver.findElement(By.name("empPass"));
		password.clear();
		password.sendKeys("2222");

		// スクリーンショット
		tempFileList.add(((TakesScreenshot) webDriver).getScreenshotAs(OutputType.FILE));
		
		webDriver.findElement(By.cssSelector("input[type='submit']")).submit();

		// スクリーンショット
		tempFileList.add(((TakesScreenshot) webDriver).getScreenshotAs(OutputType.FILE));

		WebElement errElement = webDriver.findElement(By.cssSelector("p"));

		String errMsgDigitsOverOfEmpId = "社員IDは99999までの整数値で入力してください。";

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
		assertTrue(errElement.getText().contains(errMsgDigitsOverOfEmpId), "エラーメッセージが違います：" + errElement.getText());

	}

	@Test
	@Order(3)
	public void 異常系_ログイン操作_社員ID_文字種エラー1入力メッセージ出力() {
		// スクリーンショットのリスト
		ArrayList<File> tempFileList = new ArrayList<File>();

		// 指定したURLに遷移する
		webDriver.get("http://localhost:7779/spring_crud/");
		
		WebElement empIdElement = webDriver.findElement(By.name("empId"));
		empIdElement.clear();
		empIdElement.sendKeys("abc");

		WebElement password = webDriver.findElement(By.name("empPass"));
		password.clear();
		password.sendKeys("2222");

		// スクリーンショット
		tempFileList.add(((TakesScreenshot) webDriver).getScreenshotAs(OutputType.FILE));
		
		webDriver.findElement(By.cssSelector("input[type='submit']")).submit();

		// スクリーンショット
		tempFileList.add(((TakesScreenshot) webDriver).getScreenshotAs(OutputType.FILE));

		WebElement errElement = webDriver.findElement(By.cssSelector("p"));

		String errMsgCharTypeOfEmpId = "社員IDは整数値で入力してください。";

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
		assertTrue(errElement.getText().contains(errMsgCharTypeOfEmpId), "エラーメッセージが違います：" + errElement.getText());

	}

	@Test
	@Order(4)
	public void 異常系_ログイン操作_社員ID_文字種エラー2入力メッセージ出力() {
		// スクリーンショットのリスト
		ArrayList<File> tempFileList = new ArrayList<File>();
		
		// 指定したURLに遷移する
		webDriver.get("http://localhost:7779/spring_crud/");

		WebElement empIdElement = webDriver.findElement(By.name("empId"));
		empIdElement.clear();
		empIdElement.sendKeys("12.3");

		WebElement password = webDriver.findElement(By.name("empPass"));
		password.clear();
		password.sendKeys("2222");

		// スクリーンショット
		tempFileList.add(((TakesScreenshot) webDriver).getScreenshotAs(OutputType.FILE));
		
		webDriver.findElement(By.cssSelector("input[type='submit']")).submit();

		// スクリーンショット
		tempFileList.add(((TakesScreenshot) webDriver).getScreenshotAs(OutputType.FILE));

		WebElement errElement = webDriver.findElement(By.cssSelector("p"));

		String errMsgCharTypeOfEmpId = "社員IDは整数値で入力してください。";

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
		assertTrue(errElement.getText().contains(errMsgCharTypeOfEmpId), "エラーメッセージが違います：" + errElement.getText());

	}

	@Test
	@Order(5)
	public void 異常系_ログイン操作_パスワード_空文字入力メッセージ出力() {
		// スクリーンショットのリスト
		ArrayList<File> tempFileList = new ArrayList<File>();

		// 指定したURLに遷移する
		webDriver.get("http://localhost:7779/spring_crud/");

		WebElement empIdElement = webDriver.findElement(By.name("empId"));
		empIdElement.clear();
		empIdElement.sendKeys("1");

		WebElement password = webDriver.findElement(By.name("empPass"));
		password.clear();

		// スクリーンショット
		tempFileList.add(((TakesScreenshot) webDriver).getScreenshotAs(OutputType.FILE));
		
		webDriver.findElement(By.cssSelector("input[type='submit']")).submit();

		// スクリーンショット
		tempFileList.add(((TakesScreenshot) webDriver).getScreenshotAs(OutputType.FILE));

		WebElement errElement = webDriver.findElement(By.cssSelector("p"));

		String errMsgNullOfEmpPass = "パスワードを入力してください。";

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
		assertTrue(errElement.getText().contains(errMsgNullOfEmpPass), "エラーメッセージが違います：" + errElement.getText());

	}

	@Test
	@Order(6)
	public void 異常系_ログイン操作_ログインエラーメッセージ出力() {
		// スクリーンショットのリスト
		ArrayList<File> tempFileList = new ArrayList<File>();

		// 指定したURLに遷移する
		webDriver.get("http://localhost:7779/spring_crud/");

		WebElement empIdElement = webDriver.findElement(By.name("empId"));
		empIdElement.clear();
		empIdElement.sendKeys("5");

		WebElement password = webDriver.findElement(By.name("empPass"));
		password.clear();
		password.sendKeys("5");

		// スクリーンショット
		tempFileList.add(((TakesScreenshot) webDriver).getScreenshotAs(OutputType.FILE));
		
		webDriver.findElement(By.cssSelector("input[type='submit']")).submit();

		// スクリーンショット
		tempFileList.add(((TakesScreenshot) webDriver).getScreenshotAs(OutputType.FILE));

		// MOC要確認
		WebElement errElement = webDriver.findElement(By.cssSelector("p"));

		String errMsgIllegalEmpIdOrEmpPass = "社員ID、またはパスワードが間違っています。";

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
		assertTrue(errElement.getText().contains(errMsgIllegalEmpIdOrEmpPass), "エラーメッセージが違います：" + errElement.getText());

	}

}
