# behaviorMate

## Overview
behaviorMate is an Open Source package for collecting time-stamped behavior data during animal experiments. It provides:

User Interface for adjusting experimental parameters and viewing its current state
Communicating asynchronously with experimental actuators (reward valves, tone generators, etc.) and sensors (rotary encoders, capacitance sensors, etc.)
Logging experimental events in a time-stamped file

## Dependencies
Java 8

Windows 10+

## Installation

Option 1: Download and extract zip <link>

Option 2: Download and build from source <link>

## Starting BehaviorMate

To start the application simply click the BehaviortMate shortcut created during installation or execute the run.bat batch file in the project's root directory.
                
After launching BehaviorMate, a file selector will popup for choosing the desired settings file. If the settings file is invalid, the file selector will pop up again. If the selector is x'd out, the program will terminate.
                
## Setting Up Your Experiment

1) Fill out the "Project Name" and "Mouse Name" fields. The trial will not be able to start until these are filled.

2)
    2a) Treadmill only: hit the "Calibrate" button and move treadmill manually by hand until the lap reset bar turns white on lap completion and the desired error is achieved. Add the new position scale to your settings file.

    2b) VR: hit the "Reset" and "Zero" position buttons.
Filling out trial data fields
                
3) Start will start the trial. After hitting Start, the Stop button will appear. After clicking Stop, you will have the option of Saving or Deleting the collected behavior data. If saved, the data will be written to a TDML file.

## Citing behaviorMate
If you use SIMA for your research, please cite the following paper in any resulting publications:

<behaviorMate paper citation>

## Important Terminology for Developers

Context - a feature of the mouse’s environment. The feature can spatial or temporal. Some 467
context decorators may make use of both simultaneously, such as a custom decorator that 468
gives a reward in the second half of the track every other minute or lap. A context is made 469
up of subcontexts (see below). The number of subcontexts is set by the location parameter 470
(maybe should be renamed subcontexts) in the settings file. 471

Subcontext (May want to call this an Event) - Take a random context decorator with 4 locations 472
of radius 50. When the mouse enters any of the 4 random blocks, it will be given a reward. 473
Each of these blocks can be thought of as a subcontext. 474

Context List - probably don’t need to talk about this. Seems relevant only for code 475

Context Decorator - In the source code, a Context Decorator will wrap a Context List and 476
provide additional functionality. This is what the user will implement to get behaviormate to 477
provide the desired functionality. 478

Spatial Field - the spatial field of a subcontext is the section of track it encompasses. The 479
spatial field begins at LOCATION-RADIUS and ends at LOCATION+RADIUS. If the mouse’s 480
position is greater than LOCATION-RADIUS and less than LOCATION+RADIUS, it is said to 481
be in the context. The mouse is in the spatial field of a context if it is in the spatial field of any 482
of its subcontexts. 483

Temporal Field - temporal field of a subcontext is either the time or set of laps during which 484
the Context (and all of its subcontexts) will be enabled. For an alternating context with n=2, 485
the temporal field is the set of all even-numbered laps. For a delayed context decorator with 486
delay=6, the temporal field includes the span of time starting at 6 seconds and going to infinity. 487

Enable (alternate terms: active) - A Context is enabled on a given lap or at a certain moment 488
in time if when the mouse enters its field, some sort of reward or stimulus will be given. A 489
context is enabled unless the implemented decorator causes it to be Disabled under certain 
conditions. For example, an alternating context decorator Disables the wrapped context every 491
certain number of laps.

Disable - A Context is disabled if even when the mouse is in its field, no reward or stimulus 493
will be given. 494

Trigger - A Context is said to be triggered if it is 1) enabled and 2) one of its subcontexts has 495
been triggered. A subcontext is triggered when both its spatial (mouse is in the right location) 496
and temporal (the time is right) conditions have been met. A subcontext cannot be triggered if 497
the Context it is a part of is not enabled.

## License

Unless otherwise specified in individual files, all code is Copyright (C) 2023 The Trustees of Columbia University in the City of New York.

This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
