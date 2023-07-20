import java.util.ArrayList;

import processing.data.JSONObject;
import processing.data.JSONArray;

/**
 * ?
 */
public class ScheduledContextDecorator extends SuspendableContextDecorator {

    /**
     * ?
     */
    protected ArrayList<Integer> lap_list;

    /**
     * ?
     */
    protected int repeat;

    /**
     * ?
     */
    protected boolean keep_on;

    /**
     * ?
     */
    protected int last_lap;

    /**
     * ?
     *
     * @param context_list <code>ContextList</code> instance the decorator will wrap.
     * @param context_info JSONObject containing the configuration information for this context
     *                     from the settings file. The following JSON literal should be defined
     *                     in the settings file. The property key: <datatype, value> means that the key
     *                     is optional and will default to value if not provided and should be of type
     *                     datatype if provided.
     *
     * {
     * 	    "lap_list": [ [<int[]>], [<int[]>], ... , [<int[]>] ],
     * 	    "no_display": <boolean, false>,
     * 	    "repeat": <int, 0>,
     * 	    "keep_on": <boolean, false>
     * }
     */
    public ScheduledContextDecorator(ContextList context_list, JSONObject context_info) {
        super(context_list);

        this.lap_list = new ArrayList<Integer>();
        JSONArray lap_array = null;
        if (!context_info.isNull("lap_list")) {
            lap_array = context_info.getJSONArray("lap_list");
            boolean lap_range;
            try {
                lap_array.getJSONArray(0);
                lap_range = true;
            } catch (RuntimeException e) {
                lap_range = false;
            }

            this.lap_list = new ArrayList<Integer>();
            if (!lap_range) {

                int i = 0;
                for (; i < lap_array.size(); i++) {
                    this.lap_list.add(lap_array.getInt(i));
                }

                last_lap = lap_array.getInt(i-1);
            } else {
                JSONArray range = null;
                for (int j = 0; j < lap_array.size(); j++) {
                    range = lap_array.getJSONArray(j);
                    for (int i = range.getInt(0); i < range.getInt(1); i++) {
                        this.lap_list.add(i);
                    }
                }

                if (range != null) {
                    this.last_lap = range.getInt(1)-1;
                } else {
                    this.last_lap = 0;
                }
            }
        }

        if (context_info.getBoolean("no_display", false)) {
            this.display_color_suspended = null;
        }

        this.repeat = context_info.getInt("repeat", 0);

        this.keep_on = context_info.getBoolean("keep_on", false);
    }

    /**
     * Check the state of the list as well as the contexts contained in this and decide if they
     * should be activated or not. Send the start/stop messages as necessary. this method gets
     * called for each cycle of the event loop when a trial is started.
     *
     * @param position   Current position along the track in millimeters.
     * @param time       Time (in s) since the start of the trial.
     * @param lap        Current lap number since the start of the trial.
     * @param lick_count ?
     * @param msg_buffer A Java <code>String</code> array to buffer and send messages to be
     *                   logged in the .tdml file being written for this trial. Messages should
     *                   be placed at index 0 of the message buffer and must be JSON-formatted strings.
     * @return           ?
     */
    public boolean check_suspend(float position, float time, int lap,
                                 int lick_count, JSONObject[] msg_buffer) {

        if ((keep_on) && (lap > last_lap)) {
            return false;
        }

        if (this.repeat == 0) {
            return (this.lap_list.indexOf(lap) == -1);
        }

        return (this.lap_list.indexOf(lap%this.repeat) == -1);
    }
}
