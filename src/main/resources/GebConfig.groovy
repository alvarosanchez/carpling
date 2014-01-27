import org.openqa.selenium.chrome.ChromeDriver

waiting {
    timeout = 5
}
def chromeDriver = new File('test/drivers/chrome/chromedriver')
downloadDriver(chromeDriver, "http://chromedriver.storage.googleapis.com/2.8/chromedriver_mac32.zip")
System.setProperty('webdriver.chrome.driver', chromeDriver.absolutePath)
driver = { new ChromeDriver() }


private void downloadDriver(File file, String path) {
    if (!file.exists()) {
        def ant = new AntBuilder()
        ant.get(src: path, dest: 'driver.zip')
        ant.unzip(src: 'driver.zip', dest: file.parent)
        ant.delete(file: 'driver.zip')
        ant.chmod(file: file, perm: '700')
    }
}
