package pro.elab.e2e;

import java.awt.Desktop;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.logging.*;
import junit.framework.TestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class GMailTest extends TestCase {

    private WebDriver driver;
    private Properties properties = new Properties();
    Logger log;
    static File junitReport;
    static BufferedWriter junitWriter;
    final int timeoutInSec = 20;

    @Rule
    public TestRule GMailWatcher = new TestWatcher() {

        @Override
        public Statement apply(Statement base, Description description) {
            return super.apply(base, description);
        }

        @Override
        protected void succeeded(Description description) {
            System.out.println(description.getDisplayName() + " "
                    + "Test Passed!");
            try {
                junitWriter.write(description.getDisplayName() + " "
                        + "success!");
                junitWriter.write("<br/>");
            } catch (Exception e1) {
                System.out.println(e1.getMessage());
            }
        }

        @Override
        protected void failed(Throwable e, Description description) {
            System.out.println(description.getDisplayName() + " "
                    + e.getClass().getSimpleName());
            try {
                junitWriter.write(description.getDisplayName() + " "
                        + e.getClass().getSimpleName());
                junitWriter.write("<br/>");
            } catch (Exception e2) {
                System.out.println(e2.getMessage());
            }
        }
    };

    public void setUp() throws Exception {
        System.setProperty("webdriver.chrome.driver", System.getProperty("user.dir") + "/chromedriver");
        driver = new ChromeDriver();
        properties.load(new FileReader(new File("test.properties")));
        log = LogManager.getLogManager().getLogger("");

        String junitReportFile = System.getProperty("user.dir")
                + "/junitReportFile.html";
        DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        Date date = new Date();
        junitReport = new File(junitReportFile);
        junitWriter = new BufferedWriter(new FileWriter(junitReport));
        junitWriter.write("<html><head><title>Test Report</title></head><body>");
        junitWriter.write("<h1>Test Execution Summary - " + dateFormat.format(date)
                + "</h1>");
    }

    public void tearDown() throws Exception {
        driver.quit();
        junitWriter.write("</body></html>");
        junitWriter.close();
        Desktop.getDesktop().browse(junitReport.toURI());
        Desktop.getDesktop().browse(new File(System.getProperty("user.dir") + "/build/reports/tests/test/index.html").toURI());
    }

    @Test
    public void testSendEmail() throws Exception {
        long start = System.currentTimeMillis();
        String testSubject = "Subject for the test " + start;
        String testBody = "Text in body for the test " + start;
        String textSocial = "Social";

        driver.get("https://mail.google.com/");
// - Login to Gmail
        WebElement userElement = driver.findElement(By.id("identifierId"));
        userElement.sendKeys(properties.getProperty("username"));

        driver.findElement(By.id("identifierNext")).click();

        assertTrue(waitForElement("//div[@id='passwordNext']"));

        WebElement passwordElement = driver.findElement(By.id("password"));
        Actions actions = new Actions(driver);
        actions.moveToElement(passwordElement);
        actions.click();
        actions.sendKeys(properties.getProperty("password"));
        actions.build().perform();
        driver.findElement(By.id("passwordNext")).click();

        Thread.sleep(1000);
// - Compose an email with unique subject and body
        assertTrue(waitForElement("//*[@role='button' and (.)='Compose']"));
        WebElement composeElement = driver.findElement(By.xpath("//*[@role='button' and (.)='Compose']"));
        composeElement.click();

        WebElement msgTo = driver.findElement(By.name("to"));
        msgTo.click();
        msgTo.clear();
        msgTo.sendKeys(String.format("%s@gmail.com", properties.getProperty("username")));

        driver.findElement(By.name("subjectbox")).sendKeys(testSubject);
        WebElement msgBody = driver.findElement(By.xpath("//div[@role='textbox']"));
        msgBody.clear();
        msgBody.sendKeys(testBody);

// - Label email as "Social"
        driver.findElement(By.xpath("(.//*[normalize-space(text()) and normalize-space(.)='Send'])[1]/following::div[52]")).click();
        driver.findElement(By.xpath("(.//*[normalize-space(text()) and normalize-space(.)='Default to full-screen'])[1]/following::div[3]")).click();
        WebElement tagField = driver.findElement(By.xpath("(//input[@type='text'])[5]"));
        tagField.click();
        tagField.clear();
        tagField.sendKeys("Social");
        tagField.sendKeys(Keys.ENTER);

// - Send the email to the same account which was used to login (from and to addresses would be the same)
        driver.findElement(By.xpath("//*[@role='button' and text()='Send']")).click();

        String ourMsgXpath = ".//table/tbody/tr/td/div/div/div/span/span[@class='bqe' and contains(text(),'" + testSubject + "')]";
        waitForElement(ourMsgXpath);
// - Mark email as starred
        WebElement msg = driver.findElement(By.xpath(ourMsgXpath + "/../../../../../..//span[@title=\"Not starred\"]"));
        msg.click();
        Thread.sleep(1000);
// - Open the received email
        msg = driver.findElement(By.xpath(ourMsgXpath));
        msg.click();
        waitForElement("//h2[@class='hP']");
// - Verify email came under proper Label i.e. "Social"
        String socialBtnXpath = "//td/div[@name='Social' and @role='button']";
        waitForElement(socialBtnXpath);
        WebElement social = driver.findElement(By.xpath(socialBtnXpath));
        assertTrue("No social tag", social.isDisplayed());
        Thread.sleep(15000);
// - Verify the subject and body of the received email
        WebElement subj = driver.findElement(By.className("hP"));
        assertTrue("Subject is not equals", subj.getText().equals(testSubject));
        WebElement body = driver.findElement(By.xpath("//div[@role=\"gridcell\"]"));
        assertTrue("Body is not equals", body.getText().contains(testBody));
// - Generate test execution report at the end
    }

    /**
     * Print debug information with page content
     *
     */
    private void printPage() {
        WebElement msg = driver.findElement(By.xpath("//body"));
        log.log(Level.INFO, "----------------------- BODY ---------------------------");
        log.log(Level.INFO, msg.getAttribute("innerHTML"));
        log.log(Level.INFO, "--------------------------------------------------------");
    }

    /**
     * Waiting for an element
     *
     * @param xpathString
     * @throws InterruptedException
     */
    private boolean waitForElement(String xpathString) throws InterruptedException {
        WebElement el;
        boolean foundElement = false;
        log.log(Level.INFO, "TRY TO FIND " + xpathString);
        for (int second = 0;; second++) {
            if (second >= timeoutInSec) {
                printPage();
                fail("timeout");
            }
            try {
                el = driver.findElement(By.xpath(xpathString));
                if (el != null) {
                    log.log(Level.INFO, "FOUND " + xpathString);
                    if (el.isDisplayed()) {
                        log.log(Level.INFO, "FOUND VISIBLE " + xpathString);
                        foundElement = true;
                        break;
                    }
                }
            } catch (Exception e) {
                //
            }
            Thread.sleep(1000);
        }
        return foundElement;
    }

    /**
     * Waiting for an element with text
     *
     * @param xpathString
     * @param text
     * @throws InterruptedException
     */
    private boolean waitForText(String xpathString, String text) throws InterruptedException {
        WebElement el;
        boolean foundElement = false;
        log.log(Level.INFO, "TRY TO FIND TEXT " + text + " IN " + xpathString);
        for (int second = 0;; second++) {
            if (second >= timeoutInSec) {
                printPage();
                fail("timeout");
            }
            try {
                el = driver.findElement(By.xpath(xpathString));
                if (el != null && el.isDisplayed() && el.getText().contains(text)) {
                    log.log(Level.INFO, "FOUND VISIBLE TEXT " + xpathString);
                    foundElement = true;
                    break;
                }
            } catch (Exception e) {
                //
            }
            Thread.sleep(1000);
        }
        return foundElement;
    }

}
