import processing.core.PApplet;

/**
 * Time the experiment, offset the current millis of the program's clock, by
 * the start of the experiment and trigger the end of the experiment if the
 * duration has completed
 */
public class ExperimentTimer extends PApplet {
    private int startTime;
    /** previous time that this timer was checked */
    private int prevTime;
    private int currentTime;
    private int experimentDuration;

    /**
     * Creates a new experiment instance
     */
    public ExperimentTimer() {
        experimentDuration = 0;
        prevTime = 0;
        currentTime = 0;
    }

    /**
     * check how many seconds since the timer was started
     *
     * @return sends since this timer was started
     */
    public float checkTime() {
        if (experimentDuration == 0) {
            return 0;
        }
        prevTime = currentTime;
        currentTime = millis()-startTime;

        if (currentTime > experimentDuration*1000) {
            //endExperiment();
            //TODO: Fix
            println("FAILED TO END");
        }

        return (float)currentTime/1000.0f;
    }

    /**
     * start the timer. Records the current millis of the program.
     *
     * @param duration how long should the timer run before ending the
     *                 experiment
     */
    public void startTimer(int duration) {
        experimentDuration = duration;
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
