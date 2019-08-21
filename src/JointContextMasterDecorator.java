public class JointContextMasterDecorator extends ContextListDecorator {
    protected JointContextList slave_list;
    
    public JointContextMasterDecorator(ContextList context_list,
                                       JointContextList slave_list) {
        super(context_list);
        this.slave_list = slave_list;
    }

    public void move(int index, int location) {
        this.context_list.move(index, location);
        this.slave_list.update();
    }

    public void shuffle() {
        this.context_list.shuffle();
        this.slave_list.update();
    }

    public void clear() {
        this.context_list.clear();
        this.slave_list.update();
    }

    public void reset() {
        this.context_list.reset();
        this.slave_list.update();
    }

    public void end() {
        this.context_list.end();
        this.slave_list.update();
    }
}
