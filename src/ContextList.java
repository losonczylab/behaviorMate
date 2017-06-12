import processing.core.PApplet;
import processing.data.JSONObject;
import processing.data.JSONArray;
import java.util.ArrayList;

/**
 * ContextList class. Controlls activating and stopping contexts as the animal
 * progresses along the track. See {@link #ContextList(check)} for how this
 * logic is controlled.
 */
public class ContextList extends PApplet {
    /**
     * list of type context to hold the timing and position information about
     * specific locations this context is active. Contexts become inactive after
     * they're triggered and resent with each lap.
     */
    protected ArrayList<Context> contexts;

    /**
     * distance (in mm) around each location that contexts span.
     */
    protected int radius;

    /**
     * amount fo time (in seconds) that a context may remain active once it has
     * been triggered.
     */
    protected int duration;

    /**
     * interger corresponding to the index of the curretly active context in the
     * ArrayList of contexts. if -1 then no context is currently active.
     */
    protected int active;

    /**
     * stores the time the last update was sent to this context.
     */
    protected float sent;

    /**
     * if true a message has been sent and the ContextList is waiting for a
     * response
     */
    protected boolean waiting;

    /**
     * counts the number of tries to send a message to the arduino so the
     * context can send reset messages if nothing is getting through
     */
    protected int tries;

    /**
     * a status string to be displayed in the UI.
     */
    protected String status;

    /**
     * if set to true, the location of the contexts will be shuffled between
     * laps or when the ContextList is reset.
     */
    protected boolean shuffle_contexts;

    /**
     * the length of each lap (in mm).
     */
    protected float track_length;

    /**
     * the display object corresponding to the UI.
     */
    protected Display display;

    /**
     * the color to represent the current context in the UI.
     */
    protected int display_color;

    /**
     * the radius to represent this context as in the UI.
     */
    protected float display_radius;

    /**
     * UdpClient for sending messages to which relate to this context.
     */
    protected UdpClient comm;

    /**
     * a String identifier to identify this context to the UI as well as in the
     * behavior file.
     */
    protected String id;

    /**
     * the message to send via UDP at the start of each of these context.
     */
    protected String startString;

    /**
     * the message to send via UDP at the end of each of these context.
     */
    protected String stopString;

    protected JSONObject context_info;

    /**
     * Constructor.
     *
     * @param display      display object which controlls the UI
     * @param context_info json object containing the configureation information
     *                     for this context from the settings.json file
     * @param track_length the length of the track (in mm).
     * @param comm         client to post messages which configure as well as
     *                     starts and stop the context
     */
    public ContextList(Display display, JSONObject context_info,
            float track_length, UdpClient comm) {
        this.contexts = new ArrayList<Context>();
        this.display = display;
        this.comm = comm;
        this.sent = -1;
        this.tries = 0;
        this.waiting = false;

        // sets startString and stopString as well as the id field
        setId(context_info.getString("id"));

        this.duration = context_info.getInt("max_duration", -1);
        this.radius = context_info.getInt("radius", -1);
        this.active = -1;
        this.status = "";
        this.track_length = track_length;
        this.context_info = context_info;

        // resplve the display color from rbg in the settings to an integer
        this.display_color = -1;
        if (!context_info.isNull("display_color")) {
            JSONArray disp_color = context_info.getJSONArray("display_color");
            this.display_color = color(disp_color.getInt(0),
                disp_color.getInt(1), disp_color.getInt(2));
        }

        // positions the contexts
        this.shuffle_contexts = false;
        JSONArray locations = null;
        try {
            locations = context_info.getJSONArray("locations");
        } catch (RuntimeException e) { }

        // if loactions is null - specific locations for this context are not
        // supplied
        if (locations != null) {
            for (int i=0; i < locations.size(); i++) {
                add(locations.getInt(i));
            }
        } else {
            // either the field "number" is presevered for compatibility with
            // old settings files. now "locations" not being an integer instead
            // of a list is sufficient
            int num_contexts = context_info.getInt("locations",
                context_info.getInt("number", 0));

            if (num_contexts == 0) {
                // if no number is assigned, then assign this context to the
                // entire track
                add((int)(track_length/2.0) + 2);
                this.radius = (int)(track_length/2.0) + 2;
            } else {
                // otherwise add the contexts randomlly and shuffle each lap
                for (int i=0; i < num_contexts; i++) {
                    add((int)(track_length/2.0));
                    this.shuffle_contexts = true;
                    shuffle();
                    //setShuffle(true, track_length);
                }
            }
        }
        display.setContextLocations(this);

        sendCreateMessages();
    }

    public void sendCreateMessages() {
        // comm may be null for certian subclasses of ContextList which to not
        // need to talk to the behavior arduino
        if (comm != null) {
            context_info.setString("action", "create");
            JSONObject context_setup_json = new JSONObject();
            context_setup_json.setJSONObject("contexts", context_info);

            // configure the valves, the pins which have devices responsible for
            // controlling this context
            JSONArray valves = null;
            if (!context_info.isNull("valves")) {
                valves = context_info.getJSONArray("valves");
            }

            for (int i=0; ((valves != null) && (i < valves.size())); i++) {
                int valve_pin = valves.getInt(i);
                JSONObject valve_json;

                // frequency causes this singal to oscillate in order to play a
                // tone
                if (!context_info.isNull("frequency")) {
                    valve_json = TreadmillController.setup_valve_json(valve_pin,
                        context_info.getInt("frequency"));
                } else {
                    valve_json = TreadmillController.setup_valve_json(
                        valve_pin);
                }
                comm.sendMessage(valve_json.toString());
                JSONObject close_json = TreadmillController.close_valve_json(
                    valve_pin);
                comm.sendMessage(close_json.toString());
            }

            this.active = -1;
            this.status = "reset";
            this.tries = 0;
            this.waiting = false;
            comm.sendMessage(context_setup_json.toString());
        }
    }

    /**
     * Setter method for the UdpClient.
     *
     * @param comm channel to post messages to for configuring, starting or
     * stopping contexts.
     */
    public void setComm(UdpClient comm) {
        this.comm = comm;
    }

    /**
     * Setter method for the context's id. also configures the startString and
     * stopString valves.
     *
     * @param id a string identifier that is send to the comm to specifically
     *           identify this context.
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
     * Accessor for this conext's id.
     *
     * @return the id of this ContextList
     */
    public String getId() {
        return this.id;
    }

    /**
     * Setter for the display radius. Defines how wide this context appears on
     * the UI baed on the provided display scale.
     *
     * @param scale the amount to scale the radius so as to display properly.
     *              Units are in pixel/mm
     */
    public void setDisplayScale(float scale) {
        this.display_radius = ((float)this.radius) * scale;
    }

    /**
     * Accessor for the dispaly radius.
     *
     * @return the width, in pixels, to represent this context on the UI.
     */
    public float displayRadius() {
        return this.display_radius;
    }

    /**
     * Accessor for the display color.
     *
     * @return returns a color, as an integer value. see the processing.org
     *         color function.
     */
    public int displayColor() {
        return this.display_color;
    }

    /**
     * Setter for the string displayed in the UI which corresponds to the state
     * of these contexts.
     *
     * @param status the string to display in the UI.
     */
    public void setStatus(String status) {
        this.status = status;

        // if the status has been updated, then the last update has reached the
        // arduino
        waiting = false;
        this.tries = 0;
    }

    /**
     *  Accessor for the status string.
     *
     * @return the string representing the current status of the contexts in
     * this list.
     */
    public String getStatus() {
        return this.status;
    }

    /**
     * Accessor for the size of this ContextList.
     *
     * @return The number of contexts  contained in the list.
     */
    public int size() {
        return this.contexts.size();
    }

    /**
     * Accessor for a specific Context in the List.
     *
     * @param i index of the context to return
     * @return  the context at the supplied index.
     */
    public int getLocation(int i) {
        return this.contexts.get(i).location();
    }

    /**
     * Push a new context on to the List of Contexts.
     *
     * @param location integer in mm corresponding to the loction to place this
     *                 context.
     */
    protected void add(int location) {
        this.contexts.add(new Context(location, this.duration,
            this.radius, this.contexts.size()));
    }

    /**
     * If there are any contexts in this list, create a new new list (remove
     * the contexts).
     */
    public void clear() {
        if (this.size() > 0) {
            this.contexts = new ArrayList<Context>();
        }
    }

    /**
     * Resets the state of the contexts. Contexts which have been triggered are
     * reactivated and allowed to be triggered again. If this list is shuffeling
     * then execute the shuffle.
     */
    protected void reset() {
        for (int i=0; i < this.contexts.size(); i++) {
            this.contexts.get(i).reset();
        }

        if (this.shuffle_contexts) {
            shuffle();
        }
    }

    /**
     * Shuffles the location of each of the contexts contained in this list.
     */
    public void shuffle() {
        // return immediately if there are no contexts to shuffle
        if (this.contexts.size() == 0) {
            return;
        }

        if (this.contexts.size() == 1) {
            contexts.get(0).move((int) random(this.radius, this.track_length-this.radius));
            return;
        }

        // initially position contexts evenly spaced
        int interval = (int)(this.track_length-2*this.radius)/this.contexts.size();
        contexts.get(0).move(this.radius + interval/2);
        for (int i = 1; i < this.contexts.size(); i++) {
            this.contexts.get(i).move(this.contexts.get(i-1).location() + interval);
        }

        // move the contexts randomlly without allowing them to overlap
        this.contexts.get(0).move(
            (int) random(this.radius,this.contexts.get(1).location()-2*this.radius));

        for (int i = 1; i < this.contexts.size()-1; i++) {
            int prev_location = this.contexts.get(i-1).location();
            int next_location = this.contexts.get(i+1).location();
            this.contexts.get(i).move(
                (int) random(prev_location+2*this.radius, next_location-2*this.radius));
        }

        int prev_location = this.contexts.get(this.size()-2).location();
        this.contexts.get(this.size()-1).move(
            (int) random(prev_location+2*this.radius, this.track_length-this.radius));
    }

    /**
     * Create a java style array out of the context locations.
     *
     * @return list of context locations.
     */
    public int[] toList() {
        int[] list = new int[contexts.size()];
        for (int i=0; i < this.contexts.size(); i++) {
            list[i] = this.contexts.get(i).location;
        }

        return list;
    }

    /**
     * Check the state of the contexts contained in this list and send the
     * start/stop messages as necessary. this method gets called for each cycle
     * of the event loop when a trial is started.
     *
     * @param position   current position along the track
     * @param time       time (in s) since the start of the trial
     * @param lap        current lap number since the start of the trial
     * @param msg_buffer a Java array of type String to buffer and send messages
     *                   to be logged in the the tdml file being written for
     *                   this trial. messages should be placed in index 0 of the
     *                   message buffer and must be JSON formatted strings.
     * @return           returns true to indicate that the trial has started.
     *                   Note: all messages to the behavior comm are sent from
     *                   within this method returning true or false indicates
     *                   the state of the context, but does not actually
     *                   influence the connected arduinos or UI.
     */
    public boolean check(float position, float time, int lap,
            String[] msg_buffer) {
        boolean inZone = false;
        int i=0;

        // This loop checks to see if any of the individual contexts are
        // triggered to be active both in space and time
        for (; i < this.contexts.size(); i++) {
            if (this.contexts.get(i).check(position, time, lap)) {
                inZone = true;
                break;
            }
        }

        // Decide if the context defined by this ContextList needs to swtich
        // state and send the message to the UdpClient accordingly
        if (!waiting) {
            if ((!inZone) && (this.active != -1)) {
                this.status = "sent stop";
                this.active = -1;
                this.waiting = true;
                this.sent = time;
                this.comm.sendMessage(this.stopString);
            } else if((inZone) && (this.active != i)) {
                this.active = i;
                this.waiting = true;
                this.sent = time;
                this.status = "sent start";
                this.comm.sendMessage(this.startString);
            }
        }

        // Ensure that the context has actually started and reset if necessary
        if ((this.waiting) && (time-this.sent > 2)) {
            System.out.println("RESET CONTEXT " + this.tries);
            this.tries++;
            if (this.tries > 3) {
                this.tries = 0;
                sendCreateMessages();
            } else {
                if (!inZone) {
                    this.sent = time;
                    this.comm.sendMessage(this.stopString);
                } else if(inZone) {
                    this.sent = time;
                    this.comm.sendMessage(this.startString);
                }
            }
        }

        return (this.active != -1);
    }

    /**
     * Stop this context. Called at the end of trials to ensure that the context
     * is shut off.
     */
    public void stop(float time, String[] msg_buffer) {
        this.active = -1;
        this.status = "sent stop";
        this.waiting = false;
        this.comm.sendMessage(this.stopString);
    }
}
