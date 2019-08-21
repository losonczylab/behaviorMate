import processing.data.JSONObject;
import java.util.ArrayList;

public class JointContextList extends BasicContextList {

    protected String joint_list_id;
    protected ContextList joint_list;

    protected int offset;
    protected boolean fix_radius;

    public JointContextList(JSONObject context_info,
                            float track_length, String comm_id) {
        super(context_info, track_length, comm_id);
        this.joint_list_id = context_info.getString("joint_id");

        this.setRadius(context_info.getInt("radius", 0));
        this.fix_radius = (!context_info.hasKey("radius"));
        this.offset = context_info.getInt("offset", 0);
    }

    public void registerContexts(ArrayList<ContextList> contexts) {
        this.joint_list = null;
        for (int i = 0; i < contexts.size(); i++) {
            ContextList context_list = contexts.get(i);
            if (context_list.getId().equals(this.joint_list_id)) {
                context_list = new JointContextMasterDecorator(context_list, this);
                contexts.set(i, context_list);
                this.joint_list = context_list;
                break;
            }
        }

        if (this.joint_list == null) {
            //TODO: throw exception
            return;
        }

        System.out.println(this.fix_radius);
        if (this.fix_radius) {
            this.setRadius(this.joint_list.getRadius());
        }
        this.update();
    }

    public void update() {
        if (this.joint_list.size() != this.size()) {
            super.clear();
            for (int i = 0; i < this.joint_list.size(); i++) {
                super.add(this.joint_list.getLocation(i) + this.offset);
            }
        } else {
            for (int i = 0; i < this.joint_list.size(); i++) {
                super.move(i, this.joint_list.getLocation(i) + this.offset);
            }
        }
    }

    public void move(int index, int location) { }

    public void shuffle() { }

    protected void add(int location) { }

    public void clear() { }

    public void reset() { }
}
