import processing.core.PApplet;

/**
 * Time the experiment, offset the current millis of the program's clock, by the start of the
 * experiment.
 */
public class ExperimentTimer extends PApplet {
    private int startTime;
    /** previous time that this timer was checked */
    private int prevTime;
    private int currentTime;

    /**
     * Constructs a new <code>ExperimentTimer</code> with the current time set to 0.
     */
    public ExperimentTimer() {
        prevTime = 0;
        currentTime = 0;
        startTime = -1;
    }

    /**
     * @return The number of seconds elapsed since the timer was started.
     *         This updates the current time.
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
     * Start the timer. Calling <code>checkTime()</code> will return the number of seconds passed
     * since this method was called.
     */
    public void startTimer() {
        startTime = millis();
        checkTime();
    }

    /**
     * @return The last value returned by <code>checkTime()</code>. Does <b>not</b> update the
     *         current time.
     */
    public float getTime() {
        return (float)currentTime/1000.0f;
    }
}
