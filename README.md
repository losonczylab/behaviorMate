# behaviorMate

## Overview
behaviorMate is an Open Source package for collecting time-stamped behavior data during animal experiments. It provides:

- A user interface for adjusting experimental parameters and viewing its current state
- Asynchronous communication with actuators (reward valves, tone generators, etc.) and sensors (rotary encoders, capacitance sensors, etc.)
- Time-stamped logging to a TDML file

## Citing behaviorMate
If you use behaviorMate for your research, please cite the following paper in any resulting publications:

<behaviorMate paper citation>

## Dependencies
<a href="https://www.oracle.com/java/technologies/javase/javase8-archive-downloads.html">Java 8</a>

Windows 10/11

## Installation

Option 1: Download prebuilt jar
  1) Download zip: [behaviorMate.zip](https://s3.amazonaws.com/losonczylab.behaviormate/Downloads/1.0.0/behaviorMate.zip)
  2) Extract the behaviorMate folder.

Option 2: Download and build from source <link> [Windows]
  1) Clone the repository:
     ```
     git clone git@github.com:losonczylab/behaviorMate.git
     ```
  2) Install Chocolatey package manager and make
     - Open PowerShell as administrator and run the following commands:
     ```
     Set-ExecutionPolicy Bypass -Scope Process -Force; [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072; iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))
     ```
     ```
     choco install make
     ```
  3) Navigate to the behaviorMate root project directory containing the "make" file. Then run:
     ```
     make all
     ```

## Starting BehaviorMate

To start the application simply open the BehaviorMate.jar file using your Java 8 installation.
                
After launching BehaviorMate, a file selector will pop up for choosing the desired settings file. If the settings file is invalid, the file selector will pop up again. If the selector is x'd out, the program will terminate.
                
## Setting Up Your Experiment

1) Fill out the "Project Name" and "Mouse Name" fields. The trial will not be able to start until these are filled.

2)
    2a) Treadmill only: hit the "Calibrate" button and move treadmill manually by hand until the lap reset bar turns white on lap completion and the desired error is achieved. Add the new position scale to your settings file.

    2b) VR only: hit the "Reset" and "Zero" position buttons.
Filling out trial data fields
                
3) Start will start the trial. After hitting Start, the Stop button will appear. After clicking Stop, you will have the option of Saving or Deleting the collected behavior data. If saved, the data will be written to a TDML file.

## Configuring the Settings File

The settings file is what you will use to configure BehaviorMate to run your experiment. The default settings file BehaviorMate will look for is settings.json in the root project directory. However, any properly configured JSON file in any directory may be used. Example settings files and a discussion of their components are presented in main figure 2. Note: not all possible options and configurations are present in figure 2 (// may want to have a comprehensive cheat sheet for all possible properties). One of the most important aspects of the settings file are Context Decorators. These are what you will use to set up the "events" of your experiment such as releasing an odor at certain times, providing a water reward at certain locations in the track, etc. The decorators simply control the conditions in which a Context will be enabled. The Context Decorators have no knowledge of the underlying Context they are managing and work for any Context. Context decorators should not wrap other context decorators, as the resulting behavior can sometimes be unpredictable. Instead, it is encouraged that you create your own decorators for more intricate experiments since creating decorators is fairly straightforward.

## Supported Decorators

This is a brief explanation of each decorator class and how to use this class in the settings file. These are referred to in the settings file by removing "ContextDecorator" from the name (Ex: AlternatingContextDecorator = Alternating).
        
AlternatingContextDecorator -Used to disable the Context every certain number of laps.
                
BlockedShuffleContextDecorator - Used to place the Context randomly along a certain section of the track.
                
DelayedContextDecorator - Delays the Context from being enabled until a certain amount of time has passed after the start of the trial.
                
JointContextDecorator - Allows a group of Contexts to be enabled and disabled together. Provides options to allow Contexts to be enabled a certain amount of time or certain distance after another Context (Ex: Enabling an odor Context 0.5 seconds after a lick Context has been enabled).
                                
RandomContextDecorator - Used to enable the Context every random number of laps. It is disabled otherwise.
                
ScheduledContextDecorator - Used to disable the Context on certain laps and it is enabled otherwise.
                                
TimedITIContextDecorator - Used to disable the Context for random amounts of time before it is re-enabled.

# For Developers:
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

## Specifying your decorator in the settings file

## License

Unless otherwise specified in individual files, all code is Copyright (C) 2023 The Trustees of Columbia University in the City of New York.

This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
