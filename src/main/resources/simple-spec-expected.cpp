#include "rvmonitor.h"
void monitorCallback_checkLevel(const std_msgs::Float64::ConstPtr& monitored_msg)
{

    std_msgs::Float64 rv_msg;


    double& water_level = rv_msg.data;

        {
         if(water_level >= 5.0) {
           ROS_INFO("Current level %g can go above 5.0", water_level);
         }
       }



}

int main(int argc, char ** argv)
{
    ros::init(argc, argv, "rvmonitor");
    ros::NodeHandle waterlevelHandle;
    ros::Subscriber waterlevelSubscriber = 
        waterlevelHandle.subscribe("water_level" , 1000, monitorCallback_checkLevel);
        
    ros::spin();
return 0;
}
