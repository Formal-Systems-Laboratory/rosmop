# ROSMOP Usage

## Commands
 * One event specification (.rv) file: `rosmop a.rv`
 * Multiple event specification (.rv) files: `rosmop a.rv b.rv ... z.rv`
 * Folder of *only* event specification (.rv) files: `rosmop specs/`


## Event specifications

All the specifications are provided by users. ROSMOP generates C++ code automatically based on those specifications. Each event generates one call back method and all the call back methods are registered by RVMaster. Parameters of events are treated as references to fields in monitored messages, so users can modify messages in event handler code. Event handlers (i.e. actions) are inserted in call back methods and called by RVMaster at runtime. Event specification names are used to identify the monitors. By using those names, one can enable or disable desired monitors, and hence control which events take place. One shall create different specifications for each separate concern, so that disabling a monitor does not interfere with the functioning of others.

Basic form of a user-defined event specification is the following:

```c++
#include <library>
spec(){
	int i;
	bool b;

	event event1(parameters) topic messageType '{pattern}'
	{
		//action code
	}
}
```

In an event specification, there can be multiple events and the user specified
action codes of these different events can communicate through adding
specification-scoped shared variables. This allows the user to monitor
properties across different topics.  One should be careful when generating the
code from multiple specifications as all the shared variables become global for
all the callback functions. **For now, it is the user's responsibility to mind
the possible duplicates as well as making sure that the action code is
compilable.**

In case of multiple events on one topic, the callback function is named after
the topic and the action codes of these events are merged. However, this does
not affect the ability to enable/disable different monitors with events on the
same topic.

The user uses the parameters (s)he specified in the action code to check or
(possibly) alter the information received in the message. The event parameters
(along with their types) and the variable matching in the specified pattern
should be compatible with the given message type. Furthermore, the variable
names of the parameters and the ones in the pattern should match each other. If
there is such an invalid matching, the code will not be generated correctly. 


## Example

The following event specification defines a monitor which makes sure the robot doesn't shoot itself.
In this specification, there are two events, `checkPosition` and `safeTrigger`, which have their own parameters and topics. On each topic, there can only be a certain type of message sent and received, which is also provided in the event signature. `checkPosition` event checks whether the gun is at a safe position to trigger, i.e. `position > -0.45` (not pointing at itself). It listens to topic `/landshark/joint_states` with the message type `sensor_msgs/JointState`. The fields of the message type can be accessed by providing the parameters of interest as done here; there are two arrays, `name` and `position`, which are bound to variables `N` and `P`, respectively. These parameters are used in the action code of the event to check the validity of the message content.

```c++
#include <stdint.h>
 
safeTrigger() {
       bool isSafeTrigger = false;
 
       event checkPosition(std::string monitored_name, double monitored_position) /landshark/joint_states sensor_msgs/JointState '{name[1]:monitored_name, position[1]:monitored_position}'
       {
		if(monitored_name=="turret_tilt")
		{
			if(monitored_position > -0.45){
				isSafeTrigger = true;
				ROS_INFO("Safe to trigger");
			}else{
				isSafeTrigger = false;
				ROS_INFO("Not safe to trigger");
			}
		}
       }
 
       event safeTrigger() /landshark_control/trigger landshark_msgs/PaintballTrigger '{}'
       {
		if(!isSafeTrigger)
		{
			ROS_WARN("Monitor: Not allowed to trigger in this pose!");
			return;
		}
       }
}
```
