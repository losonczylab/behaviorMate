import processing.core.PApplet;
import processing.core.PFont;
import java.util.ArrayList;

/**
 * Display update to the screen showing the cureent behavior of the animial on
 * the track
 */
public class Display extends PApplet {
    private float lickRate;
    private float lapRate;
    private float positionRate;
    private float rewardRate;
    private int lickCount;
    private int rewardCount;
    private float lastLap;
    private int lapCount;
    private float displayScale;
    private String currentTag;
    private String mouseName;
    private float reward_radius;
    private float laser_radius;
    private int text_offset;
    private int map_offset;
    private int tag_offset;
    private String schedule;
    private ArrayList<ContextList> contextsContainer;
    private String reward_status;
    private String laser_status;

    public Display() {
        lickRate = 0;
        lapRate = 0;
        positionRate = 0;
        lickCount = 0;
        lapCount = 0;
        rewardCount = 0;
        lastLap = 0;
        text_offset = 420;
        map_offset = 150;
        tag_offset = 240;
        currentTag = "";
        mouseName = "";
        displayScale = 300.0f/1.0f;
        this.reward_status = "stopped";
        this.laser_status = "stopped";
        this.laser_radius = 0;
        contextsContainer = new ArrayList<ContextList>();

        this.schedule = "";
    }

    void resetContexts() {
        contextsContainer = new ArrayList<ContextList>();
    }

    void setTrackLength(float trackLength) {
        displayScale = 300.0f/trackLength;
    }

    void setMouseName(String mouseName) {
        this.mouseName = mouseName;
    }

    void addLick(boolean count) {
        lickRate = min(200, lickRate+50);
        if (count) {
            lickCount++;
        }
    }

    void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    void setLapCount(int count) {
        lapCount = count;
    }
    
    void setLickCount(int count) {
        lickCount = count; 
    }

    void setRewardCount(int count) {
        rewardCount = count; 
    }

    void addReward() {
        rewardRate = min(200, rewardRate+50);
        rewardCount++;
    }

    void setCurrentTag(String tag) {
        currentTag = tag;
        lapRate = 200;
    }

    void setLastLap(float position) {
        lastLap = position;
    }

    public void setContextLocations(ContextList contexts) {
        contexts.setDisplayScale(this.displayScale);
        contextsContainer.add(contexts);
    }

    void setContextStatus(String id, String status) {
        if (id.equals("hidden_reward")) {
            this.reward_status = status;
        } else if (id.equals("laser_context")) {
            this.laser_status = status;
        }
    }

    void update(PApplet app, float dy, float position, float time, 
            boolean lasering) {
        app.textSize(18);

        if (lickRate > 0) {
            lickRate -= 5; 
        }

        if (lapRate > 0) {
            lapRate -= 5; 
        }

        if (rewardRate > 0) {
            rewardRate -= 5; 
        }

        if (dy != 0) {
            positionRate = min(200,dy*5);
        } else {
            positionRate = positionRate/abs(positionRate) * max(0.0f, abs(positionRate)-0.5f);
        }

        app.textSize(18);
        app.text(this.mouseName, 20, 40);
        app.text("Position: " + position, text_offset, 20);
        app.text("Lick Count: " + lickCount, text_offset, 40);
        app.text("Reward Count: " + rewardCount, text_offset, 60);
        app.textSize(14);
        app.text("Last Tag: " + currentTag, text_offset, 80);
        app.textSize(18);
        app.text("Time: " + time, text_offset, 100);
        app.text("Lap Count: " + lapCount, text_offset, 120);
        
        //app.text("Reward Zone: " + this.reward_status, text_offset, 140);
        //app.text("Laser: " + this.laser_status, text_offset, 160);

        app.fill(color(204,204,0));
        app.rect(map_offset, 200, 300, 10);
        int yoffset = 140;
        for (int i=0; i < contextsContainer.size(); i++) {
            ContextList list = contextsContainer.get(i);

            app.fill(list.displayColor());
            app.textSize(14);
            app.text(list.getId() + ": "  + list.getStatus(), text_offset, yoffset+i*20);

            float radius = list.displayRadius();
            for (int j=0; j < list.size(); j++) {
                app.rect(map_offset+list.getLocation(j)*displayScale-radius,
                    200, 2*radius, 10);
            }
        }

        app.textSize(14);
        app.fill(color(204,204,0));
        app.text(this.schedule, 60, 80);

        app.fill(color(204,0,0));
        app.rect(map_offset+position*displayScale-5, 200, 10, 10);

        app.textSize(10);
        app.fill(color(255, 0, 0));
        app.rect(tag_offset,500,10,-positionRate);
        app.text("velocity", tag_offset-13, 510);

        app.fill(color(0,255,0));
        app.rect(tag_offset+40,500,10,-lickRate);
        app.text("licking", tag_offset+30, 510);

        app.fill(color(0,0,255));
        app.rect(tag_offset+80,500,10,-rewardRate);
        app.text("reward", tag_offset+70, 510);

        app.fill(color(255,255,255));
        app.rect(tag_offset+120,500,10,-lapRate);
        app.text("lap", tag_offset+115, 510);
    }
}
