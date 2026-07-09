import java.time.LocalDateTime;

public class SmartPlug extends SmartDevice {
    private LocalDateTime plugInTime;
    private int voltage;
    private double ampere;
    private boolean isPluggedIn;
    private double totalEnergyConsumption;
    public SmartPlug(String name) {
        super(name);
        this.voltage = 220;
        this.isPluggedIn = false;
        this.totalEnergyConsumption = 0.0;
    }
    public void setTotalEnergyConsumption(double totalEnergyConsumption) {
        this.totalEnergyConsumption = totalEnergyConsumption;
    }
    public void setPlugInTime(LocalDateTime plugInTime) {
        this.plugInTime = plugInTime;
    }
    public void setVoltage(int voltage) {
        this.voltage = voltage;
    }
    public void setIsPluggedIn(boolean isPluggedIn) {
        this.isPluggedIn = isPluggedIn;
    }
    public void setAmpere(double ampere) throws SmartHomeException {
    if (ampere <= 0)
        throw new SmartHomeException("ERROR: Ampere value must be a positive number!");
    this.ampere = ampere;
    }
    public double getTotalEnergyConsumption() {
        return totalEnergyConsumption;
    }
    public LocalDateTime getPlugInTime() {
        return plugInTime;
    }
    public int getVoltage() {
        return voltage;
    }
    public double getAmpere() {
        return ampere;
    }
    public boolean getIsPluggedIn() {
        return isPluggedIn;
    }
    public void plugIn() {
        this.isPluggedIn = true;
    }
    public void plugOut() {
        this.isPluggedIn = false;
        this.plugInTime = null;
    }
    public void switchOn() {
        this.initialStatus = true;
        this.switchTime = null;
    }
    public void switchOff() {
        this.initialStatus = false;
        this.switchTime = null;
    }

    @Override
    public String toString() {
        String status;

        if (this.initialStatus) {
            status = "on";
        } else {
            status = "off";
        }

        return "Smart Plug " + this.name + " is " + status
        + " and consumed " + String.format(java.util.Locale.US, "%.2f", this.totalEnergyConsumption)
        + "W so far (excluding present usage), and its time to switch its status is "
        + formatSwitchTime() + ".";
    }
}