# ROSMOP Overview

[Monitoring-Oriented Programming (MOP)](http://fsl.cs.illinois.edu/mop),
is a software development and analysis framework which aims to reduce
the gap between formal specification and implementation by allowing
them together to form a system.
In MOP, runtime monitoring is supported and encouraged as a
fundamental principle for building reliable software: monitors are
automatically synthesized from specified properties and integrated
with the original system to check its dynamic behaviors during
execution. When a specification is violated or validated at runtime,
user-defined actions will be triggered, which can be any code: from
information logging to runtime recovery. 

[ROSMOP](http://fsl.cs.illinois.edu/index.php/ROSMOP)
is an instance of MOP for the 
[Robot Operating System (ROS)](http://www.ros.org/).

## Installation

To install and build ROSMOP, please refer to [INSTALL.md](INSTALL.md) for 
instructions.

## Usage

Refer to [docs/Usage.md](docs/Usage.md) for detailed instructions on how
to use ROSMOP.
