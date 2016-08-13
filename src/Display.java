//import controlP5.ControlP5;
//import controlP5.Bang;
//import controlP5.Textfield;

import processing.core.PApplet;
import processing.core.PFont;

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
    private int[] reward_locations;
    private float reward_radius;
    private int[] laser_locations;
    private float laser_radius;
    private int text_offset;
    private int map_offset;
    private int tag_offset;

    public Display(float track_length) {
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
        displayScale = 300.0f/track_length;
        this.reward_locations = new int[0];
        this.laser_locations = new int[0];
        this.laser_radius = 0;
    }

    void setMouseName(String mouseName) {
        this.mouseName = mouseName;
    }

    void addLick(boolean count) {
        lickRate = max(200, lickRate+50);
        if (count) {
            lickCount++;
        }
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
        rewardRate = max(200, rewardRate+50);
        rewardCount++;
    }

    void setCurrentTag(String tag) {
        currentTag = tag;
        lapRate = 200;
    }

    void setLastLap(float position) {
        lastLap = position;
    }

    void setRewardLocations(int[] reward_locations, float radius) {
        this.reward_locations = reward_locations;
        this.reward_radius = radius * this.displayScale;
    }

    void setLaserLocations(int[] laser_locations, float radius) {
        this.laser_locations = laser_locations;
        this.laser_radius = radius * this.displayScale;
    }

    void update(PApplet app, float dy, float position, float time, boolean context, boolean lasering) {
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

        app.text(this.mouseName, 20, 40);
        app.text("Position: " + position, text_offset, 20);
        app.text("Lick Count: " + lickCount, text_offset, 40);
        app.text("Reward Count: " + rewardCount, text_offset, 60);
        app.textSize(14);
        app.text("Last Tag: " + currentTag, text_offset, 80);
        app.textSize(18);
        app.text("Time: " + time, text_offset, 100);
        app.text("Lap Count: " + lapCount, text_offset, 120);
        if (context) {
            app.text("Context: On", text_offset, 140);
        } else {
            app.text("Context: Off", text_offset, 140);
        }
        if (lasering) {
            app.text("Laser: On", text_offset, 160);
        } else {
            app.text("Laser: Off", text_offset, 160);
        }

        app.fill(color(204,204,0));
        app.rect(map_offset, 200, 300, 10);

        app.fill(color(0,204,204));
        for (int i=0; i < this.laser_locations.length; i++) {
            app.rect(map_offset+laser_locations[i]*displayScale-this.laser_radius, 200, 2*this.laser_radius, 10);
        }

        app.fill(color(0,204,0));
        for (int i=0; i < this.reward_locations.length; i++) {
            app.rect(map_offset+reward_locations[i]*displayScale-reward_radius, 200, 2*reward_radius, 10);
        }

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
