# Installing ROSMOP

Here are instructions for installing and building ROSMOP by checking out its
source code on GitHub.

## Prerequisites

ROSMOP requires Git, JDK and Maven.

1. [Git](http://git-scm.com/book/en/Getting-Started-Installing-Git)
v.1.8 or higher
 * Check Git is installed properly: run `git` from a terminal.
2. [JDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
v.7 or higher
 * Check Java is installed properly: run `java -version` from a terminal.
3. [Maven](https://maven.apache.org)
 * We recommend version 3.3 or higher. Building with a version lower than 3.3
   may also work, but has not been tested.

## Install and Build

ROSMOP currently works integrated with
[ROSRV](http://fsl.cs.illinois.edu/ROSRV). If you have already checked out the
ROSRV source code by using the `--recursive` option, you do not have to check
out the ROSMOP source code again (i.e. skip step 1).

1. Run `git clone https://github.com/runtimeverification/rosmop.git` to check
out the source code from the Github repository.

2. Add `<rosmop_HOME>/bin` to your PATH.

3. Run
 * `cd <rosmop_HOME>`
 * `mvn package`

4. Make sure the build is successful.

See [docs/Usage.md](docs/Usage.md) for information on how to run ROSMOP.
Get help or report problems on
[ROSMOP's issues page](https://github.com/runtimeverification/rosmop/issues).
