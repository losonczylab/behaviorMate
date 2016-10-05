import processing.core.PApplet;
import java.util.ArrayList;

public class ContextList extends PApplet {
    private ArrayList<Context> contexts;
    private int radius;
    private int duration;
    private float display_radius;
    private int display_color;
    private UdpClient comm;

    public ContextList(int display_color) {
        this.contexts = new ArrayList<Context>();
        this.display_color = display_color;
        this.comm = null;
    }

    public ContextList(int duration, int radius, int display_color) {
        this.contexts = new ArrayList<Context>();
        this.radius = radius;
        this.display_radius = (float) radius;
        this.duration = duration;
        this.display_color = display_color;
        this.comm = null;
    }

    public void setDuration(int duration) {
        this.duration = duration;
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
        for (int i=0; i < this.contexts.size(); i++) {
            if (this.contexts.get(i).check(position, time)) {
                return true;
            }
        }

        return false;
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
