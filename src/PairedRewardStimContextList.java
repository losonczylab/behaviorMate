import processing.core.PApplet;
import processing.data.JSONObject;
import processing.data.JSONArray;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * ?
 */
public class PairedRewardStimContextList extends BasicContextList {

    /**
     * ?
     */
    protected int reward_valve;

    /**
     * ?
     */
    protected int stim_valve1;

    /**
     * ?
     */
    protected int stim_valve2;

    /**
     * ?
     */
    protected int[] frequency;

    /**
     * ?
     */
    protected int[] schedule;

    /**
     * ?
     */
    protected String close_valve1;

    /**
     * ?
     */
    protected String close_valve2;

    /**
     * ?
     */
    protected String open_valve1;

    /**
     * ?
     */
    protected String open_valve2;

    /**
     * ?
     */
    protected int current_reward;

    /**
     * ?
     */
    protected int schedule_ptr;

    /**
     * ?
     */
    protected int trial_num;

    /**
     * ?
     */
    protected boolean context_state;

    /**
     * ?
     */
    protected int[] reward_locations;

    /**
     * ?
     */
    protected ArrayList<ValveInfo> valves_list;

    /**
     * ?
     */
    protected String punishment_context_id;

    /**
     * ?
     */
    protected ContextList punishment_context;

    /**
     * ?
     */
    class ValveInfo {
        public int valve;
        public String start_msg;
        public String stop_msg;
    }

    /**
     * ?
     *
     * @param context_info JSONObject containing the configuration information for this context
     *                     from the settings file. <tt>context_info</tt> should have the following
     *                     JSON object defined:
     *                     {
     *                          "reward_settings": {
     *                              "location_1": my_int1,
     *                              "location_2": my_int2,
     *                              "radius": my_int3,
     *                              "valves": [ my_int4 ]
     *                          },
     *                          "stim1_valve": my_int5,
     *                          "stim2_valve": my_int6,
     *                          "schedule": [ int1, int2, ... ],
     *                          "frequency_1": my_int7,
     *                          "frequency_2": my_int8,
     *                          "punishment_context": my_string1
     *                     }
     *                     where the "my_" values are specified by the user in the settings file.
     * @param track_length The length of the track in millimeters.
     * @param comm_id ?
     */
    public PairedRewardStimContextList(JSONObject context_info, float track_length, String comm_id) {
        super(context_info, track_length, comm_id);

        int reward_location1 = this.context_info.getJSONObject("reward_settings").getInt("location_1");
        int reward_location2 = this.context_info.getJSONObject("reward_settings").getInt("location_2");
        this.radius = this.context_info.getJSONObject("reward_settings").getInt("radius");
        this.reward_valve = this.context_info.getJSONObject("reward_settings")
                                             .getJSONArray("valves")
                                             .getInt(0);
        this.reward_locations = new int[] { reward_location1, reward_location2 };
        this.stim_valve1 = this.context_info.getInt("stim1_valve");
        this.stim_valve2 = this.context_info.getInt("stim2_valve");
        ValveInfo info1 = new ValveInfo();
        info1.valve = this.stim_valve1;
        ValveInfo info2 = new ValveInfo();
        info2.valve = this.stim_valve2;
        this.valves_list = new ArrayList<ValveInfo>(Arrays.asList(info1, info2));

        this.schedule = this.context_info.getJSONArray("schedule").getIntArray();

        this.shuffle_contexts = false;
        this.frequency = new int[] { -1, this.context_info.getInt("frequency_1", -1),
                           this.context_info.getInt("frequency_2", -1) };

        this.setRadius(this.radius);
        this.move(0, this.reward_locations[0]);
        this.current_reward = -1;
        this.schedule_ptr = -1;
        this.trial_num = 0;
        this.context_state = false;

        this.punishment_context_id = this.context_info.getString("punishment_context", null);
    }

    /**
     * ?
     *
     * @param contexts ?
     */
    public void registerContexts(ArrayList<ContextList> contexts) {
        this.punishment_context = null;
        if (this.punishment_context_id != null) {
            for (ContextList contextList : contexts) {
                if (contextList.getId().equals(this.punishment_context_id)) {
                    this.punishment_context = contextList;

                    this.punishment_context.setRadius(this.radius);

                    this.punishment_context.move(0, this.reward_locations[1]);
                }
            }
        }
    }

    /**
     * ?
     */
    public void sendCreateMessages() {
        // comm may be null for certian subclasses of ContextList which to not
        // need to talk to the behavior arduino
        if (comm != null) {
            JSONObject ci = this.context_info.getJSONObject("reward_settings");
            ci.setString("action", "create");
            ci.setString("id", this.getId());

            JSONObject context_setup_json = new JSONObject();
            context_setup_json.setJSONObject("contexts", ci);

            // configure the valves, the pins which have devices responsible for
            // controlling this context
            int[] valves = {
                this.reward_valve, this.stim_valve1,
                this.stim_valve2 };

            for (int i = 0; i < valves.length; i++) {
                if (valves[i] == -1) {
                    continue;
                }
                JSONObject valve_json;

                // frequency causes this singal to oscillate in order to play a
                // tone
                int frequency = this.frequency[i];
                if (frequency != -1) {
                    valve_json = TreadmillController.setup_valve_json(
                        valves[i], frequency);
                } else if (!context_info.isNull("inverted") && (i != 0)) {
                    valve_json = TreadmillController.setup_valve_json(
                        valves[i], context_info.getBoolean("inverted"));
                } else {
                    valve_json = TreadmillController.setup_valve_json(
                        valves[i]);
                }

                comm.sendMessage(valve_json.toString());
                JSONObject close_json = TreadmillController.close_valve_json(
                    valves[i]);
                comm.sendMessage(close_json.toString());
            }

            this.active = -1;
            this.status = "reset";
            this.tries = 0;
            this.waiting = false;
            comm.sendMessage(context_setup_json.toString());

            String close_valve1 = TreadmillController.close_valve_json(
                this.stim_valve1).toString();
            String close_valve2 = TreadmillController.close_valve_json(
                this.stim_valve2).toString();
            String open_valve1 = TreadmillController.open_valve_json(
                this.stim_valve1, -1).toString();
            String open_valve2 = TreadmillController.open_valve_json(
                this.stim_valve2, -1).toString();
            this.valves_list.get(0).start_msg = open_valve1;
            this.valves_list.get(0).stop_msg = close_valve1;
            this.valves_list.get(1).start_msg = open_valve2;
            this.valves_list.get(1).stop_msg = close_valve2;

        } else {
            System.out.println(
                "[" +this.id+ " "  + this.comm_id +
                "] SEND CREATE MESSAGES FAILED");
        }
    }

    /**
     * Setter method for the context's id. Also configures the startString and stopString valves.
     * Todo: is the id used to identify punishment_context or the ArrayList of contexts
     *     inherited from BasicContextList?
     *
     * @param id Identifies this context when communicating with the comm.
     */
    protected void setId(String id) {
        this.id = id;

        JSONObject context_message = new JSONObject();
        context_message.setString("action", "start");
        context_message.setString("id", this.id);
        JSONObject context_message_json = new JSONObject();
        context_message_json.setJSONObject("contexts", context_message);
        this.startString = context_message_json.toString();

        context_message.setString("action", "stop");
        context_message_json.setJSONObject("contexts", context_message);
        this.stopString = context_message_json.toString();
    }

    /**
     * ?
     *
     * @param idx ?
     */
    protected void setReward(int idx) {
        if (idx != this.current_reward) {
            this.move(0, this.reward_locations[idx]);
            if (this.current_reward != -1) {
                this.sendMessage(
                    this.valves_list.get(this.current_reward).stop_msg);
            }
            this.sendMessage(this.valves_list.get(idx).start_msg);
            this.current_reward = idx;
            this.getContext(0).reset();

            if (this.punishment_context != null) {
                this.punishment_context.move(
                    0, this.reward_locations[(idx+1)%2]);
                this.punishment_context.getContext(0).reset();
                ((SuspendableContextDecorator)this.punishment_context).suspend();
            }
        }
        this.context_state = false;
    }


    /**
     * Check the state of the list as well as the contexts contained in this and decide if they
     * should be activated or not. Send the start/stop messages as necessary. This method gets
     * called for each cycle of the event loop when a trial is started.
     *
     * @param position   Current position along the track in millimeters.
     * @param time       Time (in s) since the start of the trial.
     * @param lap        Current lap number since the start of the trial.
     * @param msg_buffer A Java <code>String</code> array of type to buffer and send messages to be
     *                   logged in the .tdml file being written for this trial. messages should
     *                   be placed at index 0 of the message buffer and must be JSON-formatted strings.
     * @return           ?
     */
    public boolean check(float position, float time, int lap, JSONObject[] msg_buffer) {
        if (this.schedule_ptr == -1) {
            this.setReward(this.schedule[0]);
            this.schedule_ptr = 0;

            this.log_json.getJSONObject("context")
                         .setInt("trial_type", this.schedule[0]);
            this.log_json.getJSONObject("context")
                         .setInt("trial", this.trial_num);
            msg_buffer[0] = this.log_json;
        }

        boolean inZone = false;
        int i = 0;

        if (this.contexts.get(0).check(position, time, lap)) {
            inZone = true;
        }

        // Decide if the context defined by this ContextList needs to swtich
        // state and send the message to the UdpClient accordingly
        if (!waiting) {
            if ((!inZone) && (this.active != -1)) {
                this.status = "sent stop";
                this.active = -1;
                this.waiting = true;
                this.sent = time;
                this.sendMessage(this.stopString);

            } else if((inZone) && (this.active != i)) {
                this.active = i;
                this.waiting = true;
                this.sent = time;
                this.status = "sent start";
                this.sendMessage(this.startString);
                this.context_state = true;
            }
        }

        // Ensure that the context has actually started and reset if necessary
        if ((this.waiting) && (time-this.sent > 2)) {
            this.tries++;
            if (this.tries > 3) {
                System.out.println("[" + this.id + "] RESET CONTEXT " +
                                   this.tries);
                this.tries = 0;
                sendCreateMessages();
            } else {
                System.out.println("[" + this.id + "] RESEND TO CONTEXT " +
                                   this.tries);
                this.comm.setStatus(false);
                if (!inZone) {
                    this.sent = time;
                    this.sendMessage(this.stopString);
                } else if(inZone) {
                    this.sent = time;
                    this.sendMessage(this.startString);
                }
            }
        }

        if ((this.context_state) &&
                (position > this.getContext(0).location() + this.radius)) {
            this.getContext(0).disable();
        }

        if ((this.context_state) && (!this.getContext(0).isEnabled())) {
            this.schedule_ptr = (this.schedule_ptr + 1) % this.schedule.length;
            this.setReward(this.schedule[this.schedule_ptr]);

            this.trial_num++;
            this.log_json.getJSONObject("context")
                         .setInt("trial_type", this.schedule[this.schedule_ptr]);
            this.log_json.getJSONObject("context")
                         .setInt("trial", this.trial_num);
            msg_buffer[0] = this.log_json;
        }

        return (this.active != -1);
    }

    /**
     * ?
     *
     * @param time ?
     * @param msg_buffer ?
     */
    public void stop(float time, JSONObject[] msg_buffer) {
        this.current_reward = -1;
        this.schedule_ptr = -1;
        this.trial_num = 0;
        this.context_state = false;

        this.active = -1;
        this.status = "sent stop";
        this.waiting = false;
        this.context_state = false;
        this.sendMessage(this.stopString);
        this.sendMessage(this.valves_list.get(0).stop_msg);
        this.sendMessage(this.valves_list.get(1).stop_msg);
    }
}
