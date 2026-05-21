package streamdeck;

import java.util.ArrayList;
import java.util.List;

public class ButtonConfig {
    private String label;
    private String type;
    private String target;
    private List<List<ButtonConfig>> pages;
    private boolean check;
    private String latestVideoId;
    private List<String> knownVideoIds;
    private int newCount;

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
    public boolean isCheck() { return check; }
    public String getLatestVideoId() { return latestVideoId; }
    public List<String> getKnownVideoIds() { return knownVideoIds; }
    public boolean isHasNew() { return newCount > 0; }

    public void setHasNew(boolean v) {
        if (v) {
            newCount++;
        } else {
            newCount = 0;
        }
    }

    public void setLabel(String label) { this.label = label; }
    public void setType(String type) { this.type = type; }
    public void setTarget(String target) { this.target = target; }
    public void setPages(List<List<ButtonConfig>> pages) { this.pages = pages; }
    public void setCheck(boolean check) { this.check = check; }
    public void setLatestVideoId(String latestVideoId) { this.latestVideoId = latestVideoId; }
    public void setKnownVideoIds(List<String> knownVideoIds) { this.knownVideoIds = knownVideoIds; }
    public int getNewCount() { return newCount; }
    public void setNewCount(int newCount) { this.newCount = newCount; }
}
