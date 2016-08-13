import processing.core.PApplet;

/**
 * Time the experiment, offset the current millis of the program's clock, by
 * the start of the experiment
 */
public class ExperimentTimer extends PApplet {
    private int startTime;
    /** previous time that this timer was checked */
    private int prevTime;
    private int currentTime;

    /**
     * Creates a new experiment instance
     */
    public ExperimentTimer() {
        prevTime = 0;
        currentTime = 0;
        startTime = -1;
    }

    /**
     * check how many seconds since the timer was started
     *
     * @return sends since this timer was started
     */
    public float checkTime() {
        if (this.startTime == -1) {
            return 0f;
        }
        prevTime = currentTime;
        currentTime = millis()-startTime;

        return (float)currentTime/1000.0f;
    }

    /**
     * start the timer. Records the current millis of the program.
     *
     */
    public void startTimer() {
        startTime = millis();
        checkTime();
    }

    /**
     * @return the last time this timer was checked (without updated the current
     * time)
     */
    public float getTime() {
        return (float)currentTime/1000.0f;
    }
}
