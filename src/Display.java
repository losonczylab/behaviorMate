import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PGraphics;
import java.util.ArrayList;

/**
 * Display update to the screen showing the current behavior of the animal on
 * the track
 */
public class Display extends PApplet {
    private float lickRate;
    private boolean lickOn;
    private float lapRate;
    private float lapErrorRate;
    private float positionRate;
    private float rewardRate;
    private int lickCount;
    private int[] valve_ids;
    private int[] valve_states;
    private int[] sensor_ids;
    private int[] sensor_states;
    private int rewardCount;
    private int freeRewardCount;
    private float lastLap;
    private int lapCount;
    private float displayScale;
    private String currentTag;
    private float position_scale;
    private String mouseName;
    private int text_offset;
    private int map_offset;
    private int tag_offset;
    private String schedule;
    private ArrayList<ContextList> contextsContainer;
    private PGraphics pg;
    private String totalTime;
    private String bottom_message;

    public Display() {
        lickRate = 0;
        lapRate = 0;
        positionRate = 0;
        lickCount = 0;
        lickOn = false;
        lapCount = 0;
        rewardCount = 0;
        freeRewardCount = 0;
        lastLap = 0;
        text_offset = 385;
        map_offset = 150;
        tag_offset = 240;
        currentTag = "";
        position_scale = 0;
        mouseName = "";
        bottom_message = "";
        displayScale = 300.0f/1.0f;
        //this.laser_radius = 0;
        this.valve_states = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        this.valve_ids = new int[] { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 };
        this.sensor_states = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        this.sensor_ids = new int[] { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 };
        contextsContainer = new ArrayList<ContextList>();

        this.schedule = "";
        this.totalTime = "";
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
        lickOn = true;
        if (count) {
            lickCount++;
        }
    }

    void lickStop() {
        lickOn = false;
    }

    void setPositionRate(float dy) {
        if (dy == 0) {
            positionRate = positionRate/abs(positionRate) * max(0.0f, abs(positionRate)-0.5f);
        } else {
            positionRate = min(200,dy*5);
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

    void incrementFreeRewardCount() {
        freeRewardCount += 1;
    }

    void resetFreeRewardCount() {
        freeRewardCount = 0;
    }

    void setTotalTime(int time) {
        this.totalTime = "/" + time;
    }

    void setBottomMessage(String message) {
        this.bottom_message = message;
    }

    void setValveState(int pin, int state) {
        for (int i = 0; i < this.valve_ids.length; i++) {
            if (this.valve_ids[i] == pin) {
                this.valve_states[i] = state;
                break;
            } else if (this.valve_ids[i] == -1) {
                this.valve_ids[i] = pin;
                this.valve_states[i] = state;
                break;
            }
        }
    }

    void setSensorState(int pin, int state) {
        for (int i = 0; i < this.sensor_ids.length; i++) {
            if (this.sensor_ids[i] == pin) {
                this.sensor_states[i] = state;
                break;
            } else if (this.sensor_ids[i] == -1) {
                this.sensor_ids[i] = pin;
                this.sensor_states[i] = state;
                break;
            }
        }
    }

    void clearValveStates() {
        this.valve_states = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        this.valve_ids = new int[] { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 };
    }

    void clearSensorStates() {
        this.sensor_states = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        this.sensor_ids = new int[] { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 };
    }

    void addReward() {
        rewardRate = min(200, rewardRate+50);
        rewardCount++;
    }

    void setPositionScale(float scale) {
        this.position_scale = scale;
    }

    void setCurrentTag(String tag, float position_error) {
        currentTag = tag;
        lapRate = 200;
        lapErrorRate = Math.min(200*Math.abs(position_error)/15, 200);
    }

    void setCurrentTag(String tag) {
        setCurrentTag(tag, 0);
    }

    void setLastLap(float position) {
        lastLap = position;
    }

    public void setContextLocations(ContextList contexts) {
        contexts.setDisplayScale(this.displayScale);
        contextsContainer.add(contexts);
    }

    void prepGraphics(PApplet app) {
        this.pg = app.createGraphics(app.width, app.height);
        this.pg.beginDraw();
        this.pg.background(0);

        this.pg.textSize(18);
        this.pg.text("Position: ", text_offset, 20);
        this.pg.text("Lick Count: ", text_offset, 40);
        this.pg.text("Reward Count: ", text_offset, 60);
        this.pg.text("Time: ", text_offset, 100);
        this.pg.text("Lap Count: ", text_offset, 120);

        this.pg.textSize(14);
        this.pg.text("Position Scale: ", text_offset, 80);

        this.pg.fill(color(204,204,0));
        this.pg.rect(map_offset, 200, 300, 10);

        this.pg.textSize(10);
        this.pg.fill(color(255, 0, 0));
        this.pg.text("velocity", tag_offset-13, 510);
        this.pg.fill(color(0,255,0));
        this.pg.text("licking", tag_offset+30, 510);
        this.pg.fill(color(0,0,255));
        this.pg.text("reward", tag_offset+70, 510);
        this.pg.fill(color(255,255,255));
        this.pg.text("lap", tag_offset+115, 510);

        this.pg.endDraw();
    }

    private void drawValveStates(PApplet app) {
        for (int i = 0; i < 5; i++) {
            if (this.valve_states[i] == 1) {
                app.fill(color(0, 255, 0));
            } else if(this.valve_states[i] == -1) {
                app.fill(color(255, 0, 0));
            } else {
                break;
                //app.fill(color(255, 255, 255));
            }
            app.rect(450 + 30*i, app.height-60-25, 25, 25);
            app.fill(color(255, 255, 255));
            app.text(this.valve_ids[i], 450 + 30*i + 4, app.height-60-25-5);
        }

        for (int i = 0; i < 5; i++) {
            if (this.valve_states[i+5] == 1) {
                app.fill(color(0, 255, 0));
            } else if(this.valve_states[i+5] == -1) {
                app.fill(color(255, 0, 0));
            } else {
                break;
                //app.fill(color(255, 255, 255));
            }
            app.rect(450 + 30*i, app.height-60-25-50, 25, 25);
            app.fill(color(255, 255, 255));
            app.text(this.valve_ids[i+5], 450+30*i + 4, app.height-60-50-25-5);
        }
        app.fill(color(255, 255, 255));

    }

    private void drawSensorStates(PApplet app) {
        for (int i = 0; i < 5; i++) {
            if (this.sensor_states[i] == 1) {
                app.fill(color(0, 255, 0));
            } else if(this.sensor_states[i] == -1) {
                app.fill(color(255, 0, 0));
            } else {
                break;
                //app.fill(color(255, 255, 255));
            }
            app.rect(450 + 30*i, app.height-60-25-100, 25, 25);
            app.fill(color(255, 255, 255));
            app.text(this.sensor_ids[i], 450 + 30*i + 4,
                     app.height-60-25-5-100);
        }

        for (int i = 0; i < 5; i++) {
            if (this.sensor_states[i+5] == 1) {
                app.fill(color(0, 255, 0));
            } else if(this.sensor_states[i+5] == -1) {
                app.fill(color(255, 0, 0));
            } else {
                break;
                //app.fill(color(255, 255, 255));
            }
            app.rect(450 + 30*i, app.height-60-25-50-100, 25, 25);
            app.fill(color(255, 255, 255));
            app.text(this.sensor_ids[i+5], 450+30*i + 4,
                     app.height-60-50-25-5-100);
        }
        app.fill(color(255, 255, 255));

    }

    void update(PApplet app, float dy, float position, float time) {
        //float t = app.millis();
        //TODO: running more slow with performance hack
        if (false) {
            if (this.pg != null) {
                app.image(this.pg, 0, 0);
            }
        }
        else {
        app.background(0);

        app.textSize(18);
        app.text("Position: ", text_offset, 20);
        app.text("Lick Count: ", text_offset, 40);
        app.text("Reward Count: ", text_offset, 60);
        app.text("Time: ", text_offset, 100);
        app.text("Lap Count: ", text_offset, 120);

        app.textSize(14);
        app.text("Position Scale: ", text_offset, 80);

        app.fill(color(204,204,0));
        app.rect(map_offset, 200, 300, 10);

        app.textSize(10);
        app.fill(color(255, 0, 0));
        app.text("velocity", tag_offset-13, 510);
        app.fill(color(0,255,0));
        app.text("licking", tag_offset+30, 510);
        app.fill(color(0,0,255));
        app.text("reward", tag_offset+70, 510);
        app.fill(color(255,255,255));
        app.text("lap", tag_offset+115, 510);


        }
        app.textSize(18);


        if ((lickRate > 0) && (!lickOn)) {
            lickRate -= 5;
        }

        if (lapRate > 0) {
            lapRate -= 5;
        }

        if (lapErrorRate > 5) {
            lapErrorRate -= 5;
        } else {
            lapErrorRate = 0;
        }

        if (rewardRate > 0) {
            rewardRate -= 5;
        }

        app.fill(color(255, 255, 255));
        app.textSize(18);
        app.text(this.mouseName, 20, 40);
        app.text((int)position, 75+text_offset, 20);
        app.text(lickCount, 105+text_offset, 40);

        if (freeRewardCount > 0) {
            app.text(rewardCount + " [" + freeRewardCount + "]",
                     135+text_offset, 60);
        } else {
            app.text(rewardCount, 135+text_offset, 60);
        }
        app.text((int)time + this.totalTime, 50+text_offset, 100);
        app.text(lapCount, 100+text_offset, 120);

        if (time > 0) {
            app.fill(color(204,0,0));
            app.text("Recording", 20, 20);
            app.fill(255);
        }

        app.textSize(14);
        app.text(String.format("%.4f", position_scale), 30+72+text_offset, 80);

        app.fill(color(204,204,0));
        app.rect(map_offset, 200, 300, 10);
        int yoffset = 140;
        for (int i=0; i < contextsContainer.size(); i++) {
            if (i == 3) {
                yoffset += 40;
            }
            ContextList list = contextsContainer.get(i);
            if (list.displayColor() == null) {
                app.fill(255);
                app.textSize(14);
                app.text(list.getId() + ": "  + list.getStatus(), text_offset,
                         yoffset+i*20);
                continue;
            }

            int[] c_ = list.displayColor();
            int c = color(c_[0], c_[1], c_[2]);
            app.fill(c);
            app.textSize(14);
            app.text(list.getId() + ": "  + list.getStatus(), text_offset,
                     yoffset+i*20);

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

        app.fill(color(255, 0, 0));
        app.rect(tag_offset,500,10,-positionRate);

        app.fill(color(0,255,0));
        app.rect(tag_offset+40,500,10,-lickRate);

        app.fill(color(0,0,255));
        app.rect(tag_offset+80,500,10,-rewardRate);

        app.fill(color(255,255,255));
        app.rect(tag_offset+120,500,10,-lapRate);

        app.fill(color(255,0,0));
        app.rect(tag_offset+120,500,10,-lapErrorRate);

        app.fill(color(255, 255, 255));
        app.text(this.bottom_message, 10, app.height-60);

        drawValveStates(app);
        drawSensorStates(app);

        //println("updates: " + (app.millis() - t));
    }
}
