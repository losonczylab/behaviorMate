import processing.data.JSONObject;
import processing.data.JSONArray;
import java.util.concurrent.ThreadLocalRandom;

public class BlockedShuffleDecorator extends ContextListDecorator {

    protected int[] blocked_locations_starts;
    protected int[] blocked_locations_ends;
    protected int[] blocked_location_widths;
    protected int blocked_size;

    public BlockedShuffleDecorator(ContextList context_list,
                                   JSONObject context_info) {
        super(context_list);

        if (context_info.isNull("locations")) {
            this.blocked_locations_starts = new int[0];
            this.blocked_locations_ends = new int[0];
            this.blocked_location_widths = new int[0];
        } else {
            JSONArray locations_array = context_info.getJSONArray("locations");

            this.blocked_locations_starts = new int[locations_array.size()];
            this.blocked_locations_ends = new int[locations_array.size()];
            this.blocked_location_widths = new int[locations_array.size()];
            for (int i = 0; i < locations_array.size(); i++) {
                JSONArray start_stop = locations_array.getJSONArray(i);
                this.blocked_locations_starts[i] = start_stop.getInt(0);
                this.blocked_locations_ends[i] = start_stop.getInt(1);
                this.blocked_location_widths[i] = start_stop.getInt(1) - start_stop.getInt(0);
            }
        }

        blocked_size = 0;
        for (int i=0; i < this.blocked_locations_starts.length; i++) {
            blocked_size += this.blocked_locations_ends[i] -
                            this.blocked_locations_starts[i];
        }

        this.shuffle();
    }

    public void shuffle() {
        // return immediately if there are no contexts to shuffle
        if (this.context_list.size() == 0) {
            return;
        }

        int radius = this.getRadius();
        float track_length = this.getTrackLength() - this.blocked_size;
        int size = this.size();

        if (size == 1) {
            int new_location = ThreadLocalRandom.current().nextInt(
                radius, (int)(track_length-radius + 1));

            for (int i = 0; i < this.blocked_locations_ends.length; i++) {
                if (new_location >= this.blocked_locations_starts[i]) {
                    new_location += blocked_location_widths[i];
                }
            }

            this.context_list.move(0, new_location);
            return;
        }

        // initially position contexts evenly spaced
        int interval = (int)(track_length-2*radius)/size;
        this.move(0, radius + interval/2);
        for (int i = 1; i < size; i++) {
            this.move(i, this.getLocation(i-1) + interval);
        }

        // move the contexts randomly without allowing them to overlap
        int first_location = ThreadLocalRandom.current().nextInt(
            radius, (int)this.getLocation(1)-2*radius);
        this.move(0, first_location);

        for (int i = 1; i < size-1; i++) {
            int prev_location = this.getLocation(i-1);
            int next_location = this.getLocation(i+1);
            int new_location = ThreadLocalRandom.current().nextInt(
                prev_location+2*radius, next_location-2*radius);
            this.move(i, new_location);
        }

        int prev_location = this.getLocation(size-2);
        int new_location = ThreadLocalRandom.current().nextInt(
            prev_location+2*radius, (int)track_length-radius);
        this.move(size-1, new_location);

        for (int j=0; j < this.blocked_locations_starts.length; j++) {
            int width = this.blocked_location_widths[j];
            for (int i=0; i < size; i++) {
                int location = this.getLocation(i);
                if (location >= this.blocked_locations_starts[j]) {
                    this.move(i, location + width);
                }
            }
        }
    }

    public void reset() {
        this.context_list.reset();
        shuffle();
    }

    public void end() {
        this.context_list.end();
        shuffle();
    }

}
