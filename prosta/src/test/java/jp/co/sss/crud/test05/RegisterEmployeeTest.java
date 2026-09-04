package jp.co.sss.crud.test05;

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
import org.openqa.selenium.support.ui.Select;

import com.google.common.io.Files;

@TestMethodOrder(OrderAnnotation.class)
@DisplayName("05_社員登録機能")
public class RegisterEmployeeTest {

	private WebDriver webDriver;
	// スクショ保存パス
	private String screenshotPath= "screenshots\\05_RegisterRmployeeTest\\";
	
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
	public void 正常系_社員登録操作_登録完了() {
		// スクリーンショットのリスト
		ArrayList<File> tempFileList = new ArrayList<File>();
		
		doLogin();

		/*****社員一覧から入力画面へ*****/
		WebElement registerLink = webDriver.findElement(By.linkText("新規社員登録"));

		// スクリーンショット
		tempFileList.add(((TakesScreenshot) webDriver).getScreenshotAs(OutputType.FILE));
		
		registerLink.click();

		WebElement articleInputTitle = webDriver.findElement(By.cssSelector("article h3"));
		//検証
		assertEquals("社員登録入力画面", articleInputTitle.getText());

		/*****社員入力から確認画面へ*****/
		WebElement empPassElement = webDriver.findElement(By.name("empPass"));
		empPassElement.sendKeys("4444");

		WebElement empNameElement = webDriver.findElement(By.cssSelector(".update input[name='empName']"));
		empNameElement.sendKeys("佐藤四郎");

		//radioボタン
		webDriver.findElement(By.cssSelector("input[name='gender'][value='1']")).click();

		WebElement addressElement = webDriver.findElement(By.name("address"));
		addressElement.sendKeys("千葉県");

		WebElement birthdayElement = webDriver.findElement(By.name("birthday"));
		birthdayElement.sendKeys("1979/07/02");

		//radioボタン
		webDriver.findElement(By.cssSelector("input[name='authority'][value='2']")).click();

		//プルダウン
		Select select = new Select(webDriver.findElement(By.cssSelector(".update select[name='deptId']")));
		select.selectByValue("2");

		// スクリーンショット
		tempFileList.add(((TakesScreenshot) webDriver).getScreenshotAs(OutputType.FILE));
		
		webDriver.findElement(By.cssSelector("input[value='登録']")).submit();

		WebElement articleCheckTitle = webDriver.findElement(By.cssSelector("article h3"));
		WebElement checkEmpNameElement = webDriver.findElement(By.cssSelector(".update .form:nth-of-type(2) .input"));
		WebElement checkGenderElement = webDriver.findElement(By.cssSelector(".update .form:nth-of-type(3) .input"));
		WebElement checkAddressElement = webDriver.findElement(By.cssSelector(".update .form:nth-of-type(4) .input"));
		WebElement checkBirthdayElement = webDriver.findElement(By.cssSelector(".update .form:nth-of-type(5) .input"));
		WebElement checkAuthorityElement = webDriver.findElement(By.cssSelector(".update .form:nth-of-type(6) .input"));
		WebElement checkDeptNameElement = webDriver.findElement(By.cssSelector(".update .form:nth-of-type(7) .input"));

		// 検証
		assertEquals("社員登録確認画面", articleCheckTitle.getText());
		assertEquals("佐藤四郎", checkEmpNameElement.getText());
		assertEquals("男性", checkGenderElement.getText());
		assertEquals("千葉県", checkAddressElement.getText());
		assertEquals("1979/07/02", checkBirthdayElement.getText());
		assertEquals("管理者", checkAuthorityElement.getText());
		assertEquals("経理部", checkDeptNameElement.getText());

		// スクリーンショット
		tempFileList.add(((TakesScreenshot) webDriver).getScreenshotAs(OutputType.FILE));
		
		/*****社員確認から完了画面へ*****/
		webDriver.findElement(By.cssSelector(".update .input input[value='登録']")).submit();
		
		WebElement articleCompleteTitle = webDriver.findElement(By.cssSelector("article h3"));
		//検証
		assertEquals("社員登録完了画面", articleCompleteTitle.getText());
		
		// スクリーンショット
		tempFileList.add(((TakesScreenshot) webDriver).getScreenshotAs(OutputType.FILE));
		
		/*****社員完了から一覧画面へ*****/

		WebElement logoutWebElement = webDriver.findElement(By.linkText("一覧表示に戻る"));

		logoutWebElement.click();

		// スクリーンショット
		tempFileList.add(((TakesScreenshot) webDriver).getScreenshotAs(OutputType.FILE));

		WebElement empId = webDriver.findElement(By.cssSelector("table tr:nth-of-type(5) td:nth-of-type(1)"));
		WebElement empName = webDriver.findElement(By.cssSelector("table tr:nth-of-type(5) td:nth-of-type(2)"));
		WebElement gender = webDriver.findElement(By.cssSelector("table tr:nth-of-type(5) td:nth-of-type(3)"));
		WebElement address = webDriver.findElement(By.cssSelector("table tr:nth-of-type(5) td:nth-of-type(4)"));
		WebElement birthday = webDriver.findElement(By.cssSelector("table tr:nth-of-type(5) td:nth-of-type(5)"));
		WebElement auth = webDriver.findElement(By.cssSelector("table tr:nth-of-type(5) td:nth-of-type(6)"));
		WebElement departmentName = webDriver.findElement(By.cssSelector("table tr:nth-of-type(5) td:nth-of-type(7)"));

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
		assertEquals("4", empId.getText());
		assertEquals("佐藤四郎", empName.getText());
		assertEquals("男性", gender.getText());
		assertEquals("千葉県", address.getText());
		assertEquals("1979/07/02", birthday.getText());
		assertEquals("管理者", auth.getText());
		assertEquals("経理部", departmentName.getText());

	}

	@Test
	@Order(2)
	public void 正常系_社員登録操作_入力画面_戻るボタンを押下する() {
		// スクリーンショットのリスト
		ArrayList<File> tempFileList = new ArrayList<File>();

		doLogin();

		/*****社員一覧から入力画面へ*****/
		WebElement registerLink = webDriver.findElement(By.linkText("新規社員登録"));
		
		// スクリーンショット
		tempFileList.add(((TakesScreenshot) webDriver).getScreenshotAs(OutputType.FILE));
		
		registerLink.click();

		// スクリーンショット
		tempFileList.add(((TakesScreenshot) webDriver).getScreenshotAs(OutputType.FILE));

		webDriver.findElement(By.cssSelector(".update .input input[value='戻る']")).submit();

		// スクリーンショット
		tempFileList.add(((TakesScreenshot) webDriver).getScreenshotAs(OutputType.FILE));
		
		WebElement articleCompleteTitle = webDriver.findElement(By.cssSelector("article h3"));

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
		
		//検証
		assertEquals("社員一覧画面", articleCompleteTitle.getText());

	}

	@Test
	@Order(3)
	public void 正常系_社員登録操作_確認画面_戻るボタンを押下する() {
		// スクリーンショットのリスト
		ArrayList<File> tempFileList = new ArrayList<File>();
		
		doLogin();
		/*****社員一覧から入力画面へ*****/
		WebElement registerLink = webDriver.findElement(By.linkText("新規社員登録"));
		
		// スクリーンショット
		tempFileList.add(((TakesScreenshot) webDriver).getScreenshotAs(OutputType.FILE));
		
		registerLink.click();
		
		/*****社員入力から確認画面へ*****/
		WebElement empPassElement = webDriver.findElement(By.name("empPass"));
		empPassElement.sendKeys("4444");

		WebElement empNameElement = webDriver.findElement(By.cssSelector(".update input[name='empName']"));
		empNameElement.sendKeys("佐藤四郎");

		//radioボタン
		webDriver.findElement(By.cssSelector("input[name='gender'][value='1']")).click();

		WebElement addressElement = webDriver.findElement(By.name("address"));
		addressElement.sendKeys("千葉県");

		WebElement birthdayElement = webDriver.findElement(By.name("birthday"));
		birthdayElement.sendKeys("1979/07/02");

		//radioボタン
		webDriver.findElement(By.cssSelector("input[name='authority'][value='2']")).click();

		//プルダウン
		Select select = new Select(webDriver.findElement(By.cssSelector(".update select[name='deptId']")));
		select.selectByValue("2");
		
		// スクリーンショット
		tempFileList.add(((TakesScreenshot) webDriver).getScreenshotAs(OutputType.FILE));
		
		webDriver.findElement(By.cssSelector("input[value='登録']")).submit();

		// スクリーンショット
		tempFileList.add(((TakesScreenshot) webDriver).getScreenshotAs(OutputType.FILE));
		
		/*****社員確認から入力画面へ*****/
		webDriver.findElement(By.cssSelector(".update .input input[value='戻る']")).submit();

		// スクリーンショット
		tempFileList.add(((TakesScreenshot) webDriver).getScreenshotAs(OutputType.FILE));

		WebElement articleInputTitle = webDriver.findElement(By.cssSelector("article h3"));

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
		
		//検証
		assertEquals("社員登録入力画面", articleInputTitle.getText());

	}

}
