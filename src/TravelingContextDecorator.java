import java.util.ArrayList;

import processing.data.JSONObject;
import processing.data.JSONArray;

public class TravelingContextDecorator extends ContextListDecorator {

    protected ArrayList<Integer> location_list;

    protected int last_lap;

    protected int current_lap;

    public TravelingContextDecorator(ContextList context_list,
                                     JSONObject context_info) {
        super(context_list);

        this.location_list = new ArrayList<Integer>();
        JSONArray location_array = null;
        if (!context_info.isNull("locations")) {
            location_array = context_info.getJSONArray("locations");
            this.location_list = new ArrayList<Integer>();
            int i = 0;
            for (; i < location_array.size(); i++) {
                this.location_list.add(location_array.getInt(i));
            }

            last_lap = location_array.size();
        }

        this.current_lap = 0;

        this.move(0, this.location_list.get(0));
    }

    public void reset() {
        this.context_list.reset();
        current_lap = current_lap + 1;
        this.move(0, this.location_list.get(current_lap % this.last_lap));
    }

    public void end() {
        this.context_list.end();
        current_lap = 0;
        this.move(0, this.location_list.get(0));
    }

}
