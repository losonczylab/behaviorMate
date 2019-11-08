public class Context {

    int location;
    float duration;
    int radius;
    float started_time;
    boolean triggered;
    boolean ended;
    int id;

    protected boolean enabled;


    public Context(int location, float duration, int radius, int id) {
        this.location = location;
        this.duration = duration;
        this.radius = radius;
        this.id = id;

        this.triggered = false;
        this.enabled = true;
        this.started_time = -1;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void disable() {
        this.enabled = false;
    }

    public int location() {
        return this.location;
    }

    protected boolean checkPosition(float position) {
        if (this.radius == -1) {
            return true;
        }

        return ((position > (location - radius)) &&
            (position < (location + radius)));
    }


    // assumes position has already been checked
    protected boolean checkTime(float time) {
        if (this.duration == -1) {
            return true;
        }

        if (this.started_time == -1) {
            this.started_time = time;
            return true;
        }

        if ((this.started_time + this.duration) < time) {
            this.disable();
            return false;
        }

        return true;
    }


    public boolean check(float position, float time) {

        return (checkPosition(position) && checkTime(time));
    }

    public boolean check(float position, float time, int lap) {
        return (enabled && check(position, time));
    }

    public void move(int location) {
        this.location = location;
    }

    public void reset() {
        this.started_time = -1;
        this.enabled = true;
    }
}
