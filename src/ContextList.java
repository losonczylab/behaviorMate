import java.util.ArrayList;
import java.util.HashMap;
import processing.data.JSONObject;

public interface ContextList {

    public abstract UdpClient getComm();

    public abstract void sendCreateMessages();

    public abstract boolean setupComms(ArrayList<UdpClient> comms);

    public abstract void registerContexts(ArrayList<ContextList> contexts);

    public abstract String getId();

    public abstract void setRadius(int radius);

    public abstract int getRadius();

    public abstract float getTrackLength();

    public abstract void setDisplayScale(float scale);

    public abstract float displayRadius();

    public abstract int[] displayColor();

    public abstract void setStatus(String status);

    public abstract String getStatus();

    public abstract int size();

    public abstract int getLocation(int i);

    public abstract Context getContext(int i);

    public abstract void move(int index, int location);

    public abstract void clear();

    public abstract void shuffle();

    public abstract int[] toList();

    public abstract boolean check(float position, float time, int lap,
                                  int lick_count,
                                  HashMap<Integer, Integer> sensor_counts,
                                  JSONObject[] msg_buffer);

    public abstract void trialStart(JSONObject[] msg_buffer);

    public abstract void reset();

    public abstract void end();

    public abstract boolean isActive();

    public abstract int activeIdx();

    public abstract void suspend();

    public abstract void stop(float time, JSONObject[] msg_buffer);

    public abstract void shutdown();

    public abstract void sendMessage(String message);
}
