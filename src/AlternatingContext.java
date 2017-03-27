public class AlternatingContext extends Context {

    public AlternatingContext(int location, int duration, int radius, int id) {
        super(location, duration, radius, id);
    }

    public boolean check(float position, float time, int lap) {
        if ((lap%2 == 0) && checkPosition(position) && checkTime(time)) {
            return true;
        }

        return false;

    }
}
