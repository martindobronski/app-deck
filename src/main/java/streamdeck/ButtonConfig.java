package streamdeck;

public class ButtonConfig {
    private String label;
    private String type;
    private String target;

    public ButtonConfig() {}

    public ButtonConfig(String label, String type, String target) {
        this.label = label;
        this.type = type;
        this.target = target;
    }

    public String getLabel() { return label; }
    public String getType() { return type; }
    public String getTarget() { return target; }

    public void setLabel(String label) { this.label = label; }
    public void setType(String type) { this.type = type; }
    public void setTarget(String target) { this.target = target; }
}
