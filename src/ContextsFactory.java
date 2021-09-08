import processing.data.JSONObject;
import processing.data.JSONArray;

/**
 * Entirely static class used to create a context list based on the "class" and "decorators"
 * attributes in the settings file.
 */
public final class ContextsFactory {

    /**
     * @param tc           <code>TreadmillController</code> running the experiment
     * @param display      Controls the UI
     * @param context_info Contains the configuration information for this <code>ContextList</code> from the
     *                     settings file
     * @param track_length The length of the track in mm
     * @param comm         Client to post messages for configuring, starting, and stopping the
     *                     <code>ContextList</code>
     *
     * @return A new <code>ContextList</code> matching the parameters specified in <tt>context_info</tt>.
     */
    public static ContextList Create(TreadmillController tc, Display display,
                                     JSONObject context_info, float track_length, UdpClient comm,
                                     String class_name) throws Exception {
        ContextList cl;
        JSONArray decorators = null;

        context_info = tc.parseJSONObject(context_info.toString());
        if (!context_info.isNull("decorators")) {
            decorators = context_info.getJSONArray("decorators");
            context_info.remove("decorators");
        }

        String controller = "behavior_controller";
        BasicContextList bcl = new BasicContextList(context_info, track_length, controller);


        if (class_name.equals( "alternating_context")) {
            cl = new AlternatingContextDecorator(bcl, context_info);
        } else if (class_name.equals( "random_context")) {
            cl = new RandomContextDecorator(bcl, context_info);
        } else if (class_name.equals( "timed_alt_context")) {
            AlternatingContextDecorator acd = new AlternatingContextDecorator(bcl, context_info);
            cl = new TimedContextDecorator(acd, context_info);
        } else if (class_name.equals( "scheduled_context")) {
            cl = new ScheduledContextDecorator(bcl, context_info);
        } else if (class_name.equals("vr2")) {
            cl = new VrContextList2(tc, context_info, track_length);
        } else if (class_name.equals("vr_extended")) {
            cl = new VrExtendedContextList(tc, context_info, track_length);
        } else if (class_name.equals("vr_cue2")) {
            cl = new VrCueContextList3(tc, context_info, track_length);
        } else if (class_name.equals("salience")) {
            cl = new SalienceContextList(tc, display, context_info, track_length, controller);
        } else if (class_name.equals("paired_reward_stim")) {
            cl = new PairedRewardStimContextList(context_info, track_length, controller);
        } else if (class_name.equals("gain_mod")) {
            cl = new GainModifiedContextList(tc, context_info, track_length);
        } else if (class_name.equals("fog_context")) {
            cl = new VrFogContext(tc, context_info, track_length);
        } else if (class_name.equals("joint_context")) {
            cl = new JointContextList(context_info, track_length, controller);
        } else {
            cl = bcl;
        }

        if (decorators != null) {
            JSONObject timed_context = null;
            for (int i=0; i < decorators.size(); i++) {
                JSONObject decorator = decorators.getJSONObject(i);
                String decorator_class = decorator.getString("class", "");
                if (decorator_class.equals("alternating_context")) {
                    cl = new AlternatingContextDecorator(cl, decorator);
                } else if (decorator_class.equals("running_context")) {
                    cl = new RunningContextDecorator(cl, decorator, track_length);
                } else if (decorator_class.equals("scheduled_context")) {
                    cl = new ScheduledContextDecorator(cl, decorator);
                } else if (decorator_class.equals("timed_context")) {
                    timed_context = decorator;
                } else if (decorator_class.equals("random_context")) {
                    cl = new RandomContextDecorator(cl, decorator);
                } else if (decorator_class.equals("lickstart_context")) {
                    cl = new LickStartContextDecorator(cl, decorator);
                } else if (decorator_class.equals("blocked_shuffle")) {
                    cl = new BlockedShuffleDecorator(cl, decorator);
                } else if (decorator_class.equals("timed_iti")){
                    cl = new TimedITIContextDecorator(tc, cl, decorator);
                } else if (decorator_class.equals("delayed_context")) {
                    cl = new DelayedContextDecorator(cl, decorator);
                } else if (decorator_class.equals("joint_suspend")) {
                    cl = new JointSuspendContextDecorator(cl, decorator);
                } else {
                    throw new IllegalArgumentException("Decorator "+ decorator_class +" not found");
                }
            }

            if (timed_context != null) {
                cl = new TimedContextDecorator(cl, timed_context);
            }
        }

        return cl;
    }
}
