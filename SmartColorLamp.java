public class SmartColorLamp extends SmartLamp{
    private int hexColor;
    private boolean isColorMode;

    
    public SmartColorLamp(String deviceName) {
        super(deviceName);
        kelvin = 4000;
        brightness = 100;
        hexColor = 0xFFFFFF;
    }
    public void setColorMode(boolean isColorMode) {
        this.isColorMode = isColorMode;
    }
    public void setColorCode(int colorCode) throws SmartHomeException {
        if (colorCode < 0x0 || colorCode > 0xFFFFFF)
            throw new SmartHomeException("ERROR: Color code value must be in range of 0x0-0xFFFFFF!");
        this.hexColor = colorCode;
        this.isColorMode = true;
    }
    public int getColorCode() {
        return hexColor;
    }
    @Override
    public String toString() {
        String status;

        if (this.initialStatus) {
            status = "on";
        } else {
            status = "off";
        }

        String colorValue;

        if (this.isColorMode) {
            colorValue = String.format("0x%06X", this.hexColor);
        } else {
            colorValue = this.kelvin + "K";
        }

        return "Smart Color Lamp " + this.name + " is " + status
                + " and its color value is " + colorValue + " with " + this.brightness
                + "% brightness, and its time to switch its status is " + formatSwitchTime() + ".";
    }
}