import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedList;
public class SmartHomeSystem {
    private static LocalDateTime currentTime;
    private static LinkedList<SmartDevice> devices = new LinkedList<>();
    public static void main(String[] args) {
        // Argüman sayısını kontrol et
        if (args.length < 2) {
            System.out.println("Lütfen input ve output dosyalarını argüman olarak girin.");
            return;
        }

        String inputFile = args[0];  // Örn: input.txt
        String outputFile = args[1]; // Örn: output.txt

        // try-with-resources kullanarak okuyucu ve yazıcıyı oluşturuyoruz (işlem bitince otomatik kapanırlar)
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
             PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
            // İlk satırı döngüden ÖNCE ayrıca oku
            String firstLine;

            while ((firstLine = reader.readLine()) != null && firstLine.trim().isEmpty()) {
                // Baştaki boş satırları yok say
            }

            if (firstLine == null) {
                writer.println("COMMAND: null");
                writer.println("ERROR: First command must be set initial time! Program is going to terminate!");
                return;
            }

            writer.println("COMMAND: " + firstLine);
            String[] firstParts = firstLine.split("\t");
            if (firstParts.length != 2 || !firstParts[0].equals("SetInitialTime")) {
                writer.println("ERROR: First command must be set initial time! Program is going to terminate!");
                return; // programı durdur
            }
            try {
                currentTime = parseDateTime(firstParts[1]);
                writer.println("SUCCESS: Time has been set to " + 
                    formatDateTime(currentTime) +"!");
            } catch (Exception e) {
                writer.println("ERROR: Format of the initial date is wrong! Program is going to terminate!");
                return; // programı durdur
            }

            String line;
            boolean lastCommandWasZReport = false;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                writer.println("COMMAND: " + line);

                String[] parts = line.split("\t", -1);
                String command = parts[0];
                lastCommandWasZReport = command.equals("ZReport");
                // Komuta göre ilgili işlemleri çağır
                switch (command) {
                    case "SetTime":
                        if (parts.length != 2) {
                            writer.println("ERROR: Erroneous command!");
                            break;
                        }

                        try {
                            LocalDateTime newTime = parseDateTime(parts[1]);

                            if (newTime.isBefore(currentTime)) {
                                throw new SmartHomeException("ERROR: Time cannot be reversed!");
                            }

                            if (newTime.isEqual(currentTime)) {
                                throw new SmartHomeException("ERROR: There is nothing to change!");
                            }

                            currentTime = newTime;

                            processDueSwitches();

                        } catch (SmartHomeException e) {
                            writer.println(e.getMessage());
                        } catch (Exception e) {
                            writer.println("ERROR: Time format is not correct!");
                        }

                        break;
                    case "SkipMinutes":
                        if (parts.length != 2) {
                            writer.println("ERROR: Erroneous command!");
                            continue;
                        }
                        try {
                            int minutes = Integer.parseInt(parts[1]);
                            if (minutes < 0) {
                                throw new SmartHomeException("ERROR: Time cannot be reversed!");
                            }
                            if (minutes == 0) {
                                throw new SmartHomeException("ERROR: There is nothing to skip!");
                            }
                            currentTime = currentTime.plusMinutes(minutes);
                            for (SmartDevice device : devices) {
                                if (device.getSwitchTime() != null && device.getSwitchTime().isAfter(currentTime)) {
                                    if (device.getInitialStatus()) {
                                        if (device instanceof SmartCamera) {
                                            SmartCamera camera = (SmartCamera) device;
                                            long mins = ChronoUnit.MINUTES.between(camera.getSwitchOnTime(), currentTime);
                                            camera.addStorage(camera.getMbPerMinute() * mins);
                                        } else if (device instanceof SmartPlug) {
                                            SmartPlug plug = (SmartPlug) device;
                                            if (plug.getIsPluggedIn()) {
                                                long mins = ChronoUnit.MINUTES.between(plug.getPlugInTime(), currentTime);
                                                plug.setTotalEnergyConsumption(plug.getTotalEnergyConsumption() + plug.getVoltage() * plug.getAmpere() * (mins / 60.0));
                                            }
                                        }
                                        device.switchOff();
                                    } else {
                                        if (device instanceof SmartCamera) {
                                            ((SmartCamera) device).setSwitchOnTime(currentTime);
                                        } else if (device instanceof SmartPlug) {
                                            SmartPlug plug = (SmartPlug) device;
                                            if (plug.getIsPluggedIn()) {
                                                plug.setPlugInTime(currentTime);
                                            }
                                        }
                                        device.switchOn();
                                    }
                                    device.setLastSwitchTime(device.getSwitchTime()); // kaydet
                                    device.switchOff(); // sonra switch yap

                                    device.setSwitchTime(null);
                                }
                            }
                            Collections.sort(devices);
                        } catch (SmartHomeException e) {
                            writer.println(e.getMessage());
                        } catch (Exception e) {
                            writer.println("ERROR: Time format is not correct!");
                        }
                        break;
                    case "Nop":
                        if (parts.length != 1) {
                            writer.println("ERROR: Erroneous command!");
                            break;
                        }

                        Collections.sort(devices);

                        try {
                            if (devices.isEmpty() || devices.get(0).getSwitchTime() == null) {
                                throw new SmartHomeException("ERROR: There is nothing to switch!");
                            }

                            LocalDateTime nextSwitchTime = devices.get(0).getSwitchTime();
                            currentTime = nextSwitchTime;

                            for (SmartDevice device : devices) {
                                if (device.getSwitchTime() != null && device.getSwitchTime().isEqual(nextSwitchTime)) {

                                    if (device.getInitialStatus()) {
                                        if (device instanceof SmartCamera) {
                                            SmartCamera camera = (SmartCamera) device;
                                            long minutes = ChronoUnit.MINUTES.between(camera.getSwitchOnTime(), currentTime);
                                            camera.addStorage(camera.getMbPerMinute() * minutes);
                                        } else if (device instanceof SmartPlug) {
                                            SmartPlug plug = (SmartPlug) device;
                                            if (plug.getIsPluggedIn()) {
                                                long minutes = ChronoUnit.MINUTES.between(plug.getPlugInTime(), currentTime);
                                                plug.setTotalEnergyConsumption(plug.getTotalEnergyConsumption()
                                                    + plug.getVoltage() * plug.getAmpere() * (minutes / 60.0));
                                            }
                                        }

                                        device.switchOff();

                                    } else {
                                        if (device instanceof SmartCamera) {
                                            ((SmartCamera) device).setSwitchOnTime(currentTime);
                                        } else if (device instanceof SmartPlug) {
                                            SmartPlug plug = (SmartPlug) device;
                                            if (plug.getIsPluggedIn()) {
                                                plug.setPlugInTime(currentTime);
                                            }
                                        }

                                        device.switchOn();
                                    }
                                    device.setLastSwitchTime(device.getSwitchTime()); // kaydet
                                    device.switchOff();
                                    device.setSwitchTime(null);
                                }
                            }

                            Collections.sort(devices);

                        } catch (SmartHomeException e) {
                            writer.println(e.getMessage());
                        }

                        break;
                        
                    case "Add":
                        if (parts.length < 3) {
                            writer.println("ERROR: Erroneous command!");
                            continue;
                        }
                        String type = parts[1];
                        String name = parts[2];

                        boolean exists = false;
                        
                        SmartDevice newDevice = null;
                        try{
                            if (type.equals("SmartPlug")){
                                for (SmartDevice device : devices) {
                                if (device.getName().equals(name)) {
                                    exists = true;
                                    break;
                                }
                                }
                                if (exists) {
                                    writer.println("ERROR: There is already a smart device with same name!");
                                    continue;
                                }
                                SmartPlug plug = null;
                                if (parts.length == 3){
                                    plug = new SmartPlug(name);
                                    newDevice = plug;
                                } else if (parts.length == 4) {
                                    plug = new SmartPlug(name);
                                    if (!parts[3].equals("On") && !parts[3].equals("Off")) {
                                        throw new SmartHomeException("ERROR: Erroneous command!");
                                    }
                                    plug.setInitialStatus(parts[3].equals("On"));
                                    newDevice = plug;
                                } else if (parts.length == 5) {
                                plug = new SmartPlug(name);
                                if (!parts[3].equals("On") && !parts[3].equals("Off")) {
                                    throw new SmartHomeException("ERROR: Erroneous command!");
                                }
                                plug.setInitialStatus(parts[3].equals("On"));
                                plug.setAmpere(Double.parseDouble(parts[4]));
                                plug.plugIn();
                                if (plug.getInitialStatus()) {
                                    plug.setPlugInTime(currentTime);
                                }
                                newDevice = plug;

                                } else {
                                writer.println("ERROR: Erroneous command!");
                                continue;
                                }
                            }
                            else if (type.equals("SmartCamera")){
                                SmartCamera camera = null;
                                for (SmartDevice device : devices) {
                                if (device.getName().equals(name)) {
                                    exists = true;
                                    break;
                                }
                                }
                                if (exists) {
                                    writer.println("ERROR: There is already a smart device with same name!");
                                    continue;
                                }
                                if (parts.length == 4) {
                                    double mbPerMinute = Double.parseDouble(parts[3]);
                                    camera = new SmartCamera(name, mbPerMinute);
                                    newDevice = camera;
                                } else if (parts.length == 5) {
                                    double mbPerMinute = Double.parseDouble(parts[3]);
                                    camera = new SmartCamera(name, mbPerMinute);
                                    if (!parts[4].equals("On") && !parts[4].equals("Off")) {
                                        throw new SmartHomeException("ERROR: Erroneous command!");
                                    }
                                    camera.setInitialStatus(parts[4].equals("On"));
                                    if (camera.getInitialStatus()) {
                                        camera.setSwitchOnTime(currentTime);
                                    }
                                    newDevice = camera;
                                } else {
                                    writer.println("ERROR: Erroneous command!");
                                    continue;
                                }
                            }
                                else if (type.equals("SmartLamp")){
                                    for (SmartDevice device : devices) {
                                    if (device.getName().equals(name)) {
                                        exists = true;
                                        break;
                                    }
                                    }
                                    if (exists) {
                                        writer.println("ERROR: There is already a smart device with same name!");
                                        continue;
                                    }
                                SmartLamp lamp = null;
                                if (parts.length == 3) {
                                    lamp = new SmartLamp(name);
                                    newDevice = lamp;
                                } else if (parts.length == 4) {
                                    lamp = new SmartLamp(name);
                                    if (!parts[3].equals("On") && !parts[3].equals("Off")) {
                                        throw new SmartHomeException("ERROR: Erroneous command!");
                                    }
                                    lamp.setInitialStatus(parts[3].equals("On"));
                                    newDevice = lamp;
                                } else if (parts.length == 6) {
                                    lamp = new SmartLamp(name);
                                    if (!parts[3].equals("On") && !parts[3].equals("Off")) {
                                        throw new SmartHomeException("ERROR: Erroneous command!");
                                    }
                                    lamp.setInitialStatus(parts[3].equals("On"));
                                    lamp.setKelvin(Integer.parseInt(parts[4]));
                                    lamp.setBrightness(Integer.parseInt(parts[5]));
                                    newDevice = lamp;
                                } else{
                                    writer.println("ERROR: Erroneous command!");
                                    continue;
                                }
                            }
                            else if (type.equals("SmartColorLamp")){
                                for (SmartDevice device : devices) {
                                if (device.getName().equals(name)) {
                                    exists = true;
                                    break;
                                }
                                }
                                if (exists) {
                                    writer.println("ERROR: There is already a smart device with same name!");
                                    continue;
                                }
                                SmartColorLamp colorLamp = null;
                                if (parts.length == 3) {
                                    colorLamp = new SmartColorLamp(name);
                                    newDevice = colorLamp;
                                } else if (parts.length == 4) {
                                    colorLamp = new SmartColorLamp(name);
                                    if (!parts[3].equals("On") && !parts[3].equals("Off")) {
                                        throw new SmartHomeException("ERROR: Erroneous command!");
                                    }
                                    colorLamp.setInitialStatus(parts[3].equals("On"));
                                    newDevice = colorLamp;
                                } else if (parts.length == 6) {
                                    colorLamp = new SmartColorLamp(name);
                                    if (!parts[3].equals("On") && !parts[3].equals("Off")) {
                                        throw new SmartHomeException("ERROR: Erroneous command!");
                                    }
                                    colorLamp.setInitialStatus(parts[3].equals("On"));
                                    
                                    if (parts[4].startsWith("0x")){
                                        int colorCode = Integer.decode(parts[4]);
                                        colorLamp.setColorCode(colorCode);
                                    }
                                    else {
                                        int kelvin = Integer.parseInt(parts[4]);
                                        colorLamp.setKelvin(kelvin);
                                        colorLamp.setColorMode(false);
                                    }
                                    int brightness = Integer.parseInt(parts[5]);
                                    colorLamp.setBrightness(brightness);
                                    newDevice = colorLamp;
                                } else{
                                    writer.println("ERROR: Erroneous command!");
                                    continue;
                                }
                                }
                            } catch (SmartHomeException e) {
                                writer.println(e.getMessage());
                                newDevice = null;
                            } catch (Exception e) {
                                writer.println("ERROR: Erroneous command!");
                                newDevice = null;
                            }
                        if (newDevice != null) {
                            devices.add(newDevice);
                            Collections.sort(devices);
                        }
                        break;

                    case "Remove":
                        if (parts.length != 2) {
                            writer.println("ERROR: Erroneous command!");
                            break;
                        }

                        boolean removed = false;

                        for (int i = 0; i < devices.size(); i++) {
                            SmartDevice device = devices.get(i);

                            if (device.getName().equals(parts[1])) {

                                if (device.getInitialStatus()) {
                                    if (device instanceof SmartCamera) {
                                        SmartCamera camera = (SmartCamera) device;
                                        long minutes = ChronoUnit.MINUTES.between(camera.getSwitchOnTime(), currentTime);
                                        camera.addStorage(camera.getMbPerMinute() * minutes);
                                    } else if (device instanceof SmartPlug) {
                                        SmartPlug plug = (SmartPlug) device;

                                        if (plug.getIsPluggedIn()) {
                                            long minutes = ChronoUnit.MINUTES.between(plug.getPlugInTime(), currentTime);
                                            plug.setTotalEnergyConsumption(plug.getTotalEnergyConsumption()
                                                + plug.getVoltage() * plug.getAmpere() * (minutes / 60.0));
                                        }
                                    }

                                    device.switchOff();
                                }
                                device.setLastSwitchTime(device.getSwitchTime());
                                device.switchOff();
                                device.setSwitchTime(null);

                                removed = true;
                                writer.println("SUCCESS: Information about removed smart device is as follows:");
                                writer.println(device.toString());

                                devices.remove(i);
                                break;
                            }
                        }

                        if (!removed) {
                            writer.println("ERROR: There is not such a device!");
                        }

                        break;
                    case "Switch":
                        if (parts.length != 3) {
                            writer.println("ERROR: Erroneous command!");
                            break;
                        }
                        if (!parts[2].equals("On") && !parts[2].equals("Off")) {
                            writer.println("ERROR: Erroneous command!");
                            break;
                        }
                        boolean deviceFound = false;
                        try {
                            for (SmartDevice device : devices) {
                                if (device.getName().equals(parts[1])) {
                                    deviceFound = true;
                                    if (parts[2].equals("On")) {
                                        if (device.getInitialStatus()) {
                                            throw new SmartHomeException("ERROR: This device is already switched on!");
                                        }
                                        if (device instanceof SmartCamera) {
                                            ((SmartCamera) device).setSwitchOnTime(currentTime);
                                        } else if (device instanceof SmartPlug) {
                                            SmartPlug plug = (SmartPlug) device;
                                            if (plug.getIsPluggedIn()) {
                                                plug.setPlugInTime(currentTime);
                                            }
                                        }
                                        device.setLastSwitchTime(device.getSwitchTime());
                                        device.switchOn();
                                        device.setSwitchTime(null);
                                    } else {
                                        if (!device.getInitialStatus()) {
                                            throw new SmartHomeException("ERROR: This device is already switched off!");
                                        }
                                        if (device instanceof SmartCamera) {
                                            SmartCamera camera = (SmartCamera) device;
                                            long minutes = ChronoUnit.MINUTES.between(camera.getSwitchOnTime(), currentTime);
                                            camera.addStorage(camera.getMbPerMinute() * minutes);
                                        } else if (device instanceof SmartPlug) {
                                            SmartPlug plug = (SmartPlug) device;
                                            if (plug.getIsPluggedIn()) {
                                                long minutes = ChronoUnit.MINUTES.between(plug.getPlugInTime(), currentTime);
                                                plug.setTotalEnergyConsumption(plug.getTotalEnergyConsumption() + plug.getVoltage() * plug.getAmpere() * (minutes / 60.0));
                                            }
                                        }
                                        device.setLastSwitchTime(device.getLastSwitchTime());
                                        device.switchOff();
                                        device.setSwitchTime(null);
                                    }
                                    Collections.sort(devices);
                                    break;
                                }
                            }
                            if (!deviceFound) {
                                throw new SmartHomeException("ERROR: There is not such a device!");
                            }
                        } catch (SmartHomeException e) {
                            writer.println(e.getMessage());
                        }
                        break;
                    case "SetSwitchTime":
                        if (parts.length != 3) {
                            writer.println("ERROR: Erroneous command!");
                            break;
                        }
                        boolean found = false;
                        for (SmartDevice device : devices){
                            if (device.getName().equals(parts[1])){
                                found = true;
                                try {
                                    LocalDateTime switchTime = parseDateTime(parts[2]);
                                        if (switchTime.isBefore(currentTime)) {
                                            throw new SmartHomeException("ERROR: Time cannot be reversed!");
                                        }
                                    device.setSwitchTime(switchTime);
                                    Collections.sort(devices);
                                } catch (SmartHomeException e) {
                                    writer.println(e.getMessage());
                                } catch (Exception e) {
                                    writer.println("ERROR: Time format is not correct!");
                                }
                                break;
                            }
                        }
                        if (!found) {
                            writer.println("ERROR: There is not such a device!");
                        }
                        break;
                    case "ChangeName":
                        try {
                            if (parts.length != 3) {
                                throw new SmartHomeException("ERROR: Erroneous command!");
                            }

                            if (parts[1].equals(parts[2])) {
                                throw new SmartHomeException("ERROR: Both of the names are the same, nothing changed!");
                            }

                            boolean newNameExists = false;
                            for (SmartDevice device : devices) {
                                if (device.getName().equals(parts[2])) {
                                    newNameExists = true;
                                    break;
                                }
                            }

                            if (newNameExists) {
                                throw new SmartHomeException("ERROR: There is already a smart device with same name!");
                            }

                            boolean oldNameFound = false;
                            for (SmartDevice device : devices) {
                                if (device.getName().equals(parts[1])) {
                                    oldNameFound = true;
                                    device.setName(parts[2]);
                                    break;
                                }
                            }

                            if (!oldNameFound) {
                                throw new SmartHomeException("ERROR: There is not such a device!");
                            }

                        } catch (SmartHomeException e) {
                            writer.println(e.getMessage());
                        } catch (Exception e) {
                            writer.println("ERROR: Erroneous command!");
                        }

                        break;
                    case "PlugIn":
                        if (parts.length != 3) {
                            writer.println("ERROR: Erroneous command!");
                            break;
                        }
                        boolean plugFound = false;
                        for (SmartDevice device:devices){
                            if (device.getName().equals(parts[1])){
                                plugFound = true;
                                try{
                                    if (!(device instanceof SmartPlug)) {
                                        throw new SmartHomeException("ERROR: This device is not a smart plug!");
                                    }
                                    SmartPlug smartPlug = (SmartPlug) device;
                                    if (smartPlug.getIsPluggedIn()) {
                                        throw new SmartHomeException("ERROR: There is already an item plugged in to that plug!");
                                    }
                                    smartPlug.setAmpere(Double.parseDouble(parts[2]));
                                    smartPlug.plugIn();
                                    if (smartPlug.getInitialStatus()) {
                                        smartPlug.setPlugInTime(currentTime);
                                    }
                                } catch (SmartHomeException e){
                                    writer.println(e.getMessage());
                                } catch (Exception e) {
                                    writer.println("ERROR: Erroneous command!");
                                }
                                break;
                            }
                        }
                        if (!plugFound) {
                            writer.println("ERROR: There is not such a device!");
                        }
                        break;
                    case "PlugOut":
                        if (parts.length != 2) {
                            writer.println("ERROR: Erroneous command!");
                            break;
                        }
                        boolean plugOutFound = false;
                        for (SmartDevice device : devices){
                            if (device.getName().equals(parts[1])){
                                plugOutFound = true;
                                try{
                                    if (!(device instanceof SmartPlug)) {
                                        throw new SmartHomeException("ERROR: This device is not a smart plug!");
                                    }
                                    SmartPlug smartPlug = (SmartPlug) device;
                                    if (!smartPlug.getIsPluggedIn()) {
                                        throw new SmartHomeException("ERROR: This plug has no item to plug out from that plug!");
                                    }
                                    if (smartPlug.getInitialStatus()) {
                                        long minutes = ChronoUnit.MINUTES.between(smartPlug.getPlugInTime(), currentTime);
                                        double energyConsumed = smartPlug.getVoltage() * smartPlug.getAmpere() * (minutes / 60.0);
                                        smartPlug.setTotalEnergyConsumption(smartPlug.getTotalEnergyConsumption() + energyConsumed);
                                    }
                                    smartPlug.plugOut();
                                } catch (SmartHomeException e){
                                    writer.println(e.getMessage());
                                }                                
                                break;
                            }
                        }
                        if (!plugOutFound) {
                            writer.println("ERROR: There is not such a device!");
                        }
                        break;
                    case "SetKelvin":
                        if (parts.length != 3) {
                            writer.println("ERROR: Erroneous command!");
                            break;
                        }
                        boolean kelvinFound = false;
                        for(SmartDevice device : devices) {
                            if (device.getName().equals(parts[1])){
                                kelvinFound = true;
                                try {
                                    if (!(device instanceof SmartLamp)) {
                                        throw new SmartHomeException("ERROR: This device is not a smart lamp!");
                                    }
                                    SmartLamp lamp = (SmartLamp) device;
                                    lamp.setKelvin(Integer.parseInt(parts[2]));
                                    if (device instanceof SmartColorLamp) {
                                        ((SmartColorLamp) device).setColorMode(false);
                                    }
                                } catch (SmartHomeException e) {
                                    writer.println(e.getMessage());
                                } catch (Exception e) {
                                    writer.println("ERROR: Erroneous command!");
                                }
                                break;
                            }
                        }
                        if (!kelvinFound) {
                            writer.println("ERROR: There is not such a device!");
                        }
                        break;
                    case "SetBrightness":
                        if (parts.length != 3) {
                            writer.println("ERROR: Erroneous command!");
                            break;
                        }
                        boolean brightnessFound = false;
                        for (SmartDevice device : devices) {
                            if (device.getName().equals(parts[1])) {
                                brightnessFound = true;
                                try {
                                    if (!(device instanceof SmartLamp)) {
                                        throw new SmartHomeException("ERROR: This device is not a smart lamp!");
                                    }
                                    SmartLamp lamp = (SmartLamp) device;
                                    lamp.setBrightness(Integer.parseInt(parts[2]));
                                } catch (SmartHomeException e) {
                                    writer.println(e.getMessage());
                                } catch (Exception e) {
                                    writer.println("ERROR: Erroneous command!");
                                }
                                break;
                            }
                        }
                        if (!brightnessFound) {
                            writer.println("ERROR: There is not such a device!");
                        }
                        break;
                    case "SetColorCode":
                        if (parts.length != 3) {
                            writer.println("ERROR: Erroneous command!");
                            break;
                        }
                        boolean colorCodeFound = false;
                        for (SmartDevice device : devices) {
                            if (device.getName().equals(parts[1])) {
                                colorCodeFound = true;
                                try {
                                    if (!(device instanceof SmartColorLamp)) {
                                        throw new SmartHomeException("ERROR: This device is not a smart color lamp!");
                                    }
                                    SmartColorLamp colorLamp = (SmartColorLamp) device;
                                    int colorCode = Integer.decode(parts[2]);
                                    colorLamp.setColorCode(colorCode);
                                } catch (SmartHomeException e) {
                                    writer.println(e.getMessage());
                                } catch (NumberFormatException e) {
                                    writer.println("ERROR: Erroneous command!");
                                }
                                break;
                            }
                        }                        
                        if (!colorCodeFound) {
                            writer.println("ERROR: There is not such a device!");
                        }
                        break;
                    case "SetWhite":
                        boolean whiteFound = false;
                        for(SmartDevice device : devices) {
                            if (device.getName().equals(parts[1])){
                                whiteFound = true;
                                try {
                                    if (!(device instanceof SmartLamp)) {
                                        throw new SmartHomeException("ERROR: This device is not a smart lamp!");
                                    }
                                    SmartLamp lamp = (SmartLamp) device;
                                    lamp.setKelvin(Integer.parseInt(parts[2]));
                                    lamp.setBrightness(Integer.parseInt(parts[3]));
                                    if (device instanceof SmartColorLamp) {
                                        ((SmartColorLamp) device).setColorMode(false);
                                    }
                                } catch (SmartHomeException e) {
                                    writer.println(e.getMessage());
                                }
                                break;
                            }
                        }
                        if (!whiteFound) {
                            writer.println("ERROR: There is not such a device!");
                        }
                        break;
                    case "SetColor":
                        if (parts.length != 4) {
                            writer.println("ERROR: Erroneous command!");
                            break;
                        }

                        boolean colorFound = false;

                        for (SmartDevice device : devices) {
                            if (device.getName().equals(parts[1])) {
                                colorFound = true;

                                try {
                                    if (!(device instanceof SmartColorLamp)) {
                                        throw new SmartHomeException("ERROR: This device is not a smart color lamp!");
                                    }

                                    int colorCode = Integer.decode(parts[2]);
                                    int brightness = Integer.parseInt(parts[3]);

                                    if (colorCode < 0x000000 || colorCode > 0xFFFFFF) {
                                        throw new SmartHomeException("ERROR: Color code value must be in range of 0x0-0xFFFFFF!");
                                    }

                                    if (brightness < 0 || brightness > 100) {
                                        throw new SmartHomeException("ERROR: Brightness must be in range of 0%-100%!");
                                    }

                                    SmartColorLamp lamp = (SmartColorLamp) device;
                                    lamp.setColorCode(colorCode);
                                    lamp.setBrightness(brightness);

                                } catch (NumberFormatException e) {
                                    writer.println("ERROR: Erroneous command!");
                                } catch (SmartHomeException e) {
                                    writer.println(e.getMessage());
                                }

                                break;
                            }
                        }

                        if (!colorFound) {
                            writer.println("ERROR: There is not such a device!");
                        }

                        break;
                    case "ZReport":
                        Collections.sort(devices);
                        writer.println("Time is:\t" + formatDateTime(currentTime));
                        for (SmartDevice device : devices) {
                            writer.println(device.toString());
                        }
                        break;
                    default:
                        writer.println("ERROR: Erroneous command!");
                        break;
                }
            }
            if (!lastCommandWasZReport) {
            writer.println("Time is:\t" + formatDateTime(currentTime));

            for (SmartDevice device : devices) {
                writer.println(device.toString());
                }
            }
            
        } catch (IOException e) {
            System.out.println("Dosya okuma/yazma sırasında bir hata oluştu: " + e.getMessage());
        }
    }
    private static LocalDateTime parseDateTime(String text) {
        return LocalDateTime.parse(
                text,
                DateTimeFormatter.ofPattern("yyyy-M-d_H:m:s")
        );
    }

    private static String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss"));
    }
    private static void processDueSwitches() {
        Collections.sort(devices);
        for (SmartDevice device : devices) {
            LocalDateTime switchTime = device.getSwitchTime();

            if (switchTime != null && !switchTime.isAfter(currentTime)) {
                if (device.getInitialStatus()) {
                    if (device instanceof SmartCamera) {
                        SmartCamera camera = (SmartCamera) device;
                        long minutes = ChronoUnit.MINUTES.between(camera.getSwitchOnTime(), switchTime);
                        camera.addStorage(camera.getMbPerMinute() * minutes);
                    } else if (device instanceof SmartPlug) {
                        SmartPlug plug = (SmartPlug) device;

                        if (plug.getIsPluggedIn()) {
                            long minutes = ChronoUnit.MINUTES.between(plug.getPlugInTime(), switchTime);
                            plug.setTotalEnergyConsumption(
                                    plug.getTotalEnergyConsumption()
                                            + plug.getVoltage() * plug.getAmpere() * (minutes / 60.0)
                            );
                        }
                    }

                    device.switchOff();

                } else {
                    if (device instanceof SmartCamera) {
                        ((SmartCamera) device).setSwitchOnTime(switchTime);
                    } else if (device instanceof SmartPlug) {
                        SmartPlug plug = (SmartPlug) device;

                        if (plug.getIsPluggedIn()) {
                            plug.setPlugInTime(switchTime);
                        }
                    }

                    device.switchOn();
                }
                device.setLastSwitchTime(device.getSwitchTime()); // kaydet
                device.switchOff(); // sonra switch yap
                device.setSwitchTime(null);
            }
        }

        Collections.sort(devices);
    }
}