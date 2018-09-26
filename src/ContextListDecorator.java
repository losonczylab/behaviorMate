import java.util.ArrayList;

public class ContextListDecorator implements ContextList {

    protected ContextList context_list;

    public ContextListDecorator(ContextList context_list) {
        this.context_list = context_list;
    }

    public void sendCreateMessages() {
        this.context_list.sendCreateMessages();
    }

    public void setupComms(ArrayList<UdpClient> comms) {
        this.context_list.setupComms(comms);
    }

    public UdpClient getComm() {
        return this.context_list.getComm();
    }

    public String getId() {
        return this.context_list.getId();
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
                         int lick_count, String[] msg_buffer) {

        return this.context_list.check(position, time, lap, lick_count,
                                       msg_buffer);
    }

    public boolean check(float position, float time, int lap,
                         String[] msg_buffer) {

        return this.context_list.check(position, time, lap,
                                       msg_buffer);
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

    public void stop(float time, String[] msg_buffer) {
        this.context_list.stop(time, msg_buffer);
    }
}
