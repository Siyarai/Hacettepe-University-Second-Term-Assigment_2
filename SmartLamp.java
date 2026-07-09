public class SmartLamp extends SmartDevice {
    protected int kelvin;
    protected int brightness;

    public SmartLamp(String name) {
        super(name);
        this.kelvin = 4000;
        this.brightness = 100;
    }
    public void setKelvin(int kelvin) throws SmartHomeException {
    if (kelvin < 2000 || kelvin > 6500)
        throw new SmartHomeException("ERROR: Kelvin value must be in range of 2000K-6500K!");
    this.kelvin = kelvin;
}

public void setBrightness(int brightness) throws SmartHomeException {
    if (brightness < 0 || brightness > 100)
        throw new SmartHomeException("ERROR: Brightness must be in range of 0%-100%!");
    this.brightness = brightness;
}
    public int getKelvin() {
        return kelvin;
    }
    public int getBrightness() {
        return brightness;
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

        return "Smart Lamp " + this.name + " is " + status
                + " and its kelvin value is " + this.kelvin + "K with " + this.brightness
                + "% brightness, and its time to switch its status is " + formatSwitchTime() + ".";
    }
}
