public class Context {
    
    int location;
    int duration;
    int radius;
    float started_time;
    boolean triggered;
    boolean ended;
    int id;


    public Context(int location, int duration, int radius, int id) {
        this.location = location;
        this.duration = duration;
        this.radius = radius;
        this.id = id;

        this.triggered = false;
        this.started_time = -1;
    }


    private boolean checkPosition(float position) {
        return ((position > (location - radius)) &&
            (position < (location + radius)));
    }


    private boolean checkTime(float time) {
        System.out.println((this.started_time + this.duration) + " " + time);
        if (this.started_time == -1) {
            this.started_time = time;
            return true;
        }

        if ((this.started_time + this.duration) < time) {
            return false;
        }

        return true;
    }


    public boolean check(float position, float time) {
        if (checkPosition(position) && checkTime(time)) {
            return true;
        }

        return false;
    }

    public void move(int location) {
        this.location = location;
    }

    public void reset() {
        this.started_time = -1;
    }
}
