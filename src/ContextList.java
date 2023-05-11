import java.util.ArrayList;
import java.util.HashMap;
import processing.data.JSONObject;

/**
 * ?
 */
public interface ContextList {

    /**
     *
     * @return The <code>UdpClient</code> object belonging to the implementor.
     */
    public abstract UdpClient getComm();

    /**
     * ?
     */
    public abstract void sendCreateMessages();

    /**
     * Setter method for the UdpClient of the implementor.
     *
     * @param comms channel to post messages for configuring, starting or stopping contexts.
     * @return <code>true</code> if the messages were successfully sent, <code>false</code> otherwise.
     */
    public abstract boolean setupComms(ArrayList<UdpClient> comms);

    /**
     *
     * @param contexts ?
     */
    public abstract void registerContexts(ArrayList<ContextList> contexts);

    /**
     * Returns the id of the implementor.
     *
     * @return the identifier
     */
    public abstract String getId();

    /**
     * Sets the length, in mm, the context will span in either direction.
     * @param radius
     */
    public abstract void setRadius(int radius);

    /**
     *
     * @return An int representing the length, in mm, the context will span in either direction.
     */
    public abstract int getRadius();

    /**
     *
     * @return The length of the track in mm.
     */
    public abstract float getTrackLength();

    /**
     * Sets the scaling used for displaying the implementor's radius in the UI.
     *
     * @param scale the amount to scale the radius so it displays properly in the UI.
     *              Units are in pixel/mm.
     */
    public abstract void setDisplayScale(float scale);

    /**
     *
     * @return the scaled width, in pixels, used to draw the implementor's radius in the UI.
     */
    public abstract float displayRadius();

    /**
     *
     * @return An array of 3 integers, representing the red, green, and blue pixels (in the order)
     *         used to display the implementor's currently active context.
     */
    public abstract int[] displayColor();

    /**
     * Sets the string displayed in the UI describing the state of the contexts of the implementor.
     *
     * @param status The status to display in the UI.
     */
    public abstract void setStatus(String status);

    /**
     *
     * @return The string representing the current status of the contexts.
     */
    public abstract String getStatus();

    /**
     *
     * @return The number of contexts wrapped by the implementor.
     */
    public abstract int size();

    /**
     * Todo: doesn't this return the location of the ith context?
     * Accessor for a specific Context in the list.
     *
     * @param i index of the context to return
     * @return  the context at the supplied index.
     */
    public abstract int getLocation(int i);

    /**
     * Todo: I assume the description of getLocation(int) should apply to this method instead.
     * @param i ?
     * @return ?
     */
    public abstract Context getContext(int i);

    /**
     * Moves the context at the given index in <code>contexts</code>, to the provided location (in mm).
     *
     * @param index The index of the context in <code>contexts</code>
     * @param location The new location of the context, in mm.
     */
    public abstract void move(int index, int location);

    /**
     * Removes all contexts from the implementor.
     */
    public abstract void clear();

    /**
     * Gives each context a new random location on the track.
     */
    public abstract void shuffle();

    /**
     * An array whose ith element contains the location of the ith context of the implementor.
     *
     * @return An array containing context locations.
     */
    public abstract int[] toList();

    /**
     * Check the state of the contexts contained in this list and send the
     * start/stop messages as necessary.
     *
     * @param position   Current position on the track (in mm).
     * @param time       Time (in seconds) since the start of the trial.
     * @param lap        Current lap number since the start of the trial.
     * @param lick_count Current number of licks, this trial.
     * @param sensor_counts ?
     * @param msg_buffer Array to buffer and send messages
     *                   to be logged in the .tdml file being written for
     *                   this trial. Messages should be placed in index 0 of the
     *                   message buffer and must be JSON formatted strings.
     * @return           <code>true</code> to indicate that the trial has started.
     */
    public abstract boolean check(float position, float time, int lap, int lick_count,
                                  HashMap<Integer, Integer> sensor_counts, JSONObject[] msg_buffer);


    /**
     * ?
     *
     * @param msg_buffer ?
     */
    public abstract void trialStart(JSONObject[] msg_buffer);

    /**
     * Resets the state of the contexts.
     */
    public abstract void reset();

    /**
     * Resets the state of the contexts.
     */
    public abstract void end();

    /**
     *
     * @return <code>true</code> if there is currently an active context or <code>false</code>
     * if all contexts are suspended.
     */
    public abstract boolean isActive();

    /**
     *
     * @return The index of the currently active context.
     */
    public abstract int activeIdx();

    /**
     * Suspend all contexts.
     */
    public abstract void suspend();

    /**
     * Stop this context.
     *
     * @param time ?
     * @param msg_buffer ?
     */
    public abstract void stop(float time, JSONObject[] msg_buffer);

    /**
     * ?
     */
    public abstract void shutdown();

    /**
     * Todo: does this send a message to the arduino?
     *
     * @param message ?
     */
    public abstract void sendMessage(String message);
}
