import java.time.LocalDateTime;
public class SmartCamera extends SmartDevice {
    private double totalStorageUsed;
    private double megabytesPerMinute;
    private LocalDateTime switchOnTime;
    public SmartCamera(String name, double mbPerMinute) throws SmartHomeException {
        super(name);
        if (mbPerMinute <= 0)
            throw new SmartHomeException("ERROR: Megabyte value must be a positive number!");
        this.megabytesPerMinute = mbPerMinute;
    }
    public void setMbPerMinute(double mbPerMinute) throws SmartHomeException {
        if (mbPerMinute <= 0)
            throw new SmartHomeException("ERROR: Megabyte value must be a positive number!");
        this.megabytesPerMinute = mbPerMinute;
    }
    public double getMbPerMinute() {
        return megabytesPerMinute;
    }
    public void setSwitchOnTime(LocalDateTime switchOnTime) {
        this.switchOnTime = switchOnTime;
    }
    public LocalDateTime getSwitchOnTime() {
        return switchOnTime;
    }
    public void addStorage(double storage) {
        this.totalStorageUsed += storage;
    }
    public void switchOn() {
        this.initialStatus = true;
        this.switchTime = null;
    }
    public void switchOff() {
        this.initialStatus = false;
        this.switchTime = null;
        this.switchOnTime = null;
    }
    @Override
    public String toString() {
        String status;

        if (this.initialStatus) {
            status = "on";
        } else {
            status = "off";
        }

        return "Smart Camera " + this.name + " is " + status
                + " and used " + String.format(java.util.Locale.US, "%.2f", this.totalStorageUsed)
                + " MB of storage so far (excluding present usage), and its time to switch its status is "
                + formatSwitchTime() + ".";
    }
}