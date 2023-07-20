import java.util.ArrayList;
import java.util.HashMap;

import processing.data.JSONObject;
import processing.data.JSONArray;

/**
 * ScheduledContextList class. Disables contexts based on lap count.
 */
public class MovingContextDecorator extends ContextListDecorator {

    protected ArrayList<Integer> lap_list;

    protected ArrayList<Integer> locations_list;

    protected int repeat;

    protected int index;

    protected int last_index;

    protected int current_lap;

    protected boolean trial_started;

    public MovingContextDecorator(ContextList context_list,
                                  JSONObject context_info) {
        super(context_list);

        this.lap_list = new ArrayList<Integer>();
        this.lap_list.add(0);

        JSONArray lap_array = null;
        if (!context_info.isNull("lap_list")) {
            lap_array = context_info.getJSONArray("lap_list");
            for (int j = 0; j < lap_array.size(); j++) {
                this.lap_list.add(lap_array.getInt(j));
            }
        } else {
            //TODO: raise exception
        }

        this.locations_list = new ArrayList<Integer>();
        this.locations_list.add(this.getLocation(0));
        JSONArray location_array = null;
        if (!context_info.isNull("lap_list")) {
            location_array = context_info.getJSONArray("locations");
            for (int j = 0; j < lap_array.size(); j++) {
                this.locations_list.add(location_array.getInt(j));
            }
        } else {
            //TODO: raise exception
        }

        //TODO:
        //this.repeat = context_info.getInt("repeat", 0);

        this.current_lap = 0;
        this.index = 0;
        this.trial_started = false;
        this.last_index = this.lap_list.size() - 1;
    }

    public int size() {
        if (!this.trial_started) {
            return this.locations_list.size();
        }

        return this.context_list.size();
    }

    public int getLocation(int i) {
        if ((!this.trial_started) && (i < locations_list.size())) {
            return this.locations_list.get(i);
        }

        return this.context_list.getLocation(i);
    }

    public void reset() {
        this.current_lap++;
        System.out.println(this.index);
        if (this.index < this.last_index) {
            if (this.current_lap >= lap_list.get(this.index + 1)) {
                index++;
                this.context_list.move(0, locations_list.get(index));
            }
        }
        this.context_list.reset();
    }

    public void end() {
        this.current_lap = 0;
        this.index = 0;
        this.context_list.move(0, locations_list.get(0));
        trial_started = false;
        this.context_list.end();
    }

    public boolean check(float position, float time, int lap,
                         int lick_count,
                         HashMap<Integer, Integer> sensor_counts,
                         JSONObject[] msg_buffer) {
        
        this.trial_started = true;
        return this.context_list.check(position, time, lap, lick_count,
                                       sensor_counts, msg_buffer);
    }


}
