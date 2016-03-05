# Installing ROSMOP

Here are instructions for installing and building ROSMOP by checking out its 
source code on GitHub.

## Prerequisites

ROSMOP requires Git, JDK, Ant, JavaCC and RV-Monitor.

1. [Git](http://git-scm.com/book/en/Getting-Started-Installing-Git)
v.1.8 or higher
 * Check Git is installed properly: run `git` from a terminal.
2. [JDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
v.7 or higher
 * Check Java is installed properly: run `java -version` from a terminal.
3. [Ant](http://ant.apache.org/bindownload.cgi)
v.1.8 or higher
 * Check Ant is installed properly: run `ant -version` from a terminal.
4. [JavaCC](https://javacc.java.net/)
v.5 or higher
 * Check JavaCC is installed properly: run `javacc` from a terminal.
5. [RV-Monitor](https://www.runtimeverification.com/monitor)
v.1.3 or higher
 * Check RV-Monitor is installed properly: run `rv-monitor -version` from a
   terminal.
 * Add `RVMONITOR` as an environment variable and set it to 
   `<RV-Monitor_HOME>/lib`.

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
 * `ant`

4. Make sure the build is successful.

See [docs/Usage.md](docs/Usage.md) for information on how to run ROSMOP.
Get help or report problems on
[ROSMOP's issues page](https://github.com/runtimeverification/rosmop/issues).
