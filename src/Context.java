public class Context {

    int location;
    float duration;
    int radius;
    float started_time;
    boolean triggered;
    boolean ended;
    int id;
    private boolean fixed_duration;
    private int started_lap;

    protected boolean enabled;


    public Context(int location, float duration, int radius, int id,
                   boolean fixed_duration) {
        this.location = location;
        this.duration = duration;
        this.radius = radius;
        this.id = id;
        this.fixed_duration = fixed_duration;

        this.triggered = false;
        this.enabled = true;
        this.started_time = -1;
        this.started_lap = -1;
    }


    public Context(int location, float duration, int radius, int id) {
        this(location, duration, radius, id, false);
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


    private boolean check_fixed_duration(float position, float time) {
        if (this.enabled && (position > (this.location - this.radius))) {
            if (this.started_time == -1) {
                this.started_time = time;
                this.enabled = false;
                return true;
            }
        }

        if (this.started_time != -1) {
            if ((this.started_time + this.duration) > time) {
                return true;
            } else {
                this.started_time = -1;
                return false;
            }
        }

        return false;
    }

    public boolean check(float position, float time) {
        if (fixed_duration) {
            return check_fixed_duration(position, time);
        }

        return (checkPosition(position) && checkTime(time));
    }


    public boolean check(float position, float time, int lap) {
        if (fixed_duration) {
            return check_fixed_duration(position, time);
        }

        return (enabled && check(position, time));
    }

    public void move(int location) {
        this.location = location;
    }

    public void reset() {
        if (!this.fixed_duration) {
            this.started_time = -1;
        }

        this.enabled = true;
    }
}
