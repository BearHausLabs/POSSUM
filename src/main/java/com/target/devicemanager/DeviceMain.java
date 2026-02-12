package com.target.devicemanager;

import com.target.devicemanager.common.JavaPOSDeviceDiscoveryService;
import com.target.devicemanager.common.StructuredEventLogger;
import com.target.devicemanager.common.entities.DeviceDiscoveryResponse;
import com.target.devicemanager.common.entities.DeviceTestResult;
import com.target.devicemanager.configuration.WorkstationConfig;
import jpos.util.JposPropertiesConst;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SpringBootApplication
@EnableScheduling
public class DeviceMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceMain.class);
    private static final StructuredEventLogger log = StructuredEventLogger.of("DeviceManager", "DeviceMain", LOGGER);

    @Autowired(required = false)
    private WorkstationConfig workstationConfig;

    @Autowired(required = false)
    private JavaPOSDeviceDiscoveryService discoveryService;

    public static void main(String[] args) {
        // Respect -Djpos.config.populatorFile from the launch script if provided;
        // default to "devcon.xml" on the classpath when not specified externally.
        if (System.getProperty(JposPropertiesConst.JPOS_POPULATOR_FILE_PROP_NAME) == null) {
            System.setProperty(JposPropertiesConst.JPOS_POPULATOR_FILE_PROP_NAME, "devcon.xml");
        }
        System.setProperty("jpos.config.regPopulatorClass", "jpos.config.simple.xml.SimpleXmlRegPopulator");
        System.setProperty(JposPropertiesConst.JPOS_SERVICE_MANAGER_CLASS_PROP_NAME2, "jpos.loader.simple.SimpleServiceManager");
        System.setProperty("jpos.util.tracing.TurnOnAllNamedTracers", "OFF");
        ConfigurableApplicationContext dmcontext = SpringApplication.run(DeviceMain.class,args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void crashCountInStartup() {
        Boolean isSimulationMode = Boolean.parseBoolean(System.getProperty("useSimulators"));
        if(!isSimulationMode) {
            try {
                String logPath = System.getenv("POSSUM_LOG_PATH");
                if (logPath == null) {
                    log.success("Setting default log path for POSSUM.", 5);
                    logPath = "/var/log/target/possum";
                }

                //get the latest dump file also log the crash count since rebuild
                File latestLogFile = getLatestLogFile(logPath + "/CrashLog");

                if (latestLogFile != null) {
                    String coreDump = getCoreDumpInfo(latestLogFile);
                    if (coreDump != null && coreDump.length() > 0 ) {
                        log.success(coreDump, 9);
                    }
                }

                // count crash file since reboot
                File crashCount = new File(logPath + "/crashCount.log");
                if (crashCount.exists() && crashCount.isFile()) {
                    try {
                        String count = (new BufferedReader(new FileReader(crashCount))).readLine();
                        if (!count.equals("0")) {
                            log.success("Current POSSUM start count after reboot is: " + count, 9);
                        }
                    } catch (IOException ioException) {
                        log.failure("Error reading crash count", 17, ioException);
                    }
                }
            } catch (Exception exception) {
                log.failure("Error getting crash log file path", 17, exception);
            }
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(100) // Run after other startup listeners
    public void startupDeviceDiscovery() {
        if (workstationConfig != null && workstationConfig.isTestOnStartup() && discoveryService != null) {
            Boolean isSimulationMode = Boolean.parseBoolean(System.getProperty("useSimulators"));
            if (isSimulationMode) {
                log.success("Simulation mode -- skipping startup device discovery tests", 5);
                return;
            }

            log.success("=== Startup Device Discovery Test ===", 5);
            try {
                DeviceDiscoveryResponse response = discoveryService.testAllDevices();
                if (response.getTestResults() != null) {
                    int total = response.getTestResults().size();
                    long functional = response.getTestResults().stream()
                            .filter(DeviceTestResult::isFunctional).count();
                    log.success("Discovery complete: " + functional + "/" + total +
                            " devices functional", 5);
                    for (DeviceTestResult result : response.getTestResults()) {
                        String status = result.isFunctional() ? "OK" : "FAIL";
                        log.success("  [" + status + "] " + result.getLogicalName() +
                                " (" + result.getCategory() + ") - " +
                                result.getMessage() + " [" + result.getTestDurationMs() + "ms]", 9);
                    }
                }
            } catch (Exception e) {
                log.failure("Startup device discovery failed: " + e.getMessage(), 1, e);
            }
            log.success("=== End Startup Device Discovery ===", 5);
        }
    }

    public File getLatestLogFile(String filePath) {
        //get the latest dump file
        File crashdir = new File(filePath);
        File[] files = crashdir.listFiles();
        File lastModifiedFile = null;
        if (files.length > 0) {
            log.success("Current POSSUM crash count since rebuild is: " + files.length, 9);

            //find the latest log file
            lastModifiedFile = files[0];
            for (int i = 1; i < files.length; i++) {
                if (lastModifiedFile.lastModified() < files[i].lastModified()) {
                    lastModifiedFile = files[i];
                }
            }
        }
        return lastModifiedFile;
    }

    public String getCoreDumpInfo (File logfile){
        String coreDumpInfo = null;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(logfile));
            String probFrame = "";
            String crashTime = "";
            String line;
            while (null != (line = reader.readLine())) {
                if (line.contains("Problematic frame:")) {
                    probFrame = reader.readLine();
                }
                if (line.contains("Time:")){
                    crashTime = line;
                    break;
                }
            }
            reader.close();

            if (crashTime != "") {
                String time =  parseAndFormatCrashTime(crashTime);

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm:ss");
                try {
                    LocalDateTime localTimeObj = LocalDateTime.parse(time, formatter);

                    Duration duration = Duration.between(LocalDateTime.now(), localTimeObj);
                    long diff = Math.abs(duration.toMinutes());
                    if (diff <= 120) {
                        coreDumpInfo = "core dump happened within 2 hours - " + diff + " mins ago - " + logfile.getName() + " : " + probFrame + "; Crash " + crashTime;
                    } else {
                        coreDumpInfo = "core dump happened  - " + diff + " mins ago - " + logfile.getName() + " : " + probFrame + "; Crash " + crashTime;
                    }
                } catch (DateTimeParseException exp) {
                    log.failure("Failed to parsing the crash time: " + exp.getMessage(), 17, exp);
                }
            }
        } catch (IOException ioException) {
            log.failure("Failed reading core dump file" + logfile.getName() + ioException.getMessage(), 17, ioException);
        }
        return coreDumpInfo;
    }

    public String parseAndFormatCrashTime(String crashTime) {
        //parse the crash time
        String yyyy = "";
        String mon = "";
        String dd = "";
        String hhmmss = "";
        String pattern = "Time: \\S{3}\\s+(\\S{3})\\s+(\\d+)\\s+(.*)\\s+(\\d{4})\\s+\\S{3} elapsed";
        Pattern p = Pattern.compile(pattern);
        Matcher m  = p.matcher(crashTime);
        if (m.find( )) {
            mon =  m.group(1);
            dd = m.group(2);
            if (dd.length() == 1) {
                dd = "0"+dd;
            }
            hhmmss = m.group(3);
            yyyy = m.group(4);

        }
        return yyyy + "-" + mon + "-" +dd + " " + hhmmss;
    }
}
