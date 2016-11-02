import java.util.Collections;
import java.util.ArrayList;
import java.util.Random;
import java.util.Calendar;
import java.util.Date;
import java.io.IOException;

import processing.data.JSONObject;
import processing.data.JSONArray;

public class SalienceController extends TreadmillController {

    private class Event {
        public float time;
        public String type;
        public String message;
        public String text;
    }

    private ArrayList<Event> schedule;
    private Event nextEvent;

    float prestim_time;
    float stim_time;
    float poststim_time;
    float trial_length;
    int nblocks;
    private Date exptStartDate;

    Random random;

    public SalienceController(String settings_string, String system_string,
            TrialListener el, boolean useless) {
        super(settings_string, system_string, el, useless);
    }

    public SalienceController(String filename, String tag, TrialListener el) {
        super(filename, tag, el);
    }

    public boolean Start(String mouse_name, String experiment_group) {
        if ((position == -1) || mouse_name.equals("") ||
            (experiment_group.equals(""))) {
            return false;
        }

        //display.setMouseName(mouse_name);
        display.setLickCount(0);
        display.setLapCount(0);
        display.setRewardCount(0);
        lap_count=0;
        display.setMouseName("Trial Length: "+schedule.get(schedule.size()-1).time + " s");
        
        try {
            fWriter = new FileWriter(system_json.getString("data_directory", "data"), mouse_name);
        } catch (IOException e) {
            return false;
        }

        Date startDate = Calendar.getInstance().getTime();
        
        JSONObject start_log = new JSONObject();
        start_log.setString("mouse", mouse_name);
        start_log.setString("experiment_group", experiment_group);
        start_log.setString("start", dateFormat.format(startDate));

        JSONObject settings_log = new JSONObject();
        settings_log.setJSONObject("settings", settings_json);
        fWriter.write(settings_log.toString());
        fWriter.write(start_log.toString());

        trialListener.started(fWriter.getFile());

        startContexts();
        timer.startTimer();
        JSONObject valve_json = open_valve_json(settings_json.getInt("sync_pin"), 100);
        behavior_comm.sendMessage(valve_json.toString());

        started = true;
        return true;
    }

    public void displaySchedule() {
        String result = "";
        for (int i=0; i < schedule.size(); i++) {
            result = "" + result + schedule.get(i).text + "\n";
        }

        display.setSchedule(result);
    }

    /*
    private void startTrial() {
        fWriter = new FileWriter(system_json.getString("data_directory", "data"), mouse_name);
        Date startDate = Calendar.getInstance().getTime();
        
        JSONObject start_log = new JSONObject();
        start_log.setString("mouse", mouse_name);
        start_log.setString("start", dateFormat.format(startDate));
        //TODO:
        //start_log.setString("experiment_start", exptStartDate);

        JSONObject position_log = new JSONObject();
        position_log.setFloat("time", 0);
        position_log.setFloat("y", position);

        JSONObject settings_log = new JSONObject();
        settings_log.setJSONObject("settings", settings_json);
        fWriter.write(settings_log.toString());
        fWriter.write(start_log.toString());
        fWriter.write(position_log.toString());
    }

    private void endTrial() {
        Date stopDate = Calendar.getInstance().getTime();
       
        JSONObject end_log = new JSONObject();
        end_log.setFloat("time", timer.getTime());
        end_log.setString("stop", dateFormat.format(stopDate));
        fWriter.write(end_log.toString());

        fWriter = null;
    }*/

    public void createSchedule() {
        schedule = new ArrayList<Event>();
        prestim_time = settings_json.getFloat("prestim_time");
        stim_time = settings_json.getFloat("stim_time");
        poststim_time = settings_json.getFloat("poststim_time");
        nblocks = settings_json.getInt("num_blocks", 1);
        trial_length = prestim_time + stim_time + poststim_time;
        
        JSONArray stims = settings_json.getJSONArray("stims");
        ArrayList<JSONObject> stim_array = new ArrayList<JSONObject>();
        for (int i=0; i < stims.size(); i++) {
            JSONObject stim = stims.getJSONObject(i);
            stim_array.add(stim);

            if (!stim.isNull("pin")) {
                JSONObject valve_json;
                if (stim.isNull("frequency")) {
                    valve_json = setup_valve_json(stim.getInt("pin"));
                } else {
                    valve_json = setup_valve_json(
                        stim.getInt("pin"), stim.getInt("frequency"));
                }
                behavior_comm.sendMessage(valve_json.toString());
            }
        }
        
        float time_counter = 0;
        for (int j=0; j < nblocks; j++) {
            Collections.shuffle(stim_array);

            for (int i=0; i < stim_array.size(); i++) {
                Event startEvent = new Event();
                startEvent.time = time_counter;
                JSONObject start_json = open_valve_json(
                    settings_json.getInt("sync_pin"), 100);
                startEvent.message = start_json.toString();
                startEvent.text = startEvent.time + ": start";
                startEvent.type = "start";
                schedule.add(startEvent);

                JSONObject stim = stim_array.get(i);
                int duration = stim.getInt("duration");
                Event thisEvent = new Event();

                System.out.println(stim);
                if (stim.getString("address", "behavior_controller")
                        .equals("behavior_controller")) {
                    JSONObject open_json = open_valve_json(
                        stim.getInt("pin"), duration);
                    thisEvent.message = open_json.toString();
                } else if (stim.getString("address").equals("local_controller")) {
                    thisEvent.message = stim.toString();
                }
                thisEvent.time = time_counter + prestim_time;
                thisEvent.text = thisEvent.time + " " + stim.getString("name");
                thisEvent.type = stim.getString("name");

                println(thisEvent.text);
                schedule.add(thisEvent);

                Event endEvent = new Event();
                time_counter += trial_length;
                endEvent.time = time_counter;
                endEvent.text = endEvent.time + ": end";
                endEvent.type = "end";
                schedule.add(endEvent);

                time_counter += (int)(random(settings_json.getInt("intertrial_min", 5),
                  settings_json.getInt("intertrial_max", 10)));
            }

        }

        schedule.get(schedule.size()-1).type = "end_experiment";
        display.setMouseName("Next Trial: "+schedule.get(schedule.size()-1).time + " s");
        displaySchedule();

        nextEvent = schedule.get(0);
    }

    public void setup() {
        sketchPath("");
        random = new Random();
        textSize(12);
        background(0);

        next_reward = 0;
        nextEvent = null;

        started = false;
        laser_on_reward = false;
        rewarding = false;
        lasering = false;
        reward_start = 0;
        laser_start = 0;
        trial_duration = 0;
        next_reward = 0;
        next_laser = 0;
        //laser_locations = new int[0];
        position = -1;
        lap_count = 0;
        lap_tag = "";
        fWriter = null;
        timer = new ExperimentTimer();

        display = new Display();
        reload_settings();
        prepareExitHandler();
    }

    public void startTrial() {
        JSONObject start_log = new JSONObject();
        Date dateTime = Calendar.getInstance().getTime();
        start_log.setFloat("time", millis());
        start_log.setString("trial_start", dateFormat.format(dateTime));
        fWriter.write(start_log.toString());
    }

    public void endTrial() {
        JSONObject end_log = new JSONObject();
        Date dateTime = Calendar.getInstance().getTime();
        end_log.setFloat("time", millis());
        end_log.setString("trial_end", dateFormat.format(dateTime));
        fWriter.write(end_log.toString());
    }

    public void draw() {
        background(0);
        float time = timer.checkTime();
        float dy = updatePosition(time);

        if (behavior_comm.receiveMessage(json_buffer)) {
            JSONObject behavior_json =
                json_buffer.json.getJSONObject(behavior_comm.address);

            if (!behavior_json.isNull("lick")) {
                if (behavior_json.getJSONObject("lick")
                        .getString("action", "stop").equals("start")) {
                    display.addLick(started);
                }
            }

            if (!behavior_json.isNull("valve")) {
                JSONObject valveJson = behavior_json.getJSONObject("valve");
                if (valveJson.getString(""+reward_valve, "close").equals("open")) {
                    display.addReward();
                } else if ((valveJson.getInt("pin", -1) == reward_valve) &&          // This check is needed for new arduino syntax
                        valveJson.getString("action", "close").equals("open")) {
                    display.addReward();
                }
            }

            //TODO: tag_reader is replacing lap remove when transition is complete
            if ((!behavior_json.isNull("lap")) || (!behavior_json.isNull("tag_reader"))) {
                String tag = null;
                try {
                    tag = behavior_json.getJSONObject("lap").getString("tag");
                } catch (Exception e) {
                    if (!behavior_json.getJSONObject("tag_reader").isNull("tag")) {
                        tag = behavior_json.getJSONObject("tag_reader").getString("tag");
                    }
                }

                if (tag != null) {
                    display.setCurrentTag(tag);
                    if (tag.equals(lap_tag)) {
                        display.setLastLap(position);
                        position = 0;
                        next_reward = 0;
                        lap_count++;
                        if (moving_rewards) {
                            shuffle_rewards();
                        }
                        if (fWriter != null) {
                            JSONObject lap_log = new JSONObject();
                            lap_log.setFloat("time", time);
                            lap_log.setInt("lap", lap_count);
                            fWriter.write(lap_log.toString());
                        }
                    }
                }
            }
            
            if (fWriter != null) {
                json_buffer.json.setFloat("time", time);
                fWriter.write(json_buffer.json.toString());
            }
        }

        if ((nextEvent != null) && (time > nextEvent.time)) {
            println(nextEvent.text);
            println(nextEvent.type);
            /*
            if (nextEvent.type.equals("tone")) {
                JSONObject tone_info = JSONObject.parse(nextEvent.message);
                tone_stop = time + tone_info.getFloat("duration")/1000.0;
                tone = new Oscil( tone_info.getInt("freq"), 0.5f, Waves.SINE );
                tone.patch(out);
                JSONObject tone_json = new JSONObject();
                tone_json.setFloat("time", time);
                tone_json.setString("tone", "start");
                tone_json.setInt("freq", 500);
                fWriter.write(tone_json.toString());
            } else */
            if (nextEvent.type.equals("start")) {
                startTrial();
                behavior_comm.sendMessage(nextEvent.message);
            } else if (nextEvent.type.equals("end")) {
                endTrial();
            } else if (nextEvent.type.equals("end_experiment")) { 
                endExperiment();
            } else {
                behavior_comm.sendMessage(nextEvent.message);
            }
            schedule.remove(0);
            if (schedule.size() == 0) {
                endExperiment();
            } else {
                nextEvent = schedule.get(0);
                displaySchedule();
            }
        }

        /*
        if ((tone != null) && (time >= tone_stop)) {
            tone.unpatch(out);
            tone = null;
            JSONObject tone_json = new JSONObject();
            tone_json.setFloat("time", time);
            tone_json.setString("tone", "stop");
            fWriter.write(tone_json.toString());
            tone = null;
        }*/
        
        display.update(this, dy/position_scale, position, time, lasering);
    }

}
