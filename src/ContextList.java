import processing.core.PApplet;
import processing.data.JSONObject;
import java.util.ArrayList;

public class ContextList extends PApplet {
    private ArrayList<Context> contexts;
    private int radius;
    private int duration;
    private float display_radius;
    private int display_color;
    private UdpClient comm;
    private String id;
    private String startString;
    private String stopString;
    private boolean active;
    private String status;
    private Display display;

    public ContextList(Display display, int display_color) {
        this.contexts = new ArrayList<Context>();
        this.display = display;
        this.display_color = display_color;
        this.comm = null;
        this.startString = "";
        this.stopString = "";
        this.id = "context";
        this.active = false;
        this.status = "";
    }

    public ContextList(int duration, int radius, int display_color) {
        this.contexts = new ArrayList<Context>();
        this.radius = radius;
        this.display_radius = (float) radius;
        this.duration = duration;
        this.display_color = display_color;
        this.display = display;
        this.comm = null;
        this.startString = "";
        this.stopString = "";
        this.id = "context";
        this.active = false;
        this.status = "";
    }

    public void setComm(UdpClient comm) {
        this.comm = comm;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void setId(String id) {
        this.id = id;

        JSONObject context_message = new JSONObject();
        context_message.setString("action", "start");
        context_message.setString("id", this.id);
        JSONObject context_message_json = new JSONObject();
        context_message_json.setJSONObject("contexts", context_message);
        this.startString = context_message_json.toString();

        context_message.setString("action", "stop");
        context_message_json.setJSONObject("contexts", context_message);
        this.stopString = context_message_json.toString();
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public void add(int location) {
        this.contexts.add(new Context(location, this.duration,
            this.radius, this.contexts.size()));
    }

    public void setDisplayScale(float scale) {
        this.display_radius = ((float)this.radius) * scale;
    }

    public float displayRadius() {
        return this.display_radius;
    }

    public int displayColor() {
        return this.display_color;
    }

    public void clear() {
        if (this.size() > 0) {
            this.contexts = new ArrayList<Context>();
        }
    }

    public void reset() {
        for (int i=0; i < this.contexts.size(); i++) {
            this.contexts.get(i).reset();
        }
    }

    public boolean check(float position, float time) {
        boolean inZone = false;
        for (int i=0; i < this.contexts.size(); i++) {
            if (this.contexts.get(i).check(position, time)) {
                inZone = true;
            }
        }

        if ((!inZone) && (this.active)) {
            this.active = false;
            this.status = "sent stop";
            this.comm.sendMessage(this.stopString);
        } else if((inZone) && (!this.active)) {
            this.active = true;
            this.status = "sent start";
            this.comm.sendMessage(this.startString);
            /*if (!laser_on_reward) {
                if ((reward_start + reward_duration) > time) {
                    reward_start = time;
                }
            }*/
        }

        return this.active;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void stop() {
        this.active = false;
        this.status = "sent stop";
        this.comm.sendMessage(this.stopString);
    }

    public String getStatus() {
        return this.status;
    }

    public String getId() {
        return this.id;
    }

    public int getLocation(int i) {
        return this.contexts.get(i).location();
    }

    public int size() {
        return this.contexts.size();
    }

    public void shuffle(float track_length) {
        if (this.contexts.size() == 0) {
            return;
        }

        if (this.contexts.size() == 1) {
            contexts.get(0).move((int) random(this.radius, track_length-this.radius));
        }

        int interval = (int)(track_length-2*this.radius)/this.contexts.size();
        contexts.get(0).move(this.radius + interval/2);
        for (int i = 1; i < this.contexts.size(); i++) {
            this.contexts.get(i).move(this.contexts.get(i-1).location() + interval);
        }

        this.contexts.get(0).move(
            (int) random(this.radius,this.contexts.get(1).location()-2*this.radius));

        for (int i = 1; i < this.contexts.size()-1; i++) {
            int prev_location = this.contexts.get(i-1).location();
            int next_location = this.contexts.get(i+1).location();
            this.contexts.get(i).move(
                (int) random(prev_location+2*this.radius, next_location-2*this.radius));
        }

        int prev_location = this.contexts.get(this.size()-2).location();
        this.contexts.get(this.size()-1).move(
            (int) random(prev_location+2*this.radius, track_length-this.radius));
    }

    public int[] toList() {
        int[] list = new int[contexts.size()];
        for (int i=0; i < this.contexts.size(); i++) {
            list[i] = this.contexts.get(i).location;
        }

        return list;
    }
}
