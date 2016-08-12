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
    //private ControlP5 cp5;
    //private Textfield mouse_box;
    //private Textfield testpin_box;
    //public Bang start_button;
    //public Bang refresh_button;

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

    public Display(float track_length) {
        lickRate = 0;
        lapRate = 0;
        positionRate = 0;
        lickCount = 0;
        lapCount = 0;
        rewardCount = 0;
        lastLap = 0;
        currentTag = "";
        mouseName = "";
        displayScale = 300.0f/track_length;
        this.reward_locations = new int[0];
        this.laser_locations = new int[0];
        this.laser_radius = 0;

        displayForm();
    }

    void setTestPin(int pin) {
        //testpin_box.setText(""+pin); 
    }

    void addLick() {
        lickRate = max(200, lickRate+50);
        lickCount++;
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

    void displayForm() {
        //PFont font = createFont("arial",20f);
        //PFont smallFont = createFont("arial",12);
        //int nextField = 95;
        //int fieldOffset = 65;
 /* 
        mouse_box = cp5.addTextfield("Mouse Name")
            .setPosition(20,20)
            .setSize(200,40)
            .setFont(font)
            .setFocus(true)
            .setColor(color(255,0,0));

        testpin_box = cp5.addTextfield("Test Pin")
            .setPosition(20,150)
            .setSize(200,40)
            .setFont(font)
            .setFocus(false)
            .setColor(color(255,0,0));

        cp5.addTextfield("Duration")
            .setPosition(20,220)
            .setSize(200,40)
            .setFont(font)
            .setFocus(false)
            .setText("500")
            .setColor(color(255,0,0));

        cp5.addBang("TestValve")
            .setPosition(20,290)
            .setSize(100,40)
            .getCaptionLabel().align(ControlP5.CENTER, ControlP5.CENTER);    

        addStart();
        addRefresh();*/
    }

    void addStart() {
        //start_button = cp5.addBang("Start")
        //    .setPosition(20,500)
        //    .setSize(80,40);
        //start_button.getCaptionLabel().align(ControlP5.CENTER, ControlP5.CENTER);
    }

    void addRefresh() {
        //refresh_button = cp5.addBang("RefreshSettings")
        //    .setPosition(120,500)
        //    .setSize(160,40);
        //refresh_button.getCaptionLabel().align(ControlP5.CENTER, ControlP5.CENTER);
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
        if (time > 0) {
            app.text("press 'q' to quit trial", 20, 500);
        }
        app.text("Position: " + position, 600, 20);
        app.text("Lick Count: " + lickCount, 600, 40);
        app.text("Reward Count: " + rewardCount, 600, 60);
        app.textSize(14);
        app.text("Last Tag: " + currentTag, 600, 80);
        app.textSize(18);
        app.text("Time: " + time, 600, 100);
        app.text("Lap Count: " + lapCount, 600, 120);
        if (context) {
            app.text("Context: On", 600, 140);
        } else {
            app.text("Context: Off", 600, 140);
        }
        if (lasering) {
            app.text("Laser: On", 600, 180);
        } else {
            app.text("Laser: Off", 600, 180);
        }

        app.fill(color(204,204,0));
        app.rect(450, 200, 300, 10);

        app.fill(color(0,204,204));
        for (int i=0; i < this.laser_locations.length; i++) {
            app.rect(450+laser_locations[i]*displayScale-this.laser_radius, 200, 2*this.laser_radius, 10);
        }

        app.fill(color(0,204,0));
        for (int i=0; i < this.reward_locations.length; i++) {
            app.rect(450+reward_locations[i]*displayScale-reward_radius, 200, 2*reward_radius, 10);
        }

        app.fill(color(204,0,0));
        app.rect(450+position*displayScale-5, 200, 10, 10);

        app.textSize(10);
        app.fill(color(255, 0, 0));
        app.rect(500,500,10,-positionRate);
        app.text("velocity", 487, 510);

        app.fill(color(0,255,0));
        app.rect(540,500,10,-lickRate);
        app.text("licking", 530, 510);

        app.fill(color(0,0,255));
        app.rect(580,500,10,-rewardRate);
        app.text("reward", 570, 510);

        app.fill(color(255,255,255));
        app.rect(620,500,10,-lapRate);
        app.text("lap", 615, 510);
    }

    boolean validateForm() {
        //this.mouseName = mouse_box.getText();
        //if (this.mouseName == "") {
        //    return false;
        //}

        return true;
    }
}
