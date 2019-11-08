import java.util.ArrayList;
import processing.data.JSONObject;

public class ContextListDecorator implements ContextList {

    protected ContextList context_list;

    public ContextListDecorator(ContextList context_list) {
        this.context_list = context_list;
    }

    public UdpClient getComm() {
        return this.context_list.getComm();
    }

    public void sendCreateMessages() {
        this.context_list.sendCreateMessages();
    }

    public boolean setupComms(ArrayList<UdpClient> comms) {
        return this.context_list.setupComms(comms);
    }

    public void registerContexts(ArrayList<ContextList> contexts) {
        this.context_list.registerContexts(contexts);
    }

    public String getId() {
        return this.context_list.getId();
    }

    public void setRadius(int radius) {
        this.context_list.setRadius(radius);
    }

    public int getRadius() {
        return this.context_list.getRadius();
    }

    public float getTrackLength() {
        return this.context_list.getTrackLength();
    }

    public void setDisplayScale(float scale) {
        this.context_list.setDisplayScale(scale);
    }

    public float displayRadius() {
        return this.context_list.displayRadius();
    }

    public int[] displayColor() {
        return this.context_list.displayColor();
    }

    public void setStatus(String status) {
        this.context_list.setStatus(status);
    }

    public String getStatus() {
        return this.context_list.getStatus();
    }

    public int size() {
        return this.context_list.size();
    }

    public int getLocation(int i) {
        return this.context_list.getLocation(i);
    }

    public Context getContext(int i) {
        return this.context_list.getContext(i);
    }

    public void move(int index, int location) {
        this.context_list.move(index, location);
    }


    public void clear() {
        this.context_list.clear();
    }

    public void shuffle() {
        this.context_list.shuffle();
    }

    public int[] toList() {
        return this.context_list.toList();
    }

    public boolean check(float position, float time, int lap,
                         int lick_count, JSONObject[] msg_buffer) {

        return this.context_list.check(position, time, lap, lick_count,
                                       msg_buffer);
    }

    public boolean check(float position, float time, int lap,
                         JSONObject[] msg_buffer) {

        return this.context_list.check(position, time, lap,
                                       msg_buffer);
    }

    public void trialStart(JSONObject[] msg_buffer) {
        this.context_list.trialStart(msg_buffer);
    }

    public void reset() {
        this.context_list.reset();
    }

    public void end() {
        this.context_list.end();
    }

    public boolean isActive() {
        return this.context_list.isActive();
    }

    public int activeIdx() {
        return this.context_list.activeIdx();
    }

    public void suspend() {
        this.context_list.suspend();
    }

    public void stop(float time, JSONObject[] msg_buffer) {
        this.context_list.stop(time, msg_buffer);
    }

    public void shutdown() {
        this.context_list.shutdown();
    }
}
