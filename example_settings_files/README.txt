Description of example settings files:

1. `local_test.json`: This file is intended for testing the UI locally. Creates
a 2000 mm long track with operant reward zones at positions 500 and 1500. Both
position and behavior controllers have IP addresses set to localhost (127.0.0.1)
so that the file can be used with the provided python scripts instead of actual
hardware. This is useful for testing out the UI and experimenting with changes
to the settings files.

2. `test.json`: This file contains a single random foraging task that can be run
on the physical/fabric treadmill (requires the arduino input to function). This
file contains comments which could be useful in guiding new users on how to
configure future experiments. There is both a capacitance sensor configured as
well as a simple TTL input that are rewarded. A single reward context is defined
that will place 3 reward zones with positions that are shuffled on every lap.
The `initial_open` key is set to `true`, indicating that the first water drop
will be delivered automatically when the animals enter the rewarded area. A
`blocked_shuffle` decorator is provided, but commented out. If uncommented this
would define an area of the track form which the reward zone could not be
shuffled into. Two tones are also played during the experiment;  one at position
1000 and the second at position 1500 but only between 5 and 10 seconds of
elapsed trial time.

3. `test_vr_3.json`: This settings file sets up a VR context that communicates
with the provided vrMate program to display visual cues spanning 3 screens.
Additionally, a light fog condition surrounds the reward zone between laps
2 and 30 and a heavy fog condition exists for laps 35-50 and 55-70 as an
example of the `scheduled_context` decorator. Notably, this decorator could
similarly be applied to any context type (e.g. reward zones, tones, the VR
scenes, etc..) so this provides an example of how to setup of experiments with
cues that shift across laps.

4. `salience_settings.json`: Salience experiments expose the animals to several
stimuli regardless of running or position. This settings file interleaves light,
reward/water, odor, and 2 tones into block and presents each block of 4 cues 10
times. For this experiment the arduino is outputting TTL pulses at the specified
pin numbers. In order to function, the behavior controller would need to be
connected via the listed output pins to the correct cue-providing apparatus.
