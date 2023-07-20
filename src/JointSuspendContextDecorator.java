import processing.data.JSONObject;
import java.util.ArrayList;

public class JointSuspendContextDecorator extends SuspendableContextDecorator {

    protected String joint_list_id;
    protected boolean master;
    protected ArrayList<SuspendableContextDecorator> master_suspendables;
    protected boolean invert;

    public JointSuspendContextDecorator(ContextList context_list,
                                        JSONObject context_info) {
        super(context_list);

        this.joint_list_id = context_info.getString("joint_id");
        this.master = context_info.getBoolean("master", false);
        this.invert = context_info.getBoolean("invert", false);
    }


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

    public void suspend() {
        super.suspend();
    }

    protected boolean masterSuspended() {
        for (SuspendableContextDecorator sus_context : this.master_suspendables) {
            if (sus_context.isSuspended()) {
                return true;
            }
        }

        return false;
    }

    public String joint_id() {
        return joint_list_id;
    }

    public boolean check_suspend(float position, float time, int lap,
                                 int lick_count, JSONObject[] msg_buffer) {

        if (!this.master) {
            boolean master_suspended = this.masterSuspended();

            return ((!master_suspended && this.invert) ||
                    (master_suspended && !this.invert));
        } else {
            return false;
        }
    }
}
