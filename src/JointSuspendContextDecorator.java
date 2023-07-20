import processing.data.JSONObject;
import java.util.ArrayList;

/**
 * ?
 */
public class JointSuspendContextDecorator extends SuspendableContextDecorator {

    /**
     *
     */
    protected String joint_list_id;

    /**
     *
     */
    protected boolean master;

    /**
     *
     */
    protected ArrayList<SuspendableContextDecorator> master_suspendables;
    protected boolean invert;

    /**
     * ?
     *
     * @param context_list <code>ContextList</code> instance the decorator will wrap.
     * @param context_info JSONObject containing the configuration information for this context
     *                     from the settings file. <tt>context_info</tt> should have the parameter
     *                     <tt>joint_id</tt> set to do ?. The <tt>master</tt> parameter is optional
     *                     and will default to false if not provided.
     */
    public JointSuspendContextDecorator(ContextList context_list,
                                        JSONObject context_info) {
        super(context_list);

        this.joint_list_id = context_info.getString("joint_id");
        this.master = context_info.getBoolean("master", false);
        this.invert = context_info.getBoolean("invert", false);
    }

    /**
     * ?
     *
     * @param contexts ?
     */
    public void registerContexts(ArrayList<ContextList> contexts) {
        ContextList joint_list = null;
        for (int i = 0; i < contexts.size(); i++) {
            ContextList context_list = contexts.get(i);
            if (context_list.getId().equals(this.joint_list_id)) {
                joint_list = context_list;

                String other_id = "";
                while (true) {
                    try {
                        other_id = ((JointSuspendContextDecorator) context_list).joint_id();
                    } catch (Exception e) { }

                    if (other_id.equals(this.getId())) {
                        break;
                    }

                    try {
                        context_list = ((ContextListDecorator) context_list).getContextListBase();
                    } catch (ClassCastException e) { 
                        throw new IllegalArgumentException(
                            "Joint List [" + this.joint_list_id + "] Not Found");
                    }
                }

                break;
            }
        }

        this.master_suspendables = new ArrayList<SuspendableContextDecorator>();
        ContextListDecorator cl = (ContextListDecorator) joint_list;
        while (true) {
            SuspendableContextDecorator sus_context = null;
            try {
                sus_context = ((SuspendableContextDecorator) cl);
            } catch (Exception e) { }

            if (sus_context != null) {
                this.master_suspendables.add(sus_context);
            }

            try {
                cl = (ContextListDecorator) cl.getContextListBase();
            } catch (ClassCastException e) {
                break;
            }
        }
    }

    /**
     * Suspend all contexts belonging to the wrapped <code>ContextList</code> (inherited from
     * <code>ContextListDecorator</code>).
     */
    public void suspend() {
        super.suspend();
    }

    /**
     *
     * @return <code>true</code> if all wrapped <code>SuspendableContextDecorator</code> objects
     * are suspended, <code>false</code> otherwise.
     */
    protected boolean masterSuspended() {
        for (SuspendableContextDecorator sus_context : this.master_suspendables) {
            if (sus_context.isSuspended()) {
                return true;
            }
        }

        return false;
    }

    /**
     *
     * @return ?
     */
    public String joint_id() {
        return joint_list_id;
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
     * @param msg_buffer A Java <code>String</code> array of type to buffer and send messages to be
     *                   logged in the .tdml file being written for this trial. messages should
     *                   be placed at index 0 of the message buffer and must be JSON-formatted strings.
     * @return           ?
     */
    public boolean check_suspend(float position, float time, int lap, int lick_count,
                                 JSONObject[] msg_buffer) {

        if (!this.master) {
            boolean master_suspended = this.masterSuspended();

            return ((!master_suspended && this.invert) ||
                    (master_suspended && !this.invert));
        } else {
            return false;
        }
    }
}
