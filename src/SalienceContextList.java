import java.util.Collections;
import java.util.ArrayList;
import java.util.Random;
import java.util.Iterator;
import java.util.Calendar;
import java.util.Date;
import java.io.IOException;

import processing.data.JSONObject;
import processing.data.JSONArray;

public class SalienceContextList extends BasicContextList {

    private class Event {
        public float time;
        public String type;
        public String message;
        public String text;
    }

    private class MultiEvent extends Event {
        public String[] messages;
        public float[] times;

        public boolean shift() {
            this.message = this.messages[0];
            this.time = this.times[0];

            for (int i=0; i < messages.length-1; i++) {
                this.times[i] = this.times[i+1];
                this.messages[i] = this.messages[i+1];
            }

            this.messages[messages.length-1] = null;

            return this.message != null;
        }
    }

    private ArrayList<Event> schedule;
    private Event nextEvent;
    private TreadmillController tc;
    protected Display display;
    protected int[] display_color_active;
    protected boolean repeat;
    protected float repeat_interval;

    float stim_time;
    float event_time;
    float prestim_time;
    float poststim_time;
    float trial_length;
    int nblocks;
    private Date exptStartDate;
    private ArrayList<JSONObject> stim_array;

    Random random;

    public SalienceContextList(TreadmillController tc, Display display,
            JSONObject context_info, float track_length, String comm_id)
            throws Exception {
        super(context_info, track_length, comm_id);

        this.tc = tc;
        this.nblocks = context_info.getInt("num_blocks");
        this.stim_time = context_info.getFloat("stim_time");
        this.prestim_time = context_info.getFloat("prestim_time");
        this.poststim_time = context_info.getFloat("poststim_time");

        this.repeat = context_info.getBoolean("repeat", false);
        if (this.repeat) {
            this.repeat_interval = context_info.getFloat("repeat_interval", 0);
        }

        if (this.display_color != null) {
            this.display_color_active = this.display_color;
            this.display_color = null;
        } else {
            System.out.println("display_color null");
            this.display_color_active = new int[] {150, 50, 20};
        }


        this.trial_length = prestim_time + stim_time + poststim_time;
        this.display = display;
        this.event_time = -1;
        this.display = display;

    }

    public void sendCreateMessages() {
        JSONArray stims = this.context_info.getJSONArray("stims");
        stim_array = new ArrayList<JSONObject>();
        for (int i=0; i < stims.size(); i++) {
            JSONObject stim = stims.getJSONObject(i);
            stim_array.add(stim);
            if (!stim.isNull("pin")) {
                try {
                    JSONArray pins = stim.getJSONArray("pin");
                    try {
                        JSONArray frequencies = null;
                        if (!stim.isNull("frequency")) {
                            frequencies = stim.getJSONArray("frequency");
                        }

                        for (int j=0; j<pins.size(); j++) {
                            JSONObject valve_json;
                            if ((frequencies == null) ||
                                (frequencies.getInt(j) == 0)) {
                                valve_json = TreadmillController.setup_valve_json(
                                    pins.getInt(j));
                            } else {
                                valve_json = TreadmillController.setup_valve_json(
                                    pins.getInt(j), frequencies.getInt(j));
                            }

                            this.comm.sendMessage(valve_json.toString());
                        }
                    } catch (Exception e) {
                        throw e;
                    }
                } catch (RuntimeException e) {
                    JSONObject valve_json;
                    if (stim.isNull("frequency")) {
                        valve_json = TreadmillController.setup_valve_json(stim.getInt("pin"));
                    } else {
                        valve_json = TreadmillController.setup_valve_json(
                            stim.getInt("pin"), stim.getInt("frequency"));
                    }
                    this.comm.sendMessage(valve_json.toString());
                }
            }
        }

        createSchedule();
    }


    private Event createEvent(JSONObject stim, float time_counter) {
        Event thisEvent = new Event();

        int duration = stim.getInt("duration");
        if (stim.getString("address", "behavior_controller")
                .equals("behavior_controller")) {
            JSONObject open_json = TreadmillController.open_valve_json(
                stim.getInt("pin"), duration);
            thisEvent.message = open_json.toString();
        } else if (stim.getString("address").equals("local_controller")) {
            thisEvent.message = stim.toString();
        }

        thisEvent.time = time_counter + this.prestim_time;
        thisEvent.text = thisEvent.time + " " + stim.getString("name");
        thisEvent.type = stim.getString("name");

        return thisEvent;
    }


    private Event createMultiEvent(JSONObject stim, float time_counter) {
        MultiEvent thisEvent = new MultiEvent();

        if (stim.getString("address", "behavior_controller")
                .equals("behavior_controller")) {

            JSONArray pins = stim.getJSONArray("pin");

            int[] durations = new int[pins.size()];
            JSONArray durations_array = null;
            try {
                durations_array = stim.getJSONArray("duration");
            } catch (RuntimeException e) {}

            for (int j=0; j < durations.length; j++) {
                if (durations_array != null) {
                    durations[j] = durations_array.getInt(j);
                } else {
                    durations[j] = stim.getInt("duration");
                }
            }

            String[] messages = new String[pins.size()];
            for (int j=0; j < pins.size(); j++) {
                JSONObject open_json = TreadmillController.open_valve_json(
                    pins.getInt(j), durations[j]);
                messages[j] = open_json.toString();
            }
            thisEvent.messages = messages;

            if (!stim.isNull("offset_times")) {
                float time = time_counter + this.prestim_time;
                float[] times = new float[pins.size()];
                JSONArray offsets_array = stim.getJSONArray("offset_times");
                for (int j=0; j < times.length; j++) {
                    times[j] = time + offsets_array.getFloat(j)/1000;
                }
                thisEvent.times = times;
                thisEvent.shift();
            } else {
                thisEvent.time = time_counter + this.prestim_time;
                thisEvent.message = null;
            }
        }

        thisEvent.text = thisEvent.time + " " + stim.getString("name");
        thisEvent.type = stim.getString("name");

        return thisEvent;
    }


    public void createSchedule(float start_time) {
        this.schedule = new ArrayList<Event>();

        float time_counter = start_time;
        for (int j=0; j < nblocks; j++) {
            Collections.shuffle(stim_array);

            for (int i=0; i < stim_array.size(); i++) {
                Event startEvent = new Event();
                startEvent.time = time_counter;
                JSONObject start_json = TreadmillController.open_valve_json(
                    this.context_info.getInt("sync_pin"), 100);
                startEvent.message = start_json.toString();
                startEvent.text = startEvent.time + ": start";
                startEvent.type = "start";
                schedule.add(startEvent);

                JSONObject stim = stim_array.get(i);
                Event thisEvent = null;
                try {
                    JSONArray pins = stim.getJSONArray("pin");

                    try {
                        thisEvent = createMultiEvent(stim, time_counter);
                    } catch (Exception e) {
                        throw e;
                    }
                } catch (RuntimeException e) {
                    thisEvent = createEvent(stim, time_counter);
                }

                /*
                int duration = stim.getInt("duration");

                if (stim.getString("address", "behavior_controller")
                        .equals("behavior_controller")) {
                    JSONObject open_json = TreadmillController.open_valve_json(
                        stim.getInt("pin"), duration);
                    thisEvent.message = open_json.toString();
                } else if (stim.getString("address").equals("local_controller")) {
                    thisEvent.message = stim.toString();
                }
                thisEvent.time = time_counter + prestim_time;
                thisEvent.text = thisEvent.time + " " + stim.getString("name");
                thisEvent.type = stim.getString("name");
                */

                schedule.add(thisEvent);

                Event endEvent = new Event();
                time_counter += trial_length;
                endEvent.time = time_counter;
                endEvent.text = endEvent.time + ": end";
                endEvent.type = "end";
                schedule.add(endEvent);

                time_counter += (int)(random(this.context_info.getInt("intertrial_min", 5),
                  this.context_info.getInt("intertrial_max", 10)));
            }

        }

        if (!this.repeat) {
            schedule.get(schedule.size()-1).type = "end_experiment";
        }
        this.context_info.setFloat("trial_length", schedule.get(schedule.size()-1).time);
        displaySchedule();

        nextEvent = schedule.get(0);
    }

    public void createSchedule() {
        createSchedule(0f);
    }

    public void displaySchedule() {
        String result = "";
        int i;
        for (i=0; ((i < schedule.size()) && (i < 20)); i++) {
            result = "" + result + schedule.get(i).text + "\n";
        }

        if (i < schedule.size()) {
            result += "...";
        }

        this.display.setSchedule(result);
        if (!this.repeat) {
            display.setMouseName(
                "Next Trial: " +
                schedule.get(schedule.size()-1).time +
                "s");
        }
    }

    public void startTrial(float time, JSONObject[] msg_buffer) {
        JSONObject start_log = new JSONObject();
        Date dateTime = Calendar.getInstance().getTime();
        start_log.setFloat("time", time);
        start_log.setString("trial_start", this.tc.dateFormat.format(dateTime));
        msg_buffer[0] = start_log;
    }

    public void endTrial(float time, JSONObject[] msg_buffer) {
        JSONObject end_log = new JSONObject();
        Date dateTime = Calendar.getInstance().getTime();
        end_log.setFloat("time", time);
        end_log.setString("trial_end", this.tc.dateFormat.format(dateTime));
        msg_buffer[0] = end_log;
    }


    public boolean check(float position, float time, int lap,
            JSONObject[] msg_buffer) {

        if ((this.event_time != -1) &&
                (time > (this.event_time + this.stim_time))) {
            this.display_color = null;
            this.status = "post-stim";
            this.event_time = -1;
        }

        if ((nextEvent != null) && (time > nextEvent.time)) {
            boolean removeEvent = true;
            if (nextEvent.type.equals("start")) {
                this.status = "pre-stim";
                startTrial(time, msg_buffer);
                this.comm.sendMessage(nextEvent.message);
            } else if (nextEvent.type.equals("end")) {
                endTrial(time, msg_buffer);
            } else if (nextEvent.type.equals("end_experiment")) {
                tc.endExperiment();
            } else {
                this.status = nextEvent.type;
                this.event_time = time;
                this.display_color = this.display_color_active;

                if (nextEvent instanceof MultiEvent) {
                    MultiEvent m_nextEvent = (MultiEvent)nextEvent;
                    if (m_nextEvent.message == null) {
                        for (int i = 0; i < m_nextEvent.messages.length; i++) {
                            this.comm.sendMessage(m_nextEvent.messages[i]);
                        }
                    } else {
                        this.comm.sendMessage(m_nextEvent.message);
                        m_nextEvent.shift();
                        removeEvent = (m_nextEvent.message == null);
                    }
                } else {
                    this.comm.sendMessage(nextEvent.message);
                }
            }

            if (removeEvent) {
                schedule.remove(0);
                if (schedule.size() == 0) {
                    if (!this.repeat) {
                        tc.endExperiment();
                    } else {
                        createSchedule(Math.round(time + this.repeat_interval));
                    }
                } else {
                    nextEvent = schedule.get(0);
                    displaySchedule();
                }
            }

            return true;
        }

        return false;
    }

    public void stop(float time, JSONObject[] msg_buffer) {
        createSchedule();
        super.stop(time, msg_buffer);
    }
}
