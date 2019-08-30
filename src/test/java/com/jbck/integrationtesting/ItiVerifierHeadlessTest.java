package com.jbck.integrationtesting;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ItiVerifierHeadlessTest {

    private static Logger log = Logger.getLogger(ItiVerifierHeadlessTest.class);

    private static final String LINUX_CHROME = System.getProperty("java.io.tmpdir") + "/integration_testing/chrome-linux/chrome";
    private static final String CHROMEDRIVER = System.getProperty("java.io.tmpdir") + "/integration_testing/chromedriver";
    private static final String VERIFIER_2_4 = "https://verificador.iti.gov.br/verifier-2.4/";
    private static final String PADES_SIGNED = "src/test/resources/Pades_Signed.pdf";
    private static final String CADES_SIGNED = "src/test/resources/Cades_Signed.pdf.p7s";

    private WebDriver driver;
    private String baseUrl;

    @Before
    public void setUp() {
        System.setProperty("webdriver.chrome.driver", CHROMEDRIVER);
        ChromeOptions options = new ChromeOptions();
        options.setBinary(LINUX_CHROME);
        options.addArguments("--headless", "--ignore-certificate-errors");
        driver = new ChromeDriver(options);
        baseUrl = VERIFIER_2_4;
        driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
    }

    @Test
    public void testPadesITIVerifier_2_4() {
        Map<String, String> items;

        Given:
        {
            driver.get(baseUrl);
        }

        When:
        {
            driver.findElement(By.xpath("//*[@id='signature_file0']")).sendKeys(new File(PADES_SIGNED).getAbsolutePath());
            driver.findElement(By.id("btn_verify")).click();

            log.info("Vers�o do software: " + driver.findElement(By.xpath("//*[@id=\"reportcontainer\"]/details/table/tbody/tr[2]/td")).getText());
            items = parse(driver.getPageSource());
        }

        Then:
        {
            // Informa��es de pol�tica
            Assert.assertTrue(items.get("Status da PA").contains("V�lid"));
            Assert.assertEquals(items.get("�ntegra segundo a LPA"), "Sim");
            Assert.assertEquals(items.get("�ntegra"), "Sim");
            // Informa��es da assinatura
            Assert.assertTrue(items.get("Cifra assim�trica").contains("V�lid"));
            Assert.assertEquals(items.get("Resumo criptogr�fico"), "Correto");
            Assert.assertTrue(items.get("Emissor").contains("ICP-Brasil"));
            Assert.assertTrue(items.get("Atributos obrigat�rios").contains("V�lid"));
            Assert.assertTrue(items.get("SignatureDictionary").contains("V�lid"));
            // Atributos obrigat�rios
            Assert.assertTrue(items.get("IdMessageDigest").contains("V�lid"));
            Assert.assertTrue(items.get("IdContentType").contains("V�lid"));
            Assert.assertTrue(items.get("IdAaEtsSigPolicyId").contains("V�lid"));
            Assert.assertTrue(items.get("IdAaSigningCertificateV2").contains("V�lid"));
        }
    }

    @Test
    public void testCadesITIVerifier_2_4() {
        Map<String, String> items;

        Given:
        {
            driver.get(baseUrl);
        }

        When:
        {
            driver.findElement(By.xpath("//*[@id='signature_file0']")).sendKeys(new File(CADES_SIGNED).getAbsolutePath());
            driver.findElement(By.id("btn_verify")).click();

            log.info("Vers�o do software: " + driver.findElement(By.xpath("//*[@id=\"reportcontainer\"]/details/table/tbody/tr[2]/td")).getText());
            items = parse(driver.getPageSource());
        }

        Then:
        {
            // Informa��es de pol�tica
            Assert.assertTrue(items.get("Status da PA").contains("V�lid"));
            Assert.assertEquals(items.get("�ntegra segundo a LPA"), "Sim");
            Assert.assertEquals(items.get("�ntegra"), "Sim");
            // Informa��es da assinatura
            Assert.assertTrue(items.get("Emissor").contains("ICP-Brasil"));
            // Atributos obrigat�rios
            Assert.assertTrue(items.get("IdContentType").contains("V�lid"));
            Assert.assertTrue(items.get("IdAaEtsSigPolicyId").contains("V�lid"));
            Assert.assertTrue(items.get("IdAaSigningCertificateV2").contains("V�lid"));
            // Atributos opcionais
            Assert.assertTrue(items.get("IdSigningTime").contains("V�lid"));
        }
    }

    private Map<String, String> parse(String pageSource) {
        Map<String, String> items = new HashMap<String, String>();
        for (Element table : Jsoup.parse(pageSource).normalise().body().select("details table")) {
            for (Element row : table.select("tr")) {
                items.put(row.select("th").text().trim(), row.select("td").text().trim());
            }
        }
        return items;
    }

    @After
    public void tearDown() {
        driver.quit();
    }

}
