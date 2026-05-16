package streamdeck;

import java.util.ArrayList;
import java.util.List;

public class ButtonConfig {
    private String label;
    private String type;
    private String target;
    private List<List<ButtonConfig>> pages;

    public ButtonConfig() {}

    public ButtonConfig(String label, String type, String target) {
        this.label = label;
        this.type = type;
        this.target = target;
    }

    public String getLabel() { return label; }
    public String getType() { return type; }
    public String getTarget() { return target; }
    public List<List<ButtonConfig>> getPages() { return pages; }

    public void setLabel(String label) { this.label = label; }
    public void setType(String type) { this.type = type; }
    public void setTarget(String target) { this.target = target; }
    public void setPages(List<List<ButtonConfig>> pages) { this.pages = pages; }
}
