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
            TrialListener el) {
        super(settings_string, system_string, el);
    }

    public boolean Start(String mouse_name, String experiment_group) {
        if ((position == -1) || mouse_name.equals("") ||
            (experiment_group.equals(""))) {
            return false;
        }

        display.setMouseName(
            "Trial Length: " + schedule.get(schedule.size()-1).time + " s");
        display.setLickCount(0);
        display.setLapCount(0);
        display.setRewardCount(0);
        lap_count=0;
        
        if (fWriter != null) {
            fWriter.close();
        }

        try {
            fWriter = new FileWriter(
                system_json.getString("data_directory", "data"),
                mouse_name, this.trialListener);
        } catch (IOException e) {
            return false;
        }

        Date startDate = Calendar.getInstance().getTime();
        
        JSONObject start_log = new JSONObject();
        start_log.setString("mouse", mouse_name);
        start_log.setString("experiment_group", experiment_group);
        start_log.setString("start", dateFormat.format(startDate));

        JSONObject info_msg = new JSONObject();
        JSONObject info_sub_msg = new JSONObject();
        info_sub_msg.setString("action", "info");
        info_msg.setJSONObject("communicator", info_sub_msg);

        try {
            JSONObject version_info = loadJSONObject("version.json");
            fWriter.write(version_info.toString());
        } catch (NullPointerException e) {
            println(e);
        }

        JSONObject settings_log = new JSONObject();
        settings_log.setJSONObject("settings", settings_json);
        fWriter.write(settings_log.toString());
        fWriter.write(start_log.toString());

        if (!settings_json.isNull("trial_startup")) {
            try {
                sendMessages(settings_json.getJSONArray("trial_startup"),
                    mouse_name);
            } catch (Exception e) {
                System.out.println(e);
                return false;
            }
        }

        trialListener.started(fWriter.getFile());

        timer.startTimer();
        JSONObject valve_json = open_valve_json(
            settings_json.getInt("sync_pin"), 100);
        behavior_comm.sendMessage(valve_json.toString());
        behavior_comm.sendMessage(info_msg.toString());

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
        settings_json.setFloat("trial_length", schedule.get(schedule.size()-1).time);
        displaySchedule();

        nextEvent = schedule.get(0);
    }

    public void setup() {
        sketchPath("");
        random = new Random();
        textSize(12);
        background(0);

        nextEvent = null;

        started = false;
        position = -1;
        lap_count = 0;
        lap_tag = "";
        fWriter = null;
        timer = new ExperimentTimer();

        display = new Display();
        display.prepGraphics(this);

        position_comm = null;
        behavior_comm = null;
        prepareExitHandler();
        trialListener.initialized();
    }

    public void startTrial() {
        JSONObject start_log = new JSONObject();
        Date dateTime = Calendar.getInstance().getTime();
        start_log.setFloat("time", timer.checkTime());
        start_log.setString("trial_start", dateFormat.format(dateTime));
        fWriter.write(start_log.toString());
    }

    public void endTrial() {
        JSONObject end_log = new JSONObject();
        Date dateTime = Calendar.getInstance().getTime();
        end_log.setFloat("time", timer.checkTime());
        end_log.setString("trial_end", dateFormat.format(dateTime));
        fWriter.write(end_log.toString());
    }

    public void draw() {
        float time = timer.checkTime();
        float dy = updatePosition(time);
        updateBehavior(time);

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
        int t = millis();
        int display_check = t-display_update;
        if (display_check > display_rate) {
            display.update(this, dy, position, time);
            display_update = t;
        }
    }

}
