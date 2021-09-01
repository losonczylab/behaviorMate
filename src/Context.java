/**
 * Class for representing a single event or feature of the environment, such as a hallway, reward,
 * or cue. A context is active by default and some contexts are suspendable. The user must decide
 * on the scheme by which contexts are suspended, for example randomly or every other lap.
 */
public class Context {

    /**
     * The midpoint, in mm from the start of the track, of the context.
     */
    int location;

    /**
     * The amount of time the context is active after it has been triggered.
     */
    float duration;

    /**
     * One half the total length, in mm, along the track where this context is active. If the context is
     * placed from 100 mm to 200 mm on the track, its radius is 50 mm.
     */
    int radius;

    /**
     * ?
     */
    float started_time;

    /**
     * <code>true</code> when the context has been activated, false while it is suspended.
     */
    boolean triggered;

    /**
     * ?
     */
    boolean ended;

    /**
     * ?
     */
    int id;

    /**
     * ?
     */
    private boolean fixed_duration;

    /**
     * ?
     */
    private int started_lap;

    /**
     * ?
     */
    protected boolean enabled;

    /**
     *
     * @param location The location, in mm from the start of the track, of the context.
     * @param duration The amount of time the context is active after it has been triggered.
     * @param radius One half the total length, in mm, along the track where this context is active.
     *               If the context is placed from 100 mm to 200 mm on the track, its radius is 50 mm.
     * @param id ?
     * @param fixed_duration ?
     */
    public Context(int location, float duration, int radius, int id, boolean fixed_duration) {
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

    /**
     * Constructs a new <code>Context</code> with the <tt>fixed_duration</tt> attribute set to false.
     *
     * @param location The location, in mm from the start of the track, of the context.
     * @param duration The amount of time the context is active after it has been triggered.
     * @param radius One half the total length, in mm, along the track where this context is active.
     *               If the context is placed from 100 mm to 200 mm on the track, its radius is 50 mm.
     * @param id ?
     */
    public Context(int location, float duration, int radius, int id) {
        this(location, duration, radius, id, false);
    }

    /**
     * Setter method for the radius attribute.
     */
    public void setRadius(int radius) {
        this.radius = radius;
    }

    /**
     * @return <code>true</code> if the context is enabled, false otherwise.
     */
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * Disables the context.
     */
    public void disable() {
        this.enabled = false;
    }

    /**
     *
     * @return The midpoint of the context in mm.
     */
    public int location() {
        return this.location;
    }

    /**
     *
     * @param position The location to check whether the context is in.
     * @return <code>true</code> if the context is active in this location, <code>false</code> otherwise.
     */
    protected boolean checkPosition(float position) {
        if (this.radius == -1) {
            return true;
        }

        return (position > (location - radius)) && (position < (location + radius));
    }

    // assumes position has already been checked

    /**
     * @param time ? Todo: Is this is the current time in milliseconds?
     * @return <code>true</code> if the context should be active, <code>false</code> otherwise.
     */
    protected boolean checkTime(float time) {
        // Todo: does a duration of -1 mean it is always active?
        if (this.duration == -1) {
            return true;
        }

        if (this.started_time == -1) {
            this.started_time = time;
            return true;
        }

        // Checks if the current time is past the end time (start + duration) of the context
        // and disables the context if it is
        if ((this.started_time + this.duration) < time) {
            this.disable();
            return false;
        }

        return true;
    }

    /**
     * @param position ?
     * @param time ?
     * @return ?
     */
    private boolean check_fixed_duration(float position, float time) {
        // Todo: Should this not also check that position < (this.location + this.radius)
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

    /**
     * ?
     *
     * @param position ?
     * @param time?
     * @return ?
     */
    public boolean check(float position, float time) {
        if (fixed_duration) {
            return check_fixed_duration(position, time);
        }

        return (checkPosition(position) && checkTime(time));
    }

    /**
     *
     * @param position ?
     * @param time ?
     * @param lap ?
     * @return ?
     */
    public boolean check(float position, float time, int lap) {
        if (fixed_duration) {
            return check_fixed_duration(position, time);
        }

        return (enabled && check(position, time));
    }

    /**
     * ?
     *
     * @param location ?
     */
    public void move(int location) {
        this.location = location;
    }

    /**
     * ?
     */
    public void reset() {
        if (!this.fixed_duration) {
            this.started_time = -1;
        }

        this.enabled = true;
    }
}
