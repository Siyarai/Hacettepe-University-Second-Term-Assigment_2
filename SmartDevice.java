import java.time.LocalDateTime;
public abstract class SmartDevice implements Comparable<SmartDevice> {
    protected String name;
    protected boolean initialStatus;
    protected LocalDateTime switchTime;
    private LocalDateTime lastSwitchTime;
    private static int counter = 0;
    private final int insertionOrder;

    public SmartDevice(String name) {
        this.name = name;
        this.initialStatus = false;
        this.switchTime = null;
        this.insertionOrder = counter++;
    }
    public String getName() {
        return name;
    }
    public LocalDateTime getLastSwitchTime(){
        return lastSwitchTime;
    }
    public boolean getInitialStatus() {
        return initialStatus;
    }
    public LocalDateTime getSwitchTime() {
        return switchTime;
    }
    public void setName(String name) {
        this.name = name;
    }

    public void setInitialStatus(boolean status) {
        this.initialStatus = status;
    }
    public void setSwitchTime(LocalDateTime time) {
        this.switchTime = time;
    }
    public void setLastSwitchTime(LocalDateTime time) {
        this.lastSwitchTime = time;
    }
    public abstract void switchOn();
    public abstract void switchOff();

    protected String formatSwitchTime() {
        if (switchTime == null) {
            return "null";
        }
        return switchTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss"));
    }
    @Override
    public int compareTo(SmartDevice other) {
        if (this.switchTime != null && other.switchTime != null)
            return this.switchTime.compareTo(other.switchTime);
        if (this.switchTime != null) return -1;
        if (other.switchTime != null) return 1;
        
        // İkisi de null — lastSwitchTime'a göre AZALAN sıra
        if (this.lastSwitchTime != null && other.lastSwitchTime != null) {
            int cmp = other.lastSwitchTime.compareTo(this.lastSwitchTime); // DESC
            if (cmp != 0) return cmp;
            return Integer.compare(this.insertionOrder, other.insertionOrder);
        }
        if (this.lastSwitchTime != null) return -1;
        if (other.lastSwitchTime != null) return 1;
        return Integer.compare(this.insertionOrder, other.insertionOrder);
    }
}