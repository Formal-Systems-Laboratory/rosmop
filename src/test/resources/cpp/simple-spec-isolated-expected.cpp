#include "rvmonitor.h"
void monitorCallback_testEvent(const std_msgs::Float64::ConstPtr& monitored_msg)
{

    std_msgs::Float64 rv_msg;
    rv_msg.data = monitored_msg->data;


    double& testPar = rv_msg.data;

        {
           ROS_INFO("Test Parameter Binding is %g", testPar);
       }



}

int main(int argc, char ** argv)
{
    ros::init(argc, argv, "rvmonitor");
    ros::NodeHandle testHandle;
    ros::Subscriber testSubscriber =
        testHandle.subscribe("test" , 1000, monitorCallback_testEvent);

    ros::spin();
return 0;
}
