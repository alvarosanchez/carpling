import org.openqa.selenium.firefox.FirefoxDriver

waiting {
    timeout = 10
}
/*
def downloadUrls = [
    'Mac OS X': 'http://chromedriver.storage.googleapis.com/2.8/chromedriver_mac32.zip',
    'Linux':    'http://chromedriver.storage.googleapis.com/2.8/chromedriver_linux64.zip'
]

def chromeDriver = new File('test/drivers/chrome/chromedriver')

downloadDriver(chromeDriver, downloadUrls[System.getProperty('os.name')])
System.setProperty('webdriver.chrome.driver', chromeDriver.absolutePath)

driver = { new ChromeDriver() }


private void downloadDriver(File file, path) {
    if (!file.exists()) {
        def ant = new AntBuilder()
        ant.get(src: path, dest: 'driver.zip')
        ant.unzip(src: 'driver.zip', dest: file.parent)
        ant.delete(file: 'driver.zip')
        ant.chmod(file: file, perm: '700')
    }
}
*/

driver = { new FirefoxDriver() }
