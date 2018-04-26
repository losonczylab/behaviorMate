
public interface ContextList {

    public abstract void sendCreateMessages();

    public abstract void setComm(UdpClient comm);

    public abstract String getId();

    public abstract void setDisplayScale(float scale);

    public abstract float displayRadius();

    public abstract int[] displayColor();

    public abstract void setStatus(String status);

    public abstract String getStatus();

    public abstract int size();

    public abstract int getLocation(int i);

    public abstract void clear();

    public abstract void shuffle();

    public abstract int[] toList();

    public abstract boolean check(float position, float time, int lap,
                                  int lick_count, String[] msg_buffer);

    public abstract boolean check(float position, float time, int lap,
                         String[] msg_buffer);

    public void reset();

    public boolean isActive();

    public int activeIdx();

    public void suspend();

    public abstract void stop(float time, String[] msg_buffer);
}
